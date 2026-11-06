package net.rai.scp_unleashed.scp076;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public class Scp076Vaults extends SavedData {
    public static final String DataName = "scp_unleashed_scp076";
    private static final SavedData.Factory<Scp076Vaults> DataFactory = new SavedData.Factory<>(Scp076Vaults::new, Scp076Vaults::load);

    public record Vault(ResourceKey<Level> Dimension, BlockPos Door, Direction Front, BlockPos Centre) {
    }

    private final Map<String, Vault> Vaults = new LinkedHashMap<>();
    private final Map<String, Long> Pending = new HashMap<>();

    public static Scp076Vaults get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    private static String key(ResourceKey<Level> Dimension, BlockPos Door) {
        return Dimension.location() + "|" + Door.asLong();
    }

    public Collection<Vault> vaults() {
        return Vaults.values();
    }

    public void record(ResourceKey<Level> Dimension, BlockPos Door, Direction Front, BlockPos Centre) {
        Vaults.put(key(Dimension, Door), new Vault(Dimension, Door.immutable(), Front, Centre.immutable()));
        setDirty();
    }

    public void forget(ResourceKey<Level> Dimension, BlockPos Door) {
        String Key = key(Dimension, Door);
        if (Vaults.remove(Key) != null | Pending.remove(Key) != null) {
            setDirty();
        }
    }

    public Optional<Vault> find(ResourceKey<Level> Dimension, BlockPos Door) {
        return Optional.ofNullable(Vaults.get(key(Dimension, Door)));
    }

    public Optional<Vault> nearest(ResourceKey<Level> Dimension, BlockPos Pos, double Range) {
        Vault Best = null;
        double BestDistance = Range * Range;
        for (Vault Entry : Vaults.values()) {
            if (!Entry.Dimension().equals(Dimension)) {
                continue;
            }
            double Distance = Entry.Centre().distSqr(Pos);
            if (Distance <= BestDistance) {
                BestDistance = Distance;
                Best = Entry;
            }
        }
        return Optional.ofNullable(Best);
    }

    public Optional<Vault> first(ResourceKey<Level> Dimension) {
        return Vaults.values().stream().filter(Entry -> Entry.Dimension().equals(Dimension)).findFirst();
    }

    public void requestReseal(ResourceKey<Level> Dimension, BlockPos Door, long Ticks) {
        Pending.merge(key(Dimension, Door), Ticks, Math::max);
        setDirty();
    }

    public OptionalLong takeReseal(ResourceKey<Level> Dimension, BlockPos Door) {
        Long Found = Pending.remove(key(Dimension, Door));
        if (Found == null) {
            return OptionalLong.empty();
        }
        setDirty();
        return OptionalLong.of(Found);
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag Entries = new ListTag();
        for (Vault Entry : Vaults.values()) {
            CompoundTag Row = new CompoundTag();
            Row.putString("Dimension", Entry.Dimension().location().toString());
            Row.putLong("Door", Entry.Door().asLong());
            Row.putString("Front", Entry.Front().getSerializedName());
            Row.putLong("Centre", Entry.Centre().asLong());
            Entries.add(Row);
        }
        Saved.put("Vaults", Entries);
        CompoundTag Waiting = new CompoundTag();
        for (Map.Entry<String, Long> Entry : Pending.entrySet()) {
            Waiting.putLong(Entry.getKey(), Entry.getValue());
        }
        Saved.put("Pending", Waiting);
        return Saved;
    }

    public static Scp076Vaults load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp076Vaults Data = new Scp076Vaults();
        ListTag Entries = Saved.getList("Vaults", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size(); Index++) {
            CompoundTag Row = Entries.getCompound(Index);
            ResourceLocation Id = ResourceLocation.tryParse(Row.getString("Dimension"));
            Direction Front = Direction.byName(Row.getString("Front"));
            if (Id == null || Front == null || !Front.getAxis().isHorizontal()) {
                continue;
            }
            ResourceKey<Level> Dimension = ResourceKey.create(Registries.DIMENSION, Id);
            BlockPos Door = BlockPos.of(Row.getLong("Door"));
            Data.Vaults.put(key(Dimension, Door), new Vault(Dimension, Door, Front, BlockPos.of(Row.getLong("Centre"))));
        }
        CompoundTag Waiting = Saved.getCompound("Pending");
        for (String Key : Waiting.getAllKeys()) {
            Data.Pending.put(Key, Waiting.getLong(Key));
        }
        return Data;
    }
}
