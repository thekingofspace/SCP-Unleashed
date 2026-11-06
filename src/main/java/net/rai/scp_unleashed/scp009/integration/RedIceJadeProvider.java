package net.rai.scp_unleashed.scp009.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp009.Scp009Exposure;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum RedIceJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp009_red_ice");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp009_red_ice").withStyle(ChatFormatting.RED));
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp009_red_ice.danger", (int) Scp009Exposure.Radius).withStyle(ChatFormatting.RED));
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp009_red_ice.mine").withStyle(ChatFormatting.GRAY));
    }
}
