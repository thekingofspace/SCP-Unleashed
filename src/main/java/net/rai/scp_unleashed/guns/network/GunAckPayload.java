package net.rai.scp_unleashed.guns.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record GunAckPayload(int Sequence, int Ammo) implements CustomPacketPayload {
    public static final Type<GunAckPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_ack"));
    public static final StreamCodec<ByteBuf, GunAckPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GunAckPayload::Sequence,
            ByteBufCodecs.INT, GunAckPayload::Ammo,
            GunAckPayload::new);

    @Override
    public Type<GunAckPayload> type() {
        return PayloadType;
    }
}
