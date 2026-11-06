package net.rai.scp_unleashed.scp001scarlet.king;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ScarletKingData extends SavedData {
    private static final String Name = "scp_unleashed_scarlet_king";
    private boolean Consumed;
    private long RiteStart = -1L;
    private BlockPos RiteThrone = BlockPos.ZERO;
    @Nullable
    private UUID Blob;
    @Nullable
    private UUID King;
    private int Generation;
    private long XkTick;
    private long EmptySince = -1L;
    private long MissingSince = -1L;

    public static ScarletKingData get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ScarletKingData::new, ScarletKingData::load, null), Name);
    }

    private static ScarletKingData load(CompoundTag Tag, HolderLookup.Provider Registries) {
        ScarletKingData Data = new ScarletKingData();
        Data.Consumed = Tag.getBoolean("Consumed");
        Data.RiteStart = Tag.contains("RiteStart") ? Tag.getLong("RiteStart") : -1L;
        Data.RiteThrone = BlockPos.of(Tag.getLong("RiteThrone"));
        Data.Blob = Tag.hasUUID("Blob") ? Tag.getUUID("Blob") : null;
        Data.King = Tag.hasUUID("King") ? Tag.getUUID("King") : null;
        Data.Generation = Tag.getInt("Generation");
        Data.XkTick = Tag.getLong("XkTick");
        return Data;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putBoolean("Consumed", Consumed);
        Tag.putLong("RiteStart", RiteStart);
        Tag.putLong("RiteThrone", RiteThrone.asLong());
        if (Blob != null) {
            Tag.putUUID("Blob", Blob);
        }
        if (King != null) {
            Tag.putUUID("King", King);
        }
        Tag.putInt("Generation", Generation);
        Tag.putLong("XkTick", XkTick);
        return Tag;
    }

    public boolean consumed() {
        return Consumed;
    }

    public void consumed(boolean Value) {
        Consumed = Value;
        setDirty();
    }

    public long riteStart() {
        return RiteStart;
    }

    public BlockPos riteThrone() {
        return RiteThrone;
    }

    public void startRite(long Now, BlockPos Throne, @Nullable UUID Offering, long PreviousTick) {
        RiteStart = Now;
        RiteThrone = Throne.immutable();
        Blob = Offering;
        XkTick = PreviousTick;
        EmptySince = -1L;
        MissingSince = -1L;
        setDirty();
    }

    public void endRite() {
        RiteStart = -1L;
        setDirty();
    }

    @Nullable
    public UUID blob() {
        return Blob;
    }

    @Nullable
    public UUID king() {
        return King;
    }

    public void king(@Nullable UUID Value) {
        King = Value;
        setDirty();
    }

    public int generation() {
        return Generation;
    }

    public void nextGeneration() {
        Generation++;
        setDirty();
    }

    public long xkTick() {
        return XkTick;
    }

    public long emptySince() {
        return EmptySince;
    }

    public void emptySince(long Value) {
        EmptySince = Value;
    }

    public long missingSince() {
        return MissingSince;
    }

    public void missingSince(long Value) {
        MissingSince = Value;
    }
}
