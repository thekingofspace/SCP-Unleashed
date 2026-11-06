package net.rai.scp_unleashed.storage.client;

import net.minecraft.util.Mth;

public record LayoutMotion(float BobHeight, float BobPeriod, float SpinSpeed, float SwayAngle, float SwayPeriod, float TwitchAngle, int TwitchEvery) {
    public static final LayoutMotion Still = new LayoutMotion(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0);
    public static final int TwitchTicks = 6;

    public static LayoutMotion bob(float Height, float Period) {
        return Still.withBob(Height, Period);
    }

    public static LayoutMotion spin(float DegreesPerTick) {
        return Still.withSpin(DegreesPerTick);
    }

    public static LayoutMotion sway(float Angle, float Period) {
        return Still.withSway(Angle, Period);
    }

    public static LayoutMotion twitch(float Angle, int Every) {
        return Still.withTwitch(Angle, Every);
    }

    public LayoutMotion withBob(float Height, float Period) {
        return new LayoutMotion(Height, Period, SpinSpeed, SwayAngle, SwayPeriod, TwitchAngle, TwitchEvery);
    }

    public LayoutMotion withSpin(float DegreesPerTick) {
        return new LayoutMotion(BobHeight, BobPeriod, DegreesPerTick, SwayAngle, SwayPeriod, TwitchAngle, TwitchEvery);
    }

    public LayoutMotion withSway(float Angle, float Period) {
        return new LayoutMotion(BobHeight, BobPeriod, SpinSpeed, Angle, Period, TwitchAngle, TwitchEvery);
    }

    public LayoutMotion withTwitch(float Angle, int Every) {
        return new LayoutMotion(BobHeight, BobPeriod, SpinSpeed, SwayAngle, SwayPeriod, Angle, Every);
    }

    public boolean isStill() {
        return BobHeight == 0.0f && SpinSpeed == 0.0f && SwayAngle == 0.0f && TwitchAngle == 0.0f;
    }

    public float liftAt(float Time, long Seed) {
        if (BobHeight == 0.0f || BobPeriod <= 0.0f) {
            return 0.0f;
        }
        return BobHeight * Mth.sin((Time + phase(Seed, BobPeriod)) / BobPeriod * Mth.TWO_PI);
    }

    public float spinAt(float Time) {
        return SpinSpeed * Time % 360.0f;
    }

    public float swayAt(float Time, long Seed) {
        if (SwayAngle == 0.0f || SwayPeriod <= 0.0f) {
            return 0.0f;
        }
        return SwayAngle * Mth.sin((Time + phase(Seed, SwayPeriod)) / SwayPeriod * Mth.TWO_PI);
    }

    public float twitchAt(float Time, long Seed) {
        if (TwitchAngle == 0.0f || TwitchEvery <= 0) {
            return 0.0f;
        }
        float Local = (Time + phase(Seed, TwitchEvery)) % TwitchEvery;
        if (Local > TwitchTicks) {
            return 0.0f;
        }
        return TwitchAngle * (float) Math.exp(-Local / 1.6f) * Mth.sin(Local * 2.6f);
    }

    private static float phase(long Seed, float Period) {
        long Mixed = Seed * 0x9E3779B97F4A7C15L;
        return (float) ((Mixed >>> 40) % 1000L) / 1000.0f * Period;
    }
}
