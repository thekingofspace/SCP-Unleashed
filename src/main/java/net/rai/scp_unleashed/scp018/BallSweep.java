package net.rai.scp_unleashed.scp018;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BallSweep {
    public static final double SurfaceTolerance = 1.0e-7;
    public static final double MinimumChordTicks = 0.02;
    public static final double EntityMargin = 0.5;
    public static final double ProbeDepth = 0.03;
    public static final double EntityProbe = 0.35;
    public static final int EntrySteps = 8;

    private BallSweep() {
    }

    public interface EntityFilter {
        boolean allows(Entity Target, double Time);
    }

    public record Hit(double Tau, Vec3 Normal, @Nullable Entity Struck, boolean Unloaded, boolean Water) {
    }

    private record Contact(double Fraction, Vec3 Normal) {
    }

    public static boolean collidable(Entity Target) {
        if (Target instanceof Scp018Ball || !Target.isAlive() || Target.isSpectator()) {
            return false;
        }
        return Target instanceof LivingEntity && Target.isPickable() || Target.canBeCollidedWith();
    }

    @Nullable
    public static Hit sweep(ServerLevel World, Entity Ball, BallSegment Segment, double From, double To, boolean CheckLoaded, boolean CheckWater, EntityFilter Filter) {
        if (To <= From) {
            return null;
        }
        double FastestSpeed = Math.max(Segment.velocityAtLocal(From).length(), Segment.velocityAtLocal(To).length());
        double Step = Math.max(MinimumChordTicks, Math.min(BallTuning.MaxChordTicks, BallTuning.ChordLength / Math.max(1.0e-3, FastestSpeed)));
        List<Entity> Candidates = World.getEntities(Ball, arcBox(Segment, From, To).inflate(BallTuning.Radius + EntityMargin), BallSweep::collidable);
        long LastChunk = Long.MIN_VALUE;
        double Tau = From;
        Vec3 Previous = Segment.positionAtLocal(From);
        while (Tau < To - 1.0e-12) {
            double Next = Math.min(To, Tau + Step);
            Vec3 Current = Segment.positionAtLocal(Next);
            if (CheckLoaded) {
                BlockPos Probe = BlockPos.containing(Current);
                long Chunk = ChunkPos.asLong(Probe.getX() >> 4, Probe.getZ() >> 4);
                if (!World.getWorldBorder().isWithinBounds(Probe) || Chunk != LastChunk && !World.isPositionEntityTicking(Probe)) {
                    return new Hit(Tau, Vec3.ZERO, null, true, false);
                }
                LastChunk = Chunk;
            }
            Contact Best = null;
            Entity BestEntity = null;
            AABB Chord = new AABB(Previous, Current).inflate(BallTuning.Radius + 1.0e-3);
            for (VoxelShape Shape : World.getBlockCollisions(Ball, Chord)) {
                for (AABB Part : Shape.toAabbs()) {
                    Contact Found = contact(Previous, Current, Part.inflate(BallTuning.Radius));
                    if (Found != null && (Best == null || Found.Fraction() < Best.Fraction())) {
                        Best = Found;
                    }
                }
            }
            for (Entity Target : Candidates) {
                Contact Found = contact(Previous, Current, Target.getBoundingBox().inflate(BallTuning.Radius));
                if (Found == null || Best != null && Found.Fraction() >= Best.Fraction()) {
                    continue;
                }
                if (Filter.allows(Target, Segment.StartTime() + Tau + Found.Fraction() * (Next - Tau))) {
                    Best = Found;
                    BestEntity = Target;
                }
            }
            double Reach = Best != null ? Tau + Best.Fraction() * (Next - Tau) : Next;
            if (CheckWater && BallWater.submersion(World, Segment.positionAtLocal(Reach)) > 0.0) {
                return new Hit(entry(World, Segment, Tau, Reach), Vec3.ZERO, null, false, true);
            }
            if (Best != null) {
                return new Hit(Reach, Best.Normal(), BestEntity, false, false);
            }
            Tau = Next;
            Previous = Current;
        }
        return null;
    }

    private static double entry(ServerLevel World, BallSegment Segment, double Dry, double Wet) {
        if (BallWater.submersion(World, Segment.positionAtLocal(Dry)) > 0.0) {
            return Dry;
        }
        for (int Step = 0; Step < EntrySteps; Step++) {
            double Middle = (Dry + Wet) / 2.0;
            if (BallWater.submersion(World, Segment.positionAtLocal(Middle)) > 0.0) {
                Wet = Middle;
            } else {
                Dry = Middle;
            }
        }
        return Wet;
    }

    public static AABB arcBox(BallSegment Segment, double From, double To) {
        Vec3 First = Segment.positionAtLocal(From);
        Vec3 Last = Segment.positionAtLocal(To);
        AABB Box = new AABB(First, Last);
        double Apex = Segment.Velocity().y / BallTuning.Gravity;
        if (Apex > From && Apex < To) {
            Box = Box.minmax(new AABB(Segment.positionAtLocal(Apex), Segment.positionAtLocal(Apex)));
        }
        return Box;
    }

    @Nullable
    private static Contact contact(Vec3 From, Vec3 To, AABB Box) {
        double[] Start = {From.x, From.y, From.z};
        double[] Delta = {To.x - From.x, To.y - From.y, To.z - From.z};
        double[] Low = {Box.minX, Box.minY, Box.minZ};
        double[] High = {Box.maxX, Box.maxY, Box.maxZ};
        double Enter = Double.NEGATIVE_INFINITY;
        double Exit = Double.POSITIVE_INFINITY;
        int Axis = -1;
        for (int Index = 0; Index < 3; Index++) {
            if (Math.abs(Delta[Index]) < 1.0e-12) {
                if (Start[Index] <= Low[Index] || Start[Index] >= High[Index]) {
                    return null;
                }
                continue;
            }
            double First = (Low[Index] - Start[Index]) / Delta[Index];
            double Second = (High[Index] - Start[Index]) / Delta[Index];
            double Near = Math.min(First, Second);
            double Far = Math.max(First, Second);
            if (Near > Enter) {
                Enter = Near;
                Axis = Index;
            }
            Exit = Math.min(Exit, Far);
        }
        if (Axis < 0 || Enter > Exit || Exit <= 0.0 || Enter > 1.0) {
            return null;
        }
        double Length = Math.sqrt(Delta[0] * Delta[0] + Delta[1] * Delta[1] + Delta[2] * Delta[2]);
        if (Enter < 0.0) {
            if (Enter * Length < -SurfaceTolerance) {
                return null;
            }
            Enter = 0.0;
        }
        double Sign = Delta[Axis] > 0.0 ? -1.0 : 1.0;
        Vec3 Normal = Axis == 0 ? new Vec3(Sign, 0.0, 0.0) : Axis == 1 ? new Vec3(0.0, Sign, 0.0) : new Vec3(0.0, 0.0, Sign);
        return new Contact(Enter, Normal);
    }

    public static boolean blockStillThere(ServerLevel World, Entity Ball, Vec3 Point, Vec3 Normal) {
        Vec3 Probe = Point.subtract(Normal.scale(ProbeDepth));
        AABB Box = new AABB(Probe, Probe).inflate(BallTuning.Radius);
        for (VoxelShape Shape : World.getBlockCollisions(Ball, Box)) {
            for (AABB Part : Shape.toAabbs()) {
                if (Part.intersects(Box)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean entityStillThere(@Nullable Entity Target, Vec3 Point) {
        return Target != null && collidable(Target) && Target.getBoundingBox().inflate(BallTuning.Radius + EntityProbe).contains(Point);
    }
}
