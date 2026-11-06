package net.rai.scp_unleashed.scp001scarlet.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

public final class ScarletState extends SavedData {
    public enum Phase { Asleep, Stirring, Xk2, Rite, Fight, Freed }

    public static final int Chains = 7;
    private static final String Name = "scp_unleashed_scarlet";
    private Phase Current = Phase.Asleep;
    private float Manifestation;
    private int Broken;
    private boolean Read;
    private long PhaseTick;
    private BlockPos Throne = BlockPos.ZERO;
    private boolean ThronePlaced;

    public static ScarletState get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ScarletState::new, ScarletState::load, null), Name);
    }

    private static ScarletState load(CompoundTag Tag, HolderLookup.Provider Registries) {
        ScarletState State = new ScarletState();
        String Stored = Tag.getString("Phase");
        for (Phase Each : Phase.values()) {
            if (Each.name().equals(Stored)) {
                State.Current = Each;
            }
        }
        State.Manifestation = Tag.getFloat("Manifestation");
        State.Broken = Tag.getInt("Broken");
        State.Read = Tag.getBoolean("Read");
        State.PhaseTick = Tag.getLong("PhaseTick");
        State.Throne = BlockPos.of(Tag.getLong("Throne"));
        State.ThronePlaced = Tag.getBoolean("ThronePlaced");
        return State;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putString("Phase", Current.name());
        Tag.putFloat("Manifestation", Manifestation);
        Tag.putInt("Broken", Broken);
        Tag.putBoolean("Read", Read);
        Tag.putLong("PhaseTick", PhaseTick);
        Tag.putLong("Throne", Throne.asLong());
        Tag.putBoolean("ThronePlaced", ThronePlaced);
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

    public float manifestation() {
        return Manifestation;
    }

    public void manifestation(float Value) {
        Manifestation = Mth.clamp(Value, 0.0f, 1.0f);
        int Due = Math.min(Chains, (int) Math.floor(Manifestation * Chains + 1.0E-4f));
        if (Due > Broken) {
            Broken = Due;
        }
        setDirty();
    }

    public int broken() {
        return Broken;
    }

    public boolean read() {
        return Read;
    }

    public void markRead() {
        if (!Read) {
            Read = true;
            setDirty();
        }
    }

    public boolean thronePlaced() {
        return ThronePlaced;
    }

    public BlockPos throne() {
        return Throne;
    }

    public void throne(BlockPos Pos) {
        Throne = Pos.immutable();
        ThronePlaced = true;
        setDirty();
    }
}
