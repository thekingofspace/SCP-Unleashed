package net.rai.scp_unleashed.scp5001.event.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;

public record Scp5001BurstPayload(double X, double Y, double Z, float Radius) implements CustomPacketPayload {
    public static final Type<Scp5001BurstPayload> PayloadType = new Type<>(Scp5001EventRegistry.location("scp5001_event_burst"));
    public static final StreamCodec<ByteBuf, Scp5001BurstPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Scp5001BurstPayload::X,
            ByteBufCodecs.DOUBLE, Scp5001BurstPayload::Y,
            ByteBufCodecs.DOUBLE, Scp5001BurstPayload::Z,
            ByteBufCodecs.FLOAT, Scp5001BurstPayload::Radius,
            Scp5001BurstPayload::new);

    @Override
    public Type<Scp5001BurstPayload> type() {
        return PayloadType;
    }
}
