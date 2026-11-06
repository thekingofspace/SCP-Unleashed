package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp001GatePlan {
    public static final int OpeningHalf = 5;
    public static final double SpringY = 14.5;
    public static final double ArchRadius = 5.6;
    public static final int Straight = 15;
    public static final int OpeningTop = 21;
    public static final int KeystoneY = 21;
    public static final int FrameHalf = 10;
    public static final int FrameTop = 25;
    public static final int Depth = 2;
    public static final double GuardianAhead = 9.0;
    public static final double GuardianLift = 3.0;
    public static final int Rubble = 9;
    public static final int MinX = -13;
    public static final int MaxX = 13;
    public static final int MinY = -1;
    public static final int MaxY = 32;
    public static final int MinZ = -6;
    public static final int MaxZ = 6;
    public static final int PlainRadius = 120;
    public static final int FlatRadius = 70;
    public static final int BareRadius = 60;
    public static final int ClearRadius = 112;
    public static final int GrassFrom = 96;
    public static final int Remains = 7;
    public static final long Salt = 0x5C0001L;

    public enum Kind {
        Stone, Carved, Worn, Keystone, Portal
    }

    private Scp001GatePlan() {
    }

    public static double mix(long X, long Y, long Z, long Seed) {
        long Value = X * 73856093L ^ Y * 19349663L ^ Z * 83492791L ^ Seed;
        Value ^= Value >>> 33;
        Value *= 0xFF51AFD7ED558CCDL;
        Value ^= Value >>> 33;
        return (Value & 0xFFFFL) / 65535.0;
    }

    public static boolean opening(int X, int Y) {
        if (Y < 0 || Y > OpeningTop || Math.abs(X) > OpeningHalf) {
            return false;
        }
        if (Y < Straight) {
            return true;
        }
        return X * X + (Y - SpringY) * (Y - SpringY) <= ArchRadius * ArchRadius;
    }

    public static boolean ring(int X, int Y) {
        if (opening(X, Y) || Y < Straight - 2) {
            return false;
        }
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dy = -1; Dy <= 1; Dy++) {
                if (opening(X + Dx, Y + Dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static Kind cell(int X, int Y, int Z, long Seed) {
        if (Y == -1 && Math.abs(X) <= FrameHalf + 2 && Math.abs(Z) <= 5) {
            return mix(X, Y, Z, Seed) > 0.2 ? Kind.Stone : Kind.Worn;
        }
        if (Math.abs(Z) <= Depth && Math.abs(X) <= FrameHalf && Y >= 0 && Y <= FrameTop) {
            if (opening(X, Y)) {
                return Z == 0 ? Kind.Portal : null;
            }
            if (X == 0 && Y == KeystoneY) {
                return Kind.Keystone;
            }
            if (ring(X, Y)) {
                return Kind.Carved;
            }
            if (Math.abs(X) > OpeningHalf && (Y <= 1 || Y == Straight - 2 || Y == Straight - 1)) {
                return Kind.Carved;
            }
            if (Y >= FrameTop - 1 && Math.abs(X) == FrameHalf && mix(X, Y, Z, Seed + 1) > 0.55) {
                return null;
            }
            return mix(X, Y, Z, Seed + 2) < 0.18 ? Kind.Worn : Kind.Stone;
        }
        if (Y == FrameTop + 1 && Math.abs(X) <= FrameHalf + 1 && Math.abs(Z) <= Depth + 1) {
            if ((Math.abs(X) == FrameHalf + 1 || Math.abs(Z) == Depth + 1) && mix(X, Y, Z, Seed + 3) < 0.22) {
                return null;
            }
            return Math.abs(Z) == Depth + 1 && Math.floorMod(X, 3) == 0 ? Kind.Carved : Kind.Stone;
        }
        if (Y >= FrameTop + 2 && Y <= FrameTop + 4 && Math.abs(Z) <= Depth) {
            int Reach = 9 - 3 * (Y - FrameTop - 2);
            if (Math.abs(X) <= Reach) {
                if (Y == FrameTop + 4) {
                    return Kind.Carved;
                }
                return mix(X, Y, Z, Seed + 4) < 0.2 ? Kind.Worn : Kind.Stone;
            }
        }
        if (Y == FrameTop + 5 && X == 0 && Math.abs(Z) <= 1) {
            return Kind.Keystone;
        }
        return null;
    }

    public static boolean insideFrame(int X, int Y, int Z) {
        return X >= MinX && X <= MaxX && Y >= MinY && Y <= MaxY && Z >= MinZ && Z <= MaxZ;
    }

    public record Piece(int X, int Y, int Z, Kind Type) {
    }

    public static List<Piece> rubble(long Seed) {
        List<Piece> Found = new ArrayList<>();
        for (int Index = 0; Index < Rubble; Index++) {
            double Angle = mix(Index, 0, 0, Seed + 9) * Math.PI * 2.0;
            double Radius = 11.0 + mix(Index, 1, 0, Seed + 9) * 9.0;
            int X = (int) Math.round(Math.cos(Angle) * Radius);
            int Z = (int) Math.round(Math.sin(Angle) * Radius);
            Found.add(new Piece(X, 0, Z, Index % 3 != 0 ? Kind.Worn : Kind.Carved));
            if (Index % 2 == 0) {
                Found.add(new Piece(X + 1, 0, Z, Kind.Stone));
            }
            if (Index % 4 == 1) {
                Found.add(new Piece(X, 1, Z, Kind.Worn));
            }
        }
        return Found;
    }

    public static List<Piece> remains(long Seed) {
        List<Piece> Found = new ArrayList<>();
        for (int Index = 0; Index < Remains; Index++) {
            double Angle = mix(Index, 5, 0, Seed + 13) * Math.PI * 2.0;
            double Radius = 44.0 + mix(Index, 6, 0, Seed + 13) * 7.0;
            Found.add(new Piece((int) Math.round(Math.cos(Angle) * Radius), 0, (int) Math.round(Math.sin(Angle) * Radius), Kind.Stone));
        }
        return Found;
    }

    public static BlockState state(Kind Type, Direction Front) {
        return switch (Type) {
            case Stone -> Scp001GateRegistry.Stone.get().defaultBlockState();
            case Carved -> Scp001GateRegistry.StoneCarved.get().defaultBlockState();
            case Worn -> Scp001GateRegistry.StoneWorn.get().defaultBlockState();
            case Keystone -> Scp001GateRegistry.Keystone.get().defaultBlockState();
            case Portal -> Scp001GateRegistry.Portal.get().defaultBlockState().setValue(Scp001GatePortalBlock.Axis, Front.getClockWise().getAxis());
        };
    }

    public static Direction right(Direction Front) {
        return Front.getClockWise();
    }

    public static BlockPos toWorld(BlockPos Centre, Direction Front, int X, int Y, int Z) {
        Direction Right = right(Front);
        return Centre.offset(Right.getStepX() * X + Front.getStepX() * Z, Y, Right.getStepZ() * X + Front.getStepZ() * Z);
    }

    public static int[] toLocal(BlockPos Centre, Direction Front, BlockPos Pos) {
        Direction Right = right(Front);
        int Dx = Pos.getX() - Centre.getX();
        int Dz = Pos.getZ() - Centre.getZ();
        return new int[]{Dx * Right.getStepX() + Dz * Right.getStepZ(), Pos.getY() - Centre.getY(), Dx * Front.getStepX() + Dz * Front.getStepZ()};
    }

    public static double smooth(double Value) {
        double Clamped = Mth.clamp(Value, 0.0, 1.0);
        return Clamped * Clamped * (3.0 - 2.0 * Clamped);
    }

    public static double noise(double X, double Z, double Scale, long Seed) {
        double Gx = X / Scale;
        double Gz = Z / Scale;
        int X0 = Mth.floor(Gx);
        int Z0 = Mth.floor(Gz);
        double Fx = smooth(Gx - X0);
        double Fz = smooth(Gz - Z0);
        double A = mix(X0, 0, Z0, Seed);
        double B = mix(X0 + 1, 0, Z0, Seed);
        double C = mix(X0, 0, Z0 + 1, Seed);
        double D = mix(X0 + 1, 0, Z0 + 1, Seed);
        return Mth.lerp(Fz, Mth.lerp(Fx, A, B), Mth.lerp(Fx, C, D));
    }

    public static int target(int Dx, int Dz, int Natural, int Ground, long Seed) {
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        double Bump = Distance < 24.0 ? 0.0 : (noise(Dx, Dz, 22.0, Seed + 21) - 0.5) * 2.4 * Math.min(1.0, (Distance - 24.0) / 16.0);
        double Flat = Ground + Bump;
        if (Distance <= FlatRadius) {
            return (int) Math.round(Flat);
        }
        double Blend = smooth((Distance - FlatRadius) / (PlainRadius - FlatRadius));
        return (int) Math.round(Mth.lerp(Blend, Flat, Natural));
    }

    @Nullable
    public static BlockState top(int Dx, int Dz, long Seed) {
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Distance > PlainRadius) {
            return null;
        }
        if (Distance > GrassFrom && mix(Dx, 1, Dz, Seed + 6) < (Distance - GrassFrom) / (PlainRadius - GrassFrom)) {
            return null;
        }
        double Noise = noise(Dx, Dz, 6.0, Seed) * 0.6 + mix(Dx, 0, Dz, Seed + 5) * 0.4;
        if (Noise < 0.42) {
            return Blocks.PACKED_MUD.defaultBlockState();
        }
        if (Noise < 0.7) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Noise < 0.86) {
            return Blocks.BROWN_CONCRETE_POWDER.defaultBlockState();
        }
        return Noise < 0.93 ? Blocks.GRAVEL.defaultBlockState() : Blocks.DIRT.defaultBlockState();
    }

    public static BlockState under(int Depth0) {
        return Depth0 <= 1 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState();
    }

    public static boolean deadBush(int Dx, int Dz, long Seed) {
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        return Distance > BareRadius + 4 && Distance < PlainRadius - 4 && mix(Dx, 7, Dz, Seed + 8) > 0.992;
    }

    public static Block remainsBlock(int Index) {
        return Index % 3 == 0 ? Blocks.SKELETON_SKULL : Blocks.BONE_BLOCK;
    }
}
