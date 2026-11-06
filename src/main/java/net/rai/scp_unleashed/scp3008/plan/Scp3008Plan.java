package net.rai.scp_unleashed.scp3008.plan;

import net.rai.scp_unleashed.scp3008.furniture.electronics.Scp3008ElectronicsLedger;

import java.util.ArrayList;
import java.util.List;

public final class Scp3008Plan {
    public static final int FloorY = 64;
    public static final int FloorBase = 60;
    public static final int Stand = FloorY + 1;
    public static final int CeilingY = 320;
    public static final int CeilingTop = 323;
    public static final int Unit = 40;
    public static final int Row = 120;
    public static final int Shift = 3;
    public static final int Lane = 10;
    public static final int PillarLow = 3;
    public static final int PillarHigh = 6;
    public static final int[] PillarRows = {3, 43, 83};
    public static final int LightY = FloorY + 16;
    public static final int SignTop = FloorY + 11;
    public static final int SignBottom = FloorY + 10;
    public static final int BandY = FloorY + 12;
    public static final int EmergencyY = FloorY + 4;
    public static final int Site = 288;
    public static final int Warp = 72;
    public static final double WarpScale = 260.0;
    public static final int ExitSpanX = 64;
    public static final int ExitSpanZ = 21;
    public static final int ExitMarginX = 4;
    public static final int ExitMarginZ = 2;
    public static final int ExitStart = 40;
    public static final int ExitLength = 34;
    public static final int ExitWallOffset = 14;
    public static final int ExitDoorX = 24;
    public static final int FoodOdds = 16;
    public static final int FoodLength = 34;
    public static final int[] FoodStarts = {11, 46, 84};
    public static final int EmergencyOdds = 4;
    public static final int ArrivalSpanX = 4000;
    public static final int ArrivalSpanZ = 1400;
    public static final int ContentLow = 10;
    public static final int ContentEnd = 119;
    public static final int PlotGap = 2;

    public static final String[] Departments = {"living", "dining", "kitchen", "bedroom", "bathroom", "office", "children", "lighting", "textiles", "outdoor",
            "warehouse", "electronics"};
    public static final int Living = 0;
    public static final int Dining = 1;
    public static final int Kitchen = 2;
    public static final int Bedroom = 3;
    public static final int Bathroom = 4;
    public static final int Office = 5;
    public static final int Children = 6;
    public static final int Lighting = 7;
    public static final int Textiles = 8;
    public static final int Outdoor = 9;
    public static final int Warehouse = 10;
    public static final int Electronics = 11;
    public static final int Classic = 11;

    public static final int RoomPlot = 0;
    public static final int MarketPlot = 1;
    public static final int StoragePlot = 2;
    public static final int GardenPlot = 3;
    public static final int FoodPlot = 4;
    public static final int RackPlot = 5;
    public static final int ExitPlot = 6;

    public record Plot(int Type, int Start, int Length, int Salt) {
    }

    private static final ThreadLocal<long[]> Recent = ThreadLocal.withInitial(() -> {
        long[] Slots = new long[16];
        for (int Index = 0; Index < Slots.length; Index += 2) {
            Slots[Index] = Long.MIN_VALUE;
            Slots[Index + 1] = -1;
        }
        return Slots;
    });

    private Scp3008Plan() {
    }

    public static int unitX(int X) {
        return Math.floorDiv(X + Shift, Unit);
    }

    public static int unitZ(int Z) {
        return Math.floorDiv(Z + Shift, Row);
    }

    public static int originX(int Ux) {
        return Ux * Unit - Shift;
    }

    public static int originZ(int Uz) {
        return Uz * Row - Shift;
    }

    public static int localX(int X) {
        return X - originX(unitX(X));
    }

    public static int localZ(int Z) {
        return Z - originZ(unitZ(Z));
    }

    public static boolean inLane(int X, int Z) {
        return localX(X) < Lane || localZ(Z) < Lane;
    }

