package net.rai.scp_unleashed.ultralaser.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.network.GunImpact;
import net.rai.scp_unleashed.guns.network.GunShotPayload;

import java.util.List;

public record UltralaserBlastPayload(int ShooterId, Vec3 Origin, Vec3 End, List<GunImpact> Impacts) implements CustomPacketPayload {
    public static final int MaxImpacts = 64;
    public static final Type<UltralaserBlastPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ultralaser_blast"));
    public static final StreamCodec<ByteBuf, UltralaserBlastPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UltralaserBlastPayload::ShooterId,
            GunShotPayload.PointCodec, UltralaserBlastPayload::Origin,
            GunShotPayload.PointCodec, UltralaserBlastPayload::End,
            GunImpact.ImpactCodec.apply(ByteBufCodecs.list(MaxImpacts)), UltralaserBlastPayload::Impacts,
            UltralaserBlastPayload::new);

    @Override
    public Type<UltralaserBlastPayload> type() {
        return PayloadType;
    }
}
