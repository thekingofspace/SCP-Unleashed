package net.rai.scp_unleashed.sulfur.acid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class SulfuricAcidBlock extends LiquidBlock {
    public SulfuricAcidBlock(FlowingFluid Liquid, Properties Settings) {
        super(Liquid, Settings);
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Target) {
        super.entityInside(State, World, Pos, Target);
        if (World instanceof ServerLevel Server) {
            AcidContact.contact(Server, Pos, Target);
        }
    }
}
