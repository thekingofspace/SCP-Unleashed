package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class ThroneGolem extends CourtMinion {
    public static final int Slam = SpecialA;
    public static final int SlamWindup = 30;
    public static final double SlamRadius = 4.0;

    public ThroneGolem(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.ThroneGolem);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Smash(this));
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
        play("step", 0.8f, 1.0f);
    }

    @Override
    protected void actionTick(int Action, int Elapsed) {
        if (Action == Slam && level() instanceof ServerLevel World) {
            getNavigation().stop();
            if (Elapsed == 1) {
                play("idle", 1.4f, 0.7f);
            }
            if (Elapsed < SlamWindup && Elapsed % 5 == 0) {
                CourtFx.ring(World, CourtFx.Crimson, position(), SlamRadius, 36, 0.1);
                CourtFx.ring(World, CourtFx.DeepCrimson, position(), SlamRadius * (Elapsed / (double) SlamWindup), 20, 0.1);
            }
            if (Elapsed == SlamWindup) {
                play("slam", 2.0f, 1.0f);
                CourtFx.crumble(World, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), position(), 40, SlamRadius * 0.6, 0.2);
                for (LivingEntity Victim : CourtFx.victimsNear(World, position(), SlamRadius + 0.5)) {
                    if (Math.abs(Victim.getY() - getY()) < 2.5 && CourtFx.strike(this, Victim, (float) Kind.damage())) {
                        CourtFx.push(Victim, position(), 1.0, 0.45);
                    }
                }
            }
        }
    }

    @Override
    protected String clip(int Action) {
        return Action == Slam ? "slam" : null;
    }

    public static class Smash extends Goal {
        private final ThroneGolem Owner;
        private int Wait = 40;
        private int Repath;

        public Smash(ThroneGolem Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Target != null && CourtFx.validTarget(Target) && !Owner.rising();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Owner.getTarget();
            if (Target == null) {
                return;
            }
            if (Wait > 0) {
                Wait--;
            }
            Owner.getLookControl().setLookAt(Target, 20.0f, 20.0f);
            if (Owner.busy()) {
                return;
            }
            if (Owner.distanceTo(Target) < SlamRadius && Wait <= 0) {
                Owner.startAction(Slam, SlamWindup + 14);
                Wait = 160;
                return;
            }
            if (--Repath <= 0) {
                Repath = 12;
                Owner.getNavigation().moveTo(Target, 1.0);
            }
        }
    }
}
