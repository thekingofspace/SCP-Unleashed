package net.rai.scp_unleashed.raid.underground;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.base.BaseHeatmap;
import net.rai.scp_unleashed.raid.base.RaidBases;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record RaidSurvey(boolean Underground, BlockPos Anchor, List<BlockPos> Rooms, int Covered, RaidFlood Flood) {
    public static int top(Level World, int X, int Z) {
        return World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z) - 1;
    }

    public static boolean earthy(BlockState State) {
        return State.is(BlockTags.OVERWORLD_CARVER_REPLACEABLES) || State.is(Tags.Blocks.ORES) || State.is(BlockTags.BASE_STONE_NETHER) || State.is(Tags.Blocks.GRAVELS)
                || State.is(Tags.Blocks.END_STONES) || State.is(Blocks.CLAY) || State.is(Blocks.MUD) || State.is(Blocks.MOSS_BLOCK) || State.is(Blocks.PACKED_MUD)
                || State.is(Blocks.SMOOTH_BASALT) || State.is(Blocks.DRIPSTONE_BLOCK);
    }

    public static int[] cover(Level World, BlockPos Feet) {
        int Top = top(World, Feet.getX(), Feet.getZ());
        int Depth = Top - Feet.getY();
        int Solid = 0;
        int Earth = 0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        int Ceiling = Math.min(Top, Feet.getY() + 256);
        for (int Y = Feet.getY() + 2; Y <= Ceiling; Y++) {
            Cursor.set(Feet.getX(), Y, Feet.getZ());
            BlockState State = World.getBlockState(Cursor);
            if (State.getCollisionShape(World, Cursor).isEmpty()) {
                continue;
            }
            Solid++;
            if (earthy(State)) {
                Earth++;
            }
        }
        return new int[]{Depth, Solid, Earth};
    }

    public static boolean covered(Level World, BlockPos Feet) {
        int[] Found = cover(World, Feet);
        return Found[0] >= UndergroundRules.MinDepth && Found[1] >= UndergroundRules.MinCover && Found[2] >= UndergroundRules.MinEarth;
    }

    public static boolean open(Level World, BlockPos Feet) {
        return top(World, Feet.getX(), Feet.getZ()) < Feet.getY();
    }

    @Nullable
    public static BlockPos roomCell(ServerLevel World, BlockPos Near, int Span) {
        if (!World.isLoaded(Near)) {
            return null;
        }
        BlockPos Best = null;
        int BestGap = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Span; Dx <= Span; Dx++) {
            for (int Dz = -Span; Dz <= Span; Dz++) {
                for (int Dy = -Span; Dy <= Span; Dy++) {
                    int Gap = Dx * Dx + Dz * Dz + Dy * Dy * 2;
                    if (Gap >= BestGap) {
                        continue;
                    }
                    Cursor.set(Near.getX() + Dx, Near.getY() + Dy, Near.getZ() + Dz);
                    if (World.isLoaded(Cursor) && RaidGround.standable(World, Cursor)) {
                        Best = Cursor.immutable();
                        BestGap = Gap;
                    }
                }
            }
        }
        return Best;
    }

    public static double reach(Raid Current) {
        return Current.footprint() + UndergroundRules.AreaMargin;
    }

    public static boolean inArea(Raid Current, BlockPos Pos) {
        BlockPos Centre = Current.estimate();
        double Dx = Pos.getX() - Centre.getX();
        double Dz = Pos.getZ() - Centre.getZ();
        double Reach = reach(Current);
        return Dx * Dx + Dz * Dz <= Reach * Reach && Math.abs(Pos.getY() - Centre.getY()) <= UndergroundRules.AreaRise;
    }

    public static List<BlockPos> probes(ServerLevel World, Raid Current) {
        List<BlockPos> Found = new ArrayList<>();
        Found.add(Current.estimate());
        Found.add(Current.target());
        BaseHeatmap Known = RaidBases.get(World.getServer()).find(Current.group(), Current.dimension());
        if (Known != null) {
            BlockPos Centre = Current.estimate();
            List<BlockPos> Stashes = new ArrayList<>();
            for (Long Key : Known.stashes().keySet()) {
                BlockPos Pos = BlockPos.of(Key);
                if (inArea(Current, Pos)) {
                    Stashes.add(Pos);
                }
            }
            Stashes.sort(Comparator.comparingDouble(Pos -> Pos.distSqr(Centre)));
            for (int Index = 0; Index < Math.min(UndergroundRules.RoomProbes, Stashes.size()); Index++) {
                Found.add(Stashes.get(Index));
            }
        }
        return Found;
    }

    public static RaidSurvey survey(ServerLevel World, Raid Current) {
        List<BlockPos> Rooms = new ArrayList<>();
        for (BlockPos Probe : probes(World, Current)) {
            BlockPos Cell = roomCell(World, Probe, UndergroundRules.RoomSnap);
            if (Cell != null && inArea(Current, Cell) && !Rooms.contains(Cell)) {
                Rooms.add(Cell);
            }
        }
        if (Rooms.isEmpty()) {
            return new RaidSurvey(false, Current.target(), List.of(), 0, RaidFlood.Empty);
        }
        List<BlockPos> Under = new ArrayList<>();
        for (BlockPos Room : Rooms) {
            if (covered(World, Room)) {
                Under.add(Room);
            }
        }
        BlockPos Anchor = Under.isEmpty() ? Rooms.get(0) : Under.get(0);
        boolean Underground = !Under.isEmpty() && Under.size() * 2 >= Rooms.size();
        if (!Underground) {
            return new RaidSurvey(false, Anchor, Rooms, Under.size(), RaidFlood.Empty);
        }
        RaidFlood Flood = RaidFlood.flood(World, Under, Current.estimate(), reach(Current), Anchor.getY() - UndergroundRules.AreaRise,
                Anchor.getY() + UndergroundRules.AreaRise * 2, UndergroundRules.FloodNodes);
        return new RaidSurvey(true, Anchor, Rooms, Under.size(), Flood);
    }
}
