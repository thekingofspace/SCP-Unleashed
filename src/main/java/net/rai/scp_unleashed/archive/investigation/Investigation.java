package net.rai.scp_unleashed.archive.investigation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class Investigation {
    private final UUID Id;
    private final ResourceLocation Document;
    private final long Started;
    private final CompoundTag State;
    private List<Component> Notes;
    private long Recheck;
    private boolean Dirty;
    private final List<UUID> Former = new ArrayList<>();

    public Investigation(ResourceLocation Document, long Started) {
        this(UUID.randomUUID(), Document, Started, new CompoundTag(), List.of());
    }

    private Investigation(UUID Id, ResourceLocation Document, long Started, CompoundTag State, List<Component> Notes) {
        this.Id = Id;
        this.Document = Document;
        this.Started = Started;
        this.State = State;
        this.Notes = Notes;
    }

    public UUID getId() {
        return Id;
    }

    public ResourceLocation getDocument() {
        return Document;
    }

    public long getStarted() {
        return Started;
    }

    public CompoundTag getState() {
        return State;
    }

    public List<Component> getNotes() {
        return Notes;
    }

    public void setNotes(List<Component> Lines) {
        Notes = List.copyOf(Lines);
        Dirty = true;
    }

    public long getRecheck() {
        return Recheck;
    }

    public void deferUntil(long GameTime) {
        Recheck = GameTime;
        Dirty = true;
    }

    public void markDirty() {
        Dirty = true;
    }

    public List<UUID> getFormer() {
        return List.copyOf(Former);
    }

    public void addFormer(UUID Reader) {
        if (!Former.contains(Reader)) {
            Former.add(Reader);
            Dirty = true;
        }
    }

    public void dropFormer(UUID Reader) {
        if (Former.remove(Reader)) {
            Dirty = true;
        }
    }

    public boolean consumeDirty() {
        boolean Was = Dirty;
        Dirty = false;
        return Was;
    }

    public CompoundTag save(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putUUID("Id", Id);
        Tag.putString("Document", Document.toString());
        Tag.putLong("Started", Started);
        Tag.put("State", State.copy());
        if (Recheck > 0L) {
            Tag.putLong("Recheck", Recheck);
        }
        ComponentSerialization.CODEC.listOf().encodeStart(Registries.createSerializationContext(NbtOps.INSTANCE), Notes)
                .result().ifPresent(Encoded -> Tag.put("Notes", Encoded));
        if (!Former.isEmpty()) {
            ListTag Earlier = new ListTag();
            for (UUID Reader : Former) {
                Earlier.add(NbtUtils.createUUID(Reader));
            }
            Tag.put("Former", Earlier);
        }
        return Tag;
    }

    public static Optional<Investigation> load(CompoundTag Tag, HolderLookup.Provider Registries) {
        ResourceLocation Document = ResourceLocation.tryParse(Tag.getString("Document"));
        if (Document == null) {
            return Optional.empty();
        }
        List<Component> Notes = List.of();
        if (Tag.contains("Notes")) {
            Notes = ComponentSerialization.CODEC.listOf().parse(Registries.createSerializationContext(NbtOps.INSTANCE), Tag.get("Notes")).result().orElse(List.of());
        }
        UUID Id = Tag.hasUUID("Id") ? Tag.getUUID("Id") : UUID.randomUUID();
        Investigation Case = new Investigation(Id, Document, Tag.getLong("Started"), Tag.getCompound("State").copy(), Notes);
        Case.Recheck = Tag.getLong("Recheck");
        ListTag Earlier = Tag.getList("Former", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
        for (int Index = 0; Index < Earlier.size(); Index++) {
            Case.Former.add(NbtUtils.loadUUID(Earlier.get(Index)));
        }
        return Optional.of(Case);
    }
}
