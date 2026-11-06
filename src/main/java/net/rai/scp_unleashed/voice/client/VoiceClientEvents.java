package net.rai.scp_unleashed.voice.client;

import net.rai.scp_unleashed.config.ScpConfigs;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.voice.network.VoicePreferencePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class VoiceClientEvents {
    private static Boolean LastSent;

    private VoiceClientEvents() {
    }

    private static void send() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.getConnection() == null || !Client.getConnection().hasChannel(VoicePreferencePayload.PayloadType)) {
            return;
        }
        boolean Allow = ScpClientConfig.allowVoiceRecording();
        PacketDistributor.sendToServer(new VoicePreferencePayload(Allow));
        LastSent = Allow;
    }

    @SubscribeEvent
    public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn Event) {
        LastSent = null;
        send();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        LastSent = null;
    }

    @SubscribeEvent
    public static void configChanged(ModConfigEvent.Reloading Event) {
        if (Event.getConfig().getSpec() == ScpConfigs.Client && (LastSent == null || LastSent != ScpClientConfig.allowVoiceRecording())) {
            send();
        }
    }
}
