package net.rai.scp_unleashed.scp1730.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp1730PulsePayload(double X, double Y, double Z, float Radius) implements CustomPacketPayload {
    public static final Type<Scp1730PulsePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp1730_pulse"));
    public static final StreamCodec<ByteBuf, Scp1730PulsePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Scp1730PulsePayload::X,
            ByteBufCodecs.DOUBLE, Scp1730PulsePayload::Y,
            ByteBufCodecs.DOUBLE, Scp1730PulsePayload::Z,
            ByteBufCodecs.FLOAT, Scp1730PulsePayload::Radius,
            Scp1730PulsePayload::new);

    @Override
    public Type<Scp1730PulsePayload> type() {
        return PayloadType;
    }
}
