package net.rai.scp_unleashed.scp149;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Scp149Escape {
    public static final int FillLimit = 700;
    public static final int FillRadius = 12;
    public static final float TicksPerHardness = 100.0f;
    public static final int MinChew = 20;
    public static final float HardestChew = 30.0f;
    public static final float SturdiestChew = 600.0f;
    public static final double TowardBonus = 0.4;
    public static final double DistanceWeight = 0.08;
    private static final Map<ResourceKey<Level>, Map<BlockPos, Float>> Progress = new HashMap<>();

    private Scp149Escape() {
    }

    public record Plan(BlockPos Block, BlockPos Perch) {
    }

    public static boolean passable(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.getCollisionShape(World, Pos).isEmpty();
    }

    public static boolean chewable(ServerLevel World, BlockPos Pos, Scp149Fly Fly) {
        BlockState State = World.getBlockState(Pos);
        if (State.isAir() || !State.getFluidState().isEmpty() && State.getCollisionShape(World, Pos).isEmpty()) {
            return false;
        }
        float Hardness = State.getDestroySpeed(World, Pos);
        if (Hardness < 0.0f || Hardness >= HardestChew || State.getBlock().getExplosionResistance() >= SturdiestChew) {
            return false;
        }
        if (State.is(BlockTags.WITHER_IMMUNE) || State.is(BlockTags.DRAGON_IMMUNE)) {
            return false;
        }
        return State.canEntityDestroy(World, Pos, Fly);
    }

    public static int chewTicks(ServerLevel World, BlockPos Pos) {
        float Hardness = World.getBlockState(Pos).getDestroySpeed(World, Pos);
        return Math.max(MinChew, Math.round(Hardness * TicksPerHardness));
    }

    @Nullable
    public static Plan plan(ServerLevel World, Scp149Fly Fly, @Nullable Vec3 Toward) {
        BlockPos Start = Fly.blockPosition();
        if (!passable(World, Start)) {
            Start = Fly.blockPosition().above();
        }
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        Set<BlockPos> Seen = new HashSet<>();
        Map<BlockPos, BlockPos> Walls = new HashMap<>();
        Queue.add(Start);
        Seen.add(Start);
        while (!Queue.isEmpty()) {
            BlockPos Cell = Queue.poll();
            if (World.canSeeSky(Cell) || Cell.distManhattan(Start) > FillRadius * 2 || !World.isLoaded(Cell)) {
                return null;
            }
            for (Direction Side : Direction.values()) {
                BlockPos Next = Cell.relative(Side);
                if (Seen.contains(Next)) {
                    continue;
                }
                if (passable(World, Next)) {
                    if (Seen.size() >= FillLimit) {
                        continue;
                    }
                    Seen.add(Next);
                    Queue.add(Next);
                } else if (!Walls.containsKey(Next)) {
                    Walls.put(Next, Cell);
                }
            }
        }
        Plan Best = null;
        double BestScore = Double.MAX_VALUE;
        Vec3 Eye = Fly.position();
        for (Map.Entry<BlockPos, BlockPos> Entry : Walls.entrySet()) {
            BlockPos Wall = Entry.getKey();
            if (!chewable(World, Wall, Fly)) {
                continue;
            }
            double Score = World.getBlockState(Wall).getDestroySpeed(World, Wall);
            Vec3 Centre = Vec3.atCenterOf(Wall);
            Score += Centre.distanceTo(Eye) * DistanceWeight;
            if (Toward != null) {
                Vec3 Aim = Toward.subtract(Eye);
                Vec3 Way = Centre.subtract(Eye);
                if (Aim.lengthSqr() > 1.0e-4 && Way.lengthSqr() > 1.0e-4) {
                    Score -= TowardBonus * Math.max(0.0, Aim.normalize().dot(Way.normalize()));
                }
            } else if (Wall.getY() > Fly.getBlockY()) {
                Score -= TowardBonus * 0.5;
            }
            if (Score < BestScore) {
                BestScore = Score;
                Best = new Plan(Wall.immutable(), Entry.getValue().immutable());
            }
        }
        return Best;
    }

    public static boolean gnaw(ServerLevel World, BlockPos Pos, Scp149Fly Fly) {
        if (!chewable(World, Pos, Fly)) {
            forget(World, Pos);
            return false;
        }
        Map<BlockPos, Float> Bites = Progress.computeIfAbsent(World.dimension(), Key -> new HashMap<>());
        float Done = Bites.getOrDefault(Pos, 0.0f) + 1.0f / chewTicks(World, Pos);
        if (Done < 1.0f) {
            Bites.put(Pos.immutable(), Done);
            World.destroyBlockProgress(Fly.getId(), Pos, Math.min(9, (int) (Done * 10.0f)));
            return false;
        }
        forget(World, Pos);
        World.destroyBlockProgress(Fly.getId(), Pos, -1);
        BlockState State = World.getBlockState(Pos);
        if (!EventHooks.onEntityDestroyBlock(Fly, Pos, State)) {
            return false;
        }
        World.destroyBlock(Pos, true, Fly);
        return true;
    }

    public static void forget(ServerLevel World, BlockPos Pos) {
        Map<BlockPos, Float> Bites = Progress.get(World.dimension());
        if (Bites != null) {
            Bites.remove(Pos);
            if (Bites.isEmpty()) {
                Progress.remove(World.dimension());
            }
        }
    }

    public static void clear() {
        Progress.clear();
    }
}
