package net.rai.scp_unleashed.scp5001.mekhane.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record MekhaneFxPayload(int Kind, double X, double Y, double Z, float Power) implements CustomPacketPayload {
    public static final int Shock = 0;
    public static final int Sparks = 1;
    public static final int Flash = 2;
    public static final int Rumble = 3;
    public static final int Alarm = 4;
    public static final Type<MekhaneFxPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "mekhane_fx"));
    public static final StreamCodec<ByteBuf, MekhaneFxPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MekhaneFxPayload::Kind,
            ByteBufCodecs.DOUBLE, MekhaneFxPayload::X,
            ByteBufCodecs.DOUBLE, MekhaneFxPayload::Y,
            ByteBufCodecs.DOUBLE, MekhaneFxPayload::Z,
            ByteBufCodecs.FLOAT, MekhaneFxPayload::Power,
            MekhaneFxPayload::new);

    @Override
    public Type<MekhaneFxPayload> type() {
        return PayloadType;
    }
}
