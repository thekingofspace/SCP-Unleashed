package net.rai.scp_unleashed.guns.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record GunImpact(float OffsetX, float OffsetY, float OffsetZ, int Type, int Face) {
    public static final int Miss = 0;
    public static final int Surface = 1;
    public static final int Wound = 2;
    public static final int Kill = 3;
    public static final StreamCodec<ByteBuf, GunImpact> ImpactCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, GunImpact::OffsetX,
            ByteBufCodecs.FLOAT, GunImpact::OffsetY,
            ByteBufCodecs.FLOAT, GunImpact::OffsetZ,
            ByteBufCodecs.VAR_INT, GunImpact::Type,
            ByteBufCodecs.VAR_INT, GunImpact::Face,
            GunImpact::new);

    public static GunImpact at(Vec3 Origin, Vec3 Point, int Type, Direction Side) {
        return new GunImpact((float) (Point.x - Origin.x), (float) (Point.y - Origin.y), (float) (Point.z - Origin.z), Type, Side.get3DDataValue());
    }

    public Vec3 point(Vec3 Origin) {
        return Origin.add(OffsetX, OffsetY, OffsetZ);
    }

    public Direction side() {
        return Direction.from3DDataValue(Face);
    }

    public boolean isEntity() {
        return Type == Wound || Type == Kill;
    }

    public GunImpact withType(int NewType) {
        return new GunImpact(OffsetX, OffsetY, OffsetZ, NewType, Face);
    }
}
