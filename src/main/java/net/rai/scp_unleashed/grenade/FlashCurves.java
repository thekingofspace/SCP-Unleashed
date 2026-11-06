package net.rai.scp_unleashed.grenade;

import net.minecraft.util.Mth;

public final class FlashCurves {
    private FlashCurves() {
    }

    public static float ease(float Value) {
        float Clamped = Mth.clamp(Value, 0.0F, 1.0F);
        return Clamped * Clamped * (3.0F - 2.0F * Clamped);
    }

    private static float share(float Value) {
        return Mth.clamp(Value, 0.0F, 1.0F);
    }

    public static float whitePeak(float White) {
        return share(White * GrenadeTuning.WhitePeakGain);
    }

    public static float whiteHold(float White) {
        return GrenadeTuning.WhiteHoldTicks * (float) Math.pow(share(White), GrenadeTuning.WhiteHoldCurve);
    }

    public static float whiteFade(float White) {
        return Mth.lerp(share(White), GrenadeTuning.WhiteFadeMinTicks, GrenadeTuning.WhiteFadeMaxTicks);
    }

    public static float whiteEnd(float White) {
        return White <= 0.0F ? 0.0F : whiteHold(White) + whiteFade(White);
    }

    public static float white(float White, float Age) {
        if (White <= 0.0F || Age < 0.0F) {
            return 0.0F;
        }
        float Hold = whiteHold(White);
        if (Age <= Hold) {
            return whitePeak(White);
        }
        float Into = (Age - Hold) / whiteFade(White);
        if (Into >= 1.0F) {
            return 0.0F;
        }
        return whitePeak(White) * (float) Math.pow(1.0F - ease(Into), GrenadeTuning.WhiteFadeCurve);
    }

    public static float blurPeak(float White) {
        return share(White * GrenadeTuning.BlurGain);
    }

    public static float blurFrom(float White) {
        return whiteHold(White) + whiteFade(White) * GrenadeTuning.BlurHandoff;
    }

    public static float blurFade(float White) {
        return Mth.lerp(share(White), GrenadeTuning.BlurFadeMinTicks, GrenadeTuning.BlurFadeMaxTicks);
    }

    public static float blurEnd(float White) {
        return White <= 0.0F ? 0.0F : blurFrom(White) + blurFade(White);
    }

    public static float blur(float White, float Age) {
        if (White <= 0.0F || Age < 0.0F) {
            return 0.0F;
        }
        float Rise = ease(Age / GrenadeTuning.BlurRiseTicks);
        float Into = (Age - blurFrom(White)) / blurFade(White);
        float Keep = Into <= 0.0F ? 1.0F : 1.0F - ease(Into);
        return blurPeak(White) * Rise * Keep;
    }

    public static float ringLength(float Ring) {
        return Ring <= 0.0F ? 0.0F : Mth.lerp(share(Ring), GrenadeTuning.RingMinTicks, GrenadeTuning.RingMaxTicks);
    }

    public static float ring(float Ring, float Age) {
        if (Ring <= 0.0F || Age < 0.0F) {
            return 0.0F;
        }
        float Into = Age / ringLength(Ring);
        if (Into >= 1.0F) {
            return 0.0F;
        }
        return share(Ring) * (float) Math.pow(1.0F - Into, GrenadeTuning.RingCurve);
    }

    public static float muffleLength(float Ring) {
        return Ring <= 0.0F ? 0.0F : Mth.lerp(share(Ring), GrenadeTuning.MuffleMinTicks, GrenadeTuning.MuffleMaxTicks);
    }

    public static float muffle(float Ring, float Age) {
        if (Ring <= 0.0F || Age < 0.0F) {
            return 0.0F;
        }
        float Left = share(Ring) * (1.0F - Age / muffleLength(Ring));
        return Left <= 0.0F ? 0.0F : GrenadeTuning.MaxMuffle * (float) Math.sqrt(Left);
    }

    public static float lasts(float White, float Ring) {
        return Math.max(Math.max(whiteEnd(White), blurEnd(White)), Math.max(ringLength(Ring), muffleLength(Ring)));
    }
}
