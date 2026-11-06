package net.rai.scp_unleashed.faction;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class FactionWorld extends SavedData {
    public static final String DataName = "scp_unleashed_factions";
    private static final SavedData.Factory<FactionWorld> DataFactory = new SavedData.Factory<>(FactionWorld::new, FactionWorld::load);

    private final Map<ResourceLocation, FactionState> States = new LinkedHashMap<>();

    public static FactionWorld get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static FactionState state(MinecraftServer Server, FactionType Type) {
        return get(Server).state(Type.id());
    }

    public FactionState state(ResourceLocation Id) {
        FactionState Found = States.get(Id);
        if (Found == null) {
            Found = new FactionState(Id);
            Found.bind(this::setDirty);
            States.put(Id, Found);
            setDirty();
        }
        return Found;
    }

    public Collection<FactionState> states() {
        return States.values();
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag Entries = new ListTag();
        for (FactionState State : States.values()) {
            Entries.add(State.save());
        }
        Saved.put("Factions", Entries);
        return Saved;
    }

    public static FactionWorld load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        FactionWorld Data = new FactionWorld();
        ListTag Entries = Saved.getList("Factions", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size(); Index++) {
            FactionState State = FactionState.load(Entries.getCompound(Index));
            if (State != null) {
                State.bind(Data::setDirty);
                Data.States.put(State.faction(), State);
            }
        }
        return Data;
    }
}
