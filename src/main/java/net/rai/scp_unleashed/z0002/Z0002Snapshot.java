package net.rai.scp_unleashed.z0002;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Z0002Snapshot {
    private final List<BlockState> Palette = new ArrayList<>();
    private final Map<BlockState, Integer> PaletteIds = new HashMap<>();
    private int[] Positions = new int[0];
    private int[] States = new int[0];
    private int[] Skipped = new int[0];
    private int Count;
    private int SkipCount;
    private final Map<Integer, CompoundTag> BlockEntities = new LinkedHashMap<>();
    private final List<CompoundTag> Captured = new ArrayList<>();

    private int paletteId(BlockState State) {
        Integer Id = PaletteIds.get(State);
        if (Id == null) {
            Id = Palette.size();
            Palette.add(State);
            PaletteIds.put(State, Id);
        }
        return Id;
    }

    public void record(int Index, BlockState State) {
        int Id = paletteId(State);
        if (Count == Positions.length) {
            Positions = Arrays.copyOf(Positions, Math.max(64, Count * 2));
            States = Arrays.copyOf(States, Math.max(64, Count * 2));
        }
        Positions[Count] = Index;
        States[Count] = Id;
        Count++;
    }

    public void skip(int Index) {
        if (SkipCount == Skipped.length) {
            Skipped = Arrays.copyOf(Skipped, Math.max(16, SkipCount * 2));
        }
        Skipped[SkipCount++] = Index;
    }

    public void recordBlockEntity(int Index, CompoundTag Data) {
        BlockEntities.put(Index, Data);
    }

    public void capture(CompoundTag Data) {
        Captured.add(Data);
    }

    public void seal() {
        Positions = Arrays.copyOf(Positions, Count);
        States = Arrays.copyOf(States, Count);
        Skipped = Arrays.copyOf(Skipped, SkipCount);
        boolean Sorted = true;
        for (int Index = 1; Index < Count; Index++) {
            if (Positions[Index - 1] > Positions[Index]) {
                Sorted = false;
                break;
            }
        }
        if (!Sorted) {
            Integer[] Boxed = new Integer[Count];
            for (int Index = 0; Index < Count; Index++) {
                Boxed[Index] = Index;
            }
            int[] Keys = Positions;
            Arrays.sort(Boxed, (A, B) -> Integer.compare(Keys[A], Keys[B]));
            int[] NewPositions = new int[Count];
            int[] NewStates = new int[Count];
            for (int Index = 0; Index < Count; Index++) {
                NewPositions[Index] = Positions[Boxed[Index]];
                NewStates[Index] = States[Boxed[Index]];
            }
            Positions = NewPositions;
            States = NewStates;
        }
        Arrays.sort(Skipped);
    }

    public boolean isSkipped(int Index) {
        return Arrays.binarySearch(Skipped, 0, SkipCount, Index) >= 0;
    }

    public BlockState stateAt(int Index) {
        int Found = Arrays.binarySearch(Positions, 0, Count, Index);
        return Found >= 0 ? Palette.get(States[Found]) : Blocks.AIR.defaultBlockState();
    }

    @Nullable
    public CompoundTag blockEntityAt(int Index) {
        return BlockEntities.get(Index);
    }

    public Map<Integer, CompoundTag> blockEntities() {
        return BlockEntities;
    }

    public List<CompoundTag> captured() {
        return Captured;
    }

    public int recorded() {
        return Count;
    }

    public int skipped() {
        return SkipCount;
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        ListTag PaletteRows = new ListTag();
        for (BlockState State : Palette) {
            PaletteRows.add(NbtUtils.writeBlockState(State));
        }
        Saved.put("Palette", PaletteRows);
        Saved.put("Positions", new IntArrayTag(Arrays.copyOf(Positions, Count)));
        Saved.put("States", new IntArrayTag(Arrays.copyOf(States, Count)));
        Saved.put("Skipped", new IntArrayTag(Arrays.copyOf(Skipped, SkipCount)));
        ListTag EntityRows = new ListTag();
        for (Map.Entry<Integer, CompoundTag> Entry : BlockEntities.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putInt("Index", Entry.getKey());
            Row.put("Data", Entry.getValue().copy());
            EntityRows.add(Row);
        }
        Saved.put("BlockEntities", EntityRows);
        ListTag CapturedRows = new ListTag();
        for (CompoundTag Row : Captured) {
            CapturedRows.add(Row.copy());
        }
        Saved.put("Captured", CapturedRows);
        return Saved;
    }

    public static Z0002Snapshot load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Z0002Snapshot Loaded = new Z0002Snapshot();
        HolderGetter<Block> BlockLookup = Lookup.lookupOrThrow(Registries.BLOCK);
        ListTag PaletteRows = Saved.getList("Palette", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < PaletteRows.size(); Index++) {
            BlockState State = NbtUtils.readBlockState(BlockLookup, PaletteRows.getCompound(Index));
            Loaded.Palette.add(State);
            Loaded.PaletteIds.putIfAbsent(State, Index);
        }
        int[] Positions = Saved.getIntArray("Positions");
        int[] States = Saved.getIntArray("States");
        int Usable = Math.min(Positions.length, States.length);
        Loaded.Positions = Arrays.copyOf(Positions, Usable);
        Loaded.States = Arrays.copyOf(States, Usable);
        Loaded.Count = Usable;
        for (int Index = 0; Index < Usable; Index++) {
            if (Loaded.States[Index] < 0 || Loaded.States[Index] >= Loaded.Palette.size()) {
                Loaded.States[Index] = Loaded.paletteId(Blocks.AIR.defaultBlockState());
            }
        }
        Loaded.Skipped = Saved.getIntArray("Skipped");
        Loaded.SkipCount = Loaded.Skipped.length;
        ListTag EntityRows = Saved.getList("BlockEntities", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < EntityRows.size(); Index++) {
            CompoundTag Row = EntityRows.getCompound(Index);
            Loaded.BlockEntities.put(Row.getInt("Index"), Row.getCompound("Data"));
        }
        ListTag CapturedRows = Saved.getList("Captured", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < CapturedRows.size(); Index++) {
            Loaded.Captured.add(CapturedRows.getCompound(Index));
        }
        Loaded.seal();
        return Loaded;
    }
}
