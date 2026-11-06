package net.rai.scp_unleashed.scp002.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ImpactPayload(BlockPos Centre, float Radius) implements CustomPacketPayload {
    public static final Type<ImpactPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp002_impact"));
    public static final StreamCodec<ByteBuf, ImpactPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ImpactPayload::Centre,
            ByteBufCodecs.FLOAT, ImpactPayload::Radius,
            ImpactPayload::new);

    @Override
    public Type<ImpactPayload> type() {
        return PayloadType;
    }
}
