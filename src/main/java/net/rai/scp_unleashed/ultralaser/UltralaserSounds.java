package net.rai.scp_unleashed.ultralaser;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunSounds;

import java.util.List;
import java.util.function.Supplier;

public final class UltralaserSounds {
    public static final int BellCentre = 90;
    public static final float PrimingVolume = 0.8F;
    public static final List<GunSounds.Layer> PowerUp = List.of(layer(UltralaserRegistry.PowerUp, 0.35F, 1.4F, 0));
    public static final List<GunSounds.Layer> PowerDown = List.of(layer(UltralaserRegistry.PowerDown, 0.7F, 1.3F, 0), layer(UltralaserRegistry.Vent, 0.3F, 1.4F, 2));
    public static final List<GunSounds.Layer> Blast = List.of(layer(UltralaserRegistry.BlastBoom, 0.9F, 1.2F, 0), layer(UltralaserRegistry.BlastThunder, 0.45F, 1.7F, 0),
            layer(UltralaserRegistry.BlastFirework, 0.9F, 0.8F, 0), layer(UltralaserRegistry.BlastExplode, 0.75F, 1.2F, 0), layer(UltralaserRegistry.BlastBeacon, 0.5F, 1.9F, 0),
            layer(UltralaserRegistry.BlastExplode, 0.6F, 1.4F, 4), layer(UltralaserRegistry.BlastTwinkle, 0.7F, 1.2F, 6), layer(UltralaserRegistry.BlastFirework, 0.6F, 1.1F, 11),
            layer(UltralaserRegistry.BlastTwinkle, 0.5F, 1.4F, 14),
            sparkle("A6", 0.55F, 3), sparkle("E6", 0.5F, 6), sparkle("C#6", 0.45F, 9), sparkle("A5", 0.4F, 12), sparkle("E6", 0.3F, 15));
    public static final List<GunSounds.Layer> Impact = List.of(layer(UltralaserRegistry.Impact, 0.9F, 0.75F, 0), layer(UltralaserRegistry.BlastTwinkle, 0.6F, 0.9F, 4));
    public static final List<GunSounds.Layer> Vent = List.of(layer(UltralaserRegistry.Vent, 0.8F, 0.6F, 0), layer(UltralaserRegistry.VentGust, 0.5F, 1.5F, 0),
            layer(UltralaserRegistry.Vent, 0.6F, 0.75F, 16), layer(UltralaserRegistry.Vent, 0.45F, 0.9F, 34));
    public static final List<GunSounds.Layer> Seat = List.of(layer(UltralaserRegistry.Seat, 0.6F, 1.6F, 0), layer(UltralaserRegistry.SeatChime, 0.8F, 1.3F, 1));

    private UltralaserSounds() {
    }

    public static GunSounds.Layer layer(Supplier<SoundEvent> Sound, float Volume, float Pitch, int Delay) {
        return new GunSounds.Layer(Sound, Volume, Pitch, Delay, false);
    }

    private static GunSounds.Layer sparkle(String Name, float Volume, int Delay) {
        return layer(UltralaserRegistry.Bell, Volume, bellPitch(Name), Delay);
    }

    public static void play(ServerLevel World, Vec3 At, List<GunSounds.Layer> Layers) {
        GunSounds.play(World, null, At.x, At.y, At.z, Layers, 1.0F, 0.0F);
    }

    public static void ready(ServerLevel World, Vec3 At) {
        World.playSound(null, At.x, At.y, At.z, UltralaserRegistry.Bell.get(), SoundSource.PLAYERS, 0.5F, bellPitch("E6"));
        World.playSound(null, At.x, At.y, At.z, UltralaserRegistry.Bell.get(), SoundSource.PLAYERS, 0.55F, bellPitch("A6"));
    }

    public static float bellPitch(String Name) {
        return (float) Math.pow(2.0, (midi(Name) - BellCentre) / 12.0);
    }

    public static int midi(String Name) {
        int Index = 0;
        int Semitone = switch (Name.charAt(Index++)) {
            case 'C' -> 0;
            case 'D' -> 2;
            case 'E' -> 4;
            case 'F' -> 5;
            case 'G' -> 7;
            case 'A' -> 9;
            case 'B' -> 11;
            default -> throw new IllegalArgumentException("Unknown note " + Name);
        };
        if (Name.charAt(Index) == '#') {
            Semitone++;
            Index++;
        }
        return 12 * (Integer.parseInt(Name.substring(Index)) + 1) + Semitone;
    }
}
