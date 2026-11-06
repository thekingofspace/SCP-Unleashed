package net.rai.scp_unleashed.scp143.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp143.Scp143LeavesBlock;
import net.rai.scp_unleashed.scp143.client.Scp143ClientState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp143JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp143_tree");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (Scp143ClientState.isOriginal(Accessor.getLevel(), Accessor.getPosition())) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp143.original").withStyle(ChatFormatting.DARK_RED));
        }
        if (State.getBlock() instanceof Scp143LeavesBlock && !State.getValue(LeavesBlock.PERSISTENT)) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp143.shedding").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
