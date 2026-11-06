package net.rai.scp_unleashed.archive.database;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.client.ArchiveClientHooks;
import net.rai.scp_unleashed.archive.network.DatabaseCopyPayload;
import net.rai.scp_unleashed.archive.network.DatabaseSyncPayload;
import net.rai.scp_unleashed.archive.network.OpenDatabasePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class DatabaseEvents {
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(DatabaseSyncPayload.PayloadType, DatabaseSyncPayload.PayloadCodec,
                (Payload, Context) -> ArchiveClientHooks.acceptDatabase(Payload.Records()));
        Registrar.playToClient(OpenDatabasePayload.PayloadType, OpenDatabasePayload.PayloadCodec,
                (Payload, Context) -> ArchiveClientHooks.openDatabase(Payload.Pos(), Payload.Main()));
        Registrar.playToServer(DatabaseCopyPayload.PayloadType, DatabaseCopyPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                DatabaseActions.printCopy(User, Payload.Pos(), Payload.Document());
            }
        });
    }

    @SubscribeEvent
    public static void useFolderOnDatabase(PlayerInteractEvent.RightClickBlock Event) {
        if (!DatabaseActions.isFolder(Event.getItemStack())) {
            return;
        }
        BlockState State = Event.getLevel().getBlockState(Event.getPos());
        if (!DatabaseActions.isUnit(State)) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.sidedSuccess(Event.getLevel().isClientSide));
        if (Event.getEntity() instanceof ServerPlayer User) {
            boolean Main = State.getBlock() instanceof DatabaseBlock;
            BlockPos Pos = Main ? DatabaseBlock.lowerPos(State, Event.getPos()) : Event.getPos();
            DatabaseActions.useFolder(User, Event.getHand(), Pos, Main);
        }
    }

    @SubscribeEvent
    public static void syncOnLogin(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            DatabaseActions.sync(User);
        }
    }

    @SubscribeEvent
    public static void forgetOnLogout(PlayerEvent.PlayerLoggedOutEvent Event) {
        DatabaseActions.forget(Event.getEntity().getUUID());
    }
}
