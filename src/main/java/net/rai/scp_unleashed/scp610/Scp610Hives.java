package net.rai.scp_unleashed.scp610;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionWorld;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp610Hives {
    public static final int LivingReach = 32;
    public static final int LivingDepth = 72;
    private static final Map<Integer, Scp610Hive> Loaded = new LinkedHashMap<>();
    private static final Map<ResourceKey<Level>, Long2IntOpenHashMap> Owners = new HashMap<>();
    @Nullable
    private static FactionState Bound;
    @Nullable
    private static Scp610FleshStore Store;
    private static int Era = -1;

    private Scp610Hives() {
    }

    public static FactionState state(MinecraftServer Server) {
        return FactionWorld.state(Server, Scp610Faction.Instance);
    }

    public static Map<Integer, Scp610Hive> hives(MinecraftServer Server) {
        FactionState State = state(Server);
        if (State != Bound || State.era() != Era || Store == null) {
            rebuild(Server, State);
        } else if (!matches(State)) {
            sync(State);
        }
        return Loaded;
    }

    private static boolean matches(FactionState State) {
        int Count = 0;
        for (FactionSite Site : State.sites()) {
            if (!Site.is(Scp610Hive.Kind)) {
                continue;
            }
            Count++;
            Scp610Hive Found = Loaded.get(Site.id());
            if (Found == null || Found.Site != Site) {
                return false;
            }
        }
        return Count == Loaded.size();
    }

    private static void rebuild(MinecraftServer Server, FactionState State) {
        Loaded.clear();
        Owners.clear();
        Bound = State;
        Era = State.era();
        Store = Scp610FleshStore.get(Server);
        sync(State);
    }

    private static void sync(FactionState State) {
        Map<Integer, FactionSite> Live = new HashMap<>();
        for (FactionSite Site : State.sites()) {
            if (Site.is(Scp610Hive.Kind)) {
                Live.put(Site.id(), Site);
            }
        }
        List<Scp610Hive> Gone = new ArrayList<>();
        for (Scp610Hive Hive : Loaded.values()) {
            if (Live.get(Hive.id()) != Hive.Site) {
                Gone.add(Hive);
            }
        }
        for (Scp610Hive Hive : Gone) {
            unindex(Hive);
            Loaded.remove(Hive.id());
        }
        for (FactionSite Site : Live.values()) {
            if (Loaded.containsKey(Site.id())) {
                continue;
            }
            Scp610Hive Made = Scp610Hive.load(Site, Store.owned(Site.id()));
            if (Made.Migrated) {
                Store.setDirty();
            }
            Made.survey();
            Loaded.put(Site.id(), Made);
            Long2IntOpenHashMap Index = index(Site.dimension());
            for (int At = 0; At < Made.Owned.size(); At++) {
                Index.put(Made.Owned.get(At), Site.id());
            }
        }
        Store.retain(Live.keySet());
    }

    private static void unindex(Scp610Hive Hive) {
        Long2IntOpenHashMap Index = Owners.get(Hive.Site.dimension());
        if (Index == null) {
            return;
        }
        for (int At = 0; At < Hive.Owned.size(); At++) {
            long Pos = Hive.Owned.get(At);
            if (Index.get(Pos) == Hive.id()) {
                Index.remove(Pos);
            }
        }
    }

    public static Scp610FleshStore store(MinecraftServer Server) {
        hives(Server);
        return Store != null ? Store : Scp610FleshStore.get(Server);
    }

    private static Long2IntOpenHashMap index(ResourceKey<Level> Dimension) {
        return Owners.computeIfAbsent(Dimension, Key -> {
            Long2IntOpenHashMap Made = new Long2IntOpenHashMap();
            Made.defaultReturnValue(-1);
            return Made;
        });
    }

    @Nullable
    public static Scp610Hive cached(FactionSite Site) {
        Scp610Hive Found = Loaded.get(Site.id());
        return Found != null && Found.Site == Site ? Found : null;
    }

    public static List<Scp610Hive> list(MinecraftServer Server) {
        return new ArrayList<>(hives(Server).values());
    }

    @Nullable
    public static Scp610Hive hive(MinecraftServer Server, int Id) {
        return hives(Server).get(Id);
    }

    @Nullable
    public static Scp610Hive ownerOf(ServerLevel World, BlockPos Pos) {
        Map<Integer, Scp610Hive> All = hives(World.getServer());
        Long2IntOpenHashMap Index = Owners.get(World.dimension());
        if (Index == null) {
            return null;
        }
        int Id = Index.get(Pos.asLong());
        return Id < 0 ? null : All.get(Id);
    }

    public static boolean own(ServerLevel World, Scp610Hive Hive, BlockPos Pos) {
        hives(World.getServer());
        long Key = Pos.asLong();
        if (!Hive.Owned.add(Key)) {
            return false;
        }
        index(World.dimension()).put(Key, Hive.id());
        Hive.touched(Key);
        Hive.Dirty = true;
        if (Store != null) {
            Store.setDirty();
        }
        return true;
    }

    public static boolean released(ServerLevel World, BlockPos Pos) {
        return released(World, Pos, true);
    }

    public static boolean released(ServerLevel World, BlockPos Pos, boolean Scarred) {
        Scp610Hive Owner = ownerOf(World, Pos);
        if (Owner == null) {
            return false;
        }
        Owner.Owned.remove(Pos.asLong());
        Owner.Frontier.remove(Pos.asLong());
        for (Direction Side : Direction.values()) {
            long Near = BlockPos.offset(Pos.asLong(), Side);
            if (Owner.Owned.contains(Near) && Owner.surfaceLevel(Near)) {
                Owner.Frontier.add(Near);
            }
        }
        if (Store != null) {
            Store.setDirty();
        }
        if (Scarred) {
            Owner.scar(Pos.asLong());
        }
        Long2IntOpenHashMap Index = Owners.get(World.dimension());
        if (Index != null) {
            Index.remove(Pos.asLong());
        }
        Owner.Dirty = true;
        return true;
    }

    public static void forget(ServerLevel World, Scp610Hive Hive) {
        unindex(Hive);
        Loaded.remove(Hive.id());
        Hive.Frontier.clear();
        Scp610FleshStore Held = Store != null ? Store : Scp610FleshStore.get(World.getServer());
        Held.remove(Hive.id());
    }

    public static boolean nearLivingHive(ServerLevel World, BlockPos Pos) {
        if (ownerOf(World, Pos) != null) {
            return true;
        }
        for (Scp610Hive Hive : hives(World.getServer()).values()) {
            if (!Hive.Site.dimension().equals(World.dimension())) {
                continue;
            }
            BlockPos Anchor = Hive.anchor();
            double Dx = Anchor.getX() - Pos.getX();
            double Dz = Anchor.getZ() - Pos.getZ();
            double Reach = Math.max(LivingReach, Hive.reach() + 8);
            int Floor = Math.min(Anchor.getY() - LivingDepth, Hive.Lowest == Integer.MAX_VALUE ? Integer.MAX_VALUE : Hive.Lowest - 8);
            if (Dx * Dx + Dz * Dz <= Reach * Reach && Pos.getY() <= Anchor.getY() + Scp610Growth.SurfaceRiseCap + 8 && Pos.getY() >= Floor) {
                return true;
            }
        }
        return false;
    }

    public static int totalFlesh(MinecraftServer Server) {
        int Total = 0;
        for (Scp610Hive Hive : hives(Server).values()) {
            Total += Hive.flesh();
        }
        return Total;
    }

    public static void flush(MinecraftServer Server) {
        FactionState State = state(Server);
        boolean Changed = false;
        for (Scp610Hive Hive : hives(Server).values()) {
            if (Hive.Dirty) {
                Hive.flush();
                Changed = true;
            }
        }
        if (Changed) {
            State.changed();
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        Scp610Limits.refresh(Server);
        Scp610Growth.tick(Server);
        Scp610Biomass.tick(Server);
        if (Server.getTickCount() % 20 == 0) {
            flush(Server);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Loaded.clear();
        Owners.clear();
        Bound = null;
        Store = null;
        Era = -1;
        Scp610Dialogue.forget();
    }
}
