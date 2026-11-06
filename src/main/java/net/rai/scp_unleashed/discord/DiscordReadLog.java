package net.rai.scp_unleashed.discord;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DiscordReadLog extends SavedData {
    public static final String DataName = "scp_unleashed_discord_reads";
    private static final SavedData.Factory<DiscordReadLog> DataFactory = new SavedData.Factory<>(DiscordReadLog::new, DiscordReadLog::load);

    private final Map<UUID, Set<ResourceLocation>> Opened = new LinkedHashMap<>();

    public static DiscordReadLog get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean has(UUID Reader, ResourceLocation Document) {
        Set<ResourceLocation> Seen = Opened.get(Reader);
        return Seen != null && Seen.contains(Document);
    }

    public boolean note(UUID Reader, ResourceLocation Document) {
        if (Opened.computeIfAbsent(Reader, Key -> new LinkedHashSet<>()).add(Document)) {
            setDirty();
            return true;
        }
        return false;
    }

    public Set<ResourceLocation> openedBy(UUID Reader) {
        Set<ResourceLocation> Seen = Opened.get(Reader);
        return Seen == null ? Set.of() : Collections.unmodifiableSet(Seen);
    }

    public void forget(UUID Reader) {
        if (Opened.remove(Reader) != null) {
            setDirty();
        }
    }

    public int total() {
        int Count = 0;
        for (Set<ResourceLocation> Seen : Opened.values()) {
            Count += Seen.size();
        }
        return Count;
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Lookup) {
        ListTag Readers = new ListTag();
        for (Map.Entry<UUID, Set<ResourceLocation>> Entry : Opened.entrySet()) {
            if (Entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Reader", Entry.getKey());
            ListTag Files = new ListTag();
            for (ResourceLocation Document : Entry.getValue()) {
                Files.add(StringTag.valueOf(Document.toString()));
            }
            Row.put("Opened", Files);
            Readers.add(Row);
        }
        Root.put("Readers", Readers);
        return Root;
    }

    public static DiscordReadLog load(CompoundTag Root, HolderLookup.Provider Lookup) {
        DiscordReadLog Data = new DiscordReadLog();
        ListTag Readers = Root.getList("Readers", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Readers.size(); Index++) {
            CompoundTag Row = Readers.getCompound(Index);
            if (!Row.hasUUID("Reader")) {
                continue;
            }
            Set<ResourceLocation> Seen = new LinkedHashSet<>();
            ListTag Files = Row.getList("Opened", Tag.TAG_STRING);
            for (int Slot = 0; Slot < Files.size(); Slot++) {
                ResourceLocation Document = ResourceLocation.tryParse(Files.getString(Slot));
                if (Document != null) {
                    Seen.add(Document);
                }
            }
            if (!Seen.isEmpty()) {
                Data.Opened.put(Row.getUUID("Reader"), Seen);
            }
        }
        return Data;
    }
}
