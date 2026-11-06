package net.rai.scp_unleashed.scp178;

public final class Scp178Phantom {
    private final int Id;
    boolean Hostile;
    double Distance;
    float Health;
    int Age;
    int Patience;
    int Cooldown;
    int Windup = -1;
    double Drift;
    int DriftTimer;
    double Sent = -1.0;
    int SinceSent;
    double Bearing;
    boolean Anchored;
    double PointX;
    double PointZ;
    int Behind;

    Scp178Phantom(int Id, double Distance, float Health, int Patience) {
        this.Id = Id;
        this.Distance = Distance;
        this.Drift = Distance;
        this.Health = Health;
        this.Patience = Patience;
    }

    public int id() {
        return Id;
    }

    public boolean hostile() {
        return Hostile;
    }

    public double distance() {
        return Distance;
    }

    public float health() {
        return Health;
    }

    public boolean windingUp() {
        return Windup >= 0;
    }

    void setDistance(double Value) {
        Distance = Value;
        Anchored = false;
    }

    public double bearing() {
        return Bearing;
    }

    void setPatience(int Ticks) {
        Patience = Ticks;
    }
}
