package net.rai.scp_unleashed.speaker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.NetworkObjectBlock;
import net.rai.scp_unleashed.block.entity.NetworkObjectBlockEntity;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteRecording;
import net.rai.scp_unleashed.wire.WireMessage;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SoundConsoleBlockEntity extends NetworkObjectBlockEntity {
    public static final String KindName = "sound_console";
    public static final int SendingTicks = 10;
    public static final float MinVolume = 0.1f;

    private ResourceLocation Sound = SoundCatalog.first().Id();
    @Nullable
    private CassetteRecording Recording;
    private float Volume = SoundMessages.DefaultVolume;
    private float Pitch = SoundMessages.DefaultPitch;
    private boolean Loop;
    private boolean Driving;
    private List<String> Listening = List.of();
    private boolean Input;
    private boolean Started;
    private boolean Triggered;
    private boolean Released;
    private long SendingUntil;

    public SoundConsoleBlockEntity(BlockPos Pos, BlockState State) {
        super(SpeakerRegistry.SoundConsoleEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, SoundConsoleBlockEntity Console) {
        if (World instanceof ServerLevel ServerWorld) {
            Console.tickServer(ServerWorld, State);
        }
    }

    public static float clampVolume(float Requested) {
        return Float.isFinite(Requested) ? Mth.clamp(Requested, MinVolume, SoundMessages.MaxVolume) : SoundMessages.DefaultVolume;
    }

    @Override
    public String getKindName() {
        return KindName;
    }

    public ResourceLocation getSound() {
        return Sound;
    }

    @Nullable
    public CassetteRecording getRecording() {
        return Recording;
    }

    public Component getSoundName() {
        SoundCatalog.Entry Entry = SoundCatalog.find(Sound);
        if (Entry != null && !Entry.isAnomalous()) {
            return Component.translatable(Entry.getTranslationKey());
        }
        return Recording != null ? Recording.Title() : Component.literal(Sound.toString());
    }

    public float getVolume() {
        return Volume;
    }

    public float getPitch() {
        return Pitch;
    }

    public boolean isLoop() {
        return Loop;
    }

    public boolean isDriving() {
        return Driving;
    }

    @Override
    public Collection<String> getSubscriptions() {
        return Listening;
    }

    @Override
    public void receiveMessage(WireMessage Message) {
        if (Driving && Message.Channel().equals(SoundMessages.channel(Port))) {
            drive(false);
        }
    }

    public void readInput() {
        if (level == null) {
            return;
        }
        boolean High = level.hasNeighborSignal(worldPosition);
        if (Started && High && !Input) {
            Triggered = true;
        }
        if (Started && !High && Input) {
            Released = true;
        }
        Input = High;
    }

    private void tickServer(ServerLevel World, BlockState State) {
        if (!Started) {
            Started = true;
            Input = World.hasNeighborSignal(worldPosition);
            Triggered = false;
            Released = false;
        }
        if (Triggered) {
            Triggered = false;
            transmit(World);
        }
        if (Released) {
            Released = false;
            if (Driving) {
                stopLoop(World);
            }
        }
        boolean Powered = isPowered();
        boolean Busy = World.getGameTime() < SendingUntil;
        if (State.getBlock() instanceof SoundConsoleBlock
                && (State.getValue(NetworkObjectBlock.Active) != Powered || State.getValue(SoundConsoleBlock.Sending) != Busy)) {
            World.setBlock(worldPosition, State.setValue(NetworkObjectBlock.Active, Powered).setValue(SoundConsoleBlock.Sending, Busy), Block.UPDATE_CLIENTS);
        }
    }

    public boolean transmit(ServerLevel World) {
        if (!isPowered()) {
            return false;
        }
        send(World, SoundMessages.channel(Port), SoundMessages.payload(Sound, Volume, Pitch, Loop));
        SendingUntil = World.getGameTime() + SendingTicks;
        drive(Loop);
        return true;
    }

    public boolean stopLoop(ServerLevel World) {
        if (!isPowered()) {
            return false;
        }
        send(World, SoundMessages.channel(Port), SoundMessages.stopPayload());
        SendingUntil = World.getGameTime() + SendingTicks;
        drive(false);
        return true;
    }

    private void drive(boolean Now) {
        if (Driving == Now) {
            return;
        }
        Driving = Now;
        listen();
        changed();
    }

    private void listen() {
        Listening = Driving ? List.of(SoundMessages.channel(Port)) : List.of();
    }

    @Override
    protected void portChanged(int OldPort) {
        if (!Driving) {
            return;
        }
        if (level instanceof ServerLevel World && isPowered()) {
            send(World, SoundMessages.channel(OldPort), SoundMessages.stopPayload());
            SendingUntil = World.getGameTime() + SendingTicks;
        }
        drive(false);
    }

    public void applySettings(ServerPlayer User, ResourceLocation Requested, float RequestedVolume, float RequestedPitch, boolean RequestedLoop) {
        ResourceLocation ChosenSound = Sound;
        CassetteRecording ChosenRecording = Recording;
        if (!Requested.equals(Sound)) {
            SoundCatalog.Entry Entry = SoundCatalog.find(Requested);
            Optional<CassetteRecording> Owned = AnomalousLibrary.find(User, Requested);
            if (Entry != null && !Entry.isAnomalous()) {
                ChosenSound = Entry.Id();
                ChosenRecording = null;
            } else if (Owned.isPresent()) {
                ChosenSound = Requested;
                ChosenRecording = Owned.get();
            }
        }
        float NewVolume = clampVolume(RequestedVolume);
        float NewPitch = SoundMessages.clampPitch(RequestedPitch);
        boolean LoopChanged = RequestedLoop != Loop;
        if (!LoopChanged && ChosenSound.equals(Sound) && Objects.equals(ChosenRecording, Recording) && NewVolume == Volume && NewPitch == Pitch) {
            return;
        }
        Sound = ChosenSound;
        Recording = ChosenRecording;
        Volume = NewVolume;
        Pitch = NewPitch;
        Loop = RequestedLoop;
        changed();
        if (LoopChanged && !Loop && Driving && level instanceof ServerLevel World) {
            stopLoop(World);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putString("Sound", Sound.toString());
        if (Recording != null) {
            CassetteRecording.RecordingCodec.encodeStart(Registries.createSerializationContext(NbtOps.INSTANCE), Recording)
                    .ifSuccess(Encoded -> Tag.put("Recording", Encoded));
        }
        Tag.putFloat("Volume", Volume);
        Tag.putFloat("Pitch", Pitch);
        Tag.putBoolean("Loop", Loop);
        Tag.putBoolean("Driving", Driving);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        ResourceLocation Saved = Tag.contains("Sound", CompoundTag.TAG_STRING) ? ResourceLocation.tryParse(Tag.getString("Sound")) : null;
        CassetteRecording Stored = Tag.contains("Recording") ? CassetteRecording.RecordingCodec
                .parse(Registries.createSerializationContext(NbtOps.INSTANCE), Tag.get("Recording")).result().orElse(null) : null;
        SoundCatalog.Entry Entry = Saved == null ? null : SoundCatalog.find(Saved);
        if (Entry != null && !Entry.isAnomalous()) {
            Sound = Entry.Id();
            Recording = null;
        } else if (Stored != null && Stored.Sound().equals(Saved)) {
            Sound = Saved;
            Recording = Stored;
        } else {
            Sound = SoundCatalog.first().Id();
            Recording = null;
        }
        Volume = Tag.contains("Volume", CompoundTag.TAG_ANY_NUMERIC) ? clampVolume(Tag.getFloat("Volume")) : SoundMessages.DefaultVolume;
        Pitch = Tag.contains("Pitch", CompoundTag.TAG_ANY_NUMERIC) ? SoundMessages.clampPitch(Tag.getFloat("Pitch")) : SoundMessages.DefaultPitch;
        Loop = Tag.getBoolean("Loop");
        Driving = Tag.getBoolean("Driving");
        listen();
    }
}
