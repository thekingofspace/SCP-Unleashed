package net.rai.scp_unleashed.scp469;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp469Arrival {
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int Attempts = 40;
    public static final int Vertical = 8;
    public static final int BehindAttempts = 24;
    public static final double BehindArc = 75.0;
    public static final double ViewCone = 0.35;

    private Scp469Arrival() {
    }

    @Nullable
    public static BlockPos ground(ServerLevel World, int X, int Z, int ReaderY) {
        if (!World.isLoaded(new BlockPos(X, ReaderY, Z))) {
            return null;
        }
        if (World.dimensionType().hasCeiling()) {
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Candidate = new BlockPos(X, ReaderY + Dy, Z);
                if (Scp469Reform.fits(World, Candidate)) {
                    return Candidate;
                }
            }
            return null;
        }
        BlockPos Top = new BlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z), Z);
        if (Scp469Reform.fits(World, Top)) {
            return Top;
        }
        for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
            BlockPos Candidate = new BlockPos(X, ReaderY + Dy, Z);
            if (Scp469Reform.fits(World, Candidate)) {
                return Candidate;
            }
        }
        return null;
    }

    public static boolean inView(ServerPlayer Reader, Vec3 Point) {
        Vec3 Toward = Point.subtract(Reader.getEyePosition()).normalize();
        return Toward.dot(Reader.getViewVector(1.0f)) > ViewCone;
    }

    public static boolean inSight(ServerLevel World, ServerPlayer Reader, Vec3 Bottom) {
        Vec3 Eye = Reader.getEyePosition();
        for (Vec3 Probe : new Vec3[]{Bottom.add(0.0, 1.1, 0.0), Bottom.add(0.0, 2.1, 0.0)}) {
            HitResult Hit = World.clip(new ClipContext(Eye, Probe, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader));
            if (Hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    public static boolean acceptable(ServerLevel World, ServerPlayer Reader, BlockPos Pos, int Mode) {
        Vec3 Bottom = Vec3.atBottomCenterOf(Pos);
        if (Mode == Forced) {
            return true;
        }
        boolean Visible = inView(Reader, Bottom.add(0.0, 1.1, 0.0)) && inSight(World, Reader, Bottom);
        if (Visible) {
            return false;
        }
        if (Mode == Strict) {
            return !inSight(World, Reader, Bottom) || !inView(Reader, Bottom.add(0.0, 1.1, 0.0));
        }
        return true;
    }

    public static Optional<BlockPos> findSpot(ServerLevel World, ServerPlayer Reader, int MinDistance, int MaxDistance, int Mode, RandomSource Random) {
        float Facing = Reader.getYRot();
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Yaw = Attempt < BehindAttempts ? Facing + 180.0 + (Random.nextDouble() * 2.0 - 1.0) * BehindArc : Random.nextDouble() * 360.0;
            double Radius = MinDistance + Random.nextDouble() * Math.max(0, MaxDistance - MinDistance);
            int X = Mth.floor(Reader.getX() - Mth.sin((float) Math.toRadians(Yaw)) * Radius);
            int Z = Mth.floor(Reader.getZ() + Mth.cos((float) Math.toRadians(Yaw)) * Radius);
            BlockPos Pos = ground(World, X, Z, Reader.getBlockY());
            if (Pos == null || Math.abs(Pos.getY() - Reader.getBlockY()) > 24) {
                continue;
            }
            if (acceptable(World, Reader, Pos, Mode)) {
                return Optional.of(Pos);
            }
        }
        return Optional.empty();
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        Vec3 Spot = Vec3.atBottomCenterOf(Found.Pos());
        Vec3 Toward = Found.Investigator().position().subtract(Spot);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        Optional<Scp469Angel> Placed = place(World, Spot, Yaw);
        Placed.ifPresent(Creature -> Creature.remember(Found.Investigator().position(), Found.Investigator().getUUID()));
        return Placed.isPresent();
    }

    public static Optional<Scp469Angel> place(ServerLevel World, Vec3 Spot, float Yaw) {
        Scp469Angel Creature = Scp469Registry.Angel.get().create(World);
        if (Creature == null) {
            return Optional.empty();
        }
        Creature.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Creature.setYHeadRot(Yaw);
        Creature.setYBodyRot(Yaw);
        if (!World.noCollision(Creature)) {
            return Optional.empty();
        }
        Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        if (!World.addFreshEntity(Creature)) {
            return Optional.empty();
        }
        Scp469Effects.layers(World, Creature, Scp469Registry.ShimmerLayers, 1.4f, 0.9f);
        return Optional.of(Creature);
    }
}
