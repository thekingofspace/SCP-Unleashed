package net.rai.scp_unleashed.scp902.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import net.rai.scp_unleashed.scp902.Scp902Scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public final class Scp902Ambience {
    public static final int RifleMin = 14;
    public static final int RifleMax = 55;
    public static final int BurstMin = 80;
    public static final int BurstMax = 190;
    public static final int ShellMin = 120;
    public static final int ShellMax = 280;
    public static final int RainMin = 28;
    public static final int RainMax = 44;
    public static final int SteelMin = 6;
    public static final int SteelMax = 26;
    public static final int MudMin = 10;
    public static final int MudMax = 34;
    public static final int WindMin = 140;
    public static final int WindMax = 260;
    public static final double NearShot = 40.0;
    public static final double FarShot = 115.0;
    public static final double NearShell = 95.0;
    public static final double FarShell = 150.0;
    public static final float FrontShare = 0.65f;
    public static final float FrontSpread = 75.0f;
    public static final int HushBefore = 60;
    public static final float FlashDecay = 0.86f;
    public static final double SoundSpeed = 17.0;
    private static final RandomSource Random = RandomSource.create();
    private static final List<Pending> Queue = new ArrayList<>();
    private static int NextRifle;
    private static int NextBurst;
    private static int NextShell;
    private static int NextRain;
    private static int NextSteel;
    private static int NextMud;
    private static int NextWind;
    private static int Now;
    private static float Flash;
    private static float LastFlash;
    private static float FlashBearing;

    private record Pending(int Due, ResourceLocation Sound, Vec3 At, float Volume, float Pitch, boolean Flat) {
    }

    private Scp902Ambience() {
    }

    public static void reset() {
        Scp902Overhead.reset();
        Queue.clear();
        NextRifle = 20;
        NextBurst = 60;
        NextShell = 90;
        NextRain = 0;
        NextSteel = 10;
        NextMud = 14;
        NextWind = 30;
        Now = 0;
        Flash = 0.0f;
        LastFlash = 0.0f;
    }

    public static boolean hushed(int Clock) {
        return Clock >= Scp902Scene.TextAt - HushBefore && Clock < Scp902Scene.TextAt + Scp902Scene.TextIn + Scp902Scene.TextHold;
    }

    public static void tick(Minecraft Client, int Clock) {
        Now = Clock;
        LastFlash = Flash;
        Flash *= FlashDecay;
        Vec3 Ear = Client.player.getEyePosition();
        Iterator<Pending> Walk = Queue.iterator();
        List<Pending> Ready = new ArrayList<>();
        while (Walk.hasNext()) {
            Pending Next = Walk.next();
            if (Next.Due() <= Clock) {
                Ready.add(Next);
                Walk.remove();
            }
        }
        for (Pending Next : Ready) {
            play(Client, Next.Sound(), Next.At(), Next.Volume(), Next.Pitch(), Next.Flat());
        }
        Scp902Overhead.tick(Client, Clock);
        boolean Hush = hushed(Clock);
        if (Clock >= NextRifle) {
            NextRifle = Clock + between(RifleMin, RifleMax);
            if (!Hush) {
                rifle(Ear);
            }
        }
        if (Clock >= NextBurst) {
            NextBurst = Clock + between(BurstMin, BurstMax);
            if (!Hush) {
                burst(Ear);
            }
        }
        if (Clock >= NextShell) {
            NextShell = Clock + between(ShellMin, ShellMax);
            if (!Hush) {
                shell(Ear);
            }
        }
        if (Clock >= NextRain) {
            NextRain = Clock + between(RainMin, RainMax);
            later(0, Scp902Registry.RainHeavy, Ear, 0.55f, 0.85f + Random.nextFloat() * 0.15f, true);
        }
        if (Clock >= NextSteel) {
            NextSteel = Clock + between(SteelMin, SteelMax);
            later(0, Scp902Registry.RainSteel, around(Ear, 1.5, 4.5, -1.2), 0.35f, 1.2f + Random.nextFloat() * 0.4f, false);
        }
        if (Clock >= NextMud) {
            NextMud = Clock + between(MudMin, MudMax);
            later(0, Scp902Registry.RainMud, around(Ear, 1.0, 3.5, -1.5), 0.22f, 1.1f + Random.nextFloat() * 0.3f, false);
        }
        if (Clock >= NextWind) {
            NextWind = Clock + between(WindMin, WindMax);
            later(0, Scp902Registry.Wind, Ear, 0.32f, 0.62f + Random.nextFloat() * 0.18f, true);
        }
    }

    private static int between(int Low, int High) {
        return Low + Random.nextInt(Math.max(1, High - Low + 1));
    }

    private static float bearing() {
        if (Random.nextFloat() < FrontShare) {
            return 180.0f + (Random.nextFloat() * 2.0f - 1.0f) * FrontSpread;
        }
        return Random.nextFloat() * 360.0f;
    }

    private static Vec3 toward(Vec3 Ear, float Bearing, double Distance) {
        float Radians = Bearing * Mth.DEG_TO_RAD;
        return new Vec3(Ear.x - Mth.sin(Radians) * Distance, Ear.y - 1.0, Ear.z + Mth.cos(Radians) * Distance);
    }

    private static Vec3 around(Vec3 Ear, double Near, double Far, double Drop) {
        float Bearing = Random.nextFloat() * 360.0f;
        return toward(Ear, Bearing, Near + Random.nextDouble() * (Far - Near)).add(0.0, 1.0 + Drop, 0.0);
    }

    private static int delay(double Distance) {
        return (int) Math.round(Distance / SoundSpeed);
    }

    private static void rifle(Vec3 Ear) {
        double Distance = NearShot + Random.nextDouble() * (FarShot - NearShot);
        Vec3 At = toward(Ear, bearing(), Distance);
        float Pitch = 0.78f + Random.nextFloat() * 0.2f;
        int Shots = Random.nextFloat() < 0.3f ? 2 : 1;
        int Gap = 0;
        for (int Shot = 0; Shot < Shots; Shot++) {
            later(Gap, Scp902Registry.FarRifle, At, 0.9f, Pitch, false);
            later(Gap, Scp902Registry.FarCrack, At, 0.35f, Pitch * 0.9f, false);
            later(Gap + 2, GunsRegistry.ShotEcho, At, 0.3f, Pitch * 0.62f, false);
            Gap += 5 + Random.nextInt(9);
        }
    }

    private static void burst(Vec3 Ear) {
        double Distance = NearShot + 20.0 + Random.nextDouble() * (FarShot - NearShot);
        Vec3 At = toward(Ear, bearing(), Distance);
        int Shots = 6 + Random.nextInt(8);
        float Pitch = 0.86f + Random.nextFloat() * 0.12f;
        int Gap = 0;
        for (int Shot = 0; Shot < Shots; Shot++) {
            later(Gap, Scp902Registry.FarBurst, At, 0.75f, Pitch + (Random.nextFloat() - 0.5f) * 0.04f, false);
            Gap += 2 + (Random.nextFloat() < 0.25f ? 1 : 0);
        }
        later(Gap + 2, GunsRegistry.ShotEcho, At, 0.32f, Pitch * 0.58f, false);
    }

    private static void shell(Vec3 Ear) {
        float Bearing = bearing();
        double Distance = NearShell + Random.nextDouble() * (FarShell - NearShell);
        Vec3 At = toward(Ear, Bearing, Distance);
        Flash = Math.min(1.0f, Flash + 0.65f + Random.nextFloat() * 0.35f);
        FlashBearing = Bearing;
        int Travel = delay(Distance);
        later(Travel, Scp902Registry.FarArtillery, At, 1.0f, 0.5f + Random.nextFloat() * 0.18f, false);
        later(Travel + 3, Scp902Registry.FarRumble, At, 0.8f, 0.55f + Random.nextFloat() * 0.15f, false);
        later(Travel + 1, GunsRegistry.ShotThunder, At, 0.35f, 0.5f, false);
    }

    private static void later(int Delay, Supplier<SoundEvent> Sound, Vec3 At, float Volume, float Pitch, boolean Flat) {
        Queue.add(new Pending(Now + Delay, Sound.get().getLocation(), At, Volume, Pitch, Flat));
    }

    private static void play(Minecraft Client, ResourceLocation Sound, Vec3 At, float Volume, float Pitch, boolean Flat) {
        Client.getSoundManager().play(new SimpleSoundInstance(Sound, SoundSource.AMBIENT, Volume, Mth.clamp(Pitch, 0.5f, 2.0f), Random, false, 0,
                Flat ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR, At.x, At.y, At.z, Flat));
    }

    public static float flash(float Partial) {
        return Mth.lerp(Partial, LastFlash, Flash);
    }

    public static float flashBearing() {
        return FlashBearing;
    }
}
