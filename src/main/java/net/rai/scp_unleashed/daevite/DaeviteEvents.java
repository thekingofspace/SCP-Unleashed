package net.rai.scp_unleashed.daevite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.client.DaeviteClientHooks;
import net.rai.scp_unleashed.daevite.entity.DaeviteKnight;
import net.rai.scp_unleashed.daevite.entity.DaeviteOverseer;
import net.rai.scp_unleashed.daevite.network.DaeviteSkyPayload;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp140.api.DaeviteGodSlainEvent;
import net.rai.scp_unleashed.scp140.api.Scp140PageEvent;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DaeviteEvents {
    private DaeviteEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(DaeviteRegistry.Knight.get(), DaeviteKnight.createAttributes().build());
        Event.put(DaeviteRegistry.Overseer.get(), DaeviteOverseer.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(DaeviteRegistry.KnightEgg.get());
            Event.accept(DaeviteRegistry.OverseerEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<?> Decor : DaeviteRegistry.DecorItems) {
                Event.accept(Decor.get());
            }
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(DaeviteSkyPayload.PayloadType, DaeviteSkyPayload.PayloadCodec, (Payload, Context) -> DaeviteClientHooks.sky(Payload));
    }

    @SubscribeEvent
    public static void page(Scp140PageEvent Event) {
        if (!Event.isAwakening()) {
            return;
        }
        ServerLevel Book = Event.getLevel();
        MinecraftServer Server = Book.getServer();
        ServerLevel Overworld = Server.overworld();
        BlockPos Origin = DaeviteSites.overworldOrigin(Book, Event.getOrigin());
        FactionState State = FactionControl.state(Server, DaeviteFaction.Instance);
        FactionSite Site = DaeviteFaction.Instance.answerPage(Overworld, State, Event.getPage(), Origin);
        if (Site != null) {
            Event.setSite(Site.dimension(), Site.anchor());
        }
    }

    @SubscribeEvent
    public static void godSlain(DaeviteGodSlainEvent Event) {
        FactionControl.defeat(Event.getLevel().getServer(), DaeviteFaction.Instance, Event.getWhere());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        DaeviteCollapse.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        DaeviteCollapse.forget();
        DaeviteTargets.forget();
    }
}
