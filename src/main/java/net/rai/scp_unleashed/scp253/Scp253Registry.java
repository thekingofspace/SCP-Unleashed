package net.rai.scp_unleashed.scp253;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp253Registry {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_253");
    public static final ResourceLocation SlownessId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp253_slowness");
    public static final ResourceKey<DamageType> OvergrowthDamage = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp253_overgrowth"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp253State>> Infection = AttachmentRegister.register("scp253_infection",
            () -> AttachmentType.builder(() -> Scp253State.Fresh).serialize(Scp253State.StateCodec).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp253Display>> Display = AttachmentRegister.register("scp253_display",
            () -> AttachmentType.builder(() -> Scp253Display.None).sync(Scp253Display.DisplayStream).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Growth = sound("scp253_growth");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrowthSoft = sound("scp253_growth_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp253_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreathLow = sound("scp253_breath_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Overgrown = sound("scp253_overgrown");
    public static final DeferredHolder<SoundEvent, SoundEvent> OvergrownLow = sound("scp253_overgrown_low");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrowthLayers = List.of(Growth, GrowthSoft);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreathLayers = List.of(Breath, BreathLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OvergrownLayers = List.of(Overgrown, OvergrownLow);

    private Scp253Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        LabOrganisms.register(new Scp253Organism());
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
