package net.rai.scp_unleashed.scp001day.sun;

public final class Scp001DaySun {
    public static final long CycleTicks = 24000L;
    public static final long StepTicks = 5L;
    public static final long NoonTicks = 6000L;
    public static final double Latitude = 46.4;
    public static final double Declination = 20.0;
    public static final double LethalElevation = 2.0;
    public static final double FullElevation = 12.0;

    private Scp001DaySun() {
    }

    public static long epoch(long DayTime) {
        return Math.floorDiv(DayTime, StepTicks);
    }

    public static double phase(long DayTime) {
        return Math.floorMod(epoch(DayTime) * StepTicks, CycleTicks) / (double) CycleTicks;
    }

    public static double hourAngle(double Phase) {
        return Math.PI * 2.0 * (Phase - NoonTicks / (double) CycleTicks);
    }

    public static SunVector at(double Phase) {
        double Hour = hourAngle(Phase);
        double Lat = Math.toRadians(Latitude);
        double Dec = Math.toRadians(Declination);
        double East = -Math.cos(Dec) * Math.sin(Hour);
        double North = Math.cos(Lat) * Math.sin(Dec) - Math.sin(Lat) * Math.cos(Dec) * Math.cos(Hour);
        double Up = Math.sin(Lat) * Math.sin(Dec) + Math.cos(Lat) * Math.cos(Dec) * Math.cos(Hour);
        double Length = Math.sqrt(East * East + North * North + Up * Up);
        return new SunVector(East / Length, Up / Length, -North / Length);
    }

    public static SunVector direction(long DayTime) {
        return at(phase(DayTime));
    }

    public static double elevation(SunVector Sun) {
        return Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, Sun.Y()))));
    }

    public static boolean lethal(SunVector Sun) {
        return elevation(Sun) > LethalElevation;
    }

    public static double intensity(SunVector Sun) {
        double Ramp = (elevation(Sun) - LethalElevation) / (FullElevation - LethalElevation);
        return Ramp <= 0.0 ? 0.0 : Math.sqrt(Math.min(1.0, Ramp));
    }

    public static double daylightShare() {
        double Lat = Math.toRadians(Latitude);
        double Dec = Math.toRadians(Declination);
        double Edge = (Math.sin(Math.toRadians(LethalElevation)) - Math.sin(Lat) * Math.sin(Dec)) / (Math.cos(Lat) * Math.cos(Dec));
        return Math.acos(Math.max(-1.0, Math.min(1.0, Edge))) / Math.PI;
    }

    public static long dawnTicks() {
        return Math.floorMod(NoonTicks - Math.round(daylightShare() * CycleTicks / 2.0), CycleTicks);
    }

    public static long duskTicks() {
        return Math.floorMod(NoonTicks + Math.round(daylightShare() * CycleTicks / 2.0), CycleTicks);
    }
}
