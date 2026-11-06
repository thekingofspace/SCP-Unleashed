package net.rai.scp_unleashed.scp005.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp005.KeyHoldable;
import net.rai.scp_unleashed.scp005.Scp005Doors;
import net.rai.scp_unleashed.scp005.Scp005Holds;
import net.rai.scp_unleashed.scp005.client.HeldDoors;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp005JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp005_hold");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        KeyHoldable Door = Scp005Doors.find(State);
        if (Door == null) {
            return;
        }
        BlockPos Anchor = Door.keyAnchor(Accessor.getPosition(), State);
        Long Until = HeldDoors.untilAt(Anchor);
        if (Until == null) {
            return;
        }
        if (Until == Scp005Holds.Forever) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp005_held_forever").withStyle(ChatFormatting.DARK_AQUA));
            return;
        }
        long Left = Math.max(0L, Until - Accessor.getLevel().getGameTime());
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp005_held", (Left + 19L) / 20L).withStyle(ChatFormatting.DARK_AQUA));
    }
}
