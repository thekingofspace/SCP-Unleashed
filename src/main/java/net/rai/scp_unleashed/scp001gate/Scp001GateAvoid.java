package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class Scp001GateAvoid {
    public static final String Marker = "scp001gate_avoider";
    public static final double Retreat = 66.0;
    public static final double Orbit = 58.0;
    public static final double Clearance = 50.0;
    public static final double StepDegrees = 25.0;
    public static final double Speed = 1.25;
    private static final Map<Mob, AvoidGoal> Goals = new WeakHashMap<>();

    private Scp001GateAvoid() {
    }

    public static boolean avoids(Entity Body) {
        return Body.getType().is(Scp001GateRegistry.Avoiders) || Body.getTags().contains(Marker);
    }

    public static void install(Entity Body) {
        if (Body instanceof PathfinderMob Walker && avoids(Body) && !Goals.containsKey(Walker)) {
            AvoidGoal Made = new AvoidGoal(Walker);
            Walker.goalSelector.addGoal(0, Made);
            Goals.put(Walker, Made);
        }
    }

    public static boolean blocked(Level World, Vec3 Destination) {
        return Scp001GateZones.forbidden(World, Destination);
    }

    public static boolean crosses(BlockPos Centre, Vec3 From, Vec3 To, double Radius) {
        double Cx = Centre.getX() + 0.5;
        double Cz = Centre.getZ() + 0.5;
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        double Length = Dx * Dx + Dz * Dz;
        double Share = Length < 1.0e-6 ? 0.0 : Mth.clamp(((Cx - From.x) * Dx + (Cz - From.z) * Dz) / Length, 0.0, 1.0);
        double Px = From.x + Dx * Share - Cx;
        double Pz = From.z + Dz * Share - Cz;
        return Px * Px + Pz * Pz < Radius * Radius;
    }

    public static void request(Mob Walker, Vec3 Destination) {
        AvoidGoal Goal = Goals.get(Walker);
        Optional<BlockPos> Gate = Scp001GateZones.nearest(Walker.level(), Walker.position(), Scp001GateZones.AvoidRadius + 80.0);
        if (Goal != null && Gate.isPresent() && crosses(Gate.get(), Walker.position(), Destination, Clearance)) {
            Goal.detour(Gate.get(), Destination);
            return;
        }
        if (Goal == null || !Goal.busy()) {
            Walker.getNavigation().moveTo(Destination.x, Destination.y, Destination.z, Speed);
        }
    }

    public static Vec3 retreat(BlockPos Centre, Vec3 From) {
        double Dx = From.x - (Centre.getX() + 0.5);
        double Dz = From.z - (Centre.getZ() + 0.5);
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Length < 1.0e-3) {
            Dx = 1.0;
            Dz = 0.0;
            Length = 1.0;
        }
        return new Vec3(Centre.getX() + 0.5 + Dx / Length * Retreat, From.y, Centre.getZ() + 0.5 + Dz / Length * Retreat);
    }

    public static Vec3 around(BlockPos Centre, Vec3 From, Vec3 Destination) {
        double Cx = Centre.getX() + 0.5;
        double Cz = Centre.getZ() + 0.5;
        double Here = Math.atan2(From.z - Cz, From.x - Cx);
        double There = Math.atan2(Destination.z - Cz, Destination.x - Cx);
        double Turn = Mth.wrapDegrees(Math.toDegrees(There - Here));
        double Step = Math.toRadians(Math.copySign(StepDegrees, Turn == 0.0 ? 1.0 : Turn));
        double Next = Here + Step;
        return new Vec3(Cx + Math.cos(Next) * Orbit, From.y, Cz + Math.sin(Next) * Orbit);
    }

    public static void fence(Entity Body, BlockPos Centre, double Distance) {
        if (Body instanceof Mob Walker && Distance <= Scp001GateZones.AvoidRadius + 16.0) {
            Path Route = Walker.getNavigation().getPath();
            if (Route != null && Scp001GateGuardian.pathCrosses(Walker, Centre)) {
                BlockPos End = Route.getTarget();
                Walker.getNavigation().stop();
                AvoidGoal Goal = Goals.get(Walker);
                if (Goal != null && !Scp001GateZones.within(Centre, Vec3.atBottomCenterOf(End), Scp001GateZones.KillRadius + 2.0)) {
                    Goal.detour(Centre, Vec3.atBottomCenterOf(End));
                }
            }
        }
        if (Distance > Scp001GateZones.FenceRadius || Distance <= Scp001GateZones.KillRadius) {
            return;
        }
        Vec3 Motion = Body.getDeltaMovement();
        double Ox = Body.getX() - (Centre.getX() + 0.5);
        double Oz = Body.getZ() - (Centre.getZ() + 0.5);
        double Length = Math.sqrt(Ox * Ox + Oz * Oz);
        if (Length < 1.0e-3) {
            return;
        }
        Ox /= Length;
        Oz /= Length;
        double Inward = -(Motion.x * Ox + Motion.z * Oz);
        if (Inward > 0.0) {
            Body.setDeltaMovement(Motion.x + Ox * Inward, Motion.y, Motion.z + Oz * Inward);
            Body.hurtMarked = true;
        }
    }

    public static final class AvoidGoal extends Goal {
        private final PathfinderMob Walker;
        @Nullable
        private BlockPos Gate;
        @Nullable
        private Vec3 Destination;
        private boolean Fleeing;
        private int Repath;

        public AvoidGoal(PathfinderMob Walker) {
            this.Walker = Walker;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        public boolean busy() {
            return Gate != null;
        }

        public void detour(BlockPos Centre, Vec3 Target) {
            Gate = Centre;
            Destination = Target;
            Repath = 0;
        }

        private boolean blocked() {
            return Gate != null && Destination != null && crosses(Gate, Walker.position(), Destination, Clearance);
        }

        @Override
        public boolean canUse() {
            Optional<BlockPos> Found = Scp001GateZones.nearest(Walker.level(), Walker.position(), Scp001GateZones.AvoidRadius);
            if (Found.isPresent()) {
                Gate = Found.get();
                Fleeing = true;
                return true;
            }
            Fleeing = false;
            if (blocked()) {
                return true;
            }
            if (Destination != null && Gate != null) {
                Walker.getNavigation().moveTo(Destination.x, Destination.y, Destination.z, Speed);
                Destination = null;
                Gate = null;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            if (Gate == null) {
                return false;
            }
            if (Fleeing) {
                return Scp001GateZones.horizontal(Gate, Walker.getX(), Walker.getZ()) < Scp001GateZones.AvoidRadius + 6.0;
            }
            return blocked();
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            if (Destination != null && Gate != null && !blocked()) {
                Walker.getNavigation().moveTo(Destination.x, Destination.y, Destination.z, Speed);
                Destination = null;
            }
            if (Destination == null) {
                Gate = null;
            }
            Fleeing = false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Gate == null) {
                return;
            }
            if (--Repath > 0 && !Walker.getNavigation().isDone()) {
                return;
            }
            Repath = 15;
            Vec3 Next = Fleeing || Destination == null ? retreat(Gate, Walker.position()) : around(Gate, Walker.position(), Destination);
            if (!Walker.getNavigation().moveTo(Next.x, Next.y, Next.z, Speed)) {
                Vec3 Push = Next.subtract(Walker.position()).normalize().scale(0.2);
                Walker.setDeltaMovement(Walker.getDeltaMovement().add(Push.x, 0.0, Push.z));
            }
            Walker.getLookControl().setLookAt(Next.x, Walker.getEyeY(), Next.z);
        }
    }
}
