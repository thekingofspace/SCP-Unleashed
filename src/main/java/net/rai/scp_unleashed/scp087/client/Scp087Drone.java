package net.rai.scp_unleashed.scp087.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp087.Scp087Darkness;
import net.rai.scp_unleashed.scp087.Scp087Registry;
import org.jetbrains.annotations.Nullable;

public class Scp087Drone extends AbstractTickableSoundInstance {
    public static final float Quiet = 0.18f;
    public static final float Loud = 0.55f;
    public static final float Ease = 0.05f;
    @Nullable
    private static Scp087Drone Playing;
    private float Wanted;

    public Scp087Drone() {
        super(Scp087Registry.Drone.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        looping = true;
        delay = 0;
        relative = true;
        attenuation = Attenuation.NONE;
        volume = 0.0f;
        pitch = 1.0f;
    }

    public static float loudness() {
        float Share = Mth.clamp(Scp087ClientState.flights() / (float) Scp087Darkness.DeepAt, 0.0f, 1.0f);
        return Mth.lerp(Share, Quiet, Loud);
    }

    public static void tickAll(Minecraft Client) {
        boolean Active = Scp087ClientState.inStairwell(Client) && !Scp087ClientState.inert() && Client.player != null;
        if (Active && (Playing == null || Playing.isStopped())) {
            Playing = new Scp087Drone();
            Client.getSoundManager().play(Playing);
        }
        if (Playing != null) {
            Playing.Wanted = Active ? loudness() : 0.0f;
        }
    }

    public static void stopAll(Minecraft Client) {
        if (Playing != null) {
            Client.getSoundManager().stop(Playing);
            Playing = null;
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        volume += (Wanted - volume) * Ease;
        if (Wanted <= 0.0f && volume < 0.01f) {
            stop();
            if (Playing == this) {
                Playing = null;
            }
        }
    }
}
