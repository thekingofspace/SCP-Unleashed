package net.rai.scp_unleashed.scp001scarlet.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletGlyphBlock;

public class ScarletBrazierBlock extends Block {
    public static final BooleanProperty Lit = BooleanProperty.create("lit");
    public static final VoxelShape Shape = Shapes.or(Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0), Block.box(2.0, 6.0, 2.0, 14.0, 10.0, 14.0));

    public ScarletBrazierBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Lit, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Lit);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return World.getBlockState(Pos.below()).isFaceSturdy(World, Pos.below(), Direction.UP);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Lit)) {
            return;
        }
        double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.5;
        double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.5;
        World.addParticle(ScarletGlyphBlock.Ember, X, Pos.getY() + 0.7, Z, 0.0, 0.04, 0.0);
        if (Random.nextInt(3) == 0) {
            World.addParticle(ParticleTypes.SMOKE, X, Pos.getY() + 0.75, Z, 0.0, 0.03, 0.0);
        }
        if (Random.nextInt(6) == 0) {
            World.addParticle(ParticleTypes.SMALL_FLAME, X, Pos.getY() + 0.68, Z, 0.0, 0.01, 0.0);
        }
    }
}
