package net.rai.scp_unleashed.scp294;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record Scp294Dose(ResourceLocation Effect, int Ticks, int Amplifier) {
    public static final Codec<Scp294Dose> DoseCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(Scp294Dose::Effect),
            Codec.intRange(1, 72000).fieldOf("ticks").forGetter(Scp294Dose::Ticks),
            Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(Scp294Dose::Amplifier)
    ).apply(Instance, Scp294Dose::new));
}
