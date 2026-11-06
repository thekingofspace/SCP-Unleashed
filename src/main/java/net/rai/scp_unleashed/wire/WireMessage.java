package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record WireMessage(String Channel, CompoundTag Payload, BlockPos Source, boolean Routed) {
    public WireMessage(String Channel, CompoundTag Payload, BlockPos Source) {
        this(Channel, Payload, Source, false);
    }
}
