package net.rai.scp_unleashed.scp2845;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Scp2845Circle {
    public static final int Reach = 16;
    public static final int Tolerance = 1;
    public static final int Height = 6;
    public static final int InnerReach = Reach - 1;
    public static final int SearchReach = Reach + 2;

    private Scp2845Circle() {
    }

    public enum Status {
        Valid,
        Broken,
        Unloaded
    }

    public record Result(Status Verdict, int Missing, @Nullable BlockPos FirstGap) {
        public boolean valid() {
            return Verdict == Status.Valid;
        }

        public boolean broken() {
            return Verdict == Status.Broken;
        }
    }

    public static List<BlockPos> ring(BlockPos Stone) {
        List<BlockPos> Found = new ArrayList<>(Reach * 8);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                if (Math.max(Math.abs(Dx), Math.abs(Dz)) == Reach) {
                    Found.add(Stone.offset(Dx, 0, Dz));
                }
            }
        }
        return Found;
    }

    public static boolean isStone(Level World, BlockPos Pos) {
        return World.getBlockState(Pos).is(Scp2845Registry.Stone.get());
    }

    public static boolean columnHasBone(Level World, BlockPos Column) {
        for (int Dy = -Tolerance; Dy <= Tolerance; Dy++) {
            if (World.getBlockState(Column.above(Dy)).is(Blocks.BONE_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    public static boolean loaded(Level World, BlockPos Stone) {
        return World.hasChunksAt(Stone.offset(-Reach, 0, -Reach), Stone.offset(Reach, 0, Reach));
    }

    public static Result check(Level World, BlockPos Stone) {
        if (!loaded(World, Stone)) {
            return new Result(Status.Unloaded, 0, null);
        }
        if (!isStone(World, Stone)) {
            return new Result(Status.Broken, Reach * 8, null);
        }
        int Missing = 0;
        BlockPos First = null;
        for (BlockPos Column : ring(Stone)) {
            if (!columnHasBone(World, Column)) {
                Missing++;
                if (First == null) {
                    First = Column;
                }
            }
        }
        return new Result(Missing == 0 ? Status.Valid : Status.Broken, Missing, First);
    }

    public static boolean inside(BlockPos Stone, Vec3 At) {
        double Dx = Math.abs(At.x - (Stone.getX() + 0.5));
        double Dz = Math.abs(At.z - (Stone.getZ() + 0.5));
        double Dy = At.y - Stone.getY();
        return Math.max(Dx, Dz) < Reach && Dy > -Height && Dy < Height + 4;
    }

    public static Optional<BlockPos> validAround(Level World, Vec3 At) {
        BlockPos Centre = BlockPos.containing(At);
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-InnerReach, -Height, -InnerReach), Centre.offset(InnerReach, Height, InnerReach))) {
            if (!isStone(World, Pos) || !inside(Pos, At)) {
                continue;
            }
            double Distance = Pos.distToCenterSqr(At);
            if (Distance < BestDistance && check(World, Pos).valid()) {
                Best = Pos.immutable();
                BestDistance = Distance;
            }
        }
        return Optional.ofNullable(Best);
    }

    public static List<BlockPos> stonesNear(Level World, BlockPos Centre, int Radius) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-Radius, -Radius, -Radius), Centre.offset(Radius, Radius, Radius))) {
            if (isStone(World, Pos)) {
                Found.add(Pos.immutable());
            }
        }
        return Found;
    }
}
