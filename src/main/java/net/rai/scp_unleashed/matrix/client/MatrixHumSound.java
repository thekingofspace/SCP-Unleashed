package net.rai.scp_unleashed.matrix.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlockEntity;

import java.lang.ref.WeakReference;
import java.util.function.ToDoubleFunction;

public class MatrixHumSound extends AbstractTickableSoundInstance {
    public static final float Silent = 0.008f;

    private final WeakReference<InstabilityMatrixBlockEntity> Matrix;
    private final ToDoubleFunction<InstabilityMatrixBlockEntity> Loudness;
    private final ToDoubleFunction<InstabilityMatrixBlockEntity> Tune;
    private float Smoothed;

    public MatrixHumSound(InstabilityMatrixBlockEntity Matrix, SoundEvent Sound, ToDoubleFunction<InstabilityMatrixBlockEntity> Loudness,
                          ToDoubleFunction<InstabilityMatrixBlockEntity> Tune) {
        super(Sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Matrix = new WeakReference<>(Matrix);
        this.Loudness = Loudness;
        this.Tune = Tune;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001f;
        this.pitch = (float) Tune.applyAsDouble(Matrix);
        Vec3 Centre = Matrix.getCoreCentre();
        this.x = Centre.x;
        this.y = Centre.y;
        this.z = Centre.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public float getPitchNow() {
        return pitch;
    }

    @Override
    public void tick() {
        InstabilityMatrixBlockEntity Current = Matrix.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Wanted = (float) Loudness.applyAsDouble(Current);
        Smoothed += (Wanted - Smoothed) * (Wanted > Smoothed ? 0.12f : 0.06f);
        volume = Math.max(Smoothed, 0.001f);
        pitch = (float) Tune.applyAsDouble(Current);
        if (Wanted <= 0.0f && Smoothed < Silent) {
            stop();
        }
    }
}
