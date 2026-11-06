package net.rai.scp_unleashed.fluiddump;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fluiddump.client.PollutionClientHooks;
import net.rai.scp_unleashed.fluiddump.network.PollutionSyncPayload;
import net.rai.scp_unleashed.plumbing.PlumbingRegistry;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class PollutionEvents {
    private PollutionEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Machine, Side) -> FluidDumpBlock.capabilityAt(World, Pos),
                FluidDumpRegistry.FluidDump.get());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(PollutionSyncPayload.PayloadType, PollutionSyncPayload.PayloadCodec, (Payload, Context) -> PollutionClientHooks.receive(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Machines.getKey()) {
            return;
        }
        ItemStack After = new ItemStack(PlumbingRegistry.DrainItem.get());
        ItemStack Dump = new ItemStack(FluidDumpRegistry.FluidDumpItem.get());
        if (Event.getParentEntries().contains(After) && Event.getSearchEntries().contains(After)) {
            Event.insertAfter(After, Dump, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else {
            Event.accept(Dump);
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        PollutionCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        PollutionConversion.tick(World);
        if (World.getGameTime() % PollutionMath.Step == 0) {
            PollutionField.get(World).tick(World);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        sync(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        sync(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        sync(Event.getEntity());
    }

    private static void sync(Player Joined) {
        if (Joined instanceof ServerPlayer Viewer) {
            PollutionField.get(Viewer.serverLevel()).sendTo(Viewer);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        PollutionConversion.forget();
    }
}
