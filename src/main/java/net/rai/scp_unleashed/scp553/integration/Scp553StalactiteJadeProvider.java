package net.rai.scp_unleashed.scp553.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp553.Scp553Colony;
import net.rai.scp_unleashed.scp553.Scp553StalactiteBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp553StalactiteJadeProvider implements IBlockComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp553_stalactite");
    private static final String Prefix = "jade.scp_unleashed.scp553_stalactite.";

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
        BlockState State = Accessor.getBlockState();
        if (!State.is(Accessor.getBlock()) || !(Accessor.getBlock() instanceof Scp553StalactiteBlock)) {
            return;
        }
        int Grown = State.getValue(Scp553StalactiteBlock.Stage);
        if (!Scp553Colony.dark(Accessor.getLevel(), Accessor.getPosition())) {
            Tooltip.add(Component.translatable(Prefix + "stalled").withStyle(ChatFormatting.GRAY));
        } else if (Grown >= Scp553StalactiteBlock.MaxStage) {
            Tooltip.add(Component.translatable(Prefix + "stirring").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            Tooltip.add(Component.translatable(Prefix + "growing").withStyle(ChatFormatting.AQUA));
        }
    }
}
