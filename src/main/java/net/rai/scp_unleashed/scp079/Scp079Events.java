package net.rai.scp_unleashed.scp079;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp079.client.Scp079ClientHooks;
import net.rai.scp_unleashed.scp079.network.Scp079AskPayload;
import net.rai.scp_unleashed.scp079.network.Scp079LurchPayload;
import net.rai.scp_unleashed.scp079.network.Scp079TerminalPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp079Events {
    private Scp079Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp079TerminalPayload.PayloadType, Scp079TerminalPayload.PayloadCodec, (Payload, Context) -> Scp079ClientHooks.terminal(Payload));
        Registrar.playToClient(Scp079LurchPayload.PayloadType, Scp079LurchPayload.PayloadCodec, (Payload, Context) -> Scp079ClientHooks.lurch(Payload));
        Registrar.playToServer(Scp079AskPayload.PayloadType, Scp079AskPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                ask(User, Payload);
            }
        });
    }

    private static void ask(ServerPlayer User, Scp079AskPayload Payload) {
        ServerLevel World = User.serverLevel();
        if (!World.isLoaded(Payload.Pos())) {
            return;
        }
        BlockState State = World.getBlockState(Payload.Pos());
        Scp079BlockEntity Computer = Scp079Block.masterEntity(World, Payload.Pos(), State);
        if (Computer != null) {
            Computer.ask(User, Scp079Prompt.byIndex(Payload.Prompt()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp079Registry.ComputerItem.get());
        }
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp079Registry.AnomalousUpgrade.get());
        }
    }
}
