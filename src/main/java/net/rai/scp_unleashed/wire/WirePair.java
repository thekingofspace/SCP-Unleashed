package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;

public record WirePair(BlockPos Low, BlockPos High) {
    public static WirePair of(BlockPos First, BlockPos Second) {
        return First.asLong() <= Second.asLong() ? new WirePair(First.immutable(), Second.immutable()) : new WirePair(Second.immutable(), First.immutable());
    }
}
