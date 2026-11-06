package net.rai.scp_unleashed.raid.base;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BaseHeatmap {
    public static final int CellShift = 5;
    public static final int CellSize = 1 << CellShift;
    public static final double HalfLife = 96000.0;
    public static final double RecentHalfLife = 12000.0;
    public static final double RecentWeight = 6.0;
    public static final int MaxCells = 2048;
    public static final double PruneBelow = 0.05;
    public static final double MinTotal = 6.0;
    public static final double FloorShare = 0.1;
    public static final double FloorHeat = 0.4;
    public static final int ClusterReach = 4;
    public static final double Saturation = 400.0;
    public static final double MinFootprint = 10.0;
    public static final double FootprintScale = 1.45;
    public static final int MaxStashes = 48;
    public static final int Sleep = 1;
    public static final int Storage = 2;
    public static final int Machine = 4;
    public static final int Respawn = 8;

    private final Map<Long, Cell> Cells = new HashMap<>();
    private final LinkedHashMap<Long, Long> Stashes = new LinkedHashMap<>();

    public static final class Cell {
        private final long Key;
        private double Weight;
        private double SumX;
        private double SumY;
        private double SumZ;
        private double SumR;
        private double Recent;
        private long Stamp;
        private int Flags;

        private Cell(long Key, long Stamp) {
            this.Key = Key;
            this.Stamp = Stamp;
        }

        public long key() {
            return Key;
        }

        public int flags() {
            return Flags;
        }

        public long stamp() {
            return Stamp;
        }

        public double[] sums() {
            return new double[]{Weight, SumX, SumY, SumZ, SumR, Recent};
        }

        private void decay(long Now) {
            if (Now <= Stamp) {
                return;
            }
            double Factor = Math.pow(0.5, (Now - Stamp) / HalfLife);
            Weight *= Factor;
            SumX *= Factor;
            SumY *= Factor;
            SumZ *= Factor;
            SumR *= Factor;
            Recent *= Math.pow(0.5, (Now - Stamp) / RecentHalfLife);
            Stamp = Now;
        }

        public double heat(long Now) {
            if (Now <= Stamp) {
                return Weight;
            }
            return Weight * Math.pow(0.5, (Now - Stamp) / HalfLife);
        }

        public double recent(long Now) {
            if (Now <= Stamp) {
                return Recent;
            }
            return Recent * Math.pow(0.5, (Now - Stamp) / RecentHalfLife);
        }

        private double meanX() {
            return Weight <= 0.0 ? CellSize * 0.5 : SumX / Weight;
        }

        private double meanZ() {
            return Weight <= 0.0 ? CellSize * 0.5 : SumZ / Weight;
        }

        private double meanY() {
            return Weight <= 0.0 ? 64.0 : SumY / Weight;
        }

        private double spread() {
            if (Weight <= 0.0) {
                return 0.0;
            }
            double Mx = meanX();
            double Mz = meanZ();
            return Math.max(0.0, SumR / Weight - Mx * Mx - Mz * Mz);
        }
    }

    public static long key(int CellX, int CellZ) {
        return ((long) CellX << 32) | (CellZ & 0xFFFFFFFFL);
    }

    public static int cellX(long Key) {
        return (int) (Key >> 32);
    }

    public static int cellZ(long Key) {
        return (int) Key;
    }

    public static long keyAt(int X, int Z) {
        return key(X >> CellShift, Z >> CellShift);
    }

    public int size() {
        return Cells.size();
    }

    public Collection<Cell> cells() {
        return Cells.values();
    }

    public void add(int X, int Y, int Z, double Amount, long Now, int Marks) {
        if (Amount <= 0.0) {
            return;
        }
        int CellX = X >> CellShift;
        int CellZ = Z >> CellShift;
        long Key = key(CellX, CellZ);
        Cell Found = Cells.get(Key);
        if (Found == null) {
            Found = new Cell(Key, Now);
            Cells.put(Key, Found);
        }
        Found.decay(Now);
        double LocalX = X - (CellX << CellShift) + 0.5;
        double LocalZ = Z - (CellZ << CellShift) + 0.5;
        Found.Weight += Amount;
        Found.SumX += Amount * LocalX;
        Found.SumY += Amount * Y;
        Found.SumZ += Amount * LocalZ;
        Found.SumR += Amount * (LocalX * LocalX + LocalZ * LocalZ);
        Found.Recent += Amount;
        Found.Flags |= Marks;
        if (Cells.size() > MaxCells) {
            prune(Now);
            if (Cells.size() > MaxCells) {
                trim(Now);
            }
        }
    }

    public void restore(long Key, double Weight, double SumX, double SumY, double SumZ, double SumR, double Recent, long Stamp, int Marks) {
        Cell Found = new Cell(Key, Stamp);
        Found.Weight = Weight;
        Found.Recent = Recent;
        Found.SumX = SumX;
        Found.SumY = SumY;
        Found.SumZ = SumZ;
        Found.SumR = SumR;
        Found.Flags = Marks;
        Cells.put(Key, Found);
    }

    public int prune(long Now) {
        int Removed = 0;
        Iterator<Cell> Each = Cells.values().iterator();
        while (Each.hasNext()) {
            if (Each.next().heat(Now) < PruneBelow) {
                Each.remove();
                Removed++;
            }
        }
        return Removed;
    }

    private void trim(long Now) {
        List<Cell> Order = new ArrayList<>(Cells.values());
        Order.sort(Comparator.comparingDouble(Found -> Found.heat(Now)));
        int Excess = Cells.size() - MaxCells * 9 / 10;
        for (int Index = 0; Index < Excess; Index++) {
            Cells.remove(Order.get(Index).Key);
        }
    }

    public double total(long Now) {
        double Sum = 0.0;
        for (Cell Found : Cells.values()) {
            Sum += Found.heat(Now);
        }
        return Sum;
    }

    public void noteStash(long Packed, long Now) {
        Stashes.remove(Packed);
        Stashes.put(Packed, Now);
        while (Stashes.size() > MaxStashes) {
            Iterator<Long> First = Stashes.keySet().iterator();
            First.next();
            First.remove();
        }
    }

    public void forgetStash(long Packed) {
        Stashes.remove(Packed);
    }

    public Map<Long, Long> stashes() {
        return Stashes;
    }

    public BaseEstimate estimate(long Now) {
        if (Cells.isEmpty()) {
            return null;
        }
        Map<Long, Double> Heat = new HashMap<>();
        Map<Long, Double> Pull = new HashMap<>();
        double Total = 0.0;
        for (Cell Found : Cells.values()) {
            double Value = Found.heat(Now);
            Heat.put(Found.Key, Value);
            Pull.put(Found.Key, Value + RecentWeight * Found.recent(Now));
            Total += Value;
        }
        if (Total < MinTotal) {
            return null;
        }
        long Peak = 0L;
        double PeakScore = -1.0;
        for (Map.Entry<Long, Double> Entry : Pull.entrySet()) {
            int Cx = cellX(Entry.getKey());
            int Cz = cellZ(Entry.getKey());
            double Score = 0.0;
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    Double Near = Pull.get(key(Cx + Dx, Cz + Dz));
                    if (Near == null) {
                        continue;
                    }
                    double Share = Dx == 0 && Dz == 0 ? 1.0 : Dx == 0 || Dz == 0 ? 0.5 : 0.25;
                    Score += Near * Share;
                }
            }
            if (Score > PeakScore) {
                PeakScore = Score;
                Peak = Entry.getKey();
            }
        }
        double PeakHeat = Pull.get(Peak);
        double Floor = Math.min(PeakHeat, Math.max(FloorHeat, PeakHeat * FloorShare));
        int PeakX = cellX(Peak);
        int PeakZ = cellZ(Peak);
        Set<Long> Cluster = new HashSet<>();
        Deque<Long> Open = new ArrayDeque<>();
        Open.add(Peak);
        Cluster.add(Peak);
        while (!Open.isEmpty()) {
            long At = Open.poll();
            int Cx = cellX(At);
            int Cz = cellZ(At);
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    long Next = key(Cx + Dx, Cz + Dz);
                    if (Cluster.contains(Next) || Math.abs(Cx + Dx - PeakX) > ClusterReach || Math.abs(Cz + Dz - PeakZ) > ClusterReach) {
                        continue;
                    }
                    Double Value = Pull.get(Next);
                    if (Value != null && Value >= Floor) {
                        Cluster.add(Next);
                        Open.add(Next);
                    }
                }
            }
        }
        double Mass = 0.0;
        double X = 0.0;
        double Y = 0.0;
        double Z = 0.0;
        int Marks = 0;
        for (long Member : Cluster) {
            Cell Found = Cells.get(Member);
            double Value = Heat.get(Member);
            Mass += Value;
            X += Value * ((cellX(Member) << CellShift) + Found.meanX());
            Y += Value * Found.meanY();
            Z += Value * ((cellZ(Member) << CellShift) + Found.meanZ());
            Marks |= Found.Flags;
        }
        X /= Mass;
        Y /= Mass;
        Z /= Mass;
        double Square = 0.0;
        for (long Member : Cluster) {
            Cell Found = Cells.get(Member);
            double Value = Heat.get(Member);
            double Dx = (cellX(Member) << CellShift) + Found.meanX() - X;
            double Dz = (cellZ(Member) << CellShift) + Found.meanZ() - Z;
            Square += Value * (Found.spread() + Dx * Dx + Dz * Dz);
        }
        double Spread = Math.sqrt(Square / Mass);
        double Footprint = Math.max(MinFootprint, Spread * FootprintScale);
        double Share = Mass / Total;
        double Confidence = (1.0 - Math.exp(-Mass / Saturation)) * Math.sqrt(Share);
        return new BaseEstimate(X, Y, Z, Footprint, Confidence, Share, Mass, Total, Cluster.size(), Marks);
    }
}
