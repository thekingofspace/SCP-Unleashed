package net.rai.scp_unleashed.scp178;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp178.network.Scp178ReportPayload;
import net.rai.scp_unleashed.scp178.network.Scp178WatcherPayload;

import java.util.function.Consumer;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp178Events {
    public static Consumer<Scp178WatcherPayload> ClientHandler = Payload -> {
    };

    private Scp178Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp178Registry.Watcher.get(), Scp178Watcher.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp178Registry.Glasses.get());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp178WatcherPayload.PayloadType, Scp178WatcherPayload.PayloadCodec, (Payload, Context) -> ClientHandler.accept(Payload));
        Registrar.playToServer(Scp178ReportPayload.PayloadType, Scp178ReportPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp178Watchers.report(User, Payload);
            }
        });
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp178Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp178Watchers.serverTick(Event.getServer());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp178Watchers.forget(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        Scp178Watchers.clear(Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Body = Event.getEntity();
        if (!Body.level().isClientSide && Scp178Watchers.haunt(Body.getUUID()) != null) {
            Scp178Watchers.forget(Body);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp178Watchers.reset();
    }

    @SubscribeEvent
    public static void equipped(LivingEquipmentChangeEvent Event) {
        if (Event.getSlot() == EquipmentSlot.HEAD && Event.getTo().getItem() instanceof Scp178GlassesItem) {
            Scp178Watchers.noteWearer(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void sleep(CanPlayerSleepEvent Event) {
        if (Event.getProblem() == null && Scp178Watchers.restless(Event.getEntity())) {
            Event.setProblem(Player.BedSleepingProblem.NOT_SAFE);
        }
    }
}
