package net.rai.scp_unleashed.scp063;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp063RestingBlock extends BaseEntityBlock {
    public static final MapCodec<Scp063RestingBlock> BlockCodec = simpleCodec(Scp063RestingBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final ResourceLocation Contents = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp063_toothbrush");
    private static final VoxelShape Outline = Block.box(3.0, 0.0, 3.0, 13.0, 2.5, 13.0);

    public Scp063RestingBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    public static InteractionResult placeFrom(UseOnContext Context) {
        Player User = Context.getPlayer();
        Level World = Context.getLevel();
        ItemStack Stack = Context.getItemInHand();
        BlockPlaceContext Place = new BlockPlaceContext(Context);
        BlockPos Pos = Place.getClickedPos();
        BlockState State = Scp063Registry.Resting.get().getStateForPlacement(Place);
        if (!Place.canPlace() || State == null || !State.canSurvive(World, Pos) || !World.isUnobstructed(State, Pos, CollisionContext.empty())) {
            return InteractionResult.FAIL;
        }
        if (User != null && !User.mayUseItemAt(Pos, Context.getClickedFace(), Stack)) {
            return InteractionResult.FAIL;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!World.setBlock(Pos, State, Block.UPDATE_ALL_IMMEDIATE)) {
            return InteractionResult.FAIL;
        }
        if (World.getBlockEntity(Pos) instanceof Scp063RestingBlockEntity Rest) {
            Rest.rest(Stack, User);
        }
        World.playSound(null, Pos, State.getSoundType(World, Pos, User).getPlaceSound(), SoundSource.BLOCKS, 0.8f, 1.2f);
        World.gameEvent(GameEvent.BLOCK_PLACE, Pos, GameEvent.Context.of(User, State));
        Stack.consume(1, User);
        return InteractionResult.CONSUME;
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
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Below = Pos.below();
        return !World.isOutsideBuildHeight(Below) && !World.getBlockState(Below).isAir() && World.getFluidState(Pos).isEmpty();
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
    protected List<ItemStack> getDrops(BlockState State, LootParams.Builder Params) {
        BlockEntity Found = Params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack Dropped = Found instanceof Scp063RestingBlockEntity Rest ? Rest.kept().copy() : new ItemStack(Scp063Registry.Toothbrush.get());
        return super.getDrops(State, Params.withDynamicDrop(Contents, Output -> {
            if (!Dropped.isEmpty()) {
                Output.accept(Dropped.copy());
            }
        }));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState State, HitResult Target, LevelReader World, BlockPos Pos, Player User) {
        return World.getBlockEntity(Pos) instanceof Scp063RestingBlockEntity Rest ? Rest.kept().copy() : new ItemStack(Scp063Registry.Toothbrush.get());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp063RestingBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp063Registry.RestingEntity.get(), Scp063RestingBlockEntity::serverTick);
    }
}
