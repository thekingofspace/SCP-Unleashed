package net.rai.scp_unleashed.scp469;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp469Effects {
    public static final int BurstFeathers = 140;
    public static final int ReformFeathers = 6;
    public static final double DriftChance = 0.06;
    public static final double HuntDrift = 0.12;
    public static final double LungeDrift = 0.45;

    private Scp469Effects() {
    }

    public static void layers(ServerLevel World, Entity Source, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        RandomSource Random = World.getRandom();
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            float Shift = Pitch * (0.94f + Random.nextFloat() * 0.12f);
            World.playSound(null, Source.getX(), Source.getY() + 1.0, Source.getZ(), Layer.get(), Source.getSoundSource(), Volume, Shift);
        }
    }

    public static void burstAround(ServerLevel World, Vec3 Centre, int Count, double Spread) {
        World.sendParticles(Scp469Registry.FeatherParticle.get(), Centre.x, Centre.y, Centre.z, Count, Spread, Spread * 0.8, Spread, 0.06);
    }

    public static void fold(ServerLevel World, Scp469Angel Creature, boolean Fold) {
        World.playSound(null, Creature.getX(), Creature.getY() + 1.0, Creature.getZ(), Scp469Registry.Fold.get(), Creature.getSoundSource(), 0.8f,
                (Fold ? 1.1f : 0.9f) * (0.95f + World.getRandom().nextFloat() * 0.1f));
        burstAround(World, Creature.position().add(0.0, 1.0, 0.0), Fold ? 8 : 5, 0.6);
    }

    public static void sweep(ServerLevel World, Scp469Angel Creature, Vec3 Facing) {
        Vec3 Side = new Vec3(-Facing.z, 0.0, Facing.x);
        for (int Index = -3; Index <= 3; Index++) {
            Vec3 Point = Creature.position().add(Facing.scale(1.6)).add(Side.scale(Index * 0.45)).add(0.0, 0.6 + World.getRandom().nextDouble() * 0.6, 0.0);
            World.sendParticles(Scp469Registry.FeatherParticle.get(), Point.x, Point.y, Point.z, 2, 0.15, 0.15, 0.15, 0.08);
        }
    }

    public static void glance(ServerLevel World, Scp469Angel Creature, @Nullable Vec3 From) {
        Vec3 Point = From == null ? Creature.position().add(0.0, 1.2, 0.0) : Creature.getBoundingBox().getCenter().add(From.subtract(Creature.getBoundingBox().getCenter())
                .normalize().scale(Creature.getBbWidth() * 0.5));
        World.playSound(null, Point.x, Point.y, Point.z, Scp469Registry.Glance.get(), Creature.getSoundSource(), 0.8f, 0.9f + World.getRandom().nextFloat() * 0.2f);
        burstAround(World, Point, 6, 0.2);
    }

    public static void hurtFeathers(ServerLevel World, Scp469Angel Creature) {
        burstAround(World, Creature.position().add(0.0, Creature.getBbHeight() * 0.55, 0.0), 8, 0.5);
    }

    public static void burst(ServerLevel World, Scp469Angel Creature) {
        Vec3 Centre = Creature.position().add(0.0, 1.1, 0.0);
        World.sendParticles(Scp469Registry.FeatherParticle.get(), Centre.x, Centre.y, Centre.z, BurstFeathers, 0.9, 0.7, 0.9, 0.22);
        layers(World, Creature, Scp469Registry.BurstLayers, 1.4f, 0.9f);
    }

    public static void reforming(ServerLevel World, Scp469Angel Creature, int Elapsed) {
        RandomSource Random = World.getRandom();
        Vec3 Centre = Creature.position().add(0.0, 1.0, 0.0);
        for (int Index = 0; Index < ReformFeathers; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = 2.5 + Random.nextDouble() * 2.0;
            Vec3 From = Centre.add(Math.cos(Angle) * Radius, Random.nextDouble() * 2.0 - 0.6, Math.sin(Angle) * Radius);
            Vec3 Pull = Centre.subtract(From).scale(0.08);
            World.sendParticles(Scp469Registry.FeatherParticle.get(), From.x, From.y, From.z, 0, Pull.x, Pull.y, Pull.z, 1.0);
        }
        if (Elapsed == 1) {
            layers(World, Creature, Scp469Registry.ReformLayers, 1.1f, 1.0f);
        }
    }

    public static void batter(ServerLevel World, BlockPos Door) {
        burstAround(World, Vec3.atCenterOf(Door).add(0.0, 0.5, 0.0), 4, 0.3);
    }

    public static void clientTick(Scp469Angel Creature) {
        Level World = Creature.level();
        if (!Creature.isAlive() || Creature.isReforming()) {
            return;
        }
        RandomSource Random = Creature.getRandom();
        double Chance = Creature.isLunging() ? LungeDrift : Creature.isHunting() ? HuntDrift : DriftChance;
        if (Creature.isEmbracing()) {
            Chance = LungeDrift;
        }
        if (Random.nextDouble() >= Chance) {
            return;
        }
        float Yaw = Creature.yBodyRot * Mth.DEG_TO_RAD;
        double Spread = Creature.isFolded() ? 0.4 : 1.6;
        double Across = (Random.nextDouble() - 0.5) * 2.0 * Spread;
        double Depth = (Random.nextDouble() - 0.5) * Spread;
        double X = Creature.getX() + Mth.cos(Yaw) * Across - Mth.sin(Yaw) * Depth;
        double Z = Creature.getZ() + Mth.sin(Yaw) * Across + Mth.cos(Yaw) * Depth;
        double Y = Creature.getY() + 0.6 + Random.nextDouble() * (Creature.getBbHeight() * 0.9);
        World.addParticle(Scp469Registry.FeatherParticle.get(), X, Y, Z, (Random.nextDouble() - 0.5) * 0.02, 0.01, (Random.nextDouble() - 0.5) * 0.02);
    }
}
