package net.rai.scp_unleashed.space.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.rai.scp_unleashed.space.AirInfuserBlock;
import net.rai.scp_unleashed.space.AirInfuserBlockEntity;
import net.rai.scp_unleashed.space.SpaceRegistry;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class AirInfuserSound extends AbstractTickableSoundInstance {
    public static final float FadeIn = 0.15F;
    public static final float FadeOut = 0.08F;
    public static final float Loudness = 0.8F;
    private static final Map<AirInfuserBlockEntity, AirInfuserSound> Playing = new WeakHashMap<>();

    private final WeakReference<AirInfuserBlockEntity> Machine;
    private float Smoothed;

    public AirInfuserSound(AirInfuserBlockEntity Machine) {
        super(SpaceRegistry.InfuserHum.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Machine = new WeakReference<>(Machine);
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.x = Machine.getBlockPos().getX() + 0.5;
        this.y = Machine.getBlockPos().getY() + 0.5;
        this.z = Machine.getBlockPos().getZ() + 0.5;
    }

    public static void tick(AirInfuserBlockEntity Machine) {
        if (!Machine.getBlockState().getValue(AirInfuserBlock.Active)) {
            return;
        }
        AirInfuserSound Current = Playing.get(Machine);
        if (Current == null || Current.isStopped()) {
            Current = new AirInfuserSound(Machine);
            Playing.put(Machine, Current);
            Minecraft.getInstance().getSoundManager().play(Current);
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        AirInfuserBlockEntity Current = Machine.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Wanted = Current.getBlockState().getValue(AirInfuserBlock.Active) ? 1.0F : 0.0F;
        Smoothed += (Wanted - Smoothed) * (Wanted > Smoothed ? FadeIn : FadeOut);
        volume = Smoothed * Loudness;
        if (Wanted <= 0.0F && Smoothed < 0.01F) {
            stop();
        }
    }
}
