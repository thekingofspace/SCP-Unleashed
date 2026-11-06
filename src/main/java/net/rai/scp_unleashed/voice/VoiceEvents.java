package net.rai.scp_unleashed.voice;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.voice.network.VoicePreferencePayload;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class VoiceEvents {
    public static final String NoticeKey = "voice.scp_unleashed.notice";

    private VoiceEvents() {
    }

    @SubscribeEvent
    public static void aboutToStart(ServerAboutToStartEvent Event) {
        VoiceArchive.open(Event.getServer());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        VoiceArchive.close();
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        VoiceArchive.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1").optional();
        Registrar.playToServer(VoicePreferencePayload.PayloadType, VoicePreferencePayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                preference(User, Payload);
            }
        }));
    }

    public static void preference(ServerPlayer User, VoicePreferencePayload Payload) {
        VoiceArchive.setAllowed(User.getUUID(), Payload.Allow());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        VoiceCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            notice(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        VoiceArchive.stopAll(Event.getEntity().getUUID());
    }

    public static boolean notice(ServerPlayer User) {
        if (!VoicePolicy.master()) {
            return false;
        }
        User.sendSystemMessage(Component.translatable(NoticeKey, Component.literal("/" + Scp_unleashed.ModId + " voice forgetme").withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return true;
    }
}
