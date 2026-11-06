package net.rai.scp_unleashed.scp303;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Scp303State extends SavedData {
    public static final String DataName = "scp_unleashed_scp303";
    private static final SavedData.Factory<Scp303State> DataFactory = new SavedData.Factory<>(Scp303State::new, Scp303State::load);

    private final Set<UUID> Unlocked = new LinkedHashSet<>();
    private boolean Active;
    private boolean Away;
    private long ReturnAt;
    @Nullable
    private UUID Current;
    @Nullable
    private UUID Favourite;
    @Nullable
    private Binding Bound;

    public record Binding(ResourceKey<Level> Dimension, BlockPos Door, int CellSide) {
    }

    public static Scp303State get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean isUnlocked(UUID Reader) {
        return Unlocked.contains(Reader);
    }

    public boolean unlock(UUID Reader) {
        if (Unlocked.add(Reader)) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean isActive() {
        return Active;
    }

    public boolean isAway() {
        return Active && Away;
    }

    public long returnAt() {
        return ReturnAt;
    }

    @Nullable
    public UUID current() {
        return Current;
    }

    public boolean isCurrent(UUID Id) {
        return Active && Id.equals(Current);
    }

    @Nullable
    public UUID favourite() {
        return Favourite;
    }

    public Optional<Binding> bound() {
        return Optional.ofNullable(Bound);
    }

    public boolean isBoundDoor(Level World, BlockPos Lower) {
        return Bound != null && Bound.Door().equals(Lower) && Bound.Dimension().equals(World.dimension());
    }

    public void present(UUID Id) {
        Active = true;
        Away = false;
        Current = Id;
        setDirty();
    }

    public void leave(long When) {
        Away = true;
        ReturnAt = When;
        setDirty();
    }

    public void delay(long When) {
        ReturnAt = When;
        setDirty();
    }

    public void lose() {
        Current = null;
        setDirty();
    }

    public void favour(@Nullable UUID Player) {
        if (!Objects.equals(Favourite, Player)) {
            Favourite = Player;
            setDirty();
        }
    }

    public void died(UUID Id) {
        if (!Id.equals(Current)) {
            return;
        }
        Active = false;
        Away = false;
        Current = null;
        Bound = null;
        setDirty();
    }

    public void bind(ResourceKey<Level> Dimension, BlockPos Door, int CellSide) {
        Bound = new Binding(Dimension, Door.immutable(), CellSide);
        setDirty();
    }

    public void unbind() {
        if (Bound != null) {
            Bound = null;
            setDirty();
        }
    }

    public void reset() {
        Unlocked.clear();
        Active = false;
        Away = false;
        ReturnAt = 0L;
        Current = null;
        Favourite = null;
        Bound = null;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag Readers = new ListTag();
        for (UUID Reader : Unlocked) {
            Readers.add(NbtUtils.createUUID(Reader));
        }
        Saved.put("Unlocked", Readers);
        Saved.putBoolean("Active", Active);
        Saved.putBoolean("Away", Away);
        Saved.putLong("ReturnAt", ReturnAt);
        if (Current != null) {
            Saved.putUUID("Current", Current);
        }
        if (Favourite != null) {
            Saved.putUUID("Favourite", Favourite);
        }
        if (Bound != null) {
            CompoundTag Door = new CompoundTag();
            Door.putString("Dimension", Bound.Dimension().location().toString());
            Door.put("Pos", NbtUtils.writeBlockPos(Bound.Door()));
            Door.putInt("CellSide", Bound.CellSide());
            Saved.put("Bound", Door);
        }
        return Saved;
    }

    public static Scp303State load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp303State Data = new Scp303State();
        ListTag Readers = Saved.getList("Unlocked", Tag.TAG_INT_ARRAY);
        for (int Index = 0; Index < Readers.size(); Index++) {
            Data.Unlocked.add(NbtUtils.loadUUID(Readers.get(Index)));
        }
        Data.Active = Saved.getBoolean("Active");
        Data.Away = Saved.getBoolean("Away");
        Data.ReturnAt = Saved.getLong("ReturnAt");
        Data.Current = Saved.hasUUID("Current") ? Saved.getUUID("Current") : null;
        Data.Favourite = Saved.hasUUID("Favourite") ? Saved.getUUID("Favourite") : null;
        if (Saved.contains("Bound")) {
            CompoundTag Door = Saved.getCompound("Bound");
            ResourceLocation Where = ResourceLocation.tryParse(Door.getString("Dimension"));
            Optional<BlockPos> Pos = NbtUtils.readBlockPos(Door, "Pos");
            if (Where != null && Pos.isPresent()) {
                Data.Bound = new Binding(ResourceKey.create(Registries.DIMENSION, Where), Pos.get(), Door.getInt("CellSide") >= 0 ? 1 : -1);
            }
        }
        return Data;
    }
}
