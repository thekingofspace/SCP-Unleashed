package net.rai.scp_unleashed.door.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;

import java.lang.ref.WeakReference;

public class DoorLayerSound extends AbstractTickableSoundInstance {
    private final WeakReference<PanelDoorBlockEntity> Door;
    private final DoorPart Part;
    private final float Detune;
    private int Age;
    private int Fading;

    public DoorLayerSound(PanelDoorBlockEntity Door, DoorPart Part, Vec3 Centre, RandomSource Random) {
        super(Part.Event().get(), SoundSource.BLOCKS, RandomSource.create(Random.nextLong()));
        this.Door = new WeakReference<>(Door);
        this.Part = Part;
        this.Detune = Part.isLoop() ? 1.0f : 1.0f + HeavyDoorSounds.Jitter * Random.nextFloat();
        this.looping = Part.isLoop();
        this.delay = 0;
        this.x = Centre.x;
        this.y = Centre.y;
        this.z = Centre.z;
        shape(Door.getSpeed());
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        PanelDoorBlockEntity Current = Door.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        if (Fading > 0) {
            Fading++;
        } else if (!Current.isTravelling() || !Part.isLoop() && Age >= attack() + Part.Hold()) {
            Fading = 1;
        }
        if (Fading > release()) {
            stop();
            return;
        }
        shape(Current.getSpeed());
        Age++;
    }

    private int attack() {
        return Part.isLoop() ? HeavyDoorSounds.LoopAttack : HeavyDoorSounds.GrainAttack;
    }

    private int release() {
        return Part.isLoop() ? HeavyDoorSounds.LoopRelease : HeavyDoorSounds.GrainRelease;
    }

    private void shape(float Speed) {
        float Rise = Math.min(1.0f, (Age + 1.0f) / (attack() + 1.0f));
        float Fall = Fading > 0 ? 1.0f - Fading / (release() + 1.0f) : 1.0f;
        volume = Part.loudnessAt(Speed) * Rise * Fall;
        pitch = Part.pitchAt(Speed) * Detune;
    }
}
