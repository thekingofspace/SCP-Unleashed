package net.rai.scp_unleashed.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.rai.scp_unleashed.liquidsound.LiquidSoundRegistry;
import net.rai.scp_unleashed.liquidsound.LiquidSounds;

public final class OilFluid {
    private OilFluid() {
    }

    public static class Still extends BaseFlowingFluid.Source {
        public Still(BaseFlowingFluid.Properties Settings) {
            super(Settings);
        }

        @Override
        protected boolean canBeReplacedWith(FluidState State, BlockGetter World, BlockPos Pos, Fluid Incoming, Direction Side) {
            return false;
        }

        @Override
        protected void animateTick(Level World, BlockPos Pos, FluidState State, RandomSource Random) {
            LiquidSounds.ambience(World, Pos, State, Random, LiquidSoundRegistry.Oil);
        }
    }

    public static class Moving extends BaseFlowingFluid.Flowing {
        public Moving(BaseFlowingFluid.Properties Settings) {
            super(Settings);
        }

        @Override
        protected boolean canBeReplacedWith(FluidState State, BlockGetter World, BlockPos Pos, Fluid Incoming, Direction Side) {
            return false;
        }

        @Override
        protected void animateTick(Level World, BlockPos Pos, FluidState State, RandomSource Random) {
            LiquidSounds.ambience(World, Pos, State, Random, LiquidSoundRegistry.Oil);
        }
    }
}
