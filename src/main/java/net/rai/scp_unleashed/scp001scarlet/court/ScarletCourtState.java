package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

public final class ScarletCourtState extends SavedData {
    public static final int Daughters = 4;
    private static final String Name = "scp_unleashed_scarlet_court";
    private int Slain;
    private boolean Active;
    private BlockPos Dais = BlockPos.ZERO;
    private int Players = 1;

    public static ScarletCourtState get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ScarletCourtState::new, ScarletCourtState::load, null), Name);
    }

    private static ScarletCourtState load(CompoundTag Tag, HolderLookup.Provider Registries) {
        ScarletCourtState State = new ScarletCourtState();
        State.Slain = Mth.clamp(Tag.getInt("Slain"), 0, Daughters);
        State.Active = Tag.getBoolean("Active");
        State.Dais = BlockPos.of(Tag.getLong("Dais"));
        State.Players = Math.max(1, Tag.getInt("Players"));
        return State;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putInt("Slain", Slain);
        Tag.putBoolean("Active", Active);
        Tag.putLong("Dais", Dais.asLong());
        Tag.putInt("Players", Players);
        return Tag;
    }

    public int slain() {
        return Slain;
    }

    public void slain(int Value) {
        Slain = Mth.clamp(Value, 0, Daughters);
        setDirty();
    }

    public boolean active() {
        return Active;
    }

    public void active(boolean Value) {
        Active = Value;
        setDirty();
    }

    public BlockPos dais() {
        return Dais;
    }

    public void dais(BlockPos Pos) {
        Dais = Pos.immutable();
        setDirty();
    }

    public int players() {
        return Players;
    }

    public void players(int Value) {
        Players = Math.max(1, Value);
        setDirty();
    }
}
