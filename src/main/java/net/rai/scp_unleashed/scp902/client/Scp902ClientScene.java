package net.rai.scp_unleashed.scp902.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import net.rai.scp_unleashed.scp902.Scp902Scene;
import net.rai.scp_unleashed.scp902.Scp902Wastes;
import net.rai.scp_unleashed.scp902.network.Scp902ScenePayload;

public final class Scp902ClientScene {
    public static final int ModeNone = 0;
    public static final int ModeDepart = 1;
    public static final int ModeScene = 2;
    public static final int ModeHome = 3;
    public static final int DepartTimeout = 80;
    public static final double DriftPush = 0.0105;
    public static final double DriftStopZ = -96.0;
    public static final float SwayRoll = 0.7f;
    public static final float SwayPitch = 0.45f;
    public static final float BreathRate = 0.07f;
    public static final float SwayRate = 0.031f;
    private static final RandomSource Random = RandomSource.create();
    private static int Mode = ModeNone;
    private static int Clock;
    private static int Length = Scp902Scene.ReturnAt;
    private static boolean Toned;

    private Scp902ClientScene() {
    }

    public static void receive(int Kind, int Ticks) {
        Minecraft Client = Minecraft.getInstance();
        switch (Kind) {
            case Scp902ScenePayload.Depart -> {
                Mode = ModeDepart;
                Clock = 0;
            }
            case Scp902ScenePayload.Arrive -> {
                Mode = ModeScene;
                Clock = 0;
                Length = Math.max(1, Ticks);
                Toned = false;
                Scp902Ambience.reset();
                ui(Client, Scp902Registry.ArriveSwell.get().getLocation(), 0.85f, 1.0f);
            }
            case Scp902ScenePayload.Home -> {
                Mode = ModeHome;
                Clock = 0;
                ui(Client, Scp902Registry.ReturnSwell.get().getLocation(), 0.7f, 1.0f);
            }
            default -> {
                Mode = ModeNone;
                Clock = 0;
            }
        }
    }

    public static void ui(Minecraft Client, ResourceLocation Sound, float Volume, float Pitch) {
        Client.getSoundManager().play(new SimpleSoundInstance(Sound, SoundSource.AMBIENT, Volume, Pitch, Random, false, 0, SoundInstance.Attenuation.NONE,
                0.0, 0.0, 0.0, true));
    }

    public static void tick(Minecraft Client) {
        if (Client.level == null || Client.player == null) {
            Mode = ModeNone;
            return;
        }
        if (Scp902Wastes.inside(Client.level)) {
            Client.level.setRainLevel(1.0f);
            Client.level.setThunderLevel(0.0f);
            if (Mode != ModeScene && !Client.player.isSpectator() && !Client.player.isCreative()) {
                receive(Scp902ScenePayload.Arrive, Scp902Scene.ReturnAt);
            }
        }
        if (Client.isPaused()) {
            return;
        }
        Clock++;
        switch (Mode) {
            case ModeDepart -> {
                if (Clock > DepartTimeout) {
                    Mode = ModeNone;
                }
            }
            case ModeScene -> {
                if (!Scp902Wastes.inside(Client.level)) {
                    if (Clock > Length + 40) {
                        Mode = ModeNone;
                    }
                    return;
                }
                if (!Toned && Clock >= Scp902Scene.TextAt) {
                    Toned = true;
                    ui(Client, Scp902Registry.TextTone.get().getLocation(), 0.55f, 1.0f);
                    ui(Client, Scp902Registry.TextBell.get().getLocation(), 0.35f, 1.0f);
                }
                Scp902Ambience.tick(Client, Clock);
                hold(Client.player);
                drift(Client.player);
                if (Clock > Length + 200) {
                    Mode = ModeNone;
                }
            }
            case ModeHome -> {
                if (Clock > Scp902Scene.HomeFade) {
                    Mode = ModeNone;
                }
            }
            default -> {
            }
        }
    }

    public static boolean locked() {
        return Mode == ModeScene || Mode == ModeDepart;
    }

    public static boolean cinematic() {
        return Mode == ModeScene || Mode == ModeDepart;
    }

    public static void hold(LocalPlayer User) {
        User.setYRot(Scp902Wastes.ArrivalYaw);
        User.setXRot(Scp902Wastes.ArrivalPitch);
        User.yRotO = Scp902Wastes.ArrivalYaw;
        User.xRotO = Scp902Wastes.ArrivalPitch;
        User.setYHeadRot(Scp902Wastes.ArrivalYaw);
        User.yHeadRotO = Scp902Wastes.ArrivalYaw;
        User.setYBodyRot(Scp902Wastes.ArrivalYaw);
        User.yBodyRotO = Scp902Wastes.ArrivalYaw;
    }

    private static void drift(LocalPlayer User) {
        if (!User.onGround() || Clock < Scp902Scene.ArriveFade || Clock > Scp902Scene.FinishAt) {
            return;
        }
        if (User.getZ() < DriftStopZ) {
            return;
        }
        Vec3 Motion = User.getDeltaMovement();
        User.setDeltaMovement(Motion.x, Motion.y, Motion.z - DriftPush);
        if (User.isSprinting()) {
            User.setSprinting(false);
        }
    }

    public static int mode() {
        return Mode;
    }

    public static boolean inScene() {
        return Mode == ModeScene;
    }

    public static float sceneTicks(float Partial) {
        return Mode == ModeScene ? Clock + Partial : 0.0f;
    }

    public static float white(float Partial) {
        float Time = Clock + Partial;
        return switch (Mode) {
            case ModeDepart -> Scp902Scene.departWhite(Time);
            case ModeScene -> Scp902Scene.sceneWhite(Time);
            case ModeHome -> Scp902Scene.homeWhite(Time);
            default -> 0.0f;
        };
    }

    public static float strength(float Partial) {
        if (Mode != ModeScene) {
            return Mode == ModeDepart ? Scp902Scene.departWhite(Clock + Partial) : 0.0f;
        }
        return 1.0f;
    }

    public static float text(float Partial) {
        return Mode == ModeScene ? Scp902Scene.text(Clock + Partial) : 0.0f;
    }

    public static float bars(float Partial) {
        return switch (Mode) {
            case ModeScene -> Scp902Scene.bars(Clock + Partial);
            case ModeDepart -> Scp902Scene.departWhite(Clock + Partial);
            default -> 0.0f;
        };
    }

    public static float sluggish() {
        return locked() ? 0.0f : 1.0f;
    }

    public static float roll(float Partial) {
        if (Mode != ModeScene) {
            return 0.0f;
        }
        float Time = Clock + Partial;
        return Mth.sin(Time * SwayRate) * SwayRoll + Mth.sin(Time * SwayRate * 2.3f + 1.7f) * SwayRoll * 0.35f;
    }

    public static float pitch(float Partial) {
        if (Mode != ModeScene) {
            return 0.0f;
        }
        float Time = Clock + Partial;
        return Mth.sin(Time * BreathRate) * SwayPitch;
    }

    public static void reset() {
        Mode = ModeNone;
        Clock = 0;
        Scp902Ambience.reset();
    }
}
