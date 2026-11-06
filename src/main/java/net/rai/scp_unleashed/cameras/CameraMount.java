package net.rai.scp_unleashed.cameras;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

public record CameraMount(boolean Ceiling, Direction Facing) {
    public static final float PanLimit = 80.0f;
    public static final float TiltLimit = 45.0f;
    public static final float WallPitch = 15.0f;
    public static final float CeilingPitch = 35.0f;
    public static final float LowestPitch = -60.0f;
    public static final float HighestPitch = 88.0f;
    public static final double PivotX = 8.0 / 16.0;
    public static final double PivotY = 10.0 / 16.0;
    public static final double PivotZ = 10.0 / 16.0;
    public static final double LensUp = 2.0 / 16.0;
    public static final double LensAhead = 7.0 / 16.0 + 0.12;
    public static final CameraMount Fallback = new CameraMount(false, Direction.NORTH);

    public CameraMount {
        Facing = Facing.getAxis().isHorizontal() ? Facing : Direction.NORTH;
    }

    public static CameraMount of(BlockState State) {
        if (!State.hasProperty(CameraBlock.Face) || !State.hasProperty(CameraBlock.Facing)) {
            return Fallback;
        }
        return new CameraMount(State.getValue(CameraBlock.Face) == AttachFace.CEILING, State.getValue(CameraBlock.Facing));
    }

    public float baseYaw() {
        return Facing.toYRot();
    }

    public float basePitch() {
        return Ceiling ? CeilingPitch : WallPitch;
    }

    public static float clampPan(float Pan) {
        return Mth.clamp(Mth.wrapDegrees(Pan), -PanLimit, PanLimit);
    }

    public float lowestTilt() {
        return Math.max(-TiltLimit, LowestPitch - basePitch());
    }

    public float highestTilt() {
        return Math.min(TiltLimit, HighestPitch - basePitch());
    }

    public float clampTilt(float Tilt) {
        return Mth.clamp(Tilt, lowestTilt(), highestTilt());
    }

    public float yaw(float Pan) {
        return baseYaw() + Pan;
    }

    public float pitch(float Tilt) {
        return basePitch() + Tilt;
    }

    public float mountTurn() {
        return 180.0f - baseYaw();
    }

    public static Vec3 turnY(Vec3 Point, float Degrees) {
        double Angle = Math.toRadians(Degrees);
        double Cos = Math.cos(Angle);
        double Sin = Math.sin(Angle);
        return new Vec3(Point.x * Cos + Point.z * Sin, Point.y, -Point.x * Sin + Point.z * Cos);
    }

    public static Vec3 turnX(Vec3 Point, float Degrees) {
        double Angle = Math.toRadians(Degrees);
        double Cos = Math.cos(Angle);
        double Sin = Math.sin(Angle);
        return new Vec3(Point.x, Point.y * Cos - Point.z * Sin, Point.y * Sin + Point.z * Cos);
    }

    public Vec3 pivotOffset() {
        Vec3 Local = new Vec3(PivotX - 0.5, PivotY, PivotZ - 0.5);
        Vec3 Turned = turnY(Local, mountTurn());
        return new Vec3(Turned.x + 0.5, Turned.y, Turned.z + 0.5);
    }

    public Vec3 pivot(BlockPos Pos) {
        return Vec3.atLowerCornerOf(Pos).add(pivotOffset());
    }

    public static Vec3 direction(float Yaw, float Pitch) {
        return Vec3.directionFromRotation(Pitch, Yaw);
    }

    public static Vec3 lensOffset(float Yaw, float Pitch) {
        Vec3 Local = new Vec3(0.0, LensUp, -LensAhead);
        return turnY(turnX(Local, -Pitch), 180.0f - Yaw);
    }

    public Vec3 eye(BlockPos Pos, float Pan, float Tilt) {
        return pivot(Pos).add(lensOffset(yaw(Pan), pitch(Tilt)));
    }

    public Vec3 look(float Pan, float Tilt) {
        return direction(yaw(Pan), pitch(Tilt));
    }
}
