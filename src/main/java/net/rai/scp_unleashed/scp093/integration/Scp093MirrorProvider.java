package net.rai.scp_unleashed.scp093.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp093.Scp093MirrorBlock;
import net.rai.scp_unleashed.scp093.Scp093Registry;
import net.rai.scp_unleashed.scp093.Scp093ReturnMirrorBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp093MirrorProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp093Registry.location("scp093_mirror");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (State.getBlock() instanceof Scp093ReturnMirrorBlock) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp093_return").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (!(State.getBlock() instanceof Scp093MirrorBlock)) {
            return;
        }
        if (State.getValue(Scp093MirrorBlock.Seated)) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp093_seated").withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp093_empty").withStyle(ChatFormatting.GRAY));
        }
    }
}
