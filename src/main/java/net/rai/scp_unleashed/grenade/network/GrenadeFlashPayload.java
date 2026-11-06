package net.rai.scp_unleashed.grenade.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record GrenadeFlashPayload(double X, double Y, double Z, float White, float Ring) implements CustomPacketPayload {
    public static final Type<GrenadeFlashPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "grenade_flash"));
    public static final StreamCodec<ByteBuf, GrenadeFlashPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, GrenadeFlashPayload::X,
            ByteBufCodecs.DOUBLE, GrenadeFlashPayload::Y,
            ByteBufCodecs.DOUBLE, GrenadeFlashPayload::Z,
            ByteBufCodecs.FLOAT, GrenadeFlashPayload::White,
            ByteBufCodecs.FLOAT, GrenadeFlashPayload::Ring,
            GrenadeFlashPayload::new);

    @Override
    public Type<GrenadeFlashPayload> type() {
        return PayloadType;
    }
}
