package net.rai.scp_unleashed.guns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunSoundLengths;
import net.rai.scp_unleashed.guns.GunVoiceBudget;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GunVoices {
    public static final float LocalBoost = 2.0F;
    private static final GunVoiceBudget<SoundInstance> Budget = new GunVoiceBudget<>(GunVoiceBudget.ClientLimit);
    private static final List<SoundInstance> Stolen = new ArrayList<>();

    private GunVoices() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void play(PlaySoundEvent Event) {
        SoundInstance Played = Event.getSound();
        if (Played == null || !GunVoiceBudget.managed(Played.getLocation())) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        SoundManager Speakers = Game.getSoundManager();
        if (Played.resolve(Speakers) == null || Played.getSound() == null || Played.getSound() == SoundManager.EMPTY_SOUND) {
            return;
        }
        Stolen.clear();
        long Now = GunClientState.ticks();
        if (!Budget.offer(Played, gain(Game, Played), Now, GunSoundLengths.ticks(Played.getLocation(), Played.getPitch()), Stolen)) {
            Event.setSound(null);
            return;
        }
        for (SoundInstance Victim : Stolen) {
            Speakers.stop(Victim);
        }
        Stolen.clear();
    }

    private static float gain(Minecraft Game, SoundInstance Played) {
        float Volume = Played.getVolume();
        if (Played.isRelative() || Played.getAttenuation() == SoundInstance.Attenuation.NONE) {
            return Volume * LocalBoost;
        }
        Sound Variant = Played.getSound();
        double Reach = Math.max(1.0, Math.max(Volume, 1.0F) * Variant.getAttenuationDistance());
        Vec3 Ear = Game.gameRenderer.getMainCamera().getPosition();
        double Dx = Played.getX() - Ear.x;
        double Dy = Played.getY() - Ear.y;
        double Dz = Played.getZ() - Ear.z;
        double Falloff = 1.0 - Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz) / Reach;
        return (float) (Volume * Mth.clamp(Falloff, 0.0, 1.0));
    }

    public static void clear() {
        Budget.clear();
        Stolen.clear();
    }
}
