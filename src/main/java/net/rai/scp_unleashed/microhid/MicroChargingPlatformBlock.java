package net.rai.scp_unleashed.microhid;

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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.OutletMounts;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MicroChargingPlatformBlock extends BaseEntityBlock {
    public static final MapCodec<MicroChargingPlatformBlock> BlockCodec = simpleCodec(MicroChargingPlatformBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<MicroChargingStatus> Status = EnumProperty.create("status", MicroChargingStatus.class);
    public static final int Segments = 4;
    public static final IntegerProperty Charge = IntegerProperty.create("charge", 0, Segments);
    public static final int ChargingLight = 6;
    public static final List<AABB> Boxes = List.of(
            new AABB(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
            new AABB(1.0, 2.0, 1.0, 15.0, 4.0, 12.0),
            new AABB(1.0, 2.0, 12.0, 15.0, 14.0, 15.0),
            new AABB(6.0, 6.0, 15.0, 10.0, 10.0, 16.0));
    private static final Map<Direction, VoxelShape> Bodies = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> Supports = new EnumMap<>(Direction.class);

    static {
        List<Direction> Mounts = OutletMounts.faces(Boxes, 1, 1, 1, 0, 0, 0);
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            VoxelShape Body = Shapes.empty();
            for (AABB Part : Boxes) {
                Body = Shapes.or(Body, turned(Part, Front));
            }
            Bodies.put(Front, Body.optimize());
            VoxelShape Support = Shapes.empty();
            for (Direction Side : Mounts) {
                Support = Shapes.or(Support, turned(OutletMounts.plate(Side), Front));
            }
            Supports.put(Front, Support.optimize());
        }
    }

    public MicroChargingPlatformBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Status, MicroChargingStatus.Empty).setValue(Charge, 0));
    }

    private static VoxelShape turned(AABB Part, Direction Front) {
        double MinX = Part.minX;
        double MinZ = Part.minZ;
        double MaxX = Part.maxX;
        double MaxZ = Part.maxZ;
        return switch (Front) {
            case EAST -> Block.box(16.0 - MaxZ, Part.minY, MinX, 16.0 - MinZ, Part.maxY, MaxX);
            case SOUTH -> Block.box(16.0 - MaxX, Part.minY, 16.0 - MaxZ, 16.0 - MinX, Part.maxY, 16.0 - MinZ);
            case WEST -> Block.box(MinZ, Part.minY, 16.0 - MaxX, MaxZ, Part.maxY, 16.0 - MinX);
            default -> Block.box(MinX, Part.minY, MinZ, MaxX, Part.maxY, MaxZ);
        };
    }

    public static List<Direction> outletFaces(BlockState State) {
        return List.of(State.getValue(Facing).getOpposite());
    }

    public static int segmentsFor(ItemStack Docked) {
        if (Docked.isEmpty()) {
            return 0;
        }
        int Stored = MicroHidEnergy.get(Docked);
        return Stored <= 0 ? 0 : Math.max(1, Math.min(Segments, (int) Math.ceil(Stored * (double) Segments / MicroHidEnergy.Capacity)));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Status, Charge);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Bodies.get(State.getValue(Facing));
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
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!MicroHidItem.isMicroHid(Stack) || !(World.getBlockEntity(Pos) instanceof MicroChargingPlatformBlockEntity Platform)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            ItemStack Previous = Platform.undock();
            Platform.dock(Stack.copyWithCount(1));
            Stack.shrink(1);
            if (!Previous.isEmpty()) {
                User.setItemInHand(Hand, Previous);
            }
            MicroHidSounds.play(World, Pos.getCenter(), MicroHidSounds.Dock);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!User.getMainHandItem().isEmpty() || !(World.getBlockEntity(Pos) instanceof MicroChargingPlatformBlockEntity Platform) || Platform.getDocked().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            User.setItemInHand(InteractionHand.MAIN_HAND, Platform.undock());
            MicroHidSounds.play(World, Pos.getCenter(), MicroHidSounds.Undock);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && World.getBlockEntity(Pos) instanceof MicroChargingPlatformBlockEntity Platform) {
            Platform.dropContents(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.micro_charging_platform.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.micro_charging_platform.detail.rate", MicroHidEnergy.ChargeDraw,
                    Math.round(MicroHidEnergy.chargeSeconds())).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.micro_charging_platform.detail.dock").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.micro_charging_platform.detail.wiring").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new MicroChargingPlatformBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, MicroHidRegistry.PlatformEntity.get(),
                World.isClientSide ? MicroChargingPlatformBlockEntity::clientTick : MicroChargingPlatformBlockEntity::serverTick);
    }
}
