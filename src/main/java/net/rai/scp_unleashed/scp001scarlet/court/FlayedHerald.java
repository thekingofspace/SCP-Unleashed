package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class FlayedHerald extends CourtMinion {
    public static final int Buff = SpecialA;
    public static final double BuffRadius = 10.0;
    public static final int BuffTicks = 120;
    public static final float BoltDamage = 6.0f;

    public FlayedHerald(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.FlayedHerald);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Proclaim(this));
        behaviour(3, new Cast(this));
        behaviour(4, new CourtGoals.KeepDistance(this, 8.0, 14.0, 1.0));
    }

    @Override
    protected void actionTick(int Action, int Elapsed) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        LivingEntity Target = getTarget();
        if (Action == Attack) {
            getNavigation().stop();
            if (Elapsed < 14 && Elapsed % 3 == 0) {
                CourtFx.burst(World, CourtFx.Crimson, position().add(0.0, 2.3, 0.0), 2, 0.15, 0.0);
            }
            if (Elapsed == 14 && Target != null) {
                play("cast", 1.0f, 1.0f);
                Vec3 From = position().add(0.0, 2.0, 0.0);
                CourtBolt.launch(World, this, From, Target.getEyePosition().subtract(0.0, 0.3, 0.0), CourtBolt.Crimson, 0.9, BoltDamage);
            }
        } else if (Action == Buff) {
            getNavigation().stop();
            if (Elapsed == 2) {
                play("idle", 1.0f, 0.8f);
            }
            if (Elapsed == 16) {
                play("buff", 1.2f, 1.0f);
                CourtFx.ring(World, CourtFx.Crimson, position(), BuffRadius, 48, 0.2);
                CourtFx.ring(World, CourtFx.DeepCrimson, position(), BuffRadius * 0.6, 32, 0.4);
                for (Mob Ally : World.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(BuffRadius), Each -> Each != this && Each instanceof CourtMinion)) {
                    Ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BuffTicks, 0));
                    Ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BuffTicks, 0));
                    World.sendParticles(CourtFx.Crimson, Ally.getX(), Ally.getY() + Ally.getBbHeight(), Ally.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
                }
            }
        }
    }

    @Override
    protected String clip(int Action) {
        return switch (Action) {
            case Attack -> "cast";
            case Buff -> "buff";
            default -> null;
        };
    }

    public static class Cast extends Goal {
        private final FlayedHerald Owner;
        private int Wait = 30;

        public Cast(FlayedHerald Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--Wait > 0) {
                return false;
            }
            LivingEntity Target = Owner.getTarget();
            return Target != null && !Owner.busy() && CourtFx.validTarget(Target) && Owner.distanceTo(Target) < 22.0 && Owner.hasLineOfSight(Target);
        }

        @Override
        public void start() {
            Owner.startAction(Attack, 22);
            Wait = 50 + Owner.getRandom().nextInt(20);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    public static class Proclaim extends Goal {
        private final FlayedHerald Owner;
        private int Wait = 80;

        public Proclaim(FlayedHerald Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            if (--Wait > 0) {
                return false;
            }
            return !Owner.busy() && !Owner.level().getEntitiesOfClass(CourtMinion.class, Owner.getBoundingBox().inflate(BuffRadius), Each -> Each != Owner).isEmpty();
        }

        @Override
        public void start() {
            Owner.startAction(Buff, 26);
            Wait = 200;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}
