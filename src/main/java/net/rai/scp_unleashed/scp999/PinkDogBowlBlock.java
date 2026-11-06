package net.rai.scp_unleashed.scp999;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PinkDogBowlBlock extends Block {
    public static final MapCodec<PinkDogBowlBlock> BlockCodec = simpleCodec(PinkDogBowlBlock::new);
    public static final int Capacity = 8;
    public static final int Levels = 4;
    public static final IntegerProperty Candy = IntegerProperty.create("candy", 0, Capacity);
    public static final VoxelShape Shape = Block.box(2.5, 0.0, 2.5, 13.5, 5.0, 13.5);
    public static final float FillVolume = 0.7f;
    public static final float TakeVolume = 0.6f;
    public static final int Crumbs = 4;

    public PinkDogBowlBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Candy, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Candy);
    }

    public static int level(int Count) {
        if (Count <= 0) {
            return 0;
        }
        if (Count <= 2) {
            return 1;
        }
        return Count <= 5 ? 2 : 3;
    }

    public static int candy(BlockState State) {
        return State.getBlock() instanceof PinkDogBowlBlock ? State.getValue(Candy) : 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return Block.canSupportCenter(World, Pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side == Direction.DOWN && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Scp999Registry.Candy.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (State.getValue(Candy) >= Capacity) {
            return ItemInteractionResult.CONSUME;
        }
        if (!World.isClientSide) {
            World.setBlock(Pos, State.setValue(Candy, State.getValue(Candy) + 1), Block.UPDATE_ALL);
            Stack.consume(1, User);
            play(World, Pos, Scp999Registry.FillLayers, FillVolume, 0.95f + World.getRandom().nextFloat() * 0.1f);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (State.getValue(Candy) <= 0) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            World.setBlock(Pos, State.setValue(Candy, State.getValue(Candy) - 1), Block.UPDATE_ALL);
            ItemStack Taken = new ItemStack(Scp999Registry.Candy.get());
            if (!User.getInventory().add(Taken)) {
                User.drop(Taken, false);
            }
            play(World, Pos, Scp999Registry.TakeLayers, TakeVolume, 0.95f + World.getRandom().nextFloat() * 0.1f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static boolean eatOne(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (!(State.getBlock() instanceof PinkDogBowlBlock) || State.getValue(Candy) <= 0) {
            return false;
        }
        World.setBlock(Pos, State.setValue(Candy, State.getValue(Candy) - 1), Block.UPDATE_ALL);
        Vec3 Centre = Vec3.atBottomCenterOf(Pos).add(0.0, 0.25, 0.0);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Scp999Registry.Candy.get())), Centre.x, Centre.y, Centre.z, Crumbs, 0.12, 0.05, 0.12, 0.05);
        return true;
    }

    private static void play(Level World, BlockPos Pos, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return Math.round(State.getValue(Candy) * 15.0f / Capacity);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Next, boolean Moved) {
        if (!State.is(Next.getBlock()) && !World.isClientSide && State.getValue(Candy) > 0) {
            Block.popResource(World, Pos, new ItemStack(Scp999Registry.Candy.get(), State.getValue(Candy)));
        }
        super.onRemove(State, World, Pos, Next, Moved);
    }
}
