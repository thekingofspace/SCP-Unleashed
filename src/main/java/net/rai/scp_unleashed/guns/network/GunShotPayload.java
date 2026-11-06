package net.rai.scp_unleashed.guns.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public record GunShotPayload(int ShooterId, int Kind, Vec3 Origin, List<GunImpact> Impacts) implements CustomPacketPayload {
    public static final int MaxImpacts = 64;
    public static final Type<GunShotPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_shot"));
    public static final StreamCodec<ByteBuf, Vec3> PointCodec = StreamCodec.of((Buffer, Point) -> {
        Buffer.writeDouble(Point.x);
        Buffer.writeDouble(Point.y);
        Buffer.writeDouble(Point.z);
    }, Buffer -> new Vec3(Buffer.readDouble(), Buffer.readDouble(), Buffer.readDouble()));
    public static final StreamCodec<ByteBuf, GunShotPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GunShotPayload::ShooterId,
            ByteBufCodecs.VAR_INT, GunShotPayload::Kind,
            PointCodec, GunShotPayload::Origin,
            GunImpact.ImpactCodec.apply(ByteBufCodecs.list(MaxImpacts)), GunShotPayload::Impacts,
            GunShotPayload::new);

    @Override
    public Type<GunShotPayload> type() {
        return PayloadType;
    }
}
