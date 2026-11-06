package net.rai.scp_unleashed.faction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FactionLive {
    private static final Map<ResourceLocation, Map<UUID, Entity>> Loaded = new HashMap<>();

    private FactionLive() {
    }

    public static void add(ResourceLocation Faction, Entity Body) {
        Loaded.computeIfAbsent(Faction, Key -> new LinkedHashMap<>()).put(Body.getUUID(), Body);
    }

    public static void remove(ResourceLocation Faction, Entity Body) {
        Map<UUID, Entity> Found = Loaded.get(Faction);
        if (Found != null && Found.get(Body.getUUID()) == Body) {
            Found.remove(Body.getUUID());
        }
    }

    public static List<Entity> members(ResourceLocation Faction) {
        Map<UUID, Entity> Found = Loaded.get(Faction);
        if (Found == null) {
            return List.of();
        }
        List<Entity> Result = new ArrayList<>();
        for (Entity Body : Found.values()) {
            if (!Body.isRemoved()) {
                Result.add(Body);
            }
        }
        return Result;
    }

    public static List<Mob> atSite(ResourceLocation Faction, int Site) {
        List<Mob> Result = new ArrayList<>();
        for (Entity Body : members(Faction)) {
            if (Body instanceof Mob Member && Factions.bondOf(Body).Site() == Site && Member.isAlive()) {
                Result.add(Member);
            }
        }
        return Result;
    }

    public static int count(ResourceLocation Faction) {
        return members(Faction).size();
    }

    public static int total() {
        int Count = 0;
        for (ResourceLocation Faction : Loaded.keySet()) {
            Count += count(Faction);
        }
        return Count;
    }

    public static void clear() {
        Loaded.clear();
    }
}
