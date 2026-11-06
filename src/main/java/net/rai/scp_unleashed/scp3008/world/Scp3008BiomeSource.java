package net.rai.scp_unleashed.scp3008.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Scp3008BiomeSource extends BiomeSource {
    public static final MapCodec<Scp3008BiomeSource> SourceCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.unboundedMap(Codec.STRING, Biome.CODEC).fieldOf("departments").forGetter(Source -> Source.Named)
    ).apply(Instance, Instance.stable(Scp3008BiomeSource::new)));
    private final Map<String, Holder<Biome>> Named;
    private final List<Holder<Biome>> Ordered;

    public Scp3008BiomeSource(Map<String, Holder<Biome>> Named) {
        this.Named = Map.copyOf(Named);
        List<Holder<Biome>> Found = new ArrayList<>();
        Holder<Biome> First = Named.values().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("scp3008 departments need at least one biome"));
        for (String Key : Scp3008Plan.Departments) {
            Found.add(Named.getOrDefault(Key, First));
        }
        this.Ordered = List.copyOf(Found);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return SourceCodec;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Ordered.stream().distinct();
    }

    public Holder<Biome> biomeAt(int X, int Z) {
        return Ordered.get(Scp3008Plan.departmentAt(X, Z));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int X, int Y, int Z, Climate.Sampler Sampler) {
        return biomeAt(QuartPos.toBlock(X) + 2, QuartPos.toBlock(Z) + 2);
    }
}
