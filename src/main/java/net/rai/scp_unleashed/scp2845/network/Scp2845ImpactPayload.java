package net.rai.scp_unleashed.scp2845.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp2845ImpactPayload(BlockPos Centre, float Radius) implements CustomPacketPayload {
    public static final Type<Scp2845ImpactPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp2845_impact"));
    public static final StreamCodec<ByteBuf, Scp2845ImpactPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Scp2845ImpactPayload::Centre,
            ByteBufCodecs.FLOAT, Scp2845ImpactPayload::Radius,
            Scp2845ImpactPayload::new);

    @Override
    public Type<Scp2845ImpactPayload> type() {
        return PayloadType;
    }
}
