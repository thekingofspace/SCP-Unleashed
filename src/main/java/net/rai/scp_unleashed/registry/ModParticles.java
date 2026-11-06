package net.rai.scp_unleashed.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.particle.PipeLiquidOptions;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VentAir = ParticleRegister.register("vent_air", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, ParticleType<PipeLiquidOptions>> PipeLiquid = ParticleRegister.register("pipe_liquid", () -> new ParticleType<PipeLiquidOptions>(false) {
        @Override
        public MapCodec<PipeLiquidOptions> codec() {
            return PipeLiquidOptions.DataCodec;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, PipeLiquidOptions> streamCodec() {
            return PipeLiquidOptions.NetworkCodec;
        }
    });
}
