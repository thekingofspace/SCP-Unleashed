package net.rai.scp_unleashed.scp472;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp472BloodstoneBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final MapCodec<Scp472BloodstoneBlock> BlockCodec = simpleCodec(Scp472BloodstoneBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;
    public static final double Inset = 1.5;
    public static final double Height = 7.0;
    public static final VoxelShape Shape = Block.box(Inset, 0.0, Inset, 16.0 - Inset, Height, 16.0 - Inset);

    public Scp472BloodstoneBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Waterlogged, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Waterlogged);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter Getter, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader Reader, BlockPos Pos) {
        BlockPos Support = Pos.below();
        return Reader.getBlockState(Support).isFaceSturdy(Reader, Support, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (State.getValue(Waterlogged)) {
            World.scheduleTick(Pos, Fluids.WATER, Fluids.WATER.getTickDelay(World));
        }
        if (Side == Direction.DOWN && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = defaultBlockState().setValue(Waterlogged, Context.getLevel().getFluidState(Context.getClickedPos()).getType() == Fluids.WATER)
                .setValue(Facing, Context.getHorizontalDirection().getOpposite());
        return Placed.canSurvive(Context.getLevel(), Context.getClickedPos()) ? Placed : null;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return State.getValue(Waterlogged) ? Fluids.WATER.getSource(false) : super.getFluidState(State);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp472BloodstoneBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return Type == Scp472Registry.BloodstoneEntity.get() ? (BlockEntityTicker<T>) (BlockEntityTicker<Scp472BloodstoneBlockEntity>) Scp472BloodstoneBlockEntity::tick : null;
    }
}
