package net.rai.scp_unleashed.ultralaser.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.ultralaser.UltralaserPriming;
import net.rai.scp_unleashed.ultralaser.UltralaserRegistry;
import net.rai.scp_unleashed.ultralaser.UltralaserScore;

import java.util.Locale;

@JeiPlugin
public class UltralaserJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ultralaser_jei_plugin");
    private static final String GunPrefix = "jei.scp_unleashed.info.ultralaser.";
    private static final String CellPrefix = "jei.scp_unleashed.info.ultralaser_cell.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(UltralaserRegistry.Ultralaser.get()),
                Component.translatable(GunPrefix + "1"),
                Component.translatable(GunPrefix + "2", seconds(UltralaserScore.seconds())),
                Component.translatable(GunPrefix + "3", Math.round(GunKind.Ultralaser.getStats().getDamage()), Math.round(GunKind.Ultralaser.getStats().getRange())),
                Component.translatable(GunPrefix + "4", seconds(UltralaserPriming.CooldownTicks / 20.0F)),
                Component.translatable(GunPrefix + "5"));
        Registration.addItemStackInfo(new ItemStack(UltralaserRegistry.Cell.get()),
                Component.translatable(CellPrefix + "1"),
                Component.translatable(CellPrefix + "2"),
                Component.translatable(CellPrefix + "3", UltralaserRegistry.CellStack));
    }

    private static String seconds(float Value) {
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
