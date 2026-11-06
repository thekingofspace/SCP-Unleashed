package net.rai.scp_unleashed.furnishing;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class FurnishingSounds {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> PaperShuffle = register("furnishing_paper_shuffle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CushionSit = register("furnishing_cushion_sit");
    public static final DeferredHolder<SoundEvent, SoundEvent> LampClick = register("furnishing_lamp_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoffeeBrew = register("furnishing_coffee_brew");
    public static final DeferredHolder<SoundEvent, SoundEvent> MicrowaveStart = register("furnishing_microwave_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MicrowaveHum = register("furnishing_microwave_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> MicrowaveDing = register("furnishing_microwave_ding");
    public static final DeferredHolder<SoundEvent, SoundEvent> FridgeOpen = register("furnishing_fridge_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> FridgeClose = register("furnishing_fridge_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> BottleClink = register("furnishing_bottle_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> MarkerSqueak = register("furnishing_marker_squeak");

    private FurnishingSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
