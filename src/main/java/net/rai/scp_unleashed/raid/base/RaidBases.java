package net.rai.scp_unleashed.raid.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RaidBases extends SavedData {
    public static final String DataName = "scp_unleashed_raid_bases";
    public static final String Shared = "world";
    public static final String TeamPrefix = "team:";
    private static final SavedData.Factory<RaidBases> DataFactory = new SavedData.Factory<>(RaidBases::new, RaidBases::load);

    private final Map<String, Map<ResourceKey<Level>, BaseHeatmap>> Groups = new LinkedHashMap<>();

    public static RaidBases get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static String groupOf(Player User) {
        PlayerTeam Team = User.getTeam();
        return Team == null ? Shared : TeamPrefix + Team.getName();
    }

    public BaseHeatmap map(String Group, ResourceKey<Level> Dimension) {
        return Groups.computeIfAbsent(Group, Key -> new LinkedHashMap<>()).computeIfAbsent(Dimension, Key -> new BaseHeatmap());
    }

    @Nullable
    public BaseHeatmap find(String Group, ResourceKey<Level> Dimension) {
        Map<ResourceKey<Level>, BaseHeatmap> Found = Groups.get(Group);
        return Found == null ? null : Found.get(Dimension);
    }

    @Nullable
    public BaseEstimate estimate(String Group, ResourceKey<Level> Dimension, long Now) {
        BaseHeatmap Found = find(Group, Dimension);
        return Found == null ? null : Found.estimate(Now);
    }

    public void signal(String Group, ResourceKey<Level> Dimension, BlockPos Pos, double Amount, long Now, int Marks) {
        map(Group, Dimension).add(Pos.getX(), Pos.getY(), Pos.getZ(), Amount, Now, Marks);
        setDirty();
    }

    public void noteStash(String Group, ResourceKey<Level> Dimension, BlockPos Pos, long Now) {
        map(Group, Dimension).noteStash(Pos.asLong(), Now);
        setDirty();
    }

    public void forgetStash(String Group, ResourceKey<Level> Dimension, BlockPos Pos) {
        BaseHeatmap Found = find(Group, Dimension);
        if (Found != null) {
            Found.forgetStash(Pos.asLong());
            setDirty();
        }
    }

    public void prune(long Now) {
        for (Map<ResourceKey<Level>, BaseHeatmap> Maps : Groups.values()) {
            for (BaseHeatmap Found : Maps.values()) {
                Found.prune(Now);
            }
            Maps.values().removeIf(Found -> Found.size() == 0 && Found.stashes().isEmpty());
        }
        Groups.values().removeIf(Map::isEmpty);
        setDirty();
    }

    public void clear(String Group) {
        Groups.remove(Group);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag GroupList = new ListTag();
        for (Map.Entry<String, Map<ResourceKey<Level>, BaseHeatmap>> Group : Groups.entrySet()) {
            for (Map.Entry<ResourceKey<Level>, BaseHeatmap> Entry : Group.getValue().entrySet()) {
                BaseHeatmap Found = Entry.getValue();
                CompoundTag One = new CompoundTag();
                One.putString("Group", Group.getKey());
                One.putString("Dimension", Entry.getKey().location().toString());
                int Size = Found.size();
                long[] Keys = new long[Size];
                long[] Stamps = new long[Size];
                int[] Flags = new int[Size];
                long[] Sums = new long[Size * 6];
                int Index = 0;
                for (BaseHeatmap.Cell Cell : Found.cells()) {
                    Keys[Index] = Cell.key();
                    Stamps[Index] = Cell.stamp();
                    Flags[Index] = Cell.flags();
                    double[] Values = Cell.sums();
                    for (int Part = 0; Part < 6; Part++) {
                        Sums[Index * 6 + Part] = Double.doubleToLongBits(Values[Part]);
                    }
                    Index++;
                }
                One.putLongArray("Keys", Keys);
                One.putLongArray("Stamps", Stamps);
                One.putIntArray("Flags", Flags);
                One.putLongArray("Sums", Sums);
                long[] Stashes = new long[Found.stashes().size() * 2];
                int Slot = 0;
                for (Map.Entry<Long, Long> Stash : Found.stashes().entrySet()) {
                    Stashes[Slot++] = Stash.getKey();
                    Stashes[Slot++] = Stash.getValue();
                }
                One.putLongArray("Stashes", Stashes);
                GroupList.add(One);
            }
        }
        Saved.put("Maps", GroupList);
        return Saved;
    }

    public static RaidBases load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        RaidBases Data = new RaidBases();
        ListTag GroupList = Saved.getList("Maps", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < GroupList.size(); Index++) {
            CompoundTag One = GroupList.getCompound(Index);
            ResourceLocation Place = ResourceLocation.tryParse(One.getString("Dimension"));
            if (Place == null) {
                continue;
            }
            BaseHeatmap Found = Data.map(One.getString("Group"), ResourceKey.create(Registries.DIMENSION, Place));
            long[] Keys = One.getLongArray("Keys");
            long[] Stamps = One.getLongArray("Stamps");
            int[] Flags = One.getIntArray("Flags");
            long[] Sums = One.getLongArray("Sums");
            int Size = Math.min(Keys.length, Math.min(Stamps.length, Math.min(Flags.length, Sums.length / 6)));
            for (int Cell = 0; Cell < Size; Cell++) {
                double[] Values = new double[6];
                for (int Part = 0; Part < 6; Part++) {
                    Values[Part] = Double.longBitsToDouble(Sums[Cell * 6 + Part]);
                }
                Found.restore(Keys[Cell], Values[0], Values[1], Values[2], Values[3], Values[4], Values[5], Stamps[Cell], Flags[Cell]);
            }
            long[] Stashes = One.getLongArray("Stashes");
            for (int Slot = 0; Slot + 1 < Stashes.length; Slot += 2) {
                Found.noteStash(Stashes[Slot], Stashes[Slot + 1]);
            }
        }
        return Data;
    }
}
