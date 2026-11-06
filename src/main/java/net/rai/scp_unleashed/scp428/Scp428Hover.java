package net.rai.scp_unleashed.scp428;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp428Hover extends MoveControl {
    public static final double Lift = 0.14;
    public static final double MaxRise = 0.16;
    public static final double MaxSink = 0.12;
    public static final double Sinking = 0.04;
    public static final double Accel = 0.16;
    public static final double Brake = 0.78;
    public static final double Arrive = 0.5;
    public static final double SpeedFactor = 1.6;
    public static final float TurnRate = 8.0f;
    public static final double StepUp = 1.1;
    public static final double Drop = 3.5;
    public static final double Ahead = 0.3;
    public static final double SwayShare = 0.12;
    public static final double DriftSpeed = 0.0015;

    private final Scp428Core Core;
    private double LastFloor = Double.NaN;

    public Scp428Hover(Scp428Core Core) {
        super(Core);
        this.Core = Core;
    }

    public double lastFloor() {
        return LastFloor;
    }

    public void halt() {
        operation = Operation.WAIT;
    }

    public static double cruise(double Attribute, double Modifier) {
        double Pace = Attribute * Modifier;
        return Pace * Pace * SpeedFactor;
    }

    @Override
    public void tick() {
        Vec3 Motion = mob.getDeltaMovement();
        double Time = mob.tickCount;
        double Phase = Core.getId() * 0.618;
        double Vx = Motion.x;
        double Vz = Motion.z;
        boolean Moving = operation == Operation.MOVE_TO;
        double Fx = 0.0;
        double Fz = 0.0;
        if (Moving) {
            double Dx = wantedX - mob.getX();
            double Dz = wantedZ - mob.getZ();
            double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
            if (Flat < 0.02) {
                operation = Operation.WAIT;
                Vx *= Brake;
                Vz *= Brake;
            } else {
                Fx = Dx / Flat;
                Fz = Dz / Flat;
                double Speed = cruise(mob.getAttributeValue(Attributes.MOVEMENT_SPEED), speedModifier);
                double Want = Math.min(Speed, Flat * Arrive);
                double Sway = Want * SwayShare * Math.sin(Time * 0.15 + Phase);
                Vx += (Fx * Want - Fz * Sway - Vx) * Accel;
                Vz += (Fz * Want + Fx * Sway - Vz) * Accel;
                float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
                mob.setYRot(rotlerp(mob.getYRot(), Yaw, TurnRate));
                mob.yBodyRot = mob.getYRot();
            }
        } else {
            operation = Operation.WAIT;
            Vx = Vx * Brake + DriftSpeed * Math.sin(Time * 0.05 + Phase);
            Vz = Vz * Brake + DriftSpeed * Math.cos(Time * 0.043 + Phase * 1.3);
            mob.setYRot(mob.getYRot() + 0.4f * (float) Math.sin(Time * 0.02 + Phase));
            mob.yBodyRot = mob.getYRot();
        }
        double Floor = floor(Fx, Fz);
        LastFloor = Floor;
        double Vy;
        if (Double.isNaN(Floor)) {
            Vy = -Sinking;
        } else {
            double Target = Floor + Core.clearance() + Core.bob(0.0f);
            Vy = Mth.clamp((Target - mob.getY()) * Lift, -MaxSink, MaxRise);
        }
        mob.setDeltaMovement(Vx, Vy, Vz);
    }

    public double floor(double Fx, double Fz) {
        Level World = mob.level();
        AABB Box = mob.getBoundingBox().deflate(0.05, 0.0, 0.05);
        double Bottom = Box.minY;
        double Here = Double.NaN;
        double[][] Samples = {{(Box.minX + Box.maxX) * 0.5, (Box.minZ + Box.maxZ) * 0.5}, {Box.minX, Box.minZ}, {Box.minX, Box.maxZ}, {Box.maxX, Box.minZ}, {Box.maxX, Box.maxZ}};
        for (double[] Sample : Samples) {
            double Found = scan(World, Sample[0], Sample[1], Bottom + 0.05, Bottom - Drop);
            if (!Double.isNaN(Found) && (Double.isNaN(Here) || Found > Here)) {
                Here = Found;
            }
        }
        if ((Fx != 0.0 || Fz != 0.0) && !Double.isNaN(Here)) {
            double Shift = (Box.maxX - Box.minX) * 0.5 + Ahead;
            double Start = Math.min(Bottom + StepUp, Box.maxY - 0.01);
            for (double[] Sample : Samples) {
                double Found = scan(World, Sample[0] + Fx * Shift, Sample[1] + Fz * Shift, Start, Bottom - Drop);
                if (!Double.isNaN(Found) && Found > Here && Found - Here <= StepUp) {
                    Here = Found;
                }
            }
        }
        return Here;
    }

    public static double scan(Level World, double X, double Z, double From, double To) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(Mth.floor(X), Mth.floor(From), Mth.floor(Z));
        while (Cursor.getY() >= Mth.floor(To) && Cursor.getY() >= World.getMinBuildHeight()) {
            BlockState State = World.getBlockState(Cursor);
            FluidState Fluid = State.getFluidState();
            if (!Fluid.isEmpty()) {
                return Cursor.getY() + Fluid.getHeight(World, Cursor);
            }
            VoxelShape Shape = State.getCollisionShape(World, Cursor);
            if (!Shape.isEmpty()) {
                double Top = Cursor.getY() + Shape.max(Direction.Axis.Y);
                if (Top <= From + 1.0e-3 || Cursor.getY() == Mth.floor(From)) {
                    return Top;
                }
            }
            Cursor.move(0, -1, 0);
        }
        return Double.NaN;
    }
}
