package net.rai.scp_unleashed.rifttech.client;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;

import java.util.ArrayList;
import java.util.List;

public final class GoldenFracture {
    public static final float HeartScale = 1.7f;
    public static final double HeartReach = ApocalypticRiftEntity.Height * 0.5 * HeartScale;
    public static final double HeartRim = 0.55;
    public static final double TrunkLow = -3.4;
    public static final double TrunkHigh = 7.6;
    public static final double HeartTop = 2.2;
    public static final double HeartLow = -2.0;
    public static final float TearHalf = 0.62f;
    public static final float CrackHalf = 0.07f;
    public static final float BranchHalf = 0.13f;
    public static final float ChildHalf = 0.08f;
    public static final float FlashHalf = 0.07f;
    public static final int MaxSegments = 900;

    private GoldenFracture() {
    }

    public record Branch(List<Vec3> Points, float Half, float Heat, boolean Torn) {
    }

    public record Fracture(List<Branch> Branches, float[] Tear, int TearStart) {
        public Branch trunk() {
            return Branches.getFirst();
        }
    }

    private static long mix(int Seed, int Index, long Epoch) {
        return Seed * 341873128712L + Index * 132897987541L + Epoch * 42317861L + 1442695040888963407L;
    }

    private static double unit(int Seed, int Index, int Salt) {
        RandomSource Random = RandomSource.create(mix(Seed, Index * 31 + Salt, 7L));
        return Random.nextDouble();
    }

    private static List<Vec3> shape(int Seed, int Index, long Epoch, int Levels, double Roughness) {
        return Ribbons.path(RandomSource.create(mix(Seed, Index, Epoch)), Vec3.ZERO, new Vec3(0.0, 0.0, 1.0), Levels, Roughness);
    }

    private static double smooth(double Value) {
        return Value * Value * (3.0 - 2.0 * Value);
    }

    private static double jag(int Seed, int Index, long Epoch) {
        return RandomSource.create(mix(Seed, Index + 500, Epoch)).nextDouble();
    }

    private static List<Vec3> route(int Seed, int Index, double Time, double Period, int Levels, double Roughness) {
        double Phase = Time / Period + unit(Seed, Index, 1);
        long Epoch = (long) Math.floor(Phase);
        double Blend = Phase - Epoch;
        Blend = smooth(Blend);
        List<Vec3> First = shape(Seed, Index, Epoch, Levels, Roughness);
        List<Vec3> Second = shape(Seed, Index, Epoch + 1, Levels, Roughness);
        List<Vec3> Mixed = new ArrayList<>(First.size());
        for (int Point = 0; Point < First.size(); Point++) {
            Mixed.add(First.get(Point).lerp(Second.get(Point), Blend));
        }
        return Mixed;
    }

