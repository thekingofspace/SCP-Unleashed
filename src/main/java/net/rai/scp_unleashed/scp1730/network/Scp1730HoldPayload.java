package net.rai.scp_unleashed.scp1730.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp1730HoldPayload(boolean Active, double X, double Y, double Z, float Pull) implements CustomPacketPayload {
    public static final Type<Scp1730HoldPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp1730_hold"));
    public static final StreamCodec<ByteBuf, Scp1730HoldPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp1730HoldPayload::Active,
            ByteBufCodecs.DOUBLE, Scp1730HoldPayload::X,
            ByteBufCodecs.DOUBLE, Scp1730HoldPayload::Y,
            ByteBufCodecs.DOUBLE, Scp1730HoldPayload::Z,
            ByteBufCodecs.FLOAT, Scp1730HoldPayload::Pull,
            Scp1730HoldPayload::new);

    public static Scp1730HoldPayload released() {
        return new Scp1730HoldPayload(false, 0.0, 0.0, 0.0, 0.0f);
    }

    @Override
    public Type<Scp1730HoldPayload> type() {
        return PayloadType;
    }
}
