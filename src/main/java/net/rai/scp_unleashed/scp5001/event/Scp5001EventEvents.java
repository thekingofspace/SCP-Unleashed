package net.rai.scp_unleashed.scp5001.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp5001.event.client.Scp5001ClientHooks;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Mimic;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001ActionPayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001BurstPayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001ExposurePayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001OpenPayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001StatePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp5001EventEvents {
    private Scp5001EventEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp5001EventRegistry.MimicType.get(), Scp5001Mimic.attributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp5001StatePayload.PayloadType, Scp5001StatePayload.PayloadCodec, (Payload, Context) -> Scp5001ClientHooks.state(Payload));
        Registrar.playToClient(Scp5001OpenPayload.PayloadType, Scp5001OpenPayload.PayloadCodec, (Payload, Context) -> Scp5001ClientHooks.open(Payload));
        Registrar.playToClient(Scp5001ExposurePayload.PayloadType, Scp5001ExposurePayload.PayloadCodec, (Payload, Context) -> Scp5001ClientHooks.exposure(Payload));
        Registrar.playToClient(Scp5001BurstPayload.PayloadType, Scp5001BurstPayload.PayloadCodec, (Payload, Context) -> Scp5001ClientHooks.burst(Payload));
        Registrar.playToServer(Scp5001ActionPayload.PayloadType, Scp5001ActionPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp5001Event.action(User, Payload);
            }
        });
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp5001Event.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp5001Event.forgetCache();
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        Scp5001EventCommands.register(Event);
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        LivingEntity Victim = Event.getEntity();
        if (!(Victim.level() instanceof ServerLevel World)) {
            return;
        }
        Scp5001Event.forgetSwarm(World.getServer(), Victim.getUUID());
        if (Event.getSource().is(Scp5001EventRegistry.Resonance)) {
            Scp5001Mimic.rise(World, Victim);
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        Entity Leaving = Event.getEntity();
        if (Leaving.level() instanceof ServerLevel World && Leaving.getRemovalReason() != null && Leaving.getRemovalReason().shouldDestroy()) {
            Scp5001Event.forgetSwarm(World.getServer(), Leaving.getUUID());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp5001Event.sendState(User);
            Scp5001Exposure.sync(User, false);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp5001Exposure.sync(User, false);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp5001Event.sendState(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp5001EventRegistry.ChamberCoreItem.get());
            Event.accept(Scp5001EventRegistry.ConsoleMainItem.get());
            Event.accept(Scp5001EventRegistry.GeneratorItem.get());
            Event.accept(Scp5001EventRegistry.GeneratorConsoleItem.get());
            Event.accept(Scp5001EventRegistry.RodEmitterItem.get());
            Event.accept(Scp5001EventRegistry.LaserEmitterItem.get());
        }
    }
}
