package net.rai.scp_unleashed.scp131.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import net.rai.scp_unleashed.scp131.Scp131Registry;

public class Scp131Whirr extends AbstractTickableSoundInstance {
    public static final float MaxVolume = 0.3f;
    public static final float FullSpeed = 0.42f;
    public static final float QuietSpeed = 0.06f;
    public static final int QuietTicks = 10;
    public static final float PitchBase = 0.85f;
    public static final float PitchPerSpeed = 0.6f;
    public static final float SadShare = 0.6f;
    private final Scp131Pod Pod;
    private int Quiet;

    public Scp131Whirr(Scp131Pod Pod) {
        super(Scp131Registry.Whirr.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.Pod = Pod;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.x = Pod.getX();
        this.y = Pod.getY();
        this.z = Pod.getZ();
    }

    @Override
    public boolean canPlaySound() {
        return !Pod.isSilent();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (Pod.isRemoved() || !Pod.isAlive()) {
            stop();
            return;
        }
        x = Pod.getX();
        y = Pod.getY();
        z = Pod.getZ();
        double Dx = Pod.getX() - Pod.xo;
        double Dz = Pod.getZ() - Pod.zo;
        float Speed = (float) Math.sqrt(Dx * Dx + Dz * Dz);
        Quiet = Speed < QuietSpeed || !Pod.onGround() ? Quiet + 1 : 0;
        if (Quiet > QuietTicks) {
            stop();
            return;
        }
        float Share = Mth.clamp(Speed / FullSpeed, 0.0f, 1.0f);
        volume = Share * MaxVolume * (Pod.isSad() ? SadShare : 1.0f);
        pitch = Pod.voicePitch() * (PitchBase + Share * PitchPerSpeed);
    }
}