    public static boolean isPillar(int X, int Z) {
        int Lx = localX(X);
        int Lz = localZ(Z);
        if (Lx < PillarLow || Lx > PillarHigh) {
            return false;
        }
        for (int Start : PillarRows) {
            if (Lz >= Start && Lz <= Start + 3) {
                return true;
            }
        }
        return false;
    }

    public static int departmentAt(int X, int Z) {
        return cachedDepartment(unitX(X), unitZ(Z));
    }

    public static int cachedDepartment(int Ux, int Uz) {
        long Key = (long) Ux << 32 | Uz & 0xFFFFFFFFL;
        long[] Slots = Recent.get();
        int Index = Math.floorMod(Ux * 31 + Uz, 8) * 2;
        long Stamp = Scp3008ElectronicsLedger.epoch();
        if (Slots[Index] == Key && Slots[Index + 1] >= 0 && Slots[Index + 1] >>> 8 == Stamp) {
            return (int) (Slots[Index + 1] & 0xFF);
        }
        int Found = department(Ux, Uz);
        Slots[Index] = Key;
        Slots[Index + 1] = Stamp << 8 | Found;
        return Found;
    }

    public static int department(int Ux, int Uz) {
        long Site = site(Ux, Uz);
        int Sx = Scp3008ElectronicsLedger.highOf(Site);
        int Sz = Scp3008ElectronicsLedger.lowOf(Site);
        return Scp3008ElectronicsLedger.claim(Sx, Sz, classic(Sx, Sz));
    }

    public static int classic(int Sx, int Sz) {
        return Scp3008Hash.pick(Scp3008Hash.mix(Sx, Sz, 47), Classic);
    }

    public static int classicDepartment(int Ux, int Uz) {
        long Site = site(Ux, Uz);
        return classic(Scp3008ElectronicsLedger.highOf(Site), Scp3008ElectronicsLedger.lowOf(Site));
    }

    public static long site(int Ux, int Uz) {
        double Cx = originX(Ux) + 25.0;
        double Cz = originZ(Uz) + 65.0;
        double Wx = Cx + Warp * (Scp3008Hash.noise(Cx / WarpScale, Cz / WarpScale, 11) * 2.0 - 1.0);
        double Wz = Cz + Warp * (Scp3008Hash.noise(Cx / WarpScale + 37.0, Cz / WarpScale - 19.0, 23) * 2.0 - 1.0);
        int Gx = (int) Math.floor(Wx / Site);
        int Gz = (int) Math.floor(Wz / Site);
        double Best = Double.MAX_VALUE;
        int BestX = Gx;
        int BestZ = Gz;
        for (int Ox = -1; Ox <= 1; Ox++) {
            for (int Oz = -1; Oz <= 1; Oz++) {
                int Sx = Gx + Ox;
                int Sz = Gz + Oz;
                double Px = (Sx + 0.15 + 0.7 * Scp3008Hash.unit(Scp3008Hash.mix(Sx, Sz, 31))) * Site;
                double Pz = (Sz + 0.15 + 0.7 * Scp3008Hash.unit(Scp3008Hash.mix(Sx, Sz, 37))) * Site;
                double Distance = (Px - Wx) * (Px - Wx) + (Pz - Wz) * (Pz - Wz);
                if (Distance < Best) {
                    Best = Distance;
                    BestX = Sx;
                    BestZ = Sz;
                }
            }
        }
        return Scp3008ElectronicsLedger.key(BestX, BestZ);
    }

    public static int exitRegionX(int Ux) {
        return Math.floorDiv(Ux, ExitSpanX);
    }

    public static int exitRegionZ(int Uz) {
        return Math.floorDiv(Uz, ExitSpanZ);
    }

    public static int exitUnitX(int Rx, int Rz) {
        return Rx * ExitSpanX + ExitMarginX + Scp3008Hash.pick(Scp3008Hash.mix(Rx, Rz, 211), ExitSpanX - 2 * ExitMarginX);
    }

