package net.rai.scp_unleashed.rocketry.satellite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class SatelliteNetwork extends SavedData {
    public static final String DataName = "scp_unleashed_satellites";
    public static final String GeneratedPrefix = "SAT-";
    public static final int NameLimit = 32;
    private static final SavedData.Factory<SatelliteNetwork> DataFactory = new SavedData.Factory<>(SatelliteNetwork::new, SatelliteNetwork::load);

    public record Satellite(UUID Id, String Name, UUID Owner, long DeployedAt) {
        public static final Codec<Satellite> SatelliteCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Satellite::Id),
                Codec.STRING.fieldOf("name").forGetter(Satellite::Name),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Satellite::Owner),
                Codec.LONG.fieldOf("deployed_at").forGetter(Satellite::DeployedAt)
        ).apply(Instance, Satellite::new));
    }

    private final List<Satellite> Satellites = new ArrayList<>();
    private int NextNumber = 1;

    public static SatelliteNetwork get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public List<Satellite> all() {
        return List.copyOf(Satellites);
    }

    public boolean anyDeployed() {
        return !Satellites.isEmpty();
    }

    public int count() {
        return Satellites.size();
    }

    public List<Satellite> ownedBy(UUID Owner) {
        List<Satellite> Owned = new ArrayList<>();
        for (Satellite Entry : Satellites) {
            if (Entry.Owner().equals(Owner)) {
                Owned.add(Entry);
            }
        }
        return Owned;
    }

    public Optional<Satellite> find(UUID Id) {
        for (Satellite Entry : Satellites) {
            if (Entry.Id().equals(Id)) {
                return Optional.of(Entry);
            }
        }
        return Optional.empty();
    }

    public Optional<Satellite> findNamed(String Name) {
        String Wanted = Name.trim().toLowerCase(Locale.ROOT);
        for (Satellite Entry : Satellites) {
            if (Entry.Name().toLowerCase(Locale.ROOT).equals(Wanted)) {
                return Optional.of(Entry);
            }
        }
        return Optional.empty();
    }

    public Satellite deploy(@Nullable String Name, UUID Owner, long DeployedAt) {
        String Generated = GeneratedPrefix + NextNumber;
        NextNumber++;
        String Chosen = Generated;
        if (Name != null) {
            String Trimmed = Name.trim();
            if (Trimmed.length() > NameLimit) {
                Trimmed = Trimmed.substring(0, NameLimit).trim();
            }
            if (!Trimmed.isEmpty()) {
                Chosen = findNamed(Trimmed).isPresent() ? Trimmed + " " + Generated : Trimmed;
            }
        }
        Satellite Made = new Satellite(UUID.randomUUID(), Chosen, Owner, DeployedAt);
        Satellites.add(Made);
        setDirty();
        return Made;
    }

    public boolean remove(UUID Id) {
        boolean Removed = Satellites.removeIf(Entry -> Entry.Id().equals(Id));
        if (Removed) {
            setDirty();
        }
        return Removed;
    }

    public Optional<Satellite> removeNamed(String Name) {
        Optional<Satellite> Found = findNamed(Name);
        Found.ifPresent(Entry -> remove(Entry.Id()));
        return Found;
    }

    public static SatelliteNetwork load(CompoundTag Data, HolderLookup.Provider Registries) {
        SatelliteNetwork Network = new SatelliteNetwork();
        Network.NextNumber = Math.max(1, Data.getInt("NextNumber"));
        ListTag Entries = Data.getList("Satellites", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size(); Index++) {
            Satellite.SatelliteCodec.parse(NbtOps.INSTANCE, Entries.getCompound(Index)).result().ifPresent(Network.Satellites::add);
        }
        return Network;
    }

    @Override
    public CompoundTag save(CompoundTag Data, HolderLookup.Provider Registries) {
        Data.putInt("NextNumber", NextNumber);
        ListTag Entries = new ListTag();
        for (Satellite Entry : Satellites) {
            Satellite.SatelliteCodec.encodeStart(NbtOps.INSTANCE, Entry).result().ifPresent(Entries::add);
        }
        Data.put("Satellites", Entries);
        return Data;
    }
}
