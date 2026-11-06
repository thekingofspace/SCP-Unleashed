package net.rai.scp_unleashed.gateway.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlock;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlockEntity;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum GatewayJadeProvider implements IBlockComponentProvider {
    Instance;

    public static final int Priority = 90;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "contained_gateway");
    private static final String Prefix = "jade.scp_unleashed.contained_gateway.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public int getDefaultPriority() {
        return Priority;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        ContainedGatewayBlockEntity Gate = ContainedGatewayBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Gate == null) {
            return;
        }
        GatewayDestination Destination = Gate.destination();
        switch (Gate.status()) {
            case ContainedGatewayBlockEntity.StatusOpen -> {
                Tooltip.add(Component.translatable(Prefix + "open").withStyle(ChatFormatting.GREEN));
                if (Destination != null) {
                    Tooltip.add(Component.translatable(Prefix + "leads", Destination.displayName()).withStyle(ChatFormatting.AQUA));
                }
            }
            case ContainedGatewayBlockEntity.StatusStandby -> Tooltip.add(Component.translatable(Prefix + "standby").withStyle(ChatFormatting.GOLD));
            case ContainedGatewayBlockEntity.StatusFault -> Tooltip.add(Component.translatable(Prefix + "fault").withStyle(ChatFormatting.RED));
            default -> {
                Tooltip.add(Component.translatable(Prefix + "off").withStyle(ChatFormatting.DARK_GRAY));
                if (Gate.hasShard() && Destination != null) {
                    Tooltip.add(Component.translatable(Prefix + "holding", Destination.displayName()).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}
