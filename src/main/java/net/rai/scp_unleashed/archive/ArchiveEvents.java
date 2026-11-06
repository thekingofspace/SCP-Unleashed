package net.rai.scp_unleashed.archive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.client.ArchiveClientHooks;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.investigation.InvestigationTracker;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.archive.network.DocumentSyncPayload;
import net.rai.scp_unleashed.archive.network.OpenDossierPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.Map;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class ArchiveEvents {
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(DocumentSyncPayload.PayloadType, DocumentSyncPayload.PayloadCodec,
                (Payload, Context) -> AnomalyDocuments.acceptClient(Payload.Documents()));
        Registrar.playToClient(OpenDossierPayload.PayloadType, OpenDossierPayload.PayloadCodec,
                (Payload, Context) -> ArchiveClientHooks.openDossier(Payload.Hand(), Payload.Reveal()));
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ArchiveRegistry.ArchiveEntity.get(), (Terminal, Side) -> Terminal.automation());
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent Event) {
        Event.addListener(new AnomalyDocuments.Loader());
    }

    @SubscribeEvent
    public static void forgetDocuments(ServerStoppedEvent Event) {
        AnomalyDocuments.clearServer();
        ReaderTargets.clear();
    }

    @SubscribeEvent
    public static void reassignInvestigations(ServerTickEvent.Post Event) {
        if (Event.getServer().getTickCount() % InvestigationTracker.TickInterval == 0) {
            InvestigationTracker.reassign(Event.getServer());
        }
    }

    @SubscribeEvent
    public static void readerJoined(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Reader) {
            InvestigationTracker.joined(Reader);
        }
    }

    @SubscribeEvent
    public static void readerLeft(PlayerEvent.PlayerLoggedOutEvent Event) {
        ReaderTargets.left(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void syncDocuments(OnDatapackSyncEvent Event) {
        DocumentSyncPayload Payload = new DocumentSyncPayload(AnomalyDocuments.server());
        Event.getRelevantPlayers().forEach(Reader -> PacketDistributor.sendToPlayer(Reader, Payload));
    }

    @SubscribeEvent
    public static void tickInvestigations(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer Reader && Reader.tickCount % InvestigationTracker.TickInterval == 0) {
            InvestigationTracker.tick(Reader);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Anomalies.getKey()) {
            return;
        }
        Event.accept(ArchiveRegistry.ArchiveItem.get());
        Event.accept(ArchiveRegistry.DatabaseItem.get());
        Event.accept(ArchiveRegistry.DatabaseTerminalItem.get());
        for (Map.Entry<ResourceLocation, AnomalyDocument> Listed : AnomalyDocuments.client().entrySet()) {
            Event.accept(SealedDocumentItem.create(Listed.getKey(), Listed.getValue().Classification()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
