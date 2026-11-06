package net.rai.scp_unleashed.daeviteruins.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DaeviteRuinsWorld extends SavedData {
    public static final String DataName = "scp_unleashed_daevite_ruins";
    public static final int SameChronicle = 64;
    private static final SavedData.Factory<DaeviteRuinsWorld> DataFactory = new SavedData.Factory<>(DaeviteRuinsWorld::new, DaeviteRuinsWorld::load);

    private final List<DaeviteRuin> Ruins = new ArrayList<>();
    private int NextId = 1;

    public static DaeviteRuinsWorld get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public List<DaeviteRuin> ruins() {
        return Ruins;
    }

    public DaeviteRuin add(DaeviteRuin Ruin) {
        Ruin.Id = NextId++;
        Ruins.add(Ruin);
        setDirty();
        return Ruin;
    }

    public void remove(DaeviteRuin Ruin) {
        if (Ruins.remove(Ruin)) {
            setDirty();
        }
    }

    @Nullable
    public DaeviteRuin sameChronicle(int Page, BlockPos Origin) {
        for (DaeviteRuin Ruin : Ruins) {
            if (Ruin.Page == Page && !Ruin.Command && Ruin.Origin.distSqr(Origin) <= (double) SameChronicle * SameChronicle) {
                return Ruin;
            }
        }
        return null;
    }

    @Nullable
    public DaeviteRuin latest(int Page) {
        DaeviteRuin Best = null;
        for (DaeviteRuin Ruin : Ruins) {
            if (Ruin.Page == Page && (Best == null || Ruin.Id > Best.Id)) {
                Best = Ruin;
            }
        }
        return Best;
    }

    @Nullable
    public DaeviteRuin byId(int Id) {
        for (DaeviteRuin Ruin : Ruins) {
            if (Ruin.Id == Id) {
                return Ruin;
            }
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag Rows = new ListTag();
        for (DaeviteRuin Ruin : Ruins) {
            Rows.add(Ruin.save());
        }
        Saved.put("Ruins", Rows);
        Saved.putInt("NextId", NextId);
        return Saved;
    }

    public static DaeviteRuinsWorld load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        DaeviteRuinsWorld Data = new DaeviteRuinsWorld();
        ListTag Rows = Saved.getList("Ruins", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Rows.size(); Index++) {
            Data.Ruins.add(DaeviteRuin.load(Rows.getCompound(Index)));
        }
        Data.NextId = Math.max(1, Saved.getInt("NextId"));
        for (DaeviteRuin Ruin : Data.Ruins) {
            Data.NextId = Math.max(Data.NextId, Ruin.Id + 1);
        }
        return Data;
    }
}
