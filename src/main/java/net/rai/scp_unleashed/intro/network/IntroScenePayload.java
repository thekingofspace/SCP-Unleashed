package net.rai.scp_unleashed.intro.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.intro.IntroRegistry;

public record IntroScenePayload(int Token, int Scene, int EntityId, BlockPos Origin) implements CustomPacketPayload {
    public static final Type<IntroScenePayload> PayloadType = new Type<>(IntroRegistry.location("intro_scene"));
    public static final StreamCodec<ByteBuf, IntroScenePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, IntroScenePayload::Token,
            ByteBufCodecs.VAR_INT, IntroScenePayload::Scene,
            ByteBufCodecs.VAR_INT, IntroScenePayload::EntityId,
            BlockPos.STREAM_CODEC, IntroScenePayload::Origin,
            IntroScenePayload::new);

    @Override
    public Type<IntroScenePayload> type() {
        return PayloadType;
    }
}
