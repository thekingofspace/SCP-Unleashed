package net.rai.scp_unleashed.scp018;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BallWater {
    public static final double SupportGap = 0.03;
    public static final double CeilingSubmersion = 0.999;

    private BallWater() {
    }

    public static double submersion(Level World, Vec3 Centre) {
        double Bottom = Centre.y - BallTuning.Radius;
        double Top = Centre.y + BallTuning.Radius;
        int X = Mth.floor(Centre.x);
        int Z = Mth.floor(Centre.z);
        double Wet = 0.0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Y = Mth.floor(Bottom); Y <= Mth.floor(Top); Y++) {
            Cursor.set(X, Y, Z);
            FluidState Fluid = World.getFluidState(Cursor);
            if (!Fluid.is(FluidTags.WATER)) {
                continue;
            }
            double Surface = Y + Fluid.getHeight(World, Cursor);
            Wet += Math.max(0.0, Math.min(Top, Surface) - Math.max(Bottom, Y));
        }
        return Mth.clamp(Wet / (2.0 * BallTuning.Radius), 0.0, 1.0);
    }

    public static Vec3 flow(Level World, Vec3 Centre) {
        BlockPos Pos = BlockPos.containing(Centre);
        FluidState Fluid = World.getFluidState(Pos);
        if (!Fluid.is(FluidTags.WATER)) {
            return Vec3.ZERO;
        }
        return Fluid.getFlow(World, Pos);
    }

    public static double drag(double Speed, double Wet, double Ticks) {
        double Full = Math.pow(BallTuning.WaterDrag, Ticks) * Math.exp(-BallTuning.WaterQuadraticDrag * Speed * Ticks);
        double Share = Math.min(1.0, Wet / BallTuning.FullDragDepth);
        return 1.0 - (1.0 - Full) * Share;
    }

    public static double stepTicks(Vec3 Velocity) {
        return Math.min(BallTuning.WaterStepTicks, BallTuning.WaterChord / Math.max(1.0e-3, Velocity.length()));
    }

    public static Vec3 step(Vec3 Velocity, double Wet, Vec3 Flow, double Ticks) {
        double Factor = drag(Velocity.length(), Wet, Ticks);
        double Lift = (BallTuning.Buoyancy * Wet - BallTuning.Gravity) * Ticks;
        return Velocity.scale(Factor).add(Flow.x * BallTuning.FlowPush * Ticks, Lift + Flow.y * BallTuning.FlowPush * Ticks, Flow.z * BallTuning.FlowPush * Ticks);
    }

    public static boolean settled(Level World, Entity Ball, Vec3 Centre, Vec3 Velocity, double Wet, Vec3 Flow) {
        if (Wet <= 0.0 || Velocity.length() >= BallTuning.RestSpeed || Flow.lengthSqr() > 1.0e-6) {
            return false;
        }
        if (Math.abs(Wet - BallTuning.floatDepth()) < BallTuning.RestBand) {
            return true;
        }
        if (Wet < BallTuning.floatDepth()) {
            return touching(World, Ball, Centre, -1.0);
        }
        return Wet >= CeilingSubmersion && touching(World, Ball, Centre, 1.0);
    }

    public static boolean touching(Level World, Entity Ball, Vec3 Centre, double Side) {
        double Edge = Centre.y + Side * BallTuning.Radius;
        AABB Probe = new AABB(Centre.x - BallTuning.Radius * 0.8, Math.min(Edge, Edge + Side * SupportGap), Centre.z - BallTuning.Radius * 0.8,
                Centre.x + BallTuning.Radius * 0.8, Math.max(Edge, Edge + Side * SupportGap), Centre.z + BallTuning.Radius * 0.8);
        for (VoxelShape Shape : World.getBlockCollisions(Ball, Probe)) {
            for (AABB Part : Shape.toAabbs()) {
                if (Part.intersects(Probe)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean blocked(Level World, Entity Ball, Vec3 Centre) {
        AABB Body = new AABB(Centre, Centre).inflate(BallTuning.Radius * 0.9);
        for (VoxelShape Shape : World.getBlockCollisions(Ball, Body)) {
            for (AABB Part : Shape.toAabbs()) {
                if (Part.intersects(Body)) {
                    return true;
                }
            }
        }
        return false;
    }
}
