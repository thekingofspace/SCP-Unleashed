package net.rai.scp_unleashed.scp006;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public final class Scp006Fluid {
    public static final int GlintChance = 140;
    public static final int AmbientChance = 64;
    public static final int BubbleChance = 10;

    private Scp006Fluid() {
    }

    public static boolean replaceable(Fluid Incoming, Direction Side) {
        return Side == Direction.DOWN && !Incoming.is(FluidTags.WATER);
    }

    public static void ambience(Level World, BlockPos Pos, FluidState State, RandomSource Random) {
        if (!State.isSource() && !State.getValue(BaseFlowingFluid.FALLING)) {
            if (Random.nextInt(AmbientChance) == 0) {
                World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS,
                        Random.nextFloat() * 0.25f + 0.75f, Random.nextFloat() + 0.5f, false);
            }
        } else if (Random.nextInt(BubbleChance) == 0) {
            World.addParticle(ParticleTypes.UNDERWATER, Pos.getX() + Random.nextDouble(), Pos.getY() + Random.nextDouble(), Pos.getZ() + Random.nextDouble(), 0.0, 0.0, 0.0);
        }
        if (State.isSource() && Random.nextInt(GlintChance) == 0 && World.getFluidState(Pos.above()).isEmpty() && !World.getBlockState(Pos.above()).isSolid()) {
            double Surface = Pos.getY() + State.getHeight(World, Pos) + 0.02;
            World.addParticle(Scp006Registry.Glint.get(), Pos.getX() + 0.15 + Random.nextDouble() * 0.7, Surface, Pos.getZ() + 0.15 + Random.nextDouble() * 0.7, 0.0, 0.0, 0.0);
        }
    }

    public static class Source extends BaseFlowingFluid.Source {
        public Source(BaseFlowingFluid.Properties Settings) {
            super(Settings);
        }

        @Override
        protected boolean canBeReplacedWith(FluidState State, BlockGetter World, BlockPos Pos, Fluid Incoming, Direction Side) {
            return replaceable(Incoming, Side);
        }

        @Override
        protected void animateTick(Level World, BlockPos Pos, FluidState State, RandomSource Random) {
            ambience(World, Pos, State, Random);
        }

        @Override
        protected ParticleOptions getDripParticle() {
            return ParticleTypes.DRIPPING_WATER;
        }
    }

    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(BaseFlowingFluid.Properties Settings) {
            super(Settings);
        }

        @Override
        protected boolean canBeReplacedWith(FluidState State, BlockGetter World, BlockPos Pos, Fluid Incoming, Direction Side) {
            return replaceable(Incoming, Side);
        }

        @Override
        protected void animateTick(Level World, BlockPos Pos, FluidState State, RandomSource Random) {
            ambience(World, Pos, State, Random);
        }

        @Override
        protected ParticleOptions getDripParticle() {
            return ParticleTypes.DRIPPING_WATER;
        }
    }
}
