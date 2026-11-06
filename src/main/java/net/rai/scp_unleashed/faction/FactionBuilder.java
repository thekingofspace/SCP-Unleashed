package net.rai.scp_unleashed.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FactionBuilder {
    public static final int Flags = Block.UPDATE_CLIENTS;

    private FactionBuilder() {
    }

    private record Late(BlockPos Pos, BlockState State, FactionPlan.Entry Source) {
    }

    public static Map<String, List<BlockPos>> place(ServerLevel World, FactionPlan Plan, BlockPos Origin, Direction Front, long Seed) {
        Rotation Turn = FactionGround.turnFor(Front);
        RandomSource Random = RandomSource.create(Seed);
        BlockState Empty = Blocks.AIR.defaultBlockState();
        List<Late> Deferred = new ArrayList<>();
        for (int Z = 0; Z < Plan.depth(); Z++) {
            for (int X = 0; X < Plan.width(); X++) {
                int Lowest = -1;
                for (int Y = 0; Y < Plan.height(); Y++) {
                    if (Plan.cell(X, Y, Z) != FactionPlan.Skip) {
                        Lowest = Y;
                        break;
                    }
                }
                if (Lowest < 0) {
                    continue;
                }
                for (int Y = Lowest; Y < Plan.height(); Y++) {
                    char Key = Plan.cell(X, Y, Z);
                    if (Key == FactionPlan.Skip) {
                        continue;
                    }
                    FactionPlan.Entry Spec = Plan.entry(Key);
                    if (Spec == null) {
                        continue;
                    }
                    FactionPlan.Choice Picked = Spec.pick(Seed, X, Y, Z);
                    if (Picked == null || Picked.Leave() || Picked.State() == null) {
                        continue;
                    }
                    BlockPos Pos = FactionGround.toWorld(Origin, Front, X - Plan.anchorX(), Y - Plan.anchorY(), Z - Plan.anchorZ());
                    if (World.isOutsideBuildHeight(Pos)) {
                        continue;
                    }
                    BlockState State = Picked.State().rotate(World, Pos, Turn);
                    if (Spec.Late()) {
                        Deferred.add(new Late(Pos, State, Spec));
                        if (!World.getBlockState(Pos).isAir()) {
                            World.setBlock(Pos, Empty, Flags);
                        }
                        continue;
                    }
                    if (State.isAir() && World.getBlockState(Pos).isAir()) {
                        continue;
                    }
                    World.setBlock(Pos, State, Flags);
                    if (Spec.Loot() != null) {
                        RandomizableContainer.setBlockEntityLootTable(World, Random, Pos, Spec.Loot());
                    }
                    if (Y == Lowest && Spec.Footing()) {
                        foot(World, Plan, Pos, State);
                    }
                }
                BlockPos Top = FactionGround.toWorld(Origin, Front, X - Plan.anchorX(), Plan.height() - Plan.anchorY(), Z - Plan.anchorZ());
                for (int Extra = 0; Extra < Plan.clear(); Extra++) {
                    BlockPos Above = Top.above(Extra);
                    if (World.isOutsideBuildHeight(Above)) {
                        break;
                    }
                    BlockState There = World.getBlockState(Above);
                    if (!There.isAir() && FactionGround.isGrowth(There)) {
                        World.setBlock(Above, Empty, Flags);
                    }
                }
            }
        }
        for (Late Pending : Deferred) {
            if (Pending.State().isAir()) {
                continue;
            }
            World.setBlock(Pending.Pos(), Pending.State(), Flags);
            if (Pending.Source().Loot() != null) {
                RandomizableContainer.setBlockEntityLootTable(World, Random, Pending.Pos(), Pending.Source().Loot());
            }
        }
        Map<String, List<BlockPos>> Found = new LinkedHashMap<>();
        for (Map.Entry<String, List<BlockPos>> Entry : Plan.marks().entrySet()) {
            List<BlockPos> Points = new ArrayList<>();
            for (BlockPos Point : Entry.getValue()) {
                Points.add(FactionGround.toWorld(Origin, Front, Point.getX() - Plan.anchorX(), Point.getY() - Plan.anchorY(), Point.getZ() - Plan.anchorZ()));
            }
            Found.put(Entry.getKey(), Points);
        }
        return Found;
    }

    private static void foot(ServerLevel World, FactionPlan Plan, BlockPos Pos, BlockState Placed) {
        BlockState Fill = Plan.footingState() != null ? Plan.footingState() : Placed;
        for (int Depth = 1; Depth <= Plan.footingDepth(); Depth++) {
            BlockPos Below = Pos.below(Depth);
            if (World.isOutsideBuildHeight(Below)) {
                break;
            }
            BlockState There = World.getBlockState(Below);
            if (There.isSolidRender(World, Below) && There.getFluidState().isEmpty() && !FactionGround.isGrowth(There)) {
                break;
            }
            World.setBlock(Below, Fill, Flags);
        }
    }
}
