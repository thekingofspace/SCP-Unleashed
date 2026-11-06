package net.rai.scp_unleashed.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.rai.scp_unleashed.registry.ModParticles;

public record PipeLiquidOptions(Fluid Liquid, boolean Suction) implements ParticleOptions {
    public static final int SuctionTicks = 14;
    public static final MapCodec<PipeLiquidOptions> DataCodec = RecordCodecBuilder.mapCodec(Fields -> Fields.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(PipeLiquidOptions::Liquid),
            Codec.BOOL.optionalFieldOf("suction", false).forGetter(PipeLiquidOptions::Suction)
    ).apply(Fields, PipeLiquidOptions::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PipeLiquidOptions> NetworkCodec = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.FLUID), PipeLiquidOptions::Liquid,
            ByteBufCodecs.BOOL, PipeLiquidOptions::Suction,
            PipeLiquidOptions::new);

    @Override
    public ParticleType<PipeLiquidOptions> getType() {
        return ModParticles.PipeLiquid.get();
    }
}
