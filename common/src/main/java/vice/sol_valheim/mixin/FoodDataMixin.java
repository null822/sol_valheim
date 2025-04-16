package vice.sol_valheim.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vice.sol_valheim.SOLValheim;
import vice.sol_valheim.accessors.FoodDataPlayerAccessor;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

@Mixin(FoodData.class)
public class FoodDataMixin implements FoodDataPlayerAccessor
{
    @Unique
    private Player sol_valheim$player;

    @Override
    public Player sol_valheim$getPlayer() { return sol_valheim$player;}

    @Override
    public void sol_valheim$setPlayer(Player player) { sol_valheim$player = player; }

    @Inject(at = @At("HEAD"), method = "tick")
    public void onTickHead(Player player, CallbackInfo ci) {
        var foodData = sol_valheim$player.getFoodData();
        var valheimFoodData = ((PlayerEntityMixinDataAccessor)sol_valheim$player).sol_valheim$getFoodData();

        // reduce food timers when food level would normally be reduced
        if (foodData.getExhaustionLevel() > 4.0F) {
            valheimFoodData.reduceTimers(SOLValheim.Config.common.defaultTimer * 2);
        }
    }

    @Inject(at = @At("TAIL"), method = "tick")
    public void onTickTail(Player player, CallbackInfo ci)
    {
        var foodData = sol_valheim$player.getFoodData();
        var valheimFoodData = ((PlayerEntityMixinDataAccessor)sol_valheim$player).sol_valheim$getFoodData();

        // emulate food fill level for mod compatibility
        var fill = (valheimFoodData.ItemEntries.size() * 20) / valheimFoodData.MaxItemSlots;
        if (foodData.getFoodLevel() != fill)
            foodData.setFoodLevel(fill);
    }
}