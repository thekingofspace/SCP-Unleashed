package net.rai.scp_unleashed.scp087;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;

public final class Scp087Layout {
    public static final int Version = 4;
    public static final int Steps = 13;
    public static final int Period = Steps * 2;
    public static final int Width = 7;
    public static final int Length = 21;
    public static final int SlotSpacing = 48;
    public static final int TopFloor = 230;
    public static final int WalkTop = TopFloor + 1;
    public static final int Ceiling = TopFloor + 5;
    public static final int CeilingThickness = 2;
    public static final int BottomFloor = 60;
    public static final int Periods = 7;
    public static final int InertPeriods = 2;
    public static final int InertFloor = TopFloor - Period * InertPeriods;
    public static final int WrapHigh = 200;
    public static final int SplitSpan = 20;
    public static final int WrapLow = WrapHigh - Period - SplitSpan - 3;
    public static final int DoorX = 3;
    public static final int WestWall = 0;
    public static final int EastWall = Width - 1;
    public static final int Spine = 3;
    public static final int NorthWall = 0;
    public static final int SouthWall = Length - 1;
    public static final int FlightStart = 4;
    public static final int FlightEnd = FlightStart + Steps - 1;
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private Scp087Layout() {
    }

    public static int signature(boolean Inert) {
        return Version * 2 + (Inert ? 1 : 0);
    }

    private static BlockState concrete() {
        return Scp087Registry.Concrete.get().defaultBlockState();
    }

    private static BlockState floor() {
        return Scp087Registry.Floor.get().defaultBlockState();
    }

