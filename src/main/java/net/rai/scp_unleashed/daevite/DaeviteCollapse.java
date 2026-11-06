package net.rai.scp_unleashed.daevite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.daevite.entity.DaeviteMob;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DaeviteCollapse {
    public static final DustParticleOptions Ash = new DustParticleOptions(new Vector3f(0.1f, 0.09f, 0.09f), 1.4f);
    public static final DustColorTransitionOptions Cinder = new DustColorTransitionOptions(new Vector3f(0.98f, 0.38f, 0.1f), new Vector3f(0.2f, 0.03f, 0.02f), 0.8f);
    public static final DustParticleOptions Beam = new DustParticleOptions(new Vector3f(0.86f, 0.08f, 0.06f), 0.9f);
    public static final DustParticleOptions Mend = new DustParticleOptions(new Vector3f(0.95f, 0.55f, 0.2f), 0.9f);
    public static final DustColorTransitionOptions Shard = new DustColorTransitionOptions(new Vector3f(0.9f, 0.1f, 0.12f), new Vector3f(0.35f, 0.0f, 0.04f), 1.3f);
    public static final int MaxDelay = 100;

    private record Pending(ResourceKey<Level> Dimension, long Due) {
    }

    private static final Map<UUID, Pending> Queue = new ConcurrentHashMap<>();

    private DaeviteCollapse() {
    }

    public static void doom(ServerLevel World, Entity Body, int Delay) {
        if (Body instanceof DaeviteMob Member) {
            Member.doom(Delay);
            return;
        }
        Queue.put(Body.getUUID(), new Pending(World.dimension(), World.getGameTime() + Math.max(1, Delay)));
    }

    public static int rippleDelay(Entity Body, BlockPos From, RandomSource Random) {
        double Distance = Math.sqrt(Body.blockPosition().distSqr(From));
        return Mth.clamp((int) (Distance / 6.0) + Random.nextInt(12), 2, MaxDelay);
    }

    public static void tick(MinecraftServer Server) {
        if (Queue.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Pending>> Entries = Queue.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<UUID, Pending> Entry = Entries.next();
            ServerLevel World = Server.getLevel(Entry.getValue().Dimension());
            if (World == null) {
                Entries.remove();
                continue;
            }
            if (World.getGameTime() < Entry.getValue().Due()) {
                continue;
            }
            Entries.remove();
            Entity Body = World.getEntity(Entry.getKey());
            if (Body == null || Body.isRemoved()) {
                continue;
            }
            ashBurst(World, Body);
            if (Body instanceof LivingEntity Living) {
                Living.kill();
            } else {
                Body.discard();
            }
        }
    }

    public static void ashBurst(ServerLevel World, Entity Body) {
        double Middle = Body.getY() + Body.getBbHeight() * 0.5;
        World.sendParticles(Ash, Body.getX(), Middle, Body.getZ(), 40, Body.getBbWidth() * 0.5, Body.getBbHeight() * 0.35, Body.getBbWidth() * 0.5, 0.03);
        World.sendParticles(Cinder, Body.getX(), Middle, Body.getZ(), 24, Body.getBbWidth() * 0.5, Body.getBbHeight() * 0.35, Body.getBbWidth() * 0.5, 0.06);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Body.getX(), Body.getY() + 0.3, Body.getZ(), 6, 0.4, 0.1, 0.4, 0.01);
        World.playSound(null, Body.getX(), Body.getY(), Body.getZ(), DaeviteRegistry.Collapse.get(), SoundSource.HOSTILE, 1.2f, 0.8f + World.random.nextFloat() * 0.2f);
    }

    public static void forget() {
        Queue.clear();
    }

    public static Optional<Double> groundAt(ServerLevel World, double X, double Z, double Around) {
        BlockPos Pos = BlockPos.containing(X, Around + 2.0, Z);
        int Lowest = Mth.floor(Around) - 8;
        while (Pos.getY() >= Lowest && !World.isOutsideBuildHeight(Pos)) {
            BlockPos Below = Pos.below();
            BlockState Under = World.getBlockState(Below);
            if (Under.isFaceSturdy(World, Below, Direction.UP)) {
                double Offset = 0.0;
                if (!World.isEmptyBlock(Pos)) {
                    VoxelShape Shape = World.getBlockState(Pos).getCollisionShape(World, Pos);
                    if (!Shape.isEmpty()) {
                        Offset = Shape.max(Direction.Axis.Y);
                    }
                }
                if (Offset < 1.0) {
                    return Optional.of(Pos.getY() + Offset);
                }
            }
            Pos = Below;
        }
        return Optional.empty();
    }
}
