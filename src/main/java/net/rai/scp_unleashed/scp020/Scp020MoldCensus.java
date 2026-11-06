package net.rai.scp_unleashed.scp020;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class Scp020MoldCensus extends SavedData {
    public static final String DataName = "scp_unleashed_scp020_mold";
    private static final SavedData.Factory<Scp020MoldCensus> DataFactory = new SavedData.Factory<>(Scp020MoldCensus::new, Scp020MoldCensus::load);

    private int Faces;

    public static Scp020MoldCensus get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public int faces() {
        return Faces;
    }

    public void change(int Delta) {
        if (Delta == 0) {
            return;
        }
        Faces = Math.max(0, Faces + Delta);
        setDirty();
    }

    public void reset(int Counted) {
        Faces = Math.max(0, Counted);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        Root.putInt("Faces", Faces);
        return Root;
    }

    public static Scp020MoldCensus load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp020MoldCensus Data = new Scp020MoldCensus();
        Data.Faces = Math.max(0, Root.getInt("Faces"));
        return Data;
    }
}
