package net.rai.scp_unleashed.scp098;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class Scp098Hives extends SavedData {
    public static final String DataName = "scp_unleashed_scp098_hives";
    public static final String PositionsKey = "Hives";
    private static final SavedData.Factory<Scp098Hives> DataFactory = new SavedData.Factory<>(Scp098Hives::new, Scp098Hives::load);

    private final LongOpenHashSet Positions = new LongOpenHashSet();

    public static Scp098Hives get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean contains(BlockPos Pos) {
        return Positions.contains(Pos.asLong());
    }

    public void add(BlockPos Pos) {
        if (Positions.add(Pos.asLong())) {
            setDirty();
        }
    }

    public void remove(BlockPos Pos) {
        if (Positions.remove(Pos.asLong())) {
            setDirty();
        }
    }

    public int count() {
        return Positions.size();
    }

    public int countNear(BlockPos Pos, double Radius) {
        int Found = 0;
        double Reach = Radius * Radius;
        for (long Packed : Positions) {
            if (BlockPos.of(Packed).distSqr(Pos) <= Reach) {
                Found++;
            }
        }
        return Found;
    }

    @Nullable
    public BlockPos nearest(BlockPos Pos, double Radius) {
        BlockPos Best = null;
        double BestDistance = Radius * Radius;
        for (long Packed : Positions) {
            BlockPos Candidate = BlockPos.of(Packed);
            double Distance = Candidate.distSqr(Pos);
            if (Distance <= BestDistance) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        Root.putLongArray(PositionsKey, Positions.toLongArray());
        return Root;
    }

    public static Scp098Hives load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp098Hives Data = new Scp098Hives();
        for (long Packed : Root.getLongArray(PositionsKey)) {
            Data.Positions.add(Packed);
        }
        return Data;
    }
}
