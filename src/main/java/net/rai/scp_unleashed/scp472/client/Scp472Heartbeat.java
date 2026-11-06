package net.rai.scp_unleashed.scp472.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp472.Scp472BloodstoneItem;
import net.rai.scp_unleashed.scp472.Scp472Mind;
import net.rai.scp_unleashed.scp472.Scp472Registry;
import net.rai.scp_unleashed.scp472.Scp472Stones;
import net.rai.scp_unleashed.senses.Senses;

import java.util.List;

public final class Scp472Heartbeat {
    public static final float EdgeVolume = 0.05f;
    public static final float NearVolume = 0.35f;
    public static final float[] StageGain = {1.0f, 1.0f, 1.35f, 1.7f, 1.9f, 2.1f, 2.4f};
    public static final float[] LubShares = {1.0f, 0.6f, 0.8f};
    public static final float[] DubShares = {0.7f, 0.5f};
    public static final float DubPitch = 1.15f;
    public static final float MaxVolume = 0.9f;
    public static final int[] WhisperMin = {0, 0, 0, 0, 160, 100, 50};
    public static final int[] WhisperMax = {0, 0, 0, 0, 300, 200, 120};
    public static final float[] WhisperVolume = {0.0f, 0.0f, 0.0f, 0.0f, 0.22f, 0.32f, 0.45f};
    public static final float[] WhisperShares = {1.0f, 0.55f, 0.35f};
    public static final double WhisperNear = 1.5;
    public static final double WhisperFar = 3.5;
    public static final int HeldInterval = 44;
    public static final float HeldVolume = 0.12f;
    public static final float HeldPitch = 0.92f;
    private static int NextWhisper;
    private static int HeldAge;

    private Scp472Heartbeat() {
    }

    public static float volume(double Distance, int Stage) {
        float Proximity = 1.0f - (float) Mth.clamp(Distance / Scp472Mind.HeardRange, 0.0, 1.0);
        float Base = EdgeVolume + (NearVolume - EdgeVolume) * Proximity;
        return Math.min(MaxVolume, Base * StageGain[Mth.clamp(Stage, 0, StageGain.length - 1)]);
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null || Client.level == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        boolean Lub = Scp472ClientState.beat(Client);
        Scp472Stones.Nearest Heard = Scp472ClientState.heard();
        float Muffle = Senses.isDeafened(User) ? Senses.MuffledShare : 1.0f;
        int Stage = Scp472ClientState.stage();
        if (Heard != null) {
            HeldAge = 0;
            float Volume = volume(Heard.Distance(), Stage) * Muffle;
            Vec3 At = Heard.centre();
            if (Lub) {
                layers(Client, Scp472Registry.LubLayers, LubShares, At, Volume, 1.0f);
            } else if (Scp472ClientState.dubNow()) {
                layers(Client, Scp472Registry.DubLayers, DubShares, At, Volume, DubPitch);
            }
        } else {
            held(Client, User, Muffle);
        }
        whispers(Client, User, Stage, Muffle);
    }

    private static void held(Minecraft Client, LocalPlayer User, float Muffle) {
        if (!Scp472BloodstoneItem.is(User.getMainHandItem()) && !Scp472BloodstoneItem.is(User.getOffhandItem())) {
            HeldAge = 0;
            return;
        }
        HeldAge++;
        if (HeldAge >= HeldInterval) {
            HeldAge = 0;
            Client.getSoundManager().play(new SimpleSoundInstance(Scp472Registry.HeartThump.get().getLocation(), SoundSource.PLAYERS, HeldVolume * Muffle, HeldPitch,
                    SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
        }
    }

    private static void whispers(Minecraft Client, LocalPlayer User, int Stage, float Muffle) {
        if (Stage < Scp472Mind.VisionStage || Scp472ClientState.heard() == null) {
            NextWhisper = 0;
            return;
        }
        RandomSource Random = User.getRandom();
        if (NextWhisper <= 0) {
            NextWhisper = Mth.nextInt(Random, WhisperMin[Stage] / 2, WhisperMax[Stage] / 2);
            return;
        }
        NextWhisper--;
        if (NextWhisper > 0) {
            return;
        }
        NextWhisper = Mth.nextInt(Random, WhisperMin[Stage], WhisperMax[Stage]);
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Reach = WhisperNear + Random.nextDouble() * (WhisperFar - WhisperNear);
        Vec3 At = User.getEyePosition().add(Math.cos(Angle) * Reach, (Random.nextDouble() - 0.4) * 1.2, Math.sin(Angle) * Reach);
        float Volume = WhisperVolume[Stage] * Muffle;
        float Pitch = 0.85f + Random.nextFloat() * 0.2f;
        for (int Index = 0; Index < Scp472Registry.WhisperLayers.size(); Index++) {
            play(Client, Scp472Registry.WhisperLayers.get(Index).get(), SoundSource.AMBIENT, At, Volume * WhisperShares[Index], Pitch, SoundInstance.Attenuation.LINEAR);
        }
    }

    private static void layers(Minecraft Client, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float[] Shares, Vec3 At, float Volume, float Pitch) {
        for (int Index = 0; Index < Layers.size(); Index++) {
            play(Client, Layers.get(Index).get(), SoundSource.BLOCKS, At, Volume * Shares[Math.min(Index, Shares.length - 1)], Pitch, SoundInstance.Attenuation.NONE);
        }
    }

    private static void play(Minecraft Client, SoundEvent Sound, SoundSource Source, Vec3 At, float Volume, float Pitch, SoundInstance.Attenuation Falloff) {
        if (Volume <= 0.001f) {
            return;
        }
        Client.getSoundManager().play(new SimpleSoundInstance(Sound.getLocation(), Source, Volume, Pitch, SoundInstance.createUnseededRandom(), false, 0, Falloff,
                At.x, At.y, At.z, false));
    }

    public static void reset() {
        NextWhisper = 0;
        HeldAge = 0;
    }
}
