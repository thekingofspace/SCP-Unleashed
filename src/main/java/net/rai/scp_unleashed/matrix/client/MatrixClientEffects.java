package net.rai.scp_unleashed.matrix.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlockEntity;
import net.rai.scp_unleashed.matrix.MatrixRegistry;
import net.rai.scp_unleashed.matrix.MatrixTuning;
import net.rai.scp_unleashed.rift.RiftParticleOptions;
import net.rai.scp_unleashed.rift.RiftRegistry;

import java.util.Map;
import java.util.WeakHashMap;

public final class MatrixClientEffects {
    public static final float HumStart = 0.03f;
    public static final double HumLoopSeconds = 5.8;
    public static final double TickSeconds = 0.05;
    public static final double BoundaryBand = 14.0;
    public static final double BoundaryArc = 16.0;
    public static final double BoundarySight = 20.0;
    public static final float BoundaryRate = 0.65f;
    public static final int TetherSteps = 30;

    private static final Map<InstabilityMatrixBlockEntity, Chorus> Hums = new WeakHashMap<>();

    private MatrixClientEffects() {
    }

    public static void tick(InstabilityMatrixBlockEntity Matrix) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Matrix.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        hum(Game, Matrix);
        RandomSource Random = World.getRandom();
        int Surge = Matrix.takePendingSurge();
        if (Surge >= 0) {
            surge(World, Matrix, Surge, Random);
        }
        float Glow = Matrix.getGlow(1.0f);
        if (Glow <= 0.05f) {
            return;
        }
        int Colour = colour(Matrix);
        float Resonance = Matrix.getShownResonance(1.0f);
        Vec3 Centre = Matrix.getCoreCentre();
        if (Random.nextFloat() < Glow * (0.06f + 0.22f * Resonance)) {
            arc(World, Centre, Colour, Random);
        }
        if (Random.nextFloat() < Glow * (0.1f + 0.25f * Resonance)) {
            inflow(World, Centre, Colour, Random);
        }
        if (Matrix.getStatus().isRunning()) {
            boundary(Game, World, Centre, Colour, Glow, Random);
        }
    }

    private static int colour(InstabilityMatrixBlockEntity Matrix) {
        int Red = Mth.clamp(Math.round(Matrix.getTintRed() * 255.0f), 0, 255);
        int Green = Mth.clamp(Math.round(Matrix.getTintGreen() * 255.0f), 0, 255);
        int Blue = Mth.clamp(Math.round(Matrix.getTintBlue() * 255.0f), 0, 255);
        return 0xFF000000 | (Red << 16) | (Green << 8) | Blue;
    }

    private static void hum(Minecraft Game, InstabilityMatrixBlockEntity Matrix) {
        SoundManager Sounds = Game.getSoundManager();
        Chorus Playing = Hums.get(Matrix);
        if (Matrix.getGlow(1.0f) > HumStart) {
            if (Playing == null || !Playing.isPlaying(Sounds)) {
                Playing = new Chorus(Matrix);
                Playing.start(Sounds);
                Hums.put(Matrix, Playing);
            }
        }
        if (Playing != null) {
            Playing.tick(Sounds);
        }
    }

    public static double humLoudness(InstabilityMatrixBlockEntity Matrix) {
        return 0.65 * Mth.clamp(Matrix.getGlow(1.0f), 0.0f, 1.0f);
    }

    public static double humPitch(InstabilityMatrixBlockEntity Matrix) {
        return 1.1 + 0.2 * Matrix.getShownResonance(1.0f) + 0.04 * Matrix.getFlash(1.0f);
    }

    public static double bedLoudness(InstabilityMatrixBlockEntity Matrix) {
        return Mth.clamp(Matrix.getGlow(1.0f), 0.0f, 1.0f) * (0.4 + 0.6 * Matrix.getShownResonance(1.0f));
    }

    public static double bedPitch(InstabilityMatrixBlockEntity Matrix) {
        return 0.92 + 0.34 * Matrix.getShownResonance(1.0f);
    }

    private static void arc(Level World, Vec3 Centre, int Colour, RandomSource Random) {
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Lift = (Random.nextDouble() - 0.5) * 0.35;
        double Start = 0.26 + Random.nextDouble() * 0.04;
        Vec3 From = Centre.add(Math.cos(Angle) * Start, Lift, Math.sin(Angle) * Start);
        Vec3 Out = new Vec3(Math.cos(Angle), (Random.nextDouble() - 0.5) * 0.6, Math.sin(Angle)).scale(0.05 + Random.nextDouble() * 0.05);
        for (int Count = 0; Count < 2; Count++) {
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 0.6f), From.x, From.y, From.z, Out.x, Out.y, Out.z);
        }
    }

    private static void inflow(Level World, Vec3 Centre, int Colour, RandomSource Random) {
        double Theta = Random.nextDouble() * Math.PI * 2.0;
        double Lift = Random.nextDouble() * 2.0 - 1.0;
        double Ring = Math.sqrt(1.0 - Lift * Lift);
        double Radius = 0.55 + Random.nextDouble() * 0.5;
        double X = Centre.x + Math.cos(Theta) * Ring * Radius;
        double Y = Centre.y + Lift * Radius * 0.7;
        double Z = Centre.z + Math.sin(Theta) * Ring * Radius;
        double Speed = 0.018;
        World.addParticle(new RiftParticleOptions(RiftRegistry.RiftMote.get(), Colour, -1, 0.7f), X, Y, Z,
                (Centre.x - X) * Speed - Math.sin(Theta) * 0.006, (Centre.y - Y) * Speed, (Centre.z - Z) * Speed + Math.cos(Theta) * 0.006);
    }

    private static void boundary(Minecraft Game, Level World, Vec3 Centre, int Colour, float Glow, RandomSource Random) {
        Vec3 Eye = Game.gameRenderer.getMainCamera().getPosition();
        double DeltaX = Eye.x - Centre.x;
        double DeltaZ = Eye.z - Centre.z;
        double Flat = Math.sqrt(DeltaX * DeltaX + DeltaZ * DeltaZ);
        double Radius = MatrixTuning.FieldRadius;
        if (Math.abs(Flat - Radius) > BoundaryBand || Math.abs(Eye.y - Centre.y) > MatrixTuning.VerticalReach + 8) {
            return;
        }
        double Facing = Math.atan2(DeltaZ, DeltaX);
        double Spread = BoundaryArc / Radius;
        float Rate = BoundaryRate * Glow;
        int Count = Mth.floor(Rate) + (Random.nextFloat() < Rate - Mth.floor(Rate) ? 1 : 0);
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Facing + (Random.nextDouble() * 2.0 - 1.0) * Spread;
            double X = Centre.x + Math.cos(Angle) * Radius;
            double Z = Centre.z + Math.sin(Angle) * Radius;
            double Y = Mth.clamp(Eye.y - 3.0 + Random.nextDouble() * 9.0, Centre.y - MatrixTuning.VerticalReach, Centre.y + MatrixTuning.VerticalReach);
            if (Eye.distanceToSqr(X, Y, Z) > BoundarySight * BoundarySight) {
                continue;
            }
            double Drift = 0.006 + Random.nextDouble() * 0.006;
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftMote.get(), Colour, -1, 0.85f), X, Y, Z,
                    -Math.sin(Angle) * Drift, 0.004 + Random.nextDouble() * 0.006, Math.cos(Angle) * Drift);
        }
    }

    private static void surge(Level World, InstabilityMatrixBlockEntity Matrix, int Param, RandomSource Random) {
        Vec3 Centre = Matrix.getCoreCentre();
        Vec3 Target = Centre.add(InstabilityMatrixBlockEntity.surgeOffset(Param));
        FragmentRarity Tier = InstabilityMatrixBlockEntity.surgeTier(Param);
        int Colour = Tier.getColour();
        World.addParticle(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, 0.35f), Centre.x, Centre.y, Centre.z, 0.0, 0.0, 0.0);
        for (int Count = 0; Count < 18; Count++) {
            Vec3 Heading = new Vec3(Random.nextGaussian(), Random.nextGaussian() * 0.7, Random.nextGaussian()).normalize();
            double Speed = 0.12 + Random.nextDouble() * 0.2;
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 0.9f), Centre.x, Centre.y, Centre.z,
                    Heading.x * Speed, Heading.y * Speed, Heading.z * Speed);
        }
        Vec3 Span = Target.subtract(Centre);
        double Length = Span.length();
        if (Length < 0.5) {
            return;
        }
        Vec3 Side = Span.cross(new Vec3(0.0, 1.0, 0.0));
        Side = Side.lengthSqr() < 1.0e-6 ? new Vec3(1.0, 0.0, 0.0) : Side.normalize();
        Vec3 Lift = Side.cross(Span).normalize();
        int Steps = Mth.clamp((int) Math.round(Length * 1.4), 8, TetherSteps * 2);
        double Phase = Random.nextDouble() * Math.PI * 2.0;
        for (int Step = 0; Step <= Steps; Step++) {
            double Along = (double) Step / Steps;
            double Sway = Math.sin(Along * Math.PI) * (0.4 + Length * 0.03);
            Vec3 Point = Centre.add(Span.scale(Along))
                    .add(Side.scale(Math.sin(Along * 9.0 + Phase) * Sway * 0.5))
                    .add(Lift.scale(Math.cos(Along * 7.0 + Phase) * Sway * 0.35));
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 1.1f), Point.x, Point.y, Point.z,
                    (Random.nextDouble() - 0.5) * 0.02, (Random.nextDouble() - 0.5) * 0.02, (Random.nextDouble() - 0.5) * 0.02);
            if (Step % 3 == 0) {
                World.addParticle(new RiftParticleOptions(RiftRegistry.RiftMote.get(), Colour, -1, 1.2f), Point.x, Point.y, Point.z,
                        Span.x / Length * 0.03, Span.y / Length * 0.03, Span.z / Length * 0.03);
            }
        }
        World.addParticle(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, 0.6f + 0.15f * Tier.getTier()), Target.x, Target.y, Target.z, 0.0, 0.0, 0.0);
    }

    private static final class Chorus {
        private final InstabilityMatrixBlockEntity Matrix;
        private final MatrixHumSound Lead;
        private final MatrixHumSound Bed;
        private MatrixHumSound Follower;
        private double Travel;

        Chorus(InstabilityMatrixBlockEntity Matrix) {
            this.Matrix = Matrix;
            Lead = new MatrixHumSound(Matrix, MatrixRegistry.Hum.get(), MatrixClientEffects::humLoudness, MatrixClientEffects::humPitch);
            Bed = new MatrixHumSound(Matrix, MatrixRegistry.HumBed.get(), MatrixClientEffects::bedLoudness, MatrixClientEffects::bedPitch);
        }

        void start(SoundManager Sounds) {
            Sounds.play(Lead);
            Sounds.play(Bed);
        }

        boolean isPlaying(SoundManager Sounds) {
            return Sounds.isActive(Lead);
        }

        void tick(SoundManager Sounds) {
            if (Follower != null || Lead.isStopped()) {
                return;
            }
            Travel += Lead.getPitchNow() * TickSeconds;
            if (Travel >= HumLoopSeconds * 0.5) {
                Follower = new MatrixHumSound(Matrix, MatrixRegistry.Hum.get(), MatrixClientEffects::humLoudness, MatrixClientEffects::humPitch);
                Sounds.play(Follower);
            }
        }
    }
}
