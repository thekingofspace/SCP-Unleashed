package net.rai.scp_unleashed.scp001scarlet.king;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp001scarlet.king.client.ScarletKingClientHooks;
import net.rai.scp_unleashed.scp001scarlet.king.network.ScarletAnswerPayload;
import net.rai.scp_unleashed.scp001scarlet.king.network.ScarletConfirmPayload;
import net.rai.scp_unleashed.scp001scarlet.king.network.ScarletRiteFxPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ScarletKingEvents {
    private ScarletKingEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(ScarletKingRegistry.KingType.get(), ScarletKing.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(ScarletConfirmPayload.PayloadType, ScarletConfirmPayload.PayloadCodec, (Payload, Context) -> ScarletKingClientHooks.confirm(Payload));
        Registrar.playToClient(ScarletRiteFxPayload.PayloadType, ScarletRiteFxPayload.PayloadCodec, (Payload, Context) -> ScarletKingClientHooks.riteFx(Payload));
        Registrar.playToServer(ScarletAnswerPayload.PayloadType, ScarletAnswerPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer Speaker) {
                ScarletRite.answer(Speaker, Payload.Nonce(), Payload.Yes());
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void chat(ServerChatEvent Event) {
        ServerPlayer Speaker = Event.getPlayer();
        String Raw = Event.getRawText();
        if (ScarletPhrase.matches(Raw)) {
            Speaker.server.execute(() -> ScarletRite.heard(Speaker, Raw));
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            ScarletRite.tick(World);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        ScarletRite.forget();
        ScarletKingBar.forget();
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Fallen) {
            ScarletKingBar.drop(Fallen);
        }
    }

    @SubscribeEvent
    public static void left(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Leaving) {
            ScarletKingBar.drop(Leaving);
        }
    }

    @SubscribeEvent
    public static void travelled(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Leaving) {
            ScarletKingBar.drop(Leaving);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(ScarletKingRegistry.Helmet.get());
            Event.accept(ScarletKingRegistry.Chestplate.get());
            Event.accept(ScarletKingRegistry.Leggings.get());
            Event.accept(ScarletKingRegistry.Boots.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(ScarletKingRegistry.PodItem.get());
            Event.accept(ScarletKingRegistry.RemainsItem.get());
        }
    }
}
