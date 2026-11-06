package net.rai.scp_unleashed.scp4845;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.space.SpaceRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Scp4845State extends SavedData {
    public static final String DataName = "scp_unleashed_scp4845";
    private static final SavedData.Factory<Scp4845State> DataFactory = new SavedData.Factory<>(Scp4845State::new, Scp4845State::load);

    @Nullable
    private BlockPos Origin;
    private ResourceKey<Level> Dimension = SpaceRegistry.MoonLevel;
    private Rotation Turn = Rotation.NONE;
    @Nullable
    private BlockPos Planned;
    private final Set<UUID> Staff = new LinkedHashSet<>();
    private boolean Staffed;
    private long Version;

    public static Scp4845State get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean built() {
        return Origin != null;
    }

    @Nullable
    public BlockPos origin() {
        return Origin;
    }

    public ResourceKey<Level> dimension() {
        return Dimension;
    }

    public Rotation turn() {
        return Turn;
    }

    public long version() {
        return Version;
    }

    @Nullable
    public BlockPos planned() {
        return Planned;
    }

    public void plan(BlockPos Target) {
        Planned = new BlockPos(Target.getX(), 0, Target.getZ());
        setDirty();
    }

    public void record(ResourceKey<Level> Where, BlockPos At, Rotation Facing) {
        Dimension = Where;
        Origin = At.immutable();
        Turn = Facing;
        Staffed = false;
        Version++;
        setDirty();
    }

    public void forget() {
        Origin = null;
        Planned = null;
        Staffed = false;
        Staff.clear();
        Version++;
        setDirty();
    }

    public boolean staffed() {
        return Staffed;
    }

    public void markStaffed() {
        if (!Staffed) {
            Staffed = true;
            setDirty();
        }
    }

    public Set<UUID> staff() {
        return Staff;
    }

    public void hire(UUID Id) {
        if (Staff.add(Id)) {
            setDirty();
        }
    }

    public void release(UUID Id) {
        if (Staff.remove(Id)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        if (Origin != null) {
            Saved.put("Origin", NbtUtils.writeBlockPos(Origin));
            Saved.putString("Dimension", Dimension.location().toString());
            Saved.putString("Rotation", Turn.getSerializedName());
        }
        if (Planned != null) {
            Saved.putInt("PlannedX", Planned.getX());
            Saved.putInt("PlannedZ", Planned.getZ());
        }
        ListTag Rows = new ListTag();
        for (UUID Id : Staff) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Id);
            Rows.add(Row);
        }
        Saved.put("Staff", Rows);
        Saved.putBoolean("Staffed", Staffed);
        return Saved;
    }

    public static Scp4845State load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp4845State Data = new Scp4845State();
        NbtUtils.readBlockPos(Saved, "Origin").ifPresent(Pos -> Data.Origin = Pos);
        ResourceLocation Where = ResourceLocation.tryParse(Saved.getString("Dimension"));
        if (Where != null) {
            Data.Dimension = ResourceKey.create(Registries.DIMENSION, Where);
        }
        for (Rotation Candidate : Rotation.values()) {
            if (Candidate.getSerializedName().equals(Saved.getString("Rotation"))) {
                Data.Turn = Candidate;
            }
        }
        if (Saved.contains("PlannedX")) {
            Data.Planned = new BlockPos(Saved.getInt("PlannedX"), 0, Saved.getInt("PlannedZ"));
        }
        Data.Staffed = Saved.getBoolean("Staffed");
        ListTag Rows = Saved.getList("Staff", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Rows.size(); Index++) {
            CompoundTag Row = Rows.getCompound(Index);
            if (Row.hasUUID("Id")) {
                Data.Staff.add(Row.getUUID("Id"));
            }
        }
        return Data;
    }
}
