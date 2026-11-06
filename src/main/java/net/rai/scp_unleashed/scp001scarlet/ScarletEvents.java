package net.rai.scp_unleashed.scp001scarlet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp001scarlet.client.ScarletClientHooks;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletCultist;
import net.rai.scp_unleashed.scp001scarlet.network.ScarletSyncPayload;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneEmitter;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneRise;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneSite;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ScarletEvents {
    private ScarletEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp001ScarletRegistry.CultistType.get(), ScarletCultist.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(ScarletSyncPayload.PayloadType, ScarletSyncPayload.PayloadCodec, (Payload, Context) -> ScarletClientHooks.sync(Payload));
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        ScarletMeter.tick(Server);
        ScarletXk.tick(Server);
        ScarletThroneEmitter.tick(Server);
        ScarletThroneRise.tick(Server);
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load Event) {
        if (Event.getLevel() instanceof ServerLevel World && World.dimension() == Level.OVERWORLD) {
            ScarletThroneEmitter.chunkLoaded(World, Event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            ScarletMeter.sync(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            ScarletMeter.sync(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            ScarletMeter.sync(User);
        }
    }

    @SubscribeEvent
    public static void readCopy(PlayerInteractEvent.RightClickItem Event) {
        if (!(Event.getEntity() instanceof ServerPlayer Reader)) {
            return;
        }
        ItemStack Stack = Event.getItemStack();
        if (!Stack.is(ArchiveRegistry.OpenedDocument.get())) {
            return;
        }
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        if (Stamp != null && Scp001ScarletRegistry.DocumentId.equals(Stamp.Document()) && ScarletState.get(Reader.server).read()) {
            ScarletMeter.see(Reader);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        ScarletThroneEmitter.clear();
        ScarletThroneRise.forget();
        ScarletMeter.forget();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void spawning(FinalizeSpawnEvent Event) {
        MobSpawnType Type = Event.getSpawnType();
        if (Type != MobSpawnType.NATURAL && Type != MobSpawnType.CHUNK_GENERATION && Type != MobSpawnType.PATROL) {
            return;
        }
        if (!(Event.getLevel().getLevel() instanceof ServerLevel World) || World.dimension() != Level.OVERWORLD) {
            return;
        }
        ScarletThroneSite Site = ScarletThroneSite.get(World.getServer());
        if (!Site.planned()) {
            return;
        }
        BlockPos Centre = Site.centre();
        if (ScarletThrone.inArena(Centre, Event.getEntity().blockPosition())) {
            Event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp001ScarletRegistry.Effigy.get());
            Event.accept(Scp001ScarletRegistry.CultistEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp001ScarletRegistry.BrazierItem.get());
            Event.accept(Scp001ScarletRegistry.GlyphStoneItem.get());
        }
    }
}
