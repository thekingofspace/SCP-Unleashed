package net.rai.scp_unleashed.radio;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.radio.client.RadioClientHooks;
import net.rai.scp_unleashed.radio.network.RadioGlitchPayload;
import net.rai.scp_unleashed.radio.network.RadioTunePayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RadioEvents {
    private RadioEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(RadioGlitchPayload.PayloadType, RadioGlitchPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> RadioClientHooks.glitch(Payload)));
        Registrar.playToServer(RadioTunePayload.PayloadType, RadioTunePayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                ItemStack Stack = User.getItemInHand(InteractionHand.MAIN_HAND);
                if (RadioCarry.isRadio(Stack) && Payload.Step() != 0) {
                    HandheldRadioItem.tune(User, Stack, Payload.Step());
                }
            }
        }));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(RadioRegistry.Radio.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(RadioRegistry.TowerItem.get());
            Event.accept(RadioRegistry.MastItem.get());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        RadioTransmit.tick(Event.getServer());
        RadioBleed.tick(Event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void chat(ServerChatEvent Event) {
        RadioChat.chat(Event);
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        RadioTransmit.forget(Event.getEntity().getUUID());
        RadioBleed.forgetPlayer(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        RadioTransmit.clear();
        RadioBleed.clear();
    }
}
