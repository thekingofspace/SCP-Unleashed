package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.archive.AnomalySpawners;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Scp354Pools extends SavedData {
    public static final String DataName = "scp_unleashed_scp354";
    public static final double KnownRange = 32.0;
    private static final SavedData.Factory<Scp354Pools> DataFactory = new SavedData.Factory<>(Scp354Pools::new, Scp354Pools::load);

    public static final class Pool {
        public final ResourceKey<Level> Dimension;
        public final BlockPos Centre;
        public final int Radius;
        public final int Depth;
        public int Timer;
        public long LastSeen;
        public int Born;
        public List<BlockPos> Tops = List.of();

        public Pool(ResourceKey<Level> Dimension, BlockPos Centre, int Radius, int Depth, int Timer, long LastSeen, int Born) {
            this.Dimension = Dimension;
            this.Centre = Centre.immutable();
            this.Radius = Radius;
            this.Depth = Depth;
            this.Timer = Timer;
            this.LastSeen = LastSeen;
            this.Born = Born;
        }

        public CompoundTag save() {
            CompoundTag Saved = new CompoundTag();
            Saved.putString("Dimension", Dimension.location().toString());
            Saved.putInt("X", Centre.getX());
            Saved.putInt("Y", Centre.getY());
            Saved.putInt("Z", Centre.getZ());
            Saved.putInt("Radius", Radius);
            Saved.putInt("Depth", Depth);
            Saved.putInt("Timer", Timer);
            Saved.putLong("LastSeen", LastSeen);
            Saved.putInt("Born", Born);
            return Saved;
        }

        public static Optional<Pool> load(CompoundTag Saved) {
            ResourceLocation Id = ResourceLocation.tryParse(Saved.getString("Dimension"));
            if (Id == null) {
                return Optional.empty();
            }
            return Optional.of(new Pool(ResourceKey.create(Registries.DIMENSION, Id), new BlockPos(Saved.getInt("X"), Saved.getInt("Y"), Saved.getInt("Z")),
                    Saved.getInt("Radius"), Saved.getInt("Depth"), Saved.getInt("Timer"), Saved.getLong("LastSeen"), Saved.getInt("Born")));
        }
    }

    private final List<Pool> Pools = new ArrayList<>();

    public static Scp354Pools get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static Scp354Pools load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp354Pools Data = new Scp354Pools();
        ListTag Entries = Saved.getList("Pools", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size(); Index++) {
            Pool.load(Entries.getCompound(Index)).ifPresent(Data.Pools::add);
        }
        return Data;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag Entries = new ListTag();
        for (Pool Entry : Pools) {
            Entries.add(Entry.save());
        }
        Saved.put("Pools", Entries);
        return Saved;
    }

    public List<Pool> pools() {
        return Pools;
    }

    public List<Pool> in(ResourceKey<Level> Dimension) {
        List<Pool> Found = new ArrayList<>();
        for (Pool Entry : Pools) {
            if (Entry.Dimension.equals(Dimension)) {
                Found.add(Entry);
            }
        }
        return Found;
    }

    public Pool add(Pool Entry) {
        Pools.add(Entry);
        setDirty();
        return Entry;
    }

    public boolean remove(Pool Entry) {
        boolean Removed = Pools.remove(Entry);
        if (Removed) {
            setDirty();
        }
        return Removed;
    }

    public Optional<Pool> nearest(ResourceKey<Level> Dimension, BlockPos Pos, double Range) {
        Pool Best = null;
        double BestDistance = Range * Range;
        for (Pool Entry : Pools) {
            if (!Entry.Dimension.equals(Dimension)) {
                continue;
            }
            double Dx = Entry.Centre.getX() - Pos.getX();
            double Dz = Entry.Centre.getZ() - Pos.getZ();
            double Distance = Dx * Dx + Dz * Dz;
            if (Distance <= BestDistance) {
                BestDistance = Distance;
                Best = Entry;
            }
        }
        return Optional.ofNullable(Best);
    }

    public Optional<Pool> first(ResourceKey<Level> Dimension) {
        for (Pool Entry : Pools) {
            if (Entry.Dimension.equals(Dimension)) {
                return Optional.of(Entry);
            }
        }
        return Optional.empty();
    }

    public static Pool create(ServerLevel World, BlockPos Site) {
        Scp354Basin.Result Built = Scp354Basin.build(World, Site, World.getRandom());
        if (Built.Liquid().isEmpty()) {
            return null;
        }
        Scp354Pools Data = get(World.getServer());
        return Data.add(new Pool(World.dimension(), Built.Shape().centre(), Built.Shape().radius(), Built.Shape().deepest(), Scp354Emergence.firstTimer(World.getRandom()),
                World.getGameTime(), 0));
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        Scp354Pools Data = get(World.getServer());
        if (Data.nearest(World.dimension(), Found.Pos(), KnownRange).isPresent()) {
            return true;
        }
        return create(World, Found.Pos()) != null;
    }

    public static CompoundTag snapshot(MinecraftServer Server) {
        return get(Server).save(new CompoundTag(), Server.registryAccess());
    }
}
