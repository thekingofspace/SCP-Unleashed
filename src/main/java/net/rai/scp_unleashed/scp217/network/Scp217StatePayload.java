package net.rai.scp_unleashed.scp217.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp217StatePayload(int EntityId, int State) implements CustomPacketPayload {
    public static final Type<Scp217StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217_state"));
    public static final StreamCodec<ByteBuf, Scp217StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp217StatePayload::EntityId,
            ByteBufCodecs.VAR_INT, Scp217StatePayload::State,
            Scp217StatePayload::new);

    public static final int WindingBit = 1;
    public static final int PerfectBit = 2;
    public static final int StageShift = 2;

    public static int encode(int Stage, boolean Perfect, boolean Winding) {
        return Stage << StageShift | (Perfect ? PerfectBit : 0) | (Winding ? WindingBit : 0);
    }

    public int stage() {
        return State >> StageShift;
    }

    public boolean perfect() {
        return (State & PerfectBit) != 0;
    }

    public boolean winding() {
        return (State & WindingBit) != 0;
    }

    @Override
    public Type<Scp217StatePayload> type() {
        return PayloadType;
    }
}
