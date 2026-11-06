package net.rai.scp_unleashed.scp372;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp372.network.Scp372GlimpsePayload;
import net.rai.scp_unleashed.scp372.network.Scp372HushPayload;
import net.rai.scp_unleashed.scp372.network.Scp372PulsePayload;
import net.rai.scp_unleashed.scp372.network.Scp372UnlockPayload;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerRegistry;

import java.util.function.Consumer;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp372Events {
    public static Consumer<Scp372GlimpsePayload> GlimpseHandler = Payload -> {
    };
    public static Consumer<Scp372PulsePayload> PulseHandler = Payload -> {
    };
    public static Consumer<Scp372HushPayload> HushHandler = Payload -> {
    };

    private Scp372Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp372Registry.Insect.get(), Scp372Insect.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp372Registry.InsectEgg.get());
        }
        if (Event.getTabKey() != ModCreativeTabs.ScpBlocks.getKey()) {
            return;
        }
        ItemStack Anchor = new ItemStack(Scp372Registry.AnchorItem.get());
        ItemStack Blocker = new ItemStack(SemanticBlockerRegistry.BlockerItem.get());
        if (Event.getParentEntries().contains(Blocker) && Event.getSearchEntries().contains(Blocker)) {
            Event.insertAfter(Blocker, Anchor, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else {
            Event.accept(Anchor);
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp372UnlockPayload.PayloadType, Scp372UnlockPayload.PayloadCodec,
                (Payload, Context) -> Context.enqueueWork(() -> Scp372Unlock.setClientUnlocked(Payload.Unlocked())));
        Registrar.playToClient(Scp372GlimpsePayload.PayloadType, Scp372GlimpsePayload.PayloadCodec,
                (Payload, Context) -> Context.enqueueWork(() -> GlimpseHandler.accept(Payload)));
        Registrar.playToClient(Scp372PulsePayload.PayloadType, Scp372PulsePayload.PayloadCodec,
                (Payload, Context) -> Context.enqueueWork(() -> PulseHandler.accept(Payload)));
        Registrar.playToClient(Scp372HushPayload.PayloadType, Scp372HushPayload.PayloadCodec,
                (Payload, Context) -> Context.enqueueWork(() -> HushHandler.accept(Payload)));
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp372Glimpses.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && User.tickCount % Scp372Unlock.SweepInterval == 0) {
            Scp372Unlock.sweep(User);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp372Unlock.sweep(User);
            Scp372Unlock.sync(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp372Glimpses.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp372Glimpses.clear();
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (!(Event.getEntity() instanceof Scp372Insect Creature) || !(Event.getLevel() instanceof ServerLevel World) || !Event.loadedFromDisk()) {
            return;
        }
        if (Creature.anchor() != null && !Scp372State.get(World.getServer()).isHeldBy(Creature.getUUID())) {
            Event.setCanceled(true);
        }
    }
}
