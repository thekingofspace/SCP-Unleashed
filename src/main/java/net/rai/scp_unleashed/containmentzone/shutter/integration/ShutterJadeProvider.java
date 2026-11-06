package net.rai.scp_unleashed.containmentzone.shutter.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containmentzone.CellShutterBlock;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ShutterJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "lockdown_shutter");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof CellShutterBlock)) {
            return;
        }
        Tooltip.add(modeLine(State.getValue(CellShutterBlock.Lockdown)));
        Tooltip.add(statusLine(State, Accessor.getBlockEntity() instanceof ShutterBlockEntity Shutter ? Shutter.getProgress() : -1.0f));
    }

    public static Component modeLine(boolean Lockdown) {
        return Lockdown ? Component.translatable("jade.scp_unleashed.lockdown_shutter.lockdown").withStyle(ChatFormatting.GOLD)
                : Component.translatable("jade.scp_unleashed.lockdown_shutter.normal").withStyle(ChatFormatting.AQUA);
    }

    public static Component statusLine(BlockState State, float Progress) {
        boolean Opening = State.getValue(CellShutterBlock.Open);
        String Key;
        if (State.getValue(CellShutterBlock.Shut)) {
            Key = "closed";
        } else if (Progress < 0.0f) {
            Key = Opening ? "open" : "closed";
        } else if (Opening) {
            Key = Progress >= 1.0f ? "open" : "opening";
        } else {
            Key = Progress <= 0.0f ? "closed" : "closing";
        }
        ChatFormatting Colour = Key.equals("open") ? ChatFormatting.GREEN : Key.equals("closed") ? ChatFormatting.GRAY : ChatFormatting.YELLOW;
        return Component.translatable("jade.scp_unleashed.lockdown_shutter." + Key).withStyle(Colour);
    }
}
