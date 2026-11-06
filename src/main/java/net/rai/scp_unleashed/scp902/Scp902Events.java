package net.rai.scp_unleashed.scp902;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp902.client.Scp902ClientHooks;
import net.rai.scp_unleashed.scp902.network.Scp902ScenePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp902Events {
    private Scp902Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp902ScenePayload.PayloadType, Scp902ScenePayload.PayloadCodec, (Payload, Context) -> Scp902ClientHooks.scene(Payload));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp902Registry.Copy.get(), Scp902Copy.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp902Registry.BoxItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Decoration.getKey()) {
            Event.accept(Scp902Registry.TrenchMudItem.get());
            for (DeferredItem<BlockItem> Entry : Scp902Registry.PropItems) {
                Event.accept(Entry.get());
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        Scp902Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp902Cleanup.forget(Event.getServer());
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        Scp902Trip.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp902Box.heldBeat(User, User.level().getGameTime());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp902Trip.loggedIn(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !Event.isEndConquered()) {
            Scp902Trip.home(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void joining(EntityJoinLevelEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getEntity() instanceof Scp902Copy Copy && !Scp902State.get(World.getServer()).wants(Copy.owner(), Copy.getUUID())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        if (User.isSecondaryUseActive() && Event.getLevel().getBlockState(Event.getPos()).getBlock() instanceof Scp902BoxBlock Box && !Box.scenery()) {
            Event.setUseBlock(TriState.TRUE);
        }
    }
}
