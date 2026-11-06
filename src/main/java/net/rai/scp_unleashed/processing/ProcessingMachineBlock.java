package net.rai.scp_unleashed.processing;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.upgrade.UpgradeType;

import java.util.List;

public abstract class ProcessingMachineBlock extends BaseEntityBlock {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Active = BooleanProperty.create("active");

    protected ProcessingMachineBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Active, false));
    }

    protected abstract String getMachineName();

    protected abstract int getWorkDraw();

    protected abstract void appendDetails(List<Component> Lines);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Active);
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
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World.getBlockEntity(Pos) instanceof ProcessingMachineBlockEntity Machine) {
            Machine.dropContents(World, Pos);
            if (World instanceof ServerLevel ServerWorld) {
                Machine.onBroken(ServerWorld, Pos);
            }
            World.updateNeighbourForOutputSignal(Pos, this);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof ProcessingMachineBlockEntity Machine) {
            User.openMenu(Machine);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof ProcessingMachineBlockEntity Machine ? ItemHandlerHelper.calcRedstoneFromInventory(Machine.getStorage()) : 0;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed." + getMachineName() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed." + getMachineName() + ".draw_active", getWorkDraw()).withStyle(ChatFormatting.GOLD));
            appendDetails(Lines);
            Lines.add(Component.translatable("block.scp_unleashed.machine.speed_upgrades", UpgradeType.Speed.getLimit()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.processing.outlet").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.processing.automation").withStyle(ChatFormatting.GOLD));
        }
    }
}
