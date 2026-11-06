package net.rai.scp_unleashed.scp018.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp018.BallPath;
import net.rai.scp_unleashed.scp018.BallSegment;
import net.rai.scp_unleashed.scp018.BallTuning;
import net.rai.scp_unleashed.scp018.Scp018Ball;
import net.rai.scp_unleashed.scp018.Scp018Registry;

public final class BallEffects {
    public static final double TrailSpacing = 0.22;
    public static final int MaxTrailPerTick = 40;
    public static final double SoundGap = 0.9;
    public static final double SoundLead = 0.5;
    public static final int PuffCount = 4;
    public static final double SilentEntry = 0.04;
    public static final double SoftEntry = 0.6;
    public static final double SplashPerSpeed = 6.0;
    public static final int MaxSplash = 30;
    public static final double BubbleSpeed = 0.12;
    public static final float BubbleChance = 0.6f;

    private BallEffects() {
    }

    public static BallClientData data(Scp018Ball Ball) {
        if (Ball.clientData() instanceof BallClientData Data) {
            return Data;
        }
        BallClientData Made = new BallClientData(Ball.getUUID().getLeastSignificantBits());
        Ball.setClientData(Made);
        return Made;
    }

    public static void tick(Scp018Ball Ball) {
        Level World = Ball.level();
        BallPath Path = Ball.path();
        if (Path.isEmpty()) {
            return;
        }
        BallClientData Data = data(Ball);
        double Time = BallClock.tick(World);
        Ball.setClientTime(Time);
        Vec3 Here = Path.positionAt(Time);
        if (Here != null) {
            Ball.setCentre(Here);
        }
        Ball.setDeltaMovement(Path.velocityAt(Time));
        double Shown = Time - BallClock.DisplayDelay;
        if (Double.isNaN(Data.EventTime)) {
            Data.EventTime = Shown + SoundLead;
            Data.TrailTime = Shown;
        }
        bounces(World, Path, Data, Shown + SoundLead);
        trail(World, Path, Data, Shown);
        whoosh(Ball, Data);
        Path.prune(Shown);
    }

    private static void bounces(Level World, BallPath Path, BallClientData Data, double Until) {
        if (Until <= Data.EventTime) {
            return;
        }
        BallSegment Loudest = null;
        BallSegment Splash = null;
        for (BallSegment Segment : Path.endingBetween(Data.EventTime, Until)) {
            if (Segment.isBounce() && (Loudest == null || speedAtEnd(Path, Segment) > speedAtEnd(Path, Loudest))) {
                Loudest = Segment;
            }
            if (Segment.Kind() == BallSegment.KindSplash && (Splash == null || speedAtEnd(Path, Segment) > speedAtEnd(Path, Splash))) {
                Splash = Segment;
            }
        }
        Data.EventTime = Until;
        if (Splash != null) {
            splash(World, Splash.end(), speedAtEnd(Path, Splash));
        }
        if (Loudest == null || Until - Data.LastSoundTime < SoundGap) {
            return;
        }
        Data.LastSoundTime = Until;
        Vec3 Point = Loudest.end();
        double Speed = speedAtEnd(Path, Loudest);
        bounceSound(World, Point, Speed, Loudest.inWater());
        if (Speed >= BallTuning.DamageSpeed && !Loudest.inWater()) {
            puff(World, Point, Speed);
        }
    }

    private static double speedAtEnd(BallPath Path, BallSegment Segment) {
        return Segment.Duration() > BallSegment.TinyDuration ? Segment.endVelocity().length() : Path.speedAt(Segment.endTime() - 1.0e-3);
    }

    public static void splash(Level World, Vec3 Point, double Speed) {
        if (Speed < SilentEntry) {
            return;
        }
        RandomSource Random = World.random;
        float Fraction = (float) Mth.clamp(Speed / BallTuning.ExplodeSpeed, 0.0, 1.0);
        if (Speed < SoftEntry) {
            play(World, Point, SoundEvents.FISHING_BOBBER_SPLASH, 0.25f + (float) (Speed / SoftEntry) * 0.35f, 1.1f + Random.nextFloat() * 0.2f);
        } else {
            play(World, Point, Speed >= BallTuning.DamageSpeed ? SoundEvents.PLAYER_SPLASH_HIGH_SPEED : SoundEvents.GENERIC_SPLASH, 0.35f + Fraction * 0.65f,
                    1.25f - Fraction * 0.35f + (Random.nextFloat() - 0.5f) * 0.1f);
        }
        int Drops = Math.min(MaxSplash, 2 + (int) (Speed * SplashPerSpeed));
        double Spread = 0.15 + Fraction * 0.35;
        for (int Index = 0; Index < Drops; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Reach = Random.nextDouble() * Spread;
            World.addParticle(ParticleTypes.SPLASH, Point.x + Math.cos(Angle) * Reach, Point.y + 0.05, Point.z + Math.sin(Angle) * Reach,
                    Math.cos(Angle) * 0.05 * Fraction, 0.1 + Fraction * 0.2, Math.sin(Angle) * 0.05 * Fraction);
        }
        for (int Index = 0; Index < Drops / 2; Index++) {
            World.addParticle(ParticleTypes.BUBBLE, Point.x + (Random.nextDouble() - 0.5) * 0.3, Point.y - 0.1 - Random.nextDouble() * 0.3,
                    Point.z + (Random.nextDouble() - 0.5) * 0.3, 0.0, 0.02, 0.0);
        }
    }

