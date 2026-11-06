package net.rai.scp_unleashed.raid;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RaidWorld extends SavedData {
    public static final String DataName = "scp_unleashed_raids";
    private static final SavedData.Factory<RaidWorld> DataFactory = new SavedData.Factory<>(RaidWorld::new, RaidWorld::load);

    private final Map<Integer, Raid> Active = new LinkedHashMap<>();
    private final Map<ResourceLocation, RaidLedger> Ledgers = new LinkedHashMap<>();
    private int NextId;

    public static RaidWorld get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public Collection<Raid> active() {
        return Active.values();
    }

    public List<Raid> snapshot() {
        return new ArrayList<>(Active.values());
    }

    @Nullable
    public Raid raid(int Id) {
        return Active.get(Id);
    }

    @Nullable
    public Raid activeFor(ResourceLocation Faction) {
        for (Raid Found : Active.values()) {
            if (Found.faction().equals(Faction) && Found.live()) {
                return Found;
            }
        }
        return null;
    }

    public int nextId() {
        int Id = NextId++;
        setDirty();
        return Id;
    }

    public void add(Raid Found) {
        Found.bind(this::setDirty);
        Active.put(Found.id(), Found);
        setDirty();
    }

    public void remove(Raid Found) {
        Active.remove(Found.id());
        setDirty();
    }

    public int raiderCount() {
        int Count = 0;
        for (Raid Found : Active.values()) {
            Count += Found.remaining();
        }
        return Count;
    }

    public RaidLedger ledger(ResourceLocation Faction, int Era) {
        RaidLedger Found = Ledgers.get(Faction);
        if (Found == null) {
            Found = new RaidLedger(Faction, Era);
            Found.bind(this::setDirty);
            Ledgers.put(Faction, Found);
            setDirty();
        } else if (Found.era() != Era) {
            Found.renew(Era);
        }
        return Found;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Saved.putInt("NextId", NextId);
        ListTag RaidList = new ListTag();
        for (Raid Found : Active.values()) {
            RaidList.add(Found.save());
        }
        Saved.put("Raids", RaidList);
        ListTag LedgerList = new ListTag();
        for (RaidLedger Ledger : Ledgers.values()) {
            LedgerList.add(Ledger.save(Lookup));
        }
        Saved.put("Ledgers", LedgerList);
        return Saved;
    }

    public static RaidWorld load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        RaidWorld Data = new RaidWorld();
        Data.NextId = Saved.getInt("NextId");
        ListTag RaidList = Saved.getList("Raids", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < RaidList.size(); Index++) {
            Raid Found = Raid.load(RaidList.getCompound(Index));
            if (Found != null) {
                Found.bind(Data::setDirty);
                Data.Active.put(Found.id(), Found);
                Data.NextId = Math.max(Data.NextId, Found.id() + 1);
            }
        }
        ListTag LedgerList = Saved.getList("Ledgers", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < LedgerList.size(); Index++) {
            RaidLedger Ledger = RaidLedger.load(LedgerList.getCompound(Index), Lookup);
            if (Ledger != null) {
                Ledger.bind(Data::setDirty);
                Data.Ledgers.put(Ledger.faction(), Ledger);
            }
        }
        return Data;
    }
}
