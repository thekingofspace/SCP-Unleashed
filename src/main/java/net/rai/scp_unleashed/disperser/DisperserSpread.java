package net.rai.scp_unleashed.disperser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;

import java.util.List;
import java.util.function.Predicate;

public final class DisperserSpread {
    private DisperserSpread() {
    }

    public static void release(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, LabOrganism Organism, double Reach) {
        LabOrganisms.spread(World, Source, Ignored, Organism, Reach, null);
    }

    public static ColorParticleOption haze(int Colour) {
        return ColorParticleOption.create(DisperserRegistry.Haze.get(), Colour | 0xFF000000);
    }

    public static void spray(ServerLevel World, Vec3 From, int Colour, int Count, double Speed, double Rise) {
        RandomSource Random = World.getRandom();
        ColorParticleOption Mist = haze(Colour);
        double Offset = Random.nextDouble();
        for (int Index = 0; Index < Count; Index++) {
            double Angle = (Index + Offset + Random.nextDouble() * 0.5) / Count * Mth.TWO_PI;
            double Pace = Speed * (0.45 + Random.nextDouble() * 0.55);
            double Lift = Rise * (0.3 + Random.nextDouble() * 1.4);
            World.sendParticles(Mist, From.x, From.y, From.z, 0, Math.cos(Angle) * Pace, Lift, Math.sin(Angle) * Pace, 1.0);
        }
    }

    public static void play(Level World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        RandomSource Random = World.getRandom();
        float Pitch = 0.94F + Random.nextFloat() * 0.12F;
        for (Holder<SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer, SoundSource.BLOCKS, Volume, Pitch);
        }
    }
}
