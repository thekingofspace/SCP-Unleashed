package net.rai.scp_unleashed.orbital.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record OrbitalStatusPayload(int Mask, long ReadyAt) implements CustomPacketPayload {
    public static final Type<OrbitalStatusPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "orbital_status"));
    public static final StreamCodec<ByteBuf, OrbitalStatusPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OrbitalStatusPayload::Mask,
            ByteBufCodecs.VAR_LONG, OrbitalStatusPayload::ReadyAt,
            OrbitalStatusPayload::new);

    @Override
    public Type<OrbitalStatusPayload> type() {
        return PayloadType;
    }
}