    public static int exitUnitZ(int Rx, int Rz) {
        return Rz * ExitSpanZ + ExitMarginZ + Scp3008Hash.pick(Scp3008Hash.mix(Rx, Rz, 223), ExitSpanZ - 2 * ExitMarginZ);
    }

    public static boolean isExit(int Ux, int Uz) {
        int Rx = exitRegionX(Ux);
        int Rz = exitRegionZ(Uz);
        return exitUnitX(Rx, Rz) == Ux && exitUnitZ(Rx, Rz) == Uz;
    }

    public static int[] exitDoor(int Rx, int Rz) {
        int Ux = exitUnitX(Rx, Rz);
        int Uz = exitUnitZ(Rx, Rz);
        return new int[]{originX(Ux) + ExitDoorX, Stand, originZ(Uz) + ExitStart + ExitWallOffset};
    }

    public static int[] nearestExit(int X, int Z) {
        int Rx = exitRegionX(unitX(X));
        int Rz = exitRegionZ(unitZ(Z));
        int[] Best = null;
        long BestDistance = Long.MAX_VALUE;
        for (int Ox = -1; Ox <= 1; Ox++) {
            for (int Oz = -1; Oz <= 1; Oz++) {
                int[] Door = exitDoor(Rx + Ox, Rz + Oz);
                long Dx = Door[0] + 1 - X;
                long Dz = Door[2] - Z;
                long Distance = Dx * Dx + Dz * Dz;
                if (Distance < BestDistance) {
                    BestDistance = Distance;
                    Best = Door;
                }
            }
        }
        return Best;
    }

    public static boolean isFoodCourt(int Ux, int Uz) {
        return !isExit(Ux, Uz) && Scp3008Hash.pick(Scp3008Hash.mix(Ux, Uz, 61), FoodOdds) == 0;
    }

    public static int[] arrivalUnit(long Seed) {
        int Low = (int) Seed;
        int High = (int) (Seed >>> 32);
        int Ux = Scp3008Hash.pick(Scp3008Hash.mix(Low, High, 307), ArrivalSpanX) - ArrivalSpanX / 2;
        int Uz = Scp3008Hash.pick(Scp3008Hash.mix(Low, High, 311), ArrivalSpanZ) - ArrivalSpanZ / 2;
        while (isExit(Ux, Uz) || isExit(Ux - 1, Uz)) {
            Uz++;
        }
        return new int[]{Ux, Uz};
    }

    public static int[] arrival(long Seed) {
        int[] Unit0 = arrivalUnit(Seed);
        return new int[]{originX(Unit0[0]) + 1, Stand, originZ(Unit0[1]) + 25};
    }

    public static List<Plot> plots(int Ux, int Uz) {
        List<Plot> Found = new ArrayList<>();
        int Department = cachedDepartment(Ux, Uz);
        if (isExit(Ux, Uz)) {
            Found.add(new Plot(StoragePlot, 14, 10, Scp3008Hash.mix(Ux, Uz, 401)));
            Found.add(new Plot(ExitPlot, ExitStart, ExitLength, Scp3008Hash.mix(Ux, Uz, 403)));
            Found.add(new Plot(StoragePlot, 96, 10, Scp3008Hash.mix(Ux, Uz, 409)));
            return Found;
        }
        if (isFoodCourt(Ux, Uz)) {
            int Start = FoodStarts[Scp3008Hash.pick(Scp3008Hash.mix(Ux, Uz, 67), FoodStarts.length)];
            pack(Found, Ux, Uz, Department, ContentLow + 1, Start - PlotGap);
            Found.add(new Plot(FoodPlot, Start, FoodLength, Scp3008Hash.mix(Ux, Uz, 71)));
            pack(Found, Ux, Uz, Department, Start + FoodLength + PlotGap, ContentEnd);
        } else {
            pack(Found, Ux, Uz, Department, ContentLow + 1, ContentEnd);
        }
        return Found;
    }

