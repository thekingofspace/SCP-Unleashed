package net.rai.scp_unleashed.scp4666;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Set;

public final class Scp4666Boundary {
    public static final int SearchRadius = 3;

    private Scp4666Boundary() {
    }

    public static boolean beyond(Scp4666Map Layout, Vec3 Point) {
        return Layout.distanceFromCentre(Point) > Scp4666World.TurnRadius;
    }

    public static Vec3 mirrored(Scp4666Map Layout, Vec3 Point) {
        double Cx = Layout.Centre.getX() + 0.5;
        double Cz = Layout.Centre.getZ() + 0.5;
        double Dx = Point.x - Cx;
        double Dz = Point.z - Cz;
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Length < 1.0E-4) {
            return Point;
        }
        double Radius = Scp4666World.TurnRadius - Scp4666World.TurnInset;
        return new Vec3(Cx - Dx / Length * Radius, Point.y, Cz - Dz / Length * Radius);
    }

    public static boolean check(ServerLevel World, Scp4666Map Layout, ServerPlayer User) {
        if (User.isSpectator() || !User.isAlive() || User.isPassenger() || !beyond(Layout, User.position())) {
            return false;
        }
        Vec3 Target = settle(World, mirrored(Layout, User.position()), User.getY());
        Set<RelativeMovement> Keep = EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT);
        Vec3 Motion = User.getDeltaMovement();
        User.teleportTo(World, Target.x, Target.y, Target.z, Keep, User.getYRot(), User.getXRot());
        User.setDeltaMovement(Motion);
        User.hurtMarked = true;
        User.resetFallDistance();
        return true;
    }

    public static Vec3 settle(ServerLevel World, Vec3 Wanted, double Y) {
        BlockPos Centre = BlockPos.containing(Wanted.x, Y, Wanted.z);
        for (int Radius = 0; Radius <= SearchRadius; Radius++) {
            for (int Dx = -Radius; Dx <= Radius; Dx++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Radius) {
                        continue;
                    }
                    for (int Step = 0; Step <= 16; Step++) {
                        double X = Radius == 0 ? Wanted.x : Centre.getX() + Dx + 0.5;
                        double Z = Radius == 0 ? Wanted.z : Centre.getZ() + Dz + 0.5;
                        double Height = Math.floor(Y) + Step * 0.125;
                        AABB Box = EntityType.PLAYER.getDimensions().makeBoundingBox(new Vec3(X, Height, Z));
                        if (World.noCollision(Box) && !World.noCollision(Box.move(0.0, -0.13, 0.0))) {
                            return new Vec3(X, Height, Z);
                        }
                    }
                }
            }
        }
        return new Vec3(Wanted.x, Y + 1.0, Wanted.z);
    }
}
