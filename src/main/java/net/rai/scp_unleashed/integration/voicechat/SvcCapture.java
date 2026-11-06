package net.rai.scp_unleashed.integration.voicechat;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.scp939.Scp939VoiceAttraction;
import net.rai.scp_unleashed.scp939.Scp939VoiceLure;
import net.rai.scp_unleashed.voice.VoiceArchive;
import net.rai.scp_unleashed.voice.VoicePolicy;

public final class SvcCapture {
    private SvcCapture() {
    }

    public static void capture(MicrophonePacketEvent Event, VoicechatConnection Sender, ServerPlayer Speaker) {
        if (!VoicePolicy.masterCached()) {
            return;
        }
        MicrophonePacket Spoken = Event.getPacket();
        if (Spoken == null) {
            return;
        }
        byte[] Opus = Spoken.getOpusEncodedData();
        if (Opus == null || Opus.length == 0) {
            return;
        }
        VoiceArchive.Heard Sample = new VoiceArchive.Heard(Speaker.getUUID(), Speaker.getGameProfile().getName(), Speaker.level().dimension(), Speaker.getX(),
                Speaker.getEyeY(), Speaker.getZ(), Speaker.isSpectator(), Sender.isDisabled(), Spoken.isWhispering(), Sender.isInGroup());
        long Now = System.currentTimeMillis();
        VoiceArchive.hear(Sample, Opus, Now);
        Scp939VoiceLure.hear(Sample, Opus, Now);
        Scp939VoiceAttraction.voiceHeard(Sample, Now);
    }
}
