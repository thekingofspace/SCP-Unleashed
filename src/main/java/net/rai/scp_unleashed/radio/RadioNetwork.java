package net.rai.scp_unleashed.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class RadioNetwork {
    public static final int MaxMasts = 4;
    public static final double Unreached = -1.0;

    private RadioNetwork() {
    }

    public record Ranges(int Direct, int CoverageBase, int CoveragePerMast, int LinkBase, int LinkPerMast) {
        public double coverage(int Masts) {
            return Masts <= 0 ? 0.0 : CoverageBase + CoveragePerMast * (double) Math.min(Masts, MaxMasts);
        }

        public double link(int Masts) {
            return Masts <= 0 ? 0.0 : LinkBase + LinkPerMast * (double) Math.min(Masts, MaxMasts);
        }
    }

    public record Node(BlockPos Pos, Vec3 Antenna, double Coverage, double Link) {
    }

    public static Vec3 antenna(BlockPos Pos, int Masts) {
        return new Vec3(Pos.getX() + 0.5, Pos.getY() + 0.5 + Math.max(0, Math.min(Masts, MaxMasts)), Pos.getZ() + 0.5);
    }

    public static Node node(Ranges Limits, BlockPos Pos, int Masts) {
        return new Node(Pos.immutable(), antenna(Pos, Masts), Limits.coverage(Masts), Limits.link(Masts));
    }

    public static List<Node> nodes(Ranges Limits, Collection<RadioTowerData.Tower> Towers) {
        List<Node> Made = new ArrayList<>();
        for (RadioTowerData.Tower Each : Towers) {
            if (Each.relays()) {
                Made.add(node(Limits, Each.Pos(), Each.Masts()));
            }
        }
        return Made;
    }

    public static double margin(double Distance, double Range) {
        if (Range <= 0.0 || Distance > Range) {
            return Unreached;
        }
        return 1.0 - Distance / Range;
    }

    public static double[] spread(List<Node> Nodes, double[] Start) {
        int Count = Nodes.size();
        double[] Best = Arrays.copyOf(Start, Count);
        boolean[] Done = new boolean[Count];
        for (int Round = 0; Round < Count; Round++) {
            int Pick = -1;
            for (int Index = 0; Index < Count; Index++) {
                if (!Done[Index] && Best[Index] >= 0.0 && (Pick < 0 || Best[Index] > Best[Pick])) {
                    Pick = Index;
                }
            }
            if (Pick < 0) {
                break;
            }
            Done[Pick] = true;
            Node From = Nodes.get(Pick);
            for (int Index = 0; Index < Count; Index++) {
                if (Done[Index]) {
                    continue;
                }
                Node To = Nodes.get(Index);
                double Hop = margin(From.Antenna().distanceTo(To.Antenna()), Math.min(From.Link(), To.Link()));
                double Through = Math.min(Best[Pick], Hop);
                if (Hop >= 0.0 && Through > Best[Index]) {
                    Best[Index] = Through;
                }
            }
        }
        return Best;
    }

    public static Reach from(Ranges Limits, List<Node> Nodes, Vec3 Origin) {
        double[] Start = new double[Nodes.size()];
        for (int Index = 0; Index < Start.length; Index++) {
            Node Each = Nodes.get(Index);
            Start[Index] = margin(Origin.distanceTo(Each.Antenna()), Each.Coverage());
        }
        return new Reach(Limits, Nodes, Origin, spread(Nodes, Start));
    }

    public static Reach fromTower(Ranges Limits, List<Node> Nodes, BlockPos Tower) {
        double[] Start = new double[Nodes.size()];
        Arrays.fill(Start, Unreached);
        for (int Index = 0; Index < Start.length; Index++) {
            if (Nodes.get(Index).Pos().equals(Tower)) {
                Start[Index] = 1.0;
            }
        }
        return new Reach(Limits, Nodes, null, spread(Nodes, Start));
    }

    public static double strength(Ranges Limits, List<Node> Nodes, Vec3 From, Vec3 To) {
        return from(Limits, Nodes, From).to(To);
    }

    public static final class Reach {
        private final Ranges Limits;
        private final List<Node> Nodes;
        private final Vec3 Origin;
        private final double[] Best;

        private Reach(Ranges Limits, List<Node> Nodes, Vec3 Origin, double[] Best) {
            this.Limits = Limits;
            this.Nodes = Nodes;
            this.Origin = Origin;
            this.Best = Best;
        }

        public double to(Vec3 Target) {
            double Result = Origin == null ? Unreached : margin(Origin.distanceTo(Target), Limits.Direct());
            for (int Index = 0; Index < Best.length; Index++) {
                if (Best[Index] < 0.0) {
                    continue;
                }
                Node Each = Nodes.get(Index);
                double Last = margin(Target.distanceTo(Each.Antenna()), Each.Coverage());
                if (Last >= 0.0) {
                    Result = Math.max(Result, Math.min(Best[Index], Last));
                }
            }
            return Result;
        }

        public boolean reaches(Vec3 Target) {
            return to(Target) >= 0.0;
        }

        public int towersReached() {
            int Count = 0;
            for (double Value : Best) {
                if (Value >= 0.0) {
                    Count++;
                }
            }
            return Count;
        }
    }
}
