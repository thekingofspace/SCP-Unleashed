package net.rai.scp_unleashed.speaker.cassette;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record CassetteRecording(ResourceLocation Sound, Component Title, Optional<Integer> Colour) {
    public static final int DefaultLabel = 0xE6DCC2;
    public static final Codec<CassetteRecording> RecordingCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ResourceLocation.CODEC.fieldOf("sound").forGetter(CassetteRecording::Sound),
            ComponentSerialization.CODEC.fieldOf("title").forGetter(CassetteRecording::Title),
            Codec.INT.optionalFieldOf("colour").forGetter(CassetteRecording::Colour)
    ).apply(Instance, CassetteRecording::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CassetteRecording> RecordingStreamCodec = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CassetteRecording::Sound,
            ComponentSerialization.STREAM_CODEC, CassetteRecording::Title,
            ByteBufCodecs.optional(ByteBufCodecs.INT), CassetteRecording::Colour,
            CassetteRecording::new);

    public int labelColour() {
        return Colour.orElse(DefaultLabel) & 0xFFFFFF;
    }
}
