package net.rai.scp_unleashed.scp049;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public final class Scp049Surgery {
    public static final int Length = 100;
    public static final int PickOne = 4;
    public static final int HumStart = 8;
    public static final int SnipOne = 14;
    public static final int SetOne = 22;
    public static final int SweepOne = 28;
    public static final int SnipTwo = 36;
    public static final int Uncork = 44;
    public static final int Pour = 48;
    public static final int Brew = 54;
    public static final int ChantStart = 58;
    public static final int PickTwo = 62;
    public static final int SnipThree = 66;
    public static final int ToneOne = 70;
    public static final int SweepTwo = 72;
    public static final int ToneTwo = 76;
    public static final int ToneThree = 82;
    public static final int ToneFour = 88;
    public static final int SetTwo = 92;
    public static final int Stinger = 95;
    public static final int Burst = 97;
    public static final int GlowEvery = 20;
    public static final int GlowOffset = 10;
    public static final int HumLength = 30;
    public static final int ChantLength = 22;
    public static final int StingerLength = 30;
    public static final double HearingRange = 24.0;
    public static final double TrayNear = 3.0;
    private static final int[] Tones = {ToneOne, ToneTwo, ToneThree, ToneFour};
    private static final float[] TonePitch = {0.7f, 0.84f, 1.0f, 1.2f};
    private static final int[][] Loud = {{HumStart, HumStart + HumLength}, {ChantStart, ChantStart + ChantLength}, {ToneOne, ToneFour + 8}, {Stinger, Stinger + StingerLength}};

    private Scp049Surgery() {
    }

    public static boolean busy(int From, int Ahead) {
        for (int[] Window : Loud) {
            if (From + Ahead >= Window[0] && From <= Window[1]) {
                return true;
            }
        }
        return false;
    }

    public static void clientTick(Scp049Doctor Doctor, int From, int To) {
        Level World = Doctor.level();
        if (!World.isClientSide || !World.hasNearbyAlivePlayer(Doctor.getX(), Doctor.getY(), Doctor.getZ(), HearingRange)) {
            return;
        }
        Scene Where = scene(Doctor);
        for (int Tick = Math.max(0, From + 1); Tick <= Math.min(To, Length); Tick++) {
            continuous(World, Doctor.getRandom(), Where, Tick);
            beat(World, Doctor, Doctor.getRandom(), Where, Tick);
        }
    }

    public record Scene(Vec3 Tray, Vec3 Patient, double PatientHeight, Vec3 RightHand, Vec3 LeftHand) {
    }

    public static Scene scene(Scp049Doctor Doctor) {
        float Body = Doctor.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 Forward = new Vec3(-Mth.sin(Body), 0.0, Mth.cos(Body));
        Vec3 Right = new Vec3(-Mth.cos(Body), 0.0, -Mth.sin(Body));
        BlockPos TrayPos = Doctor.syncedTray();
        boolean AtTray = TrayPos != null && Vec3.atBottomCenterOf(TrayPos).distanceToSqr(Doctor.position()) <= TrayNear * TrayNear;
        Vec3 Tray = AtTray ? Vec3.atBottomCenterOf(TrayPos).add(0.0, 0.8, 0.0) : Doctor.position().add(Forward.scale(0.9)).add(0.0, 0.8, 0.0);
        Entity Held = Doctor.syncedHeld();
        Vec3 Patient = Held != null ? Held.position() : Tray.add(Right.scale(-1.0)).add(0.0, -0.8, 0.0);
        double Height = Held != null ? Held.getBbHeight() : 1.9;
        Vec3 Chest = Doctor.position().add(Forward.scale(0.55)).add(0.0, 1.02, 0.0);
        return new Scene(Tray, Patient, Height, Chest.add(Right.scale(0.28)), Chest.add(Right.scale(-0.28)));
    }

    private static void continuous(Level World, RandomSource Random, Scene Where, int Tick) {
        int MistEvery = Tick < 50 ? 3 : 2;
        if (Tick % MistEvery == 0) {
            Vec3 Spot = Where.Patient().add((Random.nextDouble() - 0.5) * 0.5, 0.3 + Random.nextDouble() * Where.PatientHeight() * 0.8, (Random.nextDouble() - 0.5) * 0.5);
            World.addParticle(Scp049Registry.Miasma.get(), Spot.x, Spot.y, Spot.z, (Random.nextDouble() - 0.5) * 0.01, 0.012 + Random.nextDouble() * 0.01,
                    (Random.nextDouble() - 0.5) * 0.01);
        }
        if (Tick % 5 == 2) {
            Vec3 Middle = Where.Tray().add(Where.Patient().add(0.0, 1.0, 0.0)).scale(0.5);
            World.addParticle(Scp049Registry.Spore.get(), Middle.x + (Random.nextDouble() - 0.5) * 1.2, Middle.y + (Random.nextDouble() - 0.5) * 0.8,
                    Middle.z + (Random.nextDouble() - 0.5) * 1.2, (Random.nextDouble() - 0.5) * 0.01, 0.004, (Random.nextDouble() - 0.5) * 0.01);
        }
        if (Tick % GlowEvery == GlowOffset) {
            ring(World, Random, Where.Tray().add(0.0, -0.1, 0.0), 0.7, 10 + Tick / 8, 0.0);
        }
    }

    private static void beat(Level World, Scp049Doctor Doctor, RandomSource Random, Scene Where, int Tick) {
        switch (Tick) {
            case PickOne, PickTwo -> {
                play(World, Where.Tray(), Scp049Registry.Tools, 0.7f, (Tick == PickOne ? 1.5f : 1.75f) + Random.nextFloat() * 0.1f);
                glints(World, Random, Where.RightHand(), 2);
            }
            case SetOne, SetTwo -> {
                play(World, Where.Tray(), Scp049Registry.Tools, 0.6f, (Tick == SetOne ? 1.2f : 1.05f) + Random.nextFloat() * 0.1f);
                glints(World, Random, Where.Tray(), 1);
            }
            case SnipOne, SnipTwo, SnipThree -> {
                play(World, Where.RightHand(), Scp049Registry.Snip, 0.75f, 0.95f + Random.nextFloat() * 0.25f);
                glints(World, Random, Where.RightHand(), 4);
            }
            case SweepOne, SweepTwo -> {
                play(World, Where.LeftHand(), Scp049Registry.Sweep, 0.45f, 0.5f + Random.nextFloat() * 0.08f);
                Vec3 Toward = Where.Patient().add(0.0, Where.PatientHeight() * 0.6, 0.0).subtract(Where.Tray()).scale(0.04);
                for (int Index = 0; Index < 7; Index++) {
                    World.addParticle(Scp049Registry.Spore.get(), Where.Tray().x + (Random.nextDouble() - 0.5) * 0.4, Where.Tray().y + Random.nextDouble() * 0.3,
                            Where.Tray().z + (Random.nextDouble() - 0.5) * 0.4, Toward.x, Toward.y + 0.01, Toward.z);
                }
            }
            case Uncork -> {
                play(World, Where.Tray(), Scp049Registry.Uncork, 0.7f, 0.95f + Random.nextFloat() * 0.1f);
                glints(World, Random, Where.LeftHand(), 1);
            }
            case Pour -> {
                play(World, Where.Tray(), Scp049Registry.Pour, 0.7f, 0.9f + Random.nextFloat() * 0.1f);
                for (int Index = 0; Index < 4; Index++) {
                    World.addParticle(Scp049Registry.Miasma.get(), Where.Tray().x + (Random.nextDouble() - 0.5) * 0.3, Where.Tray().y + 0.1, Where.Tray().z + (Random.nextDouble() - 0.5) * 0.3,
                            0.0, 0.02, 0.0);
                }
            }
            case Brew -> {
                play(World, Where.Tray(), Scp049Registry.Brew, 0.55f, 0.85f + Random.nextFloat() * 0.1f);
                for (int Index = 0; Index < 3; Index++) {
                    World.addParticle(Scp049Registry.Glow.get(), Where.Tray().x + (Random.nextDouble() - 0.5) * 0.25, Where.Tray().y + 0.05, Where.Tray().z + (Random.nextDouble() - 0.5) * 0.25,
                            0.0, 0.03 + Random.nextDouble() * 0.02, 0.0);
                }
            }
            case HumStart, ChantStart -> {
                Vec3 Mouth = Doctor.position().add(0.0, Doctor.getEyeHeight(), 0.0);
                play(World, Mouth, Scp049Registry.Hum, Tick == HumStart ? 0.8f : 0.7f, 0.95f + Random.nextFloat() * 0.08f);
                play(World, Mouth, Scp049Registry.Chant, Tick == HumStart ? 0.5f : 0.7f, 0.92f + Random.nextFloat() * 0.08f);
            }
            case Stinger -> {
                play(World, Where.Patient(), Scp049Registry.Stinger, 1.0f, 1.0f);
                play(World, Where.Patient(), Scp049Registry.StingerLow, 0.9f, 1.0f);
            }
            case Burst -> {
                Vec3 Core = Where.Patient().add(0.0, Where.PatientHeight() * 0.55, 0.0);
                for (int Index = 0; Index < 26; Index++) {
                    double Angle = Random.nextDouble() * Mth.TWO_PI;
                    double Lift = (Random.nextDouble() - 0.3) * 0.12;
                    double Speed = 0.06 + Random.nextDouble() * 0.08;
                    World.addParticle(Scp049Registry.CureSpore.get(), Core.x, Core.y, Core.z, Math.cos(Angle) * Speed, Lift, Math.sin(Angle) * Speed);
                }
                ring(World, Random, Where.Patient().add(0.0, 0.1, 0.0), 0.3, 22, 0.09);
            }
            default -> {
                for (int Index = 0; Index < Tones.length; Index++) {
                    if (Tones[Index] == Tick) {
                        play(World, Where.Tray(), Scp049Registry.Tone, 0.4f + 0.13f * Index, TonePitch[Index]);
                        ring(World, Random, Where.Tray().add(0.0, -0.05, 0.0), 0.45 + 0.1 * Index, 8 + 2 * Index, 0.0);
                    }
                }
            }
        }
    }

    private static void glints(Level World, RandomSource Random, Vec3 At, int Count) {
        for (int Index = 0; Index < Count; Index++) {
            World.addParticle(Scp049Registry.Glint.get(), At.x + (Random.nextDouble() - 0.5) * 0.2, At.y + (Random.nextDouble() - 0.5) * 0.2, At.z + (Random.nextDouble() - 0.5) * 0.2,
                    (Random.nextDouble() - 0.5) * 0.04, Random.nextDouble() * 0.03, (Random.nextDouble() - 0.5) * 0.04);
        }
    }

    public static void ring(Level World, RandomSource Random, Vec3 Centre, double Radius, int Count, double Spread) {
        ParticleOptions Kind = Scp049Registry.Glow.get();
        double Offset = Random.nextDouble() * Mth.TWO_PI;
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Offset + Mth.TWO_PI * Index / Count;
            double Cos = Math.cos(Angle);
            double Sin = Math.sin(Angle);
            World.addParticle(Kind, Centre.x + Cos * Radius, Centre.y, Centre.z + Sin * Radius, Cos * Spread, 0.002, Sin * Spread);
        }
    }

    private static void play(Level World, Vec3 At, Supplier<SoundEvent> Sound, float Volume, float Pitch) {
        World.playLocalSound(At.x, At.y, At.z, Sound.get(), SoundSource.HOSTILE, Volume, Pitch, false);
    }
}
