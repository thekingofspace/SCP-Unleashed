package net.rai.scp_unleashed.flamethrower.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.flamethrower.FlamerRegistry;
import net.rai.scp_unleashed.flamethrower.FuelCanisterItem;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;

import java.util.Locale;

@JeiPlugin
public class FlamerJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "flamethrower_jei_plugin");
    private static final String GunPrefix = "jei.scp_unleashed.info.flamethrower.";
    private static final String CanisterPrefix = "jei.scp_unleashed.info.fuel_canister.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        GunStats Stats = GunKind.Flamethrower.getStats();
        Registration.addItemStackInfo(GunItem.loaded(GunKind.Flamethrower),
                Component.translatable(GunPrefix + "1"),
                Component.translatable(GunPrefix + "2", number(Stats.getDamage() * 20.0F / Stats.getInterval()), number(Stats.getRange())),
                Component.translatable(GunPrefix + "3"),
                Component.translatable(GunPrefix + "4", number(Stats.getCapacity() * Stats.getInterval() / 20.0F)),
                Component.translatable(GunPrefix + "5"));
        Registration.addItemStackInfo(new ItemStack(FlamerRegistry.FuelCanister.get()),
                Component.translatable(CanisterPrefix + "1", FuelCanisterItem.Capacity),
                Component.translatable(CanisterPrefix + "2"));
    }

    private static String number(float Value) {
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
