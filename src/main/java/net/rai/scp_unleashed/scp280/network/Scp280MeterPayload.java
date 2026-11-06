package net.rai.scp_unleashed.scp280.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp280.Scp280Registry;

public record Scp280MeterPayload(float Meter, byte Cue) implements CustomPacketPayload {
    public static final byte None = 0;
    public static final byte Reveal = 1;
    public static final byte Cleared = 2;
    public static final Type<Scp280MeterPayload> PayloadType = new Type<>(Scp280Registry.location("scp280_meter"));
    public static final StreamCodec<ByteBuf, Scp280MeterPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Scp280MeterPayload::Meter,
            ByteBufCodecs.BYTE, Scp280MeterPayload::Cue,
            Scp280MeterPayload::new);

    @Override
    public Type<Scp280MeterPayload> type() {
        return PayloadType;
    }
}
