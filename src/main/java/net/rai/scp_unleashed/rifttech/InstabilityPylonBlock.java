package net.rai.scp_unleashed.rifttech;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireHostBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InstabilityPylonBlock extends BaseEntityBlock implements WireHostBlock, LargeBlock {
    public static final MapCodec<InstabilityPylonBlock> BlockCodec = simpleCodec(InstabilityPylonBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<PylonPart> Part = EnumProperty.create("part", PylonPart.class);
    public static final EnumProperty<PylonMode> Mode = EnumProperty.create("mode", PylonMode.class);
    private static final VoxelShape ShaftShape = box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape CrownShape = Shapes.or(box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0), box(5.0, 4.0, 5.0, 11.0, 11.0, 11.0));

    public InstabilityPylonBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, PylonPart.Base).setValue(Mode, PylonMode.Off));
    }

    public static int lightOf(BlockState State) {
        PylonPart Piece = State.getValue(Part);
        return switch (State.getValue(Mode)) {
            case Apocalyptic -> Piece == PylonPart.Crown ? 15 : Piece == PylonPart.Shaft ? 9 : 5;
            case Active -> Piece == PylonPart.Crown ? 12 : Piece == PylonPart.Shaft ? 7 : 4;
            case Linked -> Piece == PylonPart.Crown ? 6 : 2;
            case Idle -> Piece == PylonPart.Crown ? 3 : Piece == PylonPart.Base ? 1 : 0;
            default -> 0;
        };
    }

    public static void setMode(Level World, BlockPos BasePos, PylonMode Next) {
        for (int Offset = 0; Offset < GridTuning.PylonHeight; Offset++) {
            BlockPos Pos = BasePos.above(Offset);
            BlockState State = World.getBlockState(Pos);
            if (State.getBlock() instanceof InstabilityPylonBlock && State.getValue(Part).getOffset() == Offset && State.getValue(Mode) != Next) {
                World.setBlock(Pos, State.setValue(Mode, Next), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Nullable
    public static InstabilityPylonBlockEntity baseEntity(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof InstabilityPylonBlock Pylon)) {
            return null;
        }
        return World.getBlockEntity(Pylon.devicePos(State, Pos)) instanceof InstabilityPylonBlockEntity Found ? Found : null;
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return Pos.below(State.getValue(Part).getOffset());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part, Mode);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return switch (State.getValue(Part)) {
            case Shaft -> ShaftShape;
            case Crown -> CrownShape;
            default -> Shapes.block();
        };
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.above(), Pos.above(2));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        World.setBlock(Pos.above(), State.setValue(Part, PylonPart.Shaft), Block.UPDATE_ALL);
        World.setBlock(Pos.above(2), State.setValue(Part, PylonPart.Crown), Block.UPDATE_ALL);
    }

    private boolean partnered(BlockState State, Direction Side, BlockState NeighborState) {
        int Offset = State.getValue(Part).getOffset();
        int Wanted = Offset + (Side == Direction.UP ? 1 : -1);
        if (Wanted < 0 || Wanted >= GridTuning.PylonHeight) {
            return true;
        }
        return NeighborState.is(this) && NeighborState.getValue(Part).getOffset() == Wanted;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side.getAxis() == Direction.Axis.Y && !partnered(State, Side, NeighborState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Part) != PylonPart.Base) {
            BlockPos BasePos = devicePos(State, Pos);
            BlockState BaseState = World.getBlockState(BasePos);
            if (BaseState.is(this) && BaseState.getValue(Part) == PylonPart.Base) {
                World.setBlock(BasePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, BasePos, Block.getId(BaseState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && baseEntity(World, Pos, State) instanceof InstabilityPylonBlockEntity Pylon) {
            User.openMenu(Pylon);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.instability_pylon.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.instability_pylon.layout", GridTuning.MinSide, GridTuning.MaxSide).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.instability_pylon.box", GridTuning.MinRise, GridTuning.MaxRise).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.instability_pylon.draw", GridTuning.grouped(GridTuning.drawFor(FragmentRarity.Faint, false)),
                    GridTuning.grouped(GridTuning.drawFor(FragmentRarity.Abyssal, false))).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.instability_pylon.confined").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.instability_pylon.standby", GridTuning.StandbyDraw).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.instability_pylon.wiring").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == PylonPart.Base ? new InstabilityPylonBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != PylonPart.Base) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, RiftTechRegistry.InstabilityPylonEntity.get(), InstabilityPylonBlockEntity::clientTick)
                : createTickerHelper(Type, RiftTechRegistry.InstabilityPylonEntity.get(), InstabilityPylonBlockEntity::serverTick);
    }
}
