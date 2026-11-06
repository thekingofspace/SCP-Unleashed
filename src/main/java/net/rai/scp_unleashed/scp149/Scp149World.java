package net.rai.scp_unleashed.scp149;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp149World extends SavedData {
    public static final String DataName = "scp_unleashed_scp149";
    public static final int Cap = 26;
    public static final long DayTicks = 24000L;
    public static final long HungerTicks = 3L * DayTicks;
    private static final SavedData.Factory<Scp149World> DataFactory = new SavedData.Factory<>(Scp149World::new, Scp149World::load);

    private final Map<UUID, UUID> Members = new LinkedHashMap<>();
    private final Map<UUID, Long> Fed = new LinkedHashMap<>();

    public static Scp149World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static long now(MinecraftServer Server) {
        return Server.overworld().getGameTime();
    }

    public int count() {
        return Members.size();
    }

    public boolean room() {
        return Members.size() < Cap;
    }

    public boolean holds(UUID Fly) {
        return Members.containsKey(Fly);
    }

    @Nullable
    public UUID groupOf(UUID Fly) {
        return Members.get(Fly);
    }

    public void join(UUID Fly, UUID Group, long Now) {
        UUID Before = Members.put(Fly, Group);
        if (!Fed.containsKey(Group)) {
            Fed.put(Group, Now);
        }
        if (Before != null && !Before.equals(Group)) {
            prune(Before);
        }
        setDirty();
    }

    public void leave(UUID Fly) {
        UUID Group = Members.remove(Fly);
        if (Group != null) {
            prune(Group);
            setDirty();
        }
    }

    private void prune(UUID Group) {
        if (!Members.containsValue(Group)) {
            Fed.remove(Group);
        }
    }

    public long fedAt(UUID Group, long Now) {
        return Fed.getOrDefault(Group, Now);
    }

    public void feed(UUID Group, long Now) {
        Fed.put(Group, Math.max(Now, Fed.getOrDefault(Group, Now)));
        setDirty();
    }

    public void setFed(UUID Group, long Time) {
        if (Members.containsValue(Group)) {
            Fed.put(Group, Time);
            setDirty();
        }
    }

    public boolean restless(UUID Group, long Now) {
        return Now - fedAt(Group, Now) >= HungerTicks;
    }

    public double daysHungry(UUID Group, long Now) {
        return (Now - fedAt(Group, Now)) / (double) DayTicks;
    }

    public UUID merge(UUID Keep, UUID Other) {
        if (Keep.equals(Other)) {
            return Keep;
        }
        long Latest = Math.max(Fed.getOrDefault(Keep, Long.MIN_VALUE), Fed.getOrDefault(Other, Long.MIN_VALUE));
        for (Map.Entry<UUID, UUID> Entry : Members.entrySet()) {
            if (Entry.getValue().equals(Other)) {
                Entry.setValue(Keep);
            }
        }
        Fed.remove(Other);
        if (Latest != Long.MIN_VALUE) {
            Fed.put(Keep, Latest);
        }
        setDirty();
        return Keep;
    }

    public int members(UUID Group) {
        int Count = 0;
        for (UUID Value : Members.values()) {
            if (Value.equals(Group)) {
                Count++;
            }
        }
        return Count;
    }

    public Set<UUID> groups() {
        return new LinkedHashSet<>(Members.values());
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Flies = new ListTag();
        for (Map.Entry<UUID, UUID> Entry : Members.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Fly", Entry.getKey());
            Row.putUUID("Group", Entry.getValue());
            Flies.add(Row);
        }
        Tag.put("Flies", Flies);
        ListTag Groups = new ListTag();
        for (Map.Entry<UUID, Long> Entry : Fed.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Group", Entry.getKey());
            Row.putLong("Fed", Entry.getValue());
            Groups.add(Row);
        }
        Tag.put("Groups", Groups);
        return Tag;
    }

    public static Scp149World load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp149World Data = new Scp149World();
        ListTag Flies = Tag.getList("Flies", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Flies.size(); Index++) {
            CompoundTag Row = Flies.getCompound(Index);
            if (Row.hasUUID("Fly") && Row.hasUUID("Group")) {
                Data.Members.put(Row.getUUID("Fly"), Row.getUUID("Group"));
            }
        }
        Map<UUID, Long> Saved = new HashMap<>();
        ListTag Groups = Tag.getList("Groups", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Groups.size(); Index++) {
            CompoundTag Row = Groups.getCompound(Index);
            if (Row.hasUUID("Group")) {
                Saved.put(Row.getUUID("Group"), Row.getLong("Fed"));
            }
        }
        for (UUID Group : Data.Members.values()) {
            if (Saved.containsKey(Group)) {
                Data.Fed.put(Group, Saved.get(Group));
            }
        }
        return Data;
    }
}
