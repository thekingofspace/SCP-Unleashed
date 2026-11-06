package net.rai.scp_unleashed.scp106;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.advmaterials.MagnetizedBlock;

import java.util.ArrayList;
import java.util.List;

public final class Scp106Phasing {
    public static final double Epsilon = 1.0E-7;

    private Scp106Phasing() {
    }

    public static boolean holds(BlockGetter World, BlockPos Pos, BlockState State) {
        if (MagnetizedBlock.holds(World, State)) {
            return true;
        }
        return State.getDestroySpeed(World, Pos) < 0.0f && !State.getCollisionShape(World, Pos).isEmpty();
    }

    public static boolean isMagnetized(BlockState State) {
        return MagnetizedBlock.isMagnetized(State);
    }

    public static boolean solid(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return !State.getCollisionShape(World, Pos).isEmpty();
    }

    public static double topOf(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        VoxelShape Shape = State.getCollisionShape(World, Pos);
        return Shape.isEmpty() ? Pos.getY() : Pos.getY() + Shape.max(Direction.Axis.Y);
    }

    public static List<VoxelShape> walls(Level World, AABB Swept, AABB Current) {
        List<VoxelShape> Found = new ArrayList<>();
        int X0 = Mth.floor(Swept.minX) - 1;
        int Y0 = Mth.floor(Swept.minY) - 1;
        int Z0 = Mth.floor(Swept.minZ) - 1;
        int X1 = Mth.floor(Swept.maxX) + 1;
        int Y1 = Mth.floor(Swept.maxY) + 1;
        int Z1 = Mth.floor(Swept.maxZ) + 1;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = X0; X <= X1; X++) {
            for (int Y = Y0; Y <= Y1; Y++) {
                for (int Z = Z0; Z <= Z1; Z++) {
                    Cursor.set(X, Y, Z);
                    if (!World.isLoaded(Cursor)) {
                        Found.add(Shapes.block().move(X, Y, Z));
                        continue;
                    }
                    BlockState State = World.getBlockState(Cursor);
                    if (State.isAir() || !holds(World, Cursor, State)) {
                        continue;
                    }
                    AABB Cell = new AABB(X, Y, Z, X + 1, Y + 1, Z + 1);
                    if (Cell.deflate(Epsilon).intersects(Current)) {
                        continue;
                    }
                    Found.add(Shapes.block().move(X, Y, Z));
                }
            }
        }
        return Found;
    }

    public static Vec3 collide(Level World, AABB Box, Vec3 Wanted) {
        if (Wanted.lengthSqr() < Epsilon) {
            return Vec3.ZERO;
        }
        List<VoxelShape> Walls = walls(World, Box.expandTowards(Wanted), Box);
        if (Walls.isEmpty()) {
            return Wanted;
        }
        double Dy = Shapes.collide(Direction.Axis.Y, Box, Walls, Wanted.y);
        AABB Moved = Box.move(0.0, Dy, 0.0);
        double Dx;
        double Dz;
        if (Math.abs(Wanted.x) < Math.abs(Wanted.z)) {
            Dz = Shapes.collide(Direction.Axis.Z, Moved, Walls, Wanted.z);
            Moved = Moved.move(0.0, 0.0, Dz);
            Dx = Shapes.collide(Direction.Axis.X, Moved, Walls, Wanted.x);
        } else {
            Dx = Shapes.collide(Direction.Axis.X, Moved, Walls, Wanted.x);
            Moved = Moved.move(Dx, 0.0, 0.0);
            Dz = Shapes.collide(Direction.Axis.Z, Moved, Walls, Wanted.z);
        }
        return new Vec3(Dx, Dy, Dz);
    }

    public static boolean lineHeld(Level World, Vec3 From, Vec3 To) {
        Vec3 Delta = To.subtract(From);
        double Length = Delta.length();
        int Steps = Math.max(1, Mth.ceil(Length * 3.0));
        BlockPos Last = null;
        for (int Step = 0; Step <= Steps; Step++) {
            Vec3 Point = From.add(Delta.scale((double) Step / Steps));
            BlockPos Pos = BlockPos.containing(Point);
            if (Pos.equals(Last)) {
                continue;
            }
            Last = Pos;
            if (holds(World, Pos, World.getBlockState(Pos))) {
                return true;
            }
        }
        return false;
    }

    public static boolean standingOnMagnet(Level World, AABB Box) {
        int X0 = Mth.floor(Box.minX + 0.05);
        int X1 = Mth.floor(Box.maxX - 0.05);
        int Z0 = Mth.floor(Box.minZ + 0.05);
        int Z1 = Mth.floor(Box.maxZ - 0.05);
        int Y = Mth.floor(Box.minY - 0.1);
        for (int X = X0; X <= X1; X++) {
            for (int Z = Z0; Z <= Z1; Z++) {
                for (int Dy = 0; Dy <= 1; Dy++) {
                    BlockPos Pos = new BlockPos(X, Y - Dy, Z);
                    BlockState State = World.getBlockState(Pos);
                    if (isMagnetized(State) || MagnetizedBlock.holds(World, State)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int sealedSides(Level World, Vec3 Centre, double FeetY, int Reach) {
        int Sealed = 0;
        Direction[] Sides = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        double[] Heights = {FeetY + 0.3, FeetY + 1.2, FeetY + 2.0};
        for (Direction Side : Sides) {
            boolean All = true;
            for (double Height : Heights) {
                if (!rayHeld(World, new Vec3(Centre.x, Height, Centre.z), Side, Reach)) {
                    All = false;
                    break;
                }
            }
            if (All) {
                Sealed++;
            }
        }
        if (rayHeld(World, new Vec3(Centre.x, FeetY + 0.5, Centre.z), Direction.DOWN, Reach)) {
            Sealed++;
        }
        if (rayHeld(World, new Vec3(Centre.x, FeetY + 1.5, Centre.z), Direction.UP, Reach)) {
            Sealed++;
        }
        return Sealed;
    }

    public static boolean rayHeld(Level World, Vec3 From, Direction Side, int Reach) {
        BlockPos Start = BlockPos.containing(From);
        for (int Step = 0; Step <= Reach; Step++) {
            BlockPos Pos = Start.relative(Side, Step);
            if (!World.isLoaded(Pos)) {
                return false;
            }
            if (holds(World, Pos, World.getBlockState(Pos))) {
                return true;
            }
        }
        return false;
    }

    public static BlockPos firstHeld(Level World, Vec3 From, Direction Side, int Reach) {
        BlockPos Start = BlockPos.containing(From);
        for (int Step = 0; Step <= Reach; Step++) {
            BlockPos Pos = Start.relative(Side, Step);
            if (holds(World, Pos, World.getBlockState(Pos))) {
                return Pos;
            }
        }
        return null;
    }
}
