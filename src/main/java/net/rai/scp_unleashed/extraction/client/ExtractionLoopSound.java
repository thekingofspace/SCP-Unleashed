package net.rai.scp_unleashed.extraction.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;

public class ExtractionLoopSound extends AbstractTickableSoundInstance {
    private static final float MaxVolume = 0.75F;
    private static final float LowPitch = 0.72F;
    private static final float PitchRise = 0.88F;
    private final Player User;
    private float Fade;

    public ExtractionLoopSound(Player User) {
        super(ExtractionRegistry.ExtractionLoop.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.User = User;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = LowPitch;
        follow();
    }

    @Override
    public void tick() {
        ExtractionTracker.Session Current = User.isRemoved() ? null : ExtractionTracker.sessionFor(User.getId());
        if (Current != null) {
            Fade = Math.min(1.0F, Fade + 0.25F);
            pitch = LowPitch + PitchRise * Current.getProgress();
        } else {
            Fade -= 0.2F;
            if (Fade <= 0.0F) {
                stop();
                return;
            }
        }
        volume = MaxVolume * Fade;
        follow();
    }

    private void follow() {
        x = User.getX();
        y = User.getEyeY() - 0.4;
        z = User.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
