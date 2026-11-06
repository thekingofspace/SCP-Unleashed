package net.rai.scp_unleashed.microhid.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.microhid.MicroHidEnergy;
import net.rai.scp_unleashed.microhid.MicroHidRegistry;

import java.util.Locale;

@JeiPlugin
public class MicroHidJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "micro_hid_jei_plugin");
    private static final String GunPrefix = "jei.scp_unleashed.info.micro_hid.";
    private static final String PlatformPrefix = "jei.scp_unleashed.info.micro_charging_platform.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        GunStats Stats = GunKind.MicroHid.getStats();
        Registration.addItemStackInfo(new ItemStack(MicroHidRegistry.MicroHid.get()),
                Component.translatable(GunPrefix + "1"),
                Component.translatable(GunPrefix + "2", number(MicroHidEnergy.spinSeconds())),
                Component.translatable(GunPrefix + "3", number(Stats.getDamage()), number(20.0F / Stats.getInterval()), Math.round(Stats.getRange())),
                Component.translatable(GunPrefix + "4", number(MicroHidEnergy.fireSeconds())),
                Component.translatable(GunPrefix + "5"));
        Registration.addItemStackInfo(new ItemStack(MicroHidRegistry.PlatformItem.get()),
                Component.translatable(PlatformPrefix + "1"),
                Component.translatable(PlatformPrefix + "2", MicroHidEnergy.ChargeDraw, Math.round(MicroHidEnergy.chargeSeconds())),
                Component.translatable(PlatformPrefix + "3"));
    }

    private static String number(float Value) {
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
