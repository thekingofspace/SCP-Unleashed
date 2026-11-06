package net.rai.scp_unleashed.scp066.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp066ChordPayload(double X, double Y, double Z, int Voicing) implements CustomPacketPayload {
    public static final Type<Scp066ChordPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp066_chord"));
    public static final StreamCodec<ByteBuf, Scp066ChordPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Scp066ChordPayload::X,
            ByteBufCodecs.DOUBLE, Scp066ChordPayload::Y,
            ByteBufCodecs.DOUBLE, Scp066ChordPayload::Z,
            ByteBufCodecs.VAR_INT, Scp066ChordPayload::Voicing,
            Scp066ChordPayload::new);

    @Override
    public Type<Scp066ChordPayload> type() {
        return PayloadType;
    }
}
