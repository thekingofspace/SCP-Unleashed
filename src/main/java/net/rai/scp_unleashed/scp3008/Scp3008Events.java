package net.rai.scp_unleashed.scp3008;

import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp3008.client.Scp3008ClientHooks;
import net.rai.scp_unleashed.scp3008.network.Scp3008FadePayload;
import net.rai.scp_unleashed.scp3008.world.Scp3008Catalogue;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008Events {
    private Scp3008Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp3008FadePayload.PayloadType, Scp3008FadePayload.PayloadCodec, (Payload, Context) -> Scp3008ClientHooks.fade(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<? extends Item> Entry : Scp3008Registry.Placeables) {
                Event.accept(Entry.get());
            }
        } else if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            Event.accept(Scp3008Registry.Meatballs.get());
            Event.accept(Scp3008Registry.Fries.get());
            Event.accept(Scp3008Registry.SoftDrink.get());
            Event.accept(Scp3008Registry.Jam.get());
        }
    }

    public static void setClock(MinecraftServer Server, long Value) {
        Scp3008Clock.set(Value);
        Scp3008State.get(Server).setClock(Value);
        ServerLevel Store = Scp3008World.level(Server);
        if (Store != null) {
            boolean Running = Store.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
            ClientboundSetTimePacket Packet = new ClientboundSetTimePacket(Store.getGameTime(), Store.getDayTime(), Running);
            for (ServerPlayer User : Store.players()) {
                User.connection.send(Packet);
            }
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || !Scp3008World.strict(World)) {
            return;
        }
        if (!Scp3008Clock.owns(World.getLevelData())) {
            Scp3008Clock.bind(World);
            Scp3008Clock.set(Scp3008State.get(World.getServer()).clock());
        }
        if (World.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            long Next = Scp3008Clock.time() + 1L;
            Scp3008Clock.set(Next);
            Scp3008State.get(World.getServer()).setClock(Next);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp3008Transit.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void loaded(LevelEvent.Load Event) {
        if (Event.getLevel() instanceof ServerLevel World && Scp3008World.inside(World)) {
            Scp3008Clock.bind(World);
            Scp3008Clock.set(Scp3008State.get(World.getServer()).clock());
        }
    }

    @SubscribeEvent
    public static void unloaded(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World && Scp3008World.inside(World)) {
            Scp3008Clock.bind(null);
        }
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent Event) {
        Scp3008Catalogue.forget();
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp3008Transit.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp3008Transit.clear();
        Scp3008Clock.bind(null);
    }
}
