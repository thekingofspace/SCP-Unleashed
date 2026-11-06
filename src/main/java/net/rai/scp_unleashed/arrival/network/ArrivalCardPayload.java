package net.rai.scp_unleashed.arrival.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.arrival.ArrivalRegistry;

public record ArrivalCardPayload(ResourceLocation Dimension) implements CustomPacketPayload {
    public static final Type<ArrivalCardPayload> PayloadType = new Type<>(ArrivalRegistry.location("arrival_card"));
    public static final StreamCodec<ByteBuf, ArrivalCardPayload> PayloadCodec = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ArrivalCardPayload::Dimension,
            ArrivalCardPayload::new);

    @Override
    public Type<ArrivalCardPayload> type() {
        return PayloadType;
    }
}
