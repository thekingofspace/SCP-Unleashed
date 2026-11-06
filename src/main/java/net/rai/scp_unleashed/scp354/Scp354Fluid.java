package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class Scp354Fluid {
    public static final int BubbleChance = 14;
    public static final int RippleChance = 70;
    public static final int SoundChance = 520;
    public static final double SurfaceHeight = 8.0 / 9.0;

    private Scp354Fluid() {
    }

    public static boolean isRed(FluidState State) {
        return State.getType() instanceof Source || State.getType() instanceof Flowing;
    }

    public static void ambience(Level World, BlockPos Pos, RandomSource Random) {
        boolean Open = World.getFluidState(Pos.above()).isEmpty() && !World.getBlockState(Pos.above()).isSolid();
        if (Random.nextInt(BubbleChance) == 0) {
            World.addParticle(Scp354Registry.Bubble.get(), Pos.getX() + 0.2 + Random.nextDouble() * 0.6, Pos.getY() + Random.nextDouble() * 0.6,
                    Pos.getZ() + 0.2 + Random.nextDouble() * 0.6, 0.0, 0.0, 0.0);
        }
        if (!Open) {
            return;
        }
        if (Random.nextInt(RippleChance) == 0) {
            World.addParticle(Scp354Registry.Ripple.get(), Pos.getX() + 0.15 + Random.nextDouble() * 0.7, Pos.getY() + SurfaceHeight + 0.012,
                    Pos.getZ() + 0.15 + Random.nextDouble() * 0.7, 0.0, 0.0, 0.0);
        }
        if (Random.nextInt(SoundChance) == 0) {
            float Pitch = 0.75f + Random.nextFloat() * 0.3f;
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp354Registry.BubbleLayers) {
                World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + SurfaceHeight, Pos.getZ() + 0.5, Layer.get(), SoundSource.BLOCKS, 0.55f, Pitch, false);
            }
        }
    }

    public static class Source extends BaseFlowingFluid.Source {
        public Source(BaseFlowingFluid.Properties Settings) {
            super(Settings);
        }

        @Override
        public void tick(Level World, BlockPos Pos, FluidState State) {
        }

        @Override
        protected boolean canBeReplacedWith(FluidState State, BlockGetter World, BlockPos Pos, Fluid Incoming, Direction Side) {
            return false;
        }

        @Override
        public int getTickDelay(LevelReader World) {
            return 1000;
        }

        @Override
        protected boolean isRandomlyTicking() {
            return false;
        }

        @Override
        protected void animateTick(Level World, BlockPos Pos, FluidState State, RandomSource Random) {
            ambience(World, Pos, Random);
        }

        @Override
        protected ParticleOptions getDripParticle() {
            return Scp354Registry.Drip.get();
        }
    }

    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(BaseFlowingFluid.Properties Settings) {
            super(Settings);
        }

        @Override
        public void tick(Level World, BlockPos Pos, FluidState State) {
        }

        @Override
        protected boolean canBeReplacedWith(FluidState State, BlockGetter World, BlockPos Pos, Fluid Incoming, Direction Side) {
            return false;
        }

        @Override
        public int getTickDelay(LevelReader World) {
            return 1000;
        }

        @Override
        protected boolean isRandomlyTicking() {
            return false;
        }

        @Override
        protected ParticleOptions getDripParticle() {
            return Scp354Registry.Drip.get();
        }
    }
}
