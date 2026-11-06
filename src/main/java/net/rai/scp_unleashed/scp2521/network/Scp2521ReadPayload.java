package net.rai.scp_unleashed.scp2521.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp2521.Scp2521Registry;

public record Scp2521ReadPayload() implements CustomPacketPayload {
    public static final Scp2521ReadPayload Instance = new Scp2521ReadPayload();
    public static final Type<Scp2521ReadPayload> PayloadType = new Type<>(Scp2521Registry.location("scp2521_read"));
    public static final StreamCodec<ByteBuf, Scp2521ReadPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
