package net.rai.scp_unleashed.wire;

import net.minecraft.world.level.block.state.BlockState;

public interface PowerSwitch {
    boolean isClosed(BlockState State);

    boolean tripsOnOverload();

    default boolean isSensor() {
        return false;
    }
}