    private static BlockState stair(Direction Up) {
        return Scp087Registry.Stairs.get().defaultBlockState().setValue(StairBlock.FACING, Up).setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT).setValue(StairBlock.WATERLOGGED, false);
    }

    private static BlockState rail(Direction Descent, boolean Left, Scp087RailShape Shape) {
        return Scp087Registry.Rail.get().defaultBlockState().setValue(Scp087RailBlock.Facing, Descent).setValue(Scp087RailBlock.Left, Left)
                .setValue(Scp087RailBlock.Shape, Shape);
    }

    public static Scp087RailShape slopeShape(int Step) {
        if (Step == 0) {
            return Scp087RailShape.SlopeTop;
        }
        if (Step == Steps - 1) {
            return Scp087RailShape.SlopeBottom;
        }
        return Step % 3 == 0 ? Scp087RailShape.SlopePost : Scp087RailShape.Slope;
    }

    private static BlockState southLandingRail(int X, int Z) {
        if (X == 1 && Z == 17) {
            return rail(Direction.SOUTH, false, Scp087RailShape.Flat);
        }
        if (X == 1 && Z == 18) {
            return rail(Direction.SOUTH, false, Scp087RailShape.FlatPost);
        }
        if (X == 1 && Z == 19) {
            return rail(Direction.SOUTH, false, Scp087RailShape.CornerIn);
        }
        if (Z == 19 && X >= 2 && X <= 4) {
            return rail(Direction.EAST, false, X == 3 ? Scp087RailShape.FlatPost : Scp087RailShape.Flat);
        }
        if (X == 5 && Z == 19) {
            return rail(Direction.EAST, false, Scp087RailShape.CornerIn);
        }
        if (X == 5 && Z == 18) {
            return rail(Direction.NORTH, false, Scp087RailShape.FlatPost);
        }
        if (X == 5 && Z == 17) {
            return rail(Direction.NORTH, false, Scp087RailShape.Flat);
        }
        if (X == 2 && Z == 17) {
            return rail(Direction.SOUTH, true, Scp087RailShape.CornerOut);
        }
        if (X == 3 && Z == 17) {
            return rail(Direction.EAST, true, Scp087RailShape.FlatPost);
        }
        if (X == 4 && Z == 17) {
            return rail(Direction.EAST, true, Scp087RailShape.CornerOut);
        }
        return null;
    }

    private static BlockState northLandingRail(int X, int Z, boolean Top) {
        if (X == 1 && (Z == 2 || Z == 3)) {
            return rail(Direction.SOUTH, false, Z == 2 ? Scp087RailShape.FlatPost : Scp087RailShape.Flat);
        }
        if (Top) {
            return X == 1 && Z == 1 ? rail(Direction.SOUTH, false, Scp087RailShape.Flat) : null;
        }
        if (X == 1 && Z == 1) {
            return rail(Direction.WEST, false, Scp087RailShape.CornerIn);
        }
        if (Z == 1 && X >= 2 && X <= 4) {
            return rail(Direction.WEST, false, X == 3 ? Scp087RailShape.FlatPost : Scp087RailShape.Flat);
        }
        if (X == 5 && Z == 1) {
            return rail(Direction.NORTH, false, Scp087RailShape.CornerIn);
        }
        if (X == 5 && (Z == 2 || Z == 3)) {
            return rail(Direction.NORTH, false, Z == 2 ? Scp087RailShape.FlatPost : Scp087RailShape.Flat);
        }
        if (X == 4 && Z == 3) {
            return rail(Direction.NORTH, true, Scp087RailShape.CornerOut);
        }
        if (X == 3 && Z == 3) {
            return rail(Direction.WEST, true, Scp087RailShape.FlatPost);
        }
        if (X == 2 && Z == 3) {
            return rail(Direction.WEST, true, Scp087RailShape.CornerOut);
        }
        return null;
    }

    private static BlockState landingRail(int X, int Z, int Depth, boolean Inert) {
        if (Z > FlightEnd && Z < SouthWall && validPeriod(Depth - Steps + 1, Inert)) {
            return southLandingRail(X, Z);
        }
        if (Z >= 1 && Z < FlightStart) {
            int Offset = Depth + 1;
            boolean Bottom = Inert && Offset == Period * InertPeriods;
            if (validPeriod(Offset, Inert) || Bottom) {
                return northLandingRail(X, Z, Offset == 0);
            }
        }
        return null;
    }

    private static boolean validPeriod(int Offset, boolean Inert) {
        if (Offset < 0 || Offset % Period != 0) {
            return false;
        }
        int Index = Offset / Period;
        return Index < (Inert ? InertPeriods : Periods);
    }

    public static boolean inFootprint(int X, int Z) {
        return X >= 0 && X < Width && Z >= 0 && Z < Length;
    }

    public static boolean isWall(int X, int Z) {
        return X == WestWall || X == EastWall || Z == NorthWall || Z == SouthWall || X == Spine && Z >= FlightStart && Z <= FlightEnd;
    }

    public static BlockState stateAt(int X, int Y, int Z, boolean Inert) {
        BlockState Empty = Blocks.AIR.defaultBlockState();
        if (!inFootprint(X, Z) || Y < BottomFloor || Y >= Ceiling + CeilingThickness) {
            return Empty;
        }
        if (Y == BottomFloor || Y >= Ceiling) {
            return concrete();
        }
        if (X == DoorX && Z == NorthWall && (Y == WalkTop || Y == WalkTop + 1)) {
            return Scp087Registry.Door.get().defaultBlockState().setValue(Scp087DoorBlock.Facing, Direction.SOUTH)
                    .setValue(Scp087DoorBlock.Half, Y == WalkTop ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        }
        if (isWall(X, Z)) {
            return concrete();
        }
        if (Inert && Y == InertFloor) {
            return floor();
        }
        if (Inert && Y < InertFloor) {
            return stateAt(X, Y, Z, false);
        }
        if (Y > TopFloor && X >= 4 && X <= 5 && Z >= FlightStart && Z <= FlightEnd) {
            return concrete();
        }
        int Depth = TopFloor - Y;
        if (X >= 1 && X <= 5) {
            BlockState Landing = landingRail(X, Z, Depth, Inert);
            if (Landing != null) {
                return Landing;
            }
        }
        if (Z >= 1 && Z < FlightStart && X >= 1 && X <= 5 && validPeriod(Depth, Inert)) {
            return floor();
        }
        if (Z > FlightEnd && Z < SouthWall && X >= 1 && X <= 5 && validPeriod(Depth - Steps, Inert)) {
            return floor();
        }
        if (X == 1 || X == 2) {
            int Step = Z - FlightStart;
            if (Step >= 0 && Step < Steps) {
                if (validPeriod(Depth - Step, Inert)) {
                    return stair(Direction.NORTH);
                }
                if (validPeriod(Depth - Step - 1, Inert)) {
                    return floor();
                }
                if (validPeriod(Depth - Step + 1, Inert)) {
                    return rail(Direction.SOUTH, X == 2, slopeShape(Step));
                }
            }
        }
        if (X == 4 || X == 5) {
            int Step = FlightEnd - Z;
            if (Step >= 0 && Step < Steps) {
                if (validPeriod(Depth - Steps - Step, Inert)) {
                    return stair(Direction.SOUTH);
                }
                if (validPeriod(Depth - Steps - Step - 1, Inert)) {
                    return floor();
                }
                if (validPeriod(Depth - Steps - Step + 1, Inert)) {
                    return rail(Direction.NORTH, X == 4, slopeShape(Step));
                }
            }
        }
        return Empty;
    }

    public static int build(ServerLevel World, int Slot, boolean Inert) {
        int OriginX = Scp087Stairwell.originX(Slot);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        int Changed = 0;
        for (int X = 0; X < Width; X++) {
            for (int Z = 0; Z < Length; Z++) {
                for (int Y = BottomFloor; Y < Ceiling + CeilingThickness; Y++) {
                    Cursor.set(OriginX + X, Y, Z);
                    BlockState Wanted = stateAt(X, Y, Z, Inert);
                    if (World.getBlockState(Cursor) != Wanted) {
                        World.setBlock(Cursor, Wanted, Flags);
                        Changed++;
                    }
                }
            }
        }
        return Changed;
    }

    public static double depthBlocks(double FeetY, int Wraps) {
        return WalkTop - FeetY + (double) Period * Wraps;
    }

    public static int flights(double FeetY, int Wraps) {
        return Math.max(0, (int) Math.floor((depthBlocks(FeetY, Wraps) + 0.5) / Steps));
    }
}
