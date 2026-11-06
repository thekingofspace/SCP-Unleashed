package net.rai.scp_unleashed.props;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class FilingCabinetBlock extends StorageBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<FilingCabinetBlock> BlockCodec = simpleCodec(FilingCabinetBlock::new);
    private static final Map<Direction, VoxelShape> Outlines = PropShapes.horizontal(PropShapes.of(box(1, 0, 1, 15, 16, 16)));

    public FilingCabinetBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false).setValue(PropWater.Waterlogged, false));
    }

    @Override
    public StorageKind getKind() {
        return StorageKind.FilingCabinet;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open, PropWater.Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return PropWater.placed(defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()), Context);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(Facing));
    }
}
