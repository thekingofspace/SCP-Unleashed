package net.rai.scp_unleashed.scp902;

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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp902State extends SavedData {
    public static final String DataName = "scp_unleashed_scp902";
    public static final int StageLeaving = 0;
    public static final int StageScene = 1;
    public static final int StageHome = 2;
    private static final SavedData.Factory<Scp902State> DataFactory = new SavedData.Factory<>(Scp902State::new, Scp902State::load);

    public static final class Trip {
        public final UUID Owner;
        public final ResourceKey<Level> Dimension;
        public final Vec3 Spot;
        public final float Yaw;
        public final float Pitch;
        public final int Slot;
        public final UUID Copy;
        public int Stage = StageLeaving;
        public int Ticks;
        public int Food = -1;
        public float Saturation;
        public boolean Hurried;
        public boolean Placed;
        public int Still;
        public int Rescues;
        @Nullable
        public Vec3 LastSpot;

        public Trip(UUID Owner, ResourceKey<Level> Dimension, Vec3 Spot, float Yaw, float Pitch, int Slot, UUID Copy) {
            this.Owner = Owner;
            this.Dimension = Dimension;
            this.Spot = Spot;
            this.Yaw = Yaw;
            this.Pitch = Pitch;
            this.Slot = Slot;
            this.Copy = Copy;
        }

        public CompoundTag save() {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Owner", Owner);
            Row.putString("Dimension", Dimension.location().toString());
            Row.putDouble("X", Spot.x);
            Row.putDouble("Y", Spot.y);
            Row.putDouble("Z", Spot.z);
            Row.putFloat("Yaw", Yaw);
            Row.putFloat("Pitch", Pitch);
            Row.putInt("Slot", Slot);
            Row.putUUID("Copy", Copy);
            Row.putInt("Stage", Stage);
            Row.putInt("Ticks", Ticks);
            Row.putInt("Food", Food);
            Row.putFloat("Saturation", Saturation);
            Row.putBoolean("Placed", Placed);
            return Row;
        }

        @Nullable
        public static Trip load(CompoundTag Row) {
            ResourceLocation Where = ResourceLocation.tryParse(Row.getString("Dimension"));
            if (Where == null || !Row.hasUUID("Owner") || !Row.hasUUID("Copy")) {
                return null;
            }
            Trip Made = new Trip(Row.getUUID("Owner"), ResourceKey.create(Registries.DIMENSION, Where), new Vec3(Row.getDouble("X"), Row.getDouble("Y"), Row.getDouble("Z")),
                    Row.getFloat("Yaw"), Row.getFloat("Pitch"), Row.getInt("Slot"), Row.getUUID("Copy"));
            Made.Stage = Row.getInt("Stage");
            Made.Ticks = Row.getInt("Ticks");
            Made.Food = Row.contains("Food") ? Row.getInt("Food") : -1;
            Made.Saturation = Row.getFloat("Saturation");
            Made.Placed = Row.getBoolean("Placed");
            return Made;
        }
    }

    private final Set<UUID> Visited = new HashSet<>();
    private final Map<UUID, Trip> Trips = new LinkedHashMap<>();
    private final Map<Integer, Integer> Built = new HashMap<>();
    private final Set<Integer> Dirty = new LinkedHashSet<>();
    private int Builds;

    public static Scp902State get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean visited(UUID Id) {
        return Visited.contains(Id);
    }

    public void markVisited(UUID Id) {
        if (Visited.add(Id)) {
            setDirty();
        }
    }

    public boolean forgive(UUID Id) {
        boolean Removed = Visited.remove(Id);
        if (Removed) {
            setDirty();
        }
        return Removed;
    }

    @Nullable
    public Trip trip(UUID Id) {
        return Trips.get(Id);
    }

    public List<Trip> trips() {
        return new ArrayList<>(Trips.values());
    }

    public void start(Trip Made) {
        Trips.put(Made.Owner, Made);
        setDirty();
    }

    public void finish(UUID Id) {
        Trip Ended = Trips.remove(Id);
        if (Ended != null) {
            Dirty.add(Ended.Slot);
            setDirty();
        }
    }

    public void resetAll() {
        Visited.clear();
        Trips.clear();
        Built.clear();
        Dirty.clear();
        setDirty();
    }

    public boolean dirty(int Slot) {
        return Dirty.contains(Slot);
    }

    public List<Integer> dirtySlots() {
        return new ArrayList<>(Dirty);
    }

    public void markDirty(int Slot) {
        if (Dirty.add(Slot)) {
            setDirty();
        }
    }

    public void cleaned(int Slot) {
        Dirty.remove(Slot);
        Built.remove(Slot);
        setDirty();
    }

    public boolean occupied(int Slot) {
        for (Trip Each : Trips.values()) {
            if (Each.Slot == Slot) {
                return true;
            }
        }
        return false;
    }

    public boolean wants(@Nullable UUID Owner, UUID CopyId) {
        if (Owner == null) {
            return false;
        }
        Trip Found = Trips.get(Owner);
        return Found != null && Found.Copy.equals(CopyId);
    }

    public int freeSlot() {
        Set<Integer> Taken = new HashSet<>();
        for (Trip Each : Trips.values()) {
            Taken.add(Each.Slot);
        }
        Taken.addAll(Dirty);
        int Slot = 0;
        while (Taken.contains(Slot)) {
            Slot++;
        }
        return Slot;
    }

    public boolean built(int Slot, int Signature) {
        Integer Found = Built.get(Slot);
        return Found != null && Found == Signature;
    }

    public void markBuilt(int Slot, int Signature) {
        Built.put(Slot, Signature);
        Builds++;
        setDirty();
    }

    public int builds() {
        return Builds;
    }

    public static Scp902State load(CompoundTag Saved, HolderLookup.Provider Registries) {
        Scp902State Made = new Scp902State();
        for (Tag Entry : Saved.getList("Visited", Tag.TAG_INT_ARRAY)) {
            Made.Visited.add(NbtUtils.loadUUID(Entry));
        }
        for (Tag Entry : Saved.getList("Trips", Tag.TAG_COMPOUND)) {
            Trip Found = Trip.load((CompoundTag) Entry);
            if (Found != null) {
                Made.Trips.put(Found.Owner, Found);
            }
        }
        CompoundTag Scenes = Saved.getCompound("Built");
        for (String Key : Scenes.getAllKeys()) {
            try {
                Made.Built.put(Integer.parseInt(Key), Scenes.getInt(Key));
            } catch (NumberFormatException Ignored) {
            }
        }
        Made.Builds = Saved.getInt("Builds");
        for (int Slot : Saved.getIntArray("Dirty")) {
            Made.Dirty.add(Slot);
        }
        return Made;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Registries) {
        ListTag Seen = new ListTag();
        for (UUID Id : Visited) {
            Seen.add(NbtUtils.createUUID(Id));
        }
        Saved.put("Visited", Seen);
        ListTag Open = new ListTag();
        for (Trip Each : Trips.values()) {
            Open.add(Each.save());
        }
        Saved.put("Trips", Open);
        CompoundTag Scenes = new CompoundTag();
        for (Map.Entry<Integer, Integer> Entry : Built.entrySet()) {
            Scenes.putInt(Integer.toString(Entry.getKey()), Entry.getValue());
        }
        Saved.put("Built", Scenes);
        Saved.putInt("Builds", Builds);
        Saved.putIntArray("Dirty", Dirty.stream().mapToInt(Integer::intValue).toArray());
        return Saved;
    }
}
