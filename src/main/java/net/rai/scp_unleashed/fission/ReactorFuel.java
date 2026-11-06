package net.rai.scp_unleashed.fission;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Optional;

public record ReactorFuel(int Power, float Heat, int BurnTicks) {
    public static final ReactorFuel Uranium = new ReactorFuel(3200, 0.6f, 72000);
    public static final ReactorFuel Thorium = new ReactorFuel(2400, 0.45f, 108000);
    public static final ReactorFuel Standard = new ReactorFuel(2400, 0.5f, 54000);
    public static final ResourceLocation UraniumRod = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "uranium_fuel_rod");
    public static final ResourceLocation ThoriumRod = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "thorium_fuel_rod");
    public static final ResourceLocation DepletedRod = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "depleted_fuel_rod");

    public static boolean isFuel(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(FissionTags.FissionFuel);
    }

    public static ReactorFuel of(ItemStack Stack) {
        ResourceLocation Id = BuiltInRegistries.ITEM.getKey(Stack.getItem());
        if (UraniumRod.equals(Id)) {
            return Uranium;
        }
        if (ThoriumRod.equals(Id)) {
            return Thorium;
        }
        return Standard;
    }

    public static ItemStack depletedRod() {
        Optional<Item> Found = BuiltInRegistries.ITEM.getOptional(DepletedRod);
        return Found.map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    public int minutes() {
        return BurnTicks / 1200;
    }
}
