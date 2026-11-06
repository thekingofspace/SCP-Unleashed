package net.rai.scp_unleashed.guards;

import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.duty.DClassDuties;
import net.rai.scp_unleashed.guns.npc.NpcGunner;
import net.rai.scp_unleashed.security.TurretRules;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class GuardGoals {
    public static final int EngagePriority = 1;
    public static final int PatrolPriority = 2;
    public static final double PatrolSpeed = 0.6;
    public static final double ChaseSpeed = 1.0;
    public static final double ChargeSpeed = 1.15;
    public static final double RetreatSpeed = 1.2;
    public static final double ArriveDistance = 0.8;
    public static final double DriftDistance = 1.6;
    public static final double StepDistance = 1.6;
    public static final int DoorRepath = 3;
    public static final int BlockedRecheck = 60;
    public static final int RepathTicks = 20;
    public static final int StuckTicks = 300;
    public static final int ScanTicks = 10;
    public static final int LostTicks = 100;
    public static final double ChaseRange = 32.0;
    public static final double LeashSlack = 8.0;
    public static final double GunReach = 24.0;
    public static final double GunShare = 0.7;
    public static final int BlockedTicks = 40;
    public static final double SafeDistance = 10.0;
    public static final float Accuracy = 0.75f;
    public static final float RetreatHealth = 0.25f;
    public static final int AlertCooldown = 200;
    public static final int GlanceMin = 30;
    public static final int GlanceSpread = 50;
    public static final float GlanceArc = 75.0f;
    public static final int MinSwing = 10;
    public static final int MealCheck = 20;

    private GuardGoals() {
    }


    public static void install(DClass Owner, GoalSelector Goals, GoalSelector Targets) {
        if (Owner.level() == null || Owner.level().isClientSide) {
            return;
        }
        GuardDoors Doors = new GuardDoors(Owner);
        Goals.addGoal(EngagePriority, new Engage(Owner, Doors));
        Goals.addGoal(PatrolPriority, new Patrol(Owner, Doors));
    }

    public static boolean critical(DClass Owner) {
        return Owner.isOnFire() || Owner.getHealth() < Owner.getMaxHealth() * RetreatHealth;
    }

    @Nullable
    private static GuardOrders duty(DClass Owner) {
        GuardOrders Found = GuardDuty.orders(Owner);
        return Found != null && Found.onDuty(Owner) ? Found : null;
    }

    public static final class Engage extends Goal {
        private final DClass Owner;
        private final GuardDoors Doors;
        @Nullable
        private LivingEntity Threat;
        @Nullable
        private PatrolRoute CachedRoute;
        private TurretRules Rules = new TurretRules();
        private int Scan;
        private int Unseen;
        private int Swing;
        private int Repath;
        private int Blocked;
        private long LastAlert = Long.MIN_VALUE / 2;

        public Engage(DClass Owner, GuardDoors Doors) {
            this.Owner = Owner;
            this.Doors = Doors;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private TurretRules rules(PatrolRoute Route) {
            if (CachedRoute != Route) {
                CachedRoute = Route;
                Rules = Route.Rules().toTurret();
            }
            return Rules;
        }

        @Nullable
        public LivingEntity threat() {
            return Threat;
        }

        @Override
        public boolean canUse() {
            if (--Scan > 0) {
                return false;
            }
            Scan = ScanTicks + Owner.getRandom().nextInt(4);
            GuardOrders Orders = duty(Owner);
            if (Orders == null || critical(Owner)) {
                return false;
            }
            PatrolRoute Route = Orders.route();
            Threat = GuardThreats.find(Owner, Route, rules(Route));
            return Threat != null;
        }

        @Override
        public void start() {
            Owner.wake();
            Unseen = 0;
            Repath = 0;
            Blocked = 0;
            Owner.setTarget(Threat);
            Owner.setAggressive(true);
            GuardOrders Orders = duty(Owner);
            if (Orders != null) {
                Orders.setStatus(GuardStatus.Engaging);
            }
            long Now = Owner.level().getGameTime();
            if (Now - LastAlert >= AlertCooldown) {
                LastAlert = Now;
                GuardSounds.play(Owner.level(), Owner.getEyePosition(), GuardSounds.Alert, SoundSource.NEUTRAL, 0.7f, 1.0f);
            }
        }

        @Override
        public boolean canContinueToUse() {
            GuardOrders Orders = duty(Owner);
            if (Orders == null || Threat == null || !Threat.isAlive() || Threat.isRemoved() || Threat.level() != Owner.level() || critical(Owner)) {
                return false;
            }
            PatrolRoute Route = Orders.route();
            if (!GuardThreats.isThreat(Owner, Route, rules(Route), Threat) || Owner.distanceToSqr(Threat) > ChaseRange * ChaseRange) {
                return false;
            }
            double Leash = GuardThreats.LeashRange + LeashSlack;
            return GuardThreats.routeDistanceSqr(Route, Owner.position()) <= Leash * Leash && Unseen < LostTicks;
        }

        @Override
        public void stop() {
            if (Threat != null && Owner.getTarget() == Threat) {
                Owner.setTarget(null);
            }
            Threat = null;
            Owner.setAggressive(false);
            Owner.getNavigation().stop();
            Scan = ScanTicks;
            GuardOrders Orders = duty(Owner);
            if (Orders != null) {
                Orders.setStatus(GuardStatus.Returning);
                GuardArms.equipDuty(Owner, Orders.route().Rules());
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            GuardOrders Orders = duty(Owner);
            if (Orders == null || Threat == null) {
                return;
            }
            PatrolRoute Route = Orders.route();
            GuardRules Settings = Route.Rules();
            boolean Sees = Owner.getSensing().hasLineOfSight(Threat);
            Unseen = Sees ? 0 : Unseen + 1;
            Swing = Math.max(0, Swing - 1);
            Repath = Math.max(0, Repath - 1);
            Owner.getLookControl().setLookAt(Threat, 30.0f, 30.0f);
            if (Owner.getTarget() != Threat) {
                Owner.setTarget(Threat);
            }
            if (Doors.busy()) {
                if (Sees) {
                    Doors.cancel();
                } else if (Doors.tick()) {
                    return;
                }
            }
            if (!Sees && Doors.transit()) {
                return;
            }
            int Gun = Settings.HoldFire() ? -1 : GuardArms.gunSlot(Owner);
            if (Gun >= 0) {
                shoot(Route, Gun, Sees);
                return;
            }
            int Melee = GuardArms.meleeSlot(Owner);
            if (Settings.Melee() && (Melee >= 0 || GuardThreats.isWeak(Threat))) {
                fight(Melee, Sees);
                return;
            }
            keepAway();
        }

        private boolean friendly(PatrolRoute Route, Entity Other) {
            return Other != Threat && Other instanceof LivingEntity Living && !GuardThreats.isThreat(Owner, Route, rules(Route), Living);
        }

        private void shoot(PatrolRoute Route, int Slot, boolean Sees) {
            GuardArms.equip(Owner, Slot);
            ItemStack Weapon = Owner.getMainHandItem();
            if (!NpcGunner.isGun(Weapon) || Threat == null) {
                return;
            }
            double Reach = Math.min(GunReach, NpcGunner.range(Weapon)) * GunShare;
            double Distance = Owner.distanceTo(Threat);
            boolean Clear = Sees && Distance <= NpcGunner.range(Weapon) && NpcGunner.clearShot(Owner, Threat, Other -> friendly(Route, Other));
            Blocked = Clear ? 0 : Blocked + 1;
            if (Distance > Reach || Blocked > BlockedTicks) {
                if (Repath == 0) {
                    if (Doors.hurrying()) {
                        Owner.getNavigation().stop();
                    }
                    Owner.getNavigation().moveTo(Threat, ChaseSpeed);
                    Repath = 10;
                    if (!Sees && GuardDoors.obstructed(Owner.getNavigation().getPath())) {
                        Doors.plan(Threat.position());
                    }
                }
            } else if (Clear) {
                Owner.getNavigation().stop();
            }
            if (Clear && NpcGunner.ready(Owner, Weapon)) {
                NpcGunner.fireAt(Owner, Weapon, Threat, GuardArms.ammo(Owner), Accuracy);
            }
        }

        private void fight(int Slot, boolean Sees) {
            if (Threat == null) {
                return;
            }
            if (Slot >= 0) {
                GuardArms.equip(Owner, Slot);
            } else {
                GuardArms.equip(Owner, GuardArms.emptyHotbar(Owner));
            }
            if (Repath == 0) {
                if (Doors.hurrying()) {
                    Owner.getNavigation().stop();
                }
                Owner.getNavigation().moveTo(Threat, ChargeSpeed);
                Repath = 5;
                if (GuardDoors.obstructed(Owner.getNavigation().getPath())) {
                    Doors.plan(Threat.position());
                }
            }
            if (Swing == 0 && Sees && Owner.isWithinMeleeAttackRange(Threat)) {
                Owner.swing(InteractionHand.MAIN_HAND);
                Owner.doHurtTarget(Threat);
                Swing = Math.max(MinSwing, Mth.ceil(20.0 / Math.max(0.5, Owner.getAttributeValue(Attributes.ATTACK_SPEED))));
            }
        }

        private void keepAway() {
            if (Threat == null) {
                return;
            }
            if (Owner.distanceToSqr(Threat) < SafeDistance * SafeDistance) {
                if (Repath == 0 || Owner.getNavigation().isDone()) {
                    Vec3 Away = DefaultRandomPos.getPosAway(Owner, 12, 7, Threat.position());
                    if (Away != null) {
                        Owner.getNavigation().moveTo(Away.x, Away.y, Away.z, RetreatSpeed);
                    }
                    Repath = RepathTicks;
                }
            } else if (Repath == 0) {
                Owner.getNavigation().stop();
            }
        }
    }

    public static final class Patrol extends Goal {
        private final DClass Owner;
        private final GuardDoors Doors;
        private boolean Arrived;
        private int Waited;
        private int Glance;
        private int Repath;
        private int Stalled;
        private double Closest = Double.MAX_VALUE;
        private Vec3 Gaze = Vec3.ZERO;

        public Patrol(DClass Owner, GuardDoors Doors) {
            this.Owner = Owner;
            this.Doors = Doors;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return duty(Owner) != null && !Owner.isPassenger() && !DClassDuties.busy(Owner);
        }

        @Override
        public void start() {
            Owner.wake();
            reset();
            GuardOrders Orders = duty(Owner);
            if (Orders != null) {
                GuardArms.equipDuty(Owner, Orders.route().Rules());
            }
        }

        @Override
        public boolean canContinueToUse() {
            return duty(Owner) != null && !Owner.isPassenger() && !DClassDuties.busy(Owner);
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
            Doors.cancel();
            Arrived = false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private void reset() {
            Arrived = false;
            Repath = 0;
            Stalled = 0;
            Closest = Double.MAX_VALUE;
        }

        @Override
        public void tick() {
            GuardOrders Orders = duty(Owner);
            if (Orders == null) {
                return;
            }
            PatrolRoute Route = Orders.route();
            PatrolPoint Point = Orders.current();
            Vec3 Spot = Vec3.atBottomCenterOf(Point.Pos());
            double Flat = Mth.square(Owner.getX() - Spot.x) + Mth.square(Owner.getZ() - Spot.z);
            boolean There = Flat <= ArriveDistance * ArriveDistance && Math.abs(Owner.getY() - Spot.y) <= 1.5;
            if (!Arrived) {
                if (Orders.status() != GuardStatus.Returning && Orders.status() != GuardStatus.Blocked) {
                    Orders.setStatus(Route.single() ? GuardStatus.Standing : GuardStatus.Patrolling);
                }
                if (There) {
                    arrive(Orders, Point);
                    return;
                }
                travel(Orders, Route, Spot, Flat);
                return;
            }
            if (Flat > DriftDistance * DriftDistance) {
                reset();
                return;
            }
            Owner.getNavigation().stop();
            lookAround(Point);
            if (Owner.isUsingItem()) {
                return;
            }
            if (Owner.tickCount % MealCheck == 0) {
                if (mealBreak()) {
                    return;
                }
                GuardArms.equipDuty(Owner, Route.Rules());
            }
            if (Route.single()) {
                Orders.setStatus(GuardStatus.Standing);
                return;
            }
            Orders.setStatus(GuardStatus.Patrolling);
            if (++Waited >= Point.Wait()) {
                Orders.advance();
                reset();
                GuardArms.equipDuty(Owner, Route.Rules());
            }
        }

        private void travel(GuardOrders Orders, PatrolRoute Route, Vec3 Spot, double Flat) {
            if (Doors.busy()) {
                if (Doors.tick()) {
                    return;
                }
                Repath = DoorRepath;
            }
            if (Doors.transit()) {
                Repath = 0;
                return;
            }
            PathNavigation Navigation = Owner.getNavigation();
            Vec3 Waiting = Doors.waiting();
            if (Doors.blocked() && Waiting != null) {
                Orders.setStatus(GuardStatus.Blocked);
                double Gap = Mth.square(Owner.getX() - Waiting.x) + Mth.square(Owner.getZ() - Waiting.z);
                if (--Repath <= 0) {
                    Navigation.stop();
                    Navigation.moveTo(Spot.x, Spot.y, Spot.z, 0, PatrolSpeed);
                    Repath = BlockedRecheck;
                    if (!GuardDoors.obstructed(Navigation.getPath()) || Doors.plan(Spot)) {
                        Doors.clearBlocked();
                        return;
                    }
                    Navigation.stop();
                }
                if (Gap > 1.0) {
                    if (Navigation.isDone()) {
                        Navigation.moveTo(Waiting.x, Waiting.y, Waiting.z, 0, PatrolSpeed);
                    }
                } else {
                    Navigation.stop();
                    Owner.getLookControl().setLookAt(Doors.blockedDoor().x, Doors.blockedDoor().y + 1.0, Doors.blockedDoor().z);
                }
                if (++Stalled > StuckTicks) {
                    if (!Route.single()) {
                        Orders.advance();
                    }
                    Doors.clearBlocked();
                    reset();
                }
                return;
            }
            if (Flat <= StepDistance * StepDistance && Math.abs(Owner.getY() - Spot.y) <= 1.0) {
                Navigation.stop();
                Owner.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, Doors.pace(PatrolSpeed));
            } else if (--Repath <= 0 || Navigation.isDone()) {
                if (Doors.hurrying() || GuardDoors.obstructed(Navigation.getPath())) {
                    Navigation.stop();
                }
                Navigation.moveTo(Spot.x, Spot.y, Spot.z, 0, Doors.pace(PatrolSpeed));
                Repath = RepathTicks;
                if (GuardDoors.obstructed(Navigation.getPath())) {
                    if (Doors.plan(Spot)) {
                        return;
                    }
                    if (Doors.blocked()) {
                        Navigation.stop();
                        Repath = BlockedRecheck;
                        return;
                    }
                    if (Doors.hurrying()) {
                        Repath = DoorRepath;
                    }
                }
            }
            if (Flat < Closest - 0.25) {
                Closest = Flat;
                Stalled = 0;
            } else if (++Stalled > StuckTicks) {
                if (!Route.single()) {
                    Orders.advance();
                }
                reset();
            }
            if (Doors.blocked()) {
                Orders.setStatus(GuardStatus.Blocked);
            } else if (Orders.status() == GuardStatus.Blocked) {
                Orders.setStatus(Route.single() ? GuardStatus.Standing : GuardStatus.Patrolling);
            }
        }

        private void arrive(GuardOrders Orders, PatrolPoint Point) {
            Arrived = true;
            Waited = 0;
            Glance = 0;
            Owner.getNavigation().stop();
            Orders.setStatus(Orders.route().single() ? GuardStatus.Standing : GuardStatus.Patrolling);
            GuardArms.equipDuty(Owner, Orders.route().Rules());
            Gaze = Owner.getEyePosition().add(Vec3.directionFromRotation(0.0f, Point.Facing().toYRot()).scale(8.0));
        }

        private void lookAround(PatrolPoint Point) {
            if (--Glance <= 0) {
                Glance = GlanceMin + Owner.getRandom().nextInt(GlanceSpread);
                float Turn = Owner.getRandom().nextFloat() < 0.4f ? 0.0f : (Owner.getRandom().nextFloat() * 2.0f - 1.0f) * GlanceArc;
                float Tilt = Owner.getRandom().nextFloat() * 15.0f - 5.0f;
                Gaze = Owner.getEyePosition().add(Vec3.directionFromRotation(Tilt, Point.Facing().toYRot() + Turn).scale(8.0));
            }
            Owner.getLookControl().setLookAt(Gaze.x, Gaze.y, Gaze.z, 10.0f, Owner.getMaxHeadXRot());
        }

        private boolean mealBreak() {
            int Food = Owner.food().food();
            boolean Hungry = Food < Owner.trait().hungerThreshold() || Food < 18 && Owner.getHealth() < Owner.getMaxHealth() * 0.6f;
            if (!Hungry) {
                return false;
            }
            int Slot = Owner.findFood(Food <= 6);
            if (Slot < 0) {
                return false;
            }
            Owner.beginEating(Slot);
            return true;
        }
    }
}
