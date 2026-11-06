package net.rai.scp_unleashed.warhead;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class WarheadNetwork extends SavedData {
    public static final String DataName = "scp_unleashed_warhead_network";
    public static final double LinkRange = 128.0;
    public static final double AlertRange = 256.0;
    public static final double KillRange = 100.0;
    public static final double BlastNoticeRange = 640.0;
    public static final int CountdownTicks = 1800;
    public static final int LockTicks = 200;
    public static final int TicketRadius = 2;
    public static final TicketType<ChunkPos> Ticket = TicketType.create("scp_unleashed_warhead", Comparator.comparingLong(ChunkPos::toLong), 100);
    private static final SavedData.Factory<WarheadNetwork> DataFactory = new SavedData.Factory<>(WarheadNetwork::new, WarheadNetwork::load);

    private final Set<BlockPos> Warheads = new HashSet<>();
    private final Map<BlockPos, PanelStage> Panels = new HashMap<>();
    private final Map<BlockPos, WarheadSequence> Sequences = new LinkedHashMap<>();
    private final Map<BlockPos, Integer> Outcomes = new HashMap<>();
    private final List<WarheadCrater> Craters = new ArrayList<>();

    public static WarheadNetwork get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public void addWarhead(BlockPos Pos) {
        if (Warheads.add(Pos.immutable())) {
            setDirty();
        }
    }

    public void removeWarhead(BlockPos Pos) {
        if (Warheads.remove(Pos)) {
            setDirty();
        }
    }

    public void setPanel(BlockPos Pos, PanelStage Stage) {
        if (Panels.put(Pos.immutable(), Stage) != Stage) {
            setDirty();
        }
    }

    public void removePanel(BlockPos Pos) {
        boolean Known = Panels.remove(Pos) != null;
        boolean Pending = Outcomes.remove(Pos) != null;
        if (Known || Pending) {
            setDirty();
        }
    }

    public int takeOutcome(BlockPos Panel) {
        Integer Found = Outcomes.remove(Panel);
        if (Found != null) {
            setDirty();
        }
        return Found == null ? 0 : Found;
    }

    private static boolean within(BlockPos First, BlockPos Second, double Range) {
        return First.distSqr(Second) <= Range * Range;
    }

    public List<BlockPos> warheadsNear(ServerLevel World, BlockPos Centre, double Range) {
        List<BlockPos> Found = new ArrayList<>();
        List<BlockPos> Stale = new ArrayList<>();
        for (BlockPos Warhead : Warheads) {
            if (!within(Warhead, Centre, Range)) {
                continue;
            }
            if (World.isLoaded(Warhead) && !AlphaWarheadBlock.isMaster(World.getBlockState(Warhead))) {
                Stale.add(Warhead);
                continue;
            }
            Found.add(Warhead);
        }
        if (!Stale.isEmpty()) {
            Stale.forEach(Warheads::remove);
            setDirty();
        }
        Found.sort(Comparator.comparingDouble(Warhead -> Warhead.distSqr(Centre)));
        return Found;
    }

    private boolean stalePanel(ServerLevel World, BlockPos Panel) {
        return World.isLoaded(Panel) && !WarheadPanelBlock.isMaster(World.getBlockState(Panel));
    }

    private void prunePanels(List<BlockPos> Stale) {
        if (!Stale.isEmpty()) {
            Stale.forEach(Panels::remove);
            setDirty();
        }
    }

    public int panelsNear(ServerLevel World, BlockPos Centre, double Range) {
        int Count = 0;
        List<BlockPos> Stale = new ArrayList<>();
        for (BlockPos Panel : Panels.keySet()) {
            if (!within(Panel, Centre, Range)) {
                continue;
            }
            if (stalePanel(World, Panel)) {
                Stale.add(Panel);
                continue;
            }
            Count++;
        }
        prunePanels(Stale);
        return Count;
    }

    public Collection<WarheadSequence> sequences() {
        return Sequences.values();
    }

    public Optional<WarheadSequence> sequenceOf(BlockPos Panel) {
        return Optional.ofNullable(Sequences.get(Panel));
    }

    public Optional<WarheadSequence> sequenceTargeting(BlockPos Warhead) {
        for (WarheadSequence Sequence : Sequences.values()) {
            if (Sequence.targets(Warhead)) {
                return Optional.of(Sequence);
            }
        }
        return Optional.empty();
    }

    public Optional<WarheadSequence> sequenceAffecting(ServerLevel World, BlockPos Panel) {
        WarheadSequence Own = Sequences.get(Panel);
        if (Own != null) {
            return Optional.of(Own);
        }
        for (WarheadSequence Sequence : Sequences.values()) {
            if (affects(Sequence, Panel)) {
                return Optional.of(Sequence);
            }
        }
        return Optional.empty();
    }

    private static boolean affects(WarheadSequence Sequence, BlockPos Panel) {
        if (Sequence.getPanel().equals(Panel)) {
            return true;
        }
        for (BlockPos Target : Sequence.getTargets()) {
            if (within(Target, Panel, LinkRange)) {
                return true;
            }
        }
        return false;
    }

    public WarheadStatus statusFor(ServerLevel World, BlockPos Warhead) {
        Optional<WarheadSequence> Running = sequenceTargeting(Warhead);
        if (Running.isPresent()) {
            return Running.get().isLocked() ? WarheadStatus.Locked : WarheadStatus.Counting;
        }
        WarheadStatus Result = WarheadStatus.Idle;
        List<BlockPos> Stale = new ArrayList<>();
        for (Map.Entry<BlockPos, PanelStage> Entry : Panels.entrySet()) {
            if (!within(Entry.getKey(), Warhead, LinkRange)) {
                continue;
            }
            if (stalePanel(World, Entry.getKey())) {
                Stale.add(Entry.getKey());
                continue;
            }
            if (Entry.getValue().isArmed()) {
                Result = WarheadStatus.Armed;
            }
        }
        prunePanels(Stale);
        return Result;
    }

    public int secondsLeftFor(ServerLevel World, BlockPos Warhead) {
        return sequenceTargeting(Warhead).map(Sequence -> (Sequence.ticksLeft(World.getGameTime()) + 19) / 20).orElse(0);
    }

    public void refreshNear(ServerLevel World, BlockPos Centre) {
        for (BlockPos Warhead : List.copyOf(Warheads)) {
            if (within(Warhead, Centre, LinkRange)) {
                refresh(World, Warhead);
            }
        }
    }

    private void refresh(ServerLevel World, BlockPos Warhead) {
        if (!World.isLoaded(Warhead)) {
            return;
        }
        BlockState State = World.getBlockState(Warhead);
        if (AlphaWarheadBlock.isMaster(State)) {
            WarheadStatus Wanted = statusFor(World, Warhead);
            if (State.getValue(AlphaWarheadBlock.Status) != Wanted) {
                AlphaWarheadBlock.applyStatus(World, Warhead, State, Wanted);
            }
        }
    }

    public boolean isBusy(List<BlockPos> Targets) {
        for (BlockPos Target : Targets) {
            if (sequenceTargeting(Target).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void record(BlockPos Panel, int Note) {
        if (Panels.containsKey(Panel)) {
            Outcomes.put(Panel.immutable(), Note);
        }
    }

    public WarheadSequence start(ServerLevel World, BlockPos Panel, List<BlockPos> Targets) {
        long Now = World.getGameTime();
        WarheadSequence Sequence = new WarheadSequence(Panel, Now, Now + CountdownTicks, Targets);
        Sequences.put(Sequence.getPanel(), Sequence);
        Outcomes.remove(Sequence.getPanel());
        setDirty();
        keepLoaded(World, Sequence);
        for (BlockPos Target : Targets) {
            refresh(World, Target);
        }
        WarheadAnnouncer.start(World, Sequence, Sequences.values());
        notifyPanels(World, Sequence);
        return Sequence;
    }

    public boolean cancel(ServerLevel World, BlockPos Panel) {
        WarheadSequence Sequence = Sequences.get(Panel);
        if (Sequence == null || Sequence.isLocked()) {
            return false;
        }
        Sequences.remove(Panel);
        record(Panel, WarheadPanelBlockEntity.NoteCancelled);
        Panels.computeIfPresent(Panel, (Key, Stage) -> PanelStage.Sealed);
        setDirty();
        for (BlockPos Target : Sequence.getTargets()) {
            refresh(World, Target);
        }
        WarheadAnnouncer.cancelled(World, Sequence, Sequences.values());
        notifyPanels(World, Sequence);
        return true;
    }

    private void notifyPanels(ServerLevel World, WarheadSequence Sequence) {
        for (BlockPos Panel : List.copyOf(Panels.keySet())) {
            if (affects(Sequence, Panel) && World.isLoaded(Panel) && World.getBlockEntity(Panel) instanceof WarheadPanelBlockEntity Console) {
                Console.reconcile(World);
                Console.flush(World);
            }
        }
    }

    private void keepLoaded(ServerLevel World, WarheadSequence Sequence) {
        ChunkPos Home = new ChunkPos(Sequence.getPanel());
        World.getChunkSource().addRegionTicket(Ticket, Home, TicketRadius, Home);
        for (BlockPos Target : Sequence.getTargets()) {
            ChunkPos Spot = new ChunkPos(Target);
            World.getChunkSource().addRegionTicket(Ticket, Spot, TicketRadius, Spot);
        }
    }

    public void tick(ServerLevel World) {
        if (Sequences.isEmpty() && Craters.isEmpty()) {
            return;
        }
        if (!World.tickRateManager().runsNormally()) {
            return;
        }
        long Now = World.getGameTime();
        boolean Second = Now % 20L == 0L;
        for (WarheadSequence Sequence : List.copyOf(Sequences.values())) {
            int Left = Sequence.ticksLeft(Now);
            if (Second) {
                keepLoaded(World, Sequence);
            }
            if (!Sequence.isLocked() && Left <= LockTicks) {
                Sequence.lock();
                setDirty();
                for (BlockPos Target : Sequence.getTargets()) {
                    refresh(World, Target);
                }
                notifyPanels(World, Sequence);
            }
            if (Left > 0) {
                WarheadAnnouncer.cue(World, Sequence, Sequences.values(), Left);
            } else {
                detonate(World, Sequence);
            }
        }
        if (Second && !Sequences.isEmpty()) {
            WarheadAnnouncer.alertAll(World, Sequences.values());
        }
        if (!Craters.isEmpty()) {
            long Deadline = System.nanoTime() + WarheadCrater.TickNanos;
            for (WarheadCrater Crater : List.copyOf(Craters)) {
                if (Crater.step(World, Deadline)) {
                    Craters.remove(Crater);
                }
                if (System.nanoTime() >= Deadline) {
                    break;
                }
            }
            setDirty();
        }
    }

    private void detonate(ServerLevel World, WarheadSequence Sequence) {
        Sequences.remove(Sequence.getPanel());
        Panels.computeIfPresent(Sequence.getPanel(), (Key, Stage) -> PanelStage.Sealed);
        setDirty();
        List<Vec3> Centres = new ArrayList<>();
        List<BlockPos> Bases = new ArrayList<>();
        for (BlockPos Target : Sequence.getTargets()) {
            World.getChunk(Target.getX() >> 4, Target.getZ() >> 4);
            BlockState State = World.getBlockState(Target);
            if (!AlphaWarheadBlock.isMaster(State)) {
                continue;
            }
            Centres.add(AlphaWarheadBlock.centre(Target, State));
            Bases.add(Target);
            AlphaWarheadBlock.consume(World, Target);
        }
        if (Centres.isEmpty()) {
            record(Sequence.getPanel(), WarheadPanelBlockEntity.NoteFailed);
            WarheadAnnouncer.failed(World, Sequence, Sequences.values());
            notifyPanels(World, Sequence);
            return;
        }
        record(Sequence.getPanel(), WarheadPanelBlockEntity.NoteDetonated);
        WarheadAnnouncer.detonation(World, Sequence, Sequences.values());
        GameRules Rules = World.getGameRules();
        boolean Griefing = Rules.getBoolean(GameRules.RULE_MOBGRIEFING);
        boolean Burn = Griefing && Rules.getBoolean(GameRules.RULE_DOFIRETICK);
        for (int Index = 0; Index < Centres.size(); Index++) {
            Vec3 Centre = Centres.get(Index);
            WarheadBlast.broadcast(World, Centre);
            WarheadBlast.killZone(World, Centre);
            if (Griefing) {
                BlockPos Base = BlockPos.containing(Centre.x, Bases.get(Index).getY(), Centre.z);
                Craters.add(new WarheadCrater(Base, World.getRandom().nextLong(), Burn));
            }
        }
        notifyPanels(World, Sequence);
    }
    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        ListTag WarheadList = new ListTag();
        for (BlockPos Warhead : Warheads) {
            WarheadList.add(LongTag.valueOf(Warhead.asLong()));
        }
        Root.put("Warheads", WarheadList);
        ListTag PanelList = new ListTag();
        for (Map.Entry<BlockPos, PanelStage> Entry : Panels.entrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putLong("Pos", Entry.getKey().asLong());
            Saved.putByte("Stage", (byte) Entry.getValue().ordinal());
            PanelList.add(Saved);
        }
        Root.put("Panels", PanelList);
        ListTag SequenceList = new ListTag();
        for (WarheadSequence Sequence : Sequences.values()) {
            SequenceList.add(Sequence.save());
        }
        Root.put("Sequences", SequenceList);
        ListTag OutcomeList = new ListTag();
        for (Map.Entry<BlockPos, Integer> Entry : Outcomes.entrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putLong("Pos", Entry.getKey().asLong());
            Saved.putByte("Note", Entry.getValue().byteValue());
            OutcomeList.add(Saved);
        }
        Root.put("Outcomes", OutcomeList);
        ListTag CraterList = new ListTag();
        for (WarheadCrater Crater : Craters) {
            CraterList.add(Crater.save());
        }
        Root.put("Craters", CraterList);
        return Root;
    }

    private static WarheadNetwork load(CompoundTag Root, HolderLookup.Provider Registries) {
        WarheadNetwork Network = new WarheadNetwork();
        ListTag WarheadList = Root.getList("Warheads", Tag.TAG_LONG);
        for (int Index = 0; Index < WarheadList.size(); Index++) {
            Network.Warheads.add(BlockPos.of(((LongTag) WarheadList.get(Index)).getAsLong()));
        }
        ListTag PanelList = Root.getList("Panels", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < PanelList.size(); Index++) {
            CompoundTag Saved = PanelList.getCompound(Index);
            Network.Panels.put(BlockPos.of(Saved.getLong("Pos")), PanelStage.byIndex(Saved.getByte("Stage")));
        }
        ListTag SequenceList = Root.getList("Sequences", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < SequenceList.size(); Index++) {
            WarheadSequence Sequence = WarheadSequence.load(SequenceList.getCompound(Index));
            Network.Sequences.put(Sequence.getPanel(), Sequence);
        }
        ListTag OutcomeList = Root.getList("Outcomes", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < OutcomeList.size(); Index++) {
            CompoundTag Saved = OutcomeList.getCompound(Index);
            Network.Outcomes.put(BlockPos.of(Saved.getLong("Pos")), (int) Saved.getByte("Note"));
        }
        ListTag CraterList = Root.getList("Craters", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < CraterList.size(); Index++) {
            Network.Craters.add(WarheadCrater.load(CraterList.getCompound(Index)));
        }
        return Network;
    }
}
