package net.rai.scp_unleashed.scp610;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Set;

public class Scp610FleshStore extends SavedData {
    public static final String DataName = "scp_unleashed_scp610_flesh";
    public static final String HivesKey = "Hives";
    public static final String SectionsKey = "Sections";
    public static final String CountsKey = "Counts";
    public static final String CellsKey = "Cells";
    private static final SavedData.Factory<Scp610FleshStore> DataFactory = new SavedData.Factory<>(Scp610FleshStore::new, Scp610FleshStore::load);
    private final Int2ObjectLinkedOpenHashMap<Scp610Owned> Hives = new Int2ObjectLinkedOpenHashMap<>();

    public static Scp610FleshStore get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean has(int Id) {
        return Hives.containsKey(Id);
    }

    public Scp610Owned owned(int Id) {
        Scp610Owned Found = Hives.get(Id);
        if (Found == null) {
            Found = new Scp610Owned();
            Hives.put(Id, Found);
            setDirty();
        }
        return Found;
    }

    public void remove(int Id) {
        if (Hives.remove(Id) != null) {
            setDirty();
        }
    }

    public void retain(Set<Integer> Live) {
        if (Hives.keySet().removeIf(Id -> !Live.contains(Id))) {
            setDirty();
        }
    }

    public Collection<Scp610Owned> all() {
        return Hives.values();
    }

    public static CompoundTag pack(Scp610Owned Owned) {
        Long2ObjectLinkedOpenHashMap<ShortArrayList> Buckets = new Long2ObjectLinkedOpenHashMap<>();
        for (int At = 0; At < Owned.size(); At++) {
            long Pos = Owned.get(At);
            int X = BlockPos.getX(Pos);
            int Y = BlockPos.getY(Pos);
            int Z = BlockPos.getZ(Pos);
            long Section = SectionPos.asLong(X >> 4, Y >> 4, Z >> 4);
            ShortArrayList Cells = Buckets.get(Section);
            if (Cells == null) {
                Cells = new ShortArrayList();
                Buckets.put(Section, Cells);
            }
            Cells.add((short) ((Y & 15) << 8 | (Z & 15) << 4 | X & 15));
        }
        long[] Sections = new long[Buckets.size()];
        int[] Counts = new int[Buckets.size()];
        byte[] Cells = new byte[Owned.size() * 2];
        int Index = 0;
        int Cursor = 0;
        for (Long2ObjectLinkedOpenHashMap.Entry<ShortArrayList> Entry : Buckets.long2ObjectEntrySet()) {
            Sections[Index] = Entry.getLongKey();
            ShortArrayList List = Entry.getValue();
            Counts[Index] = List.size();
            for (int Cell = 0; Cell < List.size(); Cell++) {
                short Value = List.getShort(Cell);
                Cells[Cursor++] = (byte) (Value >> 8);
                Cells[Cursor++] = (byte) Value;
            }
            Index++;
        }
        CompoundTag Saved = new CompoundTag();
        Saved.putLongArray(SectionsKey, Sections);
        Saved.putIntArray(CountsKey, Counts);
        Saved.putByteArray(CellsKey, Cells);
        return Saved;
    }

    public static void unpack(CompoundTag Saved, Scp610Owned Into) {
        long[] Sections = Saved.getLongArray(SectionsKey);
        int[] Counts = Saved.getIntArray(CountsKey);
        byte[] Cells = Saved.getByteArray(CellsKey);
        int Cursor = 0;
        for (int Index = 0; Index < Sections.length && Index < Counts.length; Index++) {
            int BaseX = SectionPos.x(Sections[Index]) << 4;
            int BaseY = SectionPos.y(Sections[Index]) << 4;
            int BaseZ = SectionPos.z(Sections[Index]) << 4;
            for (int Cell = 0; Cell < Counts[Index] && Cursor + 1 < Cells.length; Cell++) {
                int Value = (Cells[Cursor] & 255) << 8 | Cells[Cursor + 1] & 255;
                Cursor += 2;
                Into.add(BlockPos.asLong(BaseX + (Value & 15), BaseY + (Value >> 8 & 15), BaseZ + (Value >> 4 & 15)));
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        CompoundTag All = new CompoundTag();
        for (Int2ObjectLinkedOpenHashMap.Entry<Scp610Owned> Entry : Hives.int2ObjectEntrySet()) {
            All.put(Integer.toString(Entry.getIntKey()), pack(Entry.getValue()));
        }
        Saved.put(HivesKey, All);
        return Saved;
    }

    public static Scp610FleshStore load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp610FleshStore Made = new Scp610FleshStore();
        CompoundTag All = Saved.getCompound(HivesKey);
        for (String Key : All.getAllKeys()) {
            try {
                Scp610Owned Owned = new Scp610Owned();
                unpack(All.getCompound(Key), Owned);
                Made.Hives.put(Integer.parseInt(Key), Owned);
            } catch (NumberFormatException Ignored) {
            }
        }
        return Made;
    }
}
