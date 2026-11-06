package net.rai.scp_unleashed.raid.base;

public record BaseEstimate(double X, double Y, double Z, double Footprint, double Confidence, double Share, double Mass, double Total, int Cells, int Flags) {
    public static final double MinError = 6.0;
    public static final double MaxError = 80.0;
    public static final double IntelStep = 0.8;
    public static final int MaxIntel = 5;
    public static final double LargeFootprint = 36.0;

    public double error(int Intel) {
        double Doubt = Math.pow(Math.max(0.0, 1.0 - Confidence), 1.5);
        double Raw = MinError + (MaxError - MinError) * Doubt;
        return Math.max(MinError, Raw * Math.pow(IntelStep, Math.min(MaxIntel, Math.max(0, Intel))));
    }

    public double[] aim(double First, double Second, int Intel) {
        double Reach = error(Intel) * Math.sqrt(Math.max(0.0, Math.min(1.0, First)));
        double Angle = Second * Math.PI * 2.0;
        return new double[]{X + Math.cos(Angle) * Reach, Z + Math.sin(Angle) * Reach};
    }

    public boolean large() {
        return Footprint >= LargeFootprint;
    }

    public boolean has(int Mark) {
        return (Flags & Mark) != 0;
    }

    public double horizontalGap(double PointX, double PointZ) {
        double Dx = PointX - X;
        double Dz = PointZ - Z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }
}
