package net.rai.scp_unleashed.security;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SecurityEvents {
    private SecurityEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(TurretConfigPayload.PayloadType, TurretConfigPayload.PayloadCodec, TurretConfigPayload::handle);
        Registrar.playToServer(DetectorConfigPayload.PayloadType, DetectorConfigPayload.PayloadCodec, DetectorConfigPayload::handle);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SecurityRegistry.SentryTurretEntity.get(), (Turret, Side) -> Turret.getAutomation());
    }

    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(SecurityRegistry.SentryTurretItem.get());
            Event.accept(SecurityRegistry.MotionDetectorItem.get());
        }
    }
}
