package net.rai.scp_unleashed.scp280;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp280World extends SavedData {
    public static final String DataName = "scp_unleashed_scp280";
    public static final float MaxMeter = 100.0f;
    public static final long LooseTimeout = 6000L;
    private static final SavedData.Factory<Scp280World> DataFactory = new SavedData.Factory<>(Scp280World::new, Scp280World::load);

    private boolean Unlocked;
    private final Set<UUID> Readers = new LinkedHashSet<>();
    private final Set<UUID> Witnesses = new LinkedHashSet<>();
    private final Map<UUID, Float> Meters = new HashMap<>();
    private boolean Captured;
    @Nullable
    private UUID Held;
    private long Token;
    @Nullable
    private UUID Loose;
    private long LooseSeen;
    private int Reveals;
    private int Captures;

    public static Scp280World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean isUnlocked() {
        return Unlocked;
    }

    public Set<UUID> readers() {
        return Set.copyOf(Readers);
    }

    public boolean covers(UUID Player) {
        return Unlocked || Readers.contains(Player);
    }

    public void unlock(UUID Reader, boolean WorldWide) {
        if (WorldWide && !Unlocked) {
            Unlocked = true;
            setDirty();
        }
        if (Readers.add(Reader)) {
            setDirty();
        }
    }

    public void lockAll() {
        Unlocked = false;
        Readers.clear();
        Witnesses.clear();
        Meters.clear();
        setDirty();
    }

    public boolean hasWitnessed(UUID Player) {
        return Witnesses.contains(Player);
    }

    public void witness(UUID Player) {
        if (Witnesses.add(Player)) {
            setDirty();
        }
    }

    public float meter(UUID Player) {
        return Meters.getOrDefault(Player, 0.0f);
    }

    public void setMeter(UUID Player, float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, MaxMeter);
        Float Old = Meters.get(Player);
        if (Clamped <= 0.0f) {
            if (Old != null) {
                Meters.remove(Player);
                setDirty();
            }
            return;
        }
        if (Old == null || Math.abs(Old - Clamped) > 1.0e-4f) {
            Meters.put(Player, Clamped);
            setDirty();
        }
    }

    public int meterCount() {
        return Meters.size();
    }

    public void clearMeters() {
        if (!Meters.isEmpty()) {
            Meters.clear();
            setDirty();
        }
    }

    public boolean isCaptured() {
        return Captured;
    }

    @Nullable
    public UUID held() {
        return Held;
    }

    public void capture(UUID Shade) {
        Captured = true;
        Held = Shade;
        Captures++;
        if (Shade.equals(Loose)) {
            Loose = null;
        }
        Meters.clear();
        setDirty();
    }

    public void release(@Nullable UUID Shade) {
        if (!Captured) {
            return;
        }
        if (Shade != null && Held != null && !Held.equals(Shade)) {
            return;
        }
        Captured = false;
        Held = null;
        setDirty();
    }

    public long token() {
        return Token;
    }

    public long nextToken() {
        Token++;
        setDirty();
        return Token;
    }

    public boolean current(long Candidate) {
        return Candidate == Token;
    }

    @Nullable
    public UUID loose() {
        return Loose;
    }

    public boolean looseActive(long Now) {
        return Loose != null && Now - LooseSeen < LooseTimeout;
    }

    public void bindLoose(UUID Shade, long Now) {
        Loose = Shade;
        LooseSeen = Now;
        setDirty();
    }

    public void countReveal() {
        Reveals++;
        setDirty();
    }

    public void seen(UUID Shade, long Now) {
        if (Shade.equals(Loose) && Now - LooseSeen >= 100L) {
            LooseSeen = Now;
            setDirty();
        }
    }

    public void freeLoose(UUID Shade) {
        if (Shade.equals(Loose)) {
            Loose = null;
            setDirty();
        }
    }

    public int reveals() {
        return Reveals;
    }

    public int captures() {
        return Captures;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putBoolean("Unlocked", Unlocked);
        Tag.put("Readers", uuids(Readers));
        Tag.put("Witnesses", uuids(Witnesses));
        ListTag Saved = new ListTag();
        for (Map.Entry<UUID, Float> Entry : Meters.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Player", Entry.getKey());
            Row.putFloat("Meter", Entry.getValue());
            Saved.add(Row);
        }
        Tag.put("Meters", Saved);
        Tag.putBoolean("Captured", Captured);
        if (Held != null) {
            Tag.putUUID("Held", Held);
        }
        Tag.putLong("Token", Token);
        if (Loose != null) {
            Tag.putUUID("Loose", Loose);
            Tag.putLong("LooseSeen", LooseSeen);
        }
        Tag.putInt("Reveals", Reveals);
        Tag.putInt("Captures", Captures);
        return Tag;
    }

    private static ListTag uuids(Set<UUID> Values) {
        ListTag Saved = new ListTag();
        for (UUID Value : Values) {
            Saved.add(NbtUtils.createUUID(Value));
        }
        return Saved;
    }

    private static void readUuids(ListTag Saved, Set<UUID> Into) {
        for (int Index = 0; Index < Saved.size(); Index++) {
            Into.add(NbtUtils.loadUUID(Saved.get(Index)));
        }
    }

    public static Scp280World load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp280World Data = new Scp280World();
        Data.Unlocked = Tag.getBoolean("Unlocked");
        readUuids(Tag.getList("Readers", net.minecraft.nbt.Tag.TAG_INT_ARRAY), Data.Readers);
        readUuids(Tag.getList("Witnesses", net.minecraft.nbt.Tag.TAG_INT_ARRAY), Data.Witnesses);
        ListTag Saved = Tag.getList("Meters", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Row = Saved.getCompound(Index);
            if (Row.hasUUID("Player") && Row.getFloat("Meter") > 0.0f) {
                Data.Meters.put(Row.getUUID("Player"), Mth.clamp(Row.getFloat("Meter"), 0.0f, MaxMeter));
            }
        }
        Data.Captured = Tag.getBoolean("Captured");
        Data.Held = Tag.hasUUID("Held") ? Tag.getUUID("Held") : null;
        Data.Token = Tag.getLong("Token");
        Data.Loose = Tag.hasUUID("Loose") ? Tag.getUUID("Loose") : null;
        Data.LooseSeen = Tag.getLong("LooseSeen");
        Data.Reveals = Tag.getInt("Reveals");
        Data.Captures = Tag.getInt("Captures");
        return Data;
    }
}
