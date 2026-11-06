package net.rai.scp_unleashed.motion;

import java.util.Arrays;

public final class PaceMeter {
    public static final int Span = 3;
    private final double[] Recent = new double[Span];
    private int Cursor;
    private double Mean;

    public double push(double Moved) {
        Recent[Cursor] = Moved;
        Cursor = (Cursor + 1) % Span;
        double Sum = 0.0;
        for (double Value : Recent) {
            Sum += Value;
        }
        Mean = Sum / Span;
        return Mean;
    }

    public double mean() {
        return Mean;
    }

    public void reset() {
        Arrays.fill(Recent, 0.0);
        Cursor = 0;
        Mean = 0.0;
    }
}
