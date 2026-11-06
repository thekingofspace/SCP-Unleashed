package net.rai.scp_unleashed.stranger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;

import java.util.List;
import java.util.Optional;

public record StrangerEncounterCondition(String Stranger, int MinDelay, int MaxDelay, int MinDistance, int MaxDistance, int Linger,
                                         Optional<String> Dialogue, Optional<Integer> Pages, List<StrangerGift> Gift, List<String> Notes) implements StartCondition {
    public static final String TypeName = "stranger_encounter";
    public static final MapCodec<StrangerEncounterCondition> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.STRING.optionalFieldOf("stranger", StrangerVariants.FoundationId).forGetter(StrangerEncounterCondition::Stranger),
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 60).forGetter(StrangerEncounterCondition::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 300).forGetter(StrangerEncounterCondition::MaxDelay),
            Codec.intRange(6, 64).optionalFieldOf("min_distance", 12).forGetter(StrangerEncounterCondition::MinDistance),
            Codec.intRange(6, 64).optionalFieldOf("max_distance", 24).forGetter(StrangerEncounterCondition::MaxDistance),
            Codec.intRange(20, 3600).optionalFieldOf("linger", 180).forGetter(StrangerEncounterCondition::Linger),
            Codec.STRING.optionalFieldOf("dialogue").forGetter(StrangerEncounterCondition::Dialogue),
            Codec.intRange(1, 16).optionalFieldOf("pages").forGetter(StrangerEncounterCondition::Pages),
            StrangerGift.GiftCodec.listOf().optionalFieldOf("gift", List.of()).forGetter(StrangerEncounterCondition::Gift),
            Codec.STRING.listOf().optionalFieldOf("notes", List.of()).forGetter(StrangerEncounterCondition::Notes)
    ).apply(Instance, StrangerEncounterCondition::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public boolean managesReaders() {
        return true;
    }

    public StrangerPlan plan() {
        StrangerVariant Variant = StrangerVariants.get(Stranger);
        return new StrangerPlan(Variant.Id(), Dialogue.orElse(Variant.Dialogue()), Pages.orElse(Variant.Pages()), Gift.isEmpty() ? Variant.Gifts() : Gift,
                MinDelay * 20, MaxDelay * 20, MinDistance, MaxDistance, Linger * 20);
    }

    public static void ensureSpawner(ResourceLocation Document) {
        if (AnomalySpawners.spawnerFor(Document).isEmpty()) {
            AnomalySpawners.register(Document, StrangerEncounters::confirm);
        }
    }

    private void scheduleFor(ServerPlayer Reader, Investigation Case) {
        StrangerEncounters.get(Reader.server).schedule(plan(), Case.getId(), Reader.getUUID(), Case.getDocument(), Reader.getRandom());
        Case.getState().putBoolean("Scheduled", true);
        Case.markDirty();
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        ensureSpawner(Case.getDocument());
        scheduleFor(Reader, Case);
        if (Notes.isEmpty()) {
            return List.of(Component.translatable("archive.scp_unleashed.notes.stranger_encounter"), Component.translatable("archive.scp_unleashed.notes.stranger_encounter.advice"));
        }
        return Notes.stream().<Component>map(Component::translatable).toList();
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        ensureSpawner(Case.getDocument());
        Optional<StrangerEncounters.Encounter> Entry = StrangerEncounters.get(Reader.server).forCase(Case.getId());
        if (Entry.isEmpty()) {
            CompoundTag State = Case.getState();
            if (!State.getBoolean("Rescheduled")) {
                State.putBoolean("Rescheduled", true);
                scheduleFor(Reader, Case);
            }
            return Optional.empty();
        }
        if (Entry.get().isDelivered()) {
            return Optional.of(new Finding(Entry.get().getDeliveredAt(), false));
        }
        return Optional.empty();
    }
}
