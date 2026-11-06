package net.rai.scp_unleashed.scp001day.sun;

public record SunVector(double X, double Y, double Z) {
    public double horizontal() {
        return Math.sqrt(X * X + Z * Z);
    }
}
