package net.rai.scp_unleashed.scp5001.world.plan;

public final class Scp5001Hull {
    public static final double RadiusX = 75.5;
    public static final double RadiusZ = 55.5;
    public static final int Top = 55;
    public static final int Thickness = 2;
    public static final double Round = 12.0;

    private Scp5001Hull() {
    }

    public static double rim(int Dx, int Dz) {
        double Distance = Math.sqrt((double) Dx * Dx + (double) Dz * Dz);
        if (Distance < 1.0e-6) {
            return Math.min(RadiusX, RadiusZ);
        }
        double Cos = Dx / Distance;
        double Sin = Dz / Distance;
        double Reach = 1.0 / Math.sqrt((Cos / RadiusX) * (Cos / RadiusX) + (Sin / RadiusZ) * (Sin / RadiusZ));
        return Reach - Distance;
    }

    public static int band(int Dx, int Dz) {
        return (int) Math.floor(rim(Dx, Dz));
    }

    private static boolean within(double Rim, int Y, double Inset) {
        if (Rim < Inset || Y < Inset) {
            return false;
        }
        double Corner = Top - Round;
        if (Y <= Corner) {
            return true;
        }
        if (Rim >= Round) {
            return Y <= Top - Inset;
        }
        double Across = Round - Rim;
        double Up = Y - Corner;
        double Radius = Round - Inset;
        return Across * Across + Up * Up <= Radius * Radius;
    }

    public static boolean hull(int Dx, int Y, int Dz) {
        return within(rim(Dx, Dz), Y, 0.0);
    }

    public static boolean interior(int Dx, int Y, int Dz) {
        return within(rim(Dx, Dz), Y, Thickness);
    }

    public static double angle(int Dx, int Dz) {
        return Math.atan2(Dz / RadiusZ, Dx / RadiusX);
    }
}
