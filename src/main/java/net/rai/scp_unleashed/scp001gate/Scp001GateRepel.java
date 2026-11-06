package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class Scp001GateRepel {
    public static final double SafeRing = Scp001GateZones.KillRadius + Scp001GateZones.TeleportMargin + 1.5;
    public static final double ShoveSpeed = 0.9;
    public static final double ShoveLift = 0.12;
    public static final int StuckTicks = 8;
    public static final double StuckGain = 1.5;
    public static final int MaxShoveTicks = 60;
    public static final double DeepInside = 18.0;
    public static final int SearchSteps = 18;
    public static final double SearchDegrees = 10.0;
    public static final double Settle = 0.5;
    private static final Map<Entity, Shove> Shoves = new WeakHashMap<>();

    private static final class Shove {
        long Started;
        long Checked;
        double Mark;
        long Seen;
    }

    private Scp001GateRepel() {
    }

    public static Entity mover(Entity Body) {
        return Body.getRootVehicle();
    }

    public static boolean repels(Entity Body) {
        if (Body.isRemoved() || Body instanceof Scp001GateGuardian || Body.isSpectator()) {
            return false;
        }
        if (Scp001GateAvoid.avoids(Body)) {
            return true;
        }
        if (!Body.isVehicle()) {
            return false;
        }
        for (Entity Rider : Body.getIndirectPassengers()) {
            if (Scp001GateAvoid.avoids(Rider)) {
                return true;
            }
        }
        return false;
    }

    public static void hold(Entity Body) {
        if (Body.isPassenger() || !repels(Body)) {
            return;
        }
        Vec3 Here = Body.position();
        Optional<BlockPos> Gate = Scp001GateZones.liveNearest(Body.level(), Here, Scp001GateZones.KillRadius);
        if (Gate.isEmpty()) {
            return;
        }
        BlockPos Centre = Gate.get();
        double Now = Scp001GateZones.horizontal(Centre, Here.x, Here.z);
        double Before = Scp001GateZones.horizontal(Centre, Body.xo, Body.zo);
        if (Now >= Before - 1.0e-6) {
            return;
        }
        Body.setPos(Body.xo, Here.y, Body.zo);
        Vec3 Out = outward(Centre, Body.position());
        Vec3 Motion = Body.getDeltaMovement();
        double Inward = -(Motion.x * Out.x + Motion.z * Out.z);
        if (Inward > 0.0) {
            Body.setDeltaMovement(Motion.x + Out.x * Inward, Motion.y, Motion.z + Out.z * Inward);
        }
        Body.hurtMarked = true;
    }

    public static boolean tick(ServerLevel World, Entity Body, BlockPos Centre, double Distance) {
        Entity Root = mover(Body);
        if (Root != Body || Distance > SafeRing || !repels(Root)) {
            return false;
        }
        long Now = World.getGameTime();
        Shove State = Shoves.get(Root);
        boolean Fresh = State == null || Now - State.Seen > 2;
        if (Distance > Scp001GateZones.KillRadius && (Fresh || Distance >= SafeRing - Settle)) {
            Shoves.remove(Root);
            return false;
        }
        if (Fresh) {
            State = new Shove();
            State.Started = Now;
            State.Checked = Now;
            State.Mark = Distance;
            Shoves.put(Root, State);
        }
        State.Seen = Now;
        boolean Stuck = Now - State.Checked >= StuckTicks && Distance - State.Mark < StuckGain;
        boolean Slow = Now - State.Started >= MaxShoveTicks;
        boolean Deep = Scp001GateZones.KillRadius - Distance > DeepInside;
        if (Stuck || Slow || Deep || !(Root instanceof Mob) && !Root.isPushable()) {
            Vec3 Spot = safeSpot(World, Root, Centre);
            flare(World, Root.position(), Spot);
            place(Root, Spot);
            Shoves.remove(Root);
            return true;
        }
        if (Now - State.Checked >= StuckTicks) {
            State.Checked = Now;
            State.Mark = Distance;
        }
        shove(Root, Centre);
        return true;
    }

    public static void shove(Entity Root, BlockPos Centre) {
        Vec3 Out = outward(Centre, Root.position());
        Vec3 Motion = Root.getDeltaMovement();
        double Along = Motion.x * Out.x + Motion.z * Out.z;
        double Boost = Math.max(0.0, ShoveSpeed - Along);
        Root.setDeltaMovement(Motion.x + Out.x * Boost, Math.max(Motion.y, Root.onGround() ? ShoveLift : Motion.y), Motion.z + Out.z * Boost);
        Root.hurtMarked = true;
        if (Root instanceof Mob Walker) {
            Walker.getNavigation().stop();
        }
    }

    public static Vec3 outward(BlockPos Centre, Vec3 From) {
        double Dx = From.x - (Centre.getX() + 0.5);
        double Dz = From.z - (Centre.getZ() + 0.5);
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Length < 1.0e-3) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return new Vec3(Dx / Length, 0.0, Dz / Length);
    }

    public static Vec3 safeSpot(ServerLevel World, Entity Root, BlockPos Centre) {
        Vec3 Out = outward(Centre, Root.position());
        double Base = Math.atan2(Out.z, Out.x);
        for (int Step = 0; Step <= SearchSteps; Step++) {
            for (int Side = Step == 0 ? 1 : -1; Side <= 1; Side += 2) {
                double Angle = Base + Math.toRadians(Step * SearchDegrees * Side);
                Vec3 Spot = ground(World, Root, Centre, Angle);
                if (Spot != null) {
                    return Spot;
                }
            }
        }
        return new Vec3(Centre.getX() + 0.5 + Math.cos(Base) * SafeRing, Root.getY(), Centre.getZ() + 0.5 + Math.sin(Base) * SafeRing);
    }

    @Nullable
    private static Vec3 ground(ServerLevel World, Entity Root, BlockPos Centre, double Angle) {
        double X = Centre.getX() + 0.5 + Math.cos(Angle) * SafeRing;
        double Z = Centre.getZ() + 0.5 + Math.sin(Angle) * SafeRing;
        BlockPos Column = BlockPos.containing(X, Root.getY(), Z);
        if (!World.isInWorldBounds(Column)) {
            return null;
        }
        World.getChunk(Column);
        double Y = Root.isNoGravity() ? Root.getY() : World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Column.getX(), Column.getZ());
        if (!Scp001GateZones.band(Centre, Y)) {
            Y = Mth.clamp(Y, Centre.getY() - Scp001GateZones.Below + 1, Centre.getY() + Scp001GateZones.Above - 1);
        }
        AABB Box = Root.getBoundingBox().move(X - Root.getX(), Y - Root.getY(), Z - Root.getZ());
        if (!World.noCollision(Root, Box) || World.containsAnyLiquid(Box)) {
            return null;
        }
        return new Vec3(X, Y, Z);
    }

    public static void place(Entity Root, Vec3 Spot) {
        Root.setDeltaMovement(Vec3.ZERO);
        Root.teleportTo(Spot.x, Spot.y, Spot.z);
        if (Root.position().distanceToSqr(Spot) > 0.25) {
            Root.moveTo(Spot.x, Spot.y, Spot.z, Root.getYRot(), Root.getXRot());
        }
        Root.hurtMarked = true;
        if (Root instanceof Mob Walker) {
            Walker.getNavigation().stop();
        }
    }

    public static void flare(ServerLevel World, Vec3 From, Vec3 To) {
        World.sendParticles(ParticleTypes.FLASH, From.x, From.y + 1.0, From.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(ParticleTypes.FLAME, From.x, From.y + 1.0, From.z, 24, 0.5, 0.9, 0.5, 0.06);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, From.x, From.y + 1.0, From.z, 8, 0.4, 0.8, 0.4, 0.02);
        World.sendParticles(ParticleTypes.FLASH, To.x, To.y + 1.0, To.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(ParticleTypes.FLAME, To.x, To.y + 0.5, To.z, 16, 0.4, 0.6, 0.4, 0.04);
        World.sendParticles(ParticleTypes.END_ROD, To.x, To.y + 1.0, To.z, 10, 0.3, 0.8, 0.3, 0.05);
        World.playSound(null, From.x, From.y, From.z, Scp001GateRegistry.Burn.get(), SoundSource.HOSTILE, 1.6f, 0.7f);
        World.playSound(null, To.x, To.y, To.z, Scp001GateRegistry.Pass.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
    }
}
