package net.rai.scp_unleashed.mtf;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.npc.NpcGunner;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class MtfOperativeGoals {
    public static final double LeashRange = 40.0;
    public static final double HostileRange = 22.0;
    public static final double SelfAlertRange = 16.0;
    public static final int RetaliateMemory = 100;
    public static final int RethinkTicks = 10;
    public static final double MaxEngage = 48.0;

    private MtfOperativeGoals() {
    }

    private static long now(MtfOperative Owner) {
        return Owner.level().getGameTime();
    }

    public static final class PickTarget extends Goal {
        private final MtfOperative Owner;
        private int Wait;

        public PickTarget(MtfOperative Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return Owner.onDuty();
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.onDuty();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            Owner.setTarget(null);
        }

        @Override
        public void tick() {
            LivingEntity Current = Owner.getTarget();
            UUID Caller = Owner.callerId();
            if (Caller == null) {
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
            LivingEntity Best = choose(Caller, Current);
            if (Best != Current) {
                Owner.setTarget(Best);
                if (Best != null) {
                    Owner.delayReaction();
                }
            }
        }

        private boolean valid(LivingEntity Target) {
            UUID Caller = Owner.callerId();
            if (Caller == null || !Target.isAlive() || Target.level() != Owner.level()) {
                return false;
            }
            ServerPlayer Leader = Owner.caller();
            Vec3 Anchor = Leader != null ? Leader.position() : Owner.position();
            if (Target.position().distanceTo(Anchor) > LeashRange) {
                return false;
            }
            MtfMission Mission = Owner.mission();
            boolean Explicit = Mission != null && Mission.callerFight((ServerLevel) Owner.level(), now(Owner)) == Target;
            return MtfFriends.canEngage(Caller, Target, Explicit || retaliating(Target));
        }

        private boolean retaliating(LivingEntity Target) {
            return Owner.getLastHurtByMob() == Target && Owner.tickCount - Owner.getLastHurtByMobTimestamp() < RetaliateMemory;
        }

        @Nullable
        private LivingEntity choose(UUID Caller, @Nullable LivingEntity Current) {
            ServerLevel World = (ServerLevel) Owner.level();
            MtfMission Mission = Owner.mission();
            long Now = now(Owner);
            if (Mission != null) {
                LivingEntity Fight = Mission.callerFight(World, Now);
                if (Fight != null && Fight != Owner) {
                    return Fight;
                }
            }
            LivingEntity Attacker = Owner.getLastHurtByMob();
            if (Attacker != null && retaliating(Attacker) && MtfFriends.canEngage(Caller, Attacker, true)) {
                return Attacker;
            }
            if (Current != null && valid(Current)) {
                return Current;
            }
            if (Mission != null && Owner.tier().focusesFire()) {
                LivingEntity Focus = Mission.focus(World, Now);
                if (Focus != null && Owner.distanceTo(Focus) <= MaxEngage) {
                    return Focus;
                }
            }
            return nearestHostile(World, Caller);
        }

        @Nullable
        private LivingEntity nearestHostile(ServerLevel World, UUID Caller) {
            ServerPlayer Leader = Owner.caller();
            Vec3 Anchor = Leader != null ? Leader.position() : Owner.position();
            AABB Area = new AABB(Anchor, Anchor).inflate(HostileRange, 10.0, HostileRange).minmax(Owner.getBoundingBox().inflate(SelfAlertRange, 8.0, SelfAlertRange));
            List<LivingEntity> Found = World.getEntitiesOfClass(LivingEntity.class, Area, Candidate -> Candidate != Owner && MtfFriends.isHostile(Caller, Candidate));
            LivingEntity Best = null;
            double BestScore = Double.MAX_VALUE;
            for (LivingEntity Candidate : Found) {
                if (!Owner.getSensing().hasLineOfSight(Candidate)) {
                    continue;
                }
                double Score = Candidate.distanceToSqr(Anchor) * 0.6 + Candidate.distanceToSqr(Owner) * 0.4;
                if (MtfFriends.threatens(Caller, Candidate)) {
                    Score *= 0.35;
                }
                if (Score < BestScore) {
                    BestScore = Score;
                    Best = Candidate;
                }
            }
            return Best;
        }
    }

    public static final class Engage extends Goal {
        public static final int RecheckTicks = 5;
        public static final int KiteTicks = 10;
        private final MtfOperative Owner;
        private int Repath;
        private int StrafeTicks;
        private boolean StrafeLeft;
        private int Idle;
        private int Recheck;
        private int Kiting;
        private boolean Clear;

        public Engage(MtfOperative Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Owner.onDuty() && Owner.getTarget() != null && Owner.getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !Owner.inCover();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Repath = 0;
            Idle = 0;
            Recheck = 0;
            Kiting = 0;
            Clear = false;
            StrafeLeft = Owner.getRandom().nextBoolean();
        }

        @Override
        public void stop() {
            Owner.setAiming(false);
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
            ItemStack Gun = Owner.getMainHandItem();
            double Distance = Owner.distanceTo(Target);
            boolean Visible = Owner.getSensing().hasLineOfSight(Target);
            MtfRole Role = Owner.role();
            MtfTier Tier = Owner.tier();
            if (!NpcGunner.isGun(Gun)) {
                melee(Target, Distance);
                return;
            }
            double Reach = Math.min(NpcGunner.range(Gun), MaxEngage);
            double Far = Math.min(Role.far(), Reach);
            boolean Ready = Owner.reaction() <= 0 && NpcGunner.ready(Owner, Gun);
            if (Visible && Distance <= Reach && (Ready || --Recheck <= 0)) {
                Recheck = RecheckTicks;
                Clear = NpcGunner.clearShot(Owner, Target, MtfFriends.shotGuard(Owner.callerId(), Target));
            } else if (!Visible || Distance > Reach) {
                Clear = false;
            }
            move(Target, Distance, Visible, Clear, Role, Tier, Far);
            if (Visible && Distance <= Reach) {
                Owner.setAiming(true);
                Idle = 0;
            } else if (++Idle > 30) {
                Owner.setAiming(false);
            }
            if (Clear && Ready) {
                if (NpcGunner.fireAt(Owner, Gun, Target, null, Owner.accuracy())) {
                    MtfMission Mission = Owner.mission();
                    if (Mission != null) {
                        Mission.noteFocus(Target, now(Owner));
                    }
                }
            }
        }

        private void move(LivingEntity Target, double Distance, boolean Visible, boolean Clear, MtfRole Role, MtfTier Tier, double Far) {
            if (!Visible || Distance > Far) {
                Owner.getMoveControl().strafe(0.0f, 0.0f);
                if (--Repath <= 0) {
                    Repath = 10;
                    Owner.getNavigation().moveTo(Target, Distance > Far * 1.6 ? 1.25 : 1.05);
                }
                return;
            }
            if (Distance < Role.near() && Tier.kites()) {
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
            if (!Clear || Tier.strafes()) {
                if (--StrafeTicks <= 0) {
                    StrafeTicks = 20 + Owner.getRandom().nextInt(30);
                    StrafeLeft = !StrafeLeft;
                }
                float Forward = Distance > Far * 0.85 ? 0.35f : Distance < Role.near() ? -0.4f : 0.0f;
                float Side = (StrafeLeft ? 0.45f : -0.45f) * (Clear ? 1.0f : 1.4f);
                Owner.getMoveControl().strafe(Forward, Side);
                return;
            }
            Owner.getMoveControl().strafe(0.0f, 0.0f);
        }

        private void melee(LivingEntity Target, double Distance) {
            Owner.setAiming(false);
            if (--Repath <= 0) {
                Repath = 8;
                Owner.getNavigation().moveTo(Target, 1.2);
            }
            if (Distance < 2.2 && Owner.isWithinMeleeAttackRange(Target) && Owner.getSensing().hasLineOfSight(Target) && Owner.reaction() <= 0) {
                Owner.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                Owner.doHurtTarget(Target);
                Owner.delayReaction();
            }
        }
    }

    public static final class TakeCover extends Goal {
        public static final int HoldTicks = 50;
        public static final int Cooldown = 120;
        public static final int Samples = 28;
        private final MtfOperative Owner;
        @Nullable
        private Vec3 Spot;
        private int Hold;
        private long ReadyAt;

        public TakeCover(MtfOperative Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean threatened() {
            MtfTier Tier = Owner.tier();
            if (!Tier.takesCover()) {
                return false;
            }
            float Health = Owner.getHealth() / Owner.getMaxHealth();
            boolean Hurt = Owner.tickCount - Owner.getLastHurtByMobTimestamp() < 20 && Owner.getLastHurtByMob() != null;
            boolean Reloading = Tier.level() >= 4 && NpcGunner.reloading(Owner, Owner.getMainHandItem());
            if (Tier.level() <= 2) {
                return Health < 0.4f;
            }
            return Health < 0.55f || Hurt && Health < 0.85f || Reloading;
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            if (!Owner.onDuty() || Target == null || now(Owner) < ReadyAt || !threatened()) {
                return false;
            }
            Spot = find(Target);
            return Spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.onDuty() && Spot != null && Hold > 0 && Owner.getTarget() != null;
        }

        @Override
        public void start() {
            Hold = HoldTicks + 80;
            Owner.setInCover(true);
            Owner.setAiming(false);
            if (Spot != null) {
                Owner.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, 1.35);
            }
        }

        @Override
        public void stop() {
            Owner.setInCover(false);
            Owner.getNavigation().stop();
            ReadyAt = now(Owner) + Cooldown;
            Spot = null;
        }

        @Override
        public void tick() {
            Hold--;
            LivingEntity Target = Owner.getTarget();
            if (Target != null) {
                Owner.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            }
            if (Spot != null && Owner.distanceToSqr(Spot) < 1.5 && Hold > HoldTicks) {
                Hold = HoldTicks;
            }
            if (Spot != null && Owner.getNavigation().isDone() && Owner.distanceToSqr(Spot) > 2.5 && Hold > HoldTicks) {
                Hold = 0;
            }
        }

        @Nullable
        private Vec3 find(LivingEntity Target) {
            ServerLevel World = (ServerLevel) Owner.level();
            Vec3 Threat = Target.getEyePosition();
            double Now = Owner.distanceToSqr(Target);
            ServerPlayer Leader = Owner.caller();
            Vec3 Best = null;
            double BestScore = Double.MAX_VALUE;
            for (int Index = 0; Index < Samples; Index++) {
                double Angle = Index * Mth.TWO_PI / Samples + Owner.getRandom().nextDouble() * 0.2;
                double Radius = 3.0 + (Index % 3) * 2.5;
                BlockPos Probe = BlockPos.containing(Owner.getX() + Math.cos(Angle) * Radius, Owner.getY(), Owner.getZ() + Math.sin(Angle) * Radius);
                BlockPos Stand = standable(World, Probe);
                if (Stand == null) {
                    continue;
                }
                Vec3 Spot = Vec3.atBottomCenterOf(Stand);
                if (Spot.distanceToSqr(Target.position()) < Now * 0.7 || Leader != null && Spot.distanceTo(Leader.position()) > 20.0) {
                    continue;
                }
                Vec3 Eye = Spot.add(0.0, Owner.getEyeHeight(), 0.0);
                if (World.clip(new ClipContext(Threat, Eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Target)).getType() != HitResult.Type.BLOCK) {
                    continue;
                }
                double Score = Owner.distanceToSqr(Spot);
                if (Score < BestScore) {
                    Path Route = Owner.getNavigation().createPath(Stand, 0);
                    if (Route != null && Route.canReach()) {
                        BestScore = Score;
                        Best = Spot;
                    }
                }
            }
            return Best;
        }

        @Nullable
        private static BlockPos standable(ServerLevel World, BlockPos Probe) {
            for (int Dy = 2; Dy >= -2; Dy--) {
                BlockPos At = Probe.above(Dy);
                if (World.getBlockState(At.below()).isFaceSturdy(World, At.below(), net.minecraft.core.Direction.UP)
                        && World.getBlockState(At).getCollisionShape(World, At).isEmpty() && World.getBlockState(At.above()).getCollisionShape(World, At.above()).isEmpty()) {
                    return At;
                }
            }
            return null;
        }
    }

    public static final class FollowCaller extends Goal {
        public static final double Loose = 7.0;
        public static final double Settle = 3.5;
        private final MtfOperative Owner;
        private int Repath;

        public FollowCaller(MtfOperative Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private Vec3 slot(ServerPlayer Leader) {
            double Angle = Leader.getYRot() * Mth.DEG_TO_RAD + Mth.PI * 0.5 + (Owner.slot() - 1.0) * 0.55 * (Owner.slot() % 2 == 0 ? 1 : -1);
            double Radius = 3.0 + (Owner.slot() % 3);
            return Leader.position().add(Math.cos(Angle) * Radius, 0.0, Math.sin(Angle) * Radius);
        }

        @Override
        public boolean canUse() {
            if (!Owner.onDuty() || Owner.getTarget() != null) {
                return false;
            }
            ServerPlayer Leader = Owner.caller();
            return Leader != null && Leader.isAlive() && !Leader.isSpectator() && Owner.distanceToSqr(Leader) > Loose * Loose;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer Leader = Owner.caller();
            return Owner.onDuty() && Owner.getTarget() == null && Leader != null && Leader.isAlive() && Owner.distanceToSqr(slot(Leader)) > Settle * Settle;
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer Leader = Owner.caller();
            if (Leader == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Leader, 20.0f, Owner.getMaxHeadXRot());
            if (--Repath <= 0) {
                Repath = 10;
                Vec3 Spot = slot(Leader);
                double Gap = Owner.distanceTo(Leader);
                Owner.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Gap > 16.0 ? 1.35 : 1.0);
            }
        }
    }

    public static final class Regroup extends Goal {
        private final MtfOperative Owner;
        private int Repath;

        public Regroup(MtfOperative Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Owner.state() == MtfOperative.StateRegroup && Owner.regroupPoint() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Repath = 0;
            Owner.setTarget(null);
            Owner.setAiming(false);
        }

        @Override
        public void tick() {
            Vec3 Point = Owner.regroupPoint();
            if (Point == null) {
                return;
            }
            double Flat = Math.sqrt(Mth.square(Owner.getX() - Point.x) + Mth.square(Owner.getZ() - Point.z));
            if (Flat < 0.9) {
                Owner.getNavigation().stop();
                MtfHelicopter Ride = Owner.helicopterNear();
                if (Ride != null) {
                    Owner.getLookControl().setLookAt(Ride.getX(), Ride.getY(), Ride.getZ());
                }
                return;
            }
            if (--Repath <= 0) {
                Repath = 12;
                if (!Owner.getNavigation().moveTo(Point.x, Point.y, Point.z, 1.25) && Flat < 3.0) {
                    Owner.getMoveControl().setWantedPosition(Point.x, Point.y, Point.z, 1.0);
                }
            }
        }
    }

    public static final class WatchCaller extends Goal {
        private final MtfOperative Owner;
        private int Left;

        public WatchCaller(MtfOperative Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            ServerPlayer Leader = Owner.caller();
            return Owner.onDuty() && Owner.getTarget() == null && Leader != null && Owner.getRandom().nextFloat() < 0.02f && Owner.distanceToSqr(Leader) < 144.0;
        }

        @Override
        public boolean canContinueToUse() {
            return Left > 0 && Owner.onDuty() && Owner.getTarget() == null;
        }

        @Override
        public void start() {
            Left = 40 + Owner.getRandom().nextInt(40);
        }

        @Override
        public void tick() {
            Left--;
            ServerPlayer Leader = Owner.caller();
            if (Leader != null) {
                Owner.getLookControl().setLookAt(Leader, 10.0f, Owner.getMaxHeadXRot());
            }
        }
    }
}
