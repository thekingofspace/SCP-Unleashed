package net.rai.scp_unleashed.scp2317.note;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class Scp2317RiteNoteState extends SavedData {
    public static final String Name = "scp_unleashed_scp2317_rite_note";
    private final LongOpenHashSet Placed = new LongOpenHashSet();

    public static Scp2317RiteNoteState get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Scp2317RiteNoteState::new, Scp2317RiteNoteState::load, null), Name);
    }

    private static Scp2317RiteNoteState load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp2317RiteNoteState State = new Scp2317RiteNoteState();
        for (long Each : Tag.getLongArray("Placed")) {
            State.Placed.add(Each);
        }
        return State;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putLongArray("Placed", Placed.toLongArray());
        return Tag;
    }

    public boolean placed(BlockPos King) {
        return Placed.contains(King.asLong());
    }

    public void mark(BlockPos King) {
        if (Placed.add(King.asLong())) {
            setDirty();
        }
    }

    public void forget(BlockPos King) {
        if (Placed.remove(King.asLong())) {
            setDirty();
        }
    }
}
