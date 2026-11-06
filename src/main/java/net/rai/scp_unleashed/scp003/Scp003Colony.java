package net.rai.scp_unleashed.scp003;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp003Colony extends SavedData {
    public static final String DataName = "scp_unleashed_scp003_colony";
    public static final int PurgeInterval = 40;
    public static final int PromoteInterval = 100;
    public static final double PromoteRange = 64.0;
    public static final int PromoteCount = 3;
    private static final SavedData.Factory<Scp003Colony> DataFactory = new SavedData.Factory<>(Scp003Colony::new, Scp003Colony::load);

    private final Map<UUID, List<Placed>> Tracked = new HashMap<>();
    private final List<Placed> Purges = new ArrayList<>();
    @Nullable
    private UUID AlphaLineage;
    @Nullable
    private UUID AlphaEntity;
    private String AlphaDimension = "";
    private long AlphaPos;

    public record Placed(String Dimension, long Pos) {
        public BlockPos blockPos() {
            return BlockPos.of(Pos);
        }

        public CompoundTag save() {
            CompoundTag Tag = new CompoundTag();
            Tag.putString("Dimension", Dimension);
            Tag.putLong("Pos", Pos);
            return Tag;
        }

        public static Placed load(CompoundTag Tag) {
            return new Placed(Tag.getString("Dimension"), Tag.getLong("Pos"));
        }
    }

    public static Scp003Colony get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public void track(UUID Lineage, ServerLevel World, BlockPos Pos) {
        List<Placed> Cells = Tracked.computeIfAbsent(Lineage, Key -> new ArrayList<>());
        Placed Entry = new Placed(World.dimension().location().toString(), Pos.asLong());
        if (!Cells.contains(Entry)) {
            Cells.add(Entry);
            setDirty();
        }
    }

    public void forget(UUID Lineage) {
        if (Tracked.remove(Lineage) != null) {
            setDirty();
        }
    }

    public List<Placed> trackedFor(UUID Lineage) {
        List<Placed> Found = Tracked.get(Lineage);
        return Found == null ? List.of() : List.copyOf(Found);
    }

    public int trackedCount() {
        int Count = 0;
        for (List<Placed> Cells : Tracked.values()) {
            Count += Cells.size();
        }
        return Count;
    }

    public int pendingPurges() {
        return Purges.size();
    }

    public void purge(UUID Lineage, MinecraftServer Server) {
        List<Placed> Cells = Tracked.remove(Lineage);
        if (Cells == null) {
            return;
        }
        for (Placed Entry : Cells) {
            ServerLevel World = levelOf(Server, Entry.Dimension());
            if (World == null) {
                continue;
            }
            if (World.isLoaded(Entry.blockPos())) {
                removeIfOurs(World, Entry.blockPos());
            } else {
                Purges.add(Entry);
            }
        }
        setDirty();
    }

    private static void removeIfOurs(ServerLevel World, BlockPos Pos) {
        if (World.getBlockState(Pos).getBlock() instanceof Scp003PolypBlock) {
            World.destroyBlock(Pos, false);
        }
    }

    public void creatureGone(Scp003Creature Creature, ServerLevel World) {
        purge(Creature.lineage(), World.getServer());
        if (isAlpha(Creature.lineage())) {
            clearAlpha(Creature.lineage());
        }
    }

    public boolean hasAlpha() {
        return AlphaLineage != null;
    }

    public boolean isAlpha(UUID Lineage) {
        return Lineage.equals(AlphaLineage);
    }

    @Nullable
    public UUID alphaEntity() {
        return AlphaEntity;
    }

    public String alphaDimension() {
        return AlphaDimension;
    }

    public BlockPos alphaPos() {
        return BlockPos.of(AlphaPos);
    }

    public void setAlpha(Scp003Alpha Alpha, ServerLevel World) {
        AlphaLineage = Alpha.lineage();
        seeAlpha(Alpha, World);
    }

    public void seeAlpha(Scp003Alpha Alpha, ServerLevel World) {
        if (AlphaLineage == null) {
            AlphaLineage = Alpha.lineage();
        }
        if (!AlphaLineage.equals(Alpha.lineage())) {
            return;
        }
        AlphaEntity = Alpha.getUUID();
        AlphaDimension = World.dimension().location().toString();
        AlphaPos = Alpha.blockPosition().asLong();
        setDirty();
    }

    public void clearAlpha(UUID Lineage) {
        if (Lineage.equals(AlphaLineage)) {
            AlphaLineage = null;
            AlphaEntity = null;
            AlphaDimension = "";
            AlphaPos = 0L;
            setDirty();
        }
    }

    public void tick(MinecraftServer Server) {
        long Now = Server.overworld().getGameTime();
        if (Now % PurgeInterval == 0L && !Purges.isEmpty()) {
            processPurges(Server);
        }
        if (Now % PromoteInterval == 0L) {
            validateAlpha(Server);
            if (!hasAlpha()) {
                promote(Server);
            }
        }
    }

    private void processPurges(MinecraftServer Server) {
        boolean Changed = false;
        for (Placed Entry : List.copyOf(Purges)) {
            ServerLevel World = levelOf(Server, Entry.Dimension());
            if (World == null) {
                Purges.remove(Entry);
                Changed = true;
                continue;
            }
            if (World.isLoaded(Entry.blockPos())) {
                removeIfOurs(World, Entry.blockPos());
                Purges.remove(Entry);
                Changed = true;
            }
        }
        if (Changed) {
            setDirty();
        }
    }

    private void validateAlpha(MinecraftServer Server) {
        if (AlphaLineage == null) {
            return;
        }
        ServerLevel World = levelOf(Server, AlphaDimension);
        if (World == null) {
            clearAlpha(AlphaLineage);
            return;
        }
        Entity Found = AlphaEntity == null ? null : World.getEntity(AlphaEntity);
        if (Found instanceof Scp003Alpha Alpha && Alpha.isAlive()) {
            seeAlpha(Alpha, World);
            return;
        }
        ChunkPos Centre = new ChunkPos(BlockPos.of(AlphaPos));
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                if (!World.areEntitiesLoaded(ChunkPos.asLong(Centre.x + Dx, Centre.z + Dz))) {
                    return;
                }
            }
        }
        clearAlpha(AlphaLineage);
    }

    private void promote(MinecraftServer Server) {
        for (ServerLevel World : Server.getAllLevels()) {
            List<? extends Scp003Creature> Living = World.getEntities(EntityTypeTest.forClass(Scp003Creature.class), Creature -> Creature.isAlive() && !Creature.isTransferred());
            if (Living.size() < PromoteCount) {
                continue;
            }
            Scp003Creature Best = null;
            for (Scp003Creature Candidate : Living) {
                if (Candidate.stage() < 2 || Candidate.stage() > 3 || Candidate.locksMovement()) {
                    continue;
                }
                int Near = 0;
                for (Scp003Creature Other : Living) {
                    if (Other != Candidate && Other.distanceToSqr(Candidate) <= PromoteRange * PromoteRange) {
                        Near++;
                    }
                }
                if (Near + 1 < PromoteCount) {
                    continue;
                }
                if (Best == null || rank().compare(Candidate, Best) > 0) {
                    Best = Candidate;
                }
            }
            if (Best != null) {
                promoteNow(World, Best);
                return;
            }
        }
    }

    private static Comparator<Scp003Creature> rank() {
        return Comparator.<Scp003Creature>comparingInt(Scp003Creature::stage).thenComparingInt(Scp003Creature::kills).thenComparingInt(Scp003Creature::lifeTicks);
    }

    @Nullable
    public Scp003Alpha promoteNow(ServerLevel World, Scp003Creature Chosen) {
        if (hasAlpha() || Chosen instanceof Scp003Alpha) {
            return null;
        }
        World.playSound(null, Chosen.getX(), Chosen.getY() + 1.0, Chosen.getZ(), Scp003Registry.Promote.get(), SoundSource.HOSTILE, 1.4f, 1.0f);
        World.playSound(null, Chosen.getX(), Chosen.getY() + 1.0, Chosen.getZ(), Scp003Registry.PromoteLow.get(), SoundSource.HOSTILE, 1.2f, 1.0f);
        World.sendParticles(Scp003Registry.Flesh.get(), Chosen.getX(), Chosen.getY() + Chosen.getBbHeight() * 0.5, Chosen.getZ(), 40, 0.5, 0.6, 0.5, 0.15);
        World.sendParticles(Scp003Registry.Heal.get(), Chosen.getX(), Chosen.getY() + Chosen.getBbHeight() * 0.5, Chosen.getZ(), 24, 0.6, 0.8, 0.6, 0.05);
        return Scp003Growth.transform(Chosen, Scp003Registry.Alpha.get(), Scp003Creature.ActionPromote, Scp003Creature.PromoteTicks);
    }

    @Nullable
    private static ServerLevel levelOf(MinecraftServer Server, String Dimension) {
        ResourceLocation Id = ResourceLocation.tryParse(Dimension);
        return Id == null ? null : Server.getLevel(ResourceKey.create(Registries.DIMENSION, Id));
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Owners = new ListTag();
        for (Map.Entry<UUID, List<Placed>> Group : Tracked.entrySet()) {
            CompoundTag Owner = new CompoundTag();
            Owner.putUUID("Lineage", Group.getKey());
            ListTag Cells = new ListTag();
            for (Placed Entry : Group.getValue()) {
                Cells.add(Entry.save());
            }
            Owner.put("Blocks", Cells);
            Owners.add(Owner);
        }
        Tag.put("Tracked", Owners);
        ListTag Pending = new ListTag();
        for (Placed Entry : Purges) {
            Pending.add(Entry.save());
        }
        Tag.put("Purges", Pending);
        if (AlphaLineage != null) {
            Tag.putUUID("AlphaLineage", AlphaLineage);
            if (AlphaEntity != null) {
                Tag.putUUID("AlphaEntity", AlphaEntity);
            }
            Tag.putString("AlphaDimension", AlphaDimension);
            Tag.put("AlphaPos", LongTag.valueOf(AlphaPos));
        }
        return Tag;
    }

    private static Scp003Colony load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp003Colony Data = new Scp003Colony();
        ListTag Owners = Tag.getList("Tracked", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Owners.size(); Index++) {
            CompoundTag Owner = Owners.getCompound(Index);
            if (!Owner.hasUUID("Lineage")) {
                continue;
            }
            List<Placed> Cells = new ArrayList<>();
            ListTag Saved = Owner.getList("Blocks", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int Slot = 0; Slot < Saved.size(); Slot++) {
                Cells.add(Placed.load(Saved.getCompound(Slot)));
            }
            if (!Cells.isEmpty()) {
                Data.Tracked.put(Owner.getUUID("Lineage"), Cells);
            }
        }
        ListTag Pending = Tag.getList("Purges", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Pending.size(); Index++) {
            Data.Purges.add(Placed.load(Pending.getCompound(Index)));
        }
        if (Tag.hasUUID("AlphaLineage")) {
            Data.AlphaLineage = Tag.getUUID("AlphaLineage");
            Data.AlphaEntity = Tag.hasUUID("AlphaEntity") ? Tag.getUUID("AlphaEntity") : null;
            Data.AlphaDimension = Tag.getString("AlphaDimension");
            Data.AlphaPos = Tag.getLong("AlphaPos");
        }
        return Data;
    }
}
