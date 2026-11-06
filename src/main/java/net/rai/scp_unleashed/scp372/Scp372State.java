package net.rai.scp_unleashed.scp372;

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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Scp372State extends SavedData {
    public static final String DataName = "scp_unleashed_scp372";
    private static final SavedData.Factory<Scp372State> DataFactory = new SavedData.Factory<>(Scp372State::new, Scp372State::load);

    private final Set<UUID> Unlocked = new LinkedHashSet<>();
    @Nullable
    private Hold Held;
    private long ResumeAt;
    private int Holds;
    private int Escapes;
    @Nullable
    private Spot Charging;

    public record Hold(ResourceKey<Level> Dimension, BlockPos Anchor, UUID Creature) {
    }

    public record Spot(ResourceKey<Level> Dimension, BlockPos Anchor) {
    }

    public static Scp372State get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean isUnlocked(UUID Reader) {
        return Unlocked.contains(Reader);
    }

    public int unlockedCount() {
        return Unlocked.size();
    }

    public boolean unlock(UUID Reader) {
        if (Unlocked.add(Reader)) {
            setDirty();
            return true;
        }
        return false;
    }

    public Optional<Hold> held() {
        return Optional.ofNullable(Held);
    }

    public boolean isHeld() {
        return Held != null;
    }

    public boolean isHeldBy(UUID Creature) {
        return Held != null && Held.Creature().equals(Creature);
    }

    public boolean isHeldAt(Level World, BlockPos Anchor) {
        return Held != null && Held.Anchor().equals(Anchor) && Held.Dimension().equals(World.dimension());
    }

    public long resumeAt() {
        return ResumeAt;
    }

    public int holds() {
        return Holds;
    }

    public int escapes() {
        return Escapes;
    }

    public boolean isCharging() {
        return Charging != null;
    }

    public boolean isChargingAt(Level World, BlockPos Anchor) {
        return Charging != null && Charging.Anchor().equals(Anchor) && Charging.Dimension().equals(World.dimension());
    }

    public boolean beginCharging(Level World, BlockPos Anchor) {
        if (Held != null || Charging != null && !isChargingAt(World, Anchor)) {
            return false;
        }
        Charging = new Spot(World.dimension(), Anchor.immutable());
        return true;
    }

    public void endCharging(Level World, BlockPos Anchor) {
        if (isChargingAt(World, Anchor)) {
            Charging = null;
        }
    }

    public void hold(Level World, BlockPos Anchor, UUID Creature) {
        Held = new Hold(World.dimension(), Anchor.immutable(), Creature);
        Charging = null;
        Holds++;
        setDirty();
    }

    public boolean release(long ResumeTime) {
        if (Held == null) {
            return false;
        }
        Held = null;
        ResumeAt = ResumeTime;
        Escapes++;
        setDirty();
        return true;
    }

    void reset() {
        Unlocked.clear();
        Held = null;
        Charging = null;
        ResumeAt = 0L;
        Holds = 0;
        Escapes = 0;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag Readers = new ListTag();
        for (UUID Reader : Unlocked) {
            Readers.add(NbtUtils.createUUID(Reader));
        }
        Saved.put("Unlocked", Readers);
        Saved.putLong("ResumeAt", ResumeAt);
        Saved.putInt("Holds", Holds);
        Saved.putInt("Escapes", Escapes);
        if (Held != null) {
            CompoundTag Entry = new CompoundTag();
            Entry.putString("Dimension", Held.Dimension().location().toString());
            Entry.put("Anchor", NbtUtils.writeBlockPos(Held.Anchor()));
            Entry.putUUID("Creature", Held.Creature());
            Saved.put("Held", Entry);
        }
        return Saved;
    }

    public static Scp372State load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp372State Data = new Scp372State();
        ListTag Readers = Saved.getList("Unlocked", Tag.TAG_INT_ARRAY);
        for (int Index = 0; Index < Readers.size(); Index++) {
            Data.Unlocked.add(NbtUtils.loadUUID(Readers.get(Index)));
        }
        Data.ResumeAt = Saved.getLong("ResumeAt");
        Data.Holds = Saved.getInt("Holds");
        Data.Escapes = Saved.getInt("Escapes");
        if (Saved.contains("Held")) {
            CompoundTag Entry = Saved.getCompound("Held");
            ResourceLocation Where = ResourceLocation.tryParse(Entry.getString("Dimension"));
            Optional<BlockPos> Anchor = NbtUtils.readBlockPos(Entry, "Anchor");
            if (Where != null && Anchor.isPresent() && Entry.hasUUID("Creature")) {
                Data.Held = new Hold(ResourceKey.create(Registries.DIMENSION, Where), Anchor.get(), Entry.getUUID("Creature"));
            }
        }
        return Data;
    }
}
