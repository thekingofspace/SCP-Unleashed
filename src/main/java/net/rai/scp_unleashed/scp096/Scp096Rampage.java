package net.rai.scp_unleashed.scp096;

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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scp096Rampage {
    public static final float HardnessCap = 30.0f;
    public static final float TicksPerHardness = 1.5f;
    public static final int MinTicks = 1;
    public static final int MaxTicks = 40;
    public static final int SmashLimit = 64;
    public static final double LookAhead = 0.7;
    public static final int HitGap = 4;
    public static final int Forgotten = 96;
    public static final double FootClear = 0.01;
    public static final double Snag = 1.0e-7;

    private final Map<Long, Float> Progress = new HashMap<>();
    private int HitReady;

    public static boolean solid(ServerLevel World, BlockPos Pos, BlockState State) {
        return !State.isAir() && !State.getCollisionShape(World, Pos).isEmpty();
    }

    public static int required(ServerLevel World, BlockPos Pos, BlockState State) {
        if (State.is(Scp096Registry.RageProof) || State.is(BlockTags.WITHER_IMMUNE)) {
            return -1;
        }
        float Hardness = State.getDestroySpeed(World, Pos);
        if (Hardness < 0.0f || Hardness > HardnessCap) {
            return -1;
        }
        return Mth.clamp(Mth.ceil(Hardness * TicksPerHardness), MinTicks, MaxTicks);
    }

    public static List<BlockPos> blocking(ServerLevel World, Scp096ShyGuy Guy, Vec3 Heading, boolean Overhead) {
        Vec3 Flat = new Vec3(Heading.x, 0.0, Heading.z);
        if (Flat.lengthSqr() < 1.0e-4) {
            Flat = Vec3.directionFromRotation(0.0f, Guy.getYRot());
        }
        Flat = Flat.normalize();
        AABB Body = Guy.getBoundingBox();
        AABB Ahead = Body.move(Flat.scale(LookAhead)).deflate(0.05, 0.0, 0.05).setMinY(Body.minY + 0.55);
        if (Overhead) {
            Ahead = Ahead.expandTowards(0.0, 1.0, 0.0);
        }
        return solids(World, Ahead);
    }

    public static List<BlockPos> solids(ServerLevel World, AABB Area) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Each : BlockPos.betweenClosed(BlockPos.containing(Area.minX, Area.minY, Area.minZ), BlockPos.containing(Area.maxX, Area.maxY - 1.0e-3, Area.maxZ))) {
            BlockState State = World.getBlockState(Each);
            if (solid(World, Each, State)) {
                Found.add(Each.immutable());
            }
        }
        return Found;
    }

    public static AABB lift(AABB Body, double Rise, double Headroom) {
        return new AABB(Body.minX + Snag, Body.minY + FootClear, Body.minZ + Snag, Body.maxX - Snag, Body.maxY + Math.max(0.0, Rise) + Headroom, Body.maxZ - Snag);
    }

    public static List<BlockPos> touching(ServerLevel World, Scp096ShyGuy Guy, AABB Area) {
        List<BlockPos> Found = new ArrayList<>();
        CollisionContext Context = CollisionContext.of(Guy);
        BlockPos Low = BlockPos.containing(Area.minX, Area.minY - 1.0, Area.minZ);
        BlockPos High = BlockPos.containing(Area.maxX, Area.maxY, Area.maxZ);
        for (BlockPos Each : BlockPos.betweenClosed(Low, High)) {
            BlockState State = World.getBlockState(Each);
            if (State.isAir()) {
                continue;
            }
            VoxelShape Shape = State.getCollisionShape(World, Each, Context);
            if (Shape.isEmpty()) {
                continue;
            }
            for (AABB Part : Shape.toAabbs()) {
                if (Part.move(Each).intersects(Area)) {
                    Found.add(Each.immutable());
                    break;
                }
            }
        }
        return Found;
    }

    public boolean tearBelow(ServerLevel World, Scp096ShyGuy Guy) {
        AABB Body = Guy.getBoundingBox();
        AABB Floor = new AABB(Body.minX + 0.05, Body.minY - 0.6, Body.minZ + 0.05, Body.maxX - 0.05, Body.minY - 0.01, Body.maxZ - 0.05);
        return work(World, Guy, solids(World, Floor));
    }

    public boolean soar(ServerLevel World, Scp096ShyGuy Guy, AABB Body, double Rise, double Headroom) {
        if (!EventHooks.canEntityGrief(World, Guy)) {
            return false;
        }
        AABB Area = lift(Body, Rise, Headroom);
        CollisionContext Context = CollisionContext.of(Guy);
        List<BlockPos> Found = touching(World, Guy, Area);
        double Ceiling = Area.maxY;
        for (BlockPos Pos : Found) {
            BlockState State = World.getBlockState(Pos);
            if (required(World, Pos, State) >= 0) {
                continue;
            }
            for (AABB Part : State.getCollisionShape(World, Pos, Context).toAabbs()) {
                AABB Placed = Part.move(Pos);
                if (Placed.intersects(Area) && Placed.minY >= Body.maxY - Snag) {
                    Ceiling = Math.min(Ceiling, Placed.minY);
                }
            }
        }
        AABB Reach = new AABB(Area.minX, Area.minY, Area.minZ, Area.maxX, Math.max(Area.minY, Ceiling), Area.maxZ);
        boolean Broke = false;
        int Count = 0;
        for (BlockPos Pos : Found) {
            if (Count >= SmashLimit) {
                break;
            }
            BlockState State = World.getBlockState(Pos);
            if (State.isAir() || required(World, Pos, State) < 0 || !reaches(World, Pos, State, Context, Reach)) {
                continue;
            }
            Progress.remove(Pos.asLong());
            World.destroyBlockProgress(breaker(Guy, Pos), Pos, -1);
            World.destroyBlock(Pos, true, Guy);
            Broke = true;
            Count++;
        }
        if (Broke && --HitReady <= 0) {
            HitReady = HitGap;
            World.playSound(null, Guy.getX(), Guy.getY() + Guy.getBbHeight(), Guy.getZ(), Scp096Registry.Tear.get(), SoundSource.HOSTILE, 1.4f, 0.8f + World.getRandom().nextFloat() * 0.2f);
        }
        return Broke;
    }

    public static double room(ServerLevel World, Scp096ShyGuy Guy, AABB Body, double Reach) {
        AABB Area = new AABB(Body.minX + Snag, Body.maxY, Body.minZ + Snag, Body.maxX - Snag, Body.maxY + Reach, Body.maxZ - Snag);
        CollisionContext Context = CollisionContext.of(Guy);
        double Free = Reach;
        for (BlockPos Pos : touching(World, Guy, Area)) {
            for (AABB Part : World.getBlockState(Pos).getCollisionShape(World, Pos, Context).toAabbs()) {
                AABB Placed = Part.move(Pos);
                if (Placed.intersects(Area) && Placed.minY >= Body.maxY - Snag) {
                    Free = Math.min(Free, Placed.minY - Body.maxY);
                }
            }
        }
        return Math.max(0.0, Free);
    }

    private static boolean reaches(ServerLevel World, BlockPos Pos, BlockState State, CollisionContext Context, AABB Area) {
        for (AABB Part : State.getCollisionShape(World, Pos, Context).toAabbs()) {
            if (Part.move(Pos).intersects(Area)) {
                return true;
            }
        }
        return false;
    }

    public boolean tear(ServerLevel World, Scp096ShyGuy Guy, Vec3 Heading, boolean Overhead) {
        return work(World, Guy, blocking(World, Guy, Heading, Overhead));
    }

    private boolean work(ServerLevel World, Scp096ShyGuy Guy, List<BlockPos> Found) {
        if (Found.isEmpty()) {
            return false;
        }
        boolean Allowed = EventHooks.canEntityGrief(World, Guy);
        boolean Working = false;
        boolean Hit = --HitReady <= 0;
        if (Hit) {
            HitReady = HitGap;
        }
        for (BlockPos Pos : Found) {
            BlockState State = World.getBlockState(Pos);
            int Need = required(World, Pos, State);
            if (Hit) {
                knock(World, Guy, Pos, State, Need >= 0 && Allowed);
            }
            if (Need < 0 || !Allowed) {
                continue;
            }
            Working = true;
            float Done = Progress.merge(Pos.asLong(), 1.0f / Need, Float::sum);
            if (Done >= 1.0f) {
                Progress.remove(Pos.asLong());
                World.destroyBlockProgress(breaker(Guy, Pos), Pos, -1);
                World.destroyBlock(Pos, true, Guy);
            } else {
                World.destroyBlockProgress(breaker(Guy, Pos), Pos, Mth.clamp((int) (Done * 10.0f), 0, 9));
            }
        }
        if (Progress.size() > Forgotten) {
            Progress.keySet().removeIf(Key -> !solid(World, BlockPos.of(Key), World.getBlockState(BlockPos.of(Key))));
        }
        return Working;
    }

    private static void knock(ServerLevel World, Scp096ShyGuy Guy, BlockPos Pos, BlockState State, boolean Giving) {
        Vec3 Centre = Vec3.atCenterOf(Pos);
        SoundType Sound = State.getSoundType(World, Pos, Guy);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Centre.x, Centre.y, Centre.z, 8, 0.3, 0.3, 0.3, 0.1);
        World.playSound(null, Pos, Sound.getHitSound(), SoundSource.BLOCKS, Sound.getVolume() * 1.3f, Sound.getPitch() * 0.75f);
        float Pitch = 0.9f + World.getRandom().nextFloat() * 0.2f;
        World.playSound(null, Pos, Giving ? Scp096Registry.Tear.get() : Scp096Registry.TearCrack.get(), SoundSource.HOSTILE, 1.1f, Pitch);
    }

    private static int breaker(Scp096ShyGuy Guy, BlockPos Pos) {
        return -1 - (Guy.getId() * 31 + Math.floorMod(Pos.hashCode(), 31));
    }

    public void clear(ServerLevel World, Scp096ShyGuy Guy) {
        for (Long Key : Progress.keySet()) {
            BlockPos Pos = BlockPos.of(Key);
            World.destroyBlockProgress(breaker(Guy, Pos), Pos, -1);
        }
        Progress.clear();
    }
}
