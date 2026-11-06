package net.rai.scp_unleashed.door;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DoorGeometry {
    public static final int Steps = 64;
    public static final double Teeth = 2;
    public static final double MobClearance = 32.0;
    public static final int HeaderPiece = 2;
    private static final double Epsilon = 1.0E-3;
    private static final Map<DoorKind, Layout> Layouts = new EnumMap<>(DoorKind.class);
    private static final Map<DoorKind, int[][]> Tiles = new EnumMap<>(DoorKind.class);
    private static final Map<DoorKind, List<int[]>> Lamps = new EnumMap<>(DoorKind.class);
    private static final VoxelShape[][][][] ShapeCache = new VoxelShape[DoorKind.values().length][4][][];
    private static final VoxelShape[][][][] CollisionCache = new VoxelShape[DoorKind.values().length][4][][];

    static {
        for (DoorKind Kind : DoorKind.values()) {
            Layouts.put(Kind, layoutOf(Kind));
            Tiles.put(Kind, tilesOf(Kind, Layouts.get(Kind).Leaves()));
            Lamps.put(Kind, lampsOf(Kind));
        }
    }

    private DoorGeometry() {
    }

    private static Layout layoutOf(DoorKind Kind) {
        double Wide = Kind.getWidth() * 16;
        double Tall = Kind.getHeight() * 16;
        double Jamb = Kind.getJamb();
        double Clear = Tall - Kind.getHeader();
        double Front = Kind.getFront();
        double Back = Kind.getBack();
        List<Box> Frame = List.of(new Box(0, 0, 0, Jamb, Tall, 16), new Box(Wide - Jamb, 0, 0, Wide, Tall, 16), new Box(0, Clear, 0, Wide, Tall, 16));
        return switch (Kind.getMotion()) {
            case Split -> new Layout(Frame, List.of(
                    new Leaf(new Box(0, 0, Front, Wide / 2, Clear, Back), -(Wide / 2 - Jamb - 0.5), 0),
                    new Leaf(new Box(Wide / 2, 0, Front, Wide, Clear, Back), Wide / 2 - Jamb - 0.5, 0)),
                    new Box(Jamb, 0, Front - 1, Wide - Jamb, Clear, Back + 1));
            case Slide -> new Layout(Frame, List.of(
                    new Leaf(new Box(0, 0, Front, Wide, Clear, Back), -(Wide - Jamb - 0.5), 0)),
                    new Box(Jamb, 0, Front - 1, Wide - Jamb, Clear, Back + 1));
            case Bulkhead -> new Layout(Frame, List.of(
                    new Leaf(new Box(0, 0, Front, Wide / 2, Clear, Back), -(Wide / 2 - Jamb + Teeth + 0.5), 0),
                    new Leaf(new Box(Wide / 2, 0, Front, Wide, Clear, Back), Wide / 2 - Jamb + Teeth + 0.5, 0)),
                    new Box(Jamb, 0, Front - 1, Wide - Jamb, Clear, Back + 1));
            case Rise -> {
                double Split = Kind.getSize().risingSplit();
                yield new Layout(Frame, List.of(
                        new Leaf(new Box(0, 0, Front, Wide, Split, (Front + Back) / 2), 0, Tall),
                        new Leaf(new Box(0, Split, (Front + Back) / 2, Wide, Tall, Back), 0, Tall - Split)),
                        new Box(Jamb, 0, Front - 1, Wide - Jamb, Clear, Back + 1));
            }
        };
    }

    private static int[][] tilesOf(DoorKind Kind, List<Leaf> Leaves) {
        int[][] Result = new int[Kind.getCellCount()][];
        for (int Part = 0; Part < Kind.getCellCount(); Part++) {
            double U0 = Kind.columnOf(Part) * 16;
            double V0 = Kind.rowOf(Part) * 16;
            List<Integer> Found = new ArrayList<>();
            for (int Index = 0; Index < Leaves.size(); Index++) {
                Box Rest = Leaves.get(Index).Rest();
                if (Math.min(Rest.U1(), U0 + 16) - Math.max(Rest.U0(), U0) > Epsilon && Math.min(Rest.V1(), V0 + 16) - Math.max(Rest.V0(), V0) > Epsilon) {
                    Found.add(Index);
                }
            }
            Result[Part] = Found.stream().mapToInt(Integer::intValue).toArray();
        }
        return Result;
    }

    private static List<int[]> lampsOf(DoorKind Kind) {
        if (!Kind.hasWarningLamps()) {
            return List.of();
        }
        int Right = Kind.getWidth() - 1;
        int Top = Kind.getHeight() - 1;
        return List.of(new int[]{0, 1}, new int[]{Right, 1}, new int[]{0, Top}, new int[]{Right, Top});
    }

    public static int[] tileLeaves(DoorKind Kind, int Column, int Row) {
        return Tiles.get(Kind)[Kind.partOf(Column, Row)];
    }

    public static List<int[]> lampCells(DoorKind Kind) {
        return Lamps.get(Kind);
    }

    public static float ease(float Progress) {
        float Clamped = Mth.clamp(Progress, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static Direction rightOf(Direction Front) {
        return Front.getCounterClockWise();
    }

    public static int quarterTurns(Direction Front) {
        return (Front.get2DDataValue() + 2) % 4;
    }

    public static List<Box> frame(DoorKind Kind) {
        return Layouts.get(Kind).Frame();
    }

    public static List<Leaf> leaves(DoorKind Kind) {
        return Layouts.get(Kind).Leaves();
    }

    public static Box clearance(DoorKind Kind) {
        return Layouts.get(Kind).Clearance();
    }

    public static int leafAt(DoorKind Kind, int Column, int Row) {
        double U = Column * 16 + 8;
        double V = Row * 16 + 8;
        List<Leaf> Leaves = leaves(Kind);
        for (int Index = 0; Index < Leaves.size(); Index++) {
            Box Rest = Leaves.get(Index).Rest();
            if (U > Rest.U0() && U < Rest.U1() && V > Rest.V0() && V < Rest.V1()) {
                return Index;
            }
        }
        return -1;
    }

    public static VoxelShape cellShape(DoorKind Kind, Direction Front, int Column, int Row, float Eased) {
        return cached(ShapeCache, Kind, Front, Column, Row, Eased, true);
    }

    public static VoxelShape cellCollision(DoorKind Kind, Direction Front, int Column, int Row, float Eased) {
        return cached(CollisionCache, Kind, Front, Column, Row, Eased, !lowHeader(Kind));
    }

    public static boolean lowHeader(DoorKind Kind) {
        return Kind.getHeight() * 16 - Kind.getHeader() < MobClearance;
    }

    private static VoxelShape cached(VoxelShape[][][][] Cache, DoorKind Kind, Direction Front, int Column, int Row, float Eased, boolean Header) {
        int Turns = quarterTurns(Front);
        int Part = Kind.partOf(Column, Row);
        int Step = Mth.clamp(Math.round(Eased * Steps), 0, Steps);
        VoxelShape[][] PerPart = Cache[Kind.ordinal()][Turns];
        if (PerPart == null) {
            PerPart = new VoxelShape[Kind.getCellCount()][Steps + 1];
            Cache[Kind.ordinal()][Turns] = PerPart;
        }
        VoxelShape Found = PerPart[Part][Step];
        if (Found == null) {
            Found = buildShape(Kind, Turns, Column, Row, (float) Step / Steps, Header);
            PerPart[Part][Step] = Found;
        }
        return Found;
    }

    private static VoxelShape buildShape(DoorKind Kind, int Turns, int Column, int Row, float Eased, boolean Header) {
        VoxelShape Shape = Shapes.empty();
        List<Box> Pieces = frame(Kind);
        for (int Index = 0; Index < Pieces.size(); Index++) {
            if (!Header && Index == HeaderPiece) {
                continue;
            }
            Shape = Shapes.or(Shape, cellBox(Pieces.get(Index), Column, Row, Turns));
        }
        for (Leaf Moving : leaves(Kind)) {
            Shape = Shapes.or(Shape, cellBox(Moving.at(Eased), Column, Row, Turns));
        }
        return Shape.optimize();
    }

    private static VoxelShape cellBox(Box Piece, int Column, int Row, int Turns) {
        double X0 = Mth.clamp(16 * (Column + 1) - Piece.U1(), 0, 16);
        double X1 = Mth.clamp(16 * (Column + 1) - Piece.U0(), 0, 16);
        double Y0 = Mth.clamp(Piece.V0() - 16 * Row, 0, 16);
        double Y1 = Mth.clamp(Piece.V1() - 16 * Row, 0, 16);
        double Z0 = Mth.clamp(Piece.W0(), 0, 16);
        double Z1 = Mth.clamp(Piece.W1(), 0, 16);
        if (X1 - X0 < Epsilon || Y1 - Y0 < Epsilon || Z1 - Z0 < Epsilon) {
            return Shapes.empty();
        }
        return turned(Turns, X0, Y0, Z0, X1, Y1, Z1);
    }

    public static VoxelShape turnedBox(Direction Front, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return turned(quarterTurns(Front), X0, Y0, Z0, X1, Y1, Z1);
    }

    private static VoxelShape turned(int Turns, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        double MinX = X0;
        double MaxX = X1;
        double MinZ = Z0;
        double MaxZ = Z1;
        switch (Turns) {
            case 1 -> {
                MinX = 16 - Z1;
                MaxX = 16 - Z0;
                MinZ = X0;
                MaxZ = X1;
            }
            case 2 -> {
                MinX = 16 - X1;
                MaxX = 16 - X0;
                MinZ = 16 - Z1;
                MaxZ = 16 - Z0;
            }
            case 3 -> {
                MinX = Z0;
                MaxX = Z1;
                MinZ = 16 - X1;
                MaxZ = 16 - X0;
            }
            default -> {
            }
        }
        return Shapes.box(MinX / 16, Y0 / 16, MinZ / 16, MaxX / 16, Y1 / 16, MaxZ / 16);
    }

    public static Vec3 worldPoint(BlockPos Origin, Direction Front, double U, double V, double W) {
        Direction Right = rightOf(Front);
        Direction Back = Front.getOpposite();
        double X = Origin.getX() + corner(Right, Front, Direction.Axis.X) + (Right.getStepX() * U + Back.getStepX() * W) / 16;
        double Y = Origin.getY() + V / 16;
        double Z = Origin.getZ() + corner(Right, Front, Direction.Axis.Z) + (Right.getStepZ() * U + Back.getStepZ() * W) / 16;
        return new Vec3(X, Y, Z);
    }

    private static double corner(Direction Right, Direction Front, Direction.Axis Axis) {
        double Offset = 0;
        if (Right.getAxis() == Axis && Right.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            Offset += 1;
        }
        if (Front.getAxis() == Axis && Front.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            Offset += 1;
        }
        return Offset;
    }

    public static AABB worldBox(BlockPos Origin, Direction Front, Box Piece) {
        return new AABB(worldPoint(Origin, Front, Piece.U0(), Piece.V0(), Piece.W0()), worldPoint(Origin, Front, Piece.U1(), Piece.V1(), Piece.W1()));
    }

    public static AABB renderBounds(DoorKind Kind, BlockPos Origin, Direction Front) {
        AABB Bounds = worldBox(Origin, Front, new Box(0, 0, 0, Kind.getWidth() * 16, Kind.getHeight() * 16, 16));
        for (Leaf Moving : leaves(Kind)) {
            Bounds = Bounds.minmax(worldBox(Origin, Front, Moving.at(1.0f)));
        }
        return Bounds;
    }

    public static Vec3 centre(DoorKind Kind, BlockPos Origin, Direction Front) {
        return worldPoint(Origin, Front, Kind.getWidth() * 8, Kind.getHeight() * 8, 8);
    }

    public record Box(double U0, double V0, double W0, double U1, double V1, double W1) {
        public Box shifted(double DeltaU, double DeltaV) {
            return new Box(U0 + DeltaU, V0 + DeltaV, W0, U1 + DeltaU, V1 + DeltaV, W1);
        }
    }

    private record Layout(List<Box> Frame, List<Leaf> Leaves, Box Clearance) {
    }

    public record Leaf(Box Rest, double TravelU, double TravelV) {
        public Box at(float Eased) {
            return Rest.shifted(TravelU * Eased, TravelV * Eased);
        }
    }
}
