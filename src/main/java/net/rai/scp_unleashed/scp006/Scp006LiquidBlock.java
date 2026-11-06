package net.rai.scp_unleashed.scp006;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class Scp006LiquidBlock extends LiquidBlock {
    public Scp006LiquidBlock(FlowingFluid Liquid, Properties Settings) {
        super(Liquid, Settings);
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Target) {
        super.entityInside(State, World, Pos, Target);
        if (World instanceof ServerLevel Server && Target instanceof LivingEntity Living && Scp006Mending.submerged(Living, Server, Pos)) {
            Scp006Mending.touch(Server, Living);
        }
    }
}
