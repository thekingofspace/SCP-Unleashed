package net.rai.scp_unleashed.scp129.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.scp129.Scp129Course;
import net.rai.scp_unleashed.scp129.Scp129Symptoms;
import net.rai.scp_unleashed.scp129.network.Scp129EpisodePayload;
import net.rai.scp_unleashed.scp129.network.Scp129StatePayload;

public final class Scp129ClientState {
    public static final float AimShare = 0.3f;
    public static final float AimRise = 6.0f;
    public static final int AimLinger = 30;
    public static final float RiseShare = 0.2f;
    public static final float FallShare = 0.3f;
    public static final float WobbleRate = 0.22f;
    public static final float WobbleDepth = 0.18f;
    public static final float JoltDegrees = 3.5f;
    private static int Stage;
    private static int BlurLeft;
    private static int BlurTotal;
    private static float BlurStrength;
    private static int FreezeLeft;
    private static int AimLeft;
    private static int AimTotal;
    private static int JoltLeft;
    private static float JoltStrength;
    private static int Clock;

    private Scp129ClientState() {
    }

    public static void apply(Scp129StatePayload Payload) {
        Stage = Mth.clamp(Payload.Stage(), 0, Scp129Course.Stages);
    }

    public static void apply(Scp129EpisodePayload Payload) {
        switch (Payload.Kind()) {
            case Scp129Symptoms.Blur -> {
                BlurTotal = Math.max(1, Payload.Ticks());
                BlurLeft = BlurTotal;
                BlurStrength = Math.max(0.0f, Payload.Strength());
            }
            case Scp129Symptoms.Freeze -> {
                FreezeLeft = Math.max(0, Payload.Ticks());
                AimTotal = FreezeLeft + AimLinger;
                AimLeft = AimTotal;
            }
            case Scp129Symptoms.Cough -> {
                JoltLeft = Math.max(1, Payload.Ticks());
                JoltStrength = Payload.Strength();
            }
            case Scp129Symptoms.Clear -> {
                BlurLeft = 0;
                FreezeLeft = 0;
                AimLeft = 0;
                JoltLeft = 0;
            }
            default -> {
            }
        }
    }

    public static void tick(Minecraft Client) {
        if (Client.player == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        Clock++;
        if (BlurLeft > 0) {
            BlurLeft--;
        }
        if (FreezeLeft > 0) {
            FreezeLeft--;
        }
        if (AimLeft > 0) {
            AimLeft--;
        }
        if (JoltLeft > 0) {
            JoltLeft--;
        }
    }

    public static int stage() {
        return Minecraft.getInstance().player == null ? 0 : Stage;
    }

    public static int clock() {
        return Clock;
    }

    public static float blur(float Partial) {
        if (BlurLeft <= 0 || BlurTotal <= 0) {
            return 0.0f;
        }
        float Age = BlurTotal - BlurLeft + Partial;
        float Left = Math.max(0.0f, BlurLeft - Partial);
        float Rise = Mth.clamp(Age / Math.max(1.0f, BlurTotal * RiseShare), 0.0f, 1.0f);
        float Fall = Mth.clamp(Left / Math.max(1.0f, BlurTotal * FallShare), 0.0f, 1.0f);
        float Envelope = Math.min(Rise * Rise * (3.0f - 2.0f * Rise), Fall * Fall * (3.0f - 2.0f * Fall));
        float Wobble = 1.0f - WobbleDepth + WobbleDepth * Mth.sin((Clock + Partial) * WobbleRate);
        return Envelope * Wobble * BlurStrength;
    }

    public static boolean frozen() {
        return FreezeLeft > 0 && Minecraft.getInstance().player != null;
    }

    public static float aim(float Partial) {
        if (AimLeft <= 0 || AimTotal <= 0 || Minecraft.getInstance().player == null) {
            return 1.0f;
        }
        float Age = AimTotal - AimLeft + Partial;
        float Left = Math.max(0.0f, AimLeft - Partial);
        float Rise = Mth.clamp(Age / AimRise, 0.0f, 1.0f);
        float Fall = Mth.clamp(Left / AimLinger, 0.0f, 1.0f);
        float Depth = Math.min(Rise * Rise * (3.0f - 2.0f * Rise), Fall * Fall * (3.0f - 2.0f * Fall));
        return 1.0f - (1.0f - AimShare) * Depth;
    }

    public static float jolt(float Partial) {
        if (JoltLeft <= 0) {
            return 0.0f;
        }
        float Share = Math.max(0.0f, JoltLeft - Partial) / Scp129Symptoms.JoltTicks;
        return Share * Share * JoltDegrees * Math.min(1.2f, JoltStrength);
    }

    public static void steer(Player Mover, Input Controls) {
        if (!frozen() || !(Mover instanceof LocalPlayer User)) {
            return;
        }
        Controls.leftImpulse = 0.0f;
        Controls.forwardImpulse = 0.0f;
        Controls.up = false;
        Controls.down = false;
        Controls.left = false;
        Controls.right = false;
        Controls.jumping = false;
        Controls.shiftKeyDown = false;
        User.setSprinting(false);
    }

    public static void reset() {
        Stage = 0;
        BlurLeft = 0;
        BlurTotal = 0;
        BlurStrength = 0.0f;
        FreezeLeft = 0;
        AimLeft = 0;
        AimTotal = 0;
        JoltLeft = 0;
        JoltStrength = 0.0f;
        Clock = 0;
    }
}
