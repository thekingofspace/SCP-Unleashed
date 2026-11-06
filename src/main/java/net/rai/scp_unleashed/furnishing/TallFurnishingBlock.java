package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class TallFurnishingBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, LargeBlock {
    public static final MapCodec<TallFurnishingBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            TallKind.KindCodec.fieldOf("kind").forGetter(TallFurnishingBlock::getKind), propertiesCodec()).apply(Instance, TallFurnishingBlock::new));
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private final TallKind Kind;
    private final Map<Direction, VoxelShape> LowerOutlines;
    private final Map<Direction, VoxelShape> UpperOutlines;

    public TallFurnishingBlock(TallKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        this.LowerOutlines = FurnishingShapes.horizontal(Kind.getLower());
        this.UpperOutlines = FurnishingShapes.horizontal(Kind.getUpper());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(FurnishingWater.Waterlogged, false));
    }

    public TallKind getKind() {
        return Kind;
    }

    protected String[] details() {
        return new String[0];
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Half, FurnishingWater.Waterlogged);
    }

    public static BlockPos lowerPos(BlockState State, BlockPos Pos) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.above());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return FurnishingWater.placed(defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite()), Context);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        BlockPos Above = Pos.above();
        World.setBlock(Above, FurnishingWater.at(State.setValue(Half, DoubleBlockHalf.UPPER), World, Above), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        FurnishingWater.update(State, World, Pos);
        DoubleBlockHalf ThisHalf = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (ThisHalf == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            boolean Partner = NeighborState.is(this) && NeighborState.getValue(Half) != ThisHalf;
            return Partner ? partnered(State, NeighborState) : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    protected BlockState partnered(BlockState State, BlockState Partner) {
        return State;
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Half) == DoubleBlockHalf.UPPER) {
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

    @Override
    protected FluidState getFluidState(BlockState State) {
        return FurnishingWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        FurnishingTooltips.add(this, Lines, details());
    }
}
