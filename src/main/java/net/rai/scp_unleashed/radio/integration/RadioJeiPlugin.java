package net.rai.scp_unleashed.radio.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.radio.RadioNetwork;
import net.rai.scp_unleashed.radio.RadioRegistry;
import net.rai.scp_unleashed.radio.VoiceConfig;

@JeiPlugin
public class RadioJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "radio_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        RadioNetwork.Ranges Limits = VoiceConfig.ranges();
        Registration.addItemStackInfo(new ItemStack(RadioRegistry.Radio.get()),
                Component.translatable("jei.scp_unleashed.info.handheld_radio.1"),
                Component.translatable("jei.scp_unleashed.info.handheld_radio.2"),
                Component.translatable("jei.scp_unleashed.info.handheld_radio.3", VoiceConfig.chatPrefix()),
                Component.translatable("jei.scp_unleashed.info.handheld_radio.4", Limits.Direct()));
        Registration.addItemStackInfo(new ItemStack(RadioRegistry.TowerItem.get()),
                Component.translatable("jei.scp_unleashed.info.radio_booster_tower.1", RadioNetwork.MaxMasts),
                Component.translatable("jei.scp_unleashed.info.radio_booster_tower.2", (int) Limits.coverage(1), (int) Limits.coverage(RadioNetwork.MaxMasts),
                        (int) Limits.link(1), (int) Limits.link(RadioNetwork.MaxMasts)),
                Component.translatable("jei.scp_unleashed.info.radio_booster_tower.3", VoiceConfig.demand(1), VoiceConfig.demand(RadioNetwork.MaxMasts)),
                Component.translatable("jei.scp_unleashed.info.radio_booster_tower.4"));
        Registration.addItemStackInfo(new ItemStack(RadioRegistry.MastItem.get()),
                Component.translatable("jei.scp_unleashed.info.radio_mast.1", RadioNetwork.MaxMasts));
    }
}
