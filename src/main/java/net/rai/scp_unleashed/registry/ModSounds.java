package net.rai.scp_unleashed.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> WireAttach = SoundEventRegister.register("wire_attach",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "wire_attach")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WireConnect = SoundEventRegister.register("wire_connect",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "wire_connect")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WireDisconnect = SoundEventRegister.register("wire_disconnect",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "wire_disconnect")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WireSnap = SoundEventRegister.register("wire_snap",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "wire_snap")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WireCutterSnip = SoundEventRegister.register("wire_cutter_snip",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "wire_cutter_snip")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PanelPress = SoundEventRegister.register("panel_press",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "panel_press")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PanelRelease = SoundEventRegister.register("panel_release",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "panel_release")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PanelSelect = SoundEventRegister.register("panel_select",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "panel_select")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PanelTick = SoundEventRegister.register("panel_tick",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "panel_tick")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakerTrip = SoundEventRegister.register("breaker_trip",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "breaker_trip")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakerReset = SoundEventRegister.register("breaker_reset",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "breaker_reset")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CutoffOpen = SoundEventRegister.register("cutoff_open",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "cutoff_open")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CutoffClose = SoundEventRegister.register("cutoff_close",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "cutoff_close")));
}
