package net.rai.scp_unleashed.space;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.wire.OutletMounts;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AirInfuserBlock extends BaseEntityBlock {
    public static final MapCodec<AirInfuserBlock> BlockCodec = simpleCodec(AirInfuserBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final BooleanProperty Loaded = BooleanProperty.create("loaded");
    public static final int ActiveLight = 4;
    public static final List<AABB> Boxes = List.of(
            new AABB(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
            new AABB(1.0, 2.0, 1.0, 15.0, 13.0, 15.0),
            new AABB(3.0, 13.0, 3.0, 13.0, 16.0, 13.0));
    private static final VoxelShape Body = shapeOf(Boxes);
    private static final Map<Direction, VoxelShape> Supports = new EnumMap<>(Direction.class);

    static {
        List<Direction> Mounts = OutletMounts.faces(Boxes, 1, 1, 1, 0, 0, 0);
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (Direction Side : Mounts) {
                if (Side != Front) {
                    Shape = Shapes.or(Shape, shapeOf(List.of(OutletMounts.plate(Side))));
                }
            }
            Supports.put(Front, Shape.optimize());
        }
    }

    public AirInfuserBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Active, false).setValue(Loaded, false));
    }

    private static VoxelShape shapeOf(List<AABB> Parts) {
        VoxelShape Shape = Shapes.empty();
        for (AABB Part : Parts) {
            Shape = Shapes.or(Shape, Block.box(Part.minX, Part.minY, Part.minZ, Part.maxX, Part.maxY, Part.maxZ));
        }
        return Shape.optimize();
    }

    public static List<Direction> outletFaces(BlockState State) {
        return OutletMounts.faces(Boxes, 1, 1, 1, 0, 0, 0).stream().filter(Side -> Side != State.getValue(Facing)).toList();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Active, Loaded);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Body;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Supports.get(State.getValue(Facing));
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
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && World.getBlockEntity(Pos) instanceof AirInfuserBlockEntity Machine) {
            Machine.dropContents(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof AirInfuserBlockEntity Machine) {
            User.openMenu(Machine);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.air_infuser.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.air_infuser.detail.rate", AirInfuserBlockEntity.fillTicks(0) / 20, AirInfuserBlockEntity.WorkDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.machine.speed_upgrades", UpgradeType.Speed.getLimit()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.air_infuser.detail.hoppers").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.air_infuser.detail.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.air_infuser.detail.atmosphere").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new AirInfuserBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, SpaceRegistry.AirInfuserEntity.get(), World.isClientSide ? AirInfuserBlockEntity::clientTick : AirInfuserBlockEntity::serverTick);
    }
}
