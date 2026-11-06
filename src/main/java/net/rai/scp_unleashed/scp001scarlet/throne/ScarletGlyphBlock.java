package net.rai.scp_unleashed.scp001scarlet.throne;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class ScarletGlyphBlock extends Block {
    public static final DustParticleOptions Ember = new DustParticleOptions(new Vector3f(0.75f, 0.08f, 0.06f), 0.8f);

    public ScarletGlyphBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(14) != 0) {
            return;
        }
        double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 1.1;
        double Y = Pos.getY() + Random.nextDouble();
        double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 1.1;
        World.addParticle(Ember, X, Y, Z, 0.0, 0.01, 0.0);
    }
}
