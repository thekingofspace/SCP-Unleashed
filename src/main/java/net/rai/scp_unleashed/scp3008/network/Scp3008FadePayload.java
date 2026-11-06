package net.rai.scp_unleashed.scp3008.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp3008FadePayload(int Phase, int Ticks) implements CustomPacketPayload {
    public static final int FadeIn = 0;
    public static final int FadeOut = 1;
    public static final int Cancel = 2;
    public static final Type<Scp3008FadePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp3008_fade"));
    public static final StreamCodec<ByteBuf, Scp3008FadePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp3008FadePayload::Phase,
            ByteBufCodecs.VAR_INT, Scp3008FadePayload::Ticks,
            Scp3008FadePayload::new);

    @Override
    public Type<Scp3008FadePayload> type() {
        return PayloadType;
    }
}
