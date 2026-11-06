package net.rai.scp_unleashed.mtf.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.mtf.MtfLandingZone;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.mtf.MtfTier;

@JeiPlugin
public class MtfJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "mtf_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(MtfRegistry.SignalRadio.get()),
                Component.translatable("jei.scp_unleashed.info.mtf_signal_radio.1"),
                Component.translatable("jei.scp_unleashed.info.mtf_signal_radio.2", MtfLandingZone.DefaultHover),
                Component.translatable("jei.scp_unleashed.info.mtf_signal_radio.3", MtfTier.Faint.size(), MtfTier.Abyssal.size()),
                Component.translatable("jei.scp_unleashed.info.mtf_signal_radio.4", MtfTier.Faint.durationTicks() / 1200, MtfTier.Abyssal.durationTicks() / 1200),
                Component.translatable("jei.scp_unleashed.info.mtf_signal_radio.5"));
    }
}
