package net.rai.scp_unleashed.scp096.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp096.Scp096ShyGuy;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class Scp096Voices {
    public static final int CryFadeIn = 20;
    public static final int CryFadeOut = 30;
    public static final int RageFadeIn = 4;
    public static final int RageFadeOut = 12;
    public static final int ChargeFadeOut = 8;
    public static final int ChargeLate = 10;
    public static final int TailGap = 50;
    public static final int TailCount = 3;
    public static final double ChargeRangeMin = 24.0;
    public static final double ChargeRangeMax = 64.0;
    public static final float Mouth = 0.8f;
    private static final Map<Integer, Voice> Crying = new HashMap<>();
    private static final Map<Integer, Voice> Raging = new HashMap<>();
    private static final Map<Integer, Voice> Charging = new HashMap<>();
    private static final Map<Integer, Integer> Voiced = new HashMap<>();
    @Nullable
    private static ClientLevel Known;

    private Scp096Voices() {
    }

    public static void tick(Minecraft Client) {
        ClientLevel World = Client.level;
        if (World != Known) {
            Crying.clear();
            Raging.clear();
            Charging.clear();
            Voiced.clear();
            Known = World;
        }
        if (World == null) {
            return;
        }
        SoundManager Sounds = Client.getSoundManager();
        boolean Audible = Client.options.getSoundSourceVolume(SoundSource.HOSTILE) > 0.0f && Client.options.getSoundSourceVolume(SoundSource.MASTER) > 0.0f;
        Set<Integer> Present = new HashSet<>();
        for (Entity Each : World.entitiesForRendering()) {
            if (!(Each instanceof Scp096ShyGuy Guy) || !Guy.isAlive()) {
                continue;
            }
            Present.add(Guy.getId());
            drive(Sounds, Crying, Guy, Guy.isCrying(), Scp096Registry.Crying.get(), CryFadeIn, CryFadeOut, Audible);
            drive(Sounds, Raging, Guy, Guy.isEnraged(), Scp096Registry.Enraged.get(), RageFadeIn, RageFadeOut, Audible);
            charge(Sounds, Guy, Audible);
        }
        prune(Sounds, Crying, Present);
        prune(Sounds, Raging, Present);
        prune(Sounds, Charging, Present);
        Voiced.keySet().retainAll(Present);
    }

    private static boolean alive(SoundManager Sounds, @Nullable Voice Current) {
        return Current != null && !Current.isStopped() && Sounds.isActive(Current);
    }

    private static void drive(SoundManager Sounds, Map<Integer, Voice> Voices, Scp096ShyGuy Guy, boolean Wanted, SoundEvent Event, int FadeIn, int FadeOut, boolean Audible) {
        Voice Current = Voices.get(Guy.getId());
        if (Current != null && !alive(Sounds, Current)) {
            Voices.remove(Guy.getId());
            Current = null;
        }
        if (Current != null) {
            Current.want(Wanted);
            return;
        }
        if (Wanted && Audible) {
            Voice Fresh = new Voice(Guy, Event, true, FadeIn, FadeOut, 0.0f);
            Sounds.play(Fresh);
            if (Sounds.isActive(Fresh)) {
                Voices.put(Guy.getId(), Fresh);
            }
        }
    }

    private static void charge(SoundManager Sounds, Scp096ShyGuy Guy, boolean Audible) {
        int Id = Guy.getId();
        Voice Current = Charging.get(Id);
        if (Current != null && !alive(Sounds, Current)) {
            Charging.remove(Id);
            Current = null;
        }
        if (Guy.isDistressed()) {
            Integer Done = Voiced.get(Id);
            int Key = Guy.chargeKey();
            SoundEvent Entry = Done != null && Done == Key ? null : entryFor(Guy.stateElapsed());
            if (Entry != null && Audible) {
                Voiced.put(Id, Key);
                if (Current != null) {
                    Current.end();
                }
                Voice Fresh = new Voice(Guy, Entry, false, 1, ChargeFadeOut, 1.0f).swelling();
                Sounds.play(Fresh);
                if (Sounds.isActive(Fresh)) {
                    Charging.put(Id, Fresh);
                }
            }
        } else if (Current != null && !Guy.isRaging()) {
            Current.want(false);
        }
    }

    @Nullable
    private static SoundEvent entryFor(double Elapsed) {
        if (Elapsed <= ChargeLate) {
            return Scp096Registry.Triggered.get();
        }
        for (int Index = TailCount; Index >= 1; Index--) {
            int From = TailGap * Index;
            if (Elapsed >= From && Elapsed <= From + ChargeLate) {
                return switch (Index) {
                    case 1 -> Scp096Registry.TriggeredFrom50.get();
                    case 2 -> Scp096Registry.TriggeredFrom100.get();
                    default -> Scp096Registry.TriggeredFrom150.get();
                };
            }
        }
        return null;
    }

    private static void prune(SoundManager Sounds, Map<Integer, Voice> Voices, Set<Integer> Present) {
        Iterator<Map.Entry<Integer, Voice>> Each = Voices.entrySet().iterator();
        while (Each.hasNext()) {
            Map.Entry<Integer, Voice> Entry = Each.next();
            if (!Present.contains(Entry.getKey())) {
                Entry.getValue().end();
                Sounds.stop(Entry.getValue());
                Each.remove();
            }
        }
    }

    public static final class Voice extends AbstractTickableSoundInstance {
        private final Scp096ShyGuy Guy;
        private final int FadeIn;
        private final int FadeOut;
        private float Level;
        private boolean Wanted = true;
        private boolean Swell;

        public Voice(Scp096ShyGuy Owner, SoundEvent Event, boolean Loop, int FadeIn, int FadeOut, float Start) {
            super(Event, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.Guy = Owner;
            this.FadeIn = Math.max(1, FadeIn);
            this.FadeOut = Math.max(1, FadeOut);
            this.looping = Loop;
            this.delay = 0;
            this.Level = Start;
            this.volume = Math.max(Start, 0.001f);
            this.pitch = 1.0f;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.relative = false;
            follow();
        }

        public Voice swelling() {
            Swell = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = Math.max(Level * loudness(), 0.001f);
            return this;
        }

        private float loudness() {
            float Gain = Guy.isDistressed() ? Guy.chargeGain() : 1.0f;
            double Share = (Gain - Scp096ShyGuy.ChargeQuiet) / (1.0 - Scp096ShyGuy.ChargeQuiet);
            double Range = ChargeRangeMin + (ChargeRangeMax - ChargeRangeMin) * Mth.clamp(Share, 0.0, 1.0);
            Vec3 Ear = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            double Away = Ear.distanceTo(new Vec3(x, y, z));
            return Gain * (float) Math.max(0.0, 1.0 - Away / Range);
        }

        public void want(boolean Value) {
            Wanted = Value;
        }

        public void end() {
            stop();
        }

        private void follow() {
            x = Guy.getX();
            y = Guy.getY() + Guy.getBbHeight() * Mouth;
            z = Guy.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (Guy.isRemoved() || !Guy.isAlive() || Guy.level() != Minecraft.getInstance().level) {
                stop();
                return;
            }
            Level = Wanted ? Math.min(1.0f, Level + 1.0f / FadeIn) : Math.max(0.0f, Level - 1.0f / FadeOut);
            if (!Wanted && Level <= 0.0f) {
                stop();
                return;
            }
            follow();
            volume = Math.max(Swell ? Level * loudness() : Level, 0.001f);
        }
    }
}
