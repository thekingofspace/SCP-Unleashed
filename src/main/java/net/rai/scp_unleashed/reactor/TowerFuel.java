package net.rai.scp_unleashed.reactor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.fission.ReactorFuel;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;

public final class TowerFuel {
    private TowerFuel() {
    }

    public static boolean isFuel(ItemStack Stack) {
        return ReactorFuel.isFuel(Stack);
    }

    public static boolean isDepleted(ItemStack Stack) {
        return !Stack.isEmpty() && ReactorFuel.DepletedRod.equals(BuiltInRegistries.ITEM.getKey(Stack.getItem()));
    }

    public static float heatOf(ItemStack Stack) {
        return isFuel(Stack) ? ReactorFuel.of(Stack).Heat() * ReactorTuning.FuelHeatScale : 0.0f;
    }

    public static float heatOf(ReactorFuel Fuel) {
        return Fuel.Heat() * ReactorTuning.FuelHeatScale;
    }

    public static int burnTicks(ItemStack Stack) {
        return isFuel(Stack) ? ReactorFuel.of(Stack).BurnTicks() * ReactorTuning.FuelBurnScale : 0;
    }

    public static ItemStack depleted() {
        return ReactorFuel.depletedRod();
    }
}
