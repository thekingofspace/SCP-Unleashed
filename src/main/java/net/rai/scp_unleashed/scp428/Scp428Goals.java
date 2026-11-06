package net.rai.scp_unleashed.scp428;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.npc.doors.NpcDoors;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp428Goals {
    public static final double HuntSpeed = 1.0;
    public static final double HuntArrive = 0.4;
    public static final double CloseIn = 2.5;
    public static final double HerdArrive = 0.8;
    public static final double RoamSpeed = 0.8;
    public static final double FleeSpeed = 1.3;
    public static final int RoamShortest = 160;
    public static final int RoamLongest = 360;
    public static final int RoamReach = 20;
    public static final int RoamRise = 6;
    public static final double HerdSpeed = 1.35;
    public static final double LeashSpeed = 1.15;
    public static final double HerdReach = 2.6;
    public static final double HerdSpread = 70.0;
    public static final double GuardGap = 1.6;
    public static final int HuddleRefresh = 5;
    public static final int HuddleQuick = 2;
    public static final double HuddleSpeed = 0.9;
    public static final double ShuffleSpeed = 0.55;
    public static final double GuardSpeed = 1.0;
    public static final double BraceSpeed = 1.3;
    public static final double ShuffleReach = 1.2;
    public static final double HuddleSettle = 0.25;
    public static final double HuddleDrift = 0.32;
    public static final double HuddleArrive = 0.35;
    public static final double SqueezeFollow = 0.3;
    public static final double FollowGap = 1.3;
    public static final float FaceTurn = 12.0f;
    public static final double SwipeSpeed = 1.0;
    public static final double SwipeArrive = 0.6;
    public static final int NoShotRest = 20;
    public static final double SpotShift = 0.18;
    public static final double StallGain = 0.04;
    public static final int StallTicks = 30;
    public static final int SidestepTicks = 12;
    public static final double SidestepWidth = 0.8;
    public static final double SettleSpeed = 0.45;
    public static final double SettleSlope = 1.2;
    public static final double DirectReach = 3.5;
    public static final double ArcStep = 35.0;
    public static final double ArcNear = 0.9;
    public static final float TurnSpeed = 6.0f;
    public static final double GraspSettle = 0.2;
    public static final double GraspSpeed = 1.0;
    public static final double ClutchArrive = 0.6;

    private Scp428Goals() {
    }

    public static class Hold extends Goal {
        private final Scp428Core Core;

        public Hold(Scp428Core Core) {
            this.Core = Core;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Core.isGripping();
        }

        @Override
        public void start() {
            Core.getNavigation().stop();
        }

        @Override
        public void tick() {
            Core.getNavigation().stop();
        }
    }

    public static class Flee extends Goal {
        private final Scp428Core Core;
        private int Repath;

        public Flee(Scp428Core Core) {
            this.Core = Core;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Core.fleeLeft() > 0 && !Core.isGripping() && threat() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Core.getNavigation().stop();
        }

        @Nullable
        private Entity threat() {
            if (Core.fleeFrom() == null || !(Core.level() instanceof ServerLevel World)) {
                return null;
            }
            return World.getEntity(Core.fleeFrom());
        }

        @Override
        public void tick() {
            Entity From = threat();
            if (From == null || --Repath > 0) {
                return;
            }
            Repath = 10;
            Vec3 Away = DefaultRandomPos.getPosAway(Core, 12, 6, From.position());
            if (Away != null) {
                Core.getNavigation().moveTo(Away.x, Away.y, Away.z, FleeSpeed);
            }
        }
    }

    public static class Hunt extends Goal {
        private final Scp428Core Core;

        public Hunt(Scp428Core Core) {
            this.Core = Core;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Core.getTarget();
            return Target != null && Target.isAlive() && !Core.isGripping() && Core.fleeLeft() <= 0;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Core.agent().reset();
        }

        @Override
        public void stop() {
            Core.getNavigation().stop();
            Core.agent().reset();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Core.getTarget();
            if (Target == null || !(Core.level() instanceof ServerLevel World)) {
                return;
            }
            if (Core.inGrabRange(Target) && Scp428Quarry.sees(Core, Target) || Core.inGrabRange(Target) && Core.distanceTo(Target) < Core.radius() + 1.0) {
                Core.getNavigation().stop();
                Core.startGrip(Target);
                return;
            }
            if (Core.distanceTo(Target) < 8.0) {
                Core.getLookControl().setLookAt(Target, TurnSpeed, 30.0f);
            }
            double Gap = Math.sqrt(Core.distanceToSqr(Target.getX(), Core.getY(), Target.getZ()));
            if (Gap < Core.radius() + CloseIn && Scp428Quarry.sees(Core, Target) && !Core.agent().busy()) {
                Core.getNavigation().stop();
                Core.getMoveControl().setWantedPosition(Target.getX(), Target.getY(), Target.getZ(), HuntSpeed);
                return;
            }
            Core.agent().route(Core, World, Target.position(), HuntSpeed, HuntArrive);
        }
    }

    public static class Roam extends Goal {
        private final Scp428Core Core;
        @Nullable
        private Vec3 Wander;
        private int Left;

        public Roam(Scp428Core Core) {
            this.Core = Core;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Core.getTarget() == null && !Core.isGripping() && Core.fleeLeft() <= 0;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Wander = null;
            Left = 0;
        }

        @Override
        public void stop() {
            Core.getNavigation().stop();
            Core.agent().reset();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(Core.level() instanceof ServerLevel World)) {
                return;
            }
            if (Wander == null || --Left <= 0 || Core.position().distanceTo(Wander) <= Scp428Doorwork.Arrived) {
                Wander = pick();
                Left = RoamShortest + Core.getRandom().nextInt(RoamLongest - RoamShortest);
                Core.agent().reset();
            }
            if (Wander != null) {
                Core.agent().route(Core, World, Wander, RoamSpeed);
            }
        }

        @Nullable
        private Vec3 pick() {
            Vec3 Found = LandRandomPos.getPos(Core, RoamReach, RoamRise);
            if (Found != null) {
                return Found;
            }
            double Angle = Core.getRandom().nextDouble() * Math.PI * 2.0;
            double Reach = 8.0 + Core.getRandom().nextDouble() * 10.0;
            return Core.position().add(Math.cos(Angle) * Reach, 0.0, Math.sin(Angle) * Reach);
        }
    }

    public static class Grasp extends Goal {
        private final Scp428Instance Body;

        public Grasp(Scp428Instance Body) {
            this.Body = Body;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Body.isHolding();
        }

        @Override
        public void start() {
            Body.getNavigation().stop();
            Body.agent().reset();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp428Core Core = Body.core();
            if (Core == null || !(Body.level() instanceof ServerLevel World) || Body.holdingVictim() == null) {
                return;
            }
            Entity Victim = World.getEntity(Body.holdingVictim());
            if (Victim != null) {
                Body.getLookControl().setLookAt(Victim.getX(), Victim.getY() + Victim.getBbHeight() * 0.75, Victim.getZ(), 30.0f, 30.0f);
            }
            Vec3 Slot = Core.slotPoint(Body);
            if (Slot == null) {
                return;
            }
            if (Body.position().distanceToSqr(Slot.x, Body.getY(), Slot.z) > GraspSettle * GraspSettle) {
                Body.getMoveControl().setWantedPosition(Slot.x, Slot.y, Slot.z, GraspSpeed);
            } else {
                Body.getNavigation().stop();
            }
        }
    }

    public static class Clutch extends Goal {
        private final Scp428Instance Body;
        @Nullable
        private LivingEntity Victim;

        public Clutch(Scp428Instance Body) {
            this.Body = Body;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Scp428Core Core = Body.core();
            if (Core == null || !Body.canGrab()) {
                return false;
            }
            Victim = Core.clutchTarget(Body);
            return Victim != null && !(Victim == Core.angerTarget() && Body.armed() && Body.distanceTo(Victim) > Scp428Arms.ShootClose);
        }

        @Override
        public boolean canContinueToUse() {
            Scp428Core Core = Body.core();
            return Core != null && Victim != null && Victim.isAlive() && !Body.isHolding() && Core.canClutch(Body, Victim);
        }

        @Override
        public void start() {
            Body.agent().reset();
        }

        @Override
        public void stop() {
            Victim = null;
            Body.getNavigation().stop();
            Body.agent().reset();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp428Core Core = Body.core();
            if (Core == null || Victim == null || !(Body.level() instanceof ServerLevel World)) {
                return;
            }
            Body.getLookControl().setLookAt(Victim, 30.0f, 30.0f);
            if (Body.distanceTo(Victim) <= Core.joinReach() && Body.hasLineOfSight(Victim)) {
                Core.join(Body, Victim);
                return;
            }
            Body.agent().route(Body, World, Victim.position(), Scp428Instance.ClutchSpeed * Core.pace(), ClutchArrive);
        }
    }

    public static class Herd extends Goal {
        private final Scp428Instance Body;

        public Herd(Scp428Instance Body) {
            this.Body = Body;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Scp428Core Core = Body.core();
            return Core != null && (Core.getTarget() != null || Core.isGripping());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Body.agent().reset();
        }

        @Override
        public void stop() {
            Body.getNavigation().stop();
            Body.agent().reset();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp428Core Core = Body.core();
            if (Core == null || !(Body.level() instanceof ServerLevel World)) {
                return;
            }
            int Slot = Math.max(0, Core.slotOf(Body.recordId()));
            int Count = Math.max(1, Core.count());
            Vec3 Point;
            if (Core.isGripping()) {
                double Angle = Slot * Math.PI * 2.0 / Count;
                double Ring = Core.radius() + GuardGap + (Slot % 2) * 0.8;
                Point = Core.position().add(Math.cos(Angle) * Ring, 0.0, Math.sin(Angle) * Ring);
            } else {
                LivingEntity Target = Core.getTarget();
                if (Target == null) {
                    return;
                }
                Vec3 Toward = new Vec3(Target.getX() - Core.getX(), 0.0, Target.getZ() - Core.getZ());
                if (Toward.lengthSqr() < 1.0e-4) {
                    Toward = new Vec3(1.0, 0.0, 0.0);
                }
                Toward = Toward.normalize();
                double Share = Count <= 1 ? 0.0 : (double) Slot / (Count - 1) - 0.5;
                double Turn = Math.toRadians(Share * HerdSpread * 2.0);
                Vec3 Spread = new Vec3(Toward.x * Math.cos(Turn) - Toward.z * Math.sin(Turn), 0.0, Toward.x * Math.sin(Turn) + Toward.z * Math.cos(Turn));
                double Reach = Body.distanceTo(Target) < 3.0 ? 1.2 : HerdReach;
                Point = Target.position().add(Spread.scale(Reach));
                Point = Body.tethered(Point);
            }
            Body.agent().route(Body, World, Point, HerdSpeed * Core.pace(), HerdArrive);
        }
    }

    public static class Leash extends Goal {
        private final Scp428Instance Body;

        public Leash(Scp428Instance Body) {
            this.Body = Body;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Scp428Core Core = Body.core();
            return Core != null && Body.distanceTo(Core) > Scp428Instance.SoftRange;
        }

        @Override
        public boolean canContinueToUse() {
            Scp428Core Core = Body.core();
            return Core != null && Body.distanceTo(Core) > Scp428Instance.SoftRange * 0.6;
        }

        @Override
        public void stop() {
            Body.getNavigation().stop();
            Body.agent().reset();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp428Core Core = Body.core();
            if (Core != null && Body.level() instanceof ServerLevel World) {
                Body.agent().route(Body, World, Core.position(), LeashSpeed);
            }
        }
    }

    public static class Swipe extends Goal {
        private final Scp428Instance Body;
        private int Into;

        public Swipe(Scp428Instance Body) {
            this.Body = Body;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Body.swipeTarget() != null && !Body.isHolding();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Into = -1;
            Body.agent().reset();
        }

        @Override
        public void stop() {
            Body.endSwipe();
            Body.clearSwipe();
            Body.getNavigation().stop();
            Scp428Record Entry = Body.record();
            if (Entry != null) {
                Body.arm(Entry);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos Scanner = Body.swipeTarget();
            Scp428Record Entry = Body.record();
            if (Scanner == null || Entry == null || !(Body.level() instanceof ServerLevel World)) {
                Body.clearSwipe();
                return;
            }
            ItemStack Card = NpcDoors.pickCard(World, Scanner, Entry.carried()).orElse(null);
            if (Card == null || Body.waitSwipe()) {
                Body.clearSwipe();
                return;
            }
            Vec3 Centre = Vec3.atCenterOf(Scanner);
            Body.getLookControl().setLookAt(Centre.x, Centre.y, Centre.z, 30.0f, 30.0f);
            if (Into < 0) {
                if (!Scp428Doorwork.inReach(Body, Scanner)) {
                    Body.agent().route(Body, World, Centre, SwipeSpeed, SwipeArrive);
                    return;
                }
                Body.getNavigation().stop();
                Scp428Core Core = Body.core();
                if (Core != null && !Core.agent().ready(Core)) {
                    return;
                }
                Into = 0;
                Body.setItemSlot(EquipmentSlot.MAINHAND, Card.copyWithCount(1));
                Body.startSwipe(Body.tickCount);
            }
            Into++;
            if (Into == Scp428Instance.SwipeHit) {
                NpcDoors.swipe(Body, Card, Scanner);
            }
            if (Into >= Scp428Instance.SwipeTicks) {
                Body.clearSwipe();
            }
        }
    }

    public static class Shoot extends Goal {
        private final Scp428Instance Body;
        private int Drawn;
        private int Rest;

        public Shoot(Scp428Instance Body) {
            this.Body = Body;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private LivingEntity target() {
            Scp428Core Core = Body.core();
            return Core == null ? null : Core.angerTarget();
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = target();
            ItemStack Weapon = Body.weapon();
            return Target != null && Weapon != null && !Body.isHolding() && Body.swipeTarget() == null && Scp428Arms.usable(Body, Weapon)
                    && Body.distanceTo(Target) <= Scp428Arms.range(Weapon) && Body.distanceTo(Target) > Scp428Arms.ShootClose * 0.5;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Drawn = 0;
            Body.getNavigation().stop();
            Body.agent().reset();
            Body.setAiming(true);
        }

        @Override
        public void stop() {
            Body.setAiming(false);
            Body.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = target();
            ItemStack Weapon = Body.weapon();
            Scp428Record Entry = Body.record();
            if (Target == null || Weapon == null || Entry == null || !(Body.level() instanceof ServerLevel World)) {
                return;
            }
            Body.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (!Scp428Arms.clear(Body, Target)) {
                Drawn = 0;
                Body.agent().route(Body, World, Target.position(), HerdSpeed, HerdArrive);
                return;
            }
            Body.getNavigation().stop();
            if (Rest > 0) {
                Rest--;
                return;
            }
            if (++Drawn < Scp428Arms.draw(Weapon) || !Scp428Arms.ready(Body, Weapon)) {
                return;
            }
            Drawn = 0;
            Rest = Scp428Arms.fire(Body, Entry, Weapon, Target) ? Scp428Arms.rest(Weapon) : NoShotRest;
        }
    }

    public static class Huddle extends Goal {
        private final Scp428Instance Body;
        @Nullable
        private Vec3 Spot;
        private int Refresh;
        private boolean Settled;
        private byte Last;
        @Nullable
        private Vec3 SettledAt;
        private double BestFlat = Double.MAX_VALUE;
        private int Stall;
        private int Sidestep;
        private double SideSign = 1.0;

        public Huddle(Scp428Instance Body) {
            this.Body = Body;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Scp428Core Core = Body.core();
            return Core != null && Core.isResting() && !Body.isHolding();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Spot = null;
            Refresh = 0;
            Settled = false;
            Last = Scp428Instance.Loose;
            BestFlat = Double.MAX_VALUE;
            Stall = 0;
            Sidestep = 0;
            Body.agent().reset();
        }

        @Override
        public void stop() {
            Spot = null;
            Settled = false;
            Body.setHuddle(Scp428Instance.Loose);
            Body.getNavigation().stop();
            Body.agent().reset();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp428Core Core = Body.core();
            if (Core == null || !(Body.level() instanceof ServerLevel World)) {
                return;
            }
            boolean Guard = Core.guards(Body);
            boolean Brace = Core.bracing();
            byte Mode = Brace ? Scp428Instance.Bracing : Guard ? Scp428Instance.Guarding : Scp428Instance.Huddled;
            if (Mode != Last) {
                Last = Mode;
                Settled = false;
                Refresh = 0;
                BestFlat = Double.MAX_VALUE;
                Stall = 0;
            }
            if (--Refresh <= 0 || Spot == null) {
                Spot = Core.huddleSpot(World, Body);
                Refresh = Guard || Brace ? HuddleQuick : HuddleRefresh;
            }
            if (Spot == null || Core.squeeze() > SqueezeFollow) {
                Settled = false;
                Body.agent().route(Body, World, new Vec3(Core.getX(), Core.floorLevel(), Core.getZ()), HuddleSpeed, Core.radius() + FollowGap);
                Body.getLookControl().setLookAt(Core.getX(), Core.getY() + Core.getBbHeight() * 0.5, Core.getZ(), 10.0f, 30.0f);
                Body.setHuddle(Scp428Instance.Huddled);
                return;
            }
            double Flat = Math.sqrt(Body.distanceToSqr(Spot.x, Body.getY(), Spot.z));
            if (Settled && SettledAt != null && Math.sqrt(SettledAt.distanceToSqr(Spot.x, SettledAt.y, Spot.z)) > SpotShift) {
                Settled = false;
            }
            double Speed = Brace ? BraceSpeed : Guard ? GuardSpeed : Flat > ShuffleReach ? HuddleSpeed : ShuffleSpeed;
            if (Flat > DirectReach) {
                Settled = false;
                Body.agent().route(Body, World, Spot, Speed, HuddleArrive);
            } else if (Flat > (Settled ? HuddleDrift : HuddleSettle)) {
                Settled = false;
                Body.getNavigation().stop();
                if (Flat < BestFlat - StallGain) {
                    BestFlat = Flat;
                    Stall = 0;
                } else if (++Stall > StallTicks) {
                    Stall = 0;
                    BestFlat = Flat;
                    Sidestep = SidestepTicks;
                    SideSign = -SideSign;
                }
                Vec3 Step = Sidestep-- > 0 ? sidestep(Spot) : around(Core, Spot, Flat);
                Body.getMoveControl().setWantedPosition(Step.x, Spot.y, Step.z, Math.min(Speed, Math.max(SettleSpeed, Flat * SettleSlope)));
            } else {
                if (!Settled) {
                    SettledAt = Spot;
                    BestFlat = Double.MAX_VALUE;
                    Stall = 0;
                    Sidestep = 0;
                }
                Settled = true;
                Body.getNavigation().stop();
            }
            LivingEntity Watch = Guard ? Core.intruder() : null;
            double Lx;
            double Lz;
            if (Watch != null) {
                Body.getLookControl().setLookAt(Watch, 30.0f, 30.0f);
                Lx = Watch.getX() - Body.getX();
                Lz = Watch.getZ() - Body.getZ();
            } else {
                Lx = Spot.x - Core.getX();
                Lz = Spot.z - Core.getZ();
                double Length = Math.sqrt(Lx * Lx + Lz * Lz);
                if (Length > 1.0e-4) {
                    Lx /= Length;
                    Lz /= Length;
                }
                Body.getLookControl().setLookAt(Body.getX() + Lx * 4.0, Body.getEyeY(), Body.getZ() + Lz * 4.0, 10.0f, 30.0f);
            }
            if (Settled && Lx * Lx + Lz * Lz > 1.0e-6) {
                float Facing = (float) (Mth.atan2(Lz, Lx) * Mth.RAD_TO_DEG) - 90.0f;
                float Turned = Mth.approachDegrees(Body.getYRot(), Facing, FaceTurn);
                Body.setYRot(Turned);
                Body.setYBodyRot(Turned);
            }
            Body.setHuddle(Mode);
        }

        private Vec3 sidestep(Vec3 Spot) {
            double Dx = Spot.x - Body.getX();
            double Dz = Spot.z - Body.getZ();
            double Length = Math.max(1.0e-4, Math.sqrt(Dx * Dx + Dz * Dz));
            return new Vec3(Body.getX() + Dx * 0.5 - Dz / Length * SidestepWidth * SideSign, Spot.y, Body.getZ() + Dz * 0.5 + Dx / Length * SidestepWidth * SideSign);
        }

        private Vec3 around(Scp428Core Core, Vec3 Spot, double Flat) {
            double Here = Math.atan2(Body.getZ() - Core.getZ(), Body.getX() - Core.getX());
            double There = Math.atan2(Spot.z - Core.getZ(), Spot.x - Core.getX());
            double Turn = Mth.wrapDegrees(Math.toDegrees(There - Here));
            if (Math.abs(Turn) <= ArcStep || Flat <= ArcNear) {
                return Spot;
            }
            double Ring = Math.max(Math.sqrt(Body.distanceToSqr(Core.getX(), Body.getY(), Core.getZ())), Math.sqrt((Spot.x - Core.getX()) * (Spot.x - Core.getX())
                    + (Spot.z - Core.getZ()) * (Spot.z - Core.getZ())));
            double Next = Here + Math.toRadians(Math.copySign(ArcStep, Turn));
            return new Vec3(Core.getX() + Math.cos(Next) * Ring, Spot.y, Core.getZ() + Math.sin(Next) * Ring);
        }
    }
}
