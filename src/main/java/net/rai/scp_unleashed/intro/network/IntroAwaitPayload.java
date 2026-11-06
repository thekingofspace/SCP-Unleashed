package net.rai.scp_unleashed.intro.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.intro.IntroRegistry;

public record IntroAwaitPayload(int Token, int Stage) implements CustomPacketPayload {
    public static final Type<IntroAwaitPayload> PayloadType = new Type<>(IntroRegistry.location("intro_await"));
    public static final StreamCodec<ByteBuf, IntroAwaitPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, IntroAwaitPayload::Token,
            ByteBufCodecs.VAR_INT, IntroAwaitPayload::Stage,
            IntroAwaitPayload::new);

    @Override
    public Type<IntroAwaitPayload> type() {
        return PayloadType;
    }
}
