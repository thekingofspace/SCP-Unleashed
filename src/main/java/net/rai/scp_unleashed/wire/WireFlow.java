package net.rai.scp_unleashed.wire;

import java.util.Arrays;

public final class WireFlow {
    public static final double MinConductance = 1.0;
    public static final double MaxConductance = 1.0E6;

    private WireFlow() {
    }

    public static double conductance(double Capacity) {
        return Math.max(MinConductance, Math.min(MaxConductance, Capacity));
    }

    public static double[] solve(int NodeCount, int[] From, int[] To, double[] Injection) {
        double[] Unit = new double[From.length];
        Arrays.fill(Unit, 1.0);
        return solve(NodeCount, From, To, Unit, Injection);
    }

    public static double[] solve(int NodeCount, int[] From, int[] To, double[] Conductance, double[] Injection) {
        double[] Flows = new double[From.length];
        if (NodeCount == 0 || From.length == 0) {
            return Flows;
        }
        double[] Supply = Injection.clone();
        double Mean = 0.0;
        for (double Value : Supply) {
            Mean += Value;
        }
        Mean /= NodeCount;
        for (int Node = 0; Node < NodeCount; Node++) {
            Supply[Node] -= Mean;
        }
        double[] Diagonal = new double[NodeCount];
        for (int Edge = 0; Edge < From.length; Edge++) {
            Diagonal[From[Edge]] += Conductance[Edge];
            Diagonal[To[Edge]] += Conductance[Edge];
        }
        double[] Potential = new double[NodeCount];
        double[] Residual = Supply.clone();
        double[] Scaled = new double[NodeCount];
        precondition(Residual, Diagonal, Scaled);
        double[] Direction = Scaled.clone();
        double[] Applied = new double[NodeCount];
        double ResidualSquared = dot(Residual, Residual);
        double Alignment = dot(Residual, Scaled);
        double Limit = Math.max(ResidualSquared * 1.0E-16, 1.0E-20);
        int MaxIterations = Math.max(NodeCount * 2, 16);
        for (int Iteration = 0; Iteration < MaxIterations && ResidualSquared > Limit; Iteration++) {
            applyLaplacian(From, To, Conductance, Direction, Applied);
            double Curvature = dot(Direction, Applied);
            if (Curvature <= 1.0E-18 || Alignment <= 0.0) {
                break;
            }
            double Step = Alignment / Curvature;
            for (int Node = 0; Node < NodeCount; Node++) {
                Potential[Node] += Step * Direction[Node];
                Residual[Node] -= Step * Applied[Node];
            }
            ResidualSquared = dot(Residual, Residual);
            precondition(Residual, Diagonal, Scaled);
            double NextAlignment = dot(Residual, Scaled);
            double Blend = NextAlignment / Alignment;
            for (int Node = 0; Node < NodeCount; Node++) {
                Direction[Node] = Scaled[Node] + Blend * Direction[Node];
            }
            Alignment = NextAlignment;
        }
        for (int Edge = 0; Edge < From.length; Edge++) {
            Flows[Edge] = Conductance[Edge] * (Potential[From[Edge]] - Potential[To[Edge]]);
        }
        return Flows;
    }

    private static void precondition(double[] Residual, double[] Diagonal, double[] Result) {
        for (int Node = 0; Node < Residual.length; Node++) {
            Result[Node] = Diagonal[Node] > 0.0 ? Residual[Node] / Diagonal[Node] : 0.0;
        }
    }

    private static void applyLaplacian(int[] From, int[] To, double[] Conductance, double[] Values, double[] Result) {
        Arrays.fill(Result, 0.0);
        for (int Edge = 0; Edge < From.length; Edge++) {
            double Difference = Conductance[Edge] * (Values[From[Edge]] - Values[To[Edge]]);
            Result[From[Edge]] += Difference;
            Result[To[Edge]] -= Difference;
        }
    }

    private static double dot(double[] First, double[] Second) {
        double Sum = 0.0;
        for (int Index = 0; Index < First.length; Index++) {
            Sum += First[Index] * Second[Index];
        }
        return Sum;
    }
}
