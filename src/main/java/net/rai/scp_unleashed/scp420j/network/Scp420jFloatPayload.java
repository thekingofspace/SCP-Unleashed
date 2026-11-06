package net.rai.scp_unleashed.scp420j.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp420jFloatPayload(int Session, int Elapsed, double BaseY, boolean Descending) implements CustomPacketPayload {
    public static final Type<Scp420jFloatPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp420j_float"));
    public static final Scp420jFloatPayload Cleared = new Scp420jFloatPayload(0, 0, 0.0, false);
    public static final StreamCodec<ByteBuf, Scp420jFloatPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp420jFloatPayload::Session,
            ByteBufCodecs.VAR_INT, Scp420jFloatPayload::Elapsed,
            ByteBufCodecs.DOUBLE, Scp420jFloatPayload::BaseY,
            ByteBufCodecs.BOOL, Scp420jFloatPayload::Descending,
            Scp420jFloatPayload::new);

    public boolean cleared() {
        return Session == 0;
    }

    @Override
    public Type<Scp420jFloatPayload> type() {
        return PayloadType;
    }
}
