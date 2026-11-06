package net.rai.scp_unleashed.scp553;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Scp553World extends SavedData {
    public static final String DataName = "scp_unleashed_scp553";
    private static final SavedData.Factory<Scp553World> DataFactory = new SavedData.Factory<>(Scp553World::new, Scp553World::load);

    private final Set<UUID> Members = new LinkedHashSet<>();

    public static Scp553World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public int count() {
        return Members.size();
    }

    public boolean holds(UUID Butterfly) {
        return Members.contains(Butterfly);
    }

    public void join(UUID Butterfly) {
        if (Members.add(Butterfly)) {
            setDirty();
        }
    }

    public void leave(UUID Butterfly) {
        if (Members.remove(Butterfly)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Registries) {
        ListTag Rows = new ListTag();
        for (UUID Member : Members) {
            Rows.add(NbtUtils.createUUID(Member));
        }
        Saved.put("Members", Rows);
        return Saved;
    }

    public static Scp553World load(CompoundTag Saved, HolderLookup.Provider Registries) {
        Scp553World Data = new Scp553World();
        ListTag Rows = Saved.getList("Members", Tag.TAG_INT_ARRAY);
        for (Tag Row : Rows) {
            Data.Members.add(NbtUtils.loadUUID(Row));
        }
        return Data;
    }
}
