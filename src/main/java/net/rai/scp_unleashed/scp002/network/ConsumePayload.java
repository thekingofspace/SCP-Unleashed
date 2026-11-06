package net.rai.scp_unleashed.scp002.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ConsumePayload(float Intensity) implements CustomPacketPayload {
    public static final Type<ConsumePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp002_consume"));
    public static final StreamCodec<ByteBuf, ConsumePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ConsumePayload::Intensity,
            ConsumePayload::new);

    @Override
    public Type<ConsumePayload> type() {
        return PayloadType;
    }
}
