package net.rai.scp_unleashed.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;

public interface LargeBlock {
    List<BlockPos> getFootprint(BlockPlaceContext Context);

    static boolean canPlace(BlockPlaceContext Context, List<BlockPos> Footprint) {
        for (BlockPos Cell : Footprint) {
            if (!fits(Context, Cell)) {
                return false;
            }
        }
        return true;
    }

    static List<BlockPos> blockedCells(BlockPlaceContext Context, List<BlockPos> Footprint) {
        List<BlockPos> Blocked = new ArrayList<>();
        for (BlockPos Cell : Footprint) {
            if (!fits(Context, Cell)) {
                Blocked.add(Cell);
            }
        }
        return Blocked;
    }

    static boolean fits(BlockPlaceContext Context, BlockPos Cell) {
        Level World = Context.getLevel();
        if (World.isOutsideBuildHeight(Cell) || !World.getWorldBorder().isWithinBounds(Cell)) {
            return false;
        }
        boolean Replaceable = Cell.equals(Context.getClickedPos()) ? Context.canPlace() : World.getBlockState(Cell).canBeReplaced(Context);
        return Replaceable && World.isUnobstructed(null, Shapes.block().move(Cell.getX(), Cell.getY(), Cell.getZ()));
    }

    static AABB bounds(List<BlockPos> Footprint) {
        AABB Box = new AABB(Footprint.getFirst());
        for (BlockPos Cell : Footprint) {
            Box = Box.minmax(new AABB(Cell));
        }
        return Box;
    }
}
