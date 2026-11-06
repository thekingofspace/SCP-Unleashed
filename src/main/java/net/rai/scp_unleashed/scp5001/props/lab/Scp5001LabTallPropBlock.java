package net.rai.scp_unleashed.scp5001.props.lab;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class Scp5001LabTallPropBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp5001LabTallPropBlock> TallCodec = simpleCodec(Props -> new Scp5001LabTallPropBlock(Scp5001LabProp.SpecimenTank, Props));
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private final Scp5001LabProp Entry;
    private final Map<Direction, VoxelShape> LowerShapes;
    private final Map<Direction, VoxelShape> UpperShapes;

    public Scp5001LabTallPropBlock(Scp5001LabProp Entry, Properties Props) {
        super(Props);
        this.Entry = Entry;
        this.LowerShapes = Scp5001LabShapes.horizontal(Entry.lower());
        this.UpperShapes = Scp5001LabShapes.horizontal(Entry.upper());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    public Scp5001LabProp entry() {
        return Entry;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return TallCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Half);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        Level World = Context.getLevel();
        if (Pos.getY() >= World.getMaxBuildHeight() - 1 || !World.getBlockState(Pos.above()).canBeReplaced(Context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite()).setValue(Half, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        World.setBlock(Pos.above(), State.setValue(Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        DoubleBlockHalf Mine = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (Mine == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            boolean Paired = NeighborState.is(this) && NeighborState.getValue(Half) != Mine;
            return Paired ? State : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && (User.isCreative() || !User.hasCorrectToolForDrops(State)) && State.getValue(Half) == DoubleBlockHalf.UPPER) {
            BlockPos LowerPos = Pos.below();
            BlockState LowerState = World.getBlockState(LowerPos);
            if (LowerState.is(this) && LowerState.getValue(Half) == DoubleBlockHalf.LOWER) {
                World.setBlock(LowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, LowerPos, Block.getId(LowerState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Half) == DoubleBlockHalf.LOWER ? LowerShapes : UpperShapes).get(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }
}
