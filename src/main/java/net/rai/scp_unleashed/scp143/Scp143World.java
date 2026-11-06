package net.rai.scp_unleashed.scp143;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class Scp143World extends SavedData {
    public static final String DataName = "scp_unleashed_scp143";
    private static final SavedData.Factory<Scp143World> DataFactory = new SavedData.Factory<>(Scp143World::new, Scp143World::load);

    @Nullable
    private BlockPos Planned;
    @Nullable
    private BlockPos Base;
    private int Attempts;

    public static Scp143World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean hasSite() {
        return Planned != null || Base != null;
    }

    public boolean isBuilt() {
        return Base != null;
    }

    @Nullable
    public BlockPos planned() {
        return Planned;
    }

    @Nullable
    public BlockPos base() {
        return Base;
    }

    @Nullable
    public BlockPos site() {
        return Base != null ? Base : Planned;
    }

    public int attempts() {
        return Attempts;
    }

    public void plan(BlockPos Spot) {
        Planned = Spot.immutable();
        Base = null;
        Attempts = 0;
        setDirty();
    }

    public void built(BlockPos Spot) {
        Base = Spot.immutable();
        if (Planned == null) {
            Planned = Base;
        }
        setDirty();
    }

    public void failed() {
        Attempts++;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (Planned != null) {
            Tag.putLong("Planned", Planned.asLong());
        }
        if (Base != null) {
            Tag.putLong("Base", Base.asLong());
        }
        Tag.putInt("Attempts", Attempts);
        return Tag;
    }

    public static Scp143World load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp143World Data = new Scp143World();
        if (Tag.contains("Planned")) {
            Data.Planned = BlockPos.of(Tag.getLong("Planned"));
        }
        if (Tag.contains("Base")) {
            Data.Base = BlockPos.of(Tag.getLong("Base"));
        }
        Data.Attempts = Tag.getInt("Attempts");
        return Data;
    }
}
