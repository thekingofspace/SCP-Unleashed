package net.rai.scp_unleashed.steam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlockEntity;

import java.lang.ref.WeakReference;

public class IndustrialTurbineSound extends AbstractTickableSoundInstance {
    public static final float Silent = 0.01f;

    private final WeakReference<IndustrialTurbineBlockEntity> Machine;
    private final TurbineLoopSound.Layer Kind;

    public IndustrialTurbineSound(IndustrialTurbineBlockEntity Machine, TurbineLoopSound.Layer Kind) {
        super(Kind.Sound(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Machine = new WeakReference<>(Machine);
        this.Kind = Kind;
        this.looping = true;
        this.delay = 0;
        this.volume = Kind.volumeFor(Machine.getSpin());
        this.pitch = Kind.pitchFor(Machine.getSpin());
        Vec3 Centre = Machine.getSoundCentre();
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
        IndustrialTurbineBlockEntity Current = Machine.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Spin = Current.getSpin();
        volume = Kind.volumeFor(Spin);
        pitch = Kind.pitchFor(Spin);
        if (Spin < Silent) {
            stop();
        }
    }
}
