package net.rai.scp_unleashed.cameras.client;

public final class CameraClientWindow {
    private static volatile boolean Open;
    private static volatile int CentreX;
    private static volatile int CentreZ;

    private CameraClientWindow() {
    }

    public static boolean isOpen() {
        return Open;
    }

    public static boolean covers(int X, int Z, int Radius) {
        return Open && Math.abs(X - CentreX) <= Radius && Math.abs(Z - CentreZ) <= Radius;
    }

    public static void open(int X, int Z) {
        CentreX = X;
        CentreZ = Z;
        Open = true;
    }

    public static void close() {
        Open = false;
    }
}
