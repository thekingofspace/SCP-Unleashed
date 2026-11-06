package net.rai.scp_unleashed.scp539.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp539.Scp539Helicopter;
import net.rai.scp_unleashed.scp539.Scp539Registry;

import java.util.HashSet;
import java.util.Set;

public final class Scp539HelicopterSounds {
    public static final float Volume = 2.2f;
    private static final Set<Integer> Heard = new HashSet<>();

    private Scp539HelicopterSounds() {
    }

    public static void heard(Scp539Helicopter Craft) {
        if (!Heard.add(Craft.getId())) {
            return;
        }
        if (Heard.size() > 64) {
            Heard.clear();
            Heard.add(Craft.getId());
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp539Registry.ArrivalLayers) {
            Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(Layer.get(), SoundSource.HOSTILE, Volume, 1.0f, Craft, Craft.getId()));
        }
    }

    public static void reset() {
        Heard.clear();
    }
}
