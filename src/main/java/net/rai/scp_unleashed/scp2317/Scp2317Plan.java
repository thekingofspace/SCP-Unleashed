package net.rai.scp_unleashed.scp2317;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class Scp2317Plan {
    public static final int Height = 128;
    public static final int Ground = 84;
    public static final double RingRadius = 11.0;
    public static final int Pillars = 7;
    public static final double PlazaRadius = 15.0;
    public static final double FlatRadius = 18.0;
    public static final double DuneRadius = 34.0;
    public static final int DoorZ = 36;
    public static final int FarDoorX = 0;
    public static final int CaveY = 47;
    public static final double CaveRadius = 34.0;
    public static final double CaveHalf = 17.0;
    public static final int CaveFloor = 30;
    public static final double CavePillarRadius = 19.0;
    public static final int DaisHalf = 2;
    public static final int CaveDaisHalf = 5;
    public static final double WalkRadius = 13.0;
    public static final double LeaveRadius = 40.0;
    public static final Direction DoorFront = Direction.NORTH;

    private Scp2317Plan() {
    }

    public static double smooth(double Low, double High, double Value) {
        double Share = Math.min(1.0, Math.max(0.0, (Value - Low) / (High - Low)));
        return Share * Share * (3.0 - 2.0 * Share);
    }

    public static double mix(int X, int Y, int Z, int Seed) {
        long Value = ((long) X * 73856093L) ^ ((long) Y * 19349663L) ^ ((long) Z * 83492791L) ^ ((long) Seed * 2654435761L);
        Value &= 0xFFFFFFFFL;
        Value = (((Value >>> 16) ^ Value) * 0x45D9F3BL) & 0xFFFFFFFFL;
        Value = (((Value >>> 16) ^ Value) * 0x45D9F3BL) & 0xFFFFFFFFL;
        Value = (Value >>> 16) ^ Value;
        return (Value & 0xFFFFL) / 65535.0;
    }

    public static double dune(int X, int Z) {
        double Value = 4.0 * Math.sin(X * 0.045 + 0.6 * Math.sin(Z * 0.03)) * Math.cos(Z * 0.035);
        Value += 2.0 * Math.sin((X + Z) * 0.08) + 1.5 * Math.sin(X * 0.13 - Z * 0.09);
        return Value;
    }

    public static int surface(int X, int Z) {
        double Radius = Math.sqrt((double) X * X + (double) Z * Z);
        double Door = Math.sqrt((X - 0.5) * (X - 0.5) + (double) (Z - DoorZ) * (Z - DoorZ));
        double Share = smooth(FlatRadius, DuneRadius, Radius) * smooth(5.0, 12.0, Door);
        return Ground + (int) Math.floor(dune(X, Z) * Share + 0.5);
    }

    public static int[] pillarCentre(int Index, double Radius) {
        double Angle = -Math.PI / 2.0 + Index * 2.0 * Math.PI / Pillars;
        return new int[]{(int) Math.floor(Math.cos(Angle) * Radius + 0.5), (int) Math.floor(Math.sin(Angle) * Radius + 0.5)};
    }

    public static int pillarAt(int X, int Z, double Radius) {
        for (int Index = 0; Index < Pillars; Index++) {
            int[] Centre = pillarCentre(Index, Radius);
            if (Math.abs(X - Centre[0]) <= 1 && Math.abs(Z - Centre[1]) <= 1) {
                return Index;
            }
        }
        return -1;
    }

    public static int pillarHeight(int Index) {
        return 11 + (int) (mix(Index, 0, 0, 2317) * 5.0);
    }

    public static boolean caveOpen(int X, int Y, int Z) {
        if (Y <= CaveFloor) {
            return false;
        }
        double Wobble = (mix(Math.floorDiv(X, 4), Math.floorDiv(Y, 3), Math.floorDiv(Z, 4), 41) - 0.5) * 0.18;
        double Shape = (X / CaveRadius) * (X / CaveRadius) + ((Y - CaveY) / CaveHalf) * ((Y - CaveY) / CaveHalf) + (Z / CaveRadius) * (Z / CaveRadius);
        return Shape < 1.0 + Wobble;
    }

    public static double shell(int X, int Y, int Z) {
        return (X / CaveRadius) * (X / CaveRadius) + ((Y - CaveY) / CaveHalf) * ((Y - CaveY) / CaveHalf) + (Z / CaveRadius) * (Z / CaveRadius);
    }

    public static int caveCeiling(int X, int Z) {
        for (int Y = CaveY + (int) CaveHalf + 3; Y > CaveFloor; Y--) {
            if (caveOpen(X, Y, Z)) {
                return Y;
            }
        }
        return CaveFloor;
    }

    public static boolean nearCave(int X, int Z) {
        return X * X + Z * Z <= (CaveRadius + 6.0) * (CaveRadius + 6.0);
    }

    @Nullable
    public static BlockState ring(int X, int Y, int Z) {
        int Index = pillarAt(X, Z, RingRadius);
        if (Index >= 0) {
            int[] Centre = pillarCentre(Index, RingRadius);
            int Dx = X - Centre[0];
            int Dz = Z - Centre[1];
            int Tall = pillarHeight(Index);
            int Rise = Y - Ground;
            boolean Corner = Math.abs(Dx) == 1 && Math.abs(Dz) == 1;
            if (Rise >= 1 && Rise <= Tall) {
                if (Rise == Tall && Corner && mix(X, Y, Z, 7) < 0.6) {
                    return null;
                }
                if (Rise == Tall - 1 && Corner && mix(X, Y, Z, 8) < 0.25) {
                    return null;
                }
                if (Rise == 1) {
                    return Blocks.POLISHED_TUFF.defaultBlockState();
                }
                if (Rise == Tall) {
                    return Blocks.CHISELED_TUFF.defaultBlockState();
                }
                if (Rise == 4 || Rise == 9) {
                    return Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
                }
                return mix(X, Y, Z, 11) < 0.18 ? Blocks.TUFF.defaultBlockState() : Blocks.TUFF_BRICKS.defaultBlockState();
            }
            if (Rise == 0) {
                return Blocks.POLISHED_TUFF.defaultBlockState();
            }
            return null;
        }
        if (Y == Ground + 1 && Math.abs(X) <= DaisHalf && Math.abs(Z) <= DaisHalf) {
            if (X == 0 && Z == 0) {
                return Blocks.CHISELED_TUFF.defaultBlockState();
            }
            return Math.max(Math.abs(X), Math.abs(Z)) <= 1 ? Blocks.POLISHED_TUFF.defaultBlockState() : Blocks.TUFF_BRICKS.defaultBlockState();
        }
        return null;
    }

    public static BlockState top(int X, int Z) {
        double Radius = Math.sqrt((double) X * X + (double) Z * Z);
        if (Radius <= PlazaRadius) {
            double Bury = smooth(PlazaRadius - 5.0, PlazaRadius, Radius) + mix(X, 0, Z, 13) * 0.6;
            if (Bury > 0.75) {
                return Blocks.SAND.defaultBlockState();
            }
            return (X + Z) % 2 == 0 ? Blocks.CUT_SANDSTONE.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        }
        return Blocks.SAND.defaultBlockState();
    }

    public static boolean bush(int X, int Z) {
        double Radius = Math.sqrt((double) X * X + (double) Z * Z);
        return Radius > PlazaRadius + 3.0 && mix(X, 1, Z, 17) < 0.006;
    }

    public static int cavePillarTop(int X, int Z) {
        return caveCeiling(X, Z) + 2;
    }

    public static BlockState cavePillar(int Index, int Dx, int Dz, int Y) {
        int Rise = Y - CaveFloor;
        if (Rise == 1) {
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        if (Rise == 7 || Rise == 15 || Rise == 23) {
            return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        }
        return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    public static BlockState cracked(BlockState Before, int X, int Y, int Z) {
        if (Before.is(Blocks.DEEPSLATE_BRICKS) && mix(X, Y, Z, 53) < 0.7) {
            return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (Before.is(Blocks.CHISELED_DEEPSLATE) && mix(X, Y, Z, 59) < 0.5) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        return Before;
    }

    public static BlockState state(int X, int Y, int Z) {
        if (Y <= 0) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        int Top = surface(X, Z);
        if (Y > Top) {
            BlockState Ring = ring(X, Y, Z);
            if (Ring != null) {
                return Ring;
            }
            if (Y == Top + 1 && bush(X, Z)) {
                return Blocks.DEAD_BUSH.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        }
        if (nearCave(X, Z) && Y >= CaveFloor - 2 && Y <= CaveY + (int) CaveHalf + 4) {
            int Index = pillarAt(X, Z, CavePillarRadius);
            if (Index >= 0 && Y > CaveFloor && Y <= cavePillarTop(X, Z)) {
                int[] Centre = pillarCentre(Index, CavePillarRadius);
                return cavePillar(Index, X - Centre[0], Z - Centre[1], Y);
            }
            if (caveOpen(X, Y, Z)) {
                if (!caveOpen(X, Y + 1, Z) && mix(X, Y, Z, 61) < 0.07) {
                    return Blocks.HANGING_ROOTS.defaultBlockState();
                }
                return Blocks.AIR.defaultBlockState();
            }
            if (Y == CaveFloor && caveOpen(X, Y + 1, Z)) {
                if (Math.abs(X) <= CaveDaisHalf && Math.abs(Z) <= CaveDaisHalf) {
                    return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                }
                double Roll = mix(X, Y, Z, 67);
                return Roll < 0.5 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Roll < 0.8 ? Blocks.TUFF.defaultBlockState() : Blocks.SAND.defaultBlockState();
            }
            double Shell = shell(X, Y, Z);
            if (Shell < 1.45) {
                double Roll = mix(X, Y, Z, 71);
                if (Y > CaveY && Shell < 1.2 && Roll < 0.12) {
                    return Blocks.ROOTED_DIRT.defaultBlockState();
                }
                return Roll < 0.55 ? Blocks.DEEPSLATE.defaultBlockState() : Roll < 0.8 ? Blocks.TUFF.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState();
            }
        }
        if (Y == Top) {
            return top(X, Z);
        }
        if (Y > Top - 4) {
            return Blocks.SAND.defaultBlockState();
        }
        return Y < 12 && mix(X, Y, Z, 73) < 0.3 ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState();
    }
}
