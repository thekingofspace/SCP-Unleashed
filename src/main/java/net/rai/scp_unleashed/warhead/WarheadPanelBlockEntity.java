package net.rai.scp_unleashed.warhead;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.door.DoorSounds;
import net.rai.scp_unleashed.door.KeycardItem;
import net.rai.scp_unleashed.discord.DiscordNotices;

import java.util.List;
import java.util.Optional;

public class WarheadPanelBlockEntity extends BlockEntity {
    public static final int NoteNone = 0;
    public static final int NoteCancelled = 1;
    public static final int NoteDetonated = 2;
    public static final int NoteFailed = 3;
    public static final int RefreshTicks = 20;
    public static final int NoteTicks = 120;
    public static final int PressTicks = 8;
    public static final float LeverSpeed = 1.0f / 3.0f;
    public static final float CoverSpeed = 1.0f / 7.0f;

    private boolean Unlocked;
    private boolean Lever;
    private boolean Cover;
    private int Linked;
    private long Deadline = -1L;
    private boolean Locked;
    private boolean Foreign;
    private int Note;
    private long NoteUntil;
    private int Presses;
    private int Aborts;
    private int LastComparator;
    private PanelStage LastStage;
    private boolean NeedsSync;
    private long LastSync = Long.MIN_VALUE / 2;

    private float LeverNow;
    private float LeverOld;
    private float CoverNow;
    private float CoverOld;
    private int SeenPresses;
    private int SeenAborts;
    private int ButtonTime;
    private int AbortTime;
    private boolean Primed;

    public WarheadPanelBlockEntity(BlockPos Pos, BlockState State) {
        super(WarheadRegistry.PanelEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, WarheadPanelBlockEntity Panel) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof WarheadPanelBlock) {
            Panel.tickServer(ServerWorld);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, WarheadPanelBlockEntity Panel) {
        Panel.tickClient();
    }

