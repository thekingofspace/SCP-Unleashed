package net.rai.scp_unleashed.scp163;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class Scp163Breathing extends SavedData {
    public static final String DataName = "scp_unleashed_scp163_breathing";
    public static final String PositionsKey = "Apparatus";
    private static final SavedData.Factory<Scp163Breathing> DataFactory = new SavedData.Factory<>(Scp163Breathing::new, Scp163Breathing::load);

    private final LongOpenHashSet Positions = new LongOpenHashSet();

    public static Scp163Breathing get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
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

    public static boolean isApparatus(ServerLevel World, BlockPos Pos) {
        return World.getBlockState(Pos).is(Scp163Registry.Apparatus.get());
    }

    @Nullable
    public BlockPos nearest(ServerLevel World, double X, double Y, double Z, double Range) {
        BlockPos Best = null;
        double BestDistance = Range * Range;
        for (long Packed : Positions.toLongArray()) {
            BlockPos Candidate = BlockPos.of(Packed);
            double Dx = Candidate.getX() + 0.5 - X;
            double Dy = Candidate.getY() + 0.5 - Y;
            double Dz = Candidate.getZ() + 0.5 - Z;
            double Distance = Dx * Dx + Dy * Dy + Dz * Dz;
            if (Distance > BestDistance) {
                continue;
            }
            if (World.isLoaded(Candidate) && !isApparatus(World, Candidate)) {
                remove(Candidate);
                continue;
            }
            BestDistance = Distance;
            Best = Candidate;
        }
        return Best;
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        Root.putLongArray(PositionsKey, Positions.toLongArray());
        return Root;
    }

    public static Scp163Breathing load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp163Breathing Data = new Scp163Breathing();
        for (long Packed : Root.getLongArray(PositionsKey)) {
            Data.Positions.add(Packed);
        }
        return Data;
    }
}
