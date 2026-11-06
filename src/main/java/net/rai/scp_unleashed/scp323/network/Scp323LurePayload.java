package net.rai.scp_unleashed.scp323.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp323LurePayload(boolean Active, double X, double Y, double Z, float Strength, float Compel) implements CustomPacketPayload {
    public static final Type<Scp323LurePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp323_lure"));
    public static final StreamCodec<ByteBuf, Scp323LurePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp323LurePayload::Active,
            ByteBufCodecs.DOUBLE, Scp323LurePayload::X,
            ByteBufCodecs.DOUBLE, Scp323LurePayload::Y,
            ByteBufCodecs.DOUBLE, Scp323LurePayload::Z,
            ByteBufCodecs.FLOAT, Scp323LurePayload::Strength,
            ByteBufCodecs.FLOAT, Scp323LurePayload::Compel,
            Scp323LurePayload::new);

    @Override
    public Type<Scp323LurePayload> type() {
        return PayloadType;
    }
}
