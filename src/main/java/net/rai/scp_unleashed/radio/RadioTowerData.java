package net.rai.scp_unleashed.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RadioTowerData extends SavedData {
    public static final String DataName = "scp_unleashed_radio_towers";
    private static final SavedData.Factory<RadioTowerData> DataFactory = new SavedData.Factory<>(RadioTowerData::new, RadioTowerData::load);

    private final Map<BlockPos, Tower> Towers = new LinkedHashMap<>();

    public record Tower(BlockPos Pos, int Masts, boolean Powered, int Channel, List<RadioBleedKind> Sources) {
        public Tower {
            Pos = Pos.immutable();
            Sources = List.copyOf(Sources);
        }

        public boolean relays() {
            return Powered && Masts > 0;
        }
    }

    public static RadioTowerData get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static List<RadioNetwork.Node> nodes(ServerLevel World, RadioNetwork.Ranges Limits) {
        return RadioNetwork.nodes(Limits, get(World).towers());
    }

    public Collection<Tower> towers() {
        return Collections.unmodifiableCollection(Towers.values());
    }

    public Tower find(BlockPos Pos) {
        return Towers.get(Pos);
    }

    public void put(Tower Entry) {
        Tower Old = Towers.put(Entry.Pos(), Entry);
        if (!Objects.equals(Old, Entry)) {
            setDirty();
        }
    }

    public void remove(BlockPos Pos) {
        if (Towers.remove(Pos) != null) {
            setDirty();
        }
    }

    public int prune(ServerLevel World) {
        List<BlockPos> Stale = new ArrayList<>();
        for (BlockPos Pos : Towers.keySet()) {
            if (World.isLoaded(Pos) && !(World.getBlockState(Pos).getBlock() instanceof RadioTowerBlock)) {
                Stale.add(Pos);
            }
        }
        for (BlockPos Pos : Stale) {
            remove(Pos);
        }
        return Stale.size();
    }

    public int size() {
        return Towers.size();
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (Tower Each : Towers.values()) {
            CompoundTag Entry = new CompoundTag();
            Entry.putLong("Pos", Each.Pos().asLong());
            Entry.putInt("Masts", Each.Masts());
            Entry.putBoolean("Powered", Each.Powered());
            Entry.putInt("Channel", Each.Channel());
            ListTag Kinds = new ListTag();
            for (RadioBleedKind Kind : Each.Sources()) {
                Kinds.add(StringTag.valueOf(Kind.getSerializedName()));
            }
            Entry.put("Sources", Kinds);
            Saved.add(Entry);
        }
        Root.put("Towers", Saved);
        return Root;
    }

    private static RadioTowerData load(CompoundTag Root, HolderLookup.Provider Registries) {
        RadioTowerData Data = new RadioTowerData();
        ListTag Saved = Root.getList("Towers", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Entry = Saved.getCompound(Index);
            List<RadioBleedKind> Kinds = new ArrayList<>();
            ListTag Names = Entry.getList("Sources", Tag.TAG_STRING);
            for (int Each = 0; Each < Names.size(); Each++) {
                RadioBleedKind Kind = RadioBleedKind.byName(Names.getString(Each));
                if (Kind != null) {
                    Kinds.add(Kind);
                }
            }
            Tower Made = new Tower(BlockPos.of(Entry.getLong("Pos")), Math.max(0, Entry.getInt("Masts")), Entry.getBoolean("Powered"),
                    RadioCarry.clampChannel(Entry.getInt("Channel")), Kinds);
            Data.Towers.put(Made.Pos(), Made);
        }
        return Data;
    }
}
