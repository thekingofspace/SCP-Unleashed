package net.rai.scp_unleashed.integration.voicechat;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.voice.VoicePlayback;
import net.rai.scp_unleashed.voice.VoiceProfile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class SvcLurePlayback implements VoicePlayback {
    public static final float Distance = 32.0f;
    public static final int MaxPlaying = 16;
    public static final int FrameSize = 960;
    private final VoicechatServerApi Api;
    private final Map<LocationalAudioChannel, Entity> Following = new ConcurrentHashMap<>();
    private final Map<AudioPlayer, Boolean> Playing = new ConcurrentHashMap<>();

    public SvcLurePlayback(VoicechatServerApi Api) {
        this.Api = Api;
    }

    private Position position(Entity Source) {
        return Api.createPosition(Source.getX(), Source.getY() + Source.getBbHeight() * 0.6, Source.getZ());
    }

    private static final class Shaper implements Supplier<short[]> {
        private final List<byte[]> Frames;
        private final OpusDecoder Decoder;
        private final VoiceProfile Profile;
        private final float PitchScale;
        private final long Seed;
        private short[] Shaped;
        private int Read;

        private Shaper(List<byte[]> Frames, OpusDecoder Decoder, VoiceProfile Profile, float PitchScale, long Seed) {
            this.Frames = Frames;
            this.Decoder = Decoder;
            this.Profile = Profile;
            this.PitchScale = PitchScale;
            this.Seed = Seed;
        }

        private short[] decodeAll() {
            short[][] Parts = new short[Frames.size()][];
            int Total = 0;
            for (int Index = 0; Index < Frames.size(); Index++) {
                try {
                    Parts[Index] = Decoder.decode(Frames.get(Index));
                } catch (RuntimeException Problem) {
                    Parts[Index] = new short[0];
                }
                Total += Parts[Index] == null ? 0 : Parts[Index].length;
            }
            short[] Joined = new short[Total];
            int At = 0;
            for (short[] Part : Parts) {
                if (Part != null) {
                    System.arraycopy(Part, 0, Joined, At, Part.length);
                    At += Part.length;
                }
            }
            return Joined;
        }

        @Override
        public short[] get() {
            if (Shaped == null) {
                Shaped = Profile.apply(decodeAll(), Seed, PitchScale);
            }
            if (Read >= Shaped.length) {
                return null;
            }
            short[] Out = new short[FrameSize];
            int Count = Math.min(FrameSize, Shaped.length - Read);
            System.arraycopy(Shaped, Read, Out, 0, Count);
            Read += Count;
            return Out;
        }
    }

    @Override
    public boolean play(ServerLevel World, Entity Source, List<byte[]> Frames, VoiceProfile Profile, float PitchScale, long Seed) {
        if (Frames.isEmpty() || Playing.size() >= MaxPlaying) {
            return false;
        }
        LocationalAudioChannel Channel = Api.createLocationalAudioChannel(UUID.randomUUID(), Api.fromServerLevel(World), position(Source));
        if (Channel == null) {
            return false;
        }
        Channel.setDistance(Distance);
        OpusDecoder Decoder = Api.createDecoder();
        OpusEncoder Encoder = Api.createEncoder();
        AudioPlayer Voice = Api.createAudioPlayer(Channel, Encoder, new Shaper(List.copyOf(Frames), Decoder, Profile, PitchScale, Seed));
        Following.put(Channel, Source);
        Playing.put(Voice, Boolean.TRUE);
        Voice.setOnStopped(() -> {
            Following.remove(Channel);
            Playing.remove(Voice);
            Decoder.close();
            Encoder.close();
        });
        Voice.startPlaying();
        return true;
    }

    @Override
    public void follow() {
        for (Map.Entry<LocationalAudioChannel, Entity> Entry : Following.entrySet()) {
            Entity Source = Entry.getValue();
            if (!Source.isRemoved()) {
                Entry.getKey().updateLocation(position(Source));
            }
        }
    }

    @Override
    public void stopAll() {
        for (AudioPlayer Each : Playing.keySet()) {
            Each.stopPlaying();
        }
        Playing.clear();
        Following.clear();
    }
}
