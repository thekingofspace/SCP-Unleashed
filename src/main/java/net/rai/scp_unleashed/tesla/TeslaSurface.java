package net.rai.scp_unleashed.tesla;

import net.minecraft.world.phys.Vec3;

public enum TeslaSurface {
    LeftPost(new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0),
            new double[]{TeslaGeometry.PostWidth, 0.0, 0.0}, new double[]{TeslaGeometry.PostWidth, TeslaGeometry.LintelBottom, TeslaGeometry.PixelsDeep}),
    RightPost(new Vec3(-1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0),
            new double[]{TeslaGeometry.PixelsWide - TeslaGeometry.PostWidth, 0.0, 0.0}, new double[]{TeslaGeometry.PixelsWide - TeslaGeometry.PostWidth, TeslaGeometry.LintelBottom, TeslaGeometry.PixelsDeep}),
    Underside(new Vec3(0.0, -1.0, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0),
            new double[]{TeslaGeometry.PostWidth, TeslaGeometry.LintelBottom, 0.0}, new double[]{TeslaGeometry.PixelsWide - TeslaGeometry.PostWidth, TeslaGeometry.LintelBottom, TeslaGeometry.PixelsDeep}),
    LintelFront(new Vec3(0.0, 0.0, -1.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0),
            new double[]{0.0, TeslaGeometry.LintelBottom, 0.0}, new double[]{TeslaGeometry.PixelsWide, TeslaGeometry.PixelsHigh, 0.0}),
    LintelBack(new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0),
            new double[]{0.0, TeslaGeometry.LintelBottom, TeslaGeometry.PixelsDeep}, new double[]{TeslaGeometry.PixelsWide, TeslaGeometry.PixelsHigh, TeslaGeometry.PixelsDeep}),
    Opening(Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0),
            new double[]{TeslaGeometry.PostWidth, 0.0, 0.0}, new double[]{TeslaGeometry.PixelsWide - TeslaGeometry.PostWidth, TeslaGeometry.LintelBottom, TeslaGeometry.PixelsDeep});

    private final Vec3 Normal;
    private final Vec3 Across;
    private final Vec3 Up;
    private final double[] Low;
    private final double[] High;

    TeslaSurface(Vec3 Normal, Vec3 Across, Vec3 Up, double[] Low, double[] High) {
        this.Normal = Normal;
        this.Across = Across;
        this.Up = Up;
        this.Low = Low;
        this.High = High;
    }

    public Vec3 normal() {
        return Normal;
    }

    public Vec3 across() {
        return Across;
    }

    public Vec3 up() {
        return Up;
    }

    public double low(int Dimension) {
        return Low[Dimension];
    }

    public double high(int Dimension) {
        return High[Dimension];
    }

    public boolean isVolume() {
        return this == Opening;
    }

    public boolean contains(Vec3 Point, double Tolerance) {
        double[] Values = {Point.x, Point.y, Point.z};
        for (int Dimension = 0; Dimension < 3; Dimension++) {
            if (Values[Dimension] < Low[Dimension] - Tolerance || Values[Dimension] > High[Dimension] + Tolerance) {
                return false;
            }
        }
        return true;
    }

    public Vec3 clamp(Vec3 Point, double Inset) {
        double[] Values = {Point.x, Point.y, Point.z};
        for (int Dimension = 0; Dimension < 3; Dimension++) {
            double Margin = High[Dimension] - Low[Dimension] > 2.0 * Inset ? Inset : 0.0;
            Values[Dimension] = Math.max(Low[Dimension] + Margin, Math.min(High[Dimension] - Margin, Values[Dimension]));
        }
        return new Vec3(Values[0], Values[1], Values[2]);
    }

    public Vec3 fold(Vec3 Point, double Inset) {
        double[] Values = {Point.x, Point.y, Point.z};
        for (int Dimension = 0; Dimension < 3; Dimension++) {
            double Margin = High[Dimension] - Low[Dimension] > 2.0 * Inset ? Inset : 0.0;
            double Bottom = Low[Dimension] + Margin;
            double Top = High[Dimension] - Margin;
            double Value = Values[Dimension];
            double Folded = Value < Bottom ? Bottom + (Bottom - Value) : Value > Top ? Top - (Value - Top) : Value;
            Values[Dimension] = Math.max(Bottom, Math.min(Top, Folded));
        }
        return new Vec3(Values[0], Values[1], Values[2]);
    }

    public boolean faces(Vec3 Point, Vec3 Eye) {
        return isVolume() || Eye.subtract(Point).dot(Normal) > 0.0;
    }
}
