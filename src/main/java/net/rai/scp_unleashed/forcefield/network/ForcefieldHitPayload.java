package net.rai.scp_unleashed.forcefield.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;

public record ForcefieldHitPayload(double X, double Y, double Z, boolean AlongX, float Strength) implements CustomPacketPayload {
    public static final Type<ForcefieldHitPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "forcefield_hit"));
    public static final StreamCodec<ByteBuf, ForcefieldHitPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ForcefieldHitPayload::X,
            ByteBufCodecs.DOUBLE, ForcefieldHitPayload::Y,
            ByteBufCodecs.DOUBLE, ForcefieldHitPayload::Z,
            ByteBufCodecs.BOOL, ForcefieldHitPayload::AlongX,
            ByteBufCodecs.FLOAT, ForcefieldHitPayload::Strength,
            ForcefieldHitPayload::new);

    public Vec3 point() {
        return new Vec3(X, Y, Z);
    }

    @Override
    public Type<ForcefieldHitPayload> type() {
        return PayloadType;
    }
}
