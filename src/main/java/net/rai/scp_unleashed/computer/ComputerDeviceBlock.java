package net.rai.scp_unleashed.computer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class ComputerDeviceBlock extends BaseEntityBlock {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;
    public static final String DrawKey = "block.scp_unleashed.network_object.draw";

    private final Map<Direction, VoxelShape> Outline;
    private final Map<Direction, VoxelShape> Support;

    protected ComputerDeviceBlock(Properties Properties, double[][] OutlineBoxes, double[][] SupportBoxes) {
        super(Properties);
        Outline = DeviceShapes.turned(OutlineBoxes);
        Support = DeviceShapes.turned(SupportBoxes);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Powered, false));
    }

    protected abstract String getKeyName();

    protected abstract int getDraw();

    protected List<String> getDetailKeys() {
        return List.of();
    }

    protected void appendStored(ItemStack Stack, List<Component> Lines) {
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Powered);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline.get(State.getValue(Facing));
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return Support.get(State.getValue(Facing));
    }

    @Nullable
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
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed." + getKeyName() + ".tooltip").withStyle(ChatFormatting.GRAY));
        appendStored(Stack, Lines);
        if (ItemTooltips.showDetails(Lines)) {
            for (String Key : getDetailKeys()) {
                Lines.add(Component.translatable(Key).withStyle(ChatFormatting.GOLD));
            }
            Lines.add(Component.translatable(DrawKey, getDraw()).withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide) {
            return null;
        }
        return (TickWorld, Pos, TickState, Entity) -> {
            if (TickWorld instanceof ServerLevel Server && Entity instanceof ComputerDeviceEntity Device) {
                Device.serverTick(Server, TickState);
            }
        };
    }

    public static int lightOf(BlockState State, int Lit) {
        return State.getValue(Powered) ? Lit : 0;
    }
}
