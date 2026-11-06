package net.rai.scp_unleashed.scp352;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class Scp352Leap {
    public static final double Gravity = 0.08;
    public static final double Drag = 0.98;
    public static final double MaxSpeed = 1.2;
    public static final double MinApex = 0.8;
    public static final double ClimbClearance = 1.2;
    public static final double LongApex = 1.2;
    public static final double ApexPerBlock = 0.14;
    public static final double LiftStep = 0.02;
    public static final double MaxLift = 1.6;
    public static final int MaxTicks = 60;
    public static final int SearchRing = 2;
    public static final int SearchAbove = 2;
    public static final int SearchBelow = 9;
    public static final double Margin = 0.06;

    private Scp352Leap() {
    }

    public record Plan(Vec3 Start, Vec3 Landing, Vec3 Velocity, int Ticks, double Apex) {
    }

    public static AABB box(Vec3 Feet) {
        return AABB.ofSize(Feet.add(0.0, Scp352Crone.TallHeight * 0.5, 0.0), Scp352Crone.TallWidth, Scp352Crone.TallHeight, Scp352Crone.TallWidth);
    }

    public static boolean landable(Level World, Entity Mover, BlockPos Feet) {
        if (!World.isLoaded(Feet) || World.isOutsideBuildHeight(Feet)) {
            return false;
        }
        BlockPos Below = Feet.below();
        BlockState Ground = World.getBlockState(Below);
        if (!Ground.isFaceSturdy(World, Below, Direction.UP) || Ground.getFluidState().is(FluidTags.LAVA) || Ground.is(Blocks.MAGMA_BLOCK) || Ground.is(Blocks.CAMPFIRE)
                || Ground.is(Blocks.SOUL_CAMPFIRE)) {
            return false;
        }
        AABB Room = box(Vec3.atBottomCenterOf(Feet)).deflate(Margin);
        return World.noCollision(Mover, Room) && !World.containsAnyLiquid(Room.inflate(0.0, 0.5, 0.0).move(0.0, -0.5, 0.0));
    }

    public static List<Vec3> landings(Level World, Entity Mover, Vec3 Aim) {
        List<Vec3> Found = new ArrayList<>();
        BlockPos Centre = BlockPos.containing(Aim);
        for (int Dx = -SearchRing; Dx <= SearchRing; Dx++) {
            for (int Dz = -SearchRing; Dz <= SearchRing; Dz++) {
                for (int Dy = SearchAbove; Dy >= -SearchBelow; Dy--) {
                    BlockPos Feet = Centre.offset(Dx, Dy, Dz);
                    if (landable(World, Mover, Feet)) {
                        Found.add(Vec3.atBottomCenterOf(Feet));
                        break;
                    }
                }
            }
        }
        Found.sort(Comparator.comparingDouble(Spot -> Spot.distanceToSqr(Aim)));
        return Found;
    }

    public static double[] apexes(Vec3 Start, Vec3 Landing) {
        double Rise = Math.max(0.0, Landing.y - Start.y);
        double Dx = Landing.x - Start.x;
        double Dz = Landing.z - Start.z;
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        double Preferred = Math.max(Rise + ClimbClearance, LongApex + ApexPerBlock * Flat);
        return new double[]{Preferred, Math.max(Rise + ClimbClearance, Preferred * 0.65), Rise + ClimbClearance * 2.0, Math.max(MinApex, Rise + MinApex)};
    }

    public static double lift(double Apex) {
        for (double Lift = LiftStep; Lift <= MaxLift; Lift += LiftStep) {
            double Height = 0.0;
            double Speed = Lift;
            while (Speed > 0.0) {
                Height += Speed;
                Speed = (Speed - Gravity) * Drag;
            }
            if (Height >= Apex) {
                return Lift;
            }
        }
        return -1.0;
    }

    public static int airTicks(double Lift, double Drop) {
        double Height = 0.0;
        double Speed = Lift;
        for (int Tick = 1; Tick <= MaxTicks; Tick++) {
            Height += Speed;
            Speed = (Speed - Gravity) * Drag;
            if (Speed < 0.0 && Height <= Drop) {
                return Tick;
            }
        }
        return -1;
    }

    public static Optional<Plan> arc(Level World, Entity Mover, Vec3 Start, Vec3 Landing, double Apex) {
        double Lift = lift(Apex);
        if (Lift <= 0.0) {
            return Optional.empty();
        }
        int Ticks = airTicks(Lift, Landing.y - Start.y);
        if (Ticks <= 2) {
            return Optional.empty();
        }
        double Vx = (Landing.x - Start.x) / Ticks;
        double Vz = (Landing.z - Start.z) / Ticks;
        if (Math.sqrt(Vx * Vx + Vz * Vz) > MaxSpeed) {
            return Optional.empty();
        }
        double Height = 0.0;
        double Speed = Lift;
        double Floor = Math.min(Start.y, Landing.y) + 0.02;
        for (int Tick = 1; Tick <= Ticks; Tick++) {
            double Before = Height;
            Height += Speed;
            Speed = (Speed - Gravity) * Drag;
            for (int Half = 1; Half <= 2; Half++) {
                double Share = Half * 0.5;
                double Step = Tick - 1 + Share;
                double Y = Tick == Ticks && Half == 2 ? Landing.y + 0.02 : Math.max(Start.y + Before + (Height - Before) * Share, Floor);
                Vec3 Feet = new Vec3(Start.x + Vx * Step, Y, Start.z + Vz * Step);
                if (!World.noCollision(Mover, box(Feet).deflate(Margin))) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(new Plan(Start, Landing, new Vec3(Vx, Lift, Vz), Ticks, Apex));
    }

    public static Optional<Plan> plan(Level World, Entity Mover, Vec3 Aim) {
        Vec3 Start = Mover.position();
        for (Vec3 Landing : landings(World, Mover, Aim)) {
            double Dx = Landing.x - Start.x;
            double Dz = Landing.z - Start.z;
            if (Dx * Dx + Dz * Dz < 1.0) {
                continue;
            }
            for (double Apex : apexes(Start, Landing)) {
                Optional<Plan> Found = arc(World, Mover, Start, Landing, Apex);
                if (Found.isPresent()) {
                    return Found;
                }
            }
        }
        return Optional.empty();
    }

    public static Vec3 aim(Vec3 From, Vec3 Target, Vec3 Velocity, double Lead, double LeadCap, double Short) {
        Vec3 Ahead = new Vec3(Velocity.x, 0.0, Velocity.z).scale(Lead);
        if (Ahead.length() > LeadCap) {
            Ahead = Ahead.normalize().scale(LeadCap);
        }
        Vec3 Point = Target.add(Ahead);
        Vec3 Back = new Vec3(Point.x - From.x, 0.0, Point.z - From.z);
        double Length = Back.length();
        if (Length > Short + 0.5) {
            Point = Point.subtract(Back.scale(Short / Length));
        }
        return new Vec3(Point.x, Mth.floor(Target.y + 0.01), Point.z);
    }
}
