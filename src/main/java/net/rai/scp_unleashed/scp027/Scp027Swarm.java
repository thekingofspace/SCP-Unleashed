package net.rai.scp_unleashed.scp027;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class Scp027Swarm {
    public static final int MaxSwarm = 10;
    public static final int IdleSwarm = 3;
    public static final int MaxBats = 3;
    public static final int MaxMites = 2;
    public static final int CrowdCap = 16;
    public static final double CountRange = 40.0;
    public static final double CrowdRange = 24.0;
    public static final double AwakeRange = 20.0;
    public static final double WatchRange = 48.0;
    public static final int AwakeInterval = 50;
    public static final int IdleInterval = 160;
    public static final double SpawnMin = 1.5;
    public static final double SpawnMax = 4.5;
    public static final int SpotTries = 12;
    public static final double SoakRange = 6.0;
    public static final int SoakFull = 6;
    public static final float Leak = 0.25f;
    public static final int RatWeight = 45;
    public static final int SilverfishWeight = 30;
    public static final int MiteWeight = 10;
    public static final int BatWeight = 15;

    public enum Kind {
        Rat,
        Silverfish,
        Mite,
        Bat
    }

    private Scp027Swarm() {
    }

    public static List<Mob> owned(ServerLevel World, Scp027Entity Master) {
        AABB Area = Master.getBoundingBox().inflate(CountRange);
        return World.getEntitiesOfClass(Mob.class, Area, Found -> Found instanceof Scp027Vermin Vermin && Found.isAlive() && Vermin.leash().ownedBy(Master)
                && !Vermin.leash().scattering());
    }

    public static int crowd(ServerLevel World, Vec3 Centre) {
        AABB Area = new AABB(Centre, Centre).inflate(CrowdRange);
        return World.getEntitiesOfClass(Mob.class, Area, Found -> Found instanceof Scp027Vermin Vermin && Found.isAlive() && !Vermin.leash().scattering()).size();
    }

    public static int count(List<Mob> Swarm, Kind Wanted) {
        int Total = 0;
        for (Mob Found : Swarm) {
            if (kindOf(Found) == Wanted) {
                Total++;
            }
        }
        return Total;
    }

    @Nullable
    public static Kind kindOf(Mob Found) {
        if (Found instanceof Scp027Rat) {
            return Kind.Rat;
        }
        if (Found instanceof Scp027Silverfish) {
            return Kind.Silverfish;
        }
        if (Found instanceof Scp027Endermite) {
            return Kind.Mite;
        }
        if (Found instanceof Scp027Bat) {
            return Kind.Bat;
        }
        return null;
    }

    public static int cap(boolean Awake) {
        return Awake ? MaxSwarm : IdleSwarm;
    }

    public static Kind pick(List<Mob> Swarm, RandomSource Random) {
        boolean BatsFull = count(Swarm, Kind.Bat) >= MaxBats;
        boolean MitesFull = count(Swarm, Kind.Mite) >= MaxMites;
        int Total = RatWeight + SilverfishWeight + (MitesFull ? 0 : MiteWeight) + (BatsFull ? 0 : BatWeight);
        int Roll = Random.nextInt(Total);
        if ((Roll -= RatWeight) < 0) {
            return Kind.Rat;
        }
        if ((Roll -= SilverfishWeight) < 0) {
            return Kind.Silverfish;
        }
        if (!MitesFull && (Roll -= MiteWeight) < 0) {
            return Kind.Mite;
        }
        return BatsFull ? Kind.Silverfish : Kind.Bat;
    }

    public static EntityType<? extends Mob> typeOf(Kind Wanted) {
        return switch (Wanted) {
            case Rat -> Scp027Registry.Rat.get();
            case Silverfish -> Scp027Registry.Silverfish.get();
            case Mite -> Scp027Registry.Endermite.get();
            case Bat -> Scp027Registry.Bat.get();
        };
    }

    public static Optional<Mob> summon(ServerLevel World, Scp027Entity Master, Kind Wanted) {
        EntityType<? extends Mob> Type = typeOf(Wanted);
        Optional<Vec3> Spot = Wanted == Kind.Bat ? airSpot(World, Master, Type) : groundSpot(World, Master, Type);
        if (Spot.isEmpty()) {
            return Optional.empty();
        }
        Mob Made = Type.create(World);
        if (!(Made instanceof Scp027Vermin Vermin)) {
            return Optional.empty();
        }
        Vec3 At = Spot.get();
        Made.moveTo(At.x, At.y, At.z, World.getRandom().nextFloat() * 360.0f, 0.0f);
        Made.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(At)), MobSpawnType.MOB_SUMMONED, null);
        Vermin.leash().bind(Master);
        if (!World.addFreshEntity(Made)) {
            return Optional.empty();
        }
        emerge(World, Made, Wanted);
        return Optional.of(Made);
    }

    private static void emerge(ServerLevel World, Mob Made, Kind Wanted) {
        if (Wanted == Kind.Bat) {
            World.sendParticles(ParticleTypes.SMOKE, Made.getX(), Made.getY() + 0.3, Made.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
        } else {
            BlockState Ground = World.getBlockState(Made.blockPosition().below());
            if (!Ground.isAir()) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Made.getX(), Made.getY() + 0.1, Made.getZ(), 10, 0.2, 0.05, 0.2, 0.1);
            }
        }
        play(World, Made.position(), Scp027Registry.SummonLayers, 0.5f, 0.9f + World.getRandom().nextFloat() * 0.25f);
    }

    public static Optional<Vec3> groundSpot(ServerLevel World, Scp027Entity Master, EntityType<?> Type) {
        RandomSource Random = World.getRandom();
        for (int Try = 0; Try < SpotTries; Try++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = SpawnMin + Random.nextDouble() * (SpawnMax - SpawnMin);
            BlockPos Column = BlockPos.containing(Master.getX() + Math.cos(Angle) * Radius, Master.getY(), Master.getZ() + Math.sin(Angle) * Radius);
            for (int Dy = 2; Dy >= -3; Dy--) {
                BlockPos Feet = Column.above(Dy);
                BlockPos Below = Feet.below();
                if (!World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP)) {
                    continue;
                }
                Vec3 At = Vec3.atBottomCenterOf(Feet);
                if (World.noCollision(Type.getDimensions().makeBoundingBox(At)) && !World.containsAnyLiquid(Type.getDimensions().makeBoundingBox(At))) {
                    return Optional.of(At);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Vec3> airSpot(ServerLevel World, Scp027Entity Master, EntityType<?> Type) {
        RandomSource Random = World.getRandom();
        for (int Try = 0; Try < SpotTries; Try++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = 0.8 + Random.nextDouble() * 1.6;
            Vec3 At = new Vec3(Master.getX() + Math.cos(Angle) * Radius, Master.getY() + 1.6 + Random.nextDouble() * 0.8, Master.getZ() + Math.sin(Angle) * Radius);
            if (World.noCollision(Type.getDimensions().makeBoundingBox(At)) && !World.containsAnyLiquid(Type.getDimensions().makeBoundingBox(At))) {
                return Optional.of(At);
            }
        }
        return Optional.empty();
    }

    public static int scatterAll(ServerLevel World, Scp027Entity Master) {
        AABB Area = Master.getBoundingBox().inflate(CountRange + 16.0);
        List<Mob> Found = World.getEntitiesOfClass(Mob.class, Area, Body -> Body instanceof Scp027Vermin Vermin && Vermin.leash().ownedBy(Master));
        for (Mob Body : Found) {
            ((Scp027Vermin) Body).leash().scatter(Body);
        }
        if (!Found.isEmpty()) {
            play(World, Master.position(), Scp027Registry.ScatterLayers, 0.9f, 1.0f);
        }
        return Found.size();
    }

    public static Optional<Mob> interceptor(ServerLevel World, Scp027Entity Master) {
        AABB Area = Master.getBoundingBox().inflate(SoakRange);
        return World.getEntitiesOfClass(Mob.class, Area, Body -> Body instanceof Scp027Vermin Vermin && Body.isAlive() && Vermin.leash().ownedBy(Master)
                        && !Vermin.leash().scattering() && Body.distanceToSqr(Master) <= SoakRange * SoakRange)
                .stream().min(Comparator.comparingDouble(Body -> Body.distanceToSqr(Master)));
    }

    public static int near(ServerLevel World, Scp027Entity Master) {
        AABB Area = Master.getBoundingBox().inflate(SoakRange);
        return World.getEntitiesOfClass(Mob.class, Area, Body -> Body instanceof Scp027Vermin Vermin && Body.isAlive() && Vermin.leash().ownedBy(Master)
                && !Vermin.leash().scattering() && Body.distanceToSqr(Master) <= SoakRange * SoakRange).size();
    }

    public static float soakChance(int Near) {
        return Math.min(1.0f, Near / (float) SoakFull);
    }

    public static void play(ServerLevel World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }
}
