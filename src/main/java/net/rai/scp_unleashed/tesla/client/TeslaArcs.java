package net.rai.scp_unleashed.tesla.client;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.tesla.TeslaGeometry;
import net.rai.scp_unleashed.tesla.TeslaPhase;
import net.rai.scp_unleashed.tesla.TeslaSurface;

import java.util.ArrayList;
import java.util.List;

public final class TeslaArcs {
    private static final double MaxSwing = 5.0;
    private static final TeslaSurface[] Posts = {TeslaSurface.LeftPost, TeslaSurface.RightPost};

    private TeslaArcs() {
    }

    public record Bolt(List<Vec3> Points, boolean Branch, TeslaSurface Surface) {
    }

    public static float generate(RandomSource Random, TeslaPhase Phase, float Progress, List<Bolt> Bolts) {
        int Rows = TeslaGeometry.PostRows.length;
        if (Phase == TeslaPhase.Discharge) {
            for (int Step = 0; Step < 5; Step++) {
                int Row = Random.nextInt(Rows);
                int Other = Math.max(0, Math.min(Rows - 1, Row + Random.nextInt(3) - 1));
                boolean Flip = Step % 2 == 1;
                Vec3 Start = postPoint(Random, Flip ? TeslaSurface.RightPost : TeslaSurface.LeftPost, Row);
                Vec3 End = postPoint(Random, Flip ? TeslaSurface.LeftPost : TeslaSurface.RightPost, Other);
                family(Random, Start, End, 5, 0.22, 2, TeslaSurface.Opening, Bolts);
            }
            for (int Step = 0; Step < 3; Step++) {
                Vec3 Floor = new Vec3(TeslaGeometry.PostWidth + 2.0 + Random.nextDouble() * (TeslaGeometry.PixelsWide - 2.0 * TeslaGeometry.PostWidth - 4.0), 0.0, 1.0 + Random.nextDouble() * (TeslaGeometry.PixelsDeep - 2.0));
                family(Random, underPoint(Random), Floor, 5, 0.25, 1, TeslaSurface.Opening, Bolts);
            }
            for (int Step = 0; Step < 2; Step++) {
                TeslaSurface Post = Posts[Random.nextInt(Posts.length)];
                family(Random, postPoint(Random, Post, Rows - 1), underPoint(Random), 4, 0.3, 1, TeslaSurface.Opening, Bolts);
            }
            for (TeslaSurface Post : Posts) {
                int From = Random.nextInt(Rows);
                int To = (From + 1 + Random.nextInt(Rows - 1)) % Rows;
                family(Random, postPoint(Random, Post, From), postPoint(Random, Post, To), 4, 0.25, 1, Post, Bolts);
            }
            return 1.0f;
        }
        if (Phase == TeslaPhase.WindUp) {
            int Count = 1 + (int) (Progress * 5.0f);
            for (int Step = 0; Step < Count; Step++) {
                if (Step % 3 == 2) {
                    double Rail = TeslaGeometry.UnderRails[Random.nextInt(TeslaGeometry.UnderRails.length)];
                    int Pair = Random.nextInt(TeslaGeometry.UnderSegments.length - 1);
                    Vec3 Start = new Vec3(inside(Random, TeslaGeometry.UnderSegments[Pair]), TeslaGeometry.LintelBottom, Rail);
                    Vec3 End = new Vec3(inside(Random, TeslaGeometry.UnderSegments[Pair + 1]), TeslaGeometry.LintelBottom, Rail);
                    family(Random, Start, End, 4, 0.5, 1, TeslaSurface.Underside, Bolts);
                    continue;
                }
                TeslaSurface Post = Posts[Step % 2];
                double Height = TeslaGeometry.PostRows[Random.nextInt(Rows)];
                Vec3 Start = new Vec3(Post.low(0), Height, inside(Random, TeslaGeometry.PostSegments[0]));
                Vec3 End = new Vec3(Post.low(0), Height, inside(Random, TeslaGeometry.PostSegments[1]));
                family(Random, Start, End, 4, 0.5, 1, Post, Bolts);
            }
            if (Progress > 0.45f) {
                TeslaSurface Post = Posts[Random.nextInt(Posts.length)];
                Vec3 Start = postPoint(Random, Post, Random.nextInt(Rows));
                Vec3 End = Start.add(Post.normal().scale(5.0 + Random.nextDouble() * 6.0)).add(0.0, Random.nextDouble() * 4.0 - 2.0, Random.nextDouble() * 4.0 - 2.0);
                family(Random, Start, End, 3, 0.45, 0, TeslaSurface.Opening, Bolts);
            }
            if (Progress > 0.75f) {
                int Row = Random.nextInt(Rows);
                family(Random, postPoint(Random, TeslaSurface.LeftPost, Row), postPoint(Random, TeslaSurface.RightPost, Row), 4, 0.25, 0, TeslaSurface.Opening, Bolts);
            }
            return 0.45f + 0.4f * Progress;
        }
        if (Phase == TeslaPhase.Charged && Random.nextFloat() < 0.4f) {
            TeslaSurface Post = Posts[Random.nextInt(Posts.length)];
            double Height = TeslaGeometry.PostRows[Random.nextInt(Rows)] + (Random.nextDouble() - 0.5);
            double Middle = (TeslaGeometry.PostSegments[0][1] + TeslaGeometry.PostSegments[1][0]) / 2.0;
            family(Random, new Vec3(Post.low(0), Height, Middle - 1.5), new Vec3(Post.low(0), Height, Middle + 1.5), 3, 0.6, 0, Post, Bolts);
            return 0.6f;
        }
        return 0.0f;
    }

