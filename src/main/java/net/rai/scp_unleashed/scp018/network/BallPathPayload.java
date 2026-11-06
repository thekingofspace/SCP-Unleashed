package net.rai.scp_unleashed.scp018.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp018.BallSegment;

import java.util.List;

public record BallPathPayload(int EntityId, List<BallSegment> Segments) implements CustomPacketPayload {
    public static final int MaxSegments = 256;
    public static final Type<BallPathPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp018_path"));
    public static final StreamCodec<ByteBuf, BallPathPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BallPathPayload::EntityId,
            BallSegment.SegmentCodec.apply(ByteBufCodecs.list(MaxSegments)), BallPathPayload::Segments,
            BallPathPayload::new);

    @Override
    public Type<BallPathPayload> type() {
        return PayloadType;
    }
}
