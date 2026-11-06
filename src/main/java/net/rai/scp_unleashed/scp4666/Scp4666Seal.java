package net.rai.scp_unleashed.scp4666;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp4666.block.Scp4666FireplaceBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666HeaterBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666WindowBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp4666Seal {
    public static final int Split = 4;
    public static final int Margin = 13;
    public static final int Listed = 20;
    private static final int[][] Steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    public record Report(List<BlockPos> Sky, List<BlockPos> Snow) {
        public boolean sealed() {
            return Sky.isEmpty() && Snow.isEmpty();
        }
    }

    private Scp4666Seal() {
    }

    public static boolean glass(BlockState Found) {
        return Found.getBlock() instanceof IronBarsBlock || Found.getBlock() instanceof Scp4666WindowBlock;
    }

    private static boolean sealing(BlockState Found) {
        return Found.getBlock() instanceof DoorBlock || Found.getBlock() instanceof TrapDoorBlock || Found.getBlock() instanceof WallBlock
                || Found.getBlock() instanceof Scp4666FireplaceBlock || Found.getBlock() instanceof Scp4666HeaterBlock
                || Found.getBlock() instanceof IronBarsBlock && !Found.is(Blocks.GLASS_PANE);
    }

    public static boolean snow(BlockState Found) {
        return Found.is(Blocks.SNOW);
    }

    public static List<BlockPos> openGlass(ServerLevel World, BlockPos Low, BlockPos High) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Pos : BlockPos.betweenClosed(Low, High)) {
            if (!glass(World.getBlockState(Pos))) {
                continue;
            }
            BlockPos Below = Pos.below();
            BlockPos Above = Pos.above();
            BlockState Under = World.getBlockState(Below);
            BlockState Over = World.getBlockState(Above);
            boolean Shut = glass(Under) || Block.isFaceFull(Under.getCollisionShape(World, Below), Direction.UP);
            boolean Capped = glass(Over) || Block.isFaceFull(Over.getCollisionShape(World, Above), Direction.DOWN);
            if (!Shut || !Capped) {
                Found.add(Pos.immutable());
            }
        }
        return Found;
    }

    public static long mask(ServerLevel World, BlockPos Pos) {
        BlockState Found = World.getBlockState(Pos);
        if (Found.isAir() || snow(Found)) {
            return 0L;
        }
        if (sealing(Found)) {
            return -1L;
        }
        long Bits = 0L;
        double Size = 1.0 / Split;
        for (AABB Box : Found.getCollisionShape(World, Pos).toAabbs()) {
            for (int X = 0; X < Split; X++) {
                for (int Y = 0; Y < Split; Y++) {
                    for (int Z = 0; Z < Split; Z++) {
                        if (Box.minX < (X + 1) * Size && Box.maxX > X * Size && Box.minY < (Y + 1) * Size && Box.maxY > Y * Size
                                && Box.minZ < (Z + 1) * Size && Box.maxZ > Z * Size) {
                            Bits |= 1L << (X * Split * Split + Y * Split + Z);
                        }
                    }
                }
            }
        }
        return Bits;
    }

    public static Report scan(ServerLevel World, Scp4666Map Layout) {
        int Cx = Layout.Centre.getX();
        int Cz = Layout.Centre.getZ();
        int MinX = (Cx - Margin) * Split;
        int MaxX = (Cx + Margin + 1) * Split - 1;
        int MinZ = (Cz - Margin) * Split;
        int MaxZ = (Cz + Margin + 1) * Split - 1;
        int MinY = (Layout.BasementMinY - 1) * Split;
        int Roof = (Layout.CabinMaxY + 3) * Split;
        Map<Long, Long> Masks = new HashMap<>();
        BlockPos.MutableBlockPos Probe = new BlockPos.MutableBlockPos();
        Set<Long> Seen = new HashSet<>();
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        for (int X = Layout.CabinMinX + 1; X < Layout.CabinMaxX; X++) {
            for (int Z = Layout.CabinMinZ + 1; Z < Layout.CabinMaxZ; Z++) {
                for (int Y = Layout.BasementMinY + 1; Y < Layout.Floor + 4 + Math.max(0, 7 - Math.abs(Z - Cz)); Y++) {
                    if (Y == Layout.Floor || Y == Layout.Floor + 4) {
                        continue;
                    }
                    int[] Sub = {X * Split + 1, Y * Split + 1, Z * Split + 1};
                    if (!blocked(World, Masks, Probe, Sub) && Seen.add(key(Sub))) {
                        Queue.add(Sub);
                    }
                }
            }
        }
        Set<BlockPos> Sky = new LinkedHashSet<>();
        Set<BlockPos> Snow = new LinkedHashSet<>();
        while (!Queue.isEmpty() && Sky.size() < Listed) {
            int[] Sub = Queue.poll();
            BlockPos Cell = new BlockPos(Math.floorDiv(Sub[0], Split), Math.floorDiv(Sub[1], Split), Math.floorDiv(Sub[2], Split));
            if (Snow.size() < Listed && snow(World.getBlockState(Cell))) {
                Snow.add(Cell);
            }
            if (Sub[1] >= Roof || Sub[0] <= MinX || Sub[0] >= MaxX || Sub[2] <= MinZ || Sub[2] >= MaxZ) {
                Sky.add(Cell);
                continue;
            }
            for (int[] Step : Steps) {
                int[] Next = {Sub[0] + Step[0], Sub[1] + Step[1], Sub[2] + Step[2]};
                if (Next[1] < MinY || !Seen.add(key(Next)) || blocked(World, Masks, Probe, Next)) {
                    continue;
                }
                Queue.add(Next);
            }
        }
        return new Report(List.copyOf(Sky), List.copyOf(Snow));
    }

    private static long key(int[] Sub) {
        return BlockPos.asLong(Sub[0], Sub[1], Sub[2]);
    }

    private static boolean blocked(ServerLevel World, Map<Long, Long> Masks, BlockPos.MutableBlockPos Probe, int[] Sub) {
        Probe.set(Math.floorDiv(Sub[0], Split), Math.floorDiv(Sub[1], Split), Math.floorDiv(Sub[2], Split));
        long Bits = Masks.computeIfAbsent(Probe.asLong(), Key -> mask(World, Probe));
        int Bit = Math.floorMod(Sub[0], Split) * Split * Split + Math.floorMod(Sub[1], Split) * Split + Math.floorMod(Sub[2], Split);
        return (Bits >> Bit & 1L) != 0L;
    }
}
