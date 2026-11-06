package net.rai.scp_unleashed.sealedcase;

import net.minecraft.core.component.DataComponentType;
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

public final class SealedCaseRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SealedContents>> Contents = DataComponentRegister.registerComponentType("sealed_case_contents",
            Builder -> Builder.persistent(SealedContents.ContentsCodec).networkSynchronized(SealedContents.ContentsStreamCodec));

    public static final DeferredItem<SealedCaseItem> SealedCase = ItemRegister.register("sealed_case",
            () -> new SealedCaseItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Unlatch = sound("sealed_case_unlatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hinge = sound("sealed_case_hinge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Release = sound("sealed_case_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> Seal = sound("sealed_case_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clamp = sound("sealed_case_clamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Deny = sound("sealed_case_deny");

    private SealedCaseRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
