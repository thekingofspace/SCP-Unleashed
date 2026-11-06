package net.rai.scp_unleashed.scp2845;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Scp2845Records extends SavedData {
    public static final String DataName = "scp_unleashed_scp2845";
    private static final SavedData.Factory<Scp2845Records> DataFactory = new SavedData.Factory<>(Scp2845Records::new, Scp2845Records::load);

    private final Map<UUID, Entry> Deer = new LinkedHashMap<>();
    private final Map<UUID, BlockPos> Landings = new HashMap<>();

    public enum State {
        Free,
        Stasis,
        Contained
    }

    public record Entry(State Phase, String Dimension, @Nullable BlockPos Stone, long Since) {
    }

    public static Scp2845Records get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public void set(UUID Id, State Phase, Level World, @Nullable BlockPos Stone) {
        Entry Previous = Deer.get(Id);
        String Dimension = World.dimension().location().toString();
        BlockPos Kept = Stone == null ? null : Stone.immutable();
        if (Previous != null && Previous.Phase() == Phase && Previous.Dimension().equals(Dimension) && Objects.equals(Previous.Stone(), Kept)) {
            return;
        }
        Deer.put(Id, new Entry(Phase, Dimension, Kept, World.getGameTime()));
        setDirty();
    }

    public void forget(UUID Id) {
        if (Deer.remove(Id) != null) {
            setDirty();
        }
    }

    public Optional<Entry> entry(UUID Id) {
        return Optional.ofNullable(Deer.get(Id));
    }

    public boolean holdsStone(Level World, BlockPos Stone) {
        String Dimension = World.dimension().location().toString();
        for (Entry Each : Deer.values()) {
            if (Each.Phase() == State.Free && Stone.equals(Each.Stone()) && Dimension.equals(Each.Dimension())) {
                return true;
            }
        }
        return false;
    }

    public Map<UUID, Entry> all() {
        return Map.copyOf(Deer);
    }

    public void landed(UUID Case, BlockPos Stone) {
        Landings.put(Case, Stone.immutable());
        setDirty();
    }

    public Optional<BlockPos> landing(UUID Case) {
        return Optional.ofNullable(Landings.get(Case));
    }

    public void clearLanding(UUID Case) {
        if (Landings.remove(Case) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (Map.Entry<UUID, Entry> Each : Deer.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Each.getKey());
            Row.putString("State", Each.getValue().Phase().name());
            Row.putString("Dimension", Each.getValue().Dimension());
            if (Each.getValue().Stone() != null) {
                Row.putLong("Stone", Each.getValue().Stone().asLong());
            }
            Row.putLong("Since", Each.getValue().Since());
            Saved.add(Row);
        }
        Root.put("Deer", Saved);
        ListTag Sites = new ListTag();
        for (Map.Entry<UUID, BlockPos> Each : Landings.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Case", Each.getKey());
            Row.putLong("Stone", Each.getValue().asLong());
            Sites.add(Row);
        }
        Root.put("Landings", Sites);
        return Root;
    }

    public static Scp2845Records load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp2845Records Data = new Scp2845Records();
        ListTag Saved = Root.getList("Deer", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Row = Saved.getCompound(Index);
            if (!Row.hasUUID("Id")) {
                continue;
            }
            State Phase;
            try {
                Phase = State.valueOf(Row.getString("State"));
            } catch (IllegalArgumentException Problem) {
                Phase = State.Free;
            }
            BlockPos Stone = Row.contains("Stone") ? BlockPos.of(Row.getLong("Stone")) : null;
            Data.Deer.put(Row.getUUID("Id"), new Entry(Phase, Row.getString("Dimension"), Stone, Row.getLong("Since")));
        }
        ListTag Sites = Root.getList("Landings", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Sites.size(); Index++) {
            CompoundTag Row = Sites.getCompound(Index);
            if (Row.hasUUID("Case")) {
                Data.Landings.put(Row.getUUID("Case"), BlockPos.of(Row.getLong("Stone")));
            }
        }
        return Data;
    }
}
