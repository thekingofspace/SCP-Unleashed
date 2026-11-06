package net.rai.scp_unleashed.fluiddump;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.rai.scp_unleashed.fluiddump.network.PollutionPatch;

public final class PollutionSource {
    private final BlockPos Centre;
    private double Load;
    private double Radius;
    private long LastFed;
    private long NextSurvey;
    private double SentRadius = -1.0;
    private double SentDensity = -1.0;

    public PollutionSource(BlockPos Centre, double Load, double Radius, long LastFed, long NextSurvey) {
        this.Centre = Centre.immutable();
        this.Load = Load;
        this.Radius = Radius;
        this.LastFed = LastFed;
        this.NextSurvey = NextSurvey;
    }

    public BlockPos getCentre() {
        return Centre;
    }

    public double getLoad() {
        return Load;
    }

    public double getRadius() {
        return Radius;
    }

    public double getDensity() {
        return PollutionMath.density(Load);
    }

    public long getLastFed() {
        return LastFed;
    }

    public long getNextSurvey() {
        return NextSurvey;
    }

    public void setNextSurvey(long Time) {
        NextSurvey = Time;
    }

    public boolean isFeeding(long Now) {
        return Now - LastFed <= PollutionMath.GraceTicks;
    }

    public void feed(double Buckets, long Now) {
        Load = PollutionMath.feed(Load, Buckets);
        LastFed = Now;
    }

    public void advance(long Now, long Ticks) {
        Load = PollutionMath.decay(Load, Now - LastFed, Ticks);
        Radius = PollutionMath.stepRadius(Radius, PollutionMath.targetRadius(Load), Ticks);
    }

    public double densityAt(double X, double Y, double Z) {
        return PollutionMath.local(X - (Centre.getX() + 0.5), Y - (Centre.getY() + 0.5), Z - (Centre.getZ() + 0.5), Radius, getDensity());
    }

    public double distanceTo(BlockPos Pos) {
        return Math.sqrt(Centre.distSqr(Pos));
    }

    public boolean drifted(double RadiusStep, double DensityStep) {
        return Math.abs(Radius - SentRadius) >= RadiusStep || Math.abs(getDensity() - SentDensity) >= DensityStep;
    }

    public void markSent() {
        SentRadius = Radius;
        SentDensity = getDensity();
    }

    public PollutionPatch patch() {
        return new PollutionPatch(Centre.getX(), Centre.getY(), Centre.getZ(), (float) Radius, (float) getDensity());
    }

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("X", Centre.getX());
        Tag.putInt("Y", Centre.getY());
        Tag.putInt("Z", Centre.getZ());
        Tag.putDouble("Load", Load);
        Tag.putDouble("Radius", Radius);
        Tag.putLong("LastFed", LastFed);
        Tag.putLong("NextSurvey", NextSurvey);
        return Tag;
    }

    public static PollutionSource load(CompoundTag Tag) {
        return new PollutionSource(new BlockPos(Tag.getInt("X"), Tag.getInt("Y"), Tag.getInt("Z")), Tag.getDouble("Load"), Tag.getDouble("Radius"),
                Tag.getLong("LastFed"), Tag.getLong("NextSurvey"));
    }
}
