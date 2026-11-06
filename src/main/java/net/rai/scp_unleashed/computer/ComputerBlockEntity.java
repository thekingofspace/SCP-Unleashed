package net.rai.scp_unleashed.computer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.computer.data.ActionStep;
import net.rai.scp_unleashed.computer.data.ActionTrigger;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.data.ComputerText;
import net.rai.scp_unleashed.computer.data.RedstoneButton;
import net.rai.scp_unleashed.computer.data.ScheduledAction;
import net.rai.scp_unleashed.computer.data.SoundEntry;
import net.rai.scp_unleashed.computer.net.ChatService;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.computer.net.ComputerSection;
import net.rai.scp_unleashed.computer.net.ComputerSessions;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireMessage;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ComputerBlockEntity extends ComputerDeviceEntity {
    public static final int Draw = 4;
    public static final int MaxFiles = 24;
    public static final int MaxButtons = 14;
    public static final int MaxSounds = 16;
    public static final int MaxActions = 12;
    public static final int Heartbeat = 10;
    public static final int SignalTimeout = 30;
    public static final int FullSignal = 15;
    public static final int FlashTicks = 30;
    public static final long Forever = Long.MAX_VALUE;
    public static final long TicksPerMinute = 1200L;
    public static final int MaxJumpMinutes = 720;
    private static final Logger Log = LogUtils.getLogger();

    private final List<ComputerFile> Files = new ArrayList<>();
    private final List<RedstoneButton> Buttons = new ArrayList<>();
    private final List<SoundEntry> Sounds = new ArrayList<>();
    private final List<ScheduledAction> Actions = new ArrayList<>();
    private final Map<String, Hold> Holds = new LinkedHashMap<>();
    private final Map<Integer, Integer> Sent = new HashMap<>();
    private final Set<Integer> Playing = new LinkedHashSet<>();
    private final Map<Integer, Long> Flashes = new HashMap<>();
    private final Map<Integer, ActionState> States = new HashMap<>();
    private final Map<Integer, Map<BlockPos, Heard>> Inputs = new HashMap<>();
    private final Map<Integer, Integer> InputLevels = new HashMap<>();
    private List<String> Subscriptions = List.of();
    private int NextId = 1;
    private int LastClock = -1;
    private int Dirty;

    public ComputerBlockEntity(BlockPos Pos, BlockState State) {
        super(ComputerRegistry.ComputerEntity.get(), Pos, State);
    }

    @Override
    public int getDraw() {
        return Draw;
    }

    public void markDirty(ComputerSection... Sections) {
        for (ComputerSection Section : Sections) {
            Dirty |= Section.bit();
        }
        setChanged();
    }

    @Override
    protected void powerChanged(ServerLevel World, boolean Running) {
        if (Running) {
            LastClock = -1;
            World.playSound(null, worldPosition, ComputerRegistry.Boot.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
            World.playSound(null, worldPosition, ComputerRegistry.Beep.get(), SoundSource.BLOCKS, 0.45f, 1.0f);
            Dirty = ComputerSection.All;
        } else {
            World.playSound(null, worldPosition, ComputerRegistry.Shutdown.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
            releaseOutputs(World);
            ComputerSessions.closeAll(World, worldPosition, ComputerSessions.CloseReason.Power);
        }
    }

    @Override
    protected void tickDevice(ServerLevel World, long Now) {
        if (!isRunning()) {
            return;
        }
        expire(Now);
        listenInputs(Now);
        tickScheduler(World, Now);
        tickBus(World, Now);
        if (Dirty != 0) {
            int Sections = Dirty;
            Dirty = 0;
            ComputerSessions.touch(World, worldPosition, Sections);
        }
    }

    private void expire(long Now) {
        Iterator<Map.Entry<String, Hold>> Each = Holds.entrySet().iterator();
        while (Each.hasNext()) {
            Map.Entry<String, Hold> Entry = Each.next();
            if (Entry.getValue().Until() <= Now) {
                Each.remove();
                Dirty |= Entry.getKey().startsWith("b") ? ComputerSection.Redstone.bit() : ComputerSection.Scheduler.bit();
            }
        }
        if (Flashes.values().removeIf(Until -> Until <= Now)) {
            Dirty |= ComputerSection.Sounds.bit();
        }
    }

    private void tickBus(ServerLevel World, long Now) {
        Map<Integer, Integer> Want = new HashMap<>();
        for (Hold Held : Holds.values()) {
            Want.merge(Held.Port(), Held.Signal(), Math::max);
        }
        Set<Integer> Ports = new HashSet<>(Want.keySet());
        Ports.addAll(Sent.keySet());
        boolean Beat = Math.floorMod(Now + worldPosition.asLong(), Heartbeat) == 0;
        for (int Port : Ports) {
            int Signal = Want.getOrDefault(Port, 0);
            Integer Last = Sent.get(Port);
            if (Last == null || Last != Signal) {
                sendSignal(World, Port, Signal);
                if (Signal > 0) {
                    Sent.put(Port, Signal);
                } else {
                    Sent.remove(Port);
                }
            } else if (Signal > 0 && Beat) {
                sendSignal(World, Port, Signal);
            }
        }
    }

    public void releaseOutputs(ServerLevel World) {
        for (int Port : List.copyOf(Sent.keySet())) {
            sendSignal(World, Port, 0);
        }
        Sent.clear();
    }

    private void sendSignal(ServerLevel World, int Port, int Signal) {
        CompoundTag Payload = new CompoundTag();
        Payload.putInt("Signal", Signal);
        send(World, WireChannels.redstonePort(Port), Payload);
    }

    private void send(ServerLevel World, String Channel, CompoundTag Payload) {
        WireNetworkManager.get(World).sendFromDevice(World, worldPosition, Channel, Payload);
    }

    @Override
    public Collection<String> getSubscriptions() {
        return Subscriptions;
    }

    private void refreshSubscriptions() {
        Set<String> Wanted = new LinkedHashSet<>();
        for (ScheduledAction Action : Actions) {
            for (ActionTrigger Trigger : Action.Triggers()) {
                if (Trigger.Type() == ActionTrigger.Kind.Redstone) {
                    Wanted.add(WireChannels.redstonePort(Trigger.Port()));
                }
            }
        }
        for (SoundEntry Entry : Sounds) {
            if (Playing.contains(Entry.Id())) {
                Wanted.add(Entry.channel());
            }
        }
        Subscriptions = List.copyOf(Wanted);
        Inputs.keySet().removeIf(Port -> !Wanted.contains(WireChannels.redstonePort(Port)));
    }

    @Override
    public void receiveMessage(WireMessage Message) {
        if (level == null) {
            return;
        }
        String Channel = Message.Channel();
        int Port = WireChannels.portOf(Channel);
        if (Port < 0) {
            return;
        }
        if (Channel.equals(WireChannels.redstonePort(Port))) {
            int Signal = Mth.clamp(Message.Payload().getInt("Signal"), 0, FullSignal);
            Map<BlockPos, Heard> Sources = Inputs.computeIfAbsent(Port, Key -> new HashMap<>());
            if (Signal > 0) {
                Sources.put(Message.Source(), new Heard(Signal, level.getGameTime()));
            } else {
                Sources.remove(Message.Source());
            }
        } else if (Channel.equals(SoundEntry.channelOf(Port))) {
            boolean Changed = false;
            for (SoundEntry Entry : Sounds) {
                if (Entry.Port() == Port) {
                    Changed |= Playing.remove(Entry.Id());
                }
            }
            if (Changed) {
                refreshSubscriptions();
                markDirty(ComputerSection.Sounds);
            }
        }
    }

    private void listenInputs(long Now) {
        InputLevels.clear();
        for (Map.Entry<Integer, Map<BlockPos, Heard>> Entry : Inputs.entrySet()) {
            Entry.getValue().values().removeIf(Signal -> Now - Signal.Seen() > SignalTimeout);
            int Best = 0;
            for (Heard Signal : Entry.getValue().values()) {
                Best = Math.max(Best, Signal.Signal());
            }
            if (Best > 0) {
                InputLevels.put(Entry.getKey(), Best);
            }
        }
    }

    public int inputLevel(int Port) {
        return InputLevels.getOrDefault(Port, 0);
    }

    public List<ComputerFile> getFiles() {
        return List.copyOf(Files);
    }

    public boolean hasRoom() {
        return Files.size() < MaxFiles;
    }

    public boolean hasCopy(ComputerFile File) {
        for (ComputerFile Existing : Files) {
            if (Existing.sameContent(File)) {
                return true;
            }
        }
        return false;
    }

    public boolean addFile(ComputerFile File) {
        if (!hasRoom()) {
            return false;
        }
        Files.add(File);
        markDirty(ComputerSection.Files);
        return true;
    }

    @Nullable
    public ComputerFile file(int Index) {
        return Index >= 0 && Index < Files.size() ? Files.get(Index) : null;
    }

    public boolean deleteFile(int Index) {
        if (Index < 0 || Index >= Files.size()) {
            return false;
        }
        Files.remove(Index);
        markDirty(ComputerSection.Files);
        return true;
    }

    public boolean renameFile(int Index, String Name) {
        ComputerFile Current = file(Index);
        if (Current == null) {
            return false;
        }
        Files.set(Index, Current.renamed(Name));
        markDirty(ComputerSection.Files);
        return true;
    }

    public boolean saveNote(int Index, String Name, String Text) {
        if (Index < 0) {
            return addFile(ComputerFile.note(Name, Text));
        }
        ComputerFile Current = file(Index);
        if (Current == null || Current.isDossier()) {
            return false;
        }
        Files.set(Index, Current.edited(Name, Text));
        markDirty(ComputerSection.Files);
        return true;
    }

    public List<RedstoneButton> getButtons() {
        return List.copyOf(Buttons);
    }

    @Nullable
    public RedstoneButton button(int Id) {
        for (RedstoneButton Button : Buttons) {
            if (Button.Id() == Id) {
                return Button;
            }
        }
        return null;
    }

    public boolean isButtonOn(int Id) {
        Hold Held = Holds.get("b" + Id);
        return Held != null && level != null && Held.Until() > level.getGameTime();
    }

    public long buttonRemaining(int Id) {
        Hold Held = Holds.get("b" + Id);
        if (Held == null || level == null || Held.Until() == Forever) {
            return 0L;
        }
        return Math.max(0L, Held.Until() - level.getGameTime());
    }

    public boolean saveButton(RedstoneButton Requested) {
        RedstoneButton Existing = button(Requested.Id());
        if (Existing == null) {
            if (Buttons.size() >= MaxButtons) {
                return false;
            }
            Buttons.add(Requested.withId(NextId++));
        } else {
            Buttons.set(Buttons.indexOf(Existing), Requested);
            Hold Held = Holds.get("b" + Requested.Id());
            if (Held != null) {
                if (Requested.Kind() == RedstoneButton.Mode.Toggle && Held.Until() != Forever) {
                    Holds.remove("b" + Requested.Id());
                } else {
                    Holds.put("b" + Requested.Id(), new Hold(Requested.Port(), FullSignal, Held.Until()));
                }
            }
        }
        markDirty(ComputerSection.Redstone, ComputerSection.Scheduler);
        return true;
    }

    public boolean deleteButton(int Id) {
        RedstoneButton Existing = button(Id);
        if (Existing == null) {
            return false;
        }
        Buttons.remove(Existing);
        Holds.remove("b" + Id);
        markDirty(ComputerSection.Redstone);
        return true;
    }

    public boolean pressButton(ServerLevel World, int Id) {
        RedstoneButton Button = button(Id);
        if (Button == null) {
            return false;
        }
        long Now = World.getGameTime();
        String Key = "b" + Id;
        if (Button.Kind() == RedstoneButton.Mode.Toggle) {
            if (isButtonOn(Id)) {
                Holds.remove(Key);
            } else {
                Holds.put(Key, new Hold(Button.Port(), FullSignal, Forever));
            }
        } else {
            Holds.put(Key, new Hold(Button.Port(), FullSignal, Now + Button.Seconds() * 20L));
        }
        World.playSound(null, worldPosition, ComputerRegistry.Toggle.get(), SoundSource.BLOCKS, 0.5f, isButtonOn(Id) ? 1.2f : 0.9f);
        markDirty(ComputerSection.Redstone);
        return true;
    }

    public List<SoundEntry> getSounds() {
        return List.copyOf(Sounds);
    }

    @Nullable
    public SoundEntry sound(int Id) {
        for (SoundEntry Entry : Sounds) {
            if (Entry.Id() == Id) {
                return Entry;
            }
        }
        return null;
    }

    public boolean isPlaying(int Id) {
        return Playing.contains(Id) || Flashes.containsKey(Id);
    }

    public boolean saveSound(SoundEntry Requested) {
        SoundEntry Existing = sound(Requested.Id());
        if (Existing == null) {
            if (Sounds.size() >= MaxSounds) {
                return false;
            }
            Sounds.add(Requested.withId(NextId++));
        } else {
            Sounds.set(Sounds.indexOf(Existing), Requested);
            if (Playing.contains(Requested.Id()) && (Existing.Port() != Requested.Port() || !Requested.Loop())) {
                if (level instanceof ServerLevel World) {
                    send(World, Existing.channel(), SoundMessages.stopPayload());
                }
                Playing.remove(Requested.Id());
            }
        }
        refreshSubscriptions();
        markDirty(ComputerSection.Sounds, ComputerSection.Scheduler);
        return true;
    }

    public boolean deleteSound(int Id) {
        SoundEntry Existing = sound(Id);
        if (Existing == null) {
            return false;
        }
        if (Playing.remove(Id) && level instanceof ServerLevel World) {
            send(World, Existing.channel(), SoundMessages.stopPayload());
        }
        Sounds.remove(Existing);
        Flashes.remove(Id);
        refreshSubscriptions();
        markDirty(ComputerSection.Sounds, ComputerSection.Scheduler);
        return true;
    }

    public boolean pressSound(ServerLevel World, int Id) {
        SoundEntry Entry = sound(Id);
        if (Entry == null) {
            return false;
        }
        if (Entry.Loop() && Playing.contains(Id)) {
            stopSound(World, Entry);
        } else {
            startSound(World, Entry);
        }
        return true;
    }

    public void startSound(ServerLevel World, SoundEntry Entry) {
        send(World, Entry.channel(), SoundMessages.payload(Entry.Sound(), Entry.Volume(), Entry.Pitch(), Entry.Loop()));
        clearPort(Entry.Port());
        if (Entry.Loop()) {
            Playing.add(Entry.Id());
        } else {
            Flashes.put(Entry.Id(), World.getGameTime() + FlashTicks);
        }
        refreshSubscriptions();
        markDirty(ComputerSection.Sounds);
    }

    public void stopSound(ServerLevel World, SoundEntry Entry) {
        send(World, Entry.channel(), SoundMessages.stopPayload());
        clearPort(Entry.Port());
        refreshSubscriptions();
        markDirty(ComputerSection.Sounds);
    }

    private void clearPort(int Port) {
        for (SoundEntry Other : Sounds) {
            if (Other.Port() == Port) {
                Playing.remove(Other.Id());
            }
        }
    }

    public List<ScheduledAction> getActions() {
        return List.copyOf(Actions);
    }

    @Nullable
    public ScheduledAction action(int Id) {
        for (ScheduledAction Action : Actions) {
            if (Action.Id() == Id) {
                return Action;
            }
        }
        return null;
    }

    public boolean isActive(int Id) {
        ActionState State = States.get(Id);
        return State != null && State.Active;
    }

    public long activeFor(int Id) {
        ActionState State = States.get(Id);
        return State == null || level == null ? 0L : Math.max(0L, level.getGameTime() - State.ChangedAt);
    }

    public boolean saveAction(ScheduledAction Requested) {
        ScheduledAction Existing = action(Requested.Id());
        if (Existing == null) {
            if (Actions.size() >= MaxActions) {
                return false;
            }
            ScheduledAction Created = Requested.withId(NextId++);
            Actions.add(Created);
            state(Created.Id());
        } else {
            Actions.set(Actions.indexOf(Existing), Requested);
            keepMatchingHolds(Requested);
            ActionState State = state(Requested.Id());
            State.NextAt.clear();
            State.LastLevels.clear();
        }
        refreshSubscriptions();
        markDirty(ComputerSection.Scheduler);
        return true;
    }

    private void keepMatchingHolds(ScheduledAction Action) {
        String Prefix = "a" + Action.Id() + ":";
        Iterator<Map.Entry<String, Hold>> Each = Holds.entrySet().iterator();
        while (Each.hasNext()) {
            Map.Entry<String, Hold> Entry = Each.next();
            if (!Entry.getKey().startsWith(Prefix)) {
                continue;
            }
            String[] Parts = Entry.getKey().split(":");
            if (Parts.length != 3) {
                Each.remove();
                continue;
            }
            List<ActionStep> Steps = Action.steps("on".equals(Parts[1]));
            int Index;
            try {
                Index = Integer.parseInt(Parts[2]);
            } catch (NumberFormatException Problem) {
                Each.remove();
                continue;
            }
            if (Index < 0 || Index >= Steps.size() || Steps.get(Index).Type() != ActionStep.Kind.Redstone || Steps.get(Index).Port() != Entry.getValue().Port()) {
                Each.remove();
            }
        }
    }

    public boolean deleteAction(int Id) {
        ScheduledAction Existing = action(Id);
        if (Existing == null) {
            return false;
        }
        Actions.remove(Existing);
        States.remove(Id);
        Holds.keySet().removeIf(Key -> Key.startsWith("a" + Id + ":"));
        refreshSubscriptions();
        markDirty(ComputerSection.Scheduler);
        return true;
    }

    public boolean runAction(ServerLevel World, int Id, boolean On) {
        ScheduledAction Action = action(Id);
        if (Action == null) {
            return false;
        }
        setActive(World, Action, On, World.getGameTime());
        return true;
    }

    private ActionState state(int Id) {
        return States.computeIfAbsent(Id, Key -> new ActionState(level == null ? 0L : level.getGameTime()));
    }

    private void tickScheduler(ServerLevel World, long Now) {
        int Clock = ComputerText.clockMinute(World.getDayTime());
        boolean Moved = LastClock >= 0 && Clock != LastClock;
        for (ScheduledAction Action : List.copyOf(Actions)) {
            if (action(Action.Id()) == null) {
                continue;
            }
            ActionState State = state(Action.Id());
            List<ActionTrigger> Triggers = Action.Triggers();
            for (int Index = 0; Index < Triggers.size(); Index++) {
                ActionTrigger Trigger = Triggers.get(Index);
                switch (Trigger.Type()) {
                    case Time -> {
                        if (Moved && crossed(LastClock, Clock, Trigger.Value())) {
                            fire(World, Action, State, Trigger.When(), Now);
                        }
                    }
                    case Every -> {
                        long Period = Trigger.Value() * TicksPerMinute;
                        Long Next = State.NextAt.get(Index);
                        if (Next == null || Next - Now > Period) {
                            State.NextAt.put(Index, Now + Period);
                        } else if (Now >= Next) {
                            State.NextAt.put(Index, Now + Period);
                            fire(World, Action, State, Trigger.When(), Now);
                        }
                    }
                    case After -> {
                        if (Now - State.ChangedAt >= Trigger.Value() * 20L) {
                            switch (Trigger.When()) {
                                case Activate -> {
                                    if (!State.Active) {
                                        setActive(World, Action, true, Now);
                                    }
                                }
                                case Deactivate -> {
                                    if (State.Active) {
                                        setActive(World, Action, false, Now);
                                    }
                                }
                                default -> setActive(World, Action, !State.Active, Now);
                            }
                        }
                    }
                    case Redstone -> {
                        int Strength = inputLevel(Trigger.Port());
                        Integer Was = State.LastLevels.put(Index, Strength);
                        if (Was == null) {
                            continue;
                        }
                        boolean Rising = Was == 0 && Strength > 0;
                        boolean Falling = Was > 0 && Strength == 0;
                        if (Trigger.When() == ActionTrigger.Event.Follow) {
                            if (Rising) {
                                setActive(World, Action, true, Now);
                            } else if (Falling) {
                                setActive(World, Action, false, Now);
                            }
                        } else if (Rising) {
                            fire(World, Action, State, Trigger.When(), Now);
                        }
                    }
                }
                if (action(Action.Id()) == null) {
                    break;
                }
            }
        }
        LastClock = Clock;
    }

    private static boolean crossed(int Last, int Now, int Target) {
        int Moved = Math.floorMod(Now - Last, ComputerText.MinutesPerDay);
        if (Moved == 0 || Moved > MaxJumpMinutes) {
            return false;
        }
        int Ahead = Math.floorMod(Target - Last, ComputerText.MinutesPerDay);
        return Ahead > 0 && Ahead <= Moved;
    }

    private void fire(ServerLevel World, ScheduledAction Action, ActionState State, ActionTrigger.Event When, long Now) {
        switch (When) {
            case Activate -> setActive(World, Action, true, Now);
            case Deactivate -> setActive(World, Action, false, Now);
            default -> setActive(World, Action, !State.Active, Now);
        }
    }

    private void setActive(ServerLevel World, ScheduledAction Action, boolean On, long Now) {
        ActionState State = state(Action.Id());
        if (State.Active == On) {
            return;
        }
        State.Active = On;
        State.ChangedAt = Now;
        String Released = "a" + Action.Id() + (On ? ":off:" : ":on:");
        Holds.entrySet().removeIf(Entry -> Entry.getKey().startsWith(Released) && Entry.getValue().Until() == Forever);
        List<ActionStep> Steps = Action.steps(On);
        ComputerGrid.Reach Reach = null;
        for (int Index = 0; Index < Steps.size(); Index++) {
            ActionStep Step = Steps.get(Index);
            switch (Step.Type()) {
                case Message -> {
                    if (Step.Text().isEmpty()) {
                        continue;
                    }
                    if (Reach == null) {
                        Reach = ComputerGrid.reach(World, worldPosition);
                    }
                    ChatMessage Draft = new ChatMessage(0L, ChatMessage.Sender.System, Action.Title(), Step.Text(), "", Optional.empty(), World.getDayTime(), false);
                    ChatService.broadcast(World, Reach, Step.Target(), Draft);
                }
                case Redstone -> {
                    String Key = "a" + Action.Id() + (On ? ":on:" : ":off:") + Index;
                    Holds.put(Key, new Hold(Step.Port(), FullSignal, Step.Timed() ? Now + Step.Seconds() * 20L : Forever));
                }
                case Sound -> {
                    SoundEntry Entry = sound(Step.SoundId());
                    if (Entry != null) {
                        if (Step.Start()) {
                            startSound(World, Entry);
                        } else {
                            stopSound(World, Entry);
                        }
                    }
                }
            }
        }
        World.playSound(null, worldPosition, ComputerRegistry.Toggle.get(), SoundSource.BLOCKS, 0.4f, On ? 1.3f : 0.8f);
        markDirty(ComputerSection.Scheduler, ComputerSection.Redstone);
    }

    public int holdCount(int ActionId) {
        int Count = 0;
        for (String Key : Holds.keySet()) {
            if (Key.startsWith("a" + ActionId + ":")) {
                Count++;
            }
        }
        return Count;
    }

    private <T> ListTag encodeList(Codec<T> Codec, List<T> Values, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (T Value : Values) {
            Codec.encodeStart(Registries.createSerializationContext(NbtOps.INSTANCE), Value).result().ifPresent(Saved::add);
        }
        return Saved;
    }

    private <T> void decodeList(Codec<T> Codec, ListTag Saved, List<T> Into, int Limit, HolderLookup.Provider Registries) {
        Into.clear();
        for (int Index = 0; Index < Saved.size() && Into.size() < Limit; Index++) {
            Codec.parse(Registries.createSerializationContext(NbtOps.INSTANCE), Saved.get(Index))
                    .resultOrPartial(Problem -> Log.warn("Skipping damaged computer data: {}", Problem))
                    .ifPresent(Into::add);
        }
    }

    private CompoundTag saveStorage(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.put("Files", encodeList(ComputerFile.FileCodec, Files, Registries));
        Tag.put("Buttons", encodeList(RedstoneButton.ButtonCodec, Buttons, Registries));
        Tag.put("Sounds", encodeList(SoundEntry.EntryCodec, Sounds, Registries));
        Tag.put("Actions", encodeList(ScheduledAction.ActionCodec, Actions, Registries));
        Tag.putInt("NextId", NextId);
        return Tag;
    }

    private void loadStorage(CompoundTag Tag, HolderLookup.Provider Registries) {
        decodeList(ComputerFile.FileCodec, Tag.getList("Files", Tag.TAG_COMPOUND), Files, MaxFiles, Registries);
        decodeList(RedstoneButton.ButtonCodec, Tag.getList("Buttons", Tag.TAG_COMPOUND), Buttons, MaxButtons, Registries);
        decodeList(SoundEntry.EntryCodec, Tag.getList("Sounds", Tag.TAG_COMPOUND), Sounds, MaxSounds, Registries);
        decodeList(ScheduledAction.ActionCodec, Tag.getList("Actions", Tag.TAG_COMPOUND), Actions, MaxActions, Registries);
        NextId = Math.max(1, Tag.getInt("NextId"));
        for (RedstoneButton Button : Buttons) {
            NextId = Math.max(NextId, Button.Id() + 1);
        }
        for (SoundEntry Entry : Sounds) {
            NextId = Math.max(NextId, Entry.Id() + 1);
        }
        for (ScheduledAction Action : Actions) {
            NextId = Math.max(NextId, Action.Id() + 1);
        }
        refreshSubscriptions();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Storage", saveStorage(Registries));
        ListTag SavedHolds = new ListTag();
        for (Map.Entry<String, Hold> Entry : Holds.entrySet()) {
            CompoundTag Held = new CompoundTag();
            Held.putString("Key", Entry.getKey());
            Held.putInt("Port", Entry.getValue().Port());
            Held.putInt("Signal", Entry.getValue().Signal());
            Held.putLong("Until", Entry.getValue().Until());
            SavedHolds.add(Held);
        }
        Tag.put("Holds", SavedHolds);
        Tag.putIntArray("Playing", List.copyOf(Playing));
        ListTag SavedStates = new ListTag();
        for (Map.Entry<Integer, ActionState> Entry : States.entrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putInt("Id", Entry.getKey());
            Saved.putBoolean("Active", Entry.getValue().Active);
            Saved.putLong("Changed", Entry.getValue().ChangedAt);
            SavedStates.add(Saved);
        }
        Tag.put("States", SavedStates);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Storage", Tag.TAG_COMPOUND)) {
            loadStorage(Tag.getCompound("Storage"), Registries);
        }
        if (Tag.contains("Holds", Tag.TAG_LIST)) {
            Holds.clear();
            ListTag SavedHolds = Tag.getList("Holds", Tag.TAG_COMPOUND);
            for (int Index = 0; Index < SavedHolds.size(); Index++) {
                CompoundTag Held = SavedHolds.getCompound(Index);
                Holds.put(Held.getString("Key"), new Hold(WireChannels.clampPort(Held.getInt("Port")), Mth.clamp(Held.getInt("Signal"), 0, FullSignal), Held.getLong("Until")));
            }
        }
        if (Tag.contains("Playing", Tag.TAG_INT_ARRAY)) {
            Playing.clear();
            for (int Id : Tag.getIntArray("Playing")) {
                Playing.add(Id);
            }
        }
        if (Tag.contains("States", Tag.TAG_LIST)) {
            States.clear();
            ListTag SavedStates = Tag.getList("States", Tag.TAG_COMPOUND);
            for (int Index = 0; Index < SavedStates.size(); Index++) {
                CompoundTag Saved = SavedStates.getCompound(Index);
                ActionState State = new ActionState(Saved.getLong("Changed"));
                State.Active = Saved.getBoolean("Active");
                States.put(Saved.getInt("Id"), State);
            }
        }
        refreshSubscriptions();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (level != null && (!Files.isEmpty() || !Buttons.isEmpty() || !Sounds.isEmpty() || !Actions.isEmpty())) {
            Components.set(ComputerRegistry.StorageComponent.get(), CustomData.of(saveStorage(level.registryAccess())));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        CustomData Stored = Input.get(ComputerRegistry.StorageComponent.get());
        if (Stored != null && level != null) {
            loadStorage(Stored.copyTag(), level.registryAccess());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag Tag) {
        super.removeComponentsFromTag(Tag);
        Tag.remove("Storage");
    }

    public static int storedCount(CustomData Stored, String Key) {
        return Stored.copyTag().getList(Key, Tag.TAG_COMPOUND).size();
    }

    private record Hold(int Port, int Signal, long Until) {
    }

    private record Heard(int Signal, long Seen) {
    }

    private static final class ActionState {
        private final Map<Integer, Long> NextAt = new HashMap<>();
        private final Map<Integer, Integer> LastLevels = new HashMap<>();
        private boolean Active;
        private long ChangedAt;

        private ActionState(long ChangedAt) {
            this.ChangedAt = ChangedAt;
        }
    }
}
