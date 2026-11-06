package net.rai.scp_unleashed.intro.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.intro.IntroRegistry;

public record IntroCuePayload(int Cue) implements CustomPacketPayload {
    public static final int Crossing = 1;
    public static final int Arrived = 2;
    public static final int Abort = 3;
    public static final int Leaving = 4;
    public static final Type<IntroCuePayload> PayloadType = new Type<>(IntroRegistry.location("intro_cue"));
    public static final StreamCodec<ByteBuf, IntroCuePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, IntroCuePayload::Cue,
            IntroCuePayload::new);

    @Override
    public Type<IntroCuePayload> type() {
        return PayloadType;
    }
}
