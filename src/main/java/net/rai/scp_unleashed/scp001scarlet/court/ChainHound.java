package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class ChainHound extends CourtMinion {
    public static final int Lunge = SpecialA;
    public static final int TetherTicks = 60;
    public static final double TetherSlack = 6.0;
    private static final BlockParticleOption Links = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CHAIN.defaultBlockState());
    @Nullable
    private LivingEntity Tethered;
    private int TetherLeft;
    private boolean Landed;

    public ChainHound(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.ChainHound);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new LungeGoal(this));
        behaviour(3, new Bite(this));
    }

    @Nullable
    public LivingEntity tethered() {
        return TetherLeft > 0 ? Tethered : null;
    }

    public void tether(LivingEntity Victim) {
        Tethered = Victim;
        TetherLeft = TetherTicks;
        play("tether", 1.0f, 1.0f);
    }

    @Override
    protected void actionTick(int Action, int Elapsed) {
        LivingEntity Target = getTarget();
        if (Action == Lunge && Target != null) {
            if (Elapsed < 12) {
                getNavigation().stop();
                getLookControl().setLookAt(Target, 40.0f, 40.0f);
                if (Elapsed == 1) {
                    play("idle", 1.0f, 0.7f);
                }
            } else if (Elapsed == 12) {
                Vec3 Way = CourtFx.flat(Target.position().subtract(position()));
                double Distance = Math.min(8.0, distanceTo(Target));
                setDeltaMovement(Way.x * (0.35 + Distance * 0.09), 0.38, Way.z * (0.35 + Distance * 0.09));
                hurtMarked = true;
                Landed = false;
                play("lunge", 1.0f, 1.0f);
            } else if (!Landed && distanceToSqr(Target) < 2.6 * 2.6) {
                Landed = true;
                if (CourtFx.strike(this, Target, (float) Kind.damage())) {
                    tether(Target);
                }
            }
            return;
        }
        if (Action == Attack && Elapsed == 2) {
            play("idle", 0.8f, 1.3f);
        }
        super.actionTick(Action, Elapsed);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (TetherLeft <= 0 || Tethered == null) {
            return;
        }
        TetherLeft--;
        if (!Tethered.isAlive() || !isAlive()) {
            TetherLeft = 0;
            return;
        }
        if (level() instanceof ServerLevel World && TetherLeft % 3 == 0) {
            CourtFx.line(World, Links, position().add(0.0, 0.7, 0.0), Tethered.position().add(0.0, 0.9, 0.0), 0.6, 0.02);
        }
        if (Tethered.distanceTo(this) > TetherSlack) {
            CourtFx.pull(Tethered, position(), 0.16);
        }
    }

    @Override
    protected String clip(int Action) {
        return switch (Action) {
            case Attack -> "attack";
            case Lunge -> "lunge";
            default -> null;
        };
    }

    public static class LungeGoal extends Goal {
        private final ChainHound Owner;
        private int Wait = 40;

        public LungeGoal(ChainHound Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (Wait > 0) {
                Wait--;
                return false;
            }
            LivingEntity Target = Owner.getTarget();
            if (Target == null || Owner.busy() || !Owner.onGround() || !CourtFx.validTarget(Target)) {
                return false;
            }
            double Distance = Owner.distanceTo(Target);
            return Distance >= 3.0 && Distance <= 8.0 && Owner.hasLineOfSight(Target);
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.action() == Lunge;
        }

        @Override
        public void start() {
            Owner.startAction(Lunge, 26);
            Wait = 120;
        }
    }

    public static class Bite extends CourtGoals.Melee {
        public Bite(CourtMinion Owner) {
            super(Owner, 1.15, 20, 14);
        }
    }

    @Override
    protected int hitTick() {
        return 6;
    }
}
