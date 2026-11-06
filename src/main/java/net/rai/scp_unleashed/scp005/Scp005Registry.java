package net.rai.scp_unleashed.scp005;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
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

public final class Scp005Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HoldTime>> HoldSetting = DataComponentRegister.registerComponentType("scp005_hold",
            Builder -> Builder.persistent(HoldTime.TimeCodec).networkSynchronized(HoldTime.TimeStreamCodec));

    public static final DeferredItem<Scp005KeyItem> Key = ItemRegister.register("scp005_key",
            () -> new Scp005KeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> KeyTurn = sound("scp005_key_turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeyClick = sound("scp005_key_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeyUnlatch = sound("scp005_key_unlatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeyShimmer = sound("scp005_key_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeyRelock = sound("scp005_key_relock");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeyCycle = sound("scp005_key_cycle");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Shimmer = ParticleRegister.register("scp005_shimmer", () -> new SimpleParticleType(false));

    private Scp005Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }
}
