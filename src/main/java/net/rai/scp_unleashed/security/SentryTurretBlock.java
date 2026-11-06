package net.rai.scp_unleashed.security;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SentryTurretBlock extends BaseEntityBlock {
    public static final MapCodec<SentryTurretBlock> BlockCodec = simpleCodec(SentryTurretBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Hanging = BlockStateProperties.HANGING;
    public static final EnumProperty<TurretLight> Light = EnumProperty.create("light", TurretLight.class);
    public static final int EventFire = 1;
    public static final int EventDry = 2;

    private static final VoxelShape FloorShape = Shapes.or(box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0), box(2.0, 2.0, 2.0, 14.0, 10.0, 14.0),
            box(3.0, 10.0, 3.0, 13.0, 12.0, 13.0), box(4.0, 12.0, 4.0, 12.0, 16.0, 12.0));
    private static final VoxelShape CeilingShape = Shapes.or(box(1.0, 14.0, 1.0, 15.0, 16.0, 15.0), box(2.0, 6.0, 2.0, 14.0, 14.0, 14.0),
            box(3.0, 4.0, 3.0, 13.0, 6.0, 13.0), box(4.0, 0.0, 4.0, 12.0, 4.0, 12.0));
    private static final Map<Direction, VoxelShape> Supports = new EnumMap<>(Direction.class);

    static {
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                if (Side != Front) {
                    Shape = Shapes.or(Shape, plate(Side));
                }
            }
            Supports.put(Front, Shape.optimize());
        }
    }

    public SentryTurretBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Hanging, false).setValue(Light, TurretLight.Off));
    }

    private static VoxelShape plate(Direction Side) {
        return switch (Side) {
            case NORTH -> box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
            case SOUTH -> box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
            case WEST -> box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
            default -> box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
        };
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Hanging, Light);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Hanging) ? CeilingShape : FloorShape;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Supports.get(State.getValue(Facing));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        boolean Ceiling = Context.getClickedFace() == Direction.DOWN;
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Hanging, Ceiling);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (!World.isClientSide && Placer instanceof Player User && World.getBlockEntity(Pos) instanceof SentryTurretBlockEntity Turret) {
            Turret.setOwner(User);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (TurretRound.of(Stack) == null || User.isSecondaryUseActive()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (World.getBlockEntity(Pos) instanceof SentryTurretBlockEntity Turret) {
            if (!Turret.canConfigure(User)) {
                Turret.refuse(User);
                return ItemInteractionResult.CONSUME;
            }
            Turret.loadFrom(User, Stack);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (World.getBlockEntity(Pos) instanceof SentryTurretBlockEntity Turret && User instanceof ServerPlayer Viewer) {
            if (Turret.canConfigure(Viewer)) {
                Viewer.openMenu(Turret, Pos);
            } else {
                Turret.refuse(Viewer);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World.getBlockEntity(Pos) instanceof SentryTurretBlockEntity Turret) {
            for (ItemStack Stack : Turret.drops()) {
                Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Stack);
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
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
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof SentryTurretBlockEntity Turret ? Turret.comparatorSignal() : 0;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.sentry_turret.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.sentry_turret.detail.power", SentryTurretBlockEntity.IdleDraw, SentryTurretBlockEntity.FireDraw)
                    .withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.sentry_turret.detail.range", (int) SentryTurretBlockEntity.Range).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.sentry_turret.detail.ammo").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.sentry_turret.detail.owner").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new SentryTurretBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, SecurityRegistry.SentryTurretEntity.get(),
                World.isClientSide ? SentryTurretBlockEntity::clientTick : SentryTurretBlockEntity::serverTick);
    }
}
