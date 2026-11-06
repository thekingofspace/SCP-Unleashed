package net.rai.scp_unleashed.labware;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class LabViralStorageBlock extends BaseEntityBlock {
    public static final MapCodec<LabViralStorageBlock> BlockCodec = simpleCodec(LabViralStorageBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Chilled = BooleanProperty.create("chilled");
    public static final BooleanProperty Hatch = BooleanProperty.create("hatch");
    public static final BooleanProperty Venting = BooleanProperty.create("venting");
    private static final Map<Direction, VoxelShape> Outlines = LabwareShapes.horizontal(LabwareShapes.ViralStorage);
    private static final Map<Direction, VoxelShape> Supports = LabwareShapes.horizontal(LabwareShapes.ViralStorageSupport);

    public LabViralStorageBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Chilled, false).setValue(Hatch, false).setValue(Venting, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Chilled, Hatch, Venting);
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
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter Getter, BlockPos Pos) {
        return Supports.get(State.getValue(Facing));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof LabViralStorageBlockEntity Locker)) {
            return InteractionResult.PASS;
        }
        boolean Open = State.getValue(Hatch);
        if (User.isSecondaryUseActive()) {
            if (!Open) {
                return InteractionResult.PASS;
            }
            if (World instanceof ServerLevel Server) {
                Locker.toggleLid(Server, Pos);
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!Open) {
            if (World instanceof ServerLevel Server) {
                Locker.toggleLid(Server, Pos);
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!World.isClientSide) {
            User.openMenu(Locker);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World.getBlockEntity(Pos) instanceof LabViralStorageBlockEntity Locker) {
            Locker.dropContents(World, Pos);
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
        return World.getBlockEntity(Pos) instanceof LabViralStorageBlockEntity Locker ? Locker.getSignal() : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new LabViralStorageBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide) {
            return createTickerHelper(Type, LabwareRegistry.ViralStorageEntity.get(), LabViralStorageBlockEntity::clientTick);
        }
        return createTickerHelper(Type, LabwareRegistry.ViralStorageEntity.get(), LabViralStorageBlockEntity::serverTick);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.tooltip", LabViralStorageBlockEntity.SlotCount).withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.lid").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.rack").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.shut").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.power", LabViralStorageBlockEntity.PowerDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.port").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.chill").withStyle(ChatFormatting.AQUA));
            Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.vent", (int) LabExposure.Radius).withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.vials").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.lab_viral_storage.comparator").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
