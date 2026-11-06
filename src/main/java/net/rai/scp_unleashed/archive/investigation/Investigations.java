package net.rai.scp_unleashed.archive.investigation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.archive.document.DocumentClaims;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Investigations extends SavedData {
    public static final String DataName = "scp_unleashed_archive_investigations";
    private static final SavedData.Factory<Investigations> DataFactory = new SavedData.Factory<>(Investigations::new, Investigations::load);

    private final Map<UUID, List<Investigation>> Cases = new HashMap<>();
    private final List<Stamp> Unstamped = new ArrayList<>();
    private int Opened = -1;

    public static Investigations get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public List<Investigation> of(UUID Reader) {
        List<Investigation> Found = Cases.get(Reader);
        return Found == null ? List.of() : List.copyOf(Found);
    }

    public Set<UUID> readers() {
        return Set.copyOf(Cases.keySet());
    }

    public int count(UUID Reader) {
        List<Investigation> Found = Cases.get(Reader);
        return Found == null ? 0 : Found.size();
    }

    public void deferStamp(UUID Reader, ResourceLocation Document, UUID CaseId) {
        Unstamped.add(new Stamp(Reader, Document, CaseId));
        setDirty();
    }

    public List<Stamp> takeStamps(UUID Reader) {
        List<Stamp> Due = new ArrayList<>();
        for (Stamp Entry : List.copyOf(Unstamped)) {
            if (Entry.Reader().equals(Reader)) {
                Due.add(Entry);
                Unstamped.remove(Entry);
            }
        }
        if (!Due.isEmpty()) {
            setDirty();
        }
        return Due;
    }

    public Optional<Investigation> find(UUID Reader, ResourceLocation Document) {
        for (Investigation Case : of(Reader)) {
            if (Case.getDocument().equals(Document)) {
                return Optional.of(Case);
            }
        }
        return Optional.empty();
    }

    public int opened(MinecraftServer Server) {
        if (Opened < 0) {
            int Open = 0;
            for (List<Investigation> Each : Cases.values()) {
                Open += Each.size();
            }
            Opened = Math.max(Open, DocumentClaims.get(Server).total());
            setDirty();
        }
        return Opened;
    }

    public void noteOpened(MinecraftServer Server) {
        Opened = opened(Server) + 1;
        setDirty();
    }

    public void setOpened(int Count) {
        Opened = Math.max(0, Count);
        setDirty();
    }

    public void add(UUID Reader, Investigation Case) {
        Cases.computeIfAbsent(Reader, Key -> new ArrayList<>()).add(Case);
        setDirty();
    }

    public void remove(UUID Reader, Investigation Case) {
        List<Investigation> Found = Cases.get(Reader);
        if (Found != null && Found.remove(Case)) {
            if (Found.isEmpty()) {
                Cases.remove(Reader);
            }
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Readers = new ListTag();
        for (Map.Entry<UUID, List<Investigation>> Group : Cases.entrySet()) {
            CompoundTag Reader = new CompoundTag();
            Reader.putUUID("Reader", Group.getKey());
            ListTag Open = new ListTag();
            for (Investigation Case : Group.getValue()) {
                Open.add(Case.save(Registries));
            }
            Reader.put("Cases", Open);
            Readers.add(Reader);
        }
        Tag.put("Readers", Readers);
        ListTag Stamps = new ListTag();
        for (Stamp Entry : Unstamped) {
            CompoundTag Saved = new CompoundTag();
            Saved.putUUID("Reader", Entry.Reader());
            Saved.putString("Document", Entry.Document().toString());
            Saved.putUUID("Case", Entry.CaseId());
            Stamps.add(Saved);
        }
        Tag.put("Unstamped", Stamps);
        if (Opened >= 0) {
            Tag.putInt("Opened", Opened);
        }
        return Tag;
    }

    private static Investigations load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Investigations Data = new Investigations();
        Data.Opened = Tag.contains("Opened") ? Tag.getInt("Opened") : -1;
        ListTag Readers = Tag.getList("Readers", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Readers.size(); Index++) {
            CompoundTag Reader = Readers.getCompound(Index);
            if (!Reader.hasUUID("Reader")) {
                continue;
            }
            List<Investigation> Open = new ArrayList<>();
            ListTag Saved = Reader.getList("Cases", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int Slot = 0; Slot < Saved.size(); Slot++) {
                Investigation.load(Saved.getCompound(Slot), Registries).ifPresent(Open::add);
            }
            if (!Open.isEmpty()) {
                Data.Cases.put(Reader.getUUID("Reader"), Open);
            }
        }
        ListTag Stamps = Tag.getList("Unstamped", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Stamps.size(); Index++) {
            CompoundTag Saved = Stamps.getCompound(Index);
            ResourceLocation Document = ResourceLocation.tryParse(Saved.getString("Document"));
            if (Saved.hasUUID("Reader") && Saved.hasUUID("Case") && Document != null) {
                Data.Unstamped.add(new Stamp(Saved.getUUID("Reader"), Document, Saved.getUUID("Case")));
            }
        }
        return Data;
    }

    public record Stamp(UUID Reader, ResourceLocation Document, UUID CaseId) {
    }
}
