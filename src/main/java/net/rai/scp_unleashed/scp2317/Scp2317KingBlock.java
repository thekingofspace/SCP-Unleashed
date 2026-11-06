package net.rai.scp_unleashed.scp2317;

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
import org.jetbrains.annotations.Nullable;

public class Scp2317KingBlock extends BaseEntityBlock {
    public static final MapCodec<Scp2317KingBlock> KingCodec = simpleCodec(Scp2317KingBlock::new);
    private static final VoxelShape Shape = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public Scp2317KingBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return KingCodec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp2317KingBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp2317Registry.KingEntity.get(), Scp2317KingBlockEntity::serverTick);
    }
}
