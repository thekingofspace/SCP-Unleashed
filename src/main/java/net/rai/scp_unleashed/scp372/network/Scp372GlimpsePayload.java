package net.rai.scp_unleashed.scp372.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp372.Scp372Registry;

public record Scp372GlimpsePayload(int Seed, boolean Dim) implements CustomPacketPayload {
    public static final Type<Scp372GlimpsePayload> PayloadType = new Type<>(Scp372Registry.location("scp372_glimpse"));
    public static final StreamCodec<ByteBuf, Scp372GlimpsePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.INT, Scp372GlimpsePayload::Seed,
            ByteBufCodecs.BOOL, Scp372GlimpsePayload::Dim,
            Scp372GlimpsePayload::new);

    @Override
    public Type<Scp372GlimpsePayload> type() {
        return PayloadType;
    }
}
