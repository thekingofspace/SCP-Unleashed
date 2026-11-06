package net.rai.scp_unleashed.door;

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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

public class DoorLeverBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<DoorLeverBlock> BlockCodec = simpleCodec(DoorLeverBlock::new);
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;

    public DoorLeverBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACE, AttachFace.WALL).setValue(FACING, Direction.NORTH).setValue(Powered, false));
    }

    public static int rotationX(BlockState State) {
        return DoorLeverGeometry.rotationX(State.getValue(FACE));
    }

    public static int rotationY(BlockState State) {
        return DoorLeverGeometry.rotationY(State.getValue(FACE), State.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACE, FACING, Powered);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return DoorLeverGeometry.outlineOf(State.getValue(FACE), State.getValue(FACING), State.getValue(Powered));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            if (World.getBlockEntity(Pos) instanceof DoorLeverBlockEntity Lever) {
                Lever.predict(!State.getValue(Powered));
            }
            return InteractionResult.SUCCESS;
        }
        pull(State, World, Pos, User);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onExplosionHit(BlockState State, Level World, BlockPos Pos, Explosion Blast, BiConsumer<ItemStack, BlockPos> Drops) {
        if (Blast.canTriggerBlocks()) {
            pull(State, World, Pos, null);
        }
        super.onExplosionHit(State, World, Pos, Blast, Drops);
    }

    public void pull(BlockState State, Level World, BlockPos Pos, @Nullable Player User) {
        BlockState Thrown = State.cycle(Powered);
        World.setBlock(Pos, Thrown, Block.UPDATE_ALL);
        updateNeighbours(Thrown, World, Pos);
        World.gameEvent(User, Thrown.getValue(Powered) ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, Pos);
    }

    private void updateNeighbours(BlockState State, Level World, BlockPos Pos) {
        World.updateNeighborsAt(Pos, this);
        World.updateNeighborsAt(Pos.relative(getConnectedDirection(State).getOpposite()), this);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!MovedByPiston && !State.is(NewState.getBlock()) && State.getValue(Powered)) {
            updateNeighbours(State, World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected boolean isSignalSource(BlockState State) {
        return true;
    }

    @Override
    protected int getSignal(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return State.getValue(Powered) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return State.getValue(Powered) && getConnectedDirection(State) == Side ? 15 : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new DoorLeverBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (!World.isClientSide || Type != DoorRegistry.DoorLeverEntity.get()) {
            return null;
        }
        return (Ticked, Pos, Current, Owner) -> DoorLeverBlockEntity.clientTick(Ticked, Pos, Current, (DoorLeverBlockEntity) Owner);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.door_lever.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.door_lever.signal").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.door_lever.mount").withStyle(ChatFormatting.GOLD));
        }
    }
}
