package net.rai.scp_unleashed.scp682;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class Scp682Lake {
    public static final int SearchRadius = 48;
    public static final int ColumnStep = 3;
    public static final int MinDepth = 3;
    public static final int MaxDepthScan = 24;
    public static final int SpreadCheck = 4;
    public static final int MinSpread = 6;
    public static final double CueRange = 160.0;
    public static final float CueVolume = 8.0f;

    private Scp682Lake() {
    }

    public static boolean water(ServerLevel World, BlockPos Pos) {
        return World.isLoaded(Pos) && World.getFluidState(Pos).is(FluidTags.WATER);
    }

    public static int depth(ServerLevel World, BlockPos Surface) {
        BlockPos.MutableBlockPos Cursor = Surface.mutable();
        int Depth = 0;
        while (Depth < MaxDepthScan && water(World, Cursor)) {
            Depth++;
            Cursor.move(0, -1, 0);
        }
        return Depth;
    }

    public static int spread(ServerLevel World, BlockPos Surface) {
        int Count = 0;
        for (int Dx = -SpreadCheck; Dx <= SpreadCheck; Dx += 2) {
            for (int Dz = -SpreadCheck; Dz <= SpreadCheck; Dz += 2) {
                if (water(World, Surface.offset(Dx, 0, Dz)) && water(World, Surface.offset(Dx, -1, Dz))) {
                    Count++;
                }
            }
        }
        return Count;
    }

    @Nullable
    public static BlockPos surface(ServerLevel World, int X, int Z, int NearY) {
        BlockPos Top = new BlockPos(X, World.getHeight(Heightmap.Types.WORLD_SURFACE, X, Z) - 1, Z);
        if (water(World, Top)) {
            return Top;
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, NearY + MaxDepthScan, Z);
        for (int Step = 0; Step < MaxDepthScan * 2; Step++) {
            if (water(World, Cursor) && !water(World, Cursor.above())) {
                return Cursor.immutable();
            }
            Cursor.move(0, -1, 0);
        }
        return null;
    }

    public static Optional<BlockPos> find(ServerLevel World, BlockPos Centre, int Radius) {
        BlockPos Best = null;
        double BestScore = Double.MAX_VALUE;
        for (int Dx = -Radius; Dx <= Radius; Dx += ColumnStep) {
            for (int Dz = -Radius; Dz <= Radius; Dz += ColumnStep) {
                int X = Centre.getX() + Dx;
                int Z = Centre.getZ() + Dz;
                BlockPos Column = new BlockPos(X, Centre.getY(), Z);
                if (!World.isLoaded(Column)) {
                    continue;
                }
                BlockPos Surface = surface(World, X, Z, Centre.getY());
                if (Surface == null) {
                    continue;
                }
                int Depth = depth(World, Surface);
                if (Depth < MinDepth || spread(World, Surface) < MinSpread) {
                    continue;
                }
                double Score = Math.sqrt(Dx * Dx + Dz * Dz) - Depth * 3.0;
                if (Score < BestScore) {
                    BestScore = Score;
                    Best = Surface.below(Math.max(1, Depth - 2));
                }
            }
        }
        return Optional.ofNullable(Best);
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Where = Found.Pos();
        Scp682 Reptile = Scp682Registry.Scp682Type.get().create(World);
        if (Reptile == null) {
            return false;
        }
        Reptile.moveTo(Where.getX() + 0.5, Where.getY(), Where.getZ() + 0.5, World.getRandom().nextFloat() * 360.0f, 0.0f);
        Reptile.finalizeSpawn(World, World.getCurrentDifficultyAt(Where), MobSpawnType.EVENT, null);
        Reptile.setPersistenceRequired();
        if (!World.addFreshEntity(Reptile)) {
            return false;
        }
        cue(World, Reptile, Found.Investigator());
        return true;
    }

    public static void cue(ServerLevel World, Scp682 Reptile, ServerPlayer Reader) {
        Vec3 At = Reptile.position();
        int Top = World.getHeight(Heightmap.Types.WORLD_SURFACE, Reptile.getBlockX(), Reptile.getBlockZ());
        World.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, At.x, At.y + 1.0, At.z, 80, 1.6, 1.0, 1.6, 0.2);
        World.sendParticles(ParticleTypes.SPLASH, At.x, Top + 0.2, At.z, 120, 2.4, 0.2, 2.4, 0.3);
        World.sendParticles(ParticleTypes.CLOUD, At.x, Top + 0.4, At.z, 30, 2.0, 0.3, 2.0, 0.02);
        for (ServerPlayer Listener : World.players()) {
            if (Listener.distanceToSqr(At) > CueRange * CueRange) {
                continue;
            }
            Vec3 Toward = LeadPlanner.towards(Listener, At.x, At.z, 12.0);
            for (Holder<SoundEvent> Layer : List.<Holder<SoundEvent>>of(Scp682Registry.Emerge, Scp682Registry.EmergeRumble)) {
                Listener.connection.send(new ClientboundSoundPacket(Layer, SoundSource.HOSTILE, Toward.x, Toward.y, Toward.z, 1.4f, 0.85f, World.getRandom().nextLong()));
            }
        }
        World.playSound(null, At.x, At.y, At.z, Scp682Registry.Roar.get(), SoundSource.HOSTILE, CueVolume, 0.7f);
        if (!Reader.isCreative() && !Reader.isSpectator() && Reader.distanceTo(Reptile) <= Reptile.getAttributeValue(Attributes.FOLLOW_RANGE) * 1.5) {
            Reptile.setTarget(Reader);
        }
    }
}
