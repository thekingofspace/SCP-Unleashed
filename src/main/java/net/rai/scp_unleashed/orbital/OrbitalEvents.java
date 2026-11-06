package net.rai.scp_unleashed.orbital;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.orbital.network.OrbitalStatusPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.function.Consumer;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class OrbitalEvents {
    private static Consumer<OrbitalStatusPayload> ClientStatus = Payload -> {
    };

    private OrbitalEvents() {
    }

    public static void setClientStatus(Consumer<OrbitalStatusPayload> Handler) {
        ClientStatus = Handler;
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            for (OrbitalPart Part : OrbitalPart.values()) {
                Event.accept(OrbitalRegistry.part(Part));
            }
            Event.accept(OrbitalRegistry.Designator.get());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1").optional();
        Registrar.playToClient(OrbitalStatusPayload.PayloadType, OrbitalStatusPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> ClientStatus.accept(Payload)));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        OrbitalCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Reader) {
            OrbitalLaunch.sync(Reader);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Reader) {
            OrbitalLaunch.sync(Reader);
        }
    }
}
