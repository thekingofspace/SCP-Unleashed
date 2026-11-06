package net.rai.scp_unleashed.food;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class FoodSounds {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> Coin = register("food_vending_coin");
    public static final DeferredHolder<SoundEvent, SoundEvent> Change = register("food_vending_change");
    public static final DeferredHolder<SoundEvent, SoundEvent> Beep = register("food_vending_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Error = register("food_vending_error");
    public static final DeferredHolder<SoundEvent, SoundEvent> Motor = register("food_vending_motor");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clunk = register("food_vending_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> CanOpen = register("food_can_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CanOpenSnap = register("food_can_open_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CanOpenFizz = register("food_can_open_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> BottleOpen = register("food_bottle_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> BottleOpenPop = register("food_bottle_open_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BottleOpenFizz = register("food_bottle_open_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> BagOpen = register("food_bag_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> BagOpenPop = register("food_bag_open_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gulp = register("food_drink_gulp");

    private FoodSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
