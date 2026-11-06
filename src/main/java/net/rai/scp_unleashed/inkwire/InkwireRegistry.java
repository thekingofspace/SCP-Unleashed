package net.rai.scp_unleashed.inkwire;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class InkwireRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredItem<InvisibleInkItem> InvisibleInk = ItemRegister.register("invisible_ink",
            () -> new InvisibleInkItem(new Item.Properties().durability(InvisibleInkItem.Uses)));

    public static final DeferredHolder<SoundEvent, SoundEvent> InkDab = sound("invisible_ink_dab");
    public static final DeferredHolder<SoundEvent, SoundEvent> InkWipe = sound("invisible_ink_wipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> InkShimmer = sound("invisible_ink_shimmer");

    private InkwireRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
