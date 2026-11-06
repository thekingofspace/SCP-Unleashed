package net.rai.scp_unleashed.orbital;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OrbitalArray extends SavedData {
    public static final String DataName = "scp_unleashed_orbital_array";
    private static final SavedData.Factory<OrbitalArray> DataFactory = new SavedData.Factory<>(OrbitalArray::new, OrbitalArray::load);

    public static final class Holding {
        private final EnumSet<OrbitalPart> Parts = EnumSet.noneOf(OrbitalPart.class);
        private final Map<OrbitalPart, Long> Launched = new HashMap<>();
        private long ReadyAt = Long.MIN_VALUE;
        private int Shots;

        public Set<OrbitalPart> getParts() {
            return Set.copyOf(Parts);
        }

        public long getReadyAt() {
            return ReadyAt;
        }

        public int getShots() {
            return Shots;
        }

        public long launchedAt(OrbitalPart Part) {
            return Launched.getOrDefault(Part, -1L);
        }

        public int mask() {
            int Mask = 0;
            for (OrbitalPart Part : Parts) {
                Mask |= Part.bit();
            }
            return Mask;
        }
    }

    private final Map<UUID, Holding> Owners = new LinkedHashMap<>();

    public static OrbitalArray get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    private Holding holding(UUID Owner) {
        return Owners.computeIfAbsent(Owner, Key -> new Holding());
    }

    public boolean record(UUID Owner, OrbitalPart Part, long At) {
        Holding Entry = holding(Owner);
        if (!Entry.Parts.add(Part)) {
            return false;
        }
        Entry.Launched.put(Part, At);
        setDirty();
        return true;
    }

    public boolean has(UUID Owner, OrbitalPart Part) {
        Holding Entry = Owners.get(Owner);
        return Entry != null && Entry.Parts.contains(Part);
    }

    public int count(UUID Owner) {
        Holding Entry = Owners.get(Owner);
        return Entry == null ? 0 : Entry.Parts.size();
    }

    public int mask(UUID Owner) {
        Holding Entry = Owners.get(Owner);
        return Entry == null ? 0 : Entry.mask();
    }

    public boolean complete(UUID Owner) {
        return count(Owner) >= OrbitalPart.Count;
    }

    public long readyAt(UUID Owner) {
        Holding Entry = Owners.get(Owner);
        return Entry == null ? Long.MIN_VALUE : Entry.ReadyAt;
    }

    public int shots(UUID Owner) {
        Holding Entry = Owners.get(Owner);
        return Entry == null ? 0 : Entry.Shots;
    }

    public void fired(UUID Owner, long ReadyAt) {
        Holding Entry = holding(Owner);
        Entry.ReadyAt = ReadyAt;
        Entry.Shots++;
        setDirty();
    }

    public void recharge(UUID Owner) {
        Holding Entry = Owners.get(Owner);
        if (Entry != null) {
            Entry.ReadyAt = Long.MIN_VALUE;
            setDirty();
        }
    }

    public boolean clear(UUID Owner) {
        boolean Removed = Owners.remove(Owner) != null;
        if (Removed) {
            setDirty();
        }
        return Removed;
    }

    public Map<UUID, Holding> all() {
        return Map.copyOf(Owners);
    }

    public static OrbitalArray load(CompoundTag Data, HolderLookup.Provider Registries) {
        OrbitalArray Array = new OrbitalArray();
        ListTag Entries = Data.getList("Owners", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size(); Index++) {
            CompoundTag Entry = Entries.getCompound(Index);
            if (!Entry.hasUUID("Owner")) {
                continue;
            }
            Holding Made = Array.holding(Entry.getUUID("Owner"));
            ListTag Names = Entry.getList("Parts", Tag.TAG_STRING);
            for (int Part = 0; Part < Names.size(); Part++) {
                OrbitalPart.byName(Names.getString(Part)).ifPresent(Made.Parts::add);
            }
            CompoundTag Times = Entry.getCompound("Launched");
            for (OrbitalPart Part : Made.Parts) {
                if (Times.contains(Part.getSerializedName())) {
                    Made.Launched.put(Part, Times.getLong(Part.getSerializedName()));
                }
            }
            Made.ReadyAt = Entry.contains("ReadyAt") ? Entry.getLong("ReadyAt") : Long.MIN_VALUE;
            Made.Shots = Entry.getInt("Shots");
        }
        return Array;
    }

    @Override
    public CompoundTag save(CompoundTag Data, HolderLookup.Provider Registries) {
        ListTag Entries = new ListTag();
        for (Map.Entry<UUID, Holding> Pair : Owners.entrySet()) {
            Holding Value = Pair.getValue();
            CompoundTag Entry = new CompoundTag();
            Entry.putUUID("Owner", Pair.getKey());
            ListTag Names = new ListTag();
            CompoundTag Times = new CompoundTag();
            for (OrbitalPart Part : Value.Parts) {
                Names.add(StringTag.valueOf(Part.getSerializedName()));
                Times.putLong(Part.getSerializedName(), Value.Launched.getOrDefault(Part, 0L));
            }
            Entry.put("Parts", Names);
            Entry.put("Launched", Times);
            Entry.putLong("ReadyAt", Value.ReadyAt);
            Entry.putInt("Shots", Value.Shots);
            Entries.add(Entry);
        }
        Data.put("Owners", Entries);
        return Data;
    }
}
