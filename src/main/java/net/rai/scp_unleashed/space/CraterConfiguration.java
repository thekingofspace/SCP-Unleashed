package net.rai.scp_unleashed.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record CraterConfiguration(IntProvider Radius, float Depth, float Rim, float Ejecta, float Rocks) implements FeatureConfiguration {
    public static final Codec<CraterConfiguration> ConfigCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            IntProvider.codec(1, 24).fieldOf("radius").forGetter(CraterConfiguration::Radius),
            Codec.floatRange(0.0F, 1.0F).fieldOf("depth").forGetter(CraterConfiguration::Depth),
            Codec.floatRange(0.0F, 1.0F).fieldOf("rim").forGetter(CraterConfiguration::Rim),
            Codec.floatRange(1.0F, 3.0F).fieldOf("ejecta").forGetter(CraterConfiguration::Ejecta),
            Codec.floatRange(0.0F, 1.0F).fieldOf("rocks").forGetter(CraterConfiguration::Rocks)
    ).apply(Instance, CraterConfiguration::new));
}
