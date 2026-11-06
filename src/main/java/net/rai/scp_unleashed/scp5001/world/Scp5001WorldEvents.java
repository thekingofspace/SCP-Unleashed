package net.rai.scp_unleashed.scp5001.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp5001.terminal.Scp5001TerminalPayload;
import net.rai.scp_unleashed.scp5001.world.client.Scp5001WorldClientHooks;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp5001WorldEvents {
    private Scp5001WorldEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp5001TerminalPayload.PayloadType, Scp5001TerminalPayload.PayloadCodec, (Payload, Context) -> Scp5001WorldClientHooks.terminal(Payload));
    }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent Event) {
        Event.addListener(new Scp5001Palettes.Loader());
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load Event) {
        if (Event.getLevel() instanceof ServerLevel World && World.dimension() == Level.OVERWORLD) {
            Scp5001Emitter.chunkLoaded(World, Event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp5001Emitter.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        Scp5001Emitter.clear();
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
        Scp5001Sites Site = Scp5001Sites.get(World.getServer());
        if (Site.inside(Event.getEntity().blockPosition())) {
            Event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<BlockItem> Entry : Scp5001WorldRegistry.TabItems) {
                Event.accept(Entry.get());
            }
        }
    }
}
