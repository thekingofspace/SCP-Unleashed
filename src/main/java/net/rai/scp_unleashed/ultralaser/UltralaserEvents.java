package net.rai.scp_unleashed.ultralaser;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.ultralaser.client.UltralaserClientHooks;
import net.rai.scp_unleashed.ultralaser.network.UltralaserBlastPayload;
import net.rai.scp_unleashed.ultralaser.network.UltralaserPrimingPayload;
import net.rai.scp_unleashed.ultralaser.network.UltralaserTriggerPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class UltralaserEvents {
    private UltralaserEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(UltralaserTriggerPayload.PayloadType, UltralaserTriggerPayload.PayloadCodec, UltralaserTriggerPayload::handle);
        Registrar.playToClient(UltralaserBlastPayload.PayloadType, UltralaserBlastPayload.PayloadCodec, (Payload, Context) -> UltralaserClientHooks.onBlast(Payload));
        Registrar.playToClient(UltralaserPrimingPayload.PayloadType, UltralaserPrimingPayload.PayloadCodec, (Payload, Context) -> UltralaserClientHooks.onPriming(Payload));
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            UltralaserPriming.tick(User);
        }
    }

    @SubscribeEvent
    public static void hurt(LivingDamageEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            UltralaserPriming.interrupt(User, Event.getNewDamage());
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            UltralaserPriming.release(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        UltralaserPriming.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        UltralaserPriming.forgetAll();
    }
}
