package net.rai.scp_unleashed.scp005;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface KeyHoldable {
    BlockPos keyAnchor(BlockPos Pos, BlockState State);

    boolean holdOpen(ServerLevel World, BlockPos Anchor, @Nullable Player User);

    void releaseHold(ServerLevel World, BlockPos Anchor);

    boolean isHeldOpen(BlockGetter World, BlockPos Anchor);
}
