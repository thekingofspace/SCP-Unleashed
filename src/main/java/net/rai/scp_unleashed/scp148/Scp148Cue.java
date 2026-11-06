package net.rai.scp_unleashed.scp148;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.joml.Vector3f;

public final class Scp148Cue {
    public static final DustParticleOptions Shimmer = new DustParticleOptions(new Vector3f(0.42f, 0.9f, 0.62f), 0.6f);
    public static final DustParticleOptions ShimmerDeep = new DustParticleOptions(new Vector3f(0.2f, 0.62f, 0.42f), 0.5f);
    public static final int RingPoints = 14;
    public static final double RingRadius = 0.55;
    public static final float ChimeVolume = 0.7f;
    public static final float WardedVolume = 0.5f;

    private Scp148Cue() {
    }

    public static void play(LivingEntity Target) {
        Level World = Target.level();
        double X = Target.getX();
        double Y = Target.getEyeY() + 0.15;
        double Z = Target.getZ();
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp148Registry.ChimeLayers) {
            if (Target instanceof ServerPlayer User) {
                User.playNotifySound(Layer.get(), SoundSource.PLAYERS, ChimeVolume, Pitch);
            } else if (World instanceof ServerLevel) {
                World.playSound(null, X, Y, Z, Layer.get(), SoundSource.NEUTRAL, WardedVolume, Pitch);
            } else {
                World.playLocalSound(X, Y, Z, Layer.get(), SoundSource.PLAYERS, ChimeVolume, Pitch, false);
            }
        }
        for (int Index = 0; Index < RingPoints; Index++) {
            double Angle = Math.PI * 2.0 * Index / RingPoints;
            double PointX = X + Math.cos(Angle) * RingRadius;
            double PointZ = Z + Math.sin(Angle) * RingRadius;
            double PointY = Y + (Index % 2 == 0 ? 0.05 : -0.05);
            DustParticleOptions Speck = Index % 3 == 0 ? ShimmerDeep : Shimmer;
            if (World instanceof ServerLevel Server) {
                Server.sendParticles(Speck, PointX, PointY, PointZ, 1, 0.0, 0.0, 0.0, 0.0);
            } else {
                World.addParticle(Speck, PointX, PointY, PointZ, 0.0, 0.01, 0.0);
            }
        }
    }
}
