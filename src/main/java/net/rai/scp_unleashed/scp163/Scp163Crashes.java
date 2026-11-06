package net.rai.scp_unleashed.scp163;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.scp002.Scp002Impact;
import net.rai.scp_unleashed.scp002.Scp002Strikes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp163Crashes extends SavedData {
    public static final String DataName = "scp_unleashed_scp163_crashes";
    public static final int RetryTicks = 100;
    public static final int WatchdogTicks = 300;
    private static final SavedData.Factory<Scp163Crashes> DataFactory = new SavedData.Factory<>(Scp163Crashes::new, Scp163Crashes::load);

    private final List<Crash> Pending = new ArrayList<>();

    public static Scp163Crashes get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public enum Stage {
        Waiting,
        Inbound,
        Landed
    }

    public List<Crash> list() {
        return List.copyOf(Pending);
    }

    public Optional<Crash> forCase(UUID CaseId) {
        for (Crash Entry : Pending) {
            if (CaseId.equals(Entry.CaseId)) {
                return Optional.of(Entry);
            }
        }
        return Optional.empty();
    }

    public Crash schedule(@Nullable UUID CaseId, @Nullable UUID Reader, @Nullable ResourceLocation Document, long FireAt, int MinDistance, int MaxDistance) {
        Crash Entry = new Crash(UUID.randomUUID(), CaseId, Reader, Document, MinDistance, MaxDistance, FireAt);
        Pending.add(Entry);
        setDirty();
        return Entry;
    }

    public Crash crashNear(ServerPlayer Target, int MinDistance, int MaxDistance) {
        Crash Entry = schedule(null, null, null, Target.server.overworld().getGameTime(), MinDistance, MaxDistance);
        Entry.Target = Target.getUUID();
        return Entry;
    }

    public Crash crashAt(ServerLevel World, BlockPos Ground, Vec3 From) {
        Crash Entry = schedule(null, null, null, World.getGameTime(), 0, 0);
        Entry.Exact = Ground;
        Entry.ExactFrom = From;
        Entry.Dimension = World.dimension().location().toString();
        return Entry;
    }

    public void tick(MinecraftServer Server) {
        if (Pending.isEmpty()) {
            return;
        }
        long Now = Server.overworld().getGameTime();
        boolean Second = Now % 20L == 0L;
        for (Crash Entry : List.copyOf(Pending)) {
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

    @Nullable
    private ServerPlayer pickTarget(MinecraftServer Server, Crash Entry) {
        if (Entry.Target != null) {
            ServerPlayer Forced = Server.getPlayerList().getPlayer(Entry.Target);
            return Forced != null && Scp002Strikes.isValidTarget(Forced) ? Forced : null;
        }
        if (Entry.Reader != null) {
            ServerPlayer Reader = Server.getPlayerList().getPlayer(Entry.Reader);
            if (Reader != null) {
                return Scp002Strikes.isValidTarget(Reader) && ReaderTargets.isReady(Reader) && AnomalyRealm.allows(Reader.level()) ? Reader : null;
            }
        }
        List<ServerPlayer> Candidates = Server.getPlayerList().getPlayers().stream()
                .filter(Candidate -> Scp002Strikes.isValidTarget(Candidate) && ReaderTargets.isReady(Candidate) && AnomalyRealm.allows(Candidate.level())).toList();
        if (Candidates.isEmpty()) {
            return null;
        }
        return Candidates.get(Server.overworld().random.nextInt(Candidates.size()));
    }

    private void fire(MinecraftServer Server, Crash Entry, long Now) {
        ServerLevel World;
        BlockPos Ground;
        Vec3 From;
        if (Entry.Exact != null) {
            World = levelOf(Server, Entry.Dimension);
            if (World == null || !Scp002Strikes.fallsIn(World)) {
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
            Notices.sendFalling(World, Entry.Document, Ground, Component.translatable("scp163.scp_unleashed.lead.falling"));
        }
        Direction Front = Scp002Impact.frontToward(Ground, From, World.random);
        Scp002Strikes.keepLoaded(World, Ground);
        Scp163CraftEntity Craft = Scp163CraftEntity.launch(World, Ground, Entry.Id, Front);
        if (Craft == null) {
            Entry.FireAt = Now + RetryTicks;
            setDirty();
            return;
        }
        Entry.Phase = Stage.Inbound;
        Entry.Dimension = World.dimension().location().toString();
        Entry.Impact = Ground;
        Entry.Craft = Craft.getUUID();
        Entry.LaunchedAt = Now;
        Entry.Front = Front;
        setDirty();
    }

    private void watch(MinecraftServer Server, Crash Entry, long Now) {
        ServerLevel World = levelOf(Server, Entry.Dimension);
        if (World == null) {
            Pending.remove(Entry);
            setDirty();
            return;
        }
        if (!Scp002Strikes.fallsIn(World)) {
            recall(Entry, Now);
            return;
        }
        Scp002Strikes.keepLoaded(World, Entry.Impact);
        if (Now - Entry.LaunchedAt < Scp163CraftEntity.FlightTicks + WatchdogTicks) {
            return;
        }
        Entity Found = Entry.Craft == null ? null : World.getEntity(Entry.Craft);
        if (Found instanceof Scp163CraftEntity Craft && Craft.isAlive()) {
            return;
        }
        Scp163Impact.Result Result = Scp163Impact.crash(World, Entry.Impact, Entry.Front, World.random);
        Vec3 Centre = Vec3.atBottomCenterOf(Entry.Impact.above());
        Scp163Sounds.impact(World, Centre, World.random);
        Scp163Impact.announce(World, Entry.Impact.above(), Result.Radius());
        landed(Entry.Id, World, Result.Hatch());
    }

    private void recall(Crash Entry, long Now) {
        if (Entry.Exact != null) {
            Pending.remove(Entry);
        } else {
            Entry.Phase = Stage.Waiting;
            Entry.FireAt = Now + RetryTicks;
            Entry.Dimension = Level.OVERWORLD.location().toString();
            Entry.Craft = null;
        }
        setDirty();
    }

    public void caseMoved(UUID CaseId, UUID Heir) {
        for (Crash Entry : Pending) {
            if (CaseId.equals(Entry.CaseId)) {
                Entry.Reader = Heir;
                setDirty();
            }
        }
    }

    public boolean isInbound(UUID CrashId) {
        for (Crash Entry : Pending) {
            if (Entry.Id.equals(CrashId)) {
                return Entry.Phase == Stage.Inbound;
            }
        }
        return false;
    }

    public void landed(UUID CrashId, ServerLevel World, BlockPos Hatch) {
        for (Crash Entry : Pending) {
            if (Entry.Id.equals(CrashId)) {
                Entry.Phase = Stage.Landed;
                Entry.Site = Hatch;
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
        Scp163Crashes Data = get(Found.World().getServer());
        UUID Reader = Found.Investigator().getUUID();
        Crash Match = null;
        for (Crash Entry : Data.Pending) {
            if (Entry.Phase == Stage.Landed && Reader.equals(Entry.Reader) && Found.DocumentId().equals(Entry.Document) && (Match == null || Found.Pos().equals(Entry.Site))) {
                Match = Entry;
            }
        }
        if (Match != null) {
            Data.Pending.remove(Match);
            Data.setDirty();
            return true;
        }
        if (!Scp002Strikes.fallsIn(Found.World())) {
            return false;
        }
        Data.crashAt(Found.World(), Found.Pos(), Found.Investigator().position());
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (Crash Entry : Pending) {
            Saved.add(Entry.save());
        }
        Tag.put("Crashes", Saved);
        return Tag;
    }

    private static Scp163Crashes load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp163Crashes Data = new Scp163Crashes();
        ListTag Saved = Tag.getList("Crashes", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            Crash Entry = Crash.load(Saved.getCompound(Index));
            if (Entry != null) {
                Data.Pending.add(Entry);
            }
        }
        return Data;
    }

    public static final class Crash {
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
        private UUID Craft;
        private long LaunchedAt;
        private Direction Front = Direction.SOUTH;
        private BlockPos Site = BlockPos.ZERO;

        private Crash(UUID Id, @Nullable UUID CaseId, @Nullable UUID Reader, @Nullable ResourceLocation Document, int MinDistance, int MaxDistance, long FireAt) {
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
            if (Craft != null) {
                Tag.putUUID("Craft", Craft);
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
        private static Crash load(CompoundTag Tag) {
            if (!Tag.hasUUID("Id")) {
                return null;
            }
            ResourceLocation Document = Tag.contains("Document") ? ResourceLocation.tryParse(Tag.getString("Document")) : null;
            Crash Entry = new Crash(Tag.getUUID("Id"), Tag.hasUUID("Case") ? Tag.getUUID("Case") : null, Tag.hasUUID("Reader") ? Tag.getUUID("Reader") : null,
                    Document, Tag.getInt("MinDistance"), Tag.getInt("MaxDistance"), Tag.getLong("FireAt"));
            Entry.Target = Tag.hasUUID("Target") ? Tag.getUUID("Target") : null;
            Entry.Exact = Tag.contains("Exact") ? BlockPos.of(Tag.getLong("Exact")) : null;
            Entry.ExactFrom = Tag.contains("FromX") ? new Vec3(Tag.getDouble("FromX"), Tag.getDouble("FromY"), Tag.getDouble("FromZ")) : null;
            Entry.Craft = Tag.hasUUID("Craft") ? Tag.getUUID("Craft") : null;
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
