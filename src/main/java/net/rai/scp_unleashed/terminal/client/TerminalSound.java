package net.rai.scp_unleashed.terminal.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.terminal.TerminalSettings;

public final class TerminalSound {
    public static final ResourceLocation Hum = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "terminal_hum");
    public static final ResourceLocation Fan = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "terminal_hum_fan");
    public static final ResourceLocation Key = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "terminal_key");
    public static final ResourceLocation Beep = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "terminal_beep");
    public static final float HumVolume = 0.35f;
    public static final float FanVolume = 0.3f;
    public static final float KeyVolume = 0.55f;
    public static final float BeepVolume = 0.5f;
    public static final int KeyGapMs = 45;
    public static final int CharsPerKey = 3;
    public static final long StaleMs = 300L;
    public static final int FadeInTicks = 10;
    public static final int FadeOutTicks = 6;

    private static final RandomSource Noise = RandomSource.create();
    private static long LastFrame = -StaleMs * 2L;
    private static int Pending;
    private static long LastKey;
    private static Loop HumLoop;
    private static Loop FanLoop;

    private TerminalSound() {
    }

    public static void frame(long Now) {
        LastFrame = Now;
        if (!TerminalSettings.sounds()) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        if (HumLoop == null || HumLoop.isStopped() || !Game.getSoundManager().isActive(HumLoop)) {
            HumLoop = new Loop(Hum, HumVolume);
            Game.getSoundManager().play(HumLoop);
        }
        if (FanLoop == null || FanLoop.isStopped() || !Game.getSoundManager().isActive(FanLoop)) {
            FanLoop = new Loop(Fan, FanVolume);
            Game.getSoundManager().play(FanLoop);
        }
    }

    public static void typed(int Chars, long Now) {
        if (Chars <= 0 || !TerminalSettings.sounds()) {
            return;
        }
        Pending += Chars;
        if (Pending < CharsPerKey || Now - LastKey < KeyGapMs) {
            return;
        }
        Pending = 0;
        LastKey = Now;
        float Pitch = 0.85f + Noise.nextFloat() * 0.3f;
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(Key, SoundSource.MASTER, KeyVolume, Pitch, Noise, false, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }

    public static void beep() {
        if (!TerminalSettings.sounds()) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(Beep, SoundSource.MASTER, BeepVolume, 1.0f, Noise, false, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }

    static boolean alive() {
        return Util.getMillis() - LastFrame < StaleMs && TerminalSettings.sounds();
    }

    public static final class Loop extends AbstractTickableSoundInstance {
        private final float Target;

        Loop(ResourceLocation Id, float NewTarget) {
            super(SoundEvent.createVariableRangeEvent(Id), SoundSource.AMBIENT, RandomSource.create());
            Target = NewTarget;
            looping = true;
            delay = 0;
            volume = 0.0f;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            x = 0.0;
            y = 0.0;
            z = 0.0;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (alive()) {
                volume = Math.min(Target, volume + Target / FadeInTicks);
                return;
            }
            volume -= Target / FadeOutTicks;
            if (volume <= 0.0f) {
                volume = 0.0f;
                stop();
            }
        }
    }
}
