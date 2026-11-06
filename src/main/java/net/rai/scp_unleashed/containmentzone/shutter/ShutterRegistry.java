package net.rai.scp_unleashed.containmentzone.shutter;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containmentzone.ContainmentZoneRegistry;

public final class ShutterRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShutterBlockEntity>> Shutter = BlockEntityTypeRegister.register("lockdown_shutter",
            () -> BlockEntityType.Builder.of(ShutterBlockEntity::new, ContainmentZoneRegistry.LockdownShutter.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Roll = sound("lockdown_shutter_roll");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gear = sound("lockdown_shutter_gear");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("lockdown_shutter_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clunk = sound("lockdown_shutter_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thud = sound("lockdown_shutter_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Warn = sound("lockdown_shutter_warn");

    private ShutterRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
