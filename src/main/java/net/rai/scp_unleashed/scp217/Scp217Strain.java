package net.rai.scp_unleashed.scp217;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.labware.LabPetriDishBlockEntity;

public final class Scp217Strain {
    public static final float Chance = 0.01f;
    public static final int Glints = 6;
    public static final double GlintSpread = 0.14;

    private Scp217Strain() {
    }

    public static boolean perfects(float Roll) {
        return Roll < Chance;
    }

    public static boolean roll(RandomSource Random) {
        return perfects(Random.nextFloat());
    }

    public static void announce(ServerLevel World, BlockPos Pos) {
        double X = Pos.getX() + 0.5;
        double Y = Pos.getY() + LabPetriDishBlockEntity.SurfaceHeight;
        double Z = Pos.getZ() + 0.5;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp217Registry.CultureLayers) {
            World.playSound(null, X, Y, Z, Layer.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        World.sendParticles(ParticleTypes.WAX_ON, X, Y + 0.05, Z, Glints, GlintSpread, 0.02, GlintSpread, 0.02);
    }
}
