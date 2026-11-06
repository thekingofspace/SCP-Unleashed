package net.rai.scp_unleashed.microhid.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlock;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlockEntity;
import net.rai.scp_unleashed.microhid.MicroChargingStatus;
import net.rai.scp_unleashed.microhid.MicroHidRegistry;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class MicroChargingPlatformSound extends AbstractTickableSoundInstance {
    public static final float FadeIn = 0.12F;
    public static final float FadeOut = 0.08F;
    public static final float Loudness = 0.45F;
    public static final float Pitch = 1.7F;
    private static final Map<MicroChargingPlatformBlockEntity, MicroChargingPlatformSound> Playing = new WeakHashMap<>();

    private final WeakReference<MicroChargingPlatformBlockEntity> Platform;
    private float Smoothed;

    public MicroChargingPlatformSound(MicroChargingPlatformBlockEntity Platform) {
        super(MicroHidRegistry.PlatformHum.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Platform = new WeakReference<>(Platform);
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = Pitch;
        this.x = Platform.getBlockPos().getX() + 0.5;
        this.y = Platform.getBlockPos().getY() + 0.5;
        this.z = Platform.getBlockPos().getZ() + 0.5;
    }

    public static boolean charging(MicroChargingPlatformBlockEntity Platform) {
        return Platform.getBlockState().getBlock() instanceof MicroChargingPlatformBlock
                && Platform.getBlockState().getValue(MicroChargingPlatformBlock.Status) == MicroChargingStatus.Charging;
    }

    public static void tick(MicroChargingPlatformBlockEntity Platform) {
        if (!charging(Platform)) {
            return;
        }
        MicroChargingPlatformSound Current = Playing.get(Platform);
        if (Current == null || Current.isStopped()) {
            Current = new MicroChargingPlatformSound(Platform);
            Playing.put(Platform, Current);
            Minecraft.getInstance().getSoundManager().play(Current);
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        MicroChargingPlatformBlockEntity Current = Platform.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Wanted = charging(Current) ? 1.0F : 0.0F;
        Smoothed += (Wanted - Smoothed) * (Wanted > Smoothed ? FadeIn : FadeOut);
        volume = Smoothed * Loudness;
        if (Wanted <= 0.0F && Smoothed < 0.01F) {
            stop();
        }
    }
}