    private static List<Vec3> place(List<Vec3> Unit, Vec3 Start, Vec3 End) {
        Vec3 Along = End.subtract(Start);
        double Length = Along.length();
        if (Length < 1.0E-6) {
            return List.of(Start, End);
        }
        Vec3 Forward = Along.scale(1.0 / Length);
        Vec3 Helper = Math.abs(Forward.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 Side = Forward.cross(Helper).normalize();
        Vec3 Up = Side.cross(Forward).normalize();
        List<Vec3> Placed = new ArrayList<>(Unit.size());
        for (Vec3 Point : Unit) {
            Placed.add(Start.add(Forward.scale(Point.z * Length)).add(Side.scale(Point.x * Length)).add(Up.scale(Point.y * Length)));
        }
        return Placed;
    }

    private static Vec3 pick(List<Vec3> Path, double Along) {
        double Scaled = Mth.clamp(Along, 0.0, 1.0) * (Path.size() - 1);
        int Index = Math.min(Path.size() - 2, (int) Math.floor(Scaled));
        return Path.get(Index).lerp(Path.get(Index + 1), Scaled - Index);
    }

    private static Vec3 heading(double Azimuth, double Elevation) {
        return new Vec3(Math.cos(Elevation) * Math.cos(Azimuth), Math.sin(Elevation), Math.cos(Elevation) * Math.sin(Azimuth));
    }

    private static List<Vec3> trim(List<Vec3> Path, double Fraction) {
        if (Fraction >= 0.999) {
            return Path;
        }
        int Keep = Math.max(2, (int) Math.ceil((Path.size() - 1) * Math.max(0.0, Fraction)) + 1);
        List<Vec3> Cut = new ArrayList<>(Path.subList(0, Math.min(Keep, Path.size())));
        Cut.set(Cut.size() - 1, pick(Path, Fraction));
        return Cut;
    }

    public static int levelsFor(double Distance) {
        return Distance < 28.0 ? 5 : Distance < 64.0 ? 4 : 3;
    }

    private static float[] tear(int Seed, double Time, List<Vec3> Trunk, float Scale) {
        int Count = Trunk.size();
        float[] Halves = new float[Count];
        double Phase = Time / 60.0 + unit(Seed, 0, 30);
        long Epoch = (long) Math.floor(Phase);
        double Blend = smooth(Phase - Epoch);
        double Floor = HeartTop * Scale;
        double Top = Trunk.getLast().y;
        for (int Index = 0; Index < Count; Index++) {
            double Height = Trunk.get(Index).y;
            double Jag = Mth.lerp(Blend, jag(Seed, Index, Epoch), jag(Seed, Index, Epoch + 1));
            if (Height < Floor || Top <= Floor) {
                Halves[Index] = (float) (CrackHalf * Scale * (0.8 + 0.4 * Jag));
                continue;
            }
            double Swell = Math.pow(Math.sin(Math.PI * Mth.clamp((Height - Floor) / (Top - Floor), 0.0, 1.0)), 0.75);
            Halves[Index] = (float) (Scale * (CrackHalf + (TearHalf - CrackHalf) * Swell * (0.55 + 0.7 * Jag)));
        }
        return Halves;
    }

    private static int tearStart(List<Vec3> Trunk, float Scale) {
        double Floor = HeartTop * Scale;
        for (int Index = 0; Index < Trunk.size(); Index++) {
            if (Trunk.get(Index).y >= Floor) {
                return Index;
            }
        }
        return Trunk.size();
    }

    public static Fracture build(int Seed, double Time, int Levels, boolean Fine, float Scale, float Strain) {
        List<Branch> Branches = new ArrayList<>();
        int Segments = 0;
        double Breath = 1.0 + 0.05 * Math.sin(Time * 0.031 + unit(Seed, 0, 3) * 6.0);
        Vec3 TrunkStart = new Vec3(0.0, TrunkLow * Scale, 0.0);
        double Lean = (unit(Seed, 0, 5) - 0.5) * 1.6;
        Vec3 TrunkEnd = new Vec3(Lean * Scale, TrunkHigh * Breath * Scale, (unit(Seed, 0, 6) - 0.5) * 1.2 * Scale);
        List<Vec3> Trunk = place(route(Seed, 0, Time, 140.0, Levels, 0.12 + 0.05 * Strain), TrunkStart, TrunkEnd);
        Branches.add(new Branch(Trunk, CrackHalf * Scale, 1.0f, true));
        Segments += Trunk.size() - 1;
        double LowAlong = (HeartLow - TrunkLow) / (TrunkHigh - TrunkLow);
        double TopAlong = (HeartTop - TrunkLow) / (TrunkHigh - TrunkLow);
        int Count = 9 + (int) (unit(Seed, 0, 9) * 5.0);
        double Turn = unit(Seed, 0, 10) * Mth.TWO_PI;
        for (int Index = 1; Index <= Count && Segments < MaxSegments; Index++) {
            double Fork = Mth.lerp(unit(Seed, Index, 11), LowAlong, TopAlong);
            double Azimuth = Turn + (Index - 1 + 0.6 * unit(Seed, Index, 12)) / Count * Mth.TWO_PI;
            double Elevation = Math.toRadians(-45.0 + 90.0 * unit(Seed, Index, 13));
            double Reach = (2.8 + 2.7 * unit(Seed, Index, 14)) * Scale;
            double Speed = 0.012 + 0.03 * unit(Seed, Index, 15);
            double Growth = Mth.clamp(0.62 + 0.42 * Math.sin(Time * Speed + unit(Seed, Index, 16) * 40.0), 0.18, 1.0);
            Vec3 Root = pick(Trunk, Fork);
            double Across = Root.y / (HeartReach * Scale);
            Root = Root.add(heading(Azimuth, 0.0).scale(HeartRim * Scale * Math.max(0.0, 1.0 - Across * Across)));
            Vec3 Tip = Root.add(heading(Azimuth, Elevation).scale(Reach));
            List<Vec3> Full = place(route(Seed, Index, Time, 80.0 + 90.0 * unit(Seed, Index, 17), Math.max(2, Levels - 1), 0.24 + 0.06 * Strain), Root, Tip);
            List<Vec3> Shown = trim(Full, Growth);
            float Heat = (float) (0.78 + 0.22 * Math.sin(Time * (0.5 + unit(Seed, Index, 18)) + Index * 2.1) + 0.3 * Strain);
            Branches.add(new Branch(Shown, BranchHalf * Scale, Heat, false));
            Segments += Shown.size() - 1;
            int Children = 1 + (unit(Seed, Index, 19) > 0.55 ? 1 : 0);
            for (int Child = 0; Child < Children && Segments < MaxSegments; Child++) {
                int Key = Index * 16 + Child + 100;
                double Along = Math.min(Growth, 0.35 + 0.45 * unit(Seed, Key, 20));
                Vec3 Base = pick(Full, Along);
                double Swing = (unit(Seed, Key, 21) > 0.5 ? 1.0 : -1.0) * Math.toRadians(25.0 + 35.0 * unit(Seed, Key, 22));
                Vec3 Direction = heading(Azimuth + Swing, Elevation + Math.toRadians(-20.0 + 50.0 * unit(Seed, Key, 23)));
                double Length = Reach * Growth * (0.35 + 0.25 * unit(Seed, Key, 24));
                List<Vec3> Twig = place(route(Seed, Key, Time, 70.0 + 70.0 * unit(Seed, Key, 25), Math.max(2, Levels - 2), 0.28), Base, Base.add(Direction.scale(Length)));
                Branches.add(new Branch(Twig, ChildHalf * Scale, Heat * 0.9f, false));
                Segments += Twig.size() - 1;
            }
            if (!Fine) {
                continue;
            }
            int Key = Index * 16 + 200;
            double Cycle = 70.0 + 90.0 * unit(Seed, Key, 26);
            double Phase = Time / Cycle + unit(Seed, Key, 27);
            long Round = (long) Math.floor(Phase);
            double Window = (Phase - Round) * Cycle;
            double Open = Window < 3.0 ? Window / 3.0 : Window < 10.0 ? 1.0 : Window < 16.0 ? 1.0 - (Window - 10.0) / 6.0 : 0.0;
            if (Open <= 0.0) {
                continue;
            }
            RandomSource Random = RandomSource.create(mix(Seed, Key, Round));
            Vec3 Base = pick(Full, 0.2 + 0.6 * Random.nextDouble() * Growth);
            Vec3 Direction = heading(Azimuth + (Random.nextDouble() - 0.5) * 2.4, Elevation + (Random.nextDouble() - 0.4) * 1.2);
            double Length = Reach * (0.25 + 0.25 * Random.nextDouble()) * Open;
            List<Vec3> Flash = place(shape(Seed, Key, Round, Math.max(2, Levels - 2), 0.32), Base, Base.add(Direction.scale(Length)));
            Branches.add(new Branch(Flash, FlashHalf * Scale, (float) (1.3 * Open), false));
            Segments += Flash.size() - 1;
        }
        return new Fracture(Branches, tear(Seed, Time, Trunk, Scale), tearStart(Trunk, Scale));
    }

    public static Vec3 strikeOrigin(List<Branch> Branches, Vec3 Target) {
        Vec3 Best = Vec3.ZERO;
        double Score = Double.MAX_VALUE;
        for (Branch Item : Branches) {
            Vec3 Tip = Item.Points().getLast();
            double Distance = Tip.distanceToSqr(Target);
            if (Distance < Score) {
                Score = Distance;
                Best = Tip;
            }
        }
        return Best;
    }
}
