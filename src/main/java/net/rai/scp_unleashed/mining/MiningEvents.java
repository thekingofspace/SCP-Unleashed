package net.rai.scp_unleashed.mining;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.mining.shop.ShopCatalog;
import net.rai.scp_unleashed.mining.shop.ShopService;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class MiningEvents {
    private MiningEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Miner, Side) -> PointMinerBlock.capabilityAt(World, Pos, State, Side),
                MiningRegistry.PointMiner.get());
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(MiningRegistry.PointMinerItem.get());
            Event.accept(MiningRegistry.PointBankItem.get());
        }
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent Event) {
        Event.addListener(new ShopCatalog.Loader(Event.getRegistryAccess()));
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        ShopCatalog.clear();
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            MiningGrid.forget(World);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        ShopService.forget(Event.getEntity().getUUID());
    }
}
