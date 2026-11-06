package net.rai.scp_unleashed.scp5001.props.hab;

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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;

public class Scp5001HabTallBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final MapCodec<Scp5001HabTallBlock> TallCodec = simpleCodec(Props -> new Scp5001HabTallBlock(Props, Scp5001HabCatalog.Locker));
    private final Scp5001HabCatalog Entry;
    private final Map<Direction, VoxelShape> LowerOutlines;
    private final Map<Direction, VoxelShape> UpperOutlines;

    public Scp5001HabTallBlock(Properties Props, Scp5001HabCatalog Entry) {
        super(Props);
        this.Entry = Entry;
        this.LowerOutlines = Scp5001HabShapes.horizontal(Entry.lower());
        this.UpperOutlines = Scp5001HabShapes.horizontal(Entry.upper());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    public Scp5001HabCatalog entry() {
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

    @Override
    @Nullable
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
        DoubleBlockHalf ThisHalf = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (ThisHalf == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            boolean Partner = NeighborState.is(this) && NeighborState.getValue(Half) != ThisHalf;
            return Partner ? State : Blocks.AIR.defaultBlockState();
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
        return (State.getValue(Half) == DoubleBlockHalf.LOWER ? LowerOutlines : UpperOutlines).get(State.getValue(FACING));
    }
}
