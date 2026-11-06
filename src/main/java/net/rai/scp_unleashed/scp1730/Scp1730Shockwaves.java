package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Scp1730Shockwaves {
    public static final double Speed = 0.55;
    public static final double MaxRadius = 14.0;
    public static final double Band = 0.75;
    public static final double Clearance = 0.9;
    public static final float Damage = 8.0f;
    private static final List<Ring> Rings = new ArrayList<>();

    public static final class Ring {
        public final ServerLevel World;
        public final Vec3 Centre;
        public final int Source;
        public double Radius;
        public final Set<UUID> Hit = new HashSet<>();

        Ring(ServerLevel World, Vec3 Centre, int Source) {
            this.World = World;
            this.Centre = Centre;
            this.Source = Source;
        }
    }

    private Scp1730Shockwaves() {
    }

    public static Ring start(ServerLevel World, Vec3 Centre, @Nullable Entity Source) {
        Ring Made = new Ring(World, Centre, Source == null ? -1 : Source.getId());
        synchronized (Rings) {
            Rings.add(Made);
        }
        return Made;
    }

    public static boolean struck(Ring Wave, Player Each) {
        double Dx = Each.getX() - Wave.Centre.x;
        double Dz = Each.getZ() - Wave.Centre.z;
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        return Math.abs(Flat - Wave.Radius) <= Band && Each.getY() - Wave.Centre.y < Clearance && Each.getY() - Wave.Centre.y > -1.5;
    }

    public static void tick() {
        synchronized (Rings) {
            Iterator<Ring> Each = Rings.iterator();
            while (Each.hasNext()) {
                Ring Wave = Each.next();
                Wave.Radius += Speed;
                if (Wave.Radius > MaxRadius) {
                    Each.remove();
                    continue;
                }
                advance(Wave);
            }
        }
    }

    private static void advance(Ring Wave) {
        ServerLevel World = Wave.World;
        Entity Source = Wave.Source >= 0 ? World.getEntity(Wave.Source) : null;
        DamageSource Hurt = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp1730Registry.GuardianDamage), Source);
        AABB Span = new AABB(Wave.Centre, Wave.Centre).inflate(Wave.Radius + Band, 2.0, Wave.Radius + Band);
        for (Player Target : World.getEntitiesOfClass(Player.class, Span, Player::isAlive)) {
            if (Wave.Hit.contains(Target.getUUID()) || !struck(Wave, Target)) {
                continue;
            }
            Wave.Hit.add(Target.getUUID());
            if (Target.hurt(Hurt, Damage)) {
                Target.setDeltaMovement(Target.getDeltaMovement().add(0.0, 0.55, 0.0));
                Target.hurtMarked = true;
            }
        }
        int Points = (int) Math.max(8, Wave.Radius * 5.0);
        for (int Index = 0; Index < Points; Index++) {
            double Angle = Index / (double) Points * Math.PI * 2.0;
            double X = Wave.Centre.x + Math.cos(Angle) * Wave.Radius;
            double Z = Wave.Centre.z + Math.sin(Angle) * Wave.Radius;
            BlockState Ground = World.getBlockState(BlockPos.containing(X, Wave.Centre.y - 0.5, Z));
            if (!Ground.isAir() && Index % 2 == 0) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), X, Wave.Centre.y + 0.15, Z, 2, 0.1, 0.05, 0.1, 0.15);
            }
            if (Index % 3 == 0) {
                World.sendParticles(ParticleTypes.CLOUD, X, Wave.Centre.y + 0.2, Z, 1, 0.0, 0.05, 0.0, 0.01);
            }
        }
    }

    public static void clear() {
        synchronized (Rings) {
            Rings.clear();
        }
    }

    public static int active() {
        synchronized (Rings) {
            return Rings.size();
        }
    }
}
