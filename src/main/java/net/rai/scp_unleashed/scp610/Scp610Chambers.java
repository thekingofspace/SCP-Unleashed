package net.rai.scp_unleashed.scp610;

import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp610Chambers {
    public static final String Tunnel = "tunnel";
    public static final String Cavern = "cavern";
    public static final String Nursery = "nursery";
    public static final String GreatCavern = "great_cavern";
    public static final String Lord = "lord";
    public static final String LordTunnel = "lord_tunnel";
    public static final double CavernWide = 4.5;
    public static final double CavernTall = 3.0;
    public static final double GreatWide = 7.5;
    public static final double GreatTall = 4.5;
    public static final int NurseryHalf = 5;
    public static final int NurseryTall = 4;
    public static final double LordWide = 8.5;
    public static final double LordTall = 5.5;
    public static final int PillarStep = 3;
    public static final byte Carve = 0;
    public static final byte Shell = 1;
    public static final byte Pillar = 2;
    public static final byte Floor = 3;
    public static final byte Light = 4;

    public record Op(long Pos, byte Kind) {
    }

    public static final class Chamber {
        public final String Kind;
        public final BlockPos From;
        public final BlockPos To;
        public int Progress;

        public Chamber(String Kind, BlockPos From, BlockPos To, int Progress) {
            this.Kind = Kind;
            this.From = From.immutable();
            this.To = To.immutable();
            this.Progress = Progress;
        }

        public CompoundTag save() {
            CompoundTag Saved = new CompoundTag();
            Saved.putString("Kind", Kind);
            Saved.putLong("From", From.asLong());
            Saved.putLong("To", To.asLong());
            Saved.putInt("Progress", Progress);
            return Saved;
        }

        public static Chamber load(CompoundTag Saved) {
            return new Chamber(Saved.getString("Kind"), BlockPos.of(Saved.getLong("From")), BlockPos.of(Saved.getLong("To")), Saved.getInt("Progress"));
        }

        public boolean lord() {
            return Lord.equals(Kind);
        }

        public boolean deep() {
            return Kind.startsWith(Lord);
        }
    }

    private Scp610Chambers() {
    }

    public static List<Op> ops(Chamber Room) {
        return switch (Room.Kind) {
            case Tunnel, LordTunnel -> tunnel(Room.From, Room.To);
            case Cavern -> ellipsoid(Room.To, Room.From, CavernWide, CavernTall, false);
            case GreatCavern -> ellipsoid(Room.To, Room.From, GreatWide, GreatTall, true);
            case Lord -> ellipsoid(Room.To, Room.From, LordWide, LordTall, true);
            default -> nursery(Room.To, Room.From);
        };
    }

    public static List<BlockPos> line(BlockPos From, BlockPos To) {
        List<BlockPos> Points = new ArrayList<>();
        int Steps = Math.max(1, Math.max(Math.abs(To.getX() - From.getX()), Math.max(Math.abs(To.getY() - From.getY()), Math.abs(To.getZ() - From.getZ()))));
        for (int Step = 0; Step <= Steps; Step++) {
            double Share = Step / (double) Steps;
            double X = Mth.lerp(Share, From.getX(), To.getX()) + 0.5;
            double Y = Mth.lerp(Share, From.getY(), To.getY()) + 0.5;
            double Z = Mth.lerp(Share, From.getZ(), To.getZ()) + 0.5;
            Points.add(BlockPos.containing(X, Y, Z));
        }
        return Points;
    }

    public static List<Op> tunnel(BlockPos From, BlockPos To) {
        List<BlockPos> Points = line(From, To);
        Set<Long> Inside = new HashSet<>();
        for (BlockPos Point : Points) {
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dy = -1; Dy <= 1; Dy++) {
                    for (int Dz = -1; Dz <= 1; Dz++) {
                        Inside.add(Point.offset(Dx, Dy, Dz).asLong());
                    }
                }
            }
        }
        Map<Long, Byte> Found = new LinkedHashMap<>();
        for (BlockPos Point : Points) {
            for (int Dx = -2; Dx <= 2; Dx++) {
                for (int Dy = -2; Dy <= 2; Dy++) {
                    for (int Dz = -2; Dz <= 2; Dz++) {
                        long Key = Point.offset(Dx, Dy, Dz).asLong();
                        if (Found.containsKey(Key)) {
                            continue;
                        }
                        boolean Core = Math.max(Math.abs(Dx), Math.max(Math.abs(Dy), Math.abs(Dz))) <= 1;
                        if (Core) {
                            Found.put(Key, Carve);
                        } else if (!Inside.contains(Key)) {
                            Found.put(Key, Dy == -2 ? Floor : Shell);
                        }
                    }
                }
            }
        }
        List<Op> Made = new ArrayList<>();
        for (Map.Entry<Long, Byte> Entry : Found.entrySet()) {
            Made.add(new Op(Entry.getKey(), Entry.getValue()));
        }
        return Made;
    }

    public static List<Op> ellipsoid(BlockPos Centre, BlockPos Entry, double Wide, double Half, boolean Lit) {
        int Reach = (int) Math.ceil(Wide);
        int Height = (int) Math.round(Half * 2.0);
        Set<Long> Inside = new HashSet<>();
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Flat = (Dx * Dx + Dz * Dz) / (Wide * Wide);
                for (int Dy = 0; Dy < Height; Dy++) {
                    double Lift = (Dy + 0.5 - Half) / Half;
                    if (Flat + Lift * Lift <= 1.0 || Dy < Half && Flat <= 0.8) {
                        Inside.add(Centre.offset(Dx, Dy, Dz).asLong());
                    }
                }
            }
        }
        return shellAround(Inside, Entry, Lit);
    }

    public static List<Op> nursery(BlockPos Centre, BlockPos Entry) {
        Set<Long> Inside = new HashSet<>();
        Set<Long> Pillars = new HashSet<>();
        for (int Dx = -NurseryHalf; Dx <= NurseryHalf; Dx++) {
            for (int Dz = -NurseryHalf; Dz <= NurseryHalf; Dz++) {
                boolean Column = Dx != 0 && Dz != 0 && Math.floorMod(Dx, PillarStep) == 0 && Math.floorMod(Dz, PillarStep) == 0;
                for (int Dy = 0; Dy < NurseryTall; Dy++) {
                    long Key = Centre.offset(Dx, Dy, Dz).asLong();
                    Inside.add(Key);
                    if (Column) {
                        Pillars.add(Key);
                    }
                }
            }
        }
        List<Op> Made = shellAround(Inside, Entry, false);
        List<Op> Marked = new ArrayList<>();
        for (Op Each : Made) {
            Marked.add(Pillars.contains(Each.Pos()) ? new Op(Each.Pos(), Pillar) : Each);
        }
        return Marked;
    }

    private static List<Op> shellAround(Set<Long> Inside, BlockPos Entry, boolean Lit) {
        Map<Long, Byte> Found = new LinkedHashMap<>();
        List<Long> Sorted = new ArrayList<>(Inside);
        Sorted.sort(Comparator.comparingDouble(Key -> BlockPos.of(Key).distSqr(Entry)));
        for (long Key : Sorted) {
            Found.put(Key, Carve);
            BlockPos Pos = BlockPos.of(Key);
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dy = -1; Dy <= 1; Dy++) {
                    for (int Dz = -1; Dz <= 1; Dz++) {
                        long Near = Pos.offset(Dx, Dy, Dz).asLong();
                        if (Inside.contains(Near) || Found.containsKey(Near)) {
                            continue;
                        }
                        byte Kind = Dy == -1 && Dx == 0 && Dz == 0 ? Floor : Shell;
                        if (Lit && Kind == Shell && Math.floorMod(Near * 31L + 7L, 23L) == 0L) {
                            Kind = Light;
                        }
                        Found.put(Near, Kind);
                    }
                }
            }
        }
        List<Op> Made = new ArrayList<>();
        for (Map.Entry<Long, Byte> Entry0 : Found.entrySet()) {
            Made.add(new Op(Entry0.getKey(), Entry0.getValue()));
        }
        return Made;
    }

    public static int interiorCount(List<Op> Made) {
        int Count = 0;
        for (Op Each : Made) {
            if (Each.Kind() == Carve) {
                Count++;
            }
        }
        return Count;
    }
}
