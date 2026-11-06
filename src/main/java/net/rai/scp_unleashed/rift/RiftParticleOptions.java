package net.rai.scp_unleashed.rift;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RiftParticleOptions(ParticleType<RiftParticleOptions> Kind, int Colour, int Anchor, float Scale) implements ParticleOptions {
    public static MapCodec<RiftParticleOptions> codec(ParticleType<RiftParticleOptions> Kind) {
        return RecordCodecBuilder.mapCodec(Fields -> Fields.group(
                Codec.INT.fieldOf("colour").forGetter(RiftParticleOptions::Colour),
                Codec.INT.optionalFieldOf("anchor", -1).forGetter(RiftParticleOptions::Anchor),
                Codec.FLOAT.optionalFieldOf("scale", 1.0f).forGetter(RiftParticleOptions::Scale)
        ).apply(Fields, (Colour, Anchor, Scale) -> new RiftParticleOptions(Kind, Colour, Anchor, Scale)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, RiftParticleOptions> streamCodec(ParticleType<RiftParticleOptions> Kind) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, RiftParticleOptions::Colour,
                ByteBufCodecs.INT, RiftParticleOptions::Anchor,
                ByteBufCodecs.FLOAT, RiftParticleOptions::Scale,
                (Colour, Anchor, Scale) -> new RiftParticleOptions(Kind, Colour, Anchor, Scale));
    }

    @Override
    public ParticleType<RiftParticleOptions> getType() {
        return Kind;
    }
}
