package net.rai.scp_unleashed.fluiddump;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

public final class DryPour {
    public static final int Reach = 5;
    public static final int Depth = 6;
    public static final int NodeCap = 1600;

    private DryPour() {
    }

    private enum Cell {
        Open,
        Pool,
        Wall
    }

    public static Fluid still(Fluid Liquid) {
        return Liquid instanceof FlowingFluid Flow ? Flow.getSource() : Liquid;
    }

    @Nullable
    public static BlockPos find(ServerLevel World, BlockPos Origin, Fluid Liquid) {
        Fluid Still = still(Liquid);
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        LongOpenHashSet Seen = new LongOpenHashSet();
        Seen.add(Origin.asLong());
        for (Direction Side : Direction.values()) {
            if (Side != Direction.UP) {
                BlockPos Start = Origin.relative(Side);
                Seen.add(Start.asLong());
                Queue.add(Start);
            }
        }
        BlockPos Best = null;
        long BestScore = Long.MAX_VALUE;
        while (!Queue.isEmpty() && Seen.size() < NodeCap) {
            BlockPos Pos = Queue.poll();
            if (!within(Origin, Pos) || !World.isLoaded(Pos)) {
                continue;
            }
            Cell Kind = classify(World.getBlockState(Pos), Still);
            if (Kind == Cell.Wall) {
                continue;
            }
            if (Kind == Cell.Open && supported(World, Pos, Still)) {
                long Score = score(Origin, Pos);
                if (Score < BestScore) {
                    BestScore = Score;
                    Best = Pos;
                }
            }
            for (Direction Side : Direction.values()) {
                if (Side == Direction.UP && Pos.getY() >= Origin.getY()) {
                    continue;
                }
                BlockPos Next = Pos.relative(Side);
                if (Seen.add(Next.asLong())) {
                    Queue.add(Next);
                }
            }
        }
        return Best;
    }

    private static boolean within(BlockPos Origin, BlockPos Pos) {
        return Math.abs(Pos.getX() - Origin.getX()) <= Reach && Math.abs(Pos.getZ() - Origin.getZ()) <= Reach
                && Pos.getY() <= Origin.getY() && Pos.getY() >= Origin.getY() - Depth;
    }

    private static long score(BlockPos Origin, BlockPos Pos) {
        long Dx = Pos.getX() - Origin.getX();
        long Dz = Pos.getZ() - Origin.getZ();
        return (Pos.getY() - (Origin.getY() - Depth)) * 1_000_000L + Dx * Dx + Dz * Dz;
    }

    private static Cell classify(BlockState State, Fluid Still) {
        FluidState Liquid = State.getFluidState();
        if (!Liquid.isEmpty()) {
            if (!Liquid.getType().isSame(Still)) {
                return Cell.Wall;
            }
            return Liquid.isSource() ? Cell.Pool : Cell.Open;
        }
        return State.isAir() || State.canBeReplaced() ? Cell.Open : Cell.Wall;
    }

    private static boolean supported(ServerLevel World, BlockPos Pos, Fluid Still) {
        BlockPos Below = Pos.below();
        if (!World.isLoaded(Below)) {
            return false;
        }
        BlockState State = World.getBlockState(Below);
        FluidState Liquid = State.getFluidState();
        if (!Liquid.isEmpty()) {
            return Liquid.isSourceOfType(Still);
        }
        return !State.isAir() && !State.canBeReplaced();
    }
}
