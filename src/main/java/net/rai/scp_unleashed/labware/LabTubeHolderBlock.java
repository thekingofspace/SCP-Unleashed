package net.rai.scp_unleashed.labware;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class LabTubeHolderBlock extends BaseEntityBlock {
    public static final MapCodec<LabTubeHolderBlock> BlockCodec = simpleCodec(LabTubeHolderBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final double SlotSpan = 4.0 / 16.0;
    public static final double SlotLow = 0.3 / 16.0;
    public static final double SlotWidth = 3.4 / 16.0;
    public static final double SlotDepth = 2.6 / 16.0;
    public static final double SlotTop = 10.0 / 16.0;
    private static final Map<Direction, VoxelShape> Outlines = LabwareShapes.horizontal(LabwareShapes.TubeHolder);

    public LabTubeHolderBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
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
    protected VoxelShape getShape(BlockState State, BlockGetter Getter, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(Facing));
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Below = Pos.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        return Side == Direction.DOWN && !canSurvive(State, World, Pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    public static Direction leftOf(Direction Front) {
        return Front.getClockWise();
    }

    public static int slotFromHit(BlockState State, BlockPos Pos, Vec3 Hit) {
        Direction Left = leftOf(State.getValue(Facing));
        double Across = Left.getStepX() * (Hit.x - Pos.getX() - 0.5) + Left.getStepZ() * (Hit.z - Pos.getZ() - 0.5);
        int Slot = (int) Math.floor((0.5 - Across) * LabTubeHolderBlockEntity.SlotCount);
        return Math.max(0, Math.min(LabTubeHolderBlockEntity.SlotCount - 1, Slot));
    }

    public static Vec3 slotCentre(Direction Front, int Slot) {
        Direction Left = leftOf(Front);
        double Along = SlotLow + Slot * SlotSpan + SlotWidth * 0.5;
        double Offset = 0.5 - Along;
        return new Vec3(0.5 + Left.getStepX() * Offset, 0.0, 0.5 + Left.getStepZ() * Offset);
    }

    public static AABB slotBox(BlockState State, BlockPos Pos, int Slot) {
        Direction Front = State.getValue(Facing);
        Vec3 Centre = slotCentre(Front, Slot);
        boolean AlongX = leftOf(Front).getAxis() == Direction.Axis.X;
        double HalfX = AlongX ? SlotWidth * 0.5 : SlotDepth;
        double HalfZ = AlongX ? SlotDepth : SlotWidth * 0.5;
        return new AABB(Pos.getX() + Centre.x - HalfX, Pos.getY() + 0.02, Pos.getZ() + Centre.z - HalfZ,
                Pos.getX() + Centre.x + HalfX, Pos.getY() + SlotTop, Pos.getZ() + Centre.z + HalfZ);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (Stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(World.getBlockEntity(Pos) instanceof LabTubeHolderBlockEntity Holder)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        int Aimed = slotFromHit(State, Pos, Hit.getLocation());
        if (LabVials.isVial(Stack)) {
            Holder.insert(User, Stack, Aimed);
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (LabVials.isSterileSwab(Stack)) {
            Holder.refuseSwab(User, Aimed);
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (LabVials.isUsedSwab(Stack)) {
            Holder.pour(User, Hand, Aimed);
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof LabTubeHolderBlockEntity Holder)) {
            return InteractionResult.PASS;
        }
        int Aimed = slotFromHit(State, Pos, Hit.getLocation());
        if (User.isSecondaryUseActive()) {
            Holder.toggleSeal(User, Aimed);
        } else {
            Holder.take(User, Aimed);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World.getBlockEntity(Pos) instanceof LabTubeHolderBlockEntity Holder) {
            Holder.dropAll(World, Pos);
            World.updateNeighbourForOutputSignal(Pos, this);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof LabTubeHolderBlockEntity Holder ? Holder.getFilled() : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new LabTubeHolderBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, LabwareRegistry.TubeHolderEntity.get(), LabTubeHolderBlockEntity::serverTick);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.lab_tube_holder.tooltip", LabTubeHolderBlockEntity.SlotCount).withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.lab_tube_holder.aim").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.lab_tube_holder.insert").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.lab_tube_holder.take").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.lab_tube_holder.seal").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.lab_tube_holder.pour").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.lab_tube_holder.source", (int) LabExposure.Radius).withStyle(ChatFormatting.RED));
        }
    }
}
