package net.rai.scp_unleashed.scp682;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scp682Smash {
    public static final float TicksPerHardness = 3.0f;
    public static final int MinTicks = 2;
    public static final int MaxTicks = 60;
    public static final int HitGap = 5;
    public static final double Reach = 0.8;
    public static final double FloorClear = 0.5;
    public static final int Forgotten = 128;
    public static final int SmashLimit = 12;

    private final Map<Long, Float> Progress = new HashMap<>();
    private int HitReady;

    public static boolean solid(ServerLevel World, BlockPos Pos, BlockState State) {
        return !State.isAir() && !State.getCollisionShape(World, Pos).isEmpty();
    }

    public static int required(ServerLevel World, BlockPos Pos, BlockState State, Scp682Size Size) {
        if (!Size.breaksBlocks() || State.is(Scp682Registry.Unbreakable) || State.is(BlockTags.WITHER_IMMUNE) || State.is(BlockTags.DRAGON_IMMUNE)) {
            return -1;
        }
        float Hardness = State.getDestroySpeed(World, Pos);
        if (Hardness < 0.0f || Hardness > Size.breakHardness()) {
            return -1;
        }
        return Mth.clamp(Mth.ceil(Hardness * TicksPerHardness), MinTicks, MaxTicks);
    }

    public static boolean allowed(ServerLevel World, Scp682 Reptile) {
        return Reptile.size().breaksBlocks() && EventHooks.canEntityGrief(World, Reptile);
    }

    public static List<BlockPos> ahead(ServerLevel World, Scp682 Reptile, Vec3 Heading) {
        Vec3 Flat = new Vec3(Heading.x, 0.0, Heading.z);
        if (Flat.lengthSqr() < 1.0e-4) {
            Flat = Vec3.directionFromRotation(0.0f, Reptile.getYRot());
        }
        Flat = Flat.normalize();
        AABB Body = Reptile.getBoundingBox();
        double Push = Reach + Body.getXsize() * 0.5;
        AABB Front = Body.move(Flat.scale(Push)).deflate(0.05, 0.0, 0.05).setMinY(Body.minY + FloorClear);
        Front = Front.setMaxY(Body.maxY + 0.4);
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Each : BlockPos.betweenClosed(BlockPos.containing(Front.minX, Front.minY, Front.minZ), BlockPos.containing(Front.maxX, Front.maxY - 1.0e-3, Front.maxZ))) {
            BlockState State = World.getBlockState(Each);
            if (solid(World, Each, State)) {
                Found.add(Each.immutable());
            }
        }
        return Found;
    }

    public boolean tear(ServerLevel World, Scp682 Reptile, Vec3 Heading) {
        if (!allowed(World, Reptile)) {
            return false;
        }
        List<BlockPos> Found = ahead(World, Reptile, Heading);
        if (Found.isEmpty()) {
            return false;
        }
        boolean Hit = --HitReady <= 0;
        if (Hit) {
            HitReady = HitGap;
        }
        boolean Working = false;
        int Broken = 0;
        for (BlockPos Pos : Found) {
            BlockState State = World.getBlockState(Pos);
            int Need = required(World, Pos, State, Reptile.size());
            if (Hit) {
                knock(World, Reptile, Pos, State, Need >= 0);
            }
            if (Need < 0) {
                continue;
            }
            Working = true;
            float Done = Progress.merge(Pos.asLong(), 1.0f / Need, Float::sum);
            if (Done >= 1.0f && Broken < SmashLimit) {
                Progress.remove(Pos.asLong());
                World.destroyBlockProgress(breaker(Reptile, Pos), Pos, -1);
                World.destroyBlock(Pos, true, Reptile);
                Broken++;
            } else if (Done < 1.0f) {
                World.destroyBlockProgress(breaker(Reptile, Pos), Pos, Mth.clamp((int) (Done * 10.0f), 0, 9));
            }
        }
        if (Progress.size() > Forgotten) {
            Progress.keySet().removeIf(Key -> !solid(World, BlockPos.of(Key), World.getBlockState(BlockPos.of(Key))));
        }
        return Working;
    }

    private static void knock(ServerLevel World, Scp682 Reptile, BlockPos Pos, BlockState State, boolean Giving) {
        Vec3 Centre = Vec3.atCenterOf(Pos);
        SoundType Sound = State.getSoundType(World, Pos, Reptile);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Centre.x, Centre.y, Centre.z, 6, 0.3, 0.3, 0.3, 0.1);
        World.playSound(null, Pos, Sound.getHitSound(), SoundSource.BLOCKS, Sound.getVolume() * 1.4f, Sound.getPitch() * 0.7f);
        if (Giving) {
            World.playSound(null, Pos, Scp682Registry.Smash.get(), SoundSource.HOSTILE, 1.0f, 0.8f + World.getRandom().nextFloat() * 0.2f);
        }
    }

    private static int breaker(Scp682 Reptile, BlockPos Pos) {
        return -1 - (Reptile.getId() * 31 + Math.floorMod(Pos.hashCode(), 31));
    }

    public void clear(ServerLevel World, Scp682 Reptile) {
        for (Long Key : Progress.keySet()) {
            BlockPos Pos = BlockPos.of(Key);
            World.destroyBlockProgress(breaker(Reptile, Pos), Pos, -1);
        }
        Progress.clear();
    }
}
