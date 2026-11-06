package net.rai.scp_unleashed.gateway.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record GatewayPromptPayload(BlockPos Gate, ResourceLocation Destination) implements CustomPacketPayload {
    public static final Type<GatewayPromptPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gateway_prompt"));
    public static final StreamCodec<ByteBuf, GatewayPromptPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GatewayPromptPayload::Gate,
            ResourceLocation.STREAM_CODEC, GatewayPromptPayload::Destination,
            GatewayPromptPayload::new);

    @Override
    public Type<GatewayPromptPayload> type() {
        return PayloadType;
    }
}
