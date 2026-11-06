package net.rai.scp_unleashed.scp127;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class Scp127Sounds {
    public static final float Jitter = 0.05F;
    public static final List<GunSounds.Layer> Purr = List.of(layer(Scp127Registry.Purr, 1.0F, 1.0F, 0), layer(Scp127Registry.Purr, 0.9F, 1.06F, 1),
            layer(Scp127Registry.Purr, 0.9F, 0.95F, 3), layer(Scp127Registry.PurrBody, 0.8F, 1.0F, 2));
    public static final List<GunSounds.Layer> Whine = List.of(layer(Scp127Registry.Whine, 0.6F, 1.0F, 0), layer(Scp127Registry.WhineSniff, 0.32F, 1.0F, 14));
    public static final List<GunSounds.Layer> LevelUp = List.of(layer(Scp127Registry.LevelUp, 0.75F, 1.0F, 0), layer(Scp127Registry.LevelUpSurge, 0.7F, 1.0F, 0),
            layer(Scp127Registry.LevelUpHum, 0.5F, 1.0F, 2), layer(Scp127Registry.Purr, 1.0F, 0.94F, 22), layer(Scp127Registry.Purr, 0.8F, 1.0F, 23),
            layer(Scp127Registry.PurrBody, 0.4F, 0.94F, 24));

    private Scp127Sounds() {
    }

    public static GunSounds.Layer layer(Supplier<SoundEvent> Sound, float Volume, float Pitch, int Delay) {
        return new GunSounds.Layer(Sound, Volume, Pitch, Delay, false);
    }

    public static List<GunSounds.Layer> delayed(List<GunSounds.Layer> Layers, int Ticks) {
        List<GunSounds.Layer> Shifted = new ArrayList<>();
        for (GunSounds.Layer Part : Layers) {
            Shifted.add(new GunSounds.Layer(Part.Sound(), Part.Volume(), Part.Pitch(), Part.Delay() + Ticks, Part.Tail()));
        }
        return Shifted;
    }

    public static void play(ServerLevel World, Vec3 At, List<GunSounds.Layer> Layers) {
        GunSounds.play(World, null, At.x, At.y, At.z, Layers, 1.0F, Jitter);
    }
}
