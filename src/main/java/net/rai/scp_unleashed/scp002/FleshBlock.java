package net.rai.scp_unleashed.scp002;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

public class FleshBlock extends Block implements LiquidBlockContainer {
    public static final MapCodec<FleshBlock> BlockCodec = simpleCodec(FleshBlock::new);

    public FleshBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends FleshBlock> codec() {
        return BlockCodec;
    }

    @Override
    public boolean canEntityDestroy(BlockState State, BlockGetter World, BlockPos Pos, Entity Breaker) {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState State) {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean canDropFromExplosion(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return false;
    }

    @Override
    public boolean isFlammable(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return false;
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player User, BlockGetter World, BlockPos Pos, BlockState State, Fluid Liquid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor World, BlockPos Pos, BlockState State, FluidState Liquid) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, Fluid Liquid) {
        return false;
    }

    public static boolean isFlesh(BlockState State) {
        return State.getBlock() instanceof FleshBlock;
    }
}
