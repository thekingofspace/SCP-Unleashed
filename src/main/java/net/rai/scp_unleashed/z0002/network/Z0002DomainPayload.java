package net.rai.scp_unleashed.z0002.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Z0002DomainPayload(boolean Inside, double X, double Y, double Z, int Radius) implements CustomPacketPayload {
    public static final Type<Z0002DomainPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0002_domain"));
    public static final StreamCodec<ByteBuf, Z0002DomainPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Z0002DomainPayload::Inside,
            ByteBufCodecs.DOUBLE, Z0002DomainPayload::X,
            ByteBufCodecs.DOUBLE, Z0002DomainPayload::Y,
            ByteBufCodecs.DOUBLE, Z0002DomainPayload::Z,
            ByteBufCodecs.VAR_INT, Z0002DomainPayload::Radius,
            Z0002DomainPayload::new);

    @Override
    public Type<Z0002DomainPayload> type() {
        return PayloadType;
    }
}
