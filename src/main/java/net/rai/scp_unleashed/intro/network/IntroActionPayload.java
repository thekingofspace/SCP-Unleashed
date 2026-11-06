package net.rai.scp_unleashed.intro.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.intro.IntroRegistry;

public record IntroActionPayload(int Token, int Action) implements CustomPacketPayload {
    public static final int DialogueDone = 1;
    public static final int Skip = 2;
    public static final int Stand = 3;
    public static final Type<IntroActionPayload> PayloadType = new Type<>(IntroRegistry.location("intro_action"));
    public static final StreamCodec<ByteBuf, IntroActionPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, IntroActionPayload::Token,
            ByteBufCodecs.VAR_INT, IntroActionPayload::Action,
            IntroActionPayload::new);

    @Override
    public Type<IntroActionPayload> type() {
        return PayloadType;
    }
}
