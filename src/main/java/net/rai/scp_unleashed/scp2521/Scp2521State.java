package net.rai.scp_unleashed.scp2521;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp2521State extends SavedData {
    public static final String DataName = "scp_unleashed_scp2521";
    private static final SavedData.Factory<Scp2521State> DataFactory = new SavedData.Factory<>(Scp2521State::new, Scp2521State::load);

    public record Holding(ResourceKey<Level> Dimension, BlockPos Pos, UUID Creature, float Yaw) {
        public CompoundTag save() {
            CompoundTag Row = new CompoundTag();
            Row.putString("Dimension", Dimension.location().toString());
            Row.putLong("Pos", Pos.asLong());
            Row.putUUID("Creature", Creature);
            Row.putFloat("Yaw", Yaw);
            return Row;
        }

        @Nullable
        public static Holding load(CompoundTag Row) {
            ResourceLocation Where = ResourceLocation.tryParse(Row.getString("Dimension"));
            if (Where == null || !Row.hasUUID("Creature")) {
                return null;
            }
            return new Holding(ResourceKey.create(Registries.DIMENSION, Where), BlockPos.of(Row.getLong("Pos")), Row.getUUID("Creature"), Row.getFloat("Yaw"));
        }
    }

    private final Set<UUID> Readers = new HashSet<>();
    private final Set<UUID> Taken = new HashSet<>();
    private final Map<Integer, UUID> Plots = new LinkedHashMap<>();
    @Nullable
    private Holding Held;

    public static Scp2521State get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean knows(UUID Id) {
        return Readers.contains(Id);
    }

    public boolean learn(UUID Id) {
        boolean Added = Readers.add(Id);
        if (Added) {
            setDirty();
        }
        return Added;
    }

    public boolean forget(UUID Id) {
        boolean Removed = Readers.remove(Id);
        if (Removed) {
            setDirty();
        }
        return Removed;
    }

    public int readers() {
        return Readers.size();
    }

    public boolean taken(UUID Id) {
        return Taken.contains(Id);
    }

    public void markTaken(UUID Id) {
        if (Taken.add(Id)) {
            setDirty();
        }
    }

    public int claimPlot(UUID Owner) {
        for (Map.Entry<Integer, UUID> Entry : Plots.entrySet()) {
            if (Entry.getValue().equals(Owner)) {
                return Entry.getKey();
            }
        }
        int Plot = 0;
        while (Plots.containsKey(Plot)) {
            Plot++;
        }
        Plots.put(Plot, Owner);
        setDirty();
        return Plot;
    }

    public void freePlot(int Plot) {
        if (Plots.remove(Plot) != null) {
            setDirty();
        }
    }

    @Nullable
    public UUID plotOwner(int Plot) {
        return Plots.get(Plot);
    }

    public Map<Integer, UUID> plots() {
        return new LinkedHashMap<>(Plots);
    }

    @Nullable
    public Holding held() {
        return Held;
    }

    public void hold(@Nullable Holding Made) {
        Held = Made;
        setDirty();
    }

    public void resetAll() {
        Readers.clear();
        Taken.clear();
        Plots.clear();
        Held = null;
        setDirty();
    }

    public static Scp2521State load(CompoundTag Saved, HolderLookup.Provider Registries) {
        Scp2521State Made = new Scp2521State();
        for (Tag Entry : Saved.getList("Readers", Tag.TAG_INT_ARRAY)) {
            Made.Readers.add(NbtUtils.loadUUID(Entry));
        }
        for (Tag Entry : Saved.getList("Taken", Tag.TAG_INT_ARRAY)) {
            Made.Taken.add(NbtUtils.loadUUID(Entry));
        }
        CompoundTag Plotted = Saved.getCompound("Plots");
        for (String Key : Plotted.getAllKeys()) {
            try {
                Made.Plots.put(Integer.parseInt(Key), Plotted.getUUID(Key));
            } catch (NumberFormatException Ignored) {
            }
        }
        if (Saved.contains("Held")) {
            Made.Held = Holding.load(Saved.getCompound("Held"));
        }
        return Made;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Registries) {
        ListTag Known = new ListTag();
        for (UUID Id : Readers) {
            Known.add(NbtUtils.createUUID(Id));
        }
        Saved.put("Readers", Known);
        ListTag Gone = new ListTag();
        for (UUID Id : Taken) {
            Gone.add(NbtUtils.createUUID(Id));
        }
        Saved.put("Taken", Gone);
        CompoundTag Plotted = new CompoundTag();
        for (Map.Entry<Integer, UUID> Entry : Plots.entrySet()) {
            Plotted.putUUID(Integer.toString(Entry.getKey()), Entry.getValue());
        }
        Saved.put("Plots", Plotted);
        if (Held != null) {
            Saved.put("Held", Held.save());
        }
        return Saved;
    }
}
