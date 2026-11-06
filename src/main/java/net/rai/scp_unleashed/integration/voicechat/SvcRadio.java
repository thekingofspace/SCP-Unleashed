package net.rai.scp_unleashed.integration.voicechat;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.rai.scp_unleashed.radio.RadioVoice;
import net.rai.scp_unleashed.radio.RadioVoiceFilter;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SvcRadio {
    public static final String CategoryName = "Radio";
    public static final String CategoryDescription = "Voices heard through handheld radios";
    private static final Map<UUID, Codec> Codecs = new ConcurrentHashMap<>();
    @Nullable
    private static volatile VoicechatServerApi Api;

    private SvcRadio() {
    }

    private static final class Codec {
        private final OpusDecoder Decoder;
        private final OpusEncoder Encoder;
        private final RadioVoiceFilter Filter = new RadioVoiceFilter();
        private final UUID Channel;
        private final Random Roll = new Random();

        private Codec(VoicechatServerApi Server, UUID Speaker) {
            Decoder = Server.createDecoder();
            Encoder = Server.createEncoder();
            Channel = UUID.nameUUIDFromBytes(("scp_unleashed:radio:" + Speaker).getBytes(StandardCharsets.UTF_8));
        }

        private synchronized byte[] shape(byte[] Opus) {
            short[] Pcm = Decoder.decode(Opus);
            if (Pcm == null || Pcm.length == 0) {
                return null;
            }
            Filter.process(Pcm);
            return Encoder.encode(Pcm);
        }

        private synchronized void close() {
            Decoder.close();
            Encoder.close();
        }
    }

    public static void start(VoicechatServerApi Server) {
        Api = Server;
        VolumeCategory Category = Server.volumeCategoryBuilder().setId(RadioVoice.Category).setName(CategoryName).setDescription(CategoryDescription).build();
        Server.registerVolumeCategory(Category);
    }

    public static void stop() {
        Api = null;
        for (UUID Speaker : Codecs.keySet()) {
            forget(Speaker);
        }
    }

    public static void forget(UUID Speaker) {
        Codec Old = Codecs.remove(Speaker);
        if (Old != null) {
            Old.close();
        }
    }

    public static void relay(MicrophonePacketEvent Event, UUID Speaker) {
        VoicechatServerApi Server = Api;
        MicrophonePacket Spoken = Event.getPacket();
        if (Server == null || Spoken == null) {
            return;
        }
        RadioVoice.Route Route = RadioVoice.route(Speaker);
        if (Route == null || Route.Targets().isEmpty()) {
            forget(Speaker);
            return;
        }
        byte[] Opus = Spoken.getOpusEncodedData();
        if (Opus == null || Opus.length == 0) {
            return;
        }
        Codec Current = Codecs.computeIfAbsent(Speaker, Key -> new Codec(Server, Key));
        byte[] Shaped = Current.shape(Opus);
        if (Shaped == null) {
            return;
        }
        for (RadioVoice.Target Each : Route.Targets()) {
            if (RadioVoice.drops(Each, Current.Roll.nextFloat())) {
                continue;
            }
            VoicechatConnection Listener = Server.getConnectionOf(Each.Id());
            if (Listener == null || !Listener.isConnected() || Listener.isDisabled()) {
                continue;
            }
            StaticSoundPacket Out = Spoken.staticSoundPacketBuilder().channelId(Current.Channel).opusEncodedData(Shaped).category(RadioVoice.Category).build();
            Server.sendStaticSoundPacketTo(Listener, Out);
        }
    }
}
