package net.rai.scp_unleashed.scp3008;

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

public class Scp3008State extends SavedData {
    public static final String DataName = "scp_unleashed_scp3008";
    private static final SavedData.Factory<Scp3008State> DataFactory = new SavedData.Factory<>(Scp3008State::new, Scp3008State::load);

    public record Spot(ResourceKey<Level> Dimension, Vec3 Pos, float Yaw) {
    }

    private long Clock = Scp3008Hours.Arrival;
    @Nullable
    private BlockPos Door;
    private ResourceKey<Level> DoorDimension = Level.OVERWORLD;
    private Direction Front = Direction.SOUTH;
    private final Map<UUID, Spot> Returns = new HashMap<>();

    public static Scp3008State get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public long clock() {
        return Clock;
    }

    public void setClock(long Value) {
        Clock = Value;
        setDirty();
    }

    public boolean hasStore() {
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

    public void recordStore(ResourceKey<Level> Dimension, BlockPos Pos, Direction Facing) {
        DoorDimension = Dimension;
        Door = Pos.immutable();
        Front = Facing;
        setDirty();
    }

    public void forgetStore() {
        Door = null;
        setDirty();
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
        Saved.putLong("Clock", Clock);
        if (Door != null) {
            Saved.put("Door", NbtUtils.writeBlockPos(Door));
            Saved.putString("DoorDimension", DoorDimension.location().toString());
            Saved.putString("Front", Front.getSerializedName());
        }
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

    public static Scp3008State load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp3008State Data = new Scp3008State();
        Data.Clock = Saved.contains("Clock") ? Saved.getLong("Clock") : Scp3008Hours.Arrival;
        NbtUtils.readBlockPos(Saved, "Door").ifPresent(Pos -> Data.Door = Pos);
        ResourceLocation Dimension = ResourceLocation.tryParse(Saved.getString("DoorDimension"));
        if (Dimension != null) {
            Data.DoorDimension = ResourceKey.create(Registries.DIMENSION, Dimension);
        }
        Direction Facing = Direction.byName(Saved.getString("Front"));
        Data.Front = Facing != null && Facing.getAxis().isHorizontal() ? Facing : Direction.SOUTH;
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
