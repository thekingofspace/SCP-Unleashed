package net.rai.scp_unleashed.scp140;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp140.client.Scp140ClientHooks;
import net.rai.scp_unleashed.scp140.network.Scp140ChroniclePayload;
import net.rai.scp_unleashed.scp140.network.Scp140ClosedPayload;
import net.rai.scp_unleashed.scp140.network.Scp140SeenPayload;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp140Events {
    private Scp140Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp140ChroniclePayload.PayloadType, Scp140ChroniclePayload.PayloadCodec, (Payload, Context) -> Scp140ClientHooks.chronicle(Payload));
        Registrar.playToServer(Scp140SeenPayload.PayloadType, Scp140SeenPayload.PayloadCodec, Scp140SeenPayload::handle);
        Registrar.playToServer(Scp140ClosedPayload.PayloadType, Scp140ClosedPayload.PayloadCodec, Scp140ClosedPayload::handle);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp140Registry.BookItem.get());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp140Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent.Post Event) {
        LivingEntity Victim = Event.getEntity();
        if (!(Victim.level() instanceof ServerLevel World) || Event.getNewDamage() <= 0.0f) {
            return;
        }
        Optional<Scp140Witnesses.Witness> Seen = Scp140Witnesses.nearest(World, Victim.position(), Scp140Ink.WitnessRadius);
        if (Seen.isEmpty()) {
            return;
        }
        Scp140Chronicle.get(World.getServer()).addInk(World, Seen.get().Pos(), Scp140Ink.inkFor(Victim, Event.getNewDamage()));
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getEntity() instanceof ItemEntity Dropped && Event.getLevel() instanceof ServerLevel World && Scp140BookItem.is(Dropped.getItem())) {
            Scp140Witnesses.dropped(World, Dropped, true);
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        if (Event.getEntity() instanceof ItemEntity Dropped && Event.getLevel() instanceof ServerLevel World) {
            Scp140Witnesses.dropped(World, Dropped, false);
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || World.getGameTime() % Scp140Ink.WhisperInterval != 0L) {
            return;
        }
        Scp140Chronicle Chronicle = Scp140Chronicle.get(World.getServer());
        if (World == World.getServer().overworld()) {
            Scp140Witnesses.refreshPlaced(World.getServer());
        }
        if (Chronicle.isComplete() || Chronicle.progress() < Scp140Ink.WhisperFraction) {
            return;
        }
        for (Scp140Witnesses.Witness Book : Scp140Witnesses.all(World)) {
            if (World.random.nextInt(Scp140Ink.WhisperChance) == 0) {
                Scp140Effects.whisper(World, Book.Centre());
            }
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp140Reading.welcome(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp140Reading.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp140Witnesses.clear();
        Scp140Reading.clear();
    }
}
