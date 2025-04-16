package vice.sol_valheim;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;


@Config(name = SOLValheim.MOD_ID)
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class ModConfig extends PartitioningSerializer.GlobalData {

    public static Common.FoodConfig getFoodConfig(ItemStack itemStack) {
        var item = itemStack.getItem();

        var existing = SOLValheim.Config.common.foodConfigs.get(item.arch$registryName());

        if (existing == null) {
            existing = generateFoodConfig(itemStack);

            if (existing != null) {
                SOLValheim.Config.common.foodConfigs.put(item.arch$registryName(), existing);
            }
        }

        return existing;
    }

    private static Common.FoodConfig generateFoodConfig(ItemStack itemStack) {

        var item = itemStack.getItem();
        var registryName = item.arch$registryName();
        if (registryName == null) {
            return null;
        }
        var itemId = registryName.toString();

        var food = item.getFoodProperties();
        if (food == null) {
            if (item == Items.CAKE) {
                food = new FoodProperties.Builder().nutrition(10).saturationMod(1f).build();
            } else if (itemId.equals("minecraft:potion")) {
                food = new FoodProperties.Builder().nutrition(1).saturationMod(0.75f).build();
            } else if (itemId.contains("milk")) {
                food = new FoodProperties.Builder().nutrition(6).saturationMod(1f).build();
            } else if (itemStack.getUseAnimation() == UseAnim.DRINK) {
                food = new FoodProperties.Builder().nutrition(2).saturationMod(0.4f).build();
            } else {
                return null;
            }
        }

        var config = new Common.FoodConfig();
        config.nutrition = food.getNutrition();
        config.healthRegenModifier = 1f;
        config.saturationModifier = food.getSaturationModifier();

        if (itemId.startsWith("farmers")) {
            config.nutrition *= 1.25f;
            config.saturationModifier *= 1.10f;
            config.healthRegenModifier *= 1.25f;
        }

        if (itemId.equals("minecraft:golden_apple")) {
            config.nutrition = 10;
            config.healthRegenModifier = 1.5f;
        }
        if (itemId.equals("minecraft:enchanted_golden_apple")) {
            config.nutrition = 25;
            config.healthRegenModifier = 3f;
        }

        return config;
    }


    @ConfigEntry.Category("common")
    @ConfigEntry.Gui.TransitiveObject()
    public Common common = new Common();

    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.TransitiveObject()
    public Client client = new Client();

    @Config(name = "common")
    public static final class Common implements ConfigData {


        @ConfigEntry.Gui.Tooltip() @Comment("Default time in seconds that food should last per saturation level")
        public int defaultTimer = 180;

        @ConfigEntry.Gui.Tooltip() @Comment("Speed at which regeneration should occur")
        public float regenSpeedModifier = 1f;

        @ConfigEntry.Gui.Tooltip() @Comment("Time in ticks that regeneration should wait after taking damage")
        public int regenDelay = 20 * 10;

        @ConfigEntry.Gui.Tooltip() @Comment("Time in seconds after spawning before sprinting is disabled")
        public int respawnGracePeriod = 60 * 5;

        @ConfigEntry.Gui.Tooltip() @Comment("Extra speed given when your hearts are full (0 to disable)")
        public float speedBoost = 0.20f;

        @ConfigEntry.Gui.Tooltip() @Comment("Number of hearts to start with")
        public float startingHearts = 3;

        @ConfigEntry.Gui.Tooltip() @Comment("Number of food slots (range 2-5, default 3)")
        public int maxSlots = 3;

        @ConfigEntry.Gui.Tooltip() @Comment("Percentage remaining before you can eat again")
        public float eatAgainPercentage = 0.2F;

        @ConfigEntry.Gui.Tooltip() @Comment("Boost given to other foods when drinking")
        public float drinkSlotFoodEffectivenessBonus = 0.10F;

        @ConfigEntry.Gui.Tooltip() @Comment("Simulate food ticking down during night")
        public boolean passTicksDuringNight = true;

        @ConfigEntry.Gui.Tooltip(count = 5) @Comment("""
            Food nutrition and effect overrides (Auto Generated if Empty)
            - nutrition: Affects Heart Gain & Health Regen
            - saturationModifier: Affects Food Duration & Player Speed
            - healthRegenModifier: Multiplies health regen speed
            - extraEffects: Extra effects provided by eating the food. Format: { String ID, float duration, int amplifier }
        """)
        public Dictionary<ResourceLocation, FoodConfig> foodConfigs = new Hashtable<>();

        public static final class FoodConfig implements ConfigData {
            public float nutrition;
            public float saturationModifier = 1f;
            public float healthRegenModifier = 1f;
            public List<MobEffectConfig> extraEffects = new ArrayList<>();

            public int getTime() {
                var time = (int) (SOLValheim.Config.common.defaultTimer * 20 * saturationModifier * nutrition);
                return Math.max(time, 6000);
            }

            public float getHealth() {
                return nutrition;
            }

            public float getHealthRegen()
            {
                return nutrition * 0.10f * healthRegenModifier;
            }
        }

        public static final class MobEffectConfig implements ConfigData {
            @ConfigEntry.Gui.Tooltip() @Comment("Mob Effect ID")
            public String ID;

            @ConfigEntry.Gui.Tooltip() @Comment("Effect duration percentage (1f is the entire food duration)")
            public float duration = 1f;

            @ConfigEntry.Gui.Tooltip() @Comment("Effect Level")
            public int amplifier = 1;

            public MobEffect getEffect() {
                return SOLValheim.MOB_EFFECTS.getRegistrar().get(new ResourceLocation(ID));
            }
        }

    }

    @Config(name = "client")
    public static final class Client implements ConfigData {
        @ConfigEntry.Gui.Tooltip
        @Comment("Enlarge the currently eaten food icons")
        public boolean useLargeIcons = true;
    }
}