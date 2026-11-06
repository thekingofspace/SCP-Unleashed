package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.entity.RenderableBlockEntity;
import org.jetbrains.annotations.Nullable;

public class RenderableBlock extends BaseEntityBlock {
    public static final MapCodec<RenderableBlock> BlockCodec = simpleCodec(RenderableBlock::new);

    public RenderableBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new RenderableBlockEntity(Pos, State);
    }
}
