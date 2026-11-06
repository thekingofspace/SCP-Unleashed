package net.rai.scp_unleashed.scp009;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class RedIceGrowth {
    public static final int SearchReach = 1;
    public static final int MaxAge = 200;
    public static final int CrackleEvery = 2;
    private static final Map<ResourceKey<Level>, List<Job>> Active = new HashMap<>();

    private RedIceGrowth() {
    }

    public static boolean isWater(BlockState State) {
        return State.is(Blocks.WATER) || State.is(Blocks.BUBBLE_COLUMN);
    }

    public static boolean start(ServerLevel World, Vec3 At, RedIceGrade Grade) {
        BlockPos Origin = findWater(World, At);
        if (Origin == null) {
            return false;
        }
        Active.computeIfAbsent(World.dimension(), Key -> new ArrayList<>()).add(new Job(Origin, Grade));
        World.playSound(null, At.x, At.y, At.z, Scp009Registry.Freeze.get(), SoundSource.BLOCKS, 1.0f, 0.95f + World.random.nextFloat() * 0.1f);
        World.playSound(null, At.x, At.y, At.z, Scp009Registry.FreezeChime.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        World.sendParticles(Scp009Registry.Glint.get(), At.x, At.y + 0.2, At.z, 18, 0.25, 0.15, 0.25, 0.05);
        return true;
    }

    @Nullable
    private static BlockPos findWater(ServerLevel World, Vec3 At) {
        BlockPos Base = BlockPos.containing(At);
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Candidate : BlockPos.betweenClosed(Base.offset(-SearchReach, -SearchReach, -SearchReach), Base.offset(SearchReach, SearchReach, SearchReach))) {
            if (!isWater(World.getBlockState(Candidate))) {
                continue;
            }
            double Distance = Vec3.atCenterOf(Candidate).distanceToSqr(At);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Candidate.immutable();
            }
        }
        return Best;
    }

    public static void tick(ServerLevel World) {
        List<Job> Jobs = Active.get(World.dimension());
        if (Jobs == null || Jobs.isEmpty()) {
            return;
        }
        Jobs.removeIf(Pending -> Pending.step(World));
    }

    public static int pending() {
        int Count = 0;
        for (List<Job> Jobs : Active.values()) {
            Count += Jobs.size();
        }
        return Count;
    }

    public static void clear() {
        Active.clear();
    }

    private static final class Job {
        private final BlockPos Origin;
        private final RedIceGrade Grade;
        private final PriorityQueue<BlockPos> Frontier;
        private final Set<Long> Seen = new HashSet<>();
        private int Converted;
        private int Age;

        private Job(BlockPos Origin, RedIceGrade Grade) {
            this.Origin = Origin;
            this.Grade = Grade;
            this.Frontier = new PriorityQueue<>(Comparator.<BlockPos>comparingDouble(Origin::distSqr).thenComparingLong(BlockPos::asLong));
            Frontier.add(Origin);
            Seen.add(Origin.asLong());
        }

        private boolean step(ServerLevel World) {
            Age++;
            int Budget = Grade.getPerTick();
            double Reach = (double) Grade.getRadius() * Grade.getRadius();
            BlockPos Last = null;
            while (Budget > 0 && !Frontier.isEmpty() && Converted < Grade.getMaxBlocks()) {
                BlockPos Next = Frontier.poll();
                if (!World.isLoaded(Next) || !isWater(World.getBlockState(Next)) || isOccupied(World, Next)) {
                    continue;
                }
                World.setBlockAndUpdate(Next, Scp009Registry.RedIce.get().defaultBlockState());
                Converted++;
                Budget--;
                Last = Next;
                World.sendParticles(Scp009Registry.Glint.get(), Next.getX() + 0.5, Next.getY() + 0.9, Next.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.02);
                for (Direction Side : Direction.values()) {
                    BlockPos Near = Next.relative(Side);
                    if (Near.distSqr(Origin) <= Reach && Seen.add(Near.asLong()) && World.isLoaded(Near) && isWater(World.getBlockState(Near))) {
                        Frontier.add(Near);
                    }
                }
            }
            if (Last != null && Age % CrackleEvery == 1) {
                World.playSound(null, Last, Scp009Registry.Crackle.get(), SoundSource.BLOCKS, 0.7f, 0.85f + World.random.nextFloat() * 0.3f);
            }
            return Frontier.isEmpty() || Converted >= Grade.getMaxBlocks() || Age >= MaxAge;
        }

        private static boolean isOccupied(ServerLevel World, BlockPos Pos) {
            return !World.getEntitiesOfClass(LivingEntity.class, new AABB(Pos), LivingEntity::isAlive).isEmpty();
        }
    }
}
