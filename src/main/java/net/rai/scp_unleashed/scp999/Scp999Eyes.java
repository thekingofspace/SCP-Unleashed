package net.rai.scp_unleashed.scp999;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class Scp999Eyes {
    public static final int BlinkMin = 50;
    public static final int BlinkMax = 120;
    public static final int BlinkTicks = 3;
    public static final int GlanceMin = 25;
    public static final int GlanceMax = 70;
    public static final float GlanceReach = 0.55f;
    public static final float GlanceRate = 0.35f;
    private float GlanceX;
    private float GlanceY;
    private float GlanceXo;
    private float GlanceYo;
    private float TargetX;
    private float TargetY;
    private int BlinkLeft;
    private int NextBlink = BlinkMin;
    private int NextGlance = GlanceMin;

    public void tick(Scp999Blob Blob) {
        RandomSource Random = Blob.getRandom();
        GlanceXo = GlanceX;
        GlanceYo = GlanceY;
        if (--NextGlance <= 0) {
            NextGlance = GlanceMin + Random.nextInt(GlanceMax - GlanceMin + 1);
            boolean Centre = Random.nextFloat() < 0.4f;
            TargetX = Centre ? 0.0f : (Random.nextFloat() * 2.0f - 1.0f) * GlanceReach;
            TargetY = Centre ? 0.0f : (Random.nextFloat() * 2.0f - 1.0f) * GlanceReach * 0.6f;
        }
        GlanceX += (TargetX - GlanceX) * GlanceRate;
        GlanceY += (TargetY - GlanceY) * GlanceRate;
        if (BlinkLeft > 0) {
            BlinkLeft--;
        } else if (--NextBlink <= 0) {
            NextBlink = BlinkMin + Random.nextInt(BlinkMax - BlinkMin + 1);
            BlinkLeft = BlinkTicks;
        }
    }

    public float glanceX(float Partial) {
        return Mth.lerp(Partial, GlanceXo, GlanceX);
    }

    public float glanceY(float Partial) {
        return Mth.lerp(Partial, GlanceYo, GlanceY);
    }

    public boolean blinking() {
        return BlinkLeft > 0;
    }
}
