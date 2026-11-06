package net.rai.scp_unleashed.rocketry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformBlock;
import net.rai.scp_unleashed.rocketry.platform.SatellitePlatformBlockEntity;
import net.rai.scp_unleashed.rocketry.rocket.DropPodEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketDestinations;
import net.rai.scp_unleashed.rocketry.rocket.RocketEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketryDestinationsPayload;
import net.rai.scp_unleashed.rocketry.rocket.RocketryInputPayload;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RocketryEvents {
    private RocketryEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        for (LaunchPlatformBlock Platform : List.of(RocketryRegistry.RocketPlatform.get(), RocketryRegistry.SatellitePlatform.get())) {
            Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Tile, Side) -> Platform.fluidCapability(World, Pos, State, Side), Platform);
        }
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Tile, Side) ->
                LaunchPlatformBlock.masterEntity(World, Pos, State) instanceof SatellitePlatformBlockEntity Platform ? Platform.getItemHandler() : null,
                RocketryRegistry.SatellitePlatform.get());
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(RocketryRegistry.RocketPlatformItem.get());
            Event.accept(RocketryRegistry.Rocket.get());
            Event.accept(RocketryRegistry.PortableLaunchPadItem.get());
            Event.accept(RocketryRegistry.SatellitePlatformItem.get());
            Event.accept(RocketryRegistry.Satellite.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Resources.getKey()) {
            Event.accept(RocketryRegistry.RocketEngine.get());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(RocketryDestinationsPayload.PayloadType, RocketryDestinationsPayload.PayloadCodec,
                (Payload, Context) -> Context.enqueueWork(() -> RocketDestinations.setClient(Payload.Entries())));
        Registrar.playToServer(RocketryInputPayload.PayloadType, RocketryInputPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (!(Context.player() instanceof ServerPlayer User)) {
                return;
            }
            Entity Vehicle = User.getVehicle();
            if (Vehicle instanceof RocketEntity Rocket) {
                Rocket.setThrottle(Payload.Thrust(), User);
            } else if (Vehicle instanceof DropPodEntity Pod) {
                Pod.setThrottle(Payload.Thrust());
            }
        }));
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent Event) {
        Event.addListener(new RocketDestinations.Loader(Event.getRegistryAccess()));
    }

    @SubscribeEvent
    public static void syncDestinations(OnDatapackSyncEvent Event) {
        RocketryDestinationsPayload Payload = new RocketryDestinationsPayload(RocketDestinations.snapshot(Event.getPlayerList().getServer()));
        Event.getRelevantPlayers().filter(Reader -> Reader.connection != null && Reader.connection.hasChannel(RocketryDestinationsPayload.PayloadType))
                .forEach(Reader -> PacketDistributor.sendToPlayer(Reader, Payload));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        RocketryCommands.register(Event.getDispatcher());
    }

    public static boolean isSheltered(Entity Rider) {
        Entity Vehicle = Rider.getVehicle();
        return Vehicle instanceof RocketEntity || Vehicle instanceof DropPodEntity;
    }

    public static boolean isEnvironmental(DamageSource Source) {
        return Source.is(DamageTypeTags.IS_FALL) || Source.is(DamageTypeTags.IS_FIRE) || Source.is(DamageTypeTags.IS_DROWNING) || Source.is(DamageTypeTags.IS_FREEZING)
                || Source.is(DamageTypes.IN_WALL) || Source.is(DamageTypes.CRAMMING) || Source.is(DamageTypes.FLY_INTO_WALL) || Source.is(DamageTypes.HOT_FLOOR);
    }

    @SubscribeEvent
    public static void shelterRiders(LivingIncomingDamageEvent Event) {
        if (isSheltered(Event.getEntity()) && isEnvironmental(Event.getSource())) {
            Event.setCanceled(true);
        }
    }
}
