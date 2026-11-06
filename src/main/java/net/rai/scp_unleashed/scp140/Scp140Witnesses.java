package net.rai.scp_unleashed.scp140;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageContainers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Scp140Witnesses {
    public static final long StoredGrace = 5L;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> Placed = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<ItemEntity>> Dropped = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> Stored = new HashMap<>();

    private Scp140Witnesses() {
    }

    public static void placed(ServerLevel World, BlockPos Pos, boolean Present) {
        Set<BlockPos> Found = Placed.computeIfAbsent(World.dimension(), Key -> new HashSet<>());
        if (Present) {
            Found.add(Pos.immutable());
        } else {
            Found.remove(Pos);
        }
    }

    public static void dropped(ServerLevel World, ItemEntity Loose, boolean Present) {
        Set<ItemEntity> Found = Dropped.computeIfAbsent(World.dimension(), Key -> Collections.newSetFromMap(new IdentityHashMap<>()));
        if (Present) {
            Found.add(Loose);
        } else {
            Found.remove(Loose);
        }
    }

    public static void stored(ServerLevel World, BlockPos Pos) {
        Stored.computeIfAbsent(World.dimension(), Key -> new HashMap<>()).put(Pos.immutable(), World.getGameTime());
    }

    public static void clear() {
        Placed.clear();
        Dropped.clear();
        Stored.clear();
    }

    public static boolean carries(Player Holder) {
        if (Holder.isSpectator() || !Holder.isAlive()) {
            return false;
        }
        return Holder.getInventory().hasAnyMatching(Scp140BookItem::is) || Scp140BookItem.is(Holder.containerMenu.getCarried());
    }

    public static List<Witness> all(ServerLevel World) {
        List<Witness> Found = new ArrayList<>();
        Set<BlockPos> Books = Placed.get(World.dimension());
        if (Books != null) {
            Iterator<BlockPos> Cursor = Books.iterator();
            while (Cursor.hasNext()) {
                BlockPos Pos = Cursor.next();
                if (!World.isLoaded(Pos) || !(World.getBlockEntity(Pos) instanceof Scp140BookBlockEntity)) {
                    Cursor.remove();
                    continue;
                }
                Found.add(new Witness(Pos, Vec3.atCenterOf(Pos), Kind.Placed));
            }
        }
        Set<ItemEntity> Items = Dropped.get(World.dimension());
        if (Items != null) {
            Iterator<ItemEntity> Cursor = Items.iterator();
            while (Cursor.hasNext()) {
                ItemEntity Loose = Cursor.next();
                if (Loose.isRemoved() || Loose.level() != World || !Scp140BookItem.is(Loose.getItem())) {
                    Cursor.remove();
                    continue;
                }
                Found.add(new Witness(Loose.blockPosition(), Loose.position(), Kind.Dropped));
            }
        }
        Map<BlockPos, Long> Lockers = Stored.get(World.dimension());
        if (Lockers != null) {
            long Now = World.getGameTime();
            Iterator<Map.Entry<BlockPos, Long>> Cursor = Lockers.entrySet().iterator();
            while (Cursor.hasNext()) {
                Map.Entry<BlockPos, Long> Entry = Cursor.next();
                BlockPos Pos = Entry.getKey();
                if (Now - Entry.getValue() > StoredGrace || !World.isLoaded(Pos)) {
                    Cursor.remove();
                    continue;
                }
                Optional<StorageContainerBlockEntity> Locker = StorageContainers.find(World, Pos);
                if (Locker.isEmpty() || !Scp140BookItem.is(Locker.get().peekContents())) {
                    Cursor.remove();
                    continue;
                }
                if (Locker.get().isVisible()) {
                    Found.add(new Witness(Pos, Locker.get().soundCentre(), Kind.Stored));
                }
            }
        }
        for (Player Holder : World.players()) {
            if (carries(Holder)) {
                Found.add(new Witness(Holder.blockPosition(), Holder.position().add(0.0, Holder.getBbHeight() * 0.5, 0.0), Kind.Carried));
            }
        }
        return Found;
    }

    public static Optional<Witness> nearest(ServerLevel World, Vec3 Where, double Radius) {
        Witness Best = null;
        double BestDistance = Radius * Radius;
        for (Witness Candidate : all(World)) {
            double Distance = Candidate.Centre().distanceToSqr(Where);
            if (Distance <= BestDistance) {
                Best = Candidate;
                BestDistance = Distance;
            }
        }
        return Optional.ofNullable(Best);
    }

    public static Optional<Witness> closest(ServerLevel World, Vec3 Where) {
        return nearest(World, Where, Double.MAX_VALUE / 4.0);
    }

    public static void refreshPlaced(MinecraftServer Server) {
        for (ServerLevel World : Server.getAllLevels()) {
            Set<BlockPos> Books = Placed.get(World.dimension());
            if (Books == null) {
                continue;
            }
            for (BlockPos Pos : List.copyOf(Books)) {
                if (World.isLoaded(Pos) && World.getBlockEntity(Pos) instanceof Scp140BookBlockEntity Book) {
                    Book.refresh();
                }
            }
        }
    }

    public enum Kind {
        Placed,
        Dropped,
        Stored,
        Carried
    }

    public record Witness(BlockPos Pos, Vec3 Centre, Kind Source) {
    }
}
