package net.rai.scp_unleashed.scp003;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class Scp003PlateBlock extends Block {
    public static final MapCodec<Scp003PlateBlock> BlockCodec = simpleCodec(Scp003PlateBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Seated = BooleanProperty.create("seated");

    public Scp003PlateBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Seated, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Seated);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Scp003Shapes.of(State.getValue(Seated) ? Scp003Shapes.PlateSeated : Scp003Shapes.PlateBase, State.getValue(Facing));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Scp003Shapes.of(Scp003Shapes.PlateBase, State.getValue(Facing));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
        return Placed.canSurvive(Context.getLevel(), Context.getClickedPos()) ? Placed : null;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return canSupportCenter(World, Pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side == Direction.DOWN && !canSurvive(State, World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Scp003Registry.BoardItem.get()) || State.getValue(Seated)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            World.setBlock(Pos, State.setValue(Seated, true), Block.UPDATE_ALL);
            Stack.consume(1, User);
            World.playSound(null, Pos, Scp003Registry.BoardSeat.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
            World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
            if (World instanceof ServerLevel Server) {
                Server.sendParticles(ParticleTypes.ELECTRIC_SPARK, Pos.getX() + 0.5, Pos.getY() + 0.2, Pos.getZ() + 0.5, 8, 0.25, 0.02, 0.25, 0.02);
            }
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!State.getValue(Seated) || !User.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide && User instanceof ServerPlayer Reader) {
            World.setBlock(Pos, State.setValue(Seated, false), Block.UPDATE_ALL);
            StrangerEncounters.give(Reader, new ItemStack(Scp003Registry.BoardItem.get()));
            World.playSound(null, Pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.6f, 0.7f);
            World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
            if (World instanceof ServerLevel Server) {
                Server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Scp003Registry.Board.get().defaultBlockState()), Pos.getX() + 0.5, Pos.getY() + 0.2,
                        Pos.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.05);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }
}
