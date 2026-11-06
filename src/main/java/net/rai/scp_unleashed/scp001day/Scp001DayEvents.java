package net.rai.scp_unleashed.scp001day;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFlesh;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayForm;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp001day.client.Scp001DayClientHooks;
import net.rai.scp_unleashed.scp001day.network.Scp001DayTurningPayload;
import net.rai.scp_unleashed.scp001day.network.Scp001DaySunlightDebugPayload;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayExposure;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayShade;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp001DayEvents {
    private Scp001DayEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp001DayTurningPayload.PayloadType, Scp001DayTurningPayload.PayloadCodec, (Payload, Context) -> Scp001DayClientHooks.turning(Payload));
        Registrar.playToClient(Scp001DaySunlightDebugPayload.PayloadType, Scp001DaySunlightDebugPayload.PayloadCodec, (Payload, Context) -> Scp001DayClientHooks.sunlightDebug());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp001DayRegistry.SunHat.get());
            Event.accept(Scp001DayRegistry.SunWraps.get());
            Event.accept(Scp001DayRegistry.SunLegWraps.get());
            Event.accept(Scp001DayRegistry.SunBoots.get());
            Event.accept(Scp001DayRegistry.CrawlerEgg.get());
            Event.accept(Scp001DayRegistry.ClingerEgg.get());
            Event.accept(Scp001DayRegistry.LurkerEgg.get());
            Event.accept(Scp001DayRegistry.MassEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            Event.accept(new ItemStack(Scp001DayRegistry.Tin.get()));
        }
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp001DayRegistry.CrawlerType.get(), Scp001DayFlesh.createAttributes(Scp001DayForm.Crawler).build());
        Event.put(Scp001DayRegistry.ClingerType.get(), Scp001DayFlesh.createAttributes(Scp001DayForm.Clinger).build());
        Event.put(Scp001DayRegistry.LurkerType.get(), Scp001DayFlesh.createAttributes(Scp001DayForm.Lurker).build());
        Event.put(Scp001DayRegistry.MassType.get(), Scp001DayFlesh.createAttributes(Scp001DayForm.Mass).build());
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp001DayTurning.died(User, Event.getSource());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        ServerLevel World = Scp001DayWorld.level(Server);
        if (World == null || World.players().isEmpty()) {
            return;
        }
        tickDimension(World);
    }

    public static void tickDimension(ServerLevel World) {
        tickDimension(World, World.getGameTime());
    }

    public static void tickDimension(ServerLevel World, long Now) {
        for (ServerPlayer User : World.players()) {
            Scp001DaySunlight.tick(World, User, Now);
            Scp001DayHunger.tick(World, User);
            if (Now % Scp001DayVoice.CaptureEvery == 0 && !User.isSpectator()) {
                Scp001DayVoice.capture(User);
            }
        }
        if (Scp001DayWorld.strict(World)) {
            Scp001DayRifts.tick(World);
            Scp001DayFleshSpawner.tick(World);
            Scp001DayLamps.tick(World, Now);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Event.getFrom() == Scp001DayWorld.key()) {
            left(User.getUUID());
            Scp001DaySunlight.forget(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        left(Event.getEntity().getUUID());
        Scp001DaySunlight.forget(Event.getEntity());
    }

    private static void left(UUID Player) {
        Scp001DayHunger.forget(Player);
        Scp001DayVoice.stop(Player);
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent Event) {
        Scp001DayShade.reset();
        Scp001DayExposure.forget();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp001DaySunlight.clear();
        Scp001DayExposure.forget();
        Scp001DayPlan.forget();
    }
}
