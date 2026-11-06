package net.rai.scp_unleashed.marksman;

import net.minecraft.sounds.SoundEvent;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunsRegistry;

import java.util.List;
import java.util.function.Supplier;

public final class CoinSounds {
    public static final float RisePerLink = 0.07F;
    public static final float PitchCeiling = 2.0F;

    private CoinSounds() {
    }

    private static GunSounds.Layer layer(Supplier<SoundEvent> Sound, float Volume, float Pitch, int Delay) {
        return new GunSounds.Layer(Sound, Volume, Math.min(PitchCeiling, Pitch), Delay, false);
    }

    public static List<GunSounds.Layer> flip() {
        return List.of(layer(GunsRegistry.MechanismClick, 0.35F, 2.0F, 0), layer(MarksmanRegistry.CoinFlick, 0.8F, 1.5F, 0), layer(MarksmanRegistry.CoinChime, 1.0F, 1.8F, 1),
                layer(MarksmanRegistry.CoinFlick, 0.35F, 1.9F, 3));
    }

    public static List<GunSounds.Layer> land() {
        return List.of(layer(MarksmanRegistry.CoinLand, 0.6F, 1.7F, 0), layer(MarksmanRegistry.CoinFlick, 0.3F, 2.0F, 1));
    }

    public static List<GunSounds.Layer> fade() {
        return List.of(layer(MarksmanRegistry.CoinChime, 0.7F, 2.0F, 0));
    }

    public static List<GunSounds.Layer> empty() {
        return List.of(layer(GunsRegistry.MechanismClick, 0.5F, 1.3F, 0));
    }

    public static List<GunSounds.Layer> ricochet(int Link) {
        float Rise = 1.0F + RisePerLink * Link;
        return List.of(layer(MarksmanRegistry.RicochetPing, 1.0F, 1.7F * Rise, Link), layer(MarksmanRegistry.RicochetClang, 0.15F, 2.0F, Link),
                layer(MarksmanRegistry.RicochetZing, 0.3F, 1.4F * Rise, Link), layer(MarksmanRegistry.CoinChime, 1.0F, 1.6F * Rise, Link));
    }
}
