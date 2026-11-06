package net.rai.scp_unleashed.voice.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record VoicePreferencePayload(boolean Allow) implements CustomPacketPayload {
    public static final Type<VoicePreferencePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "voice_preference"));
    public static final StreamCodec<ByteBuf, VoicePreferencePayload> PayloadCodec = ByteBufCodecs.BOOL.map(VoicePreferencePayload::new, VoicePreferencePayload::Allow);

    @Override
    public Type<VoicePreferencePayload> type() {
        return PayloadType;
    }
}
