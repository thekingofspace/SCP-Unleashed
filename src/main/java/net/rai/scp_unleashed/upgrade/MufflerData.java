package net.rai.scp_unleashed.upgrade;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MufflerData extends SavedData {
    public static final String Name = "scp_unleashed_mufflers";
    public static final Factory<MufflerData> DataFactory = new Factory<>(MufflerData::new, MufflerData::load, null);

    public record Entry(long Core, ResourceLocation BlockId, long[] Cells) {
    }

    private final Long2ObjectOpenHashMap<Entry> Machines = new Long2ObjectOpenHashMap<>();
    private final LongSet Cells = new LongOpenHashSet();

    public static MufflerData get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, Name);
    }

    @Nullable
    public static MufflerData peek(ServerLevel World) {
        return World.getDataStorage().get(DataFactory, Name);
    }

    public static MufflerData load(CompoundTag Compound, HolderLookup.Provider Registries) {
        MufflerData Data = new MufflerData();
        ListTag Saved = Compound.getList("Machines", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Machine = Saved.getCompound(Index);
            ResourceLocation Kind = ResourceLocation.tryParse(Machine.getString("Block"));
            if (Kind != null) {
                Data.put(new Entry(Machine.getLong("Core"), Kind, Machine.getLongArray("Cells")));
            }
        }
        return Data;
    }

    @Override
    public CompoundTag save(CompoundTag Compound, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (Entry Machine : Machines.values()) {
            CompoundTag Written = new CompoundTag();
            Written.putLong("Core", Machine.Core());
            Written.putString("Block", Machine.BlockId().toString());
            Written.put("Cells", new LongArrayTag(Machine.Cells()));
            Saved.add(Written);
        }
        Compound.put("Machines", Saved);
        return Compound;
    }

    private void put(Entry Machine) {
        Machines.put(Machine.Core(), Machine);
        for (long Cell : Machine.Cells()) {
            Cells.add(Cell);
        }
    }

    public boolean isEmpty() {
        return Machines.isEmpty();
    }

    public LongSet cells() {
        return Cells;
    }

    public boolean has(BlockPos Core) {
        return Machines.containsKey(Core.asLong());
    }

    @Nullable
    public Entry entry(long Core) {
        return Machines.get(Core);
    }

    public Collection<Entry> entries() {
        return List.copyOf(Machines.values());
    }

    public Entry add(BlockPos Core, ResourceLocation Kind, Collection<BlockPos> Footprint) {
        long[] Packed = new long[Footprint.size()];
        int Index = 0;
        for (BlockPos Cell : Footprint) {
            Packed[Index++] = Cell.asLong();
        }
        Entry Machine = new Entry(Core.asLong(), Kind, Packed);
        Machines.remove(Machine.Core());
        put(Machine);
        rebuild();
        setDirty();
        return Machine;
    }

    @Nullable
    public Entry remove(long Core) {
        Entry Gone = Machines.remove(Core);
        if (Gone != null) {
            rebuild();
            setDirty();
        }
        return Gone;
    }

    private void rebuild() {
        Cells.clear();
        List<Entry> Kept = new ArrayList<>(Machines.values());
        for (Entry Machine : Kept) {
            for (long Cell : Machine.Cells()) {
                Cells.add(Cell);
            }
        }
    }
}
