package net.rai.scp_unleashed.speaker.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import org.jetbrains.annotations.Nullable;

public class SpeakerLoopSound extends AbstractTickableSoundInstance {
    private final BlockPos Pos;
    private final SoundMessages.Request Loop;
    private final Vec3 Mouth;
    private final float Range;
    private SpeakerBlockEntity Owner;

    public SpeakerLoopSound(SpeakerBlockEntity Speaker, SoundMessages.Request Loop, float Range) {
        super(SoundEvent.createVariableRangeEvent(Loop.Id()), SoundMessages.Source, SoundInstance.createUnseededRandom());
        this.Pos = Speaker.getBlockPos().immutable();
        this.Loop = Loop;
        this.Mouth = Speaker.mouth();
        this.Range = Range;
        this.Owner = Speaker;
        this.looping = true;
        this.delay = 0;
        this.volume = Loop.Volume();
        this.pitch = Loop.Pitch();
        this.attenuation = Attenuation.LINEAR;
        this.relative = false;
        this.x = Mouth.x;
        this.y = Mouth.y;
        this.z = Mouth.z;
    }

    public BlockPos getPos() {
        return Pos;
    }

    public Vec3 getMouth() {
        return Mouth;
    }

    public float getRange() {
        return Range;
    }

    public SpeakerBlockEntity getOwner() {
        return Owner;
    }

    public boolean plays(@Nullable SoundMessages.Request Wanted) {
        return Loop.sameSound(Wanted);
    }

    public void adopt(SpeakerBlockEntity Speaker) {
        Owner = Speaker;
    }

    public void halt() {
        stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (!SpeakerLoops.keeps(this)) {
            stop();
        }
    }
}
