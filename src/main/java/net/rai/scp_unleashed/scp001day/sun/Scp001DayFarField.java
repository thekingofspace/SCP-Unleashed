package net.rai.scp_unleashed.scp001day.sun;

@FunctionalInterface
public interface Scp001DayFarField {
    Scp001DayFarField Open = (X, Y, Z, Sun) -> 1.0;

    double transmit(double X, double Y, double Z, SunVector Sun);
}
