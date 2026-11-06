package net.rai.scp_unleashed.scp035.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp035.Scp035Sight;

public final class Scp035ClientMask {
    public static final float CloseStep = 0.12f;
    public static final float OpenStep = 0.25f;
    public static final float FovCut = 0.12f;
    public static final float BreathPeriod = 90.0f;
    private static float Ramp;
    private static float RampO;
    private static float Phase;
    private static float PhaseO;
    private static float Depth = 1.0f;

    private Scp035ClientMask() {
    }

    public static boolean isWorn() {
        LocalPlayer User = Minecraft.getInstance().player;
        return User != null && User.isAlive() && !User.isSpectator() && Scp035Sight.wearsMask(User);
    }

    public static void tick(Minecraft Client) {
        RampO = Ramp;
        Ramp = isWorn() ? Math.min(1.0f, Ramp + CloseStep) : Math.max(0.0f, Ramp - OpenStep);
        LocalPlayer User = Client.player;
        float Hurt = User == null ? 0.0f : 1.0f - Mth.clamp(User.getHealth() / Math.max(1.0f, User.getMaxHealth()), 0.0f, 1.0f);
        Depth = 1.0f + Hurt;
        PhaseO = Phase;
        Phase += (1.0f + Hurt * 0.8f) / BreathPeriod * Mth.TWO_PI;
        if (Phase > Mth.TWO_PI) {
            Phase -= Mth.TWO_PI;
            PhaseO -= Mth.TWO_PI;
        }
    }

    public static float ramp(float PartialTick) {
        return Mth.lerp(PartialTick, RampO, Ramp);
    }

    public static float eased(float PartialTick) {
        float Value = ramp(PartialTick);
        return 1.0f - (1.0f - Value) * (1.0f - Value) * (1.0f - Value);
    }

    public static double fov(double Original, float PartialTick) {
        float Value = eased(PartialTick);
        return Value <= 0.0f ? Original : Original * (1.0f - FovCut * Value);
    }

    public static float breath(float PartialTick) {
        return Mth.sin(Mth.lerp(PartialTick, PhaseO, Phase)) * Depth;
    }

    public static void reset() {
        Ramp = 0.0f;
        RampO = 0.0f;
        Phase = 0.0f;
        PhaseO = 0.0f;
        Depth = 1.0f;
    }
}
