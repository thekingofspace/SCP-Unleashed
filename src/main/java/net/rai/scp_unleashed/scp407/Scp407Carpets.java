package net.rai.scp_unleashed.scp407;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class Scp407Carpets {
    public static final int Kinds = 6;
    public static final String[] Names = {"moss", "lichen", "spore", "rooty", "flower", "damp"};
    public static final double Scale = 4.5;
    public static final double DetailScale = 2.0;
    public static final int PatchSeed = 4071;
    public static final int DetailSeed = 4073;
    public static final double[] Bands = {0.284, 0.375, 0.524, 0.583, 0.678};
    public static final int[] BandKinds = {5, 3, 0, 2, 1, 4};

    private Scp407Carpets() {
    }

    public static double hash(int X, int Y, int Z, int Seed) {
        long Value = ((long) X * 73856093L) ^ ((long) Y * 19349663L) ^ ((long) Z * 83492791L) ^ ((long) Seed * 2654435761L);
        Value &= 0xFFFFFFFFL;
        Value = (((Value >>> 16) ^ Value) * 0x45D9F3BL) & 0xFFFFFFFFL;
        Value = (((Value >>> 16) ^ Value) * 0x45D9F3BL) & 0xFFFFFFFFL;
        Value = (Value >>> 16) ^ Value;
        return (Value & 0xFFFFL) / 65535.0;
    }

    private static double smooth(double Value) {
        return Value * Value * (3.0 - 2.0 * Value);
    }

    public static double noise(double X, double Y, double Z, int Seed) {
        int X0 = (int) Math.floor(X);
        int Y0 = (int) Math.floor(Y);
        int Z0 = (int) Math.floor(Z);
        double Fx = smooth(X - X0);
        double Fy = smooth(Y - Y0);
        double Fz = smooth(Z - Z0);
        double Total = 0.0;
        for (int Dx = 0; Dx <= 1; Dx++) {
            for (int Dy = 0; Dy <= 1; Dy++) {
                for (int Dz = 0; Dz <= 1; Dz++) {
                    double Weight = (Dx == 1 ? Fx : 1.0 - Fx) * (Dy == 1 ? Fy : 1.0 - Fy) * (Dz == 1 ? Fz : 1.0 - Fz);
                    Total += Weight * hash(X0 + Dx, Y0 + Dy, Z0 + Dz, Seed);
                }
            }
        }
        return Total;
    }

    public static double field(int X, int Y, int Z) {
        double Patch = noise(X / Scale, Y / Scale, Z / Scale, PatchSeed);
        double Detail = noise(X / DetailScale, Y / DetailScale, Z / DetailScale, DetailSeed);
        return Patch * 0.82 + Detail * 0.18;
    }

    public static final int Slots = 5;
    public static final int SpeckSlot = 4;
    public static final double SpeckChance = 0.35;
    public static final int SpeckSeed = 4079;

    public static Direction[] edges(Direction Face) {
        return switch (Face.getAxis()) {
            case Y -> new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
            case Z -> new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP};
            default -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP};
        };
    }

    public static int code(Direction Face, int Slot, int Kind) {
        return (Face.ordinal() * Slots + Slot) * Kinds + Kind;
    }

    public static Direction faceOf(int Code) {
        return Direction.from3DDataValue(Code / Kinds / Slots);
    }

    public static int slotOf(int Code) {
        return Code / Kinds % Slots;
    }

    public static int kindOf(int Code) {
        return Code % Kinds;
    }

    public static int neighbourKind(int Kind) {
        for (int Index = 0; Index < BandKinds.length; Index++) {
            if (BandKinds[Index] == Kind) {
                return BandKinds[Index == BandKinds.length - 1 ? Index - 1 : Index + 1];
            }
        }
        return 0;
    }

    public static void overlays(BlockPos Pos, Direction Face, IntList Out) {
        int Own = variantAt(Pos);
        Direction[] Edges = edges(Face);
        for (int Slot = 0; Slot < Edges.length; Slot++) {
            int Other = variantAt(Pos.relative(Edges[Slot]));
            if (Other != Own) {
                Out.add(code(Face, Slot, Other));
            }
        }
        if (hash(Pos.getX(), Pos.getY(), Pos.getZ(), SpeckSeed + Face.ordinal()) < SpeckChance) {
            int Turn = (int) (hash(Pos.getX(), Pos.getY(), Pos.getZ(), SpeckSeed + 11) * 4.0);
            BlockPos Source = Pos.offset(Turn == 0 ? 3 : Turn == 1 ? -3 : 0, 0, Turn == 2 ? 3 : Turn == 3 ? -3 : 0);
            int Kind = variantAt(Source);
            Out.add(code(Face, SpeckSlot, Kind == Own ? neighbourKind(Own) : Kind));
        }
    }

    public static int variantAt(BlockPos Pos) {
        double Value = field(Pos.getX(), Pos.getY(), Pos.getZ());
        for (int Index = 0; Index < Bands.length; Index++) {
            if (Value < Bands[Index]) {
                return BandKinds[Index];
            }
        }
        return BandKinds[Bands.length];
    }
}