    private static void pack(List<Plot> Found, int Ux, int Uz, int Department, int From, int To) {
        int Cursor = From;
        int Index = 0;
        while (To - Cursor >= 8) {
            int Hash = Scp3008Hash.mix(Ux, Uz, From * 7 + Index * 131 + 17);
            int Type = typeFor(Department, Hash);
            int Length = lengthFor(Type, Hash >>> 9);
            if (Cursor + Length > To) {
                Type = StoragePlot;
                Length = To - Cursor >= 10 ? 10 : 8;
            }
            Found.add(new Plot(Type, Cursor, Length, Scp3008Hash.mix(Ux, Uz, Cursor * 3 + 5)));
            Cursor += Length + PlotGap;
            Index++;
        }
    }

    public static int typeFor(int Department, int Hash) {
        double Roll = Scp3008Hash.unit(Hash);
        return switch (Department) {
            case Warehouse -> Roll < 0.8 ? RackPlot : Roll < 0.9 ? RoomPlot : StoragePlot;
            case Outdoor -> Roll < 0.5 ? GardenPlot : Roll < 0.8 ? RoomPlot : Roll < 0.9 ? MarketPlot : StoragePlot;
            case Lighting, Textiles -> Roll < 0.5 ? MarketPlot : Roll < 0.85 ? RoomPlot : StoragePlot;
            case Electronics -> Roll < 0.6 ? RoomPlot : Roll < 0.9 ? MarketPlot : StoragePlot;
            default -> Roll < 0.6 ? RoomPlot : Roll < 0.85 ? MarketPlot : StoragePlot;
        };
    }

    public static int lengthFor(int Type, int Hash) {
        int Pick = Scp3008Hash.pick(Hash, 3);
        return switch (Type) {
            case RoomPlot -> 13 + Pick * 2;
            case MarketPlot -> 17 + Pick * 5;
            case GardenPlot -> 16 + Pick * 4;
            case RackPlot -> 21 + Pick * 4;
            default -> 8 + Math.min(Pick, 1) * 2;
        };
    }

    public static int[] goodsFor(int Department) {
        return switch (Department) {
            case Living -> new int[]{5, 3, 2, 0};
            case Dining -> new int[]{1, 5, 0};
            case Kitchen -> new int[]{1, 0};
            case Bedroom -> new int[]{6, 3, 0};
            case Bathroom -> new int[]{6, 1};
            case Office -> new int[]{0, 5};
            case Children -> new int[]{7, 3};
            case Lighting -> new int[]{4};
            case Textiles -> new int[]{6, 3};
            case Outdoor -> new int[]{2};
            case Electronics -> new int[]{0, 0, 4};
            default -> new int[]{0};
        };
    }

    public static int rugFor(int Department, int Salt) {
        int[] Colours = switch (Department) {
            case Living -> new int[]{0, 1, 9};
            case Dining -> new int[]{1, 5};
            case Kitchen -> new int[]{0, 6};
            case Bedroom -> new int[]{2, 3, 8};
            case Bathroom -> new int[]{9, 2};
            case Office -> new int[]{6, 0};
            case Children -> new int[]{7, 4, 3};
            case Lighting -> new int[]{6};
            case Textiles -> new int[]{5, 8, 4};
            case Outdoor -> new int[]{4};
            case Electronics -> new int[]{6, 0, 3};
            default -> new int[]{0};
        };
        return Colours[Scp3008Hash.pick(Salt, Colours.length)];
    }

    public static int[] rugArea(int Department, int Width) {
        return switch (Department) {
            case Kitchen -> new int[]{6, 9, 2, Width - 3};
            case Bathroom -> new int[]{8, 10, 2, Math.min(Width - 3, 5)};
            case Warehouse -> null;
            default -> new int[]{3, 10, 1, Width - 2};
        };
    }
}
