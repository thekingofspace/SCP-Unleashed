package net.rai.scp_unleashed.scp5001.event.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;

public record Scp5001OpenPayload(int Screen, BlockPos Pos, BlockPos Linked, int Node) implements CustomPacketPayload {
    public static final int MainScreen = 0;
    public static final int GeneratorScreen = 1;
    public static final Type<Scp5001OpenPayload> PayloadType = new Type<>(Scp5001EventRegistry.location("scp5001_event_open"));
    public static final StreamCodec<ByteBuf, Scp5001OpenPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp5001OpenPayload::Screen,
            BlockPos.STREAM_CODEC, Scp5001OpenPayload::Pos,
            BlockPos.STREAM_CODEC, Scp5001OpenPayload::Linked,
            ByteBufCodecs.VAR_INT, Scp5001OpenPayload::Node,
            Scp5001OpenPayload::new);

    @Override
    public Type<Scp5001OpenPayload> type() {
        return PayloadType;
    }
}
