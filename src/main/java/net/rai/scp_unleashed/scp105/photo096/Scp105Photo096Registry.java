package net.rai.scp_unleashed.scp105.photo096;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class Scp105Photo096Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredItem<Scp105Photo096Item> Photograph = ItemRegister.register("scp105_photograph_096",
            () -> new Scp105Photo096Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("scp105_photograph_096_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sting = sound("scp105_photograph_096_sting");
    public static final DeferredHolder<SoundEvent, SoundEvent> StingLow = sound("scp105_photograph_096_sting_low");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StingLayers = List.of(Sting, StingLow);

    private Scp105Photo096Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
