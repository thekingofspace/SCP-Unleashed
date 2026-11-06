package net.rai.scp_unleashed.fluiddump;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.scp024.Scp024Stage;
import net.rai.scp_unleashed.scp087.Scp087Stairwell;
import net.rai.scp_unleashed.scp106.Scp106Pocket;

public final class PollutionRules {
    private PollutionRules() {
    }

    public static boolean isProtected(Level World) {
        return Scp106Pocket.inPocket(World) || Scp087Stairwell.inStairwell(World) || Scp024Stage.inStage(World);
    }

    public static boolean isWater(FluidState Liquid) {
        return !Liquid.isEmpty() && Liquid.is(FluidTags.WATER);
    }

    public static boolean isOpenWater(BlockState State) {
        return isWater(State.getFluidState()) && !State.hasProperty(BlockStateProperties.WATERLOGGED);
    }

    public static boolean isExempt(LivingEntity Target) {
        return !Target.isAlive() || Target instanceof Player User && (User.isCreative() || User.isSpectator());
    }

    public static boolean inWasteWater(LivingEntity Target) {
        return Target.isInFluidType(FissionRegistry.WasteWaterType.get());
    }
}
