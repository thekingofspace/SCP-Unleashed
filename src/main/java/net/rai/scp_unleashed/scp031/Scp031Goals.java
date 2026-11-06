package net.rai.scp_unleashed.scp031;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class Scp031Goals {
    public static final int RepathTicks = 8;
    public static final int SideChance = 40;
    public static final int SideCooldownMin = 50;
    public static final int SideCooldownSpread = 70;
    public static final int SideOutMin = 8;
    public static final int SideBackMin = 6;
    public static final double SideMinDistance = 3.5;
    public static final double WaitDistance = 3.0;
    public static final float BodyTurn = 8.0f;

    private Scp031Goals() {
    }

    public static void face(Scp031Mimic Mimic) {
        float Yaw = Mth.approachDegrees(Mimic.getYRot(), Mimic.getYHeadRot(), BodyTurn);
        Mimic.setYRot(Yaw);
        Mimic.yBodyRot = Yaw;
    }

    public static final class Embrace extends Goal {
        private final Scp031Mimic Mimic;

        public Embrace(Scp031Mimic Mimic) {
            this.Mimic = Mimic;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Mimic.isHolding();
        }

        @Override
        public void start() {
            Mimic.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Mimic.getNavigation().stop();
            LivingEntity Target = Mimic.holdTarget();
            if (Target == null) {
                return;
            }
            Mimic.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            face(Mimic);
            double Dx = Target.getX() - Mimic.getX();
            double Dz = Target.getZ() - Mimic.getZ();
            double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
            if (Distance > Scp031Mimic.HugDistance + 0.12) {
                Mimic.getMoveControl().setWantedPosition(Target.getX(), Target.getY(), Target.getZ(), 0.55);
            } else {
                Vec3 Motion = Mimic.getDeltaMovement();
                Mimic.setDeltaMovement(0.0, Motion.y, 0.0);
            }
        }
    }

    public static final class Approach extends Goal {
        private final Scp031Mimic Mimic;
        private int Repath;
        private int SidePhase;
        private int SideTicks;
        private float SideDirection;
        private int SideCooldown;

        public Approach(Scp031Mimic Mimic) {
            this.Mimic = Mimic;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !Mimic.isHolding() && Mimic.getTarget() != null && Scp031Targets.isValid(Mimic.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
            SidePhase = 0;
            SideCooldown = SideCooldownMin;
        }

        @Override
        public void stop() {
            Mimic.getNavigation().stop();
            Mimic.setFriendly(false);
            Mimic.setCrouched(false);
            SidePhase = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Mimic.getTarget();
            if (Target == null) {
                return;
            }
            double Distance = Mimic.distanceTo(Target);
            Mimic.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            boolean Friendly = Distance <= Scp031Mimic.FriendlyRange;
            Mimic.setFriendly(Friendly);
            if (Friendly) {
                Mimic.crouchSpam();
            } else {
                Mimic.setCrouched(false);
            }
            if (Distance <= Scp031Mimic.GrabRange && Mimic.mayGrab(Target) && Mimic.hasLineOfSight(Target)) {
                SidePhase = 0;
                Mimic.beginHold(Target);
                return;
            }
            if (SidePhase > 0) {
                sideStep(Target);
                return;
            }
            boolean Waiting = !Mimic.mayGrab(Target) && Distance < WaitDistance;
            if (Waiting || Distance < Scp031Mimic.HugDistance + 0.4) {
                Mimic.getNavigation().stop();
                face(Mimic);
                return;
            }
            if (SideCooldown > 0) {
                SideCooldown--;
            }
            if (Friendly && Distance > SideMinDistance && SideCooldown <= 0 && Mimic.getRandom().nextInt(SideChance) == 0) {
                SidePhase = 1;
                SideTicks = SideOutMin + Mimic.getRandom().nextInt(4);
                SideDirection = Mimic.getRandom().nextBoolean() ? 1.0f : -1.0f;
                Mimic.getNavigation().stop();
                sideStep(Target);
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathTicks;
                Mimic.getNavigation().moveTo(Target, Friendly ? Scp031Mimic.FriendlySpeed : Scp031Mimic.ApproachSpeed);
            }
        }

        private void sideStep(LivingEntity Target) {
            Mimic.getNavigation().stop();
            face(Mimic);
            if (SidePhase == 1) {
                Mimic.getMoveControl().strafe(0.12f, SideDirection * 0.85f);
                if (--SideTicks <= 0) {
                    SidePhase = 2;
                    SideTicks = SideBackMin + Mimic.getRandom().nextInt(3);
                }
                return;
            }
            Mimic.getMoveControl().strafe(0.35f, -SideDirection * 0.7f);
            if (--SideTicks <= 0) {
                SidePhase = 0;
                SideCooldown = SideCooldownMin + Mimic.getRandom().nextInt(SideCooldownSpread);
                Repath = 0;
            }
        }
    }
}
