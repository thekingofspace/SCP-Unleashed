package net.rai.scp_unleashed.scp1730.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.network.Scp1730HoldPayload;
import net.rai.scp_unleashed.scp1730.network.Scp1730PulsePayload;
import net.rai.scp_unleashed.scp1730.network.Scp1730ShakePayload;

public final class Scp1730ClientHooks {
    public static final double ControlShare = 0.1;
    private static volatile boolean Holding;
    private static volatile Vec3 Target = Vec3.ZERO;
    private static volatile float Pull;
    private static volatile float Shake;
    private static volatile int ShakeTicks;
    private static long LastFrame = -1L;

    private Scp1730ClientHooks() {
    }

    public static void hold(Scp1730HoldPayload Payload) {
        Holding = Payload.Active();
        Target = new Vec3(Payload.X(), Payload.Y(), Payload.Z());
        Pull = Payload.Pull();
        LastFrame = -1L;
    }

    public static void shake(Scp1730ShakePayload Payload) {
        Shake = Math.max(Shake * 0.5f, Payload.Intensity());
        ShakeTicks = Math.max(ShakeTicks, Payload.Ticks());
    }

    public static void pulse(Scp1730PulsePayload Payload) {
        Scp1730AnchorPulse.start(Payload);
    }

    public static boolean holding() {
        return Holding;
    }

    public static float shakeStrength() {
        return ShakeTicks > 0 ? Shake : 0.0f;
    }

    public static void tick() {
        if (ShakeTicks > 0) {
            ShakeTicks--;
        }
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            Holding = false;
            ShakeTicks = 0;
        }
    }

    public static void reset() {
        Holding = false;
        ShakeTicks = 0;
        Shake = 0.0f;
    }

    public static void pullFrame() {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (!Holding || User == null) {
            LastFrame = -1L;
            return;
        }
        long Now = System.nanoTime();
        double Seconds = LastFrame < 0L ? 0.016 : Math.min(0.1, (Now - LastFrame) / 1.0e9);
        LastFrame = Now;
        Vec3 Eye = User.getEyePosition();
        Vec3 Way = Target.subtract(Eye);
        double Flat = Math.sqrt(Way.x * Way.x + Way.z * Way.z);
        float WantYaw = (float) (Mth.atan2(Way.z, Way.x) * Mth.RAD_TO_DEG) - 90.0f;
        float WantPitch = (float) -(Mth.atan2(Way.y, Math.max(1.0e-4, Flat)) * Mth.RAD_TO_DEG);
        double Share = 1.0 - Math.pow(1.0 - Mth.clamp(Pull, 0.0f, 0.99f), Seconds * 20.0);
        float Yaw = User.getYRot() + (float) (Mth.wrapDegrees(WantYaw - User.getYRot()) * Share);
        float Pitch = User.getXRot() + (float) ((WantPitch - User.getXRot()) * Share);
        User.setYRot(Yaw);
        User.setXRot(Mth.clamp(Pitch, -90.0f, 90.0f));
        User.yRotO = User.getYRot();
        User.xRotO = User.getXRot();
    }
}
