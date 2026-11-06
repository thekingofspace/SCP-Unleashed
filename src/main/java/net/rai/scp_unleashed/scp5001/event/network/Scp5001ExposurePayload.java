package net.rai.scp_unleashed.scp5001.event.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;

public record Scp5001ExposurePayload(int Stacks, int Max, boolean Pulse) implements CustomPacketPayload {
    public static final Type<Scp5001ExposurePayload> PayloadType = new Type<>(Scp5001EventRegistry.location("scp5001_event_exposure"));
    public static final StreamCodec<ByteBuf, Scp5001ExposurePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp5001ExposurePayload::Stacks,
            ByteBufCodecs.VAR_INT, Scp5001ExposurePayload::Max,
            ByteBufCodecs.BOOL, Scp5001ExposurePayload::Pulse,
            Scp5001ExposurePayload::new);

    @Override
    public Type<Scp5001ExposurePayload> type() {
        return PayloadType;
    }
}
