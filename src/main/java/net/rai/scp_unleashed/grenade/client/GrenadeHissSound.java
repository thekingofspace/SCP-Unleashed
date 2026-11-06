package net.rai.scp_unleashed.grenade.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.grenade.GrenadeEntity;
import net.rai.scp_unleashed.grenade.GrenadeRegistry;
import net.rai.scp_unleashed.grenade.GrenadeTuning;

public class GrenadeHissSound extends AbstractTickableSoundInstance {
    public static final float MaxVolume = 0.7F;
    public static final float TailVolume = 0.25F;
    public static final float FadeStep = 0.08F;
    private final GrenadeEntity Grenade;

    public GrenadeHissSound(GrenadeEntity Grenade) {
        super(GrenadeRegistry.GasHiss.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Grenade = Grenade;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.1F;
        this.pitch = 1.0F;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        follow();
    }

    private void follow() {
        Vec3 Centre = Grenade.centre();
        this.x = Centre.x;
        this.y = Centre.y;
        this.z = Centre.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        boolean Venting = !Grenade.isRemoved() && Grenade.getPhase() == GrenadeEntity.PhaseVenting && Grenade.getPhaseTicks() > 0;
        if (!Venting) {
            volume = Math.max(0.0F, volume - FadeStep * 2.0F);
            if (volume <= 0.0F) {
                stop();
            }
            return;
        }
        follow();
        float Left = Grenade.getPhaseTicks() / (float) GrenadeTuning.ReleaseTicks;
        float Target = TailVolume + (MaxVolume - TailVolume) * Left;
        volume += Math.max(-FadeStep, Math.min(FadeStep, Target - volume));
        pitch = 0.9F + 0.15F * Left;
    }
}
