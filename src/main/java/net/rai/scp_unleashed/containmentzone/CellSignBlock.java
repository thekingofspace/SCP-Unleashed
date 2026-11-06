package net.rai.scp_unleashed.containmentzone;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.PropTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CellSignBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CellSignBlock> BlockCodec = simpleCodec(CellSignBlock::new);
    public static final EnumProperty<CellSignHalf> Half = EnumProperty.create("half", CellSignHalf.class);
    public static final int Variants = 4;
    public static final IntegerProperty Variant = IntegerProperty.create("variant", 0, Variants - 1);
    private static final Map<Direction, VoxelShape> LeftShapes = PropShapes.horizontal(PropShapes.box(0, 1, 15, 15, 16, 16));
    private static final Map<Direction, VoxelShape> RightShapes = PropShapes.horizontal(PropShapes.box(1, 1, 15, 16, 16, 16));

    public CellSignBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, CellSignHalf.Left).setValue(Variant, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Half, Variant);
    }

    public static Direction rightOf(Direction Facing) {
        return Facing.getCounterClockWise();
    }

    public static Direction towardPartner(BlockState State) {
        Direction Right = rightOf(State.getValue(FACING));
        return State.getValue(Half) == CellSignHalf.Left ? Right : Right.getOpposite();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Facing = CellPropBlock.facingFor(Context, true);
        Direction Right = rightOf(Facing);
        BlockPos Clicked = Context.getClickedPos();
        for (CellSignHalf Placed : CellSignHalf.values()) {
            BlockPos Other = Clicked.relative(Placed == CellSignHalf.Left ? Right : Right.getOpposite());
            if (fits(Context, Other)) {
                return defaultBlockState().setValue(FACING, Facing).setValue(Half, Placed);
            }
        }
        return null;
    }

    private static boolean fits(BlockPlaceContext Context, BlockPos Other) {
        Level World = Context.getLevel();
        return !World.isOutsideBuildHeight(Other) && World.getWorldBorder().isWithinBounds(Other) && World.getBlockState(Other).canBeReplaced(Context);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        World.setBlock(Pos.relative(towardPartner(State)), State.setValue(Half, State.getValue(Half).other()), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == towardPartner(State)) {
            boolean Paired = NeighborState.is(this) && NeighborState.getValue(FACING) == State.getValue(FACING) && NeighborState.getValue(Half) != State.getValue(Half);
            if (!Paired) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && State.getValue(Half) == CellSignHalf.Right) {
            BlockPos Partner = Pos.relative(towardPartner(State));
            BlockState PartnerState = World.getBlockState(Partner);
            if (PartnerState.is(this) && PartnerState.getValue(Half) == CellSignHalf.Left) {
                if (User.isCreative()) {
                    World.setBlock(Partner, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    World.levelEvent(User, 2001, Partner, Block.getId(PartnerState));
                } else {
                    World.destroyBlock(Partner, true, User);
                }
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            int Current = State.getValue(Variant);
            int Next = User.isShiftKeyDown() ? (Current + Variants - 1) % Variants : (Current + 1) % Variants;
            World.setBlock(Pos, State.setValue(Variant, Next), Block.UPDATE_ALL);
            BlockPos Partner = Pos.relative(towardPartner(State));
            BlockState PartnerState = World.getBlockState(Partner);
            if (PartnerState.is(this)) {
                World.setBlock(Partner, PartnerState.setValue(Variant, Next), Block.UPDATE_ALL);
            }
            World.playSound(null, Pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.7f, 0.9f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Half) == CellSignHalf.Left ? LeftShapes : RightShapes).get(State.getValue(FACING));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        if (Flip == Mirror.NONE) {
            return State;
        }
        return State.rotate(Flip.getRotation(State.getValue(FACING))).setValue(Half, State.getValue(Half).other());
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, "use", "room");
    }
}
