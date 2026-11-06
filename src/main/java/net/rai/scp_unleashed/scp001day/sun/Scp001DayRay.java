package net.rai.scp_unleashed.scp001day.sun;

public final class Scp001DayRay {
    public static final int Clear = 0xFF00;
    public static final int Solid = 0xFFFF;
    public static final double NearReach = 48.0;
    public static final double Cutoff = 1.0 / 256.0;
    public static final int StepLimit = 4096;

    private Scp001DayRay() {
    }

    @FunctionalInterface
    public interface Field {
        int cell(int X, int Y, int Z);
    }

    public static int cell(int Mask, int Filter) {
        return (Mask & 255) | ((Filter & 255) << 8);
    }

    public static int mask(int Cell) {
        return Cell & 255;
    }

    public static int filter(int Cell) {
        return (Cell >> 8) & 255;
    }

    public static boolean needsFar(double Y, SunVector Sun, double Reach, int SkyY) {
        return Y + Sun.Y() * Reach < SkyY;
    }

    public static double transmit(Field Cells, Scp001DayFarField Far, double X, double Y, double Z, SunVector Sun, int SkyY) {
        if (Sun.Y() <= 0.0) {
            return 0.0;
        }
        double Light = near(Cells, X, Y, Z, Sun, NearReach, SkyY);
        if (Light <= 0.0 || !needsFar(Y, Sun, NearReach, SkyY)) {
            return Light;
        }
        return Light * Far.transmit(X + Sun.X() * NearReach, Y + Sun.Y() * NearReach, Z + Sun.Z() * NearReach, Sun);
    }

    public static double near(Field Cells, double X, double Y, double Z, SunVector Sun, double Reach, int SkyY) {
        double Gx = X * 2.0;
        double Gy = Y * 2.0;
        double Gz = Z * 2.0;
        int Cx = (int) Math.floor(Gx);
        int Cy = (int) Math.floor(Gy);
        int Cz = (int) Math.floor(Gz);
        int StepX = Sun.X() > 0.0 ? 1 : (Sun.X() < 0.0 ? -1 : 0);
        int StepY = Sun.Y() > 0.0 ? 1 : (Sun.Y() < 0.0 ? -1 : 0);
        int StepZ = Sun.Z() > 0.0 ? 1 : (Sun.Z() < 0.0 ? -1 : 0);
        double DeltaX = StepX == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(Sun.X());
        double DeltaY = StepY == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(Sun.Y());
        double DeltaZ = StepZ == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(Sun.Z());
        double NextX = StepX > 0 ? (Cx + 1.0 - Gx) * DeltaX : (StepX < 0 ? (Gx - Cx) * DeltaX : Double.POSITIVE_INFINITY);
        double NextY = StepY > 0 ? (Cy + 1.0 - Gy) * DeltaY : (StepY < 0 ? (Gy - Cy) * DeltaY : Double.POSITIVE_INFINITY);
        double NextZ = StepZ > 0 ? (Cz + 1.0 - Gz) * DeltaZ : (StepZ < 0 ? (Gz - Cz) * DeltaZ : Double.POSITIVE_INFINITY);
        double Limit = Reach * 2.0;
        int SkyCell = SkyY * 2;
        double Light = 1.0;
        int LastX = Integer.MIN_VALUE;
        int LastY = Integer.MIN_VALUE;
        int LastZ = Integer.MIN_VALUE;
        int Current = Clear;
        for (int Step = 0; Step < StepLimit; Step++) {
            if (Cy >= SkyCell) {
                return Light;
            }
            int Bx = Cx >> 1;
            int By = Cy >> 1;
            int Bz = Cz >> 1;
            if (Bx != LastX || By != LastY || Bz != LastZ) {
                Current = Cells.cell(Bx, By, Bz);
                LastX = Bx;
                LastY = By;
                LastZ = Bz;
            }
            int Bit = (Cx & 1) | ((Cy & 1) << 1) | ((Cz & 1) << 2);
            if (((Current >> Bit) & 1) != 0) {
                return 0.0;
            }
            int Pass = (Current >> 8) & 255;
            if (Pass != 255) {
                Light *= Pass / 255.0;
                if (Light < Cutoff) {
                    return 0.0;
                }
            }
            if (NextX < NextY && NextX < NextZ) {
                if (NextX > Limit) {
                    return Light;
                }
                Cx += StepX;
                NextX += DeltaX;
            } else if (NextY < NextZ) {
                if (NextY > Limit) {
                    return Light;
                }
                Cy += StepY;
                NextY += DeltaY;
            } else {
                if (NextZ > Limit) {
                    return Light;
                }
                Cz += StepZ;
                NextZ += DeltaZ;
            }
        }
        return Light;
    }
}
