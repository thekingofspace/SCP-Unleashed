package net.rai.scp_unleashed.scp075;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Scp075World extends SavedData {
    public static final String DataName = "scp_unleashed_scp075";
    public static final String ArrivedKey = "Arrived";
    public static final String SpotKey = "Spot";
    public static final String LegacyKey = "Aware";
    private static final SavedData.Factory<Scp075World> DataFactory = new SavedData.Factory<>(Scp075World::new, Scp075World::load);

    private boolean Arrived;
    @Nullable
    private BlockPos Spot;

    public static Scp075World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean hasArrived() {
        return Arrived;
    }

    public Optional<BlockPos> spot() {
        return Optional.ofNullable(Spot);
    }

    public void markArrived(BlockPos Where) {
        Arrived = true;
        Spot = Where.immutable();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putBoolean(ArrivedKey, Arrived);
        if (Spot != null) {
            Tag.put(SpotKey, NbtUtils.writeBlockPos(Spot));
        }
        return Tag;
    }

    public static Scp075World load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp075World Data = new Scp075World();
        Data.Arrived = Tag.getBoolean(ArrivedKey) || !Tag.getList(LegacyKey, net.minecraft.nbt.Tag.TAG_INT_ARRAY).isEmpty();
        Data.Spot = NbtUtils.readBlockPos(Tag, SpotKey).orElse(null);
        return Data;
    }
}
