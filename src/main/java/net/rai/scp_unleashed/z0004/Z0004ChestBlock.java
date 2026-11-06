package net.rai.scp_unleashed.z0004;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Z0004ChestBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<Z0004ChestBlock> BlockCodec = simpleCodec(Z0004ChestBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;
    public static final int OpenTicks = 40;
    private static final VoxelShape NorthSouth = Block.box(1.0, 0.0, 2.5, 15.0, 13.5, 13.5);
    private static final VoxelShape EastWest = Block.box(2.5, 0.0, 1.0, 13.5, 13.5, 15.0);

    public Z0004ChestBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false).setValue(Waterlogged, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open, Waterlogged);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        FluidState Fluid = Context.getLevel().getFluidState(Context.getClickedPos());
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Waterlogged, Fluid.getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return State.getValue(Waterlogged) ? Fluids.WATER.getSource(false) : super.getFluidState(State);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (State.getValue(Waterlogged)) {
            World.scheduleTick(Pos, Fluids.WATER, Fluids.WATER.getTickDelay(World));
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Facing).getAxis() == Direction.Axis.Z ? NorthSouth : EastWest;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (World instanceof ServerLevel Server && World.getBlockEntity(Pos) instanceof Z0004ChestBlockEntity Chest) {
            if (!State.getValue(Open)) {
                World.setBlock(Pos, State.setValue(Open, true), Block.UPDATE_ALL);
            }
            World.scheduleTick(Pos, this, OpenTicks);
            Chest.open(Server, User);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Open)) {
            World.setBlock(Pos, State.setValue(Open, false), Block.UPDATE_ALL);
            Z0004Registry.play(World, Pos.getCenter(), Z0004Registry.CloseLayers, SoundSource.BLOCKS, 0.8f, 0.7f + Random.nextFloat() * 0.1f);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moved) {
        if (!State.is(Replaced.getBlock()) && World instanceof ServerLevel Server && World.getBlockEntity(Pos) instanceof Z0004ChestBlockEntity Chest) {
            Chest.abandon(Server);
        }
        super.onRemove(State, World, Pos, Replaced, Moved);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.z0004_chest.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.z0004_chest.detail").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Z0004ChestBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Z0004Registry.ChestEntity.get(), Z0004ChestBlockEntity::serverTick);
    }
}
