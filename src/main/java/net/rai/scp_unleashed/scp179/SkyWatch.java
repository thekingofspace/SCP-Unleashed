package net.rai.scp_unleashed.scp179;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SkyWatch extends SavedData {
    public static final String DataName = "scp_unleashed_sky_watch";
    private static final SavedData.Factory<SkyWatch> DataFactory = new SavedData.Factory<>(SkyWatch::new, SkyWatch::load);

    private final Map<ResourceLocation, Set<UUID>> Watchers = new HashMap<>();

    public static SkyWatch get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean add(ResourceLocation Target, UUID Watcher) {
        boolean Added = Watchers.computeIfAbsent(Target, Key -> new LinkedHashSet<>()).add(Watcher);
        if (Added) {
            setDirty();
        }
        return Added;
    }

    public boolean isWatching(ResourceLocation Target, UUID Watcher) {
        Set<UUID> Found = Watchers.get(Target);
        return Found != null && Found.contains(Watcher);
    }

    public Set<UUID> watchers(ResourceLocation Target) {
        Set<UUID> Found = Watchers.get(Target);
        return Found == null ? Set.of() : Set.copyOf(Found);
    }

    public List<ServerPlayer> online(MinecraftServer Server, ResourceLocation Target) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (UUID Watcher : watchers(Target)) {
            ServerPlayer Online = Server.getPlayerList().getPlayer(Watcher);
            if (Online != null && !Online.hasDisconnected()) {
                Found.add(Online);
            }
        }
        return Found;
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (Map.Entry<ResourceLocation, Set<UUID>> Entry : Watchers.entrySet()) {
            CompoundTag Item = new CompoundTag();
            Item.putString("Target", Entry.getKey().toString());
            ListTag Ids = new ListTag();
            for (UUID Watcher : Entry.getValue()) {
                Ids.add(NbtUtils.createUUID(Watcher));
            }
            Item.put("Watchers", Ids);
            Saved.add(Item);
        }
        Root.put("Targets", Saved);
        return Root;
    }

    private static SkyWatch load(CompoundTag Root, HolderLookup.Provider Registries) {
        SkyWatch Data = new SkyWatch();
        ListTag Saved = Root.getList("Targets", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Item = Saved.getCompound(Index);
            ResourceLocation Target = ResourceLocation.tryParse(Item.getString("Target"));
            if (Target == null) {
                continue;
            }
            Set<UUID> Ids = new LinkedHashSet<>();
            for (Tag Id : Item.getList("Watchers", Tag.TAG_INT_ARRAY)) {
                Ids.add(NbtUtils.loadUUID(Id));
            }
            if (!Ids.isEmpty()) {
                Data.Watchers.put(Target, Ids);
            }
        }
        return Data;
    }
}
