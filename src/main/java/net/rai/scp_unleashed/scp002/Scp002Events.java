package net.rai.scp_unleashed.scp002;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp002.client.Scp002ClientHooks;
import net.rai.scp_unleashed.scp002.network.ConsumePayload;
import net.rai.scp_unleashed.scp002.network.ImpactPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp002Events {
    private Scp002Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(ImpactPayload.PayloadType, ImpactPayload.PayloadCodec, (Payload, Context) -> Scp002ClientHooks.impact(Payload));
        Registrar.playToClient(ConsumePayload.PayloadType, ConsumePayload.PayloadCodec, (Payload, Context) -> Scp002ClientHooks.consume(Payload));
    }

    @SubscribeEvent
    public static void tickStrikes(ServerTickEvent.Post Event) {
        Scp002Strikes.get(Event.getServer()).tick(Event.getServer());
    }

    @SubscribeEvent
    public static void forgetRooms(ServerStoppedEvent Event) {
        Scp002Rooms.forgetAll();
    }

    @SubscribeEvent
    public static void blockChorus(EntityTeleportEvent.ChorusFruit Event) {
        if (Scp002Rooms.isInside(Event.getEntityLiving(), Event.getEntityLiving().level().getGameTime())) {
            Event.setCanceled(true);
            if (Event.getEntityLiving() instanceof ServerPlayer User) {
                User.displayClientMessage(Component.translatable("scp002.scp_unleashed.consume.no_escape").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp002Registry.Specimen.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp002Registry.ShellItem.get());
        }
    }
}
