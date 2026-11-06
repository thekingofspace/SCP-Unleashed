package net.rai.scp_unleashed.scp087.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp087.Scp087DoorBlock;
import net.rai.scp_unleashed.scp087.Scp087Stairwell;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp087DoorProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp087_door");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockState().getBlock() instanceof Scp087DoorBlock)) {
            return;
        }
        if (Scp087Stairwell.inStairwell(Accessor.getLevel())) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp087_door.inside").withStyle(ChatFormatting.GRAY));
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp087_door.outside").withStyle(ChatFormatting.DARK_GRAY));
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp087_door.sealed").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
