package net.rai.scp_unleashed.scp073;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp073Reflection {
    public static final float Share = 1.0f;
    public static final int RingGlints = 10;
    public static final double RingRadius = 0.55;
    public static final double RingSpeed = 0.06;
    public static final int TrailLimit = 24;
    public static final double TrailSpacing = 1.25;
    public static final double FrontReach = 0.45;
    public static final float ReflectVolume = 1.0f;
    public static final float RecoilVolume = 0.9f;
    private static boolean Reflecting;

    private Scp073Reflection() {
    }

    public static void reflect(ServerLevel World, Scp073Cain Cain, DamageSource Source, float Amount) {
        if (Reflecting || Amount <= 0.0f) {
            return;
        }
        Entity Cause = Source.getEntity();
        if (!(Cause instanceof LivingEntity Attacker) || Attacker == Cain || Attacker instanceof Scp073Cain || Attacker.isRemoved() || !Attacker.isAlive()
                || Attacker.level() != World) {
            return;
        }
        flash(World, Cain, Attacker);
        Reflecting = true;
        try {
            Attacker.hurt(Cain.damageSources().source(Scp073Registry.ReflectedDamage, Cain), Amount * Share);
        } finally {
            Reflecting = false;
        }
    }

    public static Vec3 centre(Entity Target) {
        return Target.getBoundingBox().getCenter();
    }

    public static void flash(ServerLevel World, Scp073Cain Cain, LivingEntity Attacker) {
        Vec3 Here = centre(Cain);
        Vec3 There = centre(Attacker);
        Vec3 Flat = new Vec3(There.x - Here.x, 0.0, There.z - Here.z);
        Vec3 Toward = Flat.lengthSqr() > 1.0e-4 ? Flat.normalize() : Vec3.ZERO;
        burst(World, Here.add(Toward.scale(FrontReach)), RingGlints);
        burst(World, There.subtract(Toward.scale(FrontReach)), RingGlints / 2);
        Vec3 Span = There.subtract(Here);
        double Length = Span.length();
        if (Length > TrailSpacing * 1.5) {
            int Steps = Math.min(TrailLimit, (int) (Length / TrailSpacing));
            for (int Step = 1; Step < Steps; Step++) {
                Vec3 Point = Here.add(Span.scale((double) Step / Steps));
                World.sendParticles(Scp073Registry.Glint.get(), Point.x, Point.y, Point.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        play(World, Here, Scp073Registry.ReflectLayers, ReflectVolume, Pitch);
        play(World, There, Scp073Registry.RecoilLayers, RecoilVolume, Pitch);
    }

    private static void burst(ServerLevel World, Vec3 Point, int Glints) {
        World.sendParticles(Scp073Registry.Flash.get(), Point.x, Point.y, Point.z, 1, 0.0, 0.0, 0.0, 0.0);
        for (int Index = 0; Index < Glints; Index++) {
            double Angle = Math.PI * 2.0 * Index / Glints + World.getRandom().nextDouble() * 0.3;
            double Dx = Math.cos(Angle);
            double Dz = Math.sin(Angle);
            World.sendParticles(Scp073Registry.Glint.get(), Point.x + Dx * RingRadius, Point.y + (World.getRandom().nextDouble() - 0.5) * 0.6, Point.z + Dz * RingRadius, 0,
                    Dx, 0.15, Dz, RingSpeed);
        }
    }

    private static void play(ServerLevel World, Vec3 Point, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Point.x, Point.y, Point.z, Layer.get(), SoundSource.NEUTRAL, Volume, Pitch);
        }
    }
}
