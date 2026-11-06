package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.npc.NpcGunner;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.RaidStage;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public final class HostileRaiderGoals {
    public static final double SightRange = 24.0;
    public static final double LeashRange = 40.0;
    public static final int RethinkTicks = 10;
    public static final double MaxEngage = 40.0;
    public static final double ProwlRadius = 10.0;
    public static final double ProwlFar = 14.0;
    public static final int ProwlEvery = 100;
    public static final double WithdrawSpeed = 1.15;
    public static final int StuckLimit = 400;
    public static final double StuckReach = 6.0;

    private HostileRaiderGoals() {
    }

    public static double near(ItemStack Gun) {
        GunKind Kind = NpcGunner.kindOf(Gun);
        if (Kind == null) {
            return 2.0;
        }
        return switch (Kind) {
            case Shotgun, DoubleBarrel, CombatShotgun -> 3.0;
            case SniperRifle -> 14.0;
            case AssaultRifle, Spiker, Scp127, MachineGun -> 7.0;
            default -> 5.0;
        };
    }

    public static double far(ItemStack Gun) {
        GunKind Kind = NpcGunner.kindOf(Gun);
        if (Kind == null) {
            return 2.0;
        }
        return switch (Kind) {
            case Shotgun, DoubleBarrel, CombatShotgun -> 8.0;
            case SniperRifle -> 32.0;
            case AssaultRifle, Spiker, Scp127 -> 18.0;
            case MachineGun -> 22.0;
            default -> 14.0;
        };
    }

    public static final class Hunt extends Goal {
        private final HostileRaider Owner;
        private int Wait;

        public Hunt(HostileRaider Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Current = Owner.getTarget();
            if (!Owner.active() || Owner.hauling()) {
                if (Current != null) {
                    Owner.setTarget(null);
                }
                return;
            }
            if (Current != null && !valid(Current)) {
                Owner.setTarget(null);
                Current = null;
            }
            if (--Wait > 0) {
                return;
            }
            Wait = RethinkTicks + Math.floorMod(Owner.getId(), 3);
            LivingEntity Best = choose(Current);
            if (Best != Current) {
                Owner.setTarget(Best);
                if (Best != null) {
                    Owner.delayReaction();
                }
            }
        }

        private boolean retaliating(LivingEntity Target) {
            return Owner.getLastHurtByMob() == Target && Owner.recentlyHurt();
        }

        private boolean valid(LivingEntity Target) {
            if (!Target.isAlive() || Target.level() != Owner.level() || Owner.distanceTo(Target) > LeashRange || Owner.ally(Target)) {
                return false;
            }
            return HostileTargets.hostileTo(Owner, Target) || retaliating(Target) && Targeting.isTargetable(Target);
        }

        @Nullable
        private LivingEntity choose(@Nullable LivingEntity Current) {
            LivingEntity Attacker = Owner.getLastHurtByMob();
            if (Attacker != null && retaliating(Attacker) && valid(Attacker)) {
                return Attacker;
            }
            if (Current != null && valid(Current)) {
                return Current;
            }
            if (Owner.retreating() && !Owner.recentlyHurt()) {
                return null;
            }
            AABB Area = Owner.getBoundingBox().inflate(SightRange, 8.0, SightRange);
            List<LivingEntity> Found = Owner.level().getEntitiesOfClass(LivingEntity.class, Area, Candidate -> HostileTargets.hostileTo(Owner, Candidate));
            LivingEntity Best = null;
            double BestGap = Double.MAX_VALUE;
            for (LivingEntity Candidate : Found) {
                double Gap = Candidate.distanceToSqr(Owner);
                if (Gap < BestGap && Owner.getSensing().hasLineOfSight(Candidate)) {
                    BestGap = Gap;
                    Best = Candidate;
                }
            }
            return Best;
        }
    }

    public static final class Engage extends Goal {
        public static final int RecheckTicks = 5;
        public static final int KiteTicks = 10;
        private final HostileRaider Owner;
        private int Repath;
        private int StrafeTicks;
        private boolean StrafeLeft;
        private int Recheck;
        private int Kiting;
        private boolean Clear;

        public Engage(HostileRaider Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Owner.active() && !Owner.hauling() && Target != null && Target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Repath = 0;
            Recheck = 0;
            Kiting = 0;
            Clear = false;
            StrafeLeft = Owner.getRandom().nextBoolean();
            Owner.setAggressive(true);
        }

        @Override
        public void stop() {
            Owner.setAggressive(false);
            Owner.getNavigation().stop();
            Owner.getMoveControl().strafe(0.0f, 0.0f);
            Owner.setXxa(0.0f);
            Owner.setZza(0.0f);
        }

        @Override
        public void tick() {
            LivingEntity Target = Owner.getTarget();
            if (Target == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Target, 40.0f, 40.0f);
            double Distance = Owner.distanceTo(Target);
            boolean Visible = Owner.getSensing().hasLineOfSight(Target);
            if (Owner.reaction() <= 0 && Owner.useArsenal(Target, Distance, Visible)) {
                return;
            }
            ItemStack Gun = Owner.getMainHandItem();
            if (!NpcGunner.isGun(Gun)) {
                melee(Target, Distance, Visible);
                return;
            }
            double Reach = Math.min(NpcGunner.range(Gun), MaxEngage);
            double Far = Math.min(far(Gun), Reach);
            double Near = near(Gun);
            boolean Ready = Owner.reaction() <= 0 && Owner.triggerReady() && NpcGunner.ready(Owner, Gun);
            if (Visible && Distance <= Reach && (Ready || --Recheck <= 0)) {
                Recheck = RecheckTicks;
                Clear = NpcGunner.clearShot(Owner, Target, Found -> Found != Target && Owner.ally(Found));
            } else if (!Visible || Distance > Reach) {
                Clear = false;
            }
            move(Target, Distance, Visible, Clear, Near, Far);
            if (Clear && Ready && NpcGunner.fireAt(Owner, Gun, Target, null, Owner.accuracy())) {
                Owner.firedShot();
            }
        }

        private void move(LivingEntity Target, double Distance, boolean Visible, boolean Clear, double Near, double Far) {
            if (!Visible || Distance > Far) {
                Owner.getMoveControl().strafe(0.0f, 0.0f);
                if (--Repath <= 0) {
                    Repath = 10;
                    Owner.getNavigation().moveTo(Target, Distance > Far * 1.6 ? 1.2 : 1.0);
                }
                return;
            }
            if (Distance < Near && Owner.kites()) {
                if (Kiting > 0) {
                    Kiting--;
                    if (!Owner.getNavigation().isDone()) {
                        return;
                    }
                } else {
                    Kiting = KiteTicks;
                    Owner.getNavigation().stop();
                    Vec3 Away = DefaultRandomPos.getPosAway(Owner, 8, 4, Target.position());
                    if (Away != null && Owner.getNavigation().moveTo(Away.x, Away.y, Away.z, 1.2)) {
                        return;
                    }
                }
            } else {
                Kiting = 0;
            }
            Owner.getNavigation().stop();
            if (!Clear || Owner.strafes()) {
                if (--StrafeTicks <= 0) {
                    StrafeTicks = 20 + Owner.getRandom().nextInt(30);
                    StrafeLeft = !StrafeLeft;
                }
                float Forward = Distance > Far * 0.85 ? 0.35f : Distance < Near ? -0.4f : 0.0f;
                float Side = (StrafeLeft ? 0.45f : -0.45f) * (Clear ? 1.0f : 1.4f);
                Owner.getMoveControl().strafe(Forward, Side);
                return;
            }
            Owner.getMoveControl().strafe(0.0f, 0.0f);
        }

        private void melee(LivingEntity Target, double Distance, boolean Visible) {
            if (--Repath <= 0) {
                Repath = 8;
                Owner.getNavigation().moveTo(Target, 1.2);
            }
            if (Distance < 2.2 && Visible && Owner.isWithinMeleeAttackRange(Target) && Owner.reaction() <= 0) {
                Owner.swing(InteractionHand.MAIN_HAND);
                Owner.doHurtTarget(Target);
                Owner.delayReaction();
            }
        }
    }

    public static final class Withdraw extends Goal {
        private final HostileRaider Owner;
        private int Repath;
        private int Stuck;
        private double Best = Double.MAX_VALUE;
        @Nullable
        private Vec3 Aimed;

        public Withdraw(HostileRaider Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean wanted() {
            if (!Owner.active()) {
                return false;
            }
            Raid Current = Owner.raid();
            if (Current == null) {
                return false;
            }
            if (Owner.hauling()) {
                return true;
            }
            return Current.stage() == RaidStage.Retreating && !(Owner.recentlyHurt() && Owner.getTarget() != null);
        }

        @Override
        public boolean canUse() {
            return wanted();
        }

        @Override
        public boolean canContinueToUse() {
            return wanted();
        }

        @Override
        public void start() {
            Repath = 0;
            Stuck = 0;
            Best = Double.MAX_VALUE;
            Aimed = null;
            Owner.setTarget(null);
            Owner.setAggressive(false);
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Raid Current = Owner.raid();
            if (Current == null) {
                return;
            }
            Vec3 Exit = Owner.exitPoint(Current);
            if (Aimed == null || Aimed.distanceToSqr(Exit) > 1.0) {
                Aimed = Exit;
                Best = Double.MAX_VALUE;
                Stuck = 0;
                Repath = 0;
            }
            double Flat = Math.sqrt(Mth.square(Owner.getX() - Exit.x) + Mth.square(Owner.getZ() - Exit.z));
            Owner.getLookControl().setLookAt(Exit.x, Owner.getEyeY(), Exit.z);
            if (Flat <= Owner.exitReach() && Math.abs(Owner.getY() - Exit.y) < 3.0) {
                Owner.getNavigation().stop();
                Owner.arrive();
                return;
            }
            if (Flat < Best - 0.5) {
                Best = Flat;
                Stuck = 0;
            } else if (++Stuck > StuckLimit && Flat < StuckReach) {
                Owner.getNavigation().stop();
                Owner.arrive();
                return;
            }
            if (--Repath <= 0) {
                Repath = 15;
                if (!Owner.getNavigation().moveTo(Exit.x, Exit.y, Exit.z, WithdrawSpeed) && Flat < 3.0) {
                    Owner.getMoveControl().setWantedPosition(Exit.x, Exit.y, Exit.z, 1.0);
                }
            }
        }
    }

    public static final class Prowl extends Goal {
        private final HostileRaider Owner;
        private int Wait;

        public Prowl(HostileRaider Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean wanted() {
            Raid Current = Owner.raid();
            return Owner.active() && !Owner.hauling() && Owner.getTarget() == null && Current != null && Current.stage() == RaidStage.Active;
        }

        @Override
        public boolean canUse() {
            return wanted() && --Wait <= 0;
        }

        @Override
        public boolean canContinueToUse() {
            return wanted() && !Owner.getNavigation().isDone();
        }

        @Override
        public void start() {
            Wait = ProwlEvery / 2 + Owner.getRandom().nextInt(ProwlEvery);
            Raid Current = Owner.raid();
            if (Current == null || !(Owner.level() instanceof ServerLevel World)) {
                return;
            }
            BlockPos Target = Current.target();
            double Gap = Math.sqrt(Owner.blockPosition().distSqr(Target));
            BlockPos Goal = Target;
            if (Gap <= ProwlFar) {
                double Angle = Owner.getRandom().nextDouble() * Mth.TWO_PI;
                double Reach = 2.0 + Owner.getRandom().nextDouble() * ProwlRadius;
                BlockPos Found = RaidGround.standNear(World, Target.getX() + Mth.floor(Math.cos(Angle) * Reach), Target.getZ() + Mth.floor(Math.sin(Angle) * Reach),
                        Target.getY(), 4);
                if (Found != null) {
                    Goal = Found;
                }
            }
            Owner.getNavigation().moveTo(Goal.getX() + 0.5, Goal.getY(), Goal.getZ() + 0.5, Gap > ProwlFar ? 1.0 : 0.8);
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }
    }
}
