package net.rai.scp_unleashed.warhead.client;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.warhead.WarheadRegistry;

import java.util.ArrayList;
import java.util.List;

public final class WarheadSiren {
    public static final int Cycle = 140;
    public static final int CycleLocked = 60;
    public static final float Rise = 0.4f;
    public static final float Low = 0.98f;
    public static final float High = 1.92f;
    public static final float LowLocked = 1.4f;
    public static final float HighLocked = 1.92f;
    public static final float Floor = 0.55f;
    public static final int LockTicks = 20;
    public static final int FadeInTicks = 30;
    public static final int ReleaseTicks = 6;
    public static final int KlaxonPeriod = 28;
    public static final int KlaxonPeriodLocked = 14;
    public static final int KlaxonOn = 16;
    public static final int KlaxonOnLocked = 8;
    public static final int KlaxonAttack = 2;
    public static final int KlaxonRelease = 4;
    public static final float KlaxonDroop = 0.03f;
    public static final int HitLife = 16;
    public static final float RaspGain = 0.4f;
    public static final float EdgeGain = 0.5f;

    private static final List<WarheadSirenChain> Wail = List.of(
            new WarheadSirenChain(WarheadRegistry.Siren, 1.0f, 2.2f, 4.65f, 0.6f, 0.75f),
            new WarheadSirenChain(WarheadRegistry.SirenChorus, 1.035f, 1.15f, 3.4f, 0.6f, 0.3f),
            new WarheadSirenChain(WarheadRegistry.SirenThird, 0.893f, 1.0f, 2.22f, 0.4f, 0.65f));
    private static final WarheadSirenChain Klaxon = new WarheadSirenChain(WarheadRegistry.SirenLow, 1.0f, 0.8f, 3.45f, 0.5f, 0.45f);
    private static final List<Hit> Hits = new ArrayList<>();

    private static boolean Playing;
    private static float Phase;
    private static float KlaxonPhase;
    private static float Blend;
    private static float Fade;
    private static int Age;

    private static final class Hit {
        private final WarheadSirenSound Sound;
        private final float Gain;
        private int Age;

        private Hit(WarheadSirenSound Sound, float Gain) {
            this.Sound = Sound;
            this.Gain = Gain;
        }
    }

    private WarheadSiren() {
    }

    public static boolean isPlaying() {
        return Playing;
    }

    public static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static float wail(float Progress) {
        if (Progress < Rise) {
            return smooth(Progress / Rise);
        }
        return 1.0f - smooth((Progress - Rise) / (1.0f - Rise));
    }

    public static float klaxon(float Ticks, float On) {
        return Mth.clamp(Ticks / KlaxonAttack, 0.0f, 1.0f) * Mth.clamp((On - Ticks) / KlaxonRelease, 0.0f, 1.0f);
    }

    public static void tick(SoundManager Manager, boolean Running, boolean Locked, float Level) {
        if (!Running && !Playing) {
            return;
        }
        if (Running && !Playing) {
            Playing = true;
            Phase = 0.0f;
            KlaxonPhase = 0.0f;
            Blend = Locked ? 1.0f : 0.0f;
            Age = 0;
        }
        Fade = Running ? 1.0f : Fade - 1.0f / ReleaseTicks;
        if (Fade <= 0.0f) {
            stop();
            return;
        }
        Blend = Mth.clamp(Blend + (Locked ? 1.0f : -1.0f) / LockTicks, 0.0f, 1.0f);
        Age++;
        float Start = Mth.clamp(Age / (float) FadeInTicks, 0.0f, 1.0f);
        float Shape = wail(Phase);
        float Bottom = Mth.lerp(Blend, Low, LowLocked);
        float Top = Mth.lerp(Blend, High, HighLocked);
        float Pitch = Bottom + (Top - Bottom) * Shape;
        float Volume = Level * Fade * Start * (Floor + (1.0f - Floor) * Shape);
        for (WarheadSirenChain Layer : Wail) {
            Layer.tick(Manager, Running, Pitch, Volume);
        }
        float Period = Mth.lerp(Blend, KlaxonPeriod, KlaxonPeriodLocked);
        float On = Mth.lerp(Blend, KlaxonOn, KlaxonOnLocked);
        float Into = KlaxonPhase * Period;
        if (Running && KlaxonPhase == 0.0f) {
            strike(Manager, WarheadRegistry.KlaxonRasp.get(), RaspGain);
            strike(Manager, WarheadRegistry.KlaxonEdge.get(), EdgeGain);
        }
        Klaxon.tick(Manager, Running, 1.0f - KlaxonDroop * Mth.clamp(Into / On, 0.0f, 1.0f), Level * Fade * Start * klaxon(Into, On));
        Hits.removeIf(Item -> {
            Item.Sound.drive(1.0f, Item.Gain * Level * Fade);
            if (++Item.Age > HitLife) {
                Item.Sound.finish();
                return true;
            }
            return false;
        });
        Phase += 1.0f / Mth.lerp(Blend, Cycle, CycleLocked);
        if (Phase >= 1.0f) {
            Phase -= 1.0f;
        }
        KlaxonPhase += 1.0f / Period;
        if (KlaxonPhase >= 0.999f) {
            KlaxonPhase = 0.0f;
        }
    }

    private static void strike(SoundManager Manager, SoundEvent Event, float Gain) {
        WarheadSirenSound Sound = new WarheadSirenSound(Event, 1.0f, Gain);
        Manager.play(Sound);
        Hits.add(new Hit(Sound, Gain));
    }

    public static int voices() {
        int Count = Klaxon.voices() + Hits.size();
        for (WarheadSirenChain Layer : Wail) {
            Count += Layer.voices();
        }
        return Count;
    }

    public static void stop() {
        for (WarheadSirenChain Layer : Wail) {
            Layer.stop();
        }
        Klaxon.stop();
        for (Hit Item : Hits) {
            Item.Sound.finish();
        }
        Hits.clear();
        Playing = false;
        Fade = 0.0f;
    }
}
