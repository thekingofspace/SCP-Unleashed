package net.rai.scp_unleashed.scp343;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp343Data extends SavedData {
    public static final String DataName = "scp_unleashed_scp343";
    private static final SavedData.Factory<Scp343Data> DataFactory = new SavedData.Factory<>(Scp343Data::new, Scp343Data::load);

    public record Away(UUID Id, CompoundTag Body, ResourceKey<Level> From, BlockPos FromPos, long ReturnAt) {
        public Away delayed(long Next) {
            return new Away(Id, Body, From, FromPos, Next);
        }
    }

    private final Map<UUID, Away> Absent = new LinkedHashMap<>();
    private final Map<ResourceKey<Level>, LongLinkedOpenHashSet> Thrones = new HashMap<>();
    private final Map<UUID, Long> Blessed = new HashMap<>();

    public static Scp343Data get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public List<Away> away() {
        return new ArrayList<>(Absent.values());
    }

    @Nullable
    public Away away(UUID Id) {
        return Absent.get(Id);
    }

    public void putAway(Away Record) {
        Absent.put(Record.Id(), Record);
        setDirty();
    }

    public void returned(UUID Id) {
        if (Absent.remove(Id) != null) {
            setDirty();
        }
    }

    public void addThrone(ResourceKey<Level> Dimension, BlockPos Pos) {
        if (Thrones.computeIfAbsent(Dimension, Key -> new LongLinkedOpenHashSet()).add(Pos.asLong())) {
            setDirty();
        }
    }

    public void removeThrone(ResourceKey<Level> Dimension, BlockPos Pos) {
        LongLinkedOpenHashSet Found = Thrones.get(Dimension);
        if (Found != null && Found.remove(Pos.asLong())) {
            setDirty();
        }
    }

    public List<BlockPos> thrones(ResourceKey<Level> Dimension) {
        List<BlockPos> Found = new ArrayList<>();
        LongLinkedOpenHashSet Stored = Thrones.get(Dimension);
        if (Stored != null) {
            Stored.forEach(Packed -> Found.add(BlockPos.of(Packed)));
        }
        return Found;
    }

    public int throneCount() {
        int Total = 0;
        for (LongLinkedOpenHashSet Stored : Thrones.values()) {
            Total += Stored.size();
        }
        return Total;
    }

    public long blessedUntil(UUID Player) {
        return Blessed.getOrDefault(Player, 0L);
    }

    public void bless(UUID Player, long Until) {
        Blessed.put(Player, Until);
        setDirty();
    }

    public void forgetBlessing(UUID Player) {
        if (Blessed.remove(Player) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag AwayList = new ListTag();
        for (Away Record : Absent.values()) {
            CompoundTag Entry = new CompoundTag();
            Entry.putUUID("Id", Record.Id());
            Entry.put("Body", Record.Body().copy());
            Entry.putString("From", Record.From().location().toString());
            Entry.putLong("FromPos", Record.FromPos().asLong());
            Entry.putLong("ReturnAt", Record.ReturnAt());
            AwayList.add(Entry);
        }
        Saved.put("Away", AwayList);
        ListTag ThroneList = new ListTag();
        for (Map.Entry<ResourceKey<Level>, LongLinkedOpenHashSet> Dimension : Thrones.entrySet()) {
            CompoundTag Entry = new CompoundTag();
            Entry.putString("Dimension", Dimension.getKey().location().toString());
            Entry.putLongArray("Positions", Dimension.getValue().toLongArray());
            ThroneList.add(Entry);
        }
        Saved.put("Thrones", ThroneList);
        ListTag BlessedList = new ListTag();
        for (Map.Entry<UUID, Long> Entry : Blessed.entrySet()) {
            CompoundTag Item = new CompoundTag();
            Item.putUUID("Player", Entry.getKey());
            Item.putLong("Until", Entry.getValue());
            BlessedList.add(Item);
        }
        Saved.put("Blessed", BlessedList);
        return Saved;
    }

    public static Scp343Data load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp343Data Data = new Scp343Data();
        for (Tag Raw : Saved.getList("Away", Tag.TAG_COMPOUND)) {
            CompoundTag Entry = (CompoundTag) Raw;
            ResourceLocation From = ResourceLocation.tryParse(Entry.getString("From"));
            if (!Entry.hasUUID("Id") || From == null) {
                continue;
            }
            Away Record = new Away(Entry.getUUID("Id"), Entry.getCompound("Body"), ResourceKey.create(Registries.DIMENSION, From), BlockPos.of(Entry.getLong("FromPos")),
                    Entry.getLong("ReturnAt"));
            Data.Absent.put(Record.Id(), Record);
        }
        for (Tag Raw : Saved.getList("Thrones", Tag.TAG_COMPOUND)) {
            CompoundTag Entry = (CompoundTag) Raw;
            ResourceLocation Dimension = ResourceLocation.tryParse(Entry.getString("Dimension"));
            if (Dimension == null) {
                continue;
            }
            LongLinkedOpenHashSet Positions = new LongLinkedOpenHashSet();
            for (long Packed : Entry.getLongArray("Positions")) {
                Positions.add(Packed);
            }
            Data.Thrones.put(ResourceKey.create(Registries.DIMENSION, Dimension), Positions);
        }
        for (Tag Raw : Saved.getList("Blessed", Tag.TAG_COMPOUND)) {
            CompoundTag Entry = (CompoundTag) Raw;
            if (Entry.hasUUID("Player")) {
                Data.Blessed.put(Entry.getUUID("Player"), Entry.getLong("Until"));
            }
        }
        return Data;
    }
}
