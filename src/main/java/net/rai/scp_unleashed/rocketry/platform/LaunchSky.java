package net.rai.scp_unleashed.rocketry.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.function.Predicate;

public final class LaunchSky {
    private LaunchSky() {
    }

    public static boolean blocks(BlockState State) {
        return State.blocksMotion() || !State.getFluidState().isEmpty();
    }

    public static boolean ready(Level World, BlockPos Column) {
        return World.getChunkSource().getChunkNow(Column.getX() >> 4, Column.getZ() >> 4) != null;
    }

    public static long print(Level World, List<BlockPos> Columns) {
        long Print = World.dimensionType().hasCeiling() ? 1L : 0L;
        for (BlockPos Column : Columns) {
            int Top = ready(World, Column) ? World.getHeight(Heightmap.Types.MOTION_BLOCKING, Column.getX(), Column.getZ()) : Integer.MIN_VALUE;
            Print = Print * 31L + Top;
        }
        return Print;
    }

    public static boolean clear(Level World, List<BlockPos> Columns, int FromY, Predicate<BlockPos> Own) {
        if (World.dimensionType().hasCeiling()) {
            return false;
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (BlockPos Column : Columns) {
            if (!ready(World, Column)) {
                return false;
            }
            int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING, Column.getX(), Column.getZ());
            for (int Y = Math.max(FromY, World.getMinBuildHeight()); Y < Top; Y++) {
                Cursor.set(Column.getX(), Y, Column.getZ());
                if (Own.test(Cursor)) {
                    continue;
                }
                if (blocks(World.getBlockState(Cursor))) {
                    return false;
                }
            }
        }
        return true;
    }
}
