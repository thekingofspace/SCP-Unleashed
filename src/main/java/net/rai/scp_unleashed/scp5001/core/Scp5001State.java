package net.rai.scp_unleashed.scp5001.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class Scp5001State extends SavedData {
    public enum Phase { Dormant, Unlocked, Rising, Overdrive, Purging, Resetting, Xk1, Xk2, Ended, Summoning, Duel, Saved }

    private static final String Name = "scp_unleashed_scp5001";
    private Phase Current = Phase.Dormant;
    private long PhaseTick;
    private float Lift;
    private int Overdriven;
    private BlockPos Core = BlockPos.ZERO;
    private boolean Located;

    public static Scp5001State get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Scp5001State::new, Scp5001State::load, null), Name);
    }

    private static Scp5001State load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp5001State State = new Scp5001State();
        String Stored = Tag.getString("Phase");
        for (Phase Each : Phase.values()) {
            if (Each.name().equals(Stored)) {
                State.Current = Each;
            }
        }
        State.PhaseTick = Tag.getLong("PhaseTick");
        State.Lift = Tag.getFloat("Lift");
        State.Overdriven = Tag.getInt("Overdriven");
        State.Located = Tag.getBoolean("Located");
        State.Core = BlockPos.of(Tag.getLong("Core"));
        return State;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putString("Phase", Current.name());
        Tag.putLong("PhaseTick", PhaseTick);
        Tag.putFloat("Lift", Lift);
        Tag.putInt("Overdriven", Overdriven);
        Tag.putBoolean("Located", Located);
        Tag.putLong("Core", Core.asLong());
        return Tag;
    }

    public Phase phase() {
        return Current;
    }

    public long phaseTick() {
        return PhaseTick;
    }

    public void phase(Phase Next, long GameTime) {
        Current = Next;
        PhaseTick = GameTime;
        setDirty();
    }

    public float lift() {
        return Lift;
    }

    public void lift(float Value) {
        Lift = Math.max(0.0f, Math.min(1.0f, Value));
        setDirty();
    }

    public int overdriven() {
        return Overdriven;
    }

    public void overdriven(int Value) {
        Overdriven = Value;
        setDirty();
    }

    public boolean located() {
        return Located;
    }

    public BlockPos core() {
        return Core;
    }

    public void core(BlockPos Pos) {
        Core = Pos.immutable();
        Located = true;
        setDirty();
    }
}
