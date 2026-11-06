package net.rai.scp_unleashed.scp006;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class Scp006FluidType extends FluidType {
    public Scp006FluidType(Properties Settings) {
        super(Settings);
    }

    @Override
    public boolean canConvertToSource(FluidState State, LevelReader Reader, BlockPos Pos) {
        if (Reader instanceof Level World) {
            return World.getGameRules().getBoolean(GameRules.RULE_WATER_SOURCE_CONVERSION);
        }
        return super.canConvertToSource(State, Reader, Pos);
    }

    @Override
    public boolean canConvertToSource(FluidStack Stack) {
        return true;
    }

    @Override
    public boolean isVaporizedOnPlacement(Level World, BlockPos Pos, FluidStack Stack) {
        return World.dimensionType().ultraWarm();
    }
}
