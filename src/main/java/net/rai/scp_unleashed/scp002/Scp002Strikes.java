package net.rai.scp_unleashed.scp002;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.notice.Notices;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp002Strikes extends SavedData {
    public static final String DataName = "scp_unleashed_scp002_strikes";
    public static final TicketType<ChunkPos> Ticket = TicketType.create("scp_unleashed_scp002", Comparator.comparingLong(ChunkPos::toLong), 100);
    public static final int TicketRadius = 3;
    public static final int RetryTicks = 100;
    public static final int WatchdogTicks = 300;
    private static final SavedData.Factory<Scp002Strikes> DataFactory = new SavedData.Factory<>(Scp002Strikes::new, Scp002Strikes::load);

    private final List<Strike> Pending = new ArrayList<>();

    public static Scp002Strikes get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public enum Stage {
        Waiting,
        Inbound,
        Landed
    }

    public List<Strike> list() {
        return List.copyOf(Pending);
    }

    public Optional<Strike> forCase(UUID CaseId) {
        for (Strike Entry : Pending) {
            if (CaseId.equals(Entry.CaseId)) {
                return Optional.of(Entry);
            }
        }
        return Optional.empty();
    }

    public Strike schedule(@Nullable UUID CaseId, @Nullable UUID Reader, @Nullable ResourceLocation Document, long FireAt, int MinDistance, int MaxDistance) {
        Strike Entry = new Strike(UUID.randomUUID(), CaseId, Reader, Document, MinDistance, MaxDistance, FireAt);
        Pending.add(Entry);
        setDirty();
        return Entry;
    }

    public Strike strikeNear(ServerPlayer Target, int MinDistance, int MaxDistance) {
        Strike Entry = schedule(null, null, null, Target.server.overworld().getGameTime(), MinDistance, MaxDistance);
        Entry.Target = Target.getUUID();
        return Entry;
    }

    public Strike strikeAt(ServerLevel World, BlockPos Ground, Vec3 From) {
        Strike Entry = schedule(null, null, null, World.getGameTime(), 0, 0);
        Entry.Exact = Ground;
        Entry.ExactFrom = From;
        Entry.Dimension = World.dimension().location().toString();
        return Entry;
    }

    public int hasten(long Now, int Delay) {
        int Count = 0;
        for (Strike Entry : Pending) {
            if (Entry.Phase == Stage.Waiting && Entry.FireAt > Now + Delay) {
                Entry.FireAt = Now + Delay;
                Count++;
            }
        }
        if (Count > 0) {
            setDirty();
        }
        return Count;
    }

    public void tick(MinecraftServer Server) {
        if (Pending.isEmpty()) {
            return;
        }
        long Now = Server.overworld().getGameTime();
        boolean Second = Now % 20L == 0L;
        for (Strike Entry : List.copyOf(Pending)) {
            if (Entry.Phase == Stage.Waiting && Now >= Entry.FireAt) {
                fire(Server, Entry, Now);
            } else if (Entry.Phase == Stage.Inbound && Second) {
                watch(Server, Entry, Now);
            }
        }
    }

    @Nullable
    private static ServerLevel levelOf(MinecraftServer Server, String Dimension) {
        ResourceLocation Id = ResourceLocation.tryParse(Dimension);
        return Id == null ? null : Server.getLevel(ResourceKey.create(Registries.DIMENSION, Id));
    }

    public static boolean fallsIn(Level World) {
        return World.dimension() == Level.OVERWORLD;
    }

    public static boolean isValidTarget(ServerPlayer Candidate) {
        return Candidate.isAlive() && !Candidate.isSpectator() && Targeting.isTargetable(Candidate) && fallsIn(Candidate.level())
                && !Candidate.serverLevel().dimensionType().hasCeiling();
    }

    @Nullable
    private ServerPlayer pickTarget(MinecraftServer Server, Strike Entry) {
        if (Entry.Target != null) {
            ServerPlayer Forced = Server.getPlayerList().getPlayer(Entry.Target);
            return Forced != null && isValidTarget(Forced) ? Forced : null;
        }
        if (Entry.Reader != null) {
            ServerPlayer Reader = Server.getPlayerList().getPlayer(Entry.Reader);
            if (Reader != null) {
                return isValidTarget(Reader) && ReaderTargets.isReady(Reader) && AnomalyRealm.allows(Reader.level()) ? Reader : null;
            }
        }
        List<ServerPlayer> Candidates = Server.getPlayerList().getPlayers().stream()
                .filter(Candidate -> isValidTarget(Candidate) && ReaderTargets.isReady(Candidate) && AnomalyRealm.allows(Candidate.level())).toList();
        if (Candidates.isEmpty()) {
            return null;
        }
        return Candidates.get(Server.overworld().random.nextInt(Candidates.size()));
    }

    public static void keepLoaded(ServerLevel World, BlockPos Ground) {
        ChunkPos Centre = new ChunkPos(Ground);
        World.getChunkSource().addRegionTicket(Ticket, Centre, TicketRadius, Centre);
    }

    private void fire(MinecraftServer Server, Strike Entry, long Now) {
        ServerLevel World;
        BlockPos Ground;
        Vec3 From;
        if (Entry.Exact != null) {
            World = levelOf(Server, Entry.Dimension);
            if (World == null || !fallsIn(World)) {
                Pending.remove(Entry);
                setDirty();
                return;
            }
            World.getChunk(Entry.Exact.getX() >> 4, Entry.Exact.getZ() >> 4);
            Ground = new BlockPos(Entry.Exact.getX(), Scp002Impact.groundY(World, Entry.Exact.getX(), Entry.Exact.getZ()), Entry.Exact.getZ());
            From = Entry.ExactFrom != null ? Entry.ExactFrom : Vec3.atCenterOf(Ground);
        } else {
            if (Entry.Target != null && Server.getPlayerList().getPlayer(Entry.Target) == null) {
                Pending.remove(Entry);
                setDirty();
                return;
            }
            ServerPlayer Target = pickTarget(Server, Entry);
            if (Target == null) {
                Entry.FireAt = Now + RetryTicks;
                setDirty();
                return;
            }
            World = Target.serverLevel();
            Optional<BlockPos> Site = Scp002Impact.chooseSite(World, Target.position(), Entry.MinDistance, Entry.MaxDistance, World.random);
            if (Site.isEmpty()) {
                Entry.FireAt = Now + RetryTicks;
                setDirty();
                return;
            }
            Ground = Site.get();
            From = Target.position();
            Notices.sendFalling(World, Entry.Document, Ground, Component.translatable("scp002.scp_unleashed.lead.falling"));
        }
        Direction Front = Scp002Impact.frontToward(Ground, From, World.random);
        keepLoaded(World, Ground);
        MeteorEntity Meteor = MeteorEntity.launch(World, Ground, From, Entry.Id, Front);
        if (Meteor == null) {
            Entry.FireAt = Now + RetryTicks;
            setDirty();
            return;
        }
        Entry.Phase = Stage.Inbound;
        Entry.Dimension = World.dimension().location().toString();
        Entry.Impact = Ground;
        Entry.Meteor = Meteor.getUUID();
        Entry.LaunchedAt = Now;
        Entry.Front = Front;
        setDirty();
    }

    private void watch(MinecraftServer Server, Strike Entry, long Now) {
        ServerLevel World = levelOf(Server, Entry.Dimension);
        if (World == null) {
            Pending.remove(Entry);
            setDirty();
            return;
        }
        if (!fallsIn(World)) {
            recall(Entry, Now);
            return;
        }
        keepLoaded(World, Entry.Impact);
        if (Now - Entry.LaunchedAt < MeteorEntity.FlightTicks + WatchdogTicks) {
            return;
        }
        Entity Found = Entry.Meteor == null ? null : World.getEntity(Entry.Meteor);
        if (Found instanceof MeteorEntity Meteor && Meteor.isAlive()) {
            return;
        }
        Scp002Impact.Result Result = Scp002Impact.strike(World, Entry.Impact, Entry.Front, World.random);
        Vec3 Centre = Vec3.atBottomCenterOf(Entry.Impact.above());
        Scp002Sounds.impact(World, Centre, World.random);
        Scp002Impact.announce(World, Result, Centre);
        landed(Entry.Id, World, Result.Door());
    }

    private void recall(Strike Entry, long Now) {
        if (Entry.Exact != null) {
            Pending.remove(Entry);
        } else {
            Entry.Phase = Stage.Waiting;
            Entry.FireAt = Now + RetryTicks;
            Entry.Dimension = Level.OVERWORLD.location().toString();
            Entry.Meteor = null;
        }
        setDirty();
    }

    public void caseMoved(UUID CaseId, UUID Heir) {
        for (Strike Entry : Pending) {
            if (CaseId.equals(Entry.CaseId)) {
                Entry.Reader = Heir;
                setDirty();
            }
        }
    }

    public boolean isInbound(UUID StrikeId) {
        for (Strike Entry : Pending) {
            if (Entry.Id.equals(StrikeId)) {
                return Entry.Phase == Stage.Inbound;
            }
        }
        return false;
    }

    public void landed(UUID StrikeId, ServerLevel World, BlockPos Door) {
        for (Strike Entry : Pending) {
            if (Entry.Id.equals(StrikeId)) {
                Entry.Phase = Stage.Landed;
                Entry.Site = Door;
                Entry.Dimension = World.dimension().location().toString();
                if (Entry.CaseId == null) {
                    Pending.remove(Entry);
                }
                setDirty();
                return;
            }
        }
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        Scp002Strikes Data = get(Found.World().getServer());
        UUID Reader = Found.Investigator().getUUID();
        Strike Match = null;
        for (Strike Entry : Data.Pending) {
            if (Entry.Phase == Stage.Landed && Reader.equals(Entry.Reader) && Found.DocumentId().equals(Entry.Document) && (Match == null || Found.Pos().equals(Entry.Site))) {
                Match = Entry;
            }
        }
        if (Match != null) {
            Data.Pending.remove(Match);
            Data.setDirty();
            return true;
        }
        if (!fallsIn(Found.World())) {
            return false;
        }
        Data.strikeAt(Found.World(), Found.Pos(), Found.Investigator().position());
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (Strike Entry : Pending) {
            Saved.add(Entry.save());
        }
        Tag.put("Strikes", Saved);
        return Tag;
    }

    private static Scp002Strikes load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp002Strikes Data = new Scp002Strikes();
        ListTag Saved = Tag.getList("Strikes", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            Strike Entry = Strike.load(Saved.getCompound(Index));
            if (Entry != null) {
                Data.Pending.add(Entry);
            }
        }
        return Data;
    }

    public static final class Strike {
        private final UUID Id;
        @Nullable
        private final UUID CaseId;
        @Nullable
        private UUID Reader;
        @Nullable
        private final ResourceLocation Document;
        private final int MinDistance;
        private final int MaxDistance;
        private long FireAt;
        private Stage Phase = Stage.Waiting;
        @Nullable
        private UUID Target;
        @Nullable
        private BlockPos Exact;
        @Nullable
        private Vec3 ExactFrom;
        private String Dimension = Level.OVERWORLD.location().toString();
        private BlockPos Impact = BlockPos.ZERO;
        @Nullable
        private UUID Meteor;
        private long LaunchedAt;
        private Direction Front = Direction.SOUTH;
        private BlockPos Site = BlockPos.ZERO;

        private Strike(UUID Id, @Nullable UUID CaseId, @Nullable UUID Reader, @Nullable ResourceLocation Document, int MinDistance, int MaxDistance, long FireAt) {
            this.Id = Id;
            this.CaseId = CaseId;
            this.Reader = Reader;
            this.Document = Document;
            this.MinDistance = MinDistance;
            this.MaxDistance = MaxDistance;
            this.FireAt = FireAt;
        }

        public UUID getId() {
            return Id;
        }

        @Nullable
        public UUID getReader() {
            return Reader;
        }

        public Stage getPhase() {
            return Phase;
        }

        public long getFireAt() {
            return FireAt;
        }

        public BlockPos getSite() {
            return Site;
        }

        public BlockPos getImpact() {
            return Impact;
        }

        public String getDimension() {
            return Dimension;
        }

        public boolean isLanded() {
            return Phase == Stage.Landed;
        }

        private CompoundTag save() {
            CompoundTag Tag = new CompoundTag();
            Tag.putUUID("Id", Id);
            if (CaseId != null) {
                Tag.putUUID("Case", CaseId);
            }
            if (Reader != null) {
                Tag.putUUID("Reader", Reader);
            }
            if (Document != null) {
                Tag.putString("Document", Document.toString());
            }
            if (Target != null) {
                Tag.putUUID("Target", Target);
            }
            if (Exact != null) {
                Tag.putLong("Exact", Exact.asLong());
            }
            if (ExactFrom != null) {
                Tag.putDouble("FromX", ExactFrom.x);
                Tag.putDouble("FromY", ExactFrom.y);
                Tag.putDouble("FromZ", ExactFrom.z);
            }
            if (Meteor != null) {
                Tag.putUUID("Meteor", Meteor);
            }
            Tag.putInt("MinDistance", MinDistance);
            Tag.putInt("MaxDistance", MaxDistance);
            Tag.putLong("FireAt", FireAt);
            Tag.putString("Stage", Phase.name());
            Tag.putString("Dimension", Dimension);
            Tag.putLong("Impact", Impact.asLong());
            Tag.putLong("Launched", LaunchedAt);
            Tag.putInt("Front", Front.get2DDataValue());
            Tag.putLong("Site", Site.asLong());
            return Tag;
        }

        @Nullable
        private static Strike load(CompoundTag Tag) {
            if (!Tag.hasUUID("Id")) {
                return null;
            }
            ResourceLocation Document = Tag.contains("Document") ? ResourceLocation.tryParse(Tag.getString("Document")) : null;
            Strike Entry = new Strike(Tag.getUUID("Id"), Tag.hasUUID("Case") ? Tag.getUUID("Case") : null, Tag.hasUUID("Reader") ? Tag.getUUID("Reader") : null,
                    Document, Tag.getInt("MinDistance"), Tag.getInt("MaxDistance"), Tag.getLong("FireAt"));
            Entry.Target = Tag.hasUUID("Target") ? Tag.getUUID("Target") : null;
            Entry.Exact = Tag.contains("Exact") ? BlockPos.of(Tag.getLong("Exact")) : null;
            Entry.ExactFrom = Tag.contains("FromX") ? new Vec3(Tag.getDouble("FromX"), Tag.getDouble("FromY"), Tag.getDouble("FromZ")) : null;
            Entry.Meteor = Tag.hasUUID("Meteor") ? Tag.getUUID("Meteor") : null;
            try {
                Entry.Phase = Stage.valueOf(Tag.getString("Stage"));
            } catch (IllegalArgumentException Problem) {
                Entry.Phase = Stage.Waiting;
            }
            if (Tag.contains("Dimension")) {
                Entry.Dimension = Tag.getString("Dimension");
            }
            Entry.Impact = BlockPos.of(Tag.getLong("Impact"));
            Entry.LaunchedAt = Tag.getLong("Launched");
            Entry.Front = Direction.from2DDataValue(Tag.getInt("Front"));
            Entry.Site = BlockPos.of(Tag.getLong("Site"));
            return Entry;
        }
    }
}
