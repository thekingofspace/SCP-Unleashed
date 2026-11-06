package net.rai.scp_unleashed.door;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class DoorSounds {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> DoorOpen = register("door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorClose = register("door_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorClunk = register("door_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> HeavyClunk = register("door_heavy_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateMove = register("door_gate_move");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateMotor = register("door_gate_motor");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateGrind = register("door_gate_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateEngage = register("door_gate_engage");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateClank = register("door_gate_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateSlam = register("door_gate_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateClang = register("door_gate_clang");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateBoom = register("door_gate_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateRattle = register("door_gate_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ContainmentMove = register("door_containment_move");
    public static final DeferredHolder<SoundEvent, SoundEvent> ContainmentGrind = register("door_containment_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> ContainmentHiss = register("door_containment_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> ContainmentUnlatch = register("door_containment_unlatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ContainmentSeal = register("door_containment_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> ContainmentChunk = register("door_containment_chunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> ContainmentThump = register("door_containment_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> ContainmentVent = register("door_containment_vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeycardAccept = register("door_keycard_accept");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeycardDeny = register("door_keycard_deny");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeycardSet = register("door_keycard_set");
    public static final DeferredHolder<SoundEvent, SoundEvent> ButtonPress = register("door_button_press");
    public static final DeferredHolder<SoundEvent, SoundEvent> ButtonRelease = register("door_button_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeverRelease = register("door_lever_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeverOn = register("door_lever_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeverOff = register("door_lever_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeverLatch = register("door_lever_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Screwdriver = register("door_screwdriver");

    private DoorSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
