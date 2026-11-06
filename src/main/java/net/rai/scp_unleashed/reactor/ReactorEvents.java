package net.rai.scp_unleashed.reactor;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.reactor.client.ReactorClientHooks;
import net.rai.scp_unleashed.reactor.network.ReactorSnapshotPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ReactorEvents {
    private ReactorEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Piece, Side) -> FuelTowerBlock.itemHandlerAt(World, Pos), ReactorRegistry.FuelTower.get());
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Piece, Side) -> SteamTowerBlock.waterHandlerAt(World, Pos, State, Side),
                ReactorRegistry.CoolantTower.get(), ReactorRegistry.SteamChannel.get());
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Piece, Side) -> IrradiationChamberBlock.itemHandlerAt(World, Pos), ReactorRegistry.Chamber.get());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(ReactorSnapshotPayload.PayloadType, ReactorSnapshotPayload.PayloadCodec, (Payload, Context) -> ReactorClientHooks.snapshot(Payload));
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(ReactorRegistry.ControllerItem.get());
            Event.accept(ReactorRegistry.FuelTowerItem.get());
            Event.accept(ReactorRegistry.ControlRodTowerItem.get());
            Event.accept(ReactorRegistry.BoosterTowerItem.get());
            Event.accept(ReactorRegistry.CoolantTowerItem.get());
            Event.accept(ReactorRegistry.SteamChannelItem.get());
            Event.accept(ReactorRegistry.ChamberItem.get());
        }
    }

    @SubscribeEvent
    public static void tickLevel(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            ReactorGridManager.tickLevel(World);
        }
    }

    @SubscribeEvent
    public static void saveLevel(LevelEvent.Save Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            ReactorGridManager.saveLevel(World);
        }
    }

    @SubscribeEvent
    public static void unloadLevel(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            ReactorGridManager.forget(World);
        }
    }

    @SubscribeEvent
    public static void unloadChunk(ChunkEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            ReactorGridManager.chunkUnloading(World, Event.getChunk().getPos());
        }
    }
}
