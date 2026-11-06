package net.rai.scp_unleashed.integration.voicechat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.voice.VoiceArchive;

import java.util.UUID;

@ForgeVoicechatPlugin
public class ScpVoicechatPlugin implements VoicechatPlugin {
    private static volatile boolean Initialized;
    private static volatile boolean Registered;

    @Override
    public String getPluginId() {
        return Scp_unleashed.ModId;
    }

    @Override
    public void initialize(VoicechatApi Api) {
        Initialized = true;
    }

    @Override
    public void registerEvents(EventRegistration Registration) {
        Registration.registerEvent(MicrophonePacketEvent.class, ScpVoicechatPlugin::microphone);
        Registration.registerEvent(EntitySoundPacketEvent.class, ScpVoicechatPlugin::heard);
        Registration.registerEvent(LocationalSoundPacketEvent.class, ScpVoicechatPlugin::heard);
        Registration.registerEvent(StaticSoundPacketEvent.class, ScpVoicechatPlugin::heard);
        Registration.registerEvent(VoicechatServerStartedEvent.class, ScpVoicechatPlugin::started);
        Registration.registerEvent(VoicechatServerStoppedEvent.class, ScpVoicechatPlugin::stopped);
        Registration.registerEvent(PlayerDisconnectedEvent.class, ScpVoicechatPlugin::disconnected);
        Registered = true;
    }

    public static boolean isInitialized() {
        return Initialized;
    }

    public static boolean isRegistered() {
        return Registered;
    }

    public static void microphone(MicrophonePacketEvent Event) {
        VoicechatConnection Sender = Event.getSenderConnection();
        if (Sender == null || Sender.getPlayer() == null) {
            return;
        }
        Object Raw = Sender.getPlayer().getPlayer();
        UUID Speaker = speaker(Raw);
        if (Speaker != null && VoiceGate.isSilenced(Speaker)) {
            Event.cancel();
            return;
        }
        if (Speaker == null || Event.getPacket() == null) {
            return;
        }
        SvcRadio.relay(Event, Speaker);
        if (Raw instanceof ServerPlayer User) {
            SvcCapture.capture(Event, Sender, User);
        }
    }

    public static void heard(SoundPacketEvent<?> Event) {
        VoicechatConnection Receiver = Event.getReceiverConnection();
        if (Receiver == null || Receiver.getPlayer() == null) {
            return;
        }
        UUID Listener = speaker(Receiver.getPlayer().getPlayer());
        if (Listener != null && VoiceGate.isDeafened(Listener)) {
            Event.cancel();
        }
    }

    public static void started(VoicechatServerStartedEvent Event) {
        VoicechatServerApi Api = Event.getVoicechat();
        SvcRadio.start(Api);
        VoiceArchive.setPlayback(new SvcLurePlayback(Api));
    }

    public static void stopped(VoicechatServerStoppedEvent Event) {
        VoiceArchive.setPlayback(null);
        SvcRadio.stop();
    }

    public static void disconnected(PlayerDisconnectedEvent Event) {
        UUID Speaker = Event.getPlayerUuid();
        SvcRadio.forget(Speaker);
    }

    private static UUID speaker(Object Raw) {
        return Raw instanceof Entity Body ? Body.getUUID() : null;
    }
}
