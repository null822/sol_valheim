package vice.sol_valheim;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ValheimFoodData
{
    public static final EntityDataSerializer<ValheimFoodData> FOOD_DATA_SERIALIZER = new EntityDataSerializer<>(){
        @Override
        public void write(FriendlyByteBuf buffer, ValheimFoodData value)
        {
            buffer.writeNbt(value.save(new CompoundTag()));
        }

        @Override
        public ValheimFoodData read(FriendlyByteBuf buffer) {

            return ValheimFoodData.read(buffer.readNbt());
        }

        @Override
        public ValheimFoodData copy(ValheimFoodData value)
        {
            var ret = new ValheimFoodData();
            ret.MaxItemSlots = value.MaxItemSlots;
            ret.ItemEntries = value.ItemEntries.stream().map(EatenFoodItem::new).collect(Collectors.toCollection(ArrayList::new));

            return ret;
        }

    };

    public List<EatenFoodItem> ItemEntries = new ArrayList<>();
    public int MaxItemSlots = SOLValheim.Config.common.maxSlots;


    public boolean canEat(ItemStack food)
    {
        if (food.getItem() == Items.ROTTEN_FLESH)
            return true;

        var existing = getEatenFood(food);
        if (existing != null)
            return existing.canEatEarly();

        if (ItemEntries.size() < MaxItemSlots)
            return true;

        return ItemEntries.stream().anyMatch(EatenFoodItem::canEatEarly);
    }

    public EatenFoodItem getEatenFood(ItemStack food) {
        return ItemEntries.stream()
                .filter((item) -> ItemStack.matches(item.item, food))
                .findFirst()
                .orElse(null);
    }

    public void eatItem(ItemStack food)
    {
        System.out.println("Eat: " + BuiltInRegistries.ITEM.getKey(food.getItem()));
        food = food.copyWithCount(1);

        if (food.getItem() == Items.ROTTEN_FLESH)
            return;

        var config = ModConfig.getFoodConfig(food);

        var existing = getEatenFood(food);
        if (existing != null)
        {
            if (!existing.canEatEarly())
                return;

            existing.ticksLeft = config.getTime();
            return;
        }

        if (ItemEntries.size() < MaxItemSlots)
        {
            var eatenFoodItem = new EatenFoodItem(food, config.getTime());
            ItemEntries.add(eatenFoodItem);
            return;
        }

        for (var item : ItemEntries)
        {
            if (item.canEatEarly())
            {
                item.ticksLeft = config.getTime();
                item.item = food;
                return;
            }
        }
    }

    public void tick()
    {
        for (var item : ItemEntries)
        {
            item.ticksLeft--;
        }

        ItemEntries.removeIf(item -> item.ticksLeft <= 0);
        ItemEntries.sort(Comparator.comparingInt(a -> a.ticksLeft));
    }

    public void clear()
    {
        ItemEntries.clear();
    }

    public float getTotalFoodNutrition()
    {
        float nutrition = 0f;
        for (var item : ItemEntries)
        {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(item.item);
            if (food == null)
                continue;

            nutrition += food.getHearts();
        }

        return nutrition;
    }


    public float getRegenSpeed()
    {
        float regen = 0.25f;
        for (var item : ItemEntries)
        {
            ModConfig.Common.FoodConfig food = ModConfig.getFoodConfig(item.item);
            if (food == null)
                continue;

            regen += food.getHealthRegen();

            // drink bonus
            if (item.item.getUseAnimation() == UseAnim.DRINK) {
                regen = regen * (1.0f + SOLValheim.Config.common.drinkSlotFoodEffectivenessBonus);
            }

        }

        return regen;
    }


    public CompoundTag save(CompoundTag tag) {
//        int count = 0;
        tag.putInt("max_slots", MaxItemSlots);
//        tag.putInt("count", ItemEntries.size());
        var stomach = new ListTag();
        for (var item : ItemEntries)
        {
            var itemCompound = new CompoundTag();

            itemCompound.putString("id", BuiltInRegistries.ITEM.getKey(item.item.getItem()).toString());
            var nbt = item.item.getTag();
            if (nbt != null) itemCompound.put("tag", nbt);
            itemCompound.putInt("ticks", item.ticksLeft);

            stomach.add(itemCompound);
        }
        tag.put("stomach", stomach);
        return tag;
    }

    public static ValheimFoodData read(CompoundTag tag) {
        var instance = new ValheimFoodData();
        if (!tag.contains("max_slots")) return instance;
        var maxSlots = tag.getInt("max_slots");
        instance.MaxItemSlots = maxSlots == 0 ? SOLValheim.Config.common.maxSlots : maxSlots;

        if (!tag.contains("stomach")) return instance;
        var stomach = tag.getList("stomach", Tag.TAG_COMPOUND);
        for (var itemTag : stomach) {
            var itemCompound = (CompoundTag)itemTag;

            var id = itemCompound.getString("id");
            var ticks = itemCompound.getInt("ticks");

            var item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
            var stack = new ItemStack(item, 1);

            if (itemCompound.contains("tag")) {
                stack.setTag(itemCompound.getCompound("tag"));
            }

            instance.ItemEntries.add(new EatenFoodItem(stack, ticks));
        }

        return instance;
    }


    public static class EatenFoodItem {
        public ItemStack item;
        public int ticksLeft;

        public boolean canEatEarly() {
            if (ticksLeft < 1200)
                return true;

            var config = ModConfig.getFoodConfig(item);
            if (config == null)
                return false;

            return ((float) this.ticksLeft / config.getTime()) < SOLValheim.Config.common.eatAgainPercentage;
        }

        public EatenFoodItem(ItemStack item, int ticksLeft)
        {
            this.item = item;
            this.ticksLeft = ticksLeft;
        }

        public EatenFoodItem(EatenFoodItem eaten)
        {
            this.item = eaten.item;
            this.ticksLeft = eaten.ticksLeft;
        }
    }
}