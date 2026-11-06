package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CrimsonKnight extends CourtMinion {
    public static final int Guard = SpecialA;
    public static final int Cleave = SpecialB;
    public static final int Sweep = SpecialC;
    public static final double ShieldCone = Math.cos(Math.toRadians(55.0));
    public static final float ShieldShare = 0.1f;
    private boolean NextCleave = true;

    public CrimsonKnight(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.CrimsonKnight);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Halberd(this));
    }

    public boolean shielded(DamageSource Source) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || Source.is(DamageTypeTags.BYPASSES_ARMOR) || isDeadOrDying()) {
            return false;
        }
        Entity From = Source.getDirectEntity() != null ? Source.getDirectEntity() : Source.getEntity();
        Vec3 Origin = From != null ? From.position() : Source.getSourcePosition();
        if (Origin == null) {
            return false;
        }
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        return Facing.dot(CourtFx.flat(Origin.subtract(position()))) >= ShieldCone;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && shielded(Source) && !(Source.getEntity() != null && CourtFx.allied(this, Source.getEntity()))) {
            play("block", 1.0f, 1.0f);
            if (level() instanceof ServerLevel World) {
                CourtFx.burst(World, ParticleTypes.CRIT, position().add(Vec3.directionFromRotation(0.0f, yBodyRot).scale(0.7)).add(0.0, 1.3, 0.0), 6, 0.2, 0.2);
            }
            if (!busy()) {
                startAction(Guard, 14);
            }
            return super.hurt(Source, Amount * ShieldShare);
        }
        return super.hurt(Source, Amount);
    }

    @Override
    protected void actionTick(int Action, int Elapsed) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Action == Cleave) {
            getNavigation().stop();
            if (Elapsed == 1) {
                play("idle", 1.0f, 0.8f);
            }
            if (Elapsed < 20 && Elapsed % 4 == 0) {
                arc(World, 3.2, 0.3);
            }
            if (Elapsed == 20) {
                play("cleave", 1.2f, 1.0f);
                swing(World, 3.4, 0.3, (float) Kind.damage(), 0.6);
            }
        } else if (Action == Sweep) {
            getNavigation().stop();
            if (Elapsed < 12 && Elapsed % 4 == 0) {
                arc(World, 2.8, -0.1);
            }
            if (Elapsed == 12) {
                play("cleave", 1.0f, 1.25f);
                swing(World, 3.0, -0.1, (float) Kind.damage() * 0.75f, 0.9);
            }
        }
    }

    private void arc(ServerLevel World, double Radius, double MinDot) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        for (int Step = -6; Step <= 6; Step++) {
            Vec3 Way = CourtFx.turn(Facing, Step * 12.0);
            if (Way.dot(Facing) < MinDot) {
                continue;
            }
            Vec3 Point = position().add(Way.scale(Radius)).add(0.0, 0.15, 0.0);
            World.sendParticles(CourtFx.Crimson, Point.x, Point.y, Point.z, 1, 0.05, 0.02, 0.05, 0.0);
        }
    }

    private void swing(ServerLevel World, double Radius, double MinDot, float Damage, double Shove) {
        for (LivingEntity Victim : CourtFx.victimsNear(World, position(), Radius + 0.6)) {
            if (inFront(Victim, MinDot) && CourtFx.strike(this, Victim, Damage)) {
                CourtFx.push(Victim, position(), Shove, 0.25);
            }
        }
        CourtFx.burst(World, ParticleTypes.SWEEP_ATTACK, front(1.6).add(0.0, 1.0, 0.0), 2, 0.6, 0.0);
    }

    @Override
    protected boolean holds(int Action) {
        return Action == Guard;
    }

    @Override
    protected String clip(int Action) {
        return switch (Action) {
            case Guard -> "block";
            case Cleave -> "cleave";
            case Sweep -> "sweep";
            default -> null;
        };
    }

    public static class Halberd extends CourtGoals.Melee {
        private final CrimsonKnight Knight;
        private int Rest;

        public Halberd(CrimsonKnight Owner) {
            super(Owner, 0.95, 50, 30);
            Knight = Owner;
        }

        @Override
        public void tick() {
            LivingEntity Target = Owner.getTarget();
            if (Rest > 0) {
                Rest--;
            }
            if (Target != null && Owner.distanceToSqr(Target) <= Owner.reachSqr(Target) + 2.0) {
                Owner.getNavigation().stop();
                Owner.getLookControl().setLookAt(Target, 30.0f, 30.0f);
                if (!Owner.busy() && Rest <= 0) {
                    if (Knight.NextCleave) {
                        Owner.startAction(Cleave, 30);
                    } else {
                        Owner.startAction(Sweep, 22);
                    }
                    Knight.NextCleave = !Knight.NextCleave;
                    Rest = 46;
                }
                return;
            }
            super.tick();
        }
    }

    @Override
    protected double reach() {
        return 2.6;
    }
}
