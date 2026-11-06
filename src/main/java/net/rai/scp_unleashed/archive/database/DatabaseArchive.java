package net.rai.scp_unleashed.archive.database;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DatabaseArchive extends SavedData {
    public static final String DataName = "scp_unleashed_archive_database";
    private static final Logger Log = LogUtils.getLogger();
    private static final SavedData.Factory<DatabaseArchive> DataFactory = new SavedData.Factory<>(DatabaseArchive::new, DatabaseArchive::load);

    private final Map<ResourceLocation, DatabaseRecord> Records = new LinkedHashMap<>();

    public static DatabaseArchive get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean contains(ResourceLocation Document) {
        return Records.containsKey(Document);
    }

    public Optional<DatabaseRecord> find(ResourceLocation Document) {
        return Optional.ofNullable(Records.get(Document));
    }

    public boolean add(DatabaseRecord Entry) {
        if (Records.putIfAbsent(Entry.Document(), Entry) != null) {
            return false;
        }
        setDirty();
        return true;
    }

    public List<DatabaseRecord> records() {
        return List.copyOf(Records.values());
    }

    public int size() {
        return Records.size();
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (DatabaseRecord Entry : Records.values()) {
            DatabaseRecord.RecordCodec.encodeStart(Registries.createSerializationContext(NbtOps.INSTANCE), Entry).result().ifPresent(Saved::add);
        }
        Tag.put("Records", Saved);
        return Tag;
    }

    private static DatabaseArchive load(CompoundTag Tag, HolderLookup.Provider Registries) {
        DatabaseArchive Data = new DatabaseArchive();
        ListTag Saved = Tag.getList("Records", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            DatabaseRecord.RecordCodec.parse(Registries.createSerializationContext(NbtOps.INSTANCE), Saved.getCompound(Index))
                    .resultOrPartial(Problem -> Log.error("Skipping a damaged database record: {}", Problem))
                    .ifPresent(Entry -> Data.Records.putIfAbsent(Entry.Document(), Entry));
        }
        return Data;
    }
}
