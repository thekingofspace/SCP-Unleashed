package net.rai.scp_unleashed.scp256;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp256TypewriterBlockEntity extends BlockEntity {
    public static final long StreakGap = 600L;
    public static final long ReturnGap = 2400L;
    public static final int CatchUp = 4;
    public static final int StrokeSounds = 3;
    private String LineId = "";
    private long Started;
    private int Duration;
    private int Seed;
    private int Uses;
    private int Streak;
    private long LastEnded;
    @Nullable
    private UUID LastUser;
    @Nullable
    private Scp256Schedule Cached;
    private String CachedKey = "";
    private long HeardStart = Long.MIN_VALUE;
    private int Heard;
    private int HeardReturns;
    private boolean HeardFeed;
    private boolean HeardDing;
    private boolean HeardStop;

    public Scp256TypewriterBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp256Registry.TypewriterEntity.get(), Pos, State);
    }

    public boolean isTyping(long Now) {
        return !LineId.isEmpty() && Now >= Started && Now < Started + Duration;
    }

    public boolean hasMessage() {
        return !LineId.isEmpty();
    }

    public Optional<Scp256Line> getLine() {
        return LineId.isEmpty() ? Optional.empty() : Scp256Line.byId(LineId);
    }

    public long getStarted() {
        return Started;
    }

    public int getDuration() {
        return Duration;
    }

    public int getSeed() {
        return Seed;
    }

    public int getUses() {
        return Uses;
    }

    public int getStreak() {
        return Streak;
    }

    public long getLastEnded() {
        return LastEnded;
    }

    @Nullable
    public UUID getLastUser() {
        return LastUser;
    }

    public Scp256Line.Context contextFor(Level World, @Nullable Player User, long Now) {
        boolean Known = LastEnded > 0L;
        long Gap = Known ? Now - LastEnded : Long.MAX_VALUE;
        int NextStreak = Known && Gap <= StreakGap ? Streak + 1 : 1;
        UUID Who = User == null ? null : User.getUUID();
        boolean Returning = Who != null && Who.equals(LastUser) && Gap >= ReturnGap;
        boolean Newcomer = Who != null && LastUser != null && !Who.equals(LastUser);
        return new Scp256Line.Context(Scp256Line.isNight(World), Returning, Newcomer, NextStreak, Uses == 0, getLine().orElse(null));
    }

    public boolean press(ServerLevel World, @Nullable Player User) {
        long Now = World.getGameTime();
        if (isTyping(Now)) {
            return false;
        }
        Scp256Line.Context Context = contextFor(World, User, Now);
        begin(World, Scp256Line.pick(Context, World.getRandom()), Now);
        Streak = Context.Streak();
        if (User != null) {
            LastUser = User.getUUID();
        }
        setChanged();
        return true;
    }

    public void arrive(ServerLevel World) {
        begin(World, Scp256Line.Arrival, World.getGameTime());
        Streak = 1;
        setChanged();
    }

    public void begin(ServerLevel World, Scp256Line Line, long Now) {
        String Text = Language.getInstance().getOrDefault(Line.key());
        Seed = World.getRandom().nextInt(Integer.MAX_VALUE);
        Duration = Scp256Schedule.of(Text, Seed).getDuration();
        Started = Now;
        LineId = Line.getId();
        LastEnded = Now + Duration;
        Uses++;
        Cached = null;
        setChanged();
        BlockState State = getBlockState();
        World.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
    }

    public void clear() {
        LineId = "";
        Duration = 0;
        Cached = null;
        setChanged();
        if (level instanceof ServerLevel World) {
            BlockState State = getBlockState();
            World.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    public String localText() {
        Optional<Scp256Line> Line = getLine();
        return Line.map(Value -> Language.getInstance().getOrDefault(Value.key())).orElse("");
    }

    @Nullable
    public Scp256Schedule schedule() {
        if (LineId.isEmpty()) {
            return null;
        }
        String Text = localText();
        String Key = LineId + "|" + Seed + "|" + Duration + "|" + Text.hashCode();
        if (Cached == null || !Key.equals(CachedKey)) {
            Cached = Scp256Schedule.of(Text, Seed).scaled(Duration);
            CachedKey = Key;
        }
        return Cached;
    }

    public double elapsed(Level World, float PartialTick) {
        return World.getGameTime() - Started + PartialTick;
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp256TypewriterBlockEntity Typewriter) {
        Scp256Typewriters.track(Typewriter);
        Typewriter.listen(World);
    }

    private void listen(Level World) {
        Scp256Schedule Plan = schedule();
        if (Plan == null) {
            return;
        }
        long Elapsed = World.getGameTime() - Started;
        if (Elapsed < 0 || Elapsed > Duration + CatchUp) {
            HeardStart = Started;
            return;
        }
        if (HeardStart != Started) {
            HeardStart = Started;
            boolean Late = Elapsed > CatchUp;
            Heard = Late ? Plan.typedAt(Elapsed) : 0;
            HeardReturns = 0;
            while (Late && HeardReturns < Plan.returns() && Plan.returnAt(HeardReturns) <= Elapsed) {
                HeardReturns++;
            }
            HeardFeed = Late;
            HeardDing = Late && Elapsed >= Plan.dingAt();
            HeardStop = Late && Elapsed >= Plan.finalReturnAt() + Scp256Schedule.ReturnTicks;
        }
        if (!HeardFeed) {
            HeardFeed = true;
            play(World, Scp256Registry.PaperLayers, 0.55f, 1.0f);
        }
        int Typed = Plan.typedAt(Elapsed);
        int Played = 0;
        for (int Stroke = Heard; Stroke < Typed; Stroke++) {
            if (Played++ < StrokeSounds) {
                stroke(World, Plan, Stroke);
            }
        }
        Heard = Math.max(Heard, Typed);
        while (HeardReturns < Plan.returns() && Plan.returnAt(HeardReturns) <= Elapsed) {
            HeardReturns++;
            play(World, Scp256Registry.ReturnLayers, 0.5f, 1.08f);
        }
        if (!HeardDing && Elapsed >= Plan.dingAt()) {
            HeardDing = true;
            play(World, Scp256Registry.DingLayers, 0.7f, 1.0f);
            play(World, Scp256Registry.ReturnLayers, 0.55f, 0.96f);
        }
        if (!HeardStop && Elapsed >= Plan.finalReturnAt() + Scp256Schedule.ReturnTicks) {
            HeardStop = true;
            play(World, Scp256Registry.StopLayers, 0.5f, 1.0f);
        }
    }

    private void stroke(Level World, Scp256Schedule Plan, int Stroke) {
        char Typed = Plan.charOf(Stroke);
        int Hash = Scp256Schedule.mix(Seed + 7, Stroke);
        float Pitch = 0.92f + (Hash % 17) / 100.0f;
        if (Typed == ' ') {
            play(World, Scp256Registry.SpaceLayers, 0.55f, Pitch);
            return;
        }
        play(World, Scp256Registry.KeyLayers, 0.6f, Pitch);
    }

    private void play(Level World, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playLocalSound(worldPosition, Layer.get(), SoundSource.BLOCKS, Volume, Pitch, false);
        }
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).inflate(0.5, 0.0, 0.5).expandTowards(0.0, 0.25, 0.0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            Scp256Typewriters.track(this);
        }
    }

    @Override
    public void setRemoved() {
        Scp256Typewriters.forget(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        Scp256Typewriters.forget(this);
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.saveAdditional(Tag, Lookup);
        writeShown(Tag);
        Tag.putInt("Streak", Streak);
        Tag.putLong("LastEnded", LastEnded);
        if (LastUser != null) {
            Tag.putUUID("LastUser", LastUser);
        }
    }

    private void writeShown(CompoundTag Tag) {
        Tag.putString("Line", LineId);
        Tag.putLong("Started", Started);
        Tag.putInt("Duration", Duration);
        Tag.putInt("Seed", Seed);
        Tag.putInt("Uses", Uses);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.loadAdditional(Tag, Lookup);
        LineId = Tag.getString("Line");
        Started = Tag.getLong("Started");
        Duration = Tag.getInt("Duration");
        Seed = Tag.getInt("Seed");
        Uses = Tag.getInt("Uses");
        if (Tag.contains("Streak")) {
            Streak = Tag.getInt("Streak");
        }
        if (Tag.contains("LastEnded")) {
            LastEnded = Tag.getLong("LastEnded");
        }
        LastUser = Tag.hasUUID("LastUser") ? Tag.getUUID("LastUser") : LastUser;
        if (Scp256Line.byId(LineId).isEmpty()) {
            LineId = "";
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Synced = super.getUpdateTag(Lookup);
        writeShown(Synced);
        return Synced;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