    public static void bounceSound(Level World, Vec3 Point, double Speed, boolean Muffled) {
        RandomSource Random = World.random;
        float Fraction = (float) Mth.clamp(Speed / BallTuning.ExplodeSpeed, 0.0, 1.0);
        float Pitch = (0.8f + Fraction * 1.1f + (Random.nextFloat() - 0.5f) * 0.06f) * (Muffled ? 0.8f : 1.0f);
        float Volume = (0.4f + Fraction * 0.6f) * (Muffled ? 0.45f : 1.0f);
        play(World, Point, Scp018Registry.Bounce.get(), Volume, Pitch);
        play(World, Point, Scp018Registry.BounceSquish.get(), Volume * 0.8f, Pitch);
        play(World, Point, Scp018Registry.BounceRing.get(), Volume * 0.7f, Pitch);
        if (Speed >= BallTuning.WhooshSpeed) {
            play(World, Point, Scp018Registry.BounceSnap.get(), Volume * (0.4f + Fraction * 0.6f), Math.min(2.0f, 0.9f + Fraction * 0.8f));
        }
    }

    private static void play(Level World, Vec3 Point, SoundEvent Sound, float Volume, float Pitch) {
        World.playLocalSound(Point.x, Point.y, Point.z, Sound, SoundSource.NEUTRAL, Volume, Pitch, false);
    }

    private static void puff(Level World, Vec3 Point, double Speed) {
        RandomSource Random = World.random;
        float Strength = (float) Mth.clamp(0.4 + BallTuning.danger(Speed) * 0.6, 0.0, 1.0);
        for (int Index = 0; Index < PuffCount; Index++) {
            World.addParticle(Scp018Registry.Trail.get(), Point.x + (Random.nextDouble() - 0.5) * 0.2, Point.y + (Random.nextDouble() - 0.5) * 0.2,
                    Point.z + (Random.nextDouble() - 0.5) * 0.2, Strength, 0.0, 0.0);
        }
    }

    private static void trail(Level World, BallPath Path, BallClientData Data, double Until) {
        double From = Data.TrailTime;
        Data.TrailTime = Math.max(From, Until);
        if (Until <= From || Minecraft.getInstance().options.particles().get().getId() > 1) {
            return;
        }
        double Time = From;
        int Count = 0;
        while (Time < Until && Count < MaxTrailPerTick) {
            double Speed = Path.speedAt(Time);
            BallSegment Segment = Path.at(Time);
            if (Segment != null && Segment.inWater()) {
                if (Speed >= BubbleSpeed && World.random.nextFloat() < BubbleChance) {
                    Vec3 Point = Path.positionAt(Time);
                    if (Point != null) {
                        World.addParticle(ParticleTypes.BUBBLE, Point.x, Point.y, Point.z, 0.0, 0.02, 0.0);
                        Count++;
                    }
                }
                Time += 0.25;
                continue;
            }
            if (Speed < BallTuning.TrailSpeed) {
                Time += 0.25;
                continue;
            }
            Vec3 Point = Path.positionAt(Time);
            if (Point != null) {
                float Strength = (float) Mth.clamp((Speed - BallTuning.TrailSpeed) / (BallTuning.ExplodeSpeed - BallTuning.TrailSpeed), 0.0, 1.0);
                World.addParticle(Scp018Registry.Trail.get(), Point.x, Point.y, Point.z, 0.25 + Strength * 0.75, 0.0, 0.0);
                Count++;
            }
            Time += TrailSpacing / Speed;
        }
    }

    private static void whoosh(Scp018Ball Ball, BallClientData Data) {
        if (Ball.getDeltaMovement().length() < BallTuning.WhooshSpeed) {
            return;
        }
        if (Data.Whoosh == null || Data.Whoosh.finished()) {
            Data.Whoosh = new BallWhooshSound(Ball);
            Minecraft.getInstance().getSoundManager().play(Data.Whoosh);
        }
    }
}
