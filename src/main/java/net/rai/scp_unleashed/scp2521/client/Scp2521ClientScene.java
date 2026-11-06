package net.rai.scp_unleashed.scp2521.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp2521.Scp2521;
import net.rai.scp_unleashed.scp2521.Scp2521Scene;
import net.rai.scp_unleashed.scp2521.Scp2521Void;
import net.rai.scp_unleashed.scp2521.network.Scp2521ScenePayload;

public final class Scp2521ClientScene {
    public static final int ModeNone = 0;
    public static final int ModeWorld = 1;
    public static final int ModeVoid = 2;
    public static final int ModeFall = 3;
    public static final int ModeHome = 4;
    public static final int Timeout = Scp2521Scene.HomeAt + 200;
    public static final float Follow = 0.22f;
    public static final float LegPitch = 58.0f;
    public static final float PulledPitch = 72.0f;
    public static final float FallPitch = -58.0f;
    private static int Mode = ModeNone;
    private static int Clock;
    private static int Target = -1;
    private static float ReturnYaw;
    private static float ReturnPitch;
    private static float Yaw;
    private static float Pitch;
    private static float YawBefore;
    private static float PitchBefore;
    private static float Roll;
    private static float RollBefore;

    private Scp2521ClientScene() {
    }

    public static void receive(Scp2521ScenePayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        switch (Payload.Kind()) {
            case Scp2521ScenePayload.Begin -> {
                Mode = ModeWorld;
                Clock = Payload.Ticks();
                Target = Payload.Target();
                ReturnYaw = Payload.Yaw();
                ReturnPitch = Payload.Pitch();
                Yaw = User == null ? ReturnYaw : User.getYRot();
                Pitch = User == null ? ReturnPitch : User.getXRot();
                YawBefore = Yaw;
                PitchBefore = Pitch;
            }
            case Scp2521ScenePayload.Void -> {
                Mode = ModeVoid;
                Clock = Payload.Ticks();
                Target = Payload.Target();
                Yaw = Payload.Yaw();
                Pitch = Payload.Pitch();
                YawBefore = Yaw;
                PitchBefore = Pitch;
            }
            case Scp2521ScenePayload.Fall -> {
                Mode = ModeFall;
                Clock = Payload.Ticks();
            }
            case Scp2521ScenePayload.Home -> {
                Mode = ModeHome;
                Clock = Scp2521Scene.HomeAt;
                Yaw = Payload.Yaw();
                Pitch = Payload.Pitch();
                YawBefore = Yaw;
                PitchBefore = Pitch;
                Roll = 0.0f;
                RollBefore = 0.0f;
            }
            default -> reset();
        }
    }

    public static void tick(Minecraft Client) {
        if (Mode == ModeNone) {
            return;
        }
        if (Client.level == null || Client.player == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        Clock++;
        YawBefore = Yaw;
        PitchBefore = Pitch;
        RollBefore = Roll;
        LocalPlayer User = Client.player;
        if (Mode == ModeHome) {
            if (Clock > Scp2521Scene.HomeAt + Scp2521Scene.HomeFade) {
                reset();
                return;
            }
            hold(User, Yaw, Pitch);
            return;
        }
        if (Clock > Timeout) {
            reset();
            return;
        }
        float WantedYaw = Yaw;
        float WantedPitch = Pitch;
        Entity Found = Target >= 0 ? Client.level.getEntity(Target) : null;
        Vec3 Eye = User.getEyePosition();
        if (Found instanceof Scp2521 Creature) {
            Vec3 Face = Creature.position().add(0.0, Scp2521.EyeHeight * Creature.squash() * (Mode == ModeWorld ? Math.min(1.0f, Clock / 50.0f) : 1.0f), 0.0);
            Vec3 Way = Face.subtract(Eye);
            WantedYaw = (float) (Mth.atan2(Way.z, Way.x) * Mth.RAD_TO_DEG) - 90.0f;
            WantedPitch = (float) (-(Mth.atan2(Way.y, Way.horizontalDistance()) * Mth.RAD_TO_DEG));
        }
        if (Mode == ModeWorld) {
            float Legs = Scp2521Scene.smooth((Clock - Scp2521Scene.BindAt - 4) / 18.0f) * (1.0f - Scp2521Scene.smooth((Clock - Scp2521Scene.GrabAt + 18) / 16.0f));
            WantedPitch = Mth.lerp(Legs, WantedPitch, LegPitch);
            float Pulled = Scp2521Scene.smooth((Clock - Scp2521Scene.CarryAt) / 18.0f);
            WantedPitch = Mth.lerp(Pulled, WantedPitch, PulledPitch);
            Roll = Pulled * 9.0f;
        } else if (Mode == ModeVoid) {
            Roll = Mth.sin(Clock * 0.035f) * 1.6f;
        } else if (Mode == ModeFall) {
            float Falling = Scp2521Scene.smooth((Clock - Scp2521Scene.FallAt) / 22.0f);
            WantedPitch = Mth.lerp(Falling, WantedPitch, FallPitch);
            Roll = Mth.sin(Clock * 0.3f) * 6.0f * Falling;
        }
        Yaw = Yaw + Mth.wrapDegrees(WantedYaw - Yaw) * Follow;
        Pitch = Pitch + (WantedPitch - Pitch) * Follow;
        hold(User, Yaw, Pitch);
    }

    public static void hold(LocalPlayer User, float HeldYaw, float HeldPitch) {
        User.setYRot(HeldYaw);
        User.setXRot(HeldPitch);
        User.yRotO = HeldYaw;
        User.xRotO = HeldPitch;
        User.setYHeadRot(HeldYaw);
        User.yHeadRotO = HeldYaw;
        User.setYBodyRot(HeldYaw);
        User.yBodyRotO = HeldYaw;
    }

    public static float yaw(float Partial) {
        return YawBefore + Mth.wrapDegrees(Yaw - YawBefore) * Partial;
    }

    public static float pitch(float Partial) {
        return Mth.lerp(Partial, PitchBefore, Pitch);
    }

    public static float roll(float Partial) {
        return Mth.lerp(Partial, RollBefore, Roll);
    }

    public static boolean cinematic() {
        return Mode != ModeNone;
    }

    public static boolean steering() {
        return Mode == ModeWorld || Mode == ModeVoid || Mode == ModeFall || Mode == ModeHome;
    }

    public static boolean inVoid() {
        return Mode == ModeVoid || Mode == ModeFall;
    }

    public static float time(float Partial) {
        return Clock + Partial;
    }

    public static int mode() {
        return Mode;
    }

    public static float darkness(float Partial) {
        return Mode == ModeNone ? 0.0f : Scp2521Scene.darkness(Clock + Partial);
    }

    public static float bars(float Partial) {
        return Mode == ModeNone ? 0.0f : Scp2521Scene.bars(Clock + Partial);
    }

    public static boolean voidAround(Minecraft Client) {
        return Client.level != null && Scp2521Void.inside(Client.level);
    }

    public static void reset() {
        Mode = ModeNone;
        Clock = 0;
        Target = -1;
        Roll = 0.0f;
        RollBefore = 0.0f;
    }
}
