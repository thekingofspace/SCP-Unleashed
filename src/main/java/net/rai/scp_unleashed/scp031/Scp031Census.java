package net.rai.scp_unleashed.scp031;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Scp031Census extends SavedData {
    public static final String DataName = "scp_unleashed_scp031_census";
    public static final int Cap = 12;
    public static final long ForgetAfter = 72000L;
    private static final SavedData.Factory<Scp031Census> DataFactory = new SavedData.Factory<>(Scp031Census::new, Scp031Census::load);

    private final Map<UUID, Long> Seen = new HashMap<>();

    public static Scp031Census get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public void touch(UUID Member, long Now) {
        Long Before = Seen.put(Member, Now);
        if (Before == null || Now - Before > 1200L) {
            setDirty();
        }
    }

    public void free(UUID Member) {
        if (Seen.remove(Member) != null) {
            setDirty();
        }
    }

    public int count(long Now) {
        if (Seen.values().removeIf(Last -> Now - Last > ForgetAfter || Last > Now + 20L)) {
            setDirty();
        }
        return Seen.size();
    }

    public boolean full(long Now) {
        return count(Now) >= Cap;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Members = new ListTag();
        for (Map.Entry<UUID, Long> Entry : Seen.entrySet()) {
            CompoundTag Member = new CompoundTag();
            Member.putUUID("Id", Entry.getKey());
            Member.putLong("Seen", Entry.getValue());
            Members.add(Member);
        }
        Tag.put("Members", Members);
        return Tag;
    }

    public static Scp031Census load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp031Census Census = new Scp031Census();
        ListTag Members = Tag.getList("Members", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Members.size(); Index++) {
            CompoundTag Member = Members.getCompound(Index);
            if (Member.hasUUID("Id")) {
                Census.Seen.put(Member.getUUID("Id"), Member.getLong("Seen"));
            }
        }
        return Census;
    }
}
