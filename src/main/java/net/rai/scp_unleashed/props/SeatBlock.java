package net.rai.scp_unleashed.props;

import net.minecraft.world.level.block.state.BlockState;

public interface SeatBlock {
    boolean seats(BlockState State);

    default double seatHeight(BlockState State) {
        return PropSeatEntity.SeatHeight;
    }
}
