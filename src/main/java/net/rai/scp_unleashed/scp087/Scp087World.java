package net.rai.scp_unleashed.scp087;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Scp087World extends SavedData {
    public static final String DataName = "scp_unleashed_scp087";
    private static final SavedData.Factory<Scp087World> DataFactory = new SavedData.Factory<>(Scp087World::new, Scp087World::load);

    public record Spot(ResourceKey<Level> Dimension, Vec3 Pos, float Yaw) {
    }

    @Nullable
    private BlockPos Door;
    private ResourceKey<Level> DoorDimension = Level.OVERWORLD;
    private Direction Front = Direction.SOUTH;
    private boolean Inert;
    private long InertSince;
    private final Map<Integer, Integer> Built = new HashMap<>();
    private final Map<UUID, Spot> Returns = new HashMap<>();

    public static Scp087World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean hasEntrance() {
        return Door != null;
    }

    @Nullable
    public BlockPos door() {
        return Door;
    }

    public ResourceKey<Level> doorDimension() {
        return DoorDimension;
    }

    public Direction front() {
        return Front;
    }

    public void recordEntrance(ResourceKey<Level> Dimension, BlockPos Lower, Direction Facing) {
        DoorDimension = Dimension;
        Door = Lower.immutable();
        Front = Facing;
        setDirty();
    }

    public void forgetEntrance() {
        Door = null;
        setDirty();
    }

    public boolean inert() {
        return Inert;
    }

    public long inertSince() {
        return InertSince;
    }

    public void setInert(boolean Value, long GameTime) {
        Inert = Value;
        InertSince = Value ? GameTime : 0L;
        setDirty();
    }

    public int builtSignature(int Slot) {
        return Built.getOrDefault(Slot, -1);
    }

    public void markBuilt(int Slot, int Signature) {
        Built.put(Slot, Signature);
        setDirty();
    }

    public void markDirty(int Slot) {
        if (Built.remove(Slot) != null) {
            setDirty();
        }
    }

    @Nullable
    public Spot returnOf(UUID Visitor) {
        return Returns.get(Visitor);
    }

    public void setReturn(UUID Visitor, Spot Where) {
        Returns.put(Visitor, Where);
        setDirty();
    }

    public void clearReturn(UUID Visitor) {
        if (Returns.remove(Visitor) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        if (Door != null) {
            Saved.put("Door", NbtUtils.writeBlockPos(Door));
            Saved.putString("DoorDimension", DoorDimension.location().toString());
            Saved.putString("Front", Front.getSerializedName());
        }
        Saved.putBoolean("Inert", Inert);
        Saved.putLong("InertSince", InertSince);
        CompoundTag Slots = new CompoundTag();
        for (Map.Entry<Integer, Integer> Entry : Built.entrySet()) {
            Slots.putInt(Integer.toString(Entry.getKey()), Entry.getValue());
        }
        Saved.put("Built", Slots);
        ListTag Rows = new ListTag();
        for (Map.Entry<UUID, Spot> Entry : Returns.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Entry.getKey());
            Row.putString("Dimension", Entry.getValue().Dimension().location().toString());
            Row.putDouble("X", Entry.getValue().Pos().x);
            Row.putDouble("Y", Entry.getValue().Pos().y);
            Row.putDouble("Z", Entry.getValue().Pos().z);
            Row.putFloat("Yaw", Entry.getValue().Yaw());
            Rows.add(Row);
        }
        Saved.put("Returns", Rows);
        return Saved;
    }

    public static Scp087World load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp087World Data = new Scp087World();
        NbtUtils.readBlockPos(Saved, "Door").ifPresent(Pos -> Data.Door = Pos);
        ResourceLocation Dimension = ResourceLocation.tryParse(Saved.getString("DoorDimension"));
        if (Dimension != null) {
            Data.DoorDimension = ResourceKey.create(Registries.DIMENSION, Dimension);
        }
        Direction Facing = Direction.byName(Saved.getString("Front"));
        Data.Front = Facing != null && Facing.getAxis().isHorizontal() ? Facing : Direction.SOUTH;
        Data.Inert = Saved.getBoolean("Inert");
        Data.InertSince = Saved.getLong("InertSince");
        CompoundTag Slots = Saved.getCompound("Built");
        for (String Key : Slots.getAllKeys()) {
            try {
                Data.Built.put(Integer.parseInt(Key), Slots.getInt(Key));
            } catch (NumberFormatException Ignored) {
                Data.setDirty();
            }
        }
        ListTag Rows = Saved.getList("Returns", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Rows.size(); Index++) {
            CompoundTag Row = Rows.getCompound(Index);
            ResourceLocation Where = ResourceLocation.tryParse(Row.getString("Dimension"));
            if (!Row.hasUUID("Id") || Where == null) {
                continue;
            }
            Data.Returns.put(Row.getUUID("Id"), new Spot(ResourceKey.create(Registries.DIMENSION, Where), new Vec3(Row.getDouble("X"), Row.getDouble("Y"), Row.getDouble("Z")),
                    Row.getFloat("Yaw")));
        }
        return Data;
    }
}
