package net.rai.scp_unleashed.scp2845;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp2845StoneBlock extends Block {
    public static final MapCodec<Scp2845StoneBlock> Codec = simpleCodec(Scp2845StoneBlock::new);
    public static final BooleanProperty Sealed = BooleanProperty.create("sealed");
    public static final int CheckTicks = 100;
    public static final VoxelShape Shape = Shapes.or(Block.box(5.0, 0.0, 5.0, 11.0, 2.5, 11.0), Block.box(2.0, 2.5, 4.0, 14.0, 9.5, 12.0),
            Block.box(4.0, 2.0, 2.5, 12.0, 10.0, 13.5), Block.box(4.5, 10.0, 4.5, 11.5, 12.5, 11.5), Block.box(5.5, 12.5, 5.5, 10.5, 14.0, 10.5),
            Block.box(6.5, 14.0, 6.5, 9.5, 15.0, 9.5));

    public Scp2845StoneBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Sealed, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Sealed);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected float getDestroyProgress(BlockState State, Player User, BlockGetter World, BlockPos Pos) {
        return State.getValue(Sealed) ? 0.0f : super.getDestroyProgress(State, User, World, Pos);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState Old, boolean Moving) {
        super.onPlace(State, World, Pos, Old, Moving);
        if (!World.isClientSide && !Old.is(this)) {
            World.scheduleTick(Pos, this, CheckTicks);
        }
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        boolean Held = Scp2845Records.get(World.getServer()).holdsStone(World, Pos);
        if (Held != State.getValue(Sealed)) {
            World.setBlock(Pos, State.setValue(Sealed, Held), Block.UPDATE_ALL);
        }
        World.scheduleTick(Pos, this, CheckTicks);
    }

    public static void seal(Level World, BlockPos Pos, boolean Value) {
        BlockState State = World.getBlockState(Pos);
        if (State.getBlock() instanceof Scp2845StoneBlock && State.getValue(Sealed) != Value) {
            World.setBlock(Pos, State.setValue(Sealed, Value), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Sealed) || Random.nextInt(3) != 0) {
            return;
        }
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        World.addParticle(ParticleTypes.END_ROD, Pos.getX() + 0.5 + Math.cos(Angle) * 0.45, Pos.getY() + 0.3 + Random.nextDouble() * 0.7,
                Pos.getZ() + 0.5 + Math.sin(Angle) * 0.45, 0.0, 0.01 + Random.nextDouble() * 0.01, 0.0);
    }
}
