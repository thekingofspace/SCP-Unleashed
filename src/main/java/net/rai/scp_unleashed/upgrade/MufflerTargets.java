package net.rai.scp_unleashed.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.fusion.FusionLayout;
import net.rai.scp_unleashed.fusion.FusionReactorBlock;
import net.rai.scp_unleashed.plumbing.DrainBlock;
import net.rai.scp_unleashed.plumbing.DrainBlockEntity;
import net.rai.scp_unleashed.reactor.TowerSegmentBlock;
import net.rai.scp_unleashed.steam.SteamMachineBlock;
import net.rai.scp_unleashed.wire.WireHostBlock;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MufflerTargets {
    public static final int MaxCells = 1024;
    public static final int MaxSpan = 16;

    private MufflerTargets() {
    }

    public static boolean muffleable(BlockState State) {
        return State.is(SoundMuffling.Muffleable);
    }

    public static BlockPos coreOf(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        Block Kind = State.getBlock();
        if (Kind instanceof FusionReactorBlock) {
            return FusionLayout.masterOf(Pos, State).immutable();
        }
        if (Kind instanceof WireHostBlock Host) {
            return Host.devicePos(State, Pos).immutable();
        }
        if (Kind instanceof SteamMachineBlock Steam) {
            return Steam.masterPos(State, Pos).immutable();
        }
        if (Kind instanceof TowerSegmentBlock) {
            List<BlockPos> Column = TowerSegmentBlock.column(World, Pos, Kind);
            if (!Column.isEmpty()) {
                BlockPos Lowest = Column.get(0);
                for (BlockPos Cell : Column) {
                    if (Cell.getY() < Lowest.getY()) {
                        Lowest = Cell;
                    }
                }
                return Lowest.immutable();
            }
        }
        return Pos.immutable();
    }

    public static Set<BlockPos> footprint(Level World, BlockPos Core) {
        Block Kind = World.getBlockState(Core).getBlock();
        Set<BlockPos> Found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> Frontier = new ArrayDeque<>();
        Found.add(Core);
        Frontier.add(Core);
        while (!Frontier.isEmpty() && Found.size() < MaxCells) {
            BlockPos Cell = Frontier.poll();
            for (Direction Side : Direction.values()) {
                BlockPos Next = Cell.relative(Side);
                if (Found.contains(Next) || Next.distManhattan(Core) > MaxSpan * 3 || !World.isLoaded(Next)) {
                    continue;
                }
                if (World.getBlockState(Next).is(Kind) && coreOf(World, Next).equals(Core)) {
                    Found.add(Next.immutable());
                    Frontier.add(Next.immutable());
                }
            }
        }
        if (Kind instanceof DrainBlock) {
            for (int Depth = 1; Depth <= DrainBlockEntity.MaxReach + 1; Depth++) {
                Found.add(Core.below(Depth));
            }
        }
        return Found;
    }
}
