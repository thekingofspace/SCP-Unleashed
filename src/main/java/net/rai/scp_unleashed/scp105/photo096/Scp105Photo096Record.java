package net.rai.scp_unleashed.scp105.photo096;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Scp105Photo096Record extends SavedData {
    public static final String DataName = "scp_unleashed_scp105_photo096";
    private static final SavedData.Factory<Scp105Photo096Record> DataFactory = new SavedData.Factory<>(Scp105Photo096Record::new, Scp105Photo096Record::load);

    private final Set<UUID> Unlocked = new LinkedHashSet<>();
    private boolean Given;
    @Nullable
    private UUID Recipient;
    private String RecipientName = "";
    private long GivenAt;

    public static Scp105Photo096Record get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean isGiven() {
        return Given;
    }

    public Optional<UUID> recipient() {
        return Optional.ofNullable(Recipient);
    }

    public String recipientName() {
        return RecipientName;
    }

    public long givenAt() {
        return GivenAt;
    }

    public boolean isUnlocked(UUID Reader) {
        return Unlocked.contains(Reader);
    }

    public Set<UUID> unlocked() {
        return Collections.unmodifiableSet(Unlocked);
    }

    public boolean unlock(UUID Reader) {
        if (Unlocked.add(Reader)) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean give(UUID Receiver, String Name, long When) {
        if (Given) {
            return false;
        }
        Given = true;
        Recipient = Receiver;
        RecipientName = Name;
        GivenAt = When;
        setDirty();
        return true;
    }

    void reset() {
        Unlocked.clear();
        Given = false;
        Recipient = null;
        RecipientName = "";
        GivenAt = 0L;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Registries) {
        Saved.putBoolean("Given", Given);
        if (Recipient != null) {
            Saved.putUUID("Recipient", Recipient);
        }
        Saved.putString("RecipientName", RecipientName);
        Saved.putLong("GivenAt", GivenAt);
        ListTag Readers = new ListTag();
        for (UUID Reader : Unlocked) {
            Readers.add(NbtUtils.createUUID(Reader));
        }
        Saved.put("Unlocked", Readers);
        return Saved;
    }

    public static Scp105Photo096Record load(CompoundTag Saved, HolderLookup.Provider Registries) {
        Scp105Photo096Record Data = new Scp105Photo096Record();
        Data.Given = Saved.getBoolean("Given");
        Data.Recipient = Saved.hasUUID("Recipient") ? Saved.getUUID("Recipient") : null;
        Data.RecipientName = Saved.getString("RecipientName");
        Data.GivenAt = Saved.getLong("GivenAt");
        ListTag Readers = Saved.getList("Unlocked", Tag.TAG_INT_ARRAY);
        for (int Index = 0; Index < Readers.size(); Index++) {
            Data.Unlocked.add(NbtUtils.loadUUID(Readers.get(Index)));
        }
        return Data;
    }
}