    private static double inside(RandomSource Random, double[] Segment) {
        return Segment[0] + 1.0 + Random.nextDouble() * (Segment[1] - Segment[0] - 2.0);
    }

    public static Vec3 postPoint(RandomSource Random, TeslaSurface Post, int Row) {
        double[] Segment = TeslaGeometry.PostSegments[Random.nextInt(TeslaGeometry.PostSegments.length)];
        return new Vec3(Post.low(0), TeslaGeometry.PostRows[Row], Segment[0] + 0.5 + Random.nextDouble() * (Segment[1] - Segment[0] - 1.0));
    }

    public static Vec3 underPoint(RandomSource Random) {
        double[] Segment = TeslaGeometry.UnderSegments[Random.nextInt(TeslaGeometry.UnderSegments.length)];
        double Rail = TeslaGeometry.UnderRails[Random.nextInt(TeslaGeometry.UnderRails.length)];
        return new Vec3(Segment[0] + 0.5 + Random.nextDouble() * (Segment[1] - Segment[0] - 1.0), TeslaGeometry.LintelBottom, Rail);
    }

    private static void family(RandomSource Random, Vec3 Start, Vec3 End, int Levels, double Roughness, int BranchCount, TeslaSurface Surface, List<Bolt> Bolts) {
        List<Vec3> Path = path(Random, Start, End, Levels, Roughness, Surface);
        Bolts.add(new Bolt(Path, false, Surface));
        double Span = Start.distanceTo(End);
        for (int Branch = 0; Branch < BranchCount && Path.size() > 6; Branch++) {
            int Index = 2 + Random.nextInt(Path.size() - 5);
            Vec3 Origin = Path.get(Index);
            Vec3 Ahead = Path.get(Math.min(Path.size() - 1, Index + 4)).subtract(Origin);
            Vec3 Drift = flatten(new Vec3(Random.nextDouble() * 2.0 - 1.0, Random.nextDouble() * 2.0 - 1.0, Random.nextDouble() * 2.0 - 1.0), Surface).scale(Ahead.length() * 1.4);
            Vec3 Heading = Ahead.add(Drift);
            if (Heading.lengthSqr() < 1.0E-6) {
                continue;
            }
            Vec3 Tip = Origin.add(Heading.normalize().scale(Span * (0.12 + Random.nextDouble() * 0.18)));
            Bolts.add(new Bolt(path(Random, Origin, Tip, Math.max(1, Levels - 2), 0.4, Surface), true, Surface));
        }
    }

    private static Vec3 flatten(Vec3 Pick, TeslaSurface Surface) {
        return Surface.isVolume() ? Pick : Pick.subtract(Surface.normal().scale(Pick.dot(Surface.normal())));
    }

    private static List<Vec3> path(RandomSource Random, Vec3 Start, Vec3 End, int Levels, double Roughness, TeslaSurface Surface) {
        List<Vec3> Points = new ArrayList<>();
        Points.add(Surface.clamp(Start, TeslaGeometry.ArcInset));
        Points.add(Surface.clamp(End, TeslaGeometry.ArcInset));
        double Amount = Math.min(MaxSwing, Start.distanceTo(End) * Roughness);
        for (int Step = 0; Step < Levels; Step++) {
            List<Vec3> Next = new ArrayList<>(Points.size() * 2);
            Next.add(Points.getFirst());
            for (int Index = 0; Index + 1 < Points.size(); Index++) {
                Vec3 A = Points.get(Index);
                Vec3 B = Points.get(Index + 1);
                Vec3 Along = B.subtract(A);
                Vec3 Pick = flatten(new Vec3(Random.nextDouble() * 2.0 - 1.0, Random.nextDouble() * 2.0 - 1.0, Random.nextDouble() * 2.0 - 1.0), Surface);
                Vec3 Across = Pick.subtract(Along.scale(Pick.dot(Along) / Math.max(Along.lengthSqr(), 1.0E-9)));
                double Length = Across.length();
                Vec3 Middle = A.add(B).scale(0.5);
                if (Length > 1.0E-9) {
                    Middle = Middle.add(Across.scale(Amount * (Random.nextDouble() * 2.0 - 1.0) / Length));
                }
                Next.add(Surface.fold(Middle, TeslaGeometry.ArcInset));
                Next.add(B);
            }
            Points = Next;
            Amount *= 0.5;
        }
        return Points;
    }
}
