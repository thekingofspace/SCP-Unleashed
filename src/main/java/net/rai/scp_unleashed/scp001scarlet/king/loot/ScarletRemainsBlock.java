package net.rai.scp_unleashed.scp001scarlet.king.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ScarletRemainsBlock extends Block {
    public static final VoxelShape Shape = Block.box(1.0, 0.0, 1.0, 15.0, 6.0, 15.0);

    public ScarletRemainsBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(4) == 0) {
            World.addParticle(ParticleTypes.SMOKE, Pos.getX() + 0.2 + Random.nextDouble() * 0.6, Pos.getY() + 0.4, Pos.getZ() + 0.2 + Random.nextDouble() * 0.6, 0.0, 0.02, 0.0);
        }
        if (Random.nextInt(9) == 0) {
            World.addParticle(ParticleTypes.SMALL_FLAME, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.35, 0.0, 0.01, 0.0);
        }
    }
}
