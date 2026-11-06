package net.rai.scp_unleashed.daevitegod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DaeviteGodLedger extends SavedData {
    public static final String DataName = "scp_unleashed_daevite_god";
    private static final SavedData.Factory<DaeviteGodLedger> DataFactory = new SavedData.Factory<>(DaeviteGodLedger::new, DaeviteGodLedger::load);

    @Nullable
    private UUID GodId;
    @Nullable
    private ResourceKey<Level> Dimension;
    private BlockPos Pos = BlockPos.ZERO;
    private int Stage = 1;
    private float Health = -1.0f;
    private boolean Alive;
    private int Slain;
    private final Set<UUID> Banished = new HashSet<>();

    public static DaeviteGodLedger get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean alive() {
        return Alive && GodId != null;
    }

    @Nullable
    public UUID godId() {
        return GodId;
    }

    @Nullable
    public ResourceKey<Level> dimension() {
        return Dimension;
    }

    public BlockPos pos() {
        return Pos;
    }

    public int stage() {
        return Stage;
    }

    public float health() {
        return Health;
    }

    public int slain() {
        return Slain;
    }

    public boolean owns(DaeviteGod God) {
        return alive() && God.getUUID().equals(GodId);
    }

    public boolean supersedes(DaeviteGod God) {
        return alive() && !God.getUUID().equals(GodId);
    }

    public void claim(DaeviteGod God) {
        GodId = God.getUUID();
        Alive = true;
        update(God);
    }

    public void update(DaeviteGod God) {
        if (!owns(God)) {
            return;
        }
        Dimension = God.level().dimension();
        Pos = God.blockPosition();
        Stage = God.stage();
        Health = God.getHealth();
        setDirty();
    }

    public void slain(DaeviteGod God) {
        if (GodId != null && GodId.equals(God.getUUID())) {
            Alive = false;
            GodId = null;
            Slain++;
            setDirty();
        }
    }

    public void forget() {
        if (GodId != null) {
            Banished.add(GodId);
        }
        Alive = false;
        GodId = null;
        setDirty();
    }

    public boolean banished(UUID Id) {
        return Banished.contains(Id);
    }

    @Nullable
    public DaeviteGod live(MinecraftServer Server) {
        if (!alive() || Dimension == null) {
            return null;
        }
        ServerLevel World = Server.getLevel(Dimension);
        if (World == null) {
            return null;
        }
        Entity Found = World.getEntity(GodId);
        return Found instanceof DaeviteGod God && God.isAlive() && !God.isRemoved() ? God : null;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        if (GodId != null) {
            Saved.putUUID("God", GodId);
        }
        if (Dimension != null) {
            Saved.putString("Dimension", Dimension.location().toString());
        }
        Saved.put("Pos", NbtUtils.writeBlockPos(Pos));
        Saved.putInt("Stage", Stage);
        Saved.putFloat("Health", Health);
        Saved.putBoolean("Alive", Alive);
        Saved.putInt("Slain", Slain);
        ListTag Gone = new ListTag();
        for (UUID Id : Banished) {
            Gone.add(NbtUtils.createUUID(Id));
        }
        Saved.put("Banished", Gone);
        return Saved;
    }

    public static DaeviteGodLedger load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        DaeviteGodLedger Data = new DaeviteGodLedger();
        Data.GodId = Saved.hasUUID("God") ? Saved.getUUID("God") : null;
        ResourceLocation Key = ResourceLocation.tryParse(Saved.getString("Dimension"));
        Data.Dimension = Key == null || Saved.getString("Dimension").isEmpty() ? null : ResourceKey.create(Registries.DIMENSION, Key);
        Data.Pos = NbtUtils.readBlockPos(Saved, "Pos").orElse(BlockPos.ZERO);
        Data.Stage = Math.max(1, Saved.getInt("Stage"));
        Data.Health = Saved.contains("Health") ? Saved.getFloat("Health") : -1.0f;
        Data.Alive = Saved.getBoolean("Alive");
        Data.Slain = Saved.getInt("Slain");
        for (Tag Entry : Saved.getList("Banished", Tag.TAG_INT_ARRAY)) {
            Data.Banished.add(NbtUtils.loadUUID(Entry));
        }
        return Data;
    }
}
