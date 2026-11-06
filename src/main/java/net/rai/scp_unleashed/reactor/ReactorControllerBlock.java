package net.rai.scp_unleashed.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReactorControllerBlock extends BaseEntityBlock {
    public static final MapCodec<ReactorControllerBlock> BlockCodec = simpleCodec(ReactorControllerBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Status = IntegerProperty.create("status", 0, 3);
    public static final int ScreenLight = 5;

    public ReactorControllerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Status, 0));
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Status) > 0 ? ScreenLight : 0;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Status);
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
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (World.isClientSide || !(World.getBlockEntity(Pos) instanceof ReactorControllerBlockEntity Controller)) {
            return;
        }
        GlobalPos Bound = Stack.get(ReactorRegistry.ReactorLink.get());
        Component Message;
        if (Bound != null && Bound.dimension() == World.dimension() && ReactorControllerBlockEntity.inRange(Pos, Bound.pos())) {
            Controller.link(Bound.pos());
            Message = Component.translatable("message.scp_unleashed.reactor_controller.linked").withStyle(ChatFormatting.GREEN);
        } else {
            BlockPos Touching = ReactorControllerBlockEntity.touchingTower(World, Pos);
            if (Touching != null) {
                Controller.link(Touching);
                Message = Component.translatable("message.scp_unleashed.reactor_controller.linked").withStyle(ChatFormatting.GREEN);
            } else {
                Message = Component.translatable(Bound != null ? "message.scp_unleashed.reactor_controller.too_far" : "message.scp_unleashed.reactor_controller.unlinked",
                        ReactorControllerBlockEntity.LinkRange).withStyle(ChatFormatting.GOLD);
            }
        }
        if (Placer instanceof Player User) {
            User.displayClientMessage(Message, true);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof ReactorControllerBlockEntity Controller) {
            User.openMenu(Controller);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbour, BlockPos NeighbourPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbour, NeighbourPos, MovedByPiston);
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof ReactorControllerBlockEntity Controller) {
            Controller.setPowered(World.hasNeighborSignal(Pos));
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof ReactorControllerBlockEntity Controller ? Controller.getComparator() : 0;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.reactor_controller.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.reactor_controller.detail.1").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.reactor_controller.detail.2", ReactorControllerBlockEntity.LinkRange).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.reactor_controller.detail.3").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.reactor_controller.detail.4").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ReactorControllerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, ReactorRegistry.ControllerEntity.get(), ReactorControllerBlockEntity::serverTick);
    }
}
