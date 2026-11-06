package net.rai.scp_unleashed.scp217;

import com.mojang.serialization.Codec;
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

public final class Scp217Registry {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_217");
    public static final ResourceKey<DamageType> WoundDown = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217_wound_down"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> Clockwork = AttachmentRegister.register("scp217_clockwork",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT, Saved -> Saved >= 0).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp217Trace>> Trace = AttachmentRegister.register("scp217_trace",
            () -> AttachmentType.builder(() -> new Scp217Trace()).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> Perfect = AttachmentRegister.register("scp217_perfect",
            () -> AttachmentType.builder(() -> Boolean.FALSE).serialize(Codec.BOOL, Saved -> Saved).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> PulseReady = AttachmentRegister.register("scp217_pulse_ready",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG, Saved -> Saved > 0L).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Tick = sound("scp217_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> TickClick = sound("scp217_tick_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Click = sound("scp217_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClickLock = sound("scp217_click_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grind = sound("scp217_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrindChain = sound("scp217_grind_chain");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp217_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepChain = sound("scp217_step_chain");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shift = sound("scp217_shift");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShiftPiston = sound("scp217_shift_piston");
    public static final DeferredHolder<SoundEvent, SoundEvent> WindDown = sound("scp217_wind_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> WindDownRatchet = sound("scp217_wind_down_ratchet");
    public static final DeferredHolder<SoundEvent, SoundEvent> WindDownClunk = sound("scp217_wind_down_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pulse = sound("scp217_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> PulseRing = sound("scp217_pulse_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> PulseCatch = sound("scp217_pulse_catch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Culture = sound("scp217_culture");
    public static final DeferredHolder<SoundEvent, SoundEvent> CultureChime = sound("scp217_culture_chime");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TickLayers = List.of(Tick, TickClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ClickLayers = List.of(Click, ClickLock);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrindLayers = List.of(Grind, GrindChain);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepLayers = List.of(Step, StepChain);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShiftLayers = List.of(Shift, ShiftPiston);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WindDownLayers = List.of(WindDown, WindDownRatchet, WindDownClunk);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PulseLayers = List.of(Pulse, PulseRing, PulseCatch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CultureLayers = List.of(Culture, CultureChime);

    private Scp217Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        LabOrganisms.register(new Scp217Organism());
        LabOrganisms.register(new Scp217PerfectOrganism());
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
