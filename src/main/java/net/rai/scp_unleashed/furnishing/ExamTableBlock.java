package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static net.rai.scp_unleashed.furnishing.FurnishingShapes.box;

public class ExamTableBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, LargeBlock, SeatHost {
    public static final MapCodec<ExamTableBlock> BlockCodec = simpleCodec(ExamTableBlock::new);
    public static final EnumProperty<BedPart> Part = BlockStateProperties.BED_PART;
    public static final double SeatHeight = 10.0 / 16.0;
    private static final Map<Direction, VoxelShape> HeadOutlines = FurnishingShapes.horizontal(FurnishingShapes.of(
            box(2, 0, 0, 14, 7, 16), box(1, 7, 0, 15, 10, 16), box(1, 10, 0, 15, 13, 6), box(3, 13, 0, 13, 15, 2)));
    private static final Map<Direction, VoxelShape> FootOutlines = FurnishingShapes.horizontal(FurnishingShapes.of(
            box(2, 0, 0, 14, 7, 13), box(1, 7, 0, 15, 10, 14), box(3, 1, 13, 13, 3, 16)));

    public ExamTableBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Part, BedPart.FOOT).setValue(FurnishingWater.Waterlogged, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Part, FurnishingWater.Waterlogged);
    }

    private static Direction partnerSide(BlockState State) {
        Direction Facing = State.getValue(FACING);
        return State.getValue(Part) == BedPart.FOOT ? Facing : Facing.getOpposite();
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.relative(Context.getHorizontalDirection()));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return FurnishingWater.placed(defaultBlockState().setValue(FACING, Context.getHorizontalDirection()), Context);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        BlockPos Head = Pos.relative(State.getValue(FACING));
        World.setBlock(Head, FurnishingWater.at(State.setValue(Part, BedPart.HEAD), World, Head), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        FurnishingWater.update(State, World, Pos);
        if (Side == partnerSide(State)) {
            return NeighborState.is(this) && NeighborState.getValue(Part) != State.getValue(Part) ? State : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Part) == BedPart.FOOT) {
            BlockPos HeadPos = Pos.relative(partnerSide(State));
            BlockState HeadState = World.getBlockState(HeadPos);
            if (HeadState.is(this) && HeadState.getValue(Part) == BedPart.HEAD) {
                World.setBlock(HeadPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, HeadPos, Block.getId(HeadState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        return Seating.sit(World, Pos, State, User);
    }

    @Override
    public double seatHeight(BlockState State) {
        return SeatHeight;
    }

    @Override
    public float seatYaw(BlockState State, BlockPos Pos, Player User) {
        Direction Side = State.getValue(FACING).getClockWise();
        double Toward = (User.getX() - (Pos.getX() + 0.5)) * Side.getStepX() + (User.getZ() - (Pos.getZ() + 0.5)) * Side.getStepZ();
        return (Toward >= 0 ? Side : Side.getOpposite()).toYRot();
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Part) == BedPart.HEAD ? HeadOutlines : FootOutlines).get(State.getValue(FACING));
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return FurnishingWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        FurnishingTooltips.add(this, Lines, "sit");
    }
}
