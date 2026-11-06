package net.rai.scp_unleashed.scp860;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Random;

public class Scp860State extends SavedData {
    public static final String DataName = "scp_unleashed_scp860";
    private static final SavedData.Factory<Scp860State> DataFactory = new SavedData.Factory<>(Scp860State::new, Scp860State::load);

    private long Seed;
    private long BuiltSeed;
    private int Generation;
    private int Trips;

    public static Scp860State get(MinecraftServer Server) {
        Scp860State Found = Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
        if (Found.Seed == 0L) {
            Found.reseed();
        }
        return Found;
    }

    public long seed() {
        return Seed;
    }

    public long builtSeed() {
        return BuiltSeed;
    }

    public boolean built() {
        return BuiltSeed == Seed;
    }

    public int generation() {
        return Generation;
    }

    public int trips() {
        return Trips;
    }

    public void countTrip() {
        Trips++;
        setDirty();
    }

    public void reseed() {
        long Next = new Random().nextLong();
        Seed = Next == 0L || Next == Seed ? Next + 860L : Next;
        Generation++;
        setDirty();
    }

    public void markBuilt(long Value) {
        BuiltSeed = Value;
        setDirty();
    }

    public static Scp860State load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp860State Made = new Scp860State();
        Made.Seed = Saved.getLong("Seed");
        Made.BuiltSeed = Saved.getLong("BuiltSeed");
        Made.Generation = Saved.getInt("Generation");
        Made.Trips = Saved.getInt("Trips");
        return Made;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Saved.putLong("Seed", Seed);
        Saved.putLong("BuiltSeed", BuiltSeed);
        Saved.putInt("Generation", Generation);
        Saved.putInt("Trips", Trips);
        return Saved;
    }
}
