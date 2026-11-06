package net.rai.scp_unleashed.archive.document;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.archive.ArchiveRules;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DocumentClaims extends SavedData {
    public static final String DataName = "scp_unleashed_archive_claims";
    private static final SavedData.Factory<DocumentClaims> DataFactory = new SavedData.Factory<>(DocumentClaims::new, DocumentClaims::load);

    private final Set<ResourceLocation> Claimed = new LinkedHashSet<>();
    private final Map<UUID, Set<ResourceLocation>> Personal = new LinkedHashMap<>();

    public static DocumentClaims get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean isClaimed(ResourceLocation Document) {
        return Claimed.contains(Document);
    }

    public boolean isClaimedBy(UUID Owner, ResourceLocation Document) {
        Set<ResourceLocation> Owned = Personal.get(Owner);
        return Owned != null && Owned.contains(Document);
    }

    public boolean claim(ResourceLocation Document) {
        if (Claimed.add(Document)) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean release(ResourceLocation Document) {
        if (Claimed.remove(Document)) {
            setDirty();
            return true;
        }
        return false;
    }

    public int total() {
        int Count = Claimed.size();
        for (Set<ResourceLocation> Owned : Personal.values()) {
            Count += Owned.size();
        }
        return Count;
    }

    public Set<ResourceLocation> getClaimed() {
        return Collections.unmodifiableSet(Claimed);
    }

    public Set<ResourceLocation> claimedBy(UUID Owner) {
        Set<ResourceLocation> Owned = Personal.get(Owner);
        return Owned == null ? Set.of() : Collections.unmodifiableSet(Owned);
    }

    public boolean isAvailable(ResourceLocation Id, AnomalyDocument Document) {
        return isAvailable(Id, Document, ArchiveRules.Mode.World, null);
    }

    public boolean isAvailable(ResourceLocation Id, AnomalyDocument Document, ArchiveRules.Mode Rule, @Nullable UUID Owner) {
        if (Document.Spawn() == SpawnRule.Repeatable || Rule == ArchiveRules.Mode.Unlimited) {
            return true;
        }
        if (Rule == ArchiveRules.Mode.PerPlayer) {
            return Owner == null || !isClaimedBy(Owner, Id);
        }
        return !isClaimed(Id);
    }

    public boolean take(ResourceLocation Id, ArchiveRules.Mode Rule, UUID Owner) {
        boolean Fresh = switch (Rule) {
            case World -> !isClaimed(Id);
            case PerPlayer -> !isClaimedBy(Owner, Id);
            case Unlimited -> true;
        };
        if (!Fresh) {
            return false;
        }
        Claimed.add(Id);
        Personal.computeIfAbsent(Owner, Key -> new LinkedHashSet<>()).add(Id);
        setDirty();
        return true;
    }

    public boolean giveBack(ResourceLocation Id, ArchiveRules.Mode Rule, UUID Owner) {
        boolean Mine = false;
        Set<ResourceLocation> Owned = Personal.get(Owner);
        if (Owned != null && Owned.remove(Id)) {
            Mine = true;
            if (Owned.isEmpty()) {
                Personal.remove(Owner);
            }
        }
        boolean Shared = Rule == ArchiveRules.Mode.PerPlayer ? !heldByAnyone(Id) && Claimed.remove(Id) : Claimed.remove(Id);
        if (Mine || Shared) {
            setDirty();
        }
        return switch (Rule) {
            case World -> Shared;
            case PerPlayer -> Mine;
            case Unlimited -> false;
        };
    }

    private boolean heldByAnyone(ResourceLocation Id) {
        for (Set<ResourceLocation> Owned : Personal.values()) {
            if (Owned.contains(Id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.put("Claimed", names(Claimed));
        ListTag Players = new ListTag();
        for (Map.Entry<UUID, Set<ResourceLocation>> Entry : Personal.entrySet()) {
            CompoundTag Record = new CompoundTag();
            Record.putUUID("Player", Entry.getKey());
            Record.put("Claimed", names(Entry.getValue()));
            Players.add(Record);
        }
        Tag.put("Players", Players);
        return Tag;
    }

    private static ListTag names(Set<ResourceLocation> Documents) {
        ListTag Saved = new ListTag();
        for (ResourceLocation Document : Documents) {
            Saved.add(StringTag.valueOf(Document.toString()));
        }
        return Saved;
    }

    private static void readNames(ListTag Saved, Set<ResourceLocation> Into) {
        for (int Index = 0; Index < Saved.size(); Index++) {
            ResourceLocation Document = ResourceLocation.tryParse(Saved.getString(Index));
            if (Document != null) {
                Into.add(Document);
            }
        }
    }

    public static DocumentClaims load(CompoundTag Tag, HolderLookup.Provider Registries) {
        DocumentClaims Data = new DocumentClaims();
        readNames(Tag.getList("Claimed", net.minecraft.nbt.Tag.TAG_STRING), Data.Claimed);
        ListTag Players = Tag.getList("Players", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Players.size(); Index++) {
            CompoundTag Record = Players.getCompound(Index);
            if (!Record.hasUUID("Player")) {
                continue;
            }
            Set<ResourceLocation> Owned = new LinkedHashSet<>();
            readNames(Record.getList("Claimed", net.minecraft.nbt.Tag.TAG_STRING), Owned);
            if (!Owned.isEmpty()) {
                Data.Personal.put(Record.getUUID("Player"), Owned);
            }
        }
        return Data;
    }
}
