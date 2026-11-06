package net.rai.scp_unleashed.scp018.client;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public final class BallClientData {
    public final Quaternionf Spin = new Quaternionf();
    public double SpinTime = Double.NaN;
    public double EventTime = Double.NaN;
    public double TrailTime = Double.NaN;
    public double LastSoundTime = Double.NEGATIVE_INFINITY;
    @Nullable
    public BallWhooshSound Whoosh;

    BallClientData(long Seed) {
        Spin.rotateXYZ((Seed & 0xFF) / 40.0f, ((Seed >> 8) & 0xFF) / 40.0f, ((Seed >> 16) & 0xFF) / 40.0f);
    }
}
