package net.rai.scp_unleashed.scp1730.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.block.Scp1730ControlBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp1730ControlJadeProvider implements IBlockComponentProvider {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.scp1730_control.";
    private static final ResourceLocation Uid = Scp1730Registry.location("scp1730_control");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp1730ControlBlock)) {
            return;
        }
        boolean Active = State.getValue(Scp1730ControlBlock.Active);
        String Key = switch (State.getValue(Scp1730ControlBlock.Type)) {
            case Reader -> Active ? "reader_open" : "reader_wait";
            case Release -> Active ? "release_done" : "release_wait";
            case Breaker -> Active ? "breaker_cut" : "breaker_on";
            case Clearance -> Active ? "clearance_open" : "clearance_wait";
            case Exit -> "exit";
        };
        Tooltip.add(Component.translatable(Prefix + Key).withStyle(Active ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }
}
