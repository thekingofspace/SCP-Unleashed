package net.rai.scp_unleashed.intro.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.intro.IntroRegistry;

public record IntroReadyPayload(int Token, boolean Skip) implements CustomPacketPayload {
    public static final Type<IntroReadyPayload> PayloadType = new Type<>(IntroRegistry.location("intro_ready"));
    public static final StreamCodec<ByteBuf, IntroReadyPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, IntroReadyPayload::Token,
            ByteBufCodecs.BOOL, IntroReadyPayload::Skip,
            IntroReadyPayload::new);

    @Override
    public Type<IntroReadyPayload> type() {
        return PayloadType;
    }
}
