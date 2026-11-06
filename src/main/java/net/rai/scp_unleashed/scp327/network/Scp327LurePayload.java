package net.rai.scp_unleashed.scp327.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp327LurePayload(boolean Active, double X, double Y, double Z, float Strength) implements CustomPacketPayload {
    public static final Type<Scp327LurePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp327_lure"));
    public static final StreamCodec<ByteBuf, Scp327LurePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp327LurePayload::Active,
            ByteBufCodecs.DOUBLE, Scp327LurePayload::X,
            ByteBufCodecs.DOUBLE, Scp327LurePayload::Y,
            ByteBufCodecs.DOUBLE, Scp327LurePayload::Z,
            ByteBufCodecs.FLOAT, Scp327LurePayload::Strength,
            Scp327LurePayload::new);

    @Override
    public Type<Scp327LurePayload> type() {
        return PayloadType;
    }
}
