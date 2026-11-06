package net.rai.scp_unleashed.scp244.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp244.Scp244VaseBlock;
import net.rai.scp_unleashed.scp244.Scp244VaseBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp244VaseProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp244_vase");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp244VaseBlock)) {
            return;
        }
        boolean Open = State.getValue(Scp244VaseBlock.Open);
        Scp244VaseBlockEntity Vase = Accessor.getBlockEntity() instanceof Scp244VaseBlockEntity Found ? Found : null;
        if (Open) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp244_vase.open").withStyle(ChatFormatting.AQUA));
        } else if (Vase != null && Vase.intensity() > 0.0f) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp244_vase.settling").withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp244_vase.sealed").withStyle(ChatFormatting.GREEN));
        }
        if (Vase != null && Vase.bound()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp244_vase.bound").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
