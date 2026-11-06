package net.rai.scp_unleashed.scp001day.world;

import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayFarField;
import net.rai.scp_unleashed.scp001day.sun.SunVector;

public final class Scp001DayFar implements Scp001DayFarField {
    public static final Scp001DayFar City = new Scp001DayFar();
    public static final double Reach = 1200.0;

    private Scp001DayFar() {
    }

    @Override
    public double transmit(double X, double Y, double Z, SunVector Sun) {
        return Scp001DayPlan.farTransmit(X, Y, Z, Sun.X(), Sun.Y(), Sun.Z(), Reach);
    }
}
