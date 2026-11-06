package net.rai.scp_unleashed.furnishing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public interface SeatHost {
    double seatHeight(BlockState State);

    float seatYaw(BlockState State, BlockPos Pos, Player User);
}
