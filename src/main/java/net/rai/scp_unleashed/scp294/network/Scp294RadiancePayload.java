package net.rai.scp_unleashed.scp294.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp294RadiancePayload(int Ticks) implements CustomPacketPayload {
    public static final Type<Scp294RadiancePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp294_radiance"));
    public static final StreamCodec<ByteBuf, Scp294RadiancePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp294RadiancePayload::Ticks,
            Scp294RadiancePayload::new);

    @Override
    public Type<Scp294RadiancePayload> type() {
        return PayloadType;
    }
}
