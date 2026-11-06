package net.rai.scp_unleashed.worldend;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.worldend.client.WorldEndClientHooks;
import net.rai.scp_unleashed.worldend.network.WorldEndPayload;

public final class WorldEndRegistry {
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("worldend.heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thump = sound("worldend.thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wet = sound("worldend.wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swell = sound("worldend.swell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Toll = sound("worldend.toll");

    private WorldEndRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        SoundEventRegister.register(ModEventBus);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(WorldEndRules::register));
        ModEventBus.addListener(RegisterPayloadHandlersEvent.class, WorldEndRegistry::payloads);
    }

    private static void payloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(WorldEndPayload.PayloadType, WorldEndPayload.PayloadCodec, (Payload, Context) -> WorldEndClientHooks.receive(Payload));
    }
}
