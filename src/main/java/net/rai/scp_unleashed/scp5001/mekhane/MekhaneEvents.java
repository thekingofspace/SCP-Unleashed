package net.rai.scp_unleashed.scp5001.mekhane;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyConsoleBlock;
import net.rai.scp_unleashed.scp5001.mekhane.client.MekhaneClientHooks;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneConsolePayload;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneFxPayload;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneSitePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class MekhaneEvents {
    public static final double ConsoleReach = 8.0;

    private MekhaneEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(MekhaneConsolePayload.PayloadType, MekhaneConsolePayload.PayloadCodec, MekhaneEvents::console);
        Registrar.playToClient(MekhaneFxPayload.PayloadType, MekhaneFxPayload.PayloadCodec, (Payload, Context) -> MekhaneClientHooks.fx(Payload));
        Registrar.playToClient(MekhaneSitePayload.PayloadType, MekhaneSitePayload.PayloadCodec, (Payload, Context) -> MekhaneClientHooks.site(Payload));
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Scp5001MekhaneRegistry.IntakeEntity.get(), (Intake, Side) -> Intake.handler());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp5001MekhaneRegistry.ConsoleItem.get());
            Event.accept(Scp5001MekhaneRegistry.IntakeItem.get());
            Event.accept(Scp5001MekhaneRegistry.EmitterItem.get());
        }
    }

    @SubscribeEvent
    public static void starting(ServerStartingEvent Event) {
        MekhaneDirector.serverStarting();
        MekhaneValues.invalidate();
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        MekhaneDirector.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Player) {
            MekhaneDirector.syncTo(Player);
        }
    }

    private static void console(MekhaneConsolePayload Payload, IPayloadContext Context) {
        if (!(Context.player() instanceof ServerPlayer Player) || Payload.Action() != MekhaneConsolePayload.Engage) {
            return;
        }
        ServerLevel World = Player.serverLevel();
        if (World != World.getServer().overworld() || Player.position().distanceToSqr(Payload.Pos().getCenter()) > ConsoleReach * ConsoleReach) {
            return;
        }
        BlockState State = World.getBlockState(Payload.Pos());
        if (!(State.getBlock() instanceof Scp5001EmergencyConsoleBlock) || !State.getValue(Scp5001EmergencyConsoleBlock.Awake)) {
            return;
        }
        if (MekhaneDirector.engage(World)) {
            World.playSound(null, Payload.Pos(), Scp5001MekhaneRegistry.ConsoleEngage.get(), SoundSource.BLOCKS, 1.4f, 0.8f);
            World.playSound(null, Payload.Pos(), Scp5001MekhaneRegistry.Alarm.get(), SoundSource.BLOCKS, 1.0f, 1.2f);
            Player.displayClientMessage(Component.translatable("message.scp_unleashed.scp5001_emergency.engaged").withStyle(ChatFormatting.GOLD), true);
        } else {
            World.playSound(null, Payload.Pos(), Scp5001MekhaneRegistry.ConsoleDeny.get(), SoundSource.BLOCKS, 0.8f, 0.7f);
        }
    }
}
