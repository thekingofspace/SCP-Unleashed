package net.rai.scp_unleashed.flamethrower;

import net.minecraft.sounds.SoundEvent;
import net.rai.scp_unleashed.guns.GunSounds;

import java.util.List;
import java.util.function.Supplier;

public final class FlamerSounds {
    private FlamerSounds() {
    }

    private static GunSounds.Layer layer(Supplier<SoundEvent> Sound, float Volume, float Pitch, int Delay) {
        return new GunSounds.Layer(Sound, Volume, Pitch, Delay, false);
    }

    public static List<GunSounds.Layer> ignite() {
        return List.of(layer(FlamerRegistry.Ignite, 0.9F, 1.0F, 0), layer(FlamerRegistry.Hiss, 0.45F, 1.0F, 0), layer(FlamerRegistry.Whoosh, 0.7F, 1.0F, 1));
    }

    public static List<GunSounds.Layer> sputter() {
        return List.of(layer(FlamerRegistry.Click, 0.55F, 1.0F, 0), layer(FlamerRegistry.Sputter, 0.7F, 1.0F, 1), layer(FlamerRegistry.Pop, 0.45F, 1.0F, 3),
                layer(FlamerRegistry.Pop, 0.35F, 0.85F, 7));
    }

    public static List<GunSounds.Layer> douse() {
        return List.of(layer(FlamerRegistry.Douse, 0.7F, 1.0F, 0), layer(FlamerRegistry.Click, 0.4F, 0.9F, 0));
    }

    public static List<GunSounds.Layer> refuelStart() {
        return List.of(layer(FlamerRegistry.Click, 0.5F, 0.8F, 0), layer(FlamerRegistry.CapOpen, 0.55F, 1.0F, 3), layer(FlamerRegistry.Glug, 0.6F, 1.0F, 12),
                layer(FlamerRegistry.Glug, 0.5F, 0.9F, 24));
    }

    public static List<GunSounds.Layer> refuelFinish() {
        return List.of(layer(FlamerRegistry.CapClose, 0.6F, 1.0F, 0), layer(FlamerRegistry.Click, 0.5F, 1.1F, 2), layer(FlamerRegistry.Hiss, 0.3F, 1.2F, 4));
    }
}
