package net.rai.scp_unleashed.scp5001.engine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp5001.world.Scp5001WorldRegistry;

public class Scp5001EngineBlock extends BaseEntityBlock {
    public static final MapCodec<Scp5001EngineBlock> BlockCodec = simpleCodec(Scp5001EngineBlock::new);
    public static final long Output = 20_000_000L;
    public static final VoxelShape Shape = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public Scp5001EngineBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp5001EngineBlockEntity(Pos, State);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp5001WorldRegistry.EngineEntity.get(), Scp5001EngineBlockEntity::serverTick);
    }
}
