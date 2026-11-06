package net.rai.scp_unleashed.scp465;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Scp465Snapshot {
    public static final int Bits = 5;
    public static final int Mask = (1 << Bits) - 1;
    public static final int QuietFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final String PaletteKey = "Palette";
    public static final String IndicesKey = "Indices";
    public static final String WideKey = "Wide";
    public static final String EntitiesKey = "Entities";
    public static final String CellKey = "Cell";
    public static final String DataKey = "Data";

    private final List<BlockState> Palette;
    private final int[] Indices;
    private final Map<Integer, CompoundTag> Entities;

    public Scp465Snapshot(List<BlockState> Palette, int[] Indices, Map<Integer, CompoundTag> Entities) {
        this.Palette = List.copyOf(Palette);
        this.Indices = Indices.clone();
        this.Entities = new TreeMap<>(Entities);
    }

    public static int pack(int X, int Y, int Z) {
        return X << (Bits * 2) | Y << Bits | Z;
    }

    public static BlockPos unpack(BlockPos Low, int Packed) {
        return Low.offset(Packed >> (Bits * 2) & Mask, Packed >> Bits & Mask, Packed & Mask);
    }

    public static int packOf(BlockPos Low, BlockPos Pos) {
        return pack(Pos.getX() - Low.getX(), Pos.getY() - Low.getY(), Pos.getZ() - Low.getZ());
    }

    public static int heightOf(int Packed) {
        return Packed >> Bits & Mask;
    }

    public List<BlockState> palette() {
        return Collections.unmodifiableList(Palette);
    }

    public int size() {
        return Indices.length;
    }

    public BlockState state(int Index) {
        return Palette.get(Indices[Index]);
    }

    public Map<Integer, CompoundTag> entities() {
        return Collections.unmodifiableMap(Entities);
    }

    public static Scp465Snapshot capture(ServerLevel World, BlockPos Low, int[] Cells) {
        Map<BlockState, Integer> Lookup = new HashMap<>();
        List<BlockState> Palette = new ArrayList<>();
        int[] Indices = new int[Cells.length];
        Map<Integer, CompoundTag> Entities = new HashMap<>();
        for (int Index = 0; Index < Cells.length; Index++) {
            BlockPos Pos = unpack(Low, Cells[Index]);
            BlockState State = World.getBlockState(Pos);
            Integer Known = Lookup.get(State);
            if (Known == null) {
                Known = Palette.size();
                Palette.add(State);
                Lookup.put(State, Known);
            }
            Indices[Index] = Known;
            BlockEntity Held = World.getBlockEntity(Pos);
            if (Held != null) {
                Entities.put(Index, Held.saveWithoutMetadata(World.registryAccess()));
            }
        }
        return new Scp465Snapshot(Palette, Indices, Entities);
    }

    public static void clear(ServerLevel World, BlockPos Low, int[] Cells) {
        for (int Packed : Cells) {
            BlockPos Pos = unpack(Low, Packed);
            BlockEntity Held = World.getBlockEntity(Pos);
            if (Held != null) {
                Clearable.tryClear(Held);
                World.removeBlockEntity(Pos);
            }
            if (!World.getBlockState(Pos).isAir()) {
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), QuietFlags);
            }
        }
    }

    public void restore(ServerLevel World, BlockPos Low, int[] Cells) {
        Integer[] Order = new Integer[Cells.length];
        for (int Index = 0; Index < Order.length; Index++) {
            Order[Index] = Index;
        }
        Arrays.sort(Order, (First, Second) -> Integer.compare(heightOf(Cells[First]), heightOf(Cells[Second])));
        for (Integer Index : Order) {
            BlockPos Pos = unpack(Low, Cells[Index]);
            BlockEntity Present = World.getBlockEntity(Pos);
            if (Present != null) {
                Clearable.tryClear(Present);
                World.removeBlockEntity(Pos);
            }
            BlockState Wanted = state(Index);
            World.setBlock(Pos, Wanted, QuietFlags);
            CompoundTag Data = Entities.get(Index);
            if (Data != null) {
                BlockEntity Fresh = World.getBlockEntity(Pos);
                if (Fresh != null) {
                    Fresh.loadWithComponents(Data.copy(), World.registryAccess());
                    Fresh.setChanged();
                    World.sendBlockUpdated(Pos, Wanted, Wanted, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        ListTag States = new ListTag();
        for (BlockState State : Palette) {
            States.add(NbtUtils.writeBlockState(State));
        }
        Saved.put(PaletteKey, States);
        if (Palette.size() <= 256) {
            byte[] Narrow = new byte[Indices.length];
            for (int Index = 0; Index < Indices.length; Index++) {
                Narrow[Index] = (byte) Indices[Index];
            }
            Saved.put(IndicesKey, new ByteArrayTag(Narrow));
        } else {
            Saved.put(IndicesKey, new IntArrayTag(Indices.clone()));
            Saved.putBoolean(WideKey, true);
        }
        ListTag Held = new ListTag();
        for (Map.Entry<Integer, CompoundTag> Entry : Entities.entrySet()) {
            CompoundTag Record = new CompoundTag();
            Record.putInt(CellKey, Entry.getKey());
            Record.put(DataKey, Entry.getValue().copy());
            Held.add(Record);
        }
        Saved.put(EntitiesKey, Held);
        return Saved;
    }

    public static Scp465Snapshot load(CompoundTag Saved, HolderGetter<Block> BlockLookup) {
        ListTag States = Saved.getList(PaletteKey, Tag.TAG_COMPOUND);
        List<BlockState> Palette = new ArrayList<>();
        for (int Index = 0; Index < States.size(); Index++) {
            Palette.add(NbtUtils.readBlockState(BlockLookup, States.getCompound(Index)));
        }
        int[] Indices;
        if (Saved.getBoolean(WideKey)) {
            Indices = Saved.getIntArray(IndicesKey);
        } else {
            byte[] Narrow = Saved.getByteArray(IndicesKey);
            Indices = new int[Narrow.length];
            for (int Index = 0; Index < Narrow.length; Index++) {
                Indices[Index] = Narrow[Index] & 255;
            }
        }
        for (int Index = 0; Index < Indices.length; Index++) {
            if (Indices[Index] < 0 || Indices[Index] >= Palette.size()) {
                Indices[Index] = 0;
                if (Palette.isEmpty()) {
                    Palette.add(Blocks.AIR.defaultBlockState());
                }
            }
        }
        Map<Integer, CompoundTag> Entities = new HashMap<>();
        ListTag Held = Saved.getList(EntitiesKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Held.size(); Index++) {
            CompoundTag Record = Held.getCompound(Index);
            Entities.put(Record.getInt(CellKey), Record.getCompound(DataKey));
        }
        return new Scp465Snapshot(Palette, Indices, Entities);
    }

    public boolean sameAs(Scp465Snapshot Other) {
        if (Other.Indices.length != Indices.length) {
            return false;
        }
        for (int Index = 0; Index < Indices.length; Index++) {
            if (!state(Index).equals(Other.state(Index))) {
                return false;
            }
        }
        return Entities.equals(Other.Entities);
    }
}
