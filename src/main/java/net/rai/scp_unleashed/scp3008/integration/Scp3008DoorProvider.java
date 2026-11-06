package net.rai.scp_unleashed.scp3008.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.block.Scp3008EntranceBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp3008DoorProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp3008_door");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (Accessor.getBlockState().getBlock() instanceof Scp3008EntranceBlock) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp3008_entrance").withStyle(ChatFormatting.GRAY));
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp3008_exit").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
