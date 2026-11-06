package net.rai.scp_unleashed.gateway.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record GatewayEnterPayload(BlockPos Gate) implements CustomPacketPayload {
    public static final Type<GatewayEnterPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gateway_enter"));
    public static final StreamCodec<ByteBuf, GatewayEnterPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GatewayEnterPayload::Gate,
            GatewayEnterPayload::new);

    @Override
    public Type<GatewayEnterPayload> type() {
        return PayloadType;
    }
}
