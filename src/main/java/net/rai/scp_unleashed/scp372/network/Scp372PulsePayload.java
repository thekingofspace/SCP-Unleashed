package net.rai.scp_unleashed.scp372.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp372.Scp372Registry;

public record Scp372PulsePayload(float Strength) implements CustomPacketPayload {
    public static final Type<Scp372PulsePayload> PayloadType = new Type<>(Scp372Registry.location("scp372_pulse"));
    public static final StreamCodec<ByteBuf, Scp372PulsePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Scp372PulsePayload::Strength,
            Scp372PulsePayload::new);

    @Override
    public Type<Scp372PulsePayload> type() {
        return PayloadType;
    }
}
