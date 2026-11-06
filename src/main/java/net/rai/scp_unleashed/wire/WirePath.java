package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class WirePath {
    private WirePath() {
    }

    public static List<BlockPos> cells(Vec3 From, Vec3 To, BlockPos FromOutlet, BlockPos ToOutlet) {
        List<BlockPos> Cells = new ArrayList<>();
        BlockGetter.traverseBlocks(From, To, Cells, (Collected, Pos) -> {
            if (!Pos.equals(FromOutlet) && !Pos.equals(ToOutlet)) {
                Collected.add(Pos.immutable());
            }
            return null;
        }, Collected -> null);
        return Cells;
    }

    @Nullable
    public static BlockPos firstObstruction(Level World, Vec3 From, Vec3 To, BlockPos FromOutlet, BlockPos ToOutlet) {
        return BlockGetter.traverseBlocks(From, To, World, (Context, Pos) -> {
            if (Pos.equals(FromOutlet) || Pos.equals(ToOutlet) || !Context.isLoaded(Pos)) {
                return null;
            }
            return obstructs(Context, Pos, From, To) ? Pos.immutable() : null;
        }, Context -> null);
    }

    public static boolean obstructs(Level World, BlockPos Pos, Vec3 From, Vec3 To) {
        VoxelShape Shape = World.getBlockState(Pos).getCollisionShape(World, Pos);
        return !Shape.isEmpty() && Shape.clip(From, To, Pos) != null;
    }
}
