package net.rai.scp_unleashed.antimemetic;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class AntiMemeticRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MobEffect> MobEffectRegister = DeferredRegister.create(Registries.MOB_EFFECT, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PillsLeft = DataComponentRegister.registerComponentType("anti_memetic_pills_left",
            Builder -> Builder.persistent(Codec.intRange(0, AntiMemeticPillsItem.Doses)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredItem<AntiMemeticPillsItem> Pills = ItemRegister.register("anti_memetic_pills",
            () -> new AntiMemeticPillsItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<MobEffect, AntiMemeticEffect> Shielded = MobEffectRegister.register("anti_memetic", AntiMemeticEffect::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("anti_memetic_pills_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RattleTick = sound("anti_memetic_pills_rattle_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cap = sound("anti_memetic_pills_cap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swallow = sound("anti_memetic_pills_swallow");

    private AntiMemeticRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        MobEffectRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
