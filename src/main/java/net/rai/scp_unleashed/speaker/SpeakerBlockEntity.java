package net.rai.scp_unleashed.speaker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.block.entity.PortHolder;
import net.rai.scp_unleashed.speaker.client.SpeakerLoops;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.wire.WireMessage;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class SpeakerBlockEntity extends OutletBlockEntity implements WireDevice, PortHolder {
    public static final String KindName = "speaker";
    public static final String LoopingKey = "Looping";
    public static final int MaxSoundsPerTick = 4;

    private int Port;
    private String Channel = SoundMessages.channel(0);
    private List<String> Subscriptions = List.of(Channel);
    @Nullable
    private SoundMessages.Request Looping;
    private long PulseUntil;
    private long PlayedTick = Long.MIN_VALUE;
    private int PlayedCount;

    public SpeakerBlockEntity(BlockPos Pos, BlockState State) {
        super(SpeakerRegistry.SpeakerEntity.get(), Pos, State);
    }

    public int getPort() {
        return Port;
    }

    public String getChannel() {
        return Channel;
    }

    @Nullable
    public SoundMessages.Request getLooping() {
        return Looping;
    }

    public boolean isLooping() {
        return Looping != null;
    }

    private void usePort(int Requested) {
        Port = WireChannels.clampPort(Requested);
        Channel = SoundMessages.channel(Port);
        Subscriptions = List.of(Channel);
    }

    @Override
    public String getKindName() {
        return KindName;
    }

    @Override
    public int getSlotCount() {
        return 1;
    }

    @Override
    public int getSlot(int Slot) {
        return Port;
    }

    @Override
    public void setSlot(int Slot, int Requested) {
        int Clamped = WireChannels.clampPort(Requested);
        if (Slot != 0 || Clamped == Port) {
            return;
        }
        usePort(Clamped);
        Looping = null;
        if (level instanceof ServerLevel World) {
            SpeakerSoundEvent.post(World, this, null);
        }
        sync();
    }

    @Override
    public Collection<String> getSubscriptions() {
        return Subscriptions;
    }

    @Override
    public void receiveMessage(WireMessage Message) {
        if (!(level instanceof ServerLevel World) || !Message.Channel().equals(Channel)) {
            return;
        }
        if (SoundMessages.isStop(Message.Payload())) {
            loop(null);
            SpeakerSoundEvent.post(World, this, null);
            return;
        }
        SoundMessages.Request Request = SoundMessages.read(Message.Payload());
        if (Request == null) {
            return;
        }
        long Now = World.getGameTime();
        if (Now != PlayedTick) {
            PlayedTick = Now;
            PlayedCount = 0;
        }
        if (PlayedCount >= MaxSoundsPerTick) {
            return;
        }
        PlayedCount++;
        if (Request.Loop()) {
            loop(Request);
        } else {
            loop(null);
            SoundMessages.play(World, mouth(), Request);
        }
        SpeakerSoundEvent.post(World, this, Request);
        pulse(World, Now);
    }

    private void loop(@Nullable SoundMessages.Request Request) {
        if (Objects.equals(Looping, Request)) {
            return;
        }
        Looping = Request;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public Vec3 mouth() {
        Direction SpeakerFacing = getFacing();
        return Vec3.atCenterOf(worldPosition).add(SpeakerFacing.getStepX() * SpeakerBlock.MouthOffset, SpeakerFacing.getStepY() * SpeakerBlock.MouthOffset,
                SpeakerFacing.getStepZ() * SpeakerBlock.MouthOffset);
    }

    public long pulseRemaining(long Now) {
        return Looping != null ? SpeakerBlock.PulseTicks : Math.max(0, PulseUntil - Now);
    }

    private void pulse(ServerLevel World, long Now) {
        PulseUntil = Now + SpeakerBlock.PulseTicks;
        BlockState State = getBlockState();
        if (!(State.getBlock() instanceof SpeakerBlock Speaker)) {
            return;
        }
        if (!State.getValue(SpeakerBlock.Active)) {
            World.setBlock(worldPosition, State.setValue(SpeakerBlock.Active, true), Block.UPDATE_CLIENTS);
        }
        World.scheduleTick(worldPosition, Speaker, SpeakerBlock.PulseTicks);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            SpeakerLoops.release(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && level.isClientSide) {
            SpeakerLoops.release(this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Port", Port);
        if (Looping != null) {
            Tag.put(LoopingKey, Looping.toPayload());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        usePort(Tag.getInt("Port"));
        SoundMessages.Request Saved = Tag.contains(LoopingKey, CompoundTag.TAG_COMPOUND) ? SoundMessages.read(Tag.getCompound(LoopingKey)) : null;
        Looping = Saved != null && Saved.Loop() ? Saved : null;
        if (level != null && level.isClientSide) {
            SpeakerLoops.update(this);
        }
    }
}
