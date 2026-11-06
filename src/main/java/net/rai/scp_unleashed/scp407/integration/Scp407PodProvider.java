package net.rai.scp_unleashed.scp407.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp407.Scp407GrowthBlock;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp407PodProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp407Registry.location("scp407_pod");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        boolean Alive = Scp407GrowthBlock.alive(Accessor.getBlockState());
        Tooltip.add(Component.translatable(Scp407SpeakerProvider.Prefix + (Alive ? "pod" : "pod.withered")).withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
    }
}
