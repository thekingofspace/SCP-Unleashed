package net.rai.scp_unleashed.daevite.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class DaeviteAltarBlock extends DaeviteDecorBlock {
    public static final MapCodec<DaeviteAltarBlock> AltarCodec = simpleCodec(DaeviteAltarBlock::new);
    public static final DustColorTransitionOptions Cinder = new DustColorTransitionOptions(new Vector3f(0.95f, 0.32f, 0.08f), new Vector3f(0.18f, 0.02f, 0.02f), 0.7f);

    public DaeviteAltarBlock(Properties Settings) {
        super(Settings, DaeviteDecor.Altar);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return AltarCodec;
    }

    @Override
    public void ambient(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.45;
        double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.45;
        if (Random.nextInt(3) == 0) {
            World.addParticle(Cinder, X, Pos.getY() + 0.9, Z, 0.0, 0.05, 0.0);
        }
        if (Random.nextInt(9) == 0) {
            World.addParticle(ParticleTypes.SMALL_FLAME, X, Pos.getY() + 0.88, Z, 0.0, 0.01, 0.0);
        }
    }
}
