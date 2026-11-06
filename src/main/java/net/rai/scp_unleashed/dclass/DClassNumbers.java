package net.rai.scp_unleashed.dclass;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

public class DClassNumbers extends SavedData {
    public static final String DataName = "scp_unleashed_dclass_numbers";
    public static final int FirstLow = 1000;
    public static final int FirstSpread = 7000;
    public static final int MaxStep = 6;
    private static final SavedData.Factory<DClassNumbers> DataFactory = new SavedData.Factory<>(DClassNumbers::new, DClassNumbers::load);

    private int Last;

    public static DClassNumbers get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public int next(RandomSource Random) {
        if (Last <= 0) {
            Last = FirstLow + Random.nextInt(FirstSpread);
        } else {
            Last += 1 + Random.nextInt(MaxStep);
        }
        setDirty();
        return Last;
    }

    public int last() {
        return Last;
    }

    public static DClassNumbers load(CompoundTag Saved, HolderLookup.Provider Registries) {
        DClassNumbers Numbers = new DClassNumbers();
        Numbers.Last = Saved.getInt("Last");
        return Numbers;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Registries) {
        Saved.putInt("Last", Last);
        return Saved;
    }

    public static String format(int Number) {
        return Number <= 0 ? "D-????" : String.format("D-%04d", Number);
    }
}
