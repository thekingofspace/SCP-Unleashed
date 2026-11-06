package net.rai.scp_unleashed.scp163.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp163ImpactPayload(BlockPos Centre, float Radius) implements CustomPacketPayload {
    public static final Type<Scp163ImpactPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp163_impact"));
    public static final StreamCodec<ByteBuf, Scp163ImpactPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Scp163ImpactPayload::Centre,
            ByteBufCodecs.FLOAT, Scp163ImpactPayload::Radius,
            Scp163ImpactPayload::new);

    @Override
    public Type<Scp163ImpactPayload> type() {
        return PayloadType;
    }
}
