package net.rai.scp_unleashed.guns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GunClientSounds {
    private static final List<Pending> Queue = new ArrayList<>();
    private static final RandomSource Random = RandomSource.create();

    private GunClientSounds() {
    }

    private record Pending(SoundEvent Sound, float Volume, float Pitch, long Due) {
    }

    public static void play(List<GunSounds.Layer> Layers, float Scale, float Jitter) {
        float Shift = 1.0F + (Random.nextFloat() * 2.0F - 1.0F) * Jitter;
        long Now = GunClientState.ticks();
        for (GunSounds.Layer Part : Layers) {
            float Volume = Part.Volume() * Scale;
            float Pitch = Part.Pitch() * Shift;
            if (Part.Delay() <= 0) {
                emit(Part.Sound().get(), Volume, Pitch);
            } else {
                Queue.add(new Pending(Part.Sound().get(), Volume, Pitch, Now + Part.Delay()));
            }
        }
    }

    public static void cue(int Cue, int KindIndex, float Scale) {
        play(GunSounds.cue(Cue, GunKind.byIndex(KindIndex)), Scale, GunSounds.PitchJitter);
    }

    public static void tick() {
        long Now = GunClientState.ticks();
        List<Pending> Ready = new ArrayList<>();
        Iterator<Pending> Entries = Queue.iterator();
        while (Entries.hasNext()) {
            Pending Entry = Entries.next();
            if (Now >= Entry.Due()) {
                Ready.add(Entry);
                Entries.remove();
            }
        }
        for (Pending Entry : Ready) {
            emit(Entry.Sound(), Entry.Volume(), Entry.Pitch());
        }
    }

    public static void clear() {
        Queue.clear();
    }

    private static void emit(SoundEvent Sound, float Volume, float Pitch) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(Sound.getLocation(), SoundSource.PLAYERS, Volume, Pitch, SoundInstance.createUnseededRandom(),
                false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }
}
