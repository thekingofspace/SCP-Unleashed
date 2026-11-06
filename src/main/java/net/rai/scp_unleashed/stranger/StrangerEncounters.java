package net.rai.scp_unleashed.stranger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.InvestigationTracker;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.discord.DiscordNotices;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StrangerEncounters extends SavedData {
    public static final String DataName = "scp_unleashed_stranger_encounters";
    public static final int CheckInterval = 20;
    public static final int RetryTicks = 200;
    public static final int MinRetreat = 1200;
    public static final int MaxRetreat = 3600;
    public static final int LostRetry = 600;
    public static final double ApproachDistance = 6.0;
    public static final double FarDistance = 64.0;
    public static final int VerticalSearch = 6;
    public static final int SpotAttempts = 64;
    public static final int QuietGap = 2400;
    private static final SavedData.Factory<StrangerEncounters> DataFactory = new SavedData.Factory<>(StrangerEncounters::new, StrangerEncounters::load);

    private final List<Encounter> Pending = new ArrayList<>();
    private final Map<UUID, Long> QuietUntil = new HashMap<>();
    private final List<Stamp> Unstamped = new ArrayList<>();

    public enum Phase {
        Waiting,
        Present,
        Delivered
    }

    public static StrangerEncounters get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public List<Encounter> list() {
        return List.copyOf(Pending);
    }

    public Optional<Encounter> forCase(UUID CaseId) {
        for (Encounter Entry : Pending) {
            if (CaseId.equals(Entry.CaseId)) {
                return Optional.of(Entry);
            }
        }
        return Optional.empty();
    }

    public Optional<Encounter> forStranger(UUID StrangerId) {
        for (Encounter Entry : Pending) {
            if (StrangerId.equals(Entry.Stranger)) {
                return Optional.of(Entry);
            }
        }
        return Optional.empty();
    }

    public Optional<Encounter> waitingFor(UUID Reader, @Nullable ResourceLocation Document) {
        for (Encounter Entry : Pending) {
            if (Reader.equals(Entry.Reader) && Entry.Stage != Phase.Delivered && (Document == null || Document.equals(Entry.Document))) {
                return Optional.of(Entry);
            }
        }
        return Optional.empty();
    }

    private boolean anotherPresent(Encounter Entry) {
        for (Encounter Other : Pending) {
            if (Other != Entry && Other.Reader.equals(Entry.Reader) && Other.Stage == Phase.Present) {
                return true;
            }
        }
        return false;
    }

    public boolean owns(UUID EncounterId, UUID StrangerId) {
        for (Encounter Entry : Pending) {
            if (Entry.Id.equals(EncounterId)) {
                return Entry.Stage == Phase.Present && StrangerId.equals(Entry.Stranger) || Entry.Stage == Phase.Delivered;
            }
        }
        return false;
    }

    public Encounter schedule(StrangerPlan Plan, @Nullable UUID CaseId, UUID Reader, @Nullable ResourceLocation Document, RandomSource Random) {
        int Delay = Plan.MinDelay() + (Plan.MaxDelay() > Plan.MinDelay() ? Random.nextInt(Plan.MaxDelay() - Plan.MinDelay() + 1) : 0);
        Encounter Entry = new Encounter(UUID.randomUUID(), CaseId, Reader, Document, Plan);
        Entry.Remaining = Delay;
        Pending.add(Entry);
        setDirty();
        return Entry;
    }

    public boolean summonNow(ServerPlayer Reader, StrangerPlan Fallback, @Nullable ResourceLocation Document) {
        Encounter Entry = waitingFor(Reader.getUUID(), Document).orElse(null);
        if (Entry == null) {
            Entry = schedule(Fallback, null, Reader.getUUID(), Document, Reader.getRandom());
        }
        for (Encounter Other : List.copyOf(Pending)) {
            if (Other != Entry && Other.Reader.equals(Entry.Reader) && Other.Stage == Phase.Present) {
                retreat(Reader.server, Other, MinRetreat);
            }
        }
        QuietUntil.remove(Reader.getUUID());
        if (Entry.Stage == Phase.Present) {
            retreat(Reader.server, Entry, 0);
        }
        Entry.Remaining = 0;
        setDirty();
        return appear(Reader, Entry, Reader.server.overworld().getGameTime());
    }

    public Map<UUID, List<UUID>> withdraw(ResourceLocation Document) {
        Map<UUID, List<UUID>> Readers = new HashMap<>();
        boolean Changed = false;
        for (Encounter Entry : List.copyOf(Pending)) {
            if (!Document.equals(Entry.Document)) {
                continue;
            }
            Pending.remove(Entry);
            Changed = true;
            if (Entry.CaseId != null) {
                List<UUID> Known = Readers.computeIfAbsent(Entry.CaseId, Key -> new ArrayList<>());
                Known.addAll(Entry.Former);
                Known.add(Entry.Reader);
            }
        }
        if (Changed) {
            setDirty();
        }
        return Readers;
    }

    public int hasten(int Delay) {
        int Count = 0;
        for (Encounter Entry : Pending) {
            if (Entry.Stage == Phase.Waiting && Entry.Remaining > Delay) {
                Entry.Remaining = Delay;
                Count++;
            }
        }
        if (Count > 0) {
            setDirty();
        }
        return Count;
    }

    public void tick(MinecraftServer Server) {
        if (Pending.isEmpty() || !ReaderTargets.anyTargetable(Server)) {
            return;
        }
        long Now = Server.overworld().getGameTime();
        if (Now % CheckInterval != 0L) {
            return;
        }
        for (Encounter Entry : List.copyOf(Pending)) {
            if (Entry.Stage != Phase.Delivered && Server.getPlayerList().getPlayer(Entry.Reader) == null) {
                reassign(Server, Entry);
            }
            switch (Entry.Stage) {
                case Waiting -> waitTick(Server, Entry, Now);
                case Present -> presentTick(Server, Entry, Now);
                case Delivered -> {
                }
            }
        }
    }

    public static boolean isValidReader(ServerPlayer Reader) {
        return Reader.isAlive() && !Reader.isSpectator() && !Reader.isSleeping() && !Reader.isFallFlying() && Targeting.isTargetable(Reader);
    }

    public static boolean canReceive(ServerPlayer Reader) {
        return Reader.isAlive() && !Reader.isRemoved() && !Reader.hasDisconnected();
    }

    private static boolean canInherit(ServerPlayer Candidate) {
        return ReaderTargets.isTargetable(Candidate);
    }

    private void waitTick(MinecraftServer Server, Encounter Entry, long Now) {
        ServerPlayer Reader = Server.getPlayerList().getPlayer(Entry.Reader);
        if (Reader == null || !isValidReader(Reader) || !ReaderTargets.isSettled(Reader) || !AnomalyRealm.allows(Reader.level())) {
            return;
        }
        if (Entry.Remaining > 0) {
            Entry.Remaining -= CheckInterval;
            setDirty();
        }
        if (Entry.Remaining > 0 || anotherPresent(Entry) || Now < QuietUntil.getOrDefault(Entry.Reader, 0L)) {
            return;
        }
        if (!appear(Reader, Entry, Now)) {
            Entry.Remaining = RetryTicks;
        }
    }

    private void presentTick(MinecraftServer Server, Encounter Entry, long Now) {
        ServerLevel World = levelOf(Server, Entry.Dimension);
        Entity Found = World == null || Entry.Stranger == null ? null : World.getEntity(Entry.Stranger);
        ServerPlayer Reader = Server.getPlayerList().getPlayer(Entry.Reader);
        if (!(Found instanceof StrangerEntity Figure) || !Figure.isAlive() || Figure.isLeaving()) {
            boolean Gone = World == null || World.areEntitiesLoaded(ChunkPos.asLong(Entry.Spot));
            if (Gone || Reader == null || World != Reader.serverLevel() || Reader.distanceToSqr(Vec3.atCenterOf(Entry.Spot)) > FarDistance * FarDistance) {
                retreat(Server, Entry, LostRetry);
            }
            return;
        }
        if (Reader == null || Reader.serverLevel() != World || Reader.distanceToSqr(Figure) > FarDistance * FarDistance) {
            retreat(Server, Entry, Server.overworld().random);
            return;
        }
        if (Figure.isTalking() || Reader.distanceToSqr(Figure) <= ApproachDistance * ApproachDistance) {
            Entry.LastNear = Now;
            setDirty();
            return;
        }
        if (Now - Entry.LastNear > Entry.Plan.Linger()) {
            retreat(Server, Entry, Server.overworld().random);
        }
    }

    private void retreat(MinecraftServer Server, Encounter Entry, RandomSource Random) {
        retreat(Server, Entry, MinRetreat + Random.nextInt(MaxRetreat - MinRetreat + 1));
    }

    private void retreat(MinecraftServer Server, Encounter Entry, int Delay) {
        DiscordNotices.strangerLeft(Server, Entry.Stage == Phase.Present, Entry.Reader, Entry.Plan.variant(), Entry.Dimension, Entry.Spot, Entry.Document);
        ServerLevel World = levelOf(Server, Entry.Dimension);
        if (World != null && Entry.Stranger != null && World.getEntity(Entry.Stranger) instanceof StrangerEntity Figure) {
            Figure.fadeAway();
        }
        Entry.Stage = Phase.Waiting;
        Entry.Stranger = null;
        Entry.Remaining = Delay;
        Entry.Retreats++;
        setDirty();
    }

    public void readerTravelled(MinecraftServer Server, UUID Reader) {
        for (Encounter Entry : List.copyOf(Pending)) {
            if (Reader.equals(Entry.Reader) && Entry.Stage == Phase.Present) {
                retreat(Server, Entry, Server.overworld().random);
            }
        }
    }

    public void readerDied(MinecraftServer Server, UUID Reader) {
        for (Encounter Entry : List.copyOf(Pending)) {
            if (Reader.equals(Entry.Reader) && Entry.Stage == Phase.Present) {
                retreat(Server, Entry, LostRetry);
            }
        }
    }

    public void readerLoggedOut(MinecraftServer Server, UUID Reader) {
        for (Encounter Entry : List.copyOf(Pending)) {
            if (Reader.equals(Entry.Reader) && Entry.Stage != Phase.Delivered) {
                reassign(Server, Entry);
            }
        }
    }

    public void readerJoined(ServerPlayer Reader) {
        boolean Changed = false;
        for (Stamp Due : List.copyOf(Unstamped)) {
            if (Due.Reader().equals(Reader.getUUID())) {
                InvestigationTracker.markResolved(Reader, Due.Document(), Due.CaseId());
                Unstamped.remove(Due);
                Changed = true;
            }
        }
        if (Changed) {
            setDirty();
        }
        for (Encounter Entry : List.copyOf(Pending)) {
            if (Entry.Stage == Phase.Delivered) {
                continue;
            }
            boolean Mine = Reader.getUUID().equals(Entry.Reader);
            if (!Mine) {
                boolean Returning = Reader.getUUID().equals(Entry.Origin) && Entry.Stage == Phase.Waiting && canInherit(Reader);
                if (!Returning && (!canInherit(Reader) || Reader.server.getPlayerList().getPlayer(Entry.Reader) != null)) {
                    continue;
                }
                handOff(Reader.server, Entry, Reader.getUUID());
            }
            if (Entry.Stage == Phase.Waiting && Entry.Orphaned) {
                Entry.Remaining = 0;
            }
            Entry.Orphaned = false;
            setDirty();
        }
    }

    private void reassign(MinecraftServer Server, Encounter Entry) {
        ServerPlayer Heir = heirFor(Server, Entry.Reader);
        if (Entry.Stage == Phase.Present) {
            if (Heir == null) {
                retreat(Server, Entry, Server.overworld().random);
            } else {
                retreat(Server, Entry, LostRetry);
            }
        }
        if (Heir != null) {
            handOff(Server, Entry, Heir.getUUID());
            Entry.Orphaned = false;
        } else if (!Entry.Orphaned) {
            Entry.Orphaned = true;
            setDirty();
        }
    }

    @Nullable
    private ServerPlayer heirFor(MinecraftServer Server, UUID Leaving) {
        return ReaderTargets.next(Server, Leaving, this::activeFor, Candidate -> true);
    }

    private int activeFor(UUID Reader) {
        int Count = 0;
        for (Encounter Entry : Pending) {
            if (Reader.equals(Entry.Reader) && Entry.Stage != Phase.Delivered) {
                Count++;
            }
        }
        return Count;
    }

    private void handOff(MinecraftServer Server, Encounter Entry, UUID Heir) {
        UUID Previous = Entry.Reader;
        if (Entry.CaseId != null) {
            moveCase(Server, Entry.CaseId, Previous, Heir);
        }
        Entry.Former.remove(Heir);
        if (!Entry.Former.contains(Previous)) {
            Entry.Former.add(Previous);
        }
        Entry.Reader = Heir;
        setDirty();
    }

    private static void moveCase(MinecraftServer Server, UUID CaseId, UUID From, UUID To) {
        Investigations Cases = Investigations.get(Server);
        for (Investigation Case : Cases.of(From)) {
            if (CaseId.equals(Case.getId())) {
                Cases.remove(From, Case);
                Cases.add(To, Case);
                return;
            }
        }
    }

    private void stampFormer(MinecraftServer Server, Encounter Entry) {
        if (Entry.CaseId == null || Entry.Document == null) {
            return;
        }
        for (UUID Former : Entry.Former) {
            if (Former.equals(Entry.Reader)) {
                continue;
            }
            ServerPlayer Online = Server.getPlayerList().getPlayer(Former);
            if (Online != null) {
                InvestigationTracker.markResolved(Online, Entry.Document, Entry.CaseId);
            } else {
                Unstamped.add(new Stamp(Former, Entry.Document, Entry.CaseId));
            }
        }
    }

    public void strangerLost(UUID StrangerId) {
        for (Encounter Entry : Pending) {
            if (StrangerId.equals(Entry.Stranger) && Entry.Stage == Phase.Present) {
                Entry.Stage = Phase.Waiting;
                Entry.Stranger = null;
                Entry.Remaining = LostRetry;
                setDirty();
            }
        }
    }

    private boolean appear(ServerPlayer Reader, Encounter Entry, long Now) {
        ServerLevel World = Reader.serverLevel();
        Optional<BlockPos> Spot = findSpot(World, Reader, Entry.Plan.MinDistance(), Entry.Plan.MaxDistance(), Reader.getRandom());
        if (Spot.isEmpty()) {
            return false;
        }
        BlockPos Pos = Spot.get();
        double Dx = Reader.getX() - (Pos.getX() + 0.5);
        double Dz = Reader.getZ() - (Pos.getZ() + 0.5);
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        StrangerEntity Figure = StrangerEntity.spawn(World, Pos, Yaw, Entry.Plan.variant(), Reader.getUUID(), Entry.Id);
        Entry.Stage = Phase.Present;
        Entry.Stranger = Figure.getUUID();
        Entry.Dimension = World.dimension().location().toString();
        Entry.Spot = Pos;
        Entry.LastNear = Now;
        Entry.Appearances++;
        setDirty();
        World.playSound(null, Figure.getX(), Figure.getY() + 1.0, Figure.getZ(), StrangerRegistry.Appear.get(), SoundSource.NEUTRAL, 0.7f, 1.0f);
        Notices.strangerArrived(Reader, Entry.Plan.variant(), Pos, Entry.Former);
        DiscordNotices.strangerAppeared(Reader, Entry.Plan.variant(), Pos, Entry.Document);
        return true;
    }

    public static Optional<BlockPos> findSpot(ServerLevel World, ServerPlayer Reader, int MinDistance, int MaxDistance, RandomSource Random) {
        Vec3 Motion = Reader.getDeltaMovement();
        Vec3 Heading = new Vec3(Motion.x, 0.0, Motion.z);
        if (Heading.lengthSqr() < 0.0025) {
            Vec3 Look = Reader.getLookAngle();
            Heading = new Vec3(Look.x, 0.0, Look.z);
        }
        if (Heading.lengthSqr() < 1.0e-4) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            Heading = new Vec3(Math.cos(Angle), 0.0, Math.sin(Angle));
        }
        Heading = Heading.normalize();
        double Facing = Math.atan2(Heading.z, Heading.x);
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < SpotAttempts; Attempt++) {
            double Spread = Math.toRadians(Attempt < 20 ? 18.0 : Attempt < 44 ? 38.0 : 65.0);
            double Angle = Facing + (Random.nextDouble() * 2.0 - 1.0) * Spread;
            double Distance = MinDistance + Random.nextDouble() * Math.max(0, MaxDistance - MinDistance);
            int X = Mth.floor(Reader.getX() + Math.cos(Angle) * Distance);
            int Z = Mth.floor(Reader.getZ() + Math.sin(Angle) * Distance);
            BlockPos Ground = groundNear(World, X, Reader.getBlockY(), Z);
            if (Ground == null) {
                continue;
            }
            if (canSee(World, Reader, Ground)) {
                return Optional.of(Ground);
            }
            if (Fallback == null) {
                Fallback = Ground;
            }
        }
        return Optional.ofNullable(Fallback);
    }

    @Nullable
    private static BlockPos groundNear(ServerLevel World, int X, int BaseY, int Z) {
        for (int Step = 0; Step <= VerticalSearch * 2; Step++) {
            int Dy = (Step + 1) / 2 * (Step % 2 == 0 ? -1 : 1);
            BlockPos Candidate = new BlockPos(X, BaseY + Dy, Z);
            if (isSafeStand(World, Candidate)) {
                return Candidate;
            }
        }
        return null;
    }

    public static boolean isSafeStand(ServerLevel World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || World.isOutsideBuildHeight(Pos) || !World.getWorldBorder().isWithinBounds(Pos)) {
            return false;
        }
        BlockState At = World.getBlockState(Pos);
        BlockState Above = World.getBlockState(Pos.above());
        BlockState Below = World.getBlockState(Pos.below());
        if (!At.getCollisionShape(World, Pos).isEmpty() || !Above.getCollisionShape(World, Pos.above()).isEmpty()) {
            return false;
        }
        if (!At.getFluidState().isEmpty() || !Above.getFluidState().isEmpty() || !Below.getFluidState().isEmpty()) {
            return false;
        }
        if (!Below.isFaceSturdy(World, Pos.below(), Direction.UP) || isHazard(Below) || isHazard(At)) {
            return false;
        }
        AABB Body = new AABB(Pos.getX() + 0.2, Pos.getY(), Pos.getZ() + 0.2, Pos.getX() + 0.8, Pos.getY() + 1.95, Pos.getZ() + 0.8);
        return World.noCollision(Body) && World.getEntities((Entity) null, Body.inflate(0.5)).isEmpty();
    }

    private static boolean isHazard(BlockState State) {
        return State.is(Blocks.MAGMA_BLOCK) || State.is(Blocks.CAMPFIRE) || State.is(Blocks.SOUL_CAMPFIRE) || State.is(Blocks.FIRE) || State.is(Blocks.SOUL_FIRE)
                || State.is(Blocks.CACTUS) || State.is(Blocks.SWEET_BERRY_BUSH) || State.is(Blocks.POWDER_SNOW) || State.is(Blocks.WITHER_ROSE) || State.is(Blocks.LAVA_CAULDRON);
    }

    private static boolean canSee(ServerLevel World, ServerPlayer Reader, BlockPos Ground) {
        Vec3 Eye = Reader.getEyePosition();
        Vec3 Head = new Vec3(Ground.getX() + 0.5, Ground.getY() + 1.6, Ground.getZ() + 0.5);
        return World.clip(new ClipContext(Eye, Head, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Reader)).getType() == HitResult.Type.MISS;
    }

    public void deliver(ServerPlayer Reader, StrangerEntity Figure) {
        if (!canReceive(Reader)) {
            return;
        }
        Encounter Entry = forStranger(Figure.getUUID()).orElse(null);
        if (Entry == null || Entry.Stage != Phase.Present || !Reader.getUUID().equals(Entry.Reader)) {
            Figure.vanish(Reader);
            return;
        }
        for (StrangerGift Gift : Entry.Plan.Gifts()) {
            give(Reader, Gift.toStack());
        }
        Reader.level().playSound(null, Reader.getX(), Reader.getY(), Reader.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3f, 0.8f);
        Entry.Stage = Phase.Delivered;
        QuietUntil.put(Entry.Reader, Reader.server.overworld().getGameTime() + QuietGap);
        Entry.DeliveredAt = Figure.blockPosition();
        DiscordNotices.strangerDelivered(Reader, Figure, Entry.Plan.variant(), Entry.Plan.Gifts(), Entry.Document);
        Entry.Dimension = Figure.level().dimension().location().toString();
        Entry.Stranger = Figure.getUUID();
        if (Entry.CaseId == null) {
            Pending.remove(Entry);
        }
        setDirty();
        Figure.vanish(Reader);
    }

    public static void give(ServerPlayer Reader, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return;
        }
        Reader.getInventory().add(Stack);
        if (!Stack.isEmpty()) {
            ItemEntity Drop = new ItemEntity(Reader.level(), Reader.getX(), Reader.getY() + 0.25, Reader.getZ(), Stack.copy());
            Drop.setNoPickUpDelay();
            Drop.setUnlimitedLifetime();
            Drop.setDeltaMovement(Vec3.ZERO);
            Reader.level().addFreshEntity(Drop);
            Stack.setCount(0);
        }
        Reader.inventoryMenu.broadcastChanges();
    }

    public static boolean confirm(AnomalySpawners.Discovery Found) {
        MinecraftServer Server = Found.World().getServer();
        StrangerEncounters Data = get(Server);
        UUID Reader = Found.Investigator().getUUID();
        boolean Exact = Data.Pending.stream().anyMatch(Entry -> Entry.Stage == Phase.Delivered && Reader.equals(Entry.Reader) && Found.DocumentId().equals(Entry.Document)
                && Found.Pos().equals(Entry.DeliveredAt));
        for (Encounter Entry : List.copyOf(Data.Pending)) {
            if (Entry.Stage == Phase.Delivered && Reader.equals(Entry.Reader) && Found.DocumentId().equals(Entry.Document) && (!Exact || Found.Pos().equals(Entry.DeliveredAt))) {
                Data.Pending.remove(Entry);
                Data.stampFormer(Server, Entry);
                Data.setDirty();
            }
        }
        return true;
    }

    @Nullable
    private static ServerLevel levelOf(MinecraftServer Server, String Dimension) {
        ResourceLocation Id = ResourceLocation.tryParse(Dimension);
        return Id == null ? null : Server.getLevel(ResourceKey.create(Registries.DIMENSION, Id));
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (Encounter Entry : Pending) {
            Saved.add(Entry.save());
        }
        Tag.put("Encounters", Saved);
        ListTag Quiet = new ListTag();
        for (Map.Entry<UUID, Long> Hold : QuietUntil.entrySet()) {
            CompoundTag Record = new CompoundTag();
            Record.putUUID("Reader", Hold.getKey());
            Record.putLong("Until", Hold.getValue());
            Quiet.add(Record);
        }
        Tag.put("Quiet", Quiet);
        ListTag Stamps = new ListTag();
        for (Stamp Due : Unstamped) {
            Stamps.add(Due.save());
        }
        Tag.put("Unstamped", Stamps);
        return Tag;
    }

    private static StrangerEncounters load(CompoundTag Tag, HolderLookup.Provider Registries) {
        StrangerEncounters Data = new StrangerEncounters();
        ListTag Saved = Tag.getList("Encounters", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            Encounter Entry = Encounter.load(Saved.getCompound(Index));
            if (Entry != null) {
                Data.Pending.add(Entry);
            }
        }
        ListTag Quiet = Tag.getList("Quiet", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Quiet.size(); Index++) {
            CompoundTag Record = Quiet.getCompound(Index);
            if (Record.hasUUID("Reader")) {
                Data.QuietUntil.put(Record.getUUID("Reader"), Record.getLong("Until"));
            }
        }
        ListTag Stamps = Tag.getList("Unstamped", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Stamps.size(); Index++) {
            Stamp Due = Stamp.load(Stamps.getCompound(Index));
            if (Due != null) {
                Data.Unstamped.add(Due);
            }
        }
        return Data;
    }

    private record Stamp(UUID Reader, ResourceLocation Document, UUID CaseId) {
        private CompoundTag save() {
            CompoundTag Saved = new CompoundTag();
            Saved.putUUID("Reader", Reader);
            Saved.putString("Document", Document.toString());
            Saved.putUUID("Case", CaseId);
            return Saved;
        }

        @Nullable
        private static Stamp load(CompoundTag Saved) {
            ResourceLocation Document = ResourceLocation.tryParse(Saved.getString("Document"));
            if (!Saved.hasUUID("Reader") || !Saved.hasUUID("Case") || Document == null) {
                return null;
            }
            return new Stamp(Saved.getUUID("Reader"), Document, Saved.getUUID("Case"));
        }
    }

    public static final class Encounter {
        private final UUID Id;
        @Nullable
        private final UUID CaseId;
        private UUID Reader;
        private UUID Origin;
        @Nullable
        private final ResourceLocation Document;
        private final StrangerPlan Plan;
        private final List<UUID> Former = new ArrayList<>();
        private Phase Stage = Phase.Waiting;
        private int Remaining;
        @Nullable
        private UUID Stranger;
        private String Dimension = Level.OVERWORLD.location().toString();
        private BlockPos Spot = BlockPos.ZERO;
        private BlockPos DeliveredAt = BlockPos.ZERO;
        private long LastNear;
        private int Appearances;
        private int Retreats;
        private boolean Orphaned;

        private Encounter(UUID Id, @Nullable UUID CaseId, UUID Reader, @Nullable ResourceLocation Document, StrangerPlan Plan) {
            this.Id = Id;
            this.CaseId = CaseId;
            this.Reader = Reader;
            this.Origin = Reader;
            this.Document = Document;
            this.Plan = Plan;
        }

        public UUID getId() {
            return Id;
        }

        public UUID getReader() {
            return Reader;
        }

        @Nullable
        public ResourceLocation getDocument() {
            return Document;
        }

        public StrangerPlan getPlan() {
            return Plan;
        }

        public Phase getStage() {
            return Stage;
        }

        public int getRemaining() {
            return Remaining;
        }

        public BlockPos getSpot() {
            return Spot;
        }

        public BlockPos getDeliveredAt() {
            return DeliveredAt;
        }

        public String getDimension() {
            return Dimension;
        }

        public int getAppearances() {
            return Appearances;
        }

        public int getRetreats() {
            return Retreats;
        }

        public boolean isDelivered() {
            return Stage == Phase.Delivered;
        }

        private CompoundTag save() {
            CompoundTag Tag = new CompoundTag();
            Tag.putUUID("Id", Id);
            if (CaseId != null) {
                Tag.putUUID("Case", CaseId);
            }
            Tag.putUUID("Reader", Reader);
            Tag.putUUID("Origin", Origin);
            if (Document != null) {
                Tag.putString("Document", Document.toString());
            }
            Tag.put("Plan", Plan.save());
            Tag.putString("Stage", Stage.name());
            Tag.putInt("Remaining", Remaining);
            if (Stranger != null) {
                Tag.putUUID("Stranger", Stranger);
            }
            Tag.putString("Dimension", Dimension);
            Tag.putLong("Spot", Spot.asLong());
            Tag.putLong("DeliveredAt", DeliveredAt.asLong());
            Tag.putLong("LastNear", LastNear);
            Tag.putInt("Appearances", Appearances);
            Tag.putInt("Retreats", Retreats);
            Tag.putBoolean("Orphaned", Orphaned);
            ListTag Previous = new ListTag();
            for (UUID Earlier : Former) {
                CompoundTag Marker = new CompoundTag();
                Marker.putUUID("Reader", Earlier);
                Previous.add(Marker);
            }
            Tag.put("Former", Previous);
            return Tag;
        }

        @Nullable
        private static Encounter load(CompoundTag Tag) {
            if (!Tag.hasUUID("Id") || !Tag.hasUUID("Reader")) {
                return null;
            }
            ResourceLocation Document = Tag.contains("Document") ? ResourceLocation.tryParse(Tag.getString("Document")) : null;
            Encounter Entry = new Encounter(Tag.getUUID("Id"), Tag.hasUUID("Case") ? Tag.getUUID("Case") : null, Tag.getUUID("Reader"), Document,
                    StrangerPlan.load(Tag.getCompound("Plan")));
            try {
                Entry.Stage = Phase.valueOf(Tag.getString("Stage"));
            } catch (IllegalArgumentException Problem) {
                Entry.Stage = Phase.Waiting;
            }
            Entry.Remaining = Tag.getInt("Remaining");
            Entry.Stranger = Tag.hasUUID("Stranger") ? Tag.getUUID("Stranger") : null;
            if (Tag.contains("Dimension")) {
                Entry.Dimension = Tag.getString("Dimension");
            }
            Entry.Spot = BlockPos.of(Tag.getLong("Spot"));
            Entry.DeliveredAt = BlockPos.of(Tag.getLong("DeliveredAt"));
            Entry.LastNear = Tag.getLong("LastNear");
            Entry.Appearances = Tag.getInt("Appearances");
            Entry.Retreats = Tag.getInt("Retreats");
            Entry.Orphaned = Tag.getBoolean("Orphaned");
            ListTag Previous = Tag.getList("Former", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int Index = 0; Index < Previous.size(); Index++) {
                CompoundTag Marker = Previous.getCompound(Index);
                if (Marker.hasUUID("Reader")) {
                    Entry.Former.add(Marker.getUUID("Reader"));
                }
            }
            Entry.Origin = Tag.hasUUID("Origin") ? Tag.getUUID("Origin") : Entry.Former.isEmpty() ? Entry.Reader : Entry.Former.get(0);
            return Entry;
        }
    }
}
