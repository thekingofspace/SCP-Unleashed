package net.rai.scp_unleashed.grenade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunGlass;
import net.rai.scp_unleashed.guns.GunsRegistry;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class GrenadeBlast {
    public static final double BlastLift = 0.2;
    public static final int MaxShrapnelLayers = 4;

    private GrenadeBlast() {
    }

    public static void detonate(ServerLevel World, GrenadeEntity Grenade) {
        Vec3 Centre = Grenade.position().add(0.0, BlastLift, 0.0);
        Entity Owner = Grenade.getOwner();
        DamageSource Blast = World.damageSources().explosion(Grenade, Owner);
        World.explode(Grenade, Blast, new Calculator(), Centre.x, Centre.y, Centre.z, GrenadeTuning.FragPower, false, Level.ExplosionInteraction.MOB,
                ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, GrenadeRegistry.FragBoom);
        GrenadeSounds.play(World, Centre, GrenadeRegistry.FragCrack, SoundSource.BLOCKS, 1.0F, 1.0F);
        World.sendParticles(GunsRegistry.Spark.get(), Centre.x, Centre.y, Centre.z, 40, 0.3, 0.2, 0.3, 0.6);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y + 0.3, Centre.z, 14, 0.6, 0.4, 0.6, 0.03);
        shrapnel(World, Grenade, Centre, Owner);
        Grenade.setDeltaMovement(Vec3.ZERO);
        Grenade.enterPhase(GrenadeEntity.PhaseSpent, GrenadeTuning.DebrisTicks);
    }

    public static void spentTick(ServerLevel World, GrenadeEntity Grenade) {
        if (Grenade.getPhaseTicks() == GrenadeTuning.DebrisTicks - GrenadeTuning.DebrisDelay) {
            GrenadeSounds.play(World, Grenade.centre(), GrenadeRegistry.FragDebris, SoundSource.BLOCKS, 0.9F, 1.0F);
        }
    }

    public static DamageSource shrapnelSource(ServerLevel World, GrenadeEntity Grenade, Vec3 Centre) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(GrenadeRegistry.ShrapnelDamage), Grenade,
                Grenade.getOwner(), Centre);
    }

    private static void shrapnel(ServerLevel World, GrenadeEntity Grenade, Vec3 Centre, Entity Owner) {
        DamageSource Source = shrapnelSource(World, Grenade, Centre);
        double Reach = GrenadeTuning.ShrapnelReach;
        Set<BlockPos> Panes = new LinkedHashSet<>();
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, new AABB(Centre, Centre).inflate(Reach),
                Living -> Living.isAlive() && !Living.isSpectator())) {
            Vec3 Aim = Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0);
            double Distance = Centre.distanceTo(Aim);
            if (Distance > Reach) {
                continue;
            }
            float Carry = carry(World, Grenade, Centre, Aim, Panes);
            float Amount = GrenadeTuning.ShrapnelDamage * (float) (1.0 - Distance / Reach) * Carry;
            if (Amount < 0.5F) {
                continue;
            }
            Target.invulnerableTime = 0;
            Target.hurt(Source, Amount);
        }
        int Broken = 0;
        for (BlockPos Pos : Panes) {
            if (Broken >= GrenadeTuning.MaxShrapnelShatter) {
                break;
            }
            if (Grenade.mayShatter(World, Pos, true) && World.destroyBlock(Pos, false, Grenade)) {
                Broken++;
            }
        }
    }

    private static float carry(ServerLevel World, Entity Grenade, Vec3 From, Vec3 To, Set<BlockPos> Panes) {
        Vec3 Heading = To.subtract(From).normalize();
        double Length = From.distanceToSqr(To);
        float Carry = 1.0F;
        Vec3 Start = From;
        for (int Layer = 0; Layer < MaxShrapnelLayers; Layer++) {
            BlockHitResult Hit = World.clip(new ClipContext(Start, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Grenade));
            if (Hit.getType() != HitResult.Type.BLOCK) {
                return Carry;
            }
            BlockPos Pos = Hit.getBlockPos().immutable();
            BlockState State = World.getBlockState(Pos);
            if (!GunGlass.shatters(State)) {
                return 0.0F;
            }
            Carry *= GunGlass.carryOf(State);
            Panes.add(Pos);
            Optional<Vec3> Exit = new AABB(Pos).clip(To, Hit.getLocation());
            if (Exit.isEmpty()) {
                return Carry;
            }
            Start = Exit.get().add(Heading.scale(GrenadeSight.Nudge));
            if (From.distanceToSqr(Start) >= Length) {
                return Carry;
            }
        }
        return 0.0F;
    }

    public static final class Calculator extends ExplosionDamageCalculator {
        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion Boom, BlockGetter World, BlockPos Pos, BlockState State, FluidState Fluid) {
            if (State.is(GunGlass.Bulletproof)) {
                return Optional.of(GrenadeTuning.BulletproofResistance);
            }
            return super.getBlockExplosionResistance(Boom, World, Pos, State, Fluid);
        }

        @Override
        public boolean shouldBlockExplode(Explosion Boom, BlockGetter World, BlockPos Pos, BlockState State, float Power) {
            return !State.is(GunGlass.Bulletproof);
        }
    }
}
