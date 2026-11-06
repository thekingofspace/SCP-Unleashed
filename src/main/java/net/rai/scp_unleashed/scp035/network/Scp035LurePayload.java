package net.rai.scp_unleashed.scp035.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp035LurePayload(boolean Active, double X, double Y, double Z, float Strength, float Compel) implements CustomPacketPayload {
    public static final Type<Scp035LurePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp035_lure"));
    public static final StreamCodec<ByteBuf, Scp035LurePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp035LurePayload::Active,
            ByteBufCodecs.DOUBLE, Scp035LurePayload::X,
            ByteBufCodecs.DOUBLE, Scp035LurePayload::Y,
            ByteBufCodecs.DOUBLE, Scp035LurePayload::Z,
            ByteBufCodecs.FLOAT, Scp035LurePayload::Strength,
            ByteBufCodecs.FLOAT, Scp035LurePayload::Compel,
            Scp035LurePayload::new);

    @Override
    public Type<Scp035LurePayload> type() {
        return PayloadType;
    }
}
