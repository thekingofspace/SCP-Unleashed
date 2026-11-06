package net.rai.scp_unleashed.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class StorageGeometry {
    public static final float PivotX = 14.0f;
    public static final float PivotZ = 0.5f;
    public static final float CavityLeft = 2.0f;
    public static final float CavityRight = 14.0f;
    public static final float CavityFloor = 4.0f;
    public static final float CavityTop = 14.0f;
    public static final float CavityTopTall = 30.0f;
    public static final float CavityFront = 3.0f;
    public static final float CavityBack = 15.0f;
    public static final float CentreX = 8.0f;
    public static final float CentreZ = 9.0f;
    public static final float FrontFace = 1.0f;

    private StorageGeometry() {
    }

    public static float cavityTop(boolean Tall) {
        return Tall ? CavityTopTall : CavityTop;
    }

    public static float yaw(Direction Facing) {
        return 180.0f - Facing.toYRot();
    }

    public static Vec3 point(BlockPos Pos, Direction Facing, double X, double Y, double Z) {
        double Px = X - 8.0;
        double Pz = Z - 8.0;
        double Wx;
        double Wz;
        switch (Facing) {
            case EAST -> {
                Wx = -Pz;
                Wz = Px;
            }
            case SOUTH -> {
                Wx = -Px;
                Wz = -Pz;
            }
            case WEST -> {
                Wx = Pz;
                Wz = -Px;
            }
            default -> {
                Wx = Px;
                Wz = Pz;
            }
        }
        return new Vec3(Pos.getX() + (Wx + 8.0) / 16.0, Pos.getY() + Y / 16.0, Pos.getZ() + (Wz + 8.0) / 16.0);
    }

    public static Vec3 motion(Direction Facing, double X, double Y, double Z) {
        return switch (Facing) {
            case EAST -> new Vec3(-Z, Y, X);
            case SOUTH -> new Vec3(-X, Y, -Z);
            case WEST -> new Vec3(Z, Y, -X);
            default -> new Vec3(X, Y, Z);
        };
    }
}
