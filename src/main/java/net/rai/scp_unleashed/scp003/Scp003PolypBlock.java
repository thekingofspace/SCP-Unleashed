package net.rai.scp_unleashed.scp003;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp003PolypBlock extends Block {
    public static final MapCodec<Scp003PolypBlock> BlockCodec = simpleCodec(Scp003PolypBlock::new);
    public static final int MaxAge = 3;
    public static final IntegerProperty Age = IntegerProperty.create("age", 0, MaxAge);
    public static final BooleanProperty Withered = BooleanProperty.create("withered");
    public static final int MinGrowTicks = 700;
    public static final int MaxGrowTicks = 1100;
    public static final int MinWitherTicks = 12;
    public static final int MaxWitherTicks = 50;
    private static final VoxelShape[] GrowthShapes = {
            Block.box(5.0, 0.0, 5.0, 11.0, 4.0, 11.0),
            Block.box(4.0, 0.0, 4.0, 12.0, 7.0, 12.0),
            Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0)
    };

    public Scp003PolypBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Age, 0).setValue(Withered, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Age, Withered);
    }

    public static boolean isGrown(BlockState State) {
        return State.getBlock() instanceof Scp003PolypBlock && State.getValue(Age) >= MaxAge && !State.getValue(Withered);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return GrowthShapes[State.getValue(Age)];
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return World.getBlockState(Pos.below()).isFaceSturdy(World, Pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side == Direction.DOWN && !canSurvive(State, World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && !World.isClientSide) {
            World.scheduleTick(Pos, this, delay(World.random, State));
        }
    }

    private static int delay(RandomSource Random, BlockState State) {
        if (State.getValue(Withered)) {
            return MinWitherTicks + Random.nextInt(MaxWitherTicks - MinWitherTicks + 1);
        }
        return MinGrowTicks + Random.nextInt(MaxGrowTicks - MinGrowTicks + 1);
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Withered)) {
            World.levelEvent(2001, Pos, Block.getId(State));
            World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        int Current = State.getValue(Age);
        if (Current >= MaxAge) {
            return;
        }
        BlockState Grown = State.setValue(Age, Current + 1);
        World.setBlock(Pos, Grown, Block.UPDATE_ALL);
        World.playSound(null, Pos, Scp003Registry.PolypGrow.get(), SoundSource.BLOCKS, 0.6f, 0.8f + Current * 0.1f);
        World.sendParticles(Scp003Registry.Flesh.get(), Pos.getX() + 0.5, Pos.getY() + 0.2 + Current * 0.15, Pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.03);
        if (Current + 1 < MaxAge) {
            World.scheduleTick(Pos, this, delay(Random, Grown));
        }
    }

    public static void wither(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (!(State.getBlock() instanceof Scp003PolypBlock Polyp) || State.getValue(Withered)) {
            return;
        }
        World.setBlock(Pos, State.setValue(Withered, true), Block.UPDATE_ALL);
        World.scheduleTick(Pos, Polyp, MinWitherTicks + World.random.nextInt(MaxWitherTicks - MinWitherTicks + 1));
        World.playSound(null, Pos, Scp003Registry.PolypWither.get(), SoundSource.BLOCKS, 0.7f, 0.9f + World.random.nextFloat() * 0.2f);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (isGrown(State) && Random.nextInt(5) == 0) {
            World.addParticle(Scp003Registry.Spore.get(), Pos.getX() + 0.25 + Random.nextDouble() * 0.5, Pos.getY() + 0.8, Pos.getZ() + 0.25 + Random.nextDouble() * 0.5,
                    0.0, 0.015, 0.0);
        }
    }
}