    private void tickServer(ServerLevel World) {
        long Now = World.getGameTime();
        if (LastStage == null || Math.floorMod(Now + worldPosition.asLong(), RefreshTicks) == 0) {
            reconcile(World);
        }
        if (NeedsSync && Now - LastSync >= 2) {
            NeedsSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void flush(ServerLevel World) {
        if (NeedsSync) {
            NeedsSync = false;
            LastSync = World.getGameTime();
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void reconcile(ServerLevel World) {
        WarheadNetwork Network = WarheadNetwork.get(World);
        boolean WasEmitting = isEmitting();
        int Count = Network.warheadsNear(World, worldPosition, WarheadNetwork.LinkRange).size();
        if (Count != Linked) {
            Linked = Count;
            changed();
        }
        int Outcome = Network.takeOutcome(worldPosition);
        if (Outcome != NoteNone) {
            resetAfter(World, Outcome);
        }
        Optional<WarheadSequence> Shown = Network.sequenceAffecting(World, worldPosition);
        long NextDeadline = Shown.map(WarheadSequence::getDeadline).orElse(-1L);
        boolean NextLocked = Shown.map(WarheadSequence::isLocked).orElse(false);
        boolean NextForeign = Shown.map(Sequence -> !Sequence.getPanel().equals(worldPosition)).orElse(false);
        if (NextDeadline != Deadline || NextLocked != Locked || NextForeign != Foreign) {
            Deadline = NextDeadline;
            Locked = NextLocked;
            Foreign = NextForeign;
            changed();
        }
        publish(World);
        int Comparator = comparatorLevel(World.getGameTime());
        if (WasEmitting != isEmitting() || Comparator != LastComparator) {
            LastComparator = Comparator;
            WarheadPanelBlock.updateAll(World, worldPosition, getBlockState());
        }
    }

    private void publish(ServerLevel World) {
        PanelStage Stage = Foreign ? physicalStage() : stage();
        if (Stage != LastStage) {
            LastStage = Stage;
            WarheadNetwork Network = WarheadNetwork.get(World);
            Network.setPanel(worldPosition, Stage);
            Network.refreshNear(World, worldPosition);
        }
    }

    private void resetAfter(ServerLevel World, int Outcome) {
        Unlocked = false;
        Lever = false;
        Cover = false;
        Note = Outcome;
        NoteUntil = World.getGameTime() + NoteTicks;
        changed();
    }

    private void changed() {
        NeedsSync = true;
        setChanged();
    }

    public PanelStage stage() {
        if (Deadline >= 0L) {
            return Locked ? PanelStage.Locked : PanelStage.Counting;
        }
        return physicalStage();
    }

    private PanelStage physicalStage() {
        if (Lever && Cover) {
            return PanelStage.Ready;
        }
        if (Lever) {
            return PanelStage.Armed;
        }
        return Unlocked ? PanelStage.Unlocked : PanelStage.Sealed;
    }

    public boolean isEmitting() {
        return Deadline >= 0L;
    }

    public boolean isOwnCountdown() {
        return Deadline >= 0L && !Foreign;
    }

    public int comparatorLevel(long Now) {
        if (Deadline < 0L) {
            return 0;
        }
        int Left = (int) Math.max(0L, Deadline - Now);
        return Mth.clamp((int) Math.ceil(15.0 * Left / WarheadNetwork.CountdownTicks), 1, 15);
    }

    public int ticksLeft(long Now) {
        return Deadline < 0L ? 0 : (int) Math.max(0L, Deadline - Now);
    }

    private Direction front() {
        BlockState State = getBlockState();
        return State.hasProperty(WarheadPanelBlock.Facing) ? State.getValue(WarheadPanelBlock.Facing) : Direction.NORTH;
    }

    public PanelControl pick(Vec3 Eye, Vec3 Point) {
        Direction Front = front();
        Vec3 Start = WarheadGeometry.toStructure(Eye, worldPosition, Front);
        Vec3 Heading = WarheadGeometry.toStructureDirection(Point.subtract(Eye).normalize(), Front);
        List<AABB> Boxes = Cover ? List.of(WarheadGeometry.KeycardBox, WarheadGeometry.LeverBox, WarheadGeometry.CoverOpenBox, WarheadGeometry.ButtonBox, WarheadGeometry.AbortBox)
                : List.of(WarheadGeometry.KeycardBox, WarheadGeometry.LeverBox, WarheadGeometry.CoverClosedBox, WarheadGeometry.AbortBox);
        List<PanelControl> Controls = Cover ? List.of(PanelControl.Keycard, PanelControl.Lever, PanelControl.Cover, PanelControl.Button, PanelControl.Abort)
                : List.of(PanelControl.Keycard, PanelControl.Lever, PanelControl.Cover, PanelControl.Abort);
        PanelControl Best = PanelControl.Face;
        double Nearest = Double.MAX_VALUE;
        for (int Index = 0; Index < Boxes.size(); Index++) {
            double Distance = WarheadGeometry.rayHit(Boxes.get(Index), Start, Heading);
            if (Distance >= 0.0 && Distance < Nearest) {
                Nearest = Distance;
                Best = Controls.get(Index);
            }
        }
        return Best;
    }

    public void interact(ServerLevel World, Player User, BlockHitResult Hit) {
        reconcile(World);
        PanelControl Control = Hit.getDirection() == front() ? pick(User.getEyePosition(), Hit.getLocation()) : PanelControl.Face;
        switch (Control) {
            case Keycard -> swipe(World, User);
            case Lever -> throwLever(World, User);
            case Cover -> flipCover(World, User);
            case Button -> pressButton(World, User);
            case Abort -> pressAbort(World, User);
            default -> report(User);
        }
        publish(World);
        World.gameEvent(User, GameEvent.BLOCK_ACTIVATE, worldPosition);
    }

    private void tell(Player User, String Key, ChatFormatting Colour, Object... Arguments) {
        User.displayClientMessage(Component.translatable("message.scp_unleashed.warhead." + Key, Arguments).withStyle(Colour), true);
    }

    private void play(ServerLevel World, Vec3 Where, SoundEvent Sound, float Volume, float Pitch) {
        Vec3 At = WarheadGeometry.toWorld(Where, worldPosition, front());
        World.playSound(null, At.x, At.y, At.z, Sound, SoundSource.BLOCKS, Volume, Pitch * (0.97f + World.getRandom().nextFloat() * 0.06f));
    }

    private void refuse(ServerLevel World, Vec3 Where, Player User, String Key) {
        play(World, Where, WarheadRegistry.ControlLocked.get(), 0.8f, 1.0f);
        tell(User, Key, ChatFormatting.RED);
    }

    private static ItemStack cardIn(Player User) {
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Stack = User.getItemInHand(Hand);
            if (Stack.getItem() instanceof KeycardItem) {
                return Stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private void swipe(ServerLevel World, Player User) {
        Vec3 Where = WarheadGeometry.KeycardBox.getCenter();
        if (Deadline >= 0L) {
            refuse(World, Where, User, "in_progress");
            return;
        }
        ItemStack Card = cardIn(User);
        if (Card.isEmpty()) {
            play(World, Where, DoorSounds.KeycardDeny.get(), 0.9f, 1.0f);
            tell(User, "need_card", ChatFormatting.RED);
            return;
        }
        if (!((KeycardItem) Card.getItem()).isOmni()) {
            play(World, Where, DoorSounds.KeycardDeny.get(), 0.9f, 1.0f);
            tell(User, "omni_required", ChatFormatting.RED);
            return;
        }
        play(World, Where, DoorSounds.KeycardAccept.get(), 0.9f, 1.0f);
        if (Unlocked) {
            tell(User, "already_unlocked", ChatFormatting.GREEN);
            return;
        }
        Unlocked = true;
        Note = NoteNone;
        changed();
        tell(User, "unlocked", ChatFormatting.GREEN);
    }

    private void throwLever(ServerLevel World, Player User) {
        Vec3 Where = WarheadGeometry.LeverPivot;
        if (Deadline >= 0L) {
            refuse(World, Where, User, "lever_in_progress");
            return;
        }
        if (!Unlocked) {
            refuse(World, Where, User, "lever_sealed");
            return;
        }
        if (!Lever && Linked == 0) {
            refuse(World, Where, User, "no_warhead");
            return;
        }
        Lever = !Lever;
        if (!Lever && Cover) {
            Cover = false;
            play(World, WarheadGeometry.CoverHinge, WarheadRegistry.CoverClose.get(), 0.8f, 1.0f);
        }
        changed();
        play(World, Where, WarheadRegistry.LeverClunk.get(), 0.9f, 1.0f);
        play(World, Where, WarheadRegistry.LeverClick.get(), 0.8f, 1.0f);
        play(World, Where, Lever ? WarheadRegistry.Armed.get() : WarheadRegistry.Disarmed.get(), 0.8f, 1.0f);
        tell(User, Lever ? "armed" : "disarmed", Lever ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        DiscordNotices.warheadLever(World, worldPosition, User, Lever, Linked);
    }

    private void flipCover(ServerLevel World, Player User) {
        Vec3 Where = WarheadGeometry.CoverHinge;
        if (Deadline >= 0L) {
            refuse(World, Where, User, "cover_in_progress");
            return;
        }
        if (!Lever) {
            refuse(World, Where, User, "cover_locked");
            return;
        }
        Cover = !Cover;
        changed();
        play(World, Where, Cover ? WarheadRegistry.CoverOpen.get() : WarheadRegistry.CoverClose.get(), 0.9f, 1.0f);
        if (Cover) {
            tell(User, "cover_open", ChatFormatting.GOLD);
        }
    }

    private void pressButton(ServerLevel World, Player User) {
        Vec3 Where = WarheadGeometry.ButtonOrigin;
        Presses++;
        changed();
        play(World, Where, WarheadRegistry.ButtonPress.get(), 1.0f, 1.0f);
        if (Deadline >= 0L) {
            return;
        }
        if (!Unlocked || !Lever) {
            refuse(World, Where, User, "lever_sealed");
            return;
        }
        WarheadNetwork Network = WarheadNetwork.get(World);
        List<BlockPos> Targets = Network.warheadsNear(World, worldPosition, WarheadNetwork.LinkRange);
        if (Targets.isEmpty()) {
            refuse(World, Where, User, "no_warhead");
            return;
        }
        if (Network.isBusy(Targets)) {
            refuse(World, Where, User, "busy");
            return;
        }
        WarheadSequence Sequence = Network.start(World, worldPosition, Targets);
        Deadline = Sequence.getDeadline();
        Locked = false;
        Foreign = false;
        Note = NoteNone;
        changed();
        play(World, Where, WarheadRegistry.Engage.get(), 1.0f, 1.0f);
        tell(User, "engaged", ChatFormatting.RED, Targets.size());
        DiscordNotices.warheadEngaged(World, worldPosition, User, Targets.size(), Sequence.getDeadline());
        LastComparator = comparatorLevel(World.getGameTime());
        WarheadPanelBlock.updateAll(World, worldPosition, getBlockState());
    }

    private void pressAbort(ServerLevel World, Player User) {
        Vec3 Where = WarheadGeometry.AbortOrigin;
        Aborts++;
        changed();
        play(World, Where, WarheadRegistry.AbortPress.get(), 0.9f, 1.0f);
        WarheadNetwork Network = WarheadNetwork.get(World);
        Optional<WarheadSequence> Own = Network.sequenceOf(worldPosition);
        if (Own.isEmpty()) {
            tell(User, Foreign ? "foreign" : "no_sequence", ChatFormatting.GRAY);
            return;
        }
        if (Own.get().isLocked()) {
            refuse(World, Where, User, "cancel_locked");
            return;
        }
        if (Network.cancel(World, worldPosition)) {
            play(World, Where, WarheadRegistry.Cancel.get(), 1.0f, 1.0f);
            tell(User, "cancelled", ChatFormatting.GREEN);
            DiscordNotices.warheadCancelled(World, worldPosition, User);
        }
    }

    private void report(Player User) {
        Component Stage = Component.translatable("jade.scp_unleashed.warhead.stage." + stage().getSerializedName());
        tell(User, "panel_status", ChatFormatting.GRAY, Stage, Linked);
    }

    private void tickClient() {
        float LeverTarget = Lever ? 1.0f : -1.0f;
        float CoverTarget = Cover ? 1.0f : 0.0f;
        if (!Primed) {
            Primed = true;
            LeverNow = LeverTarget;
            LeverOld = LeverTarget;
            CoverNow = CoverTarget;
            CoverOld = CoverTarget;
            SeenPresses = Presses;
            SeenAborts = Aborts;
        }
        LeverOld = LeverNow;
        LeverNow = approach(LeverNow, LeverTarget, LeverSpeed * 2.0f);
        CoverOld = CoverNow;
        CoverNow = approach(CoverNow, CoverTarget, CoverSpeed);
        if (Presses != SeenPresses) {
            SeenPresses = Presses;
            ButtonTime = PressTicks;
        } else if (ButtonTime > 0) {
            ButtonTime--;
        }
        if (Aborts != SeenAborts) {
            SeenAborts = Aborts;
            AbortTime = PressTicks;
        } else if (AbortTime > 0) {
            AbortTime--;
        }
    }

    private static float approach(float Current, float Target, float Step) {
        if (Current < Target) {
            return Math.min(Target, Current + Step);
        }
        return Math.max(Target, Current - Step);
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public float leverAngle(float PartialTick) {
        float Blend = (Mth.lerp(PartialTick, LeverOld, LeverNow) + 1.0f) * 0.5f;
        return (smooth(Blend) * 2.0f - 1.0f) * WarheadGeometry.LeverAngle;
    }

    public float coverAngle(float PartialTick) {
        return smooth(Mth.lerp(PartialTick, CoverOld, CoverNow)) * WarheadGeometry.CoverOpenAngle;
    }

    public float buttonDepth(float PartialTick) {
        return ButtonTime <= 0 ? 0.0f : WarheadGeometry.ButtonTravel * Mth.clamp((ButtonTime - PartialTick) / 3.0f, 0.0f, 1.0f);
    }

    public float abortDepth(float PartialTick) {
        return AbortTime <= 0 ? 0.0f : WarheadGeometry.AbortTravel * Mth.clamp((AbortTime - PartialTick) / 3.0f, 0.0f, 1.0f);
    }

    public boolean isUnlocked() {
        return Unlocked;
    }

    public boolean isLeverThrown() {
        return Lever;
    }

    public boolean isCoverOpen() {
        return Cover;
    }

    public boolean isLocked() {
        return Locked;
    }

    public boolean isForeign() {
        return Foreign;
    }

    public int getLinked() {
        return Linked;
    }

    public int noteAt(long Now) {
        return Now < NoteUntil ? Note : NoteNone;
    }

    public AABB getRenderBounds() {
        Direction Front = front();
        BlockPos First = WarheadGeometry.cellPos(worldPosition, Front, 0, WarheadGeometry.PanelWidth, WarheadGeometry.PanelHeight);
        BlockPos Last = WarheadGeometry.cellPos(worldPosition, Front, WarheadGeometry.PanelCells - 1, WarheadGeometry.PanelWidth, WarheadGeometry.PanelHeight);
        return new AABB(First).minmax(new AABB(Last)).inflate(1.0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel ServerWorld) {
            LastStage = null;
            WarheadNetwork.get(ServerWorld).setPanel(worldPosition, stage());
        }
    }

    private void write(CompoundTag Tag) {
        Tag.putBoolean("Unlocked", Unlocked);
        Tag.putBoolean("Lever", Lever);
        Tag.putBoolean("Cover", Cover);
        Tag.putInt("Linked", Linked);
        Tag.putLong("Deadline", Deadline);
        Tag.putBoolean("Locked", Locked);
        Tag.putBoolean("Foreign", Foreign);
        Tag.putInt("Note", Note);
        Tag.putLong("NoteUntil", NoteUntil);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        write(Tag);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Unlocked = Tag.getBoolean("Unlocked");
        Lever = Tag.getBoolean("Lever");
        Cover = Tag.getBoolean("Cover");
        Linked = Tag.getInt("Linked");
        Deadline = Tag.contains("Deadline") ? Tag.getLong("Deadline") : -1L;
        Locked = Tag.getBoolean("Locked");
        Foreign = Tag.getBoolean("Foreign");
        Note = Tag.getInt("Note");
        NoteUntil = Tag.getLong("NoteUntil");
        if (Tag.contains("Presses")) {
            Presses = Tag.getInt("Presses");
            Aborts = Tag.getInt("Aborts");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        write(Tag);
        Tag.putInt("Presses", Presses);
        Tag.putInt("Aborts", Aborts);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
