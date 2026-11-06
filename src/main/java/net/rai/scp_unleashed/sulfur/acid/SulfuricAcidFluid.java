package net.rai.scp_unleashed.sulfur.acid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public final class SulfuricAcidFluid {
    private SulfuricAcidFluid() {
    }

    public static boolean spreadsInto(BlockState Target, Fluid Self) {
        return Target.isAir() || Target.getFluidState().getType().isSame(Self);
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
        protected boolean canSpreadTo(BlockGetter World, BlockPos FromPos, BlockState FromState, Direction Side, BlockPos ToPos, BlockState ToState, FluidState ToFluid,
                                      Fluid Incoming) {
            return spreadsInto(ToState, this) && super.canSpreadTo(World, FromPos, FromState, Side, ToPos, ToState, ToFluid, Incoming);
        }

        @Override
        protected void animateTick(Level World, BlockPos Pos, FluidState State, RandomSource Random) {
            AcidEffects.ambience(World, Pos, State, Random);
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
        protected boolean canSpreadTo(BlockGetter World, BlockPos FromPos, BlockState FromState, Direction Side, BlockPos ToPos, BlockState ToState, FluidState ToFluid,
                                      Fluid Incoming) {
            return spreadsInto(ToState, this) && super.canSpreadTo(World, FromPos, FromState, Side, ToPos, ToState, ToFluid, Incoming);
        }

        @Override
        protected void animateTick(Level World, BlockPos Pos, FluidState State, RandomSource Random) {
            AcidEffects.ambience(World, Pos, State, Random);
        }
    }
}
