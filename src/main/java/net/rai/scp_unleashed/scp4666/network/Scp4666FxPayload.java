package net.rai.scp_unleashed.scp4666.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp4666FxPayload(int Kind, double X, double Y, double Z, int Value) implements CustomPacketPayload {
    public static final int Thud = 0;
    public static final int Sack = 1;
    public static final int Wake = 2;
    public static final int Breath = 3;
    public static final int Snow = 4;
    public static final int Hold = 5;
    public static final int Bury = 6;
    public static final int Probe = 7;
    public static final Type<Scp4666FxPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp4666_fx"));
    public static final StreamCodec<ByteBuf, Scp4666FxPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp4666FxPayload::Kind,
            ByteBufCodecs.DOUBLE, Scp4666FxPayload::X,
            ByteBufCodecs.DOUBLE, Scp4666FxPayload::Y,
            ByteBufCodecs.DOUBLE, Scp4666FxPayload::Z,
            ByteBufCodecs.VAR_INT, Scp4666FxPayload::Value,
            Scp4666FxPayload::new);

    @Override
    public Type<Scp4666FxPayload> type() {
        return PayloadType;
    }
}
