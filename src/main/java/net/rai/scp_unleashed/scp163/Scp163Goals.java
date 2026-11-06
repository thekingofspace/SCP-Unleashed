package net.rai.scp_unleashed.scp163;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public final class Scp163Goals {
    private Scp163Goals() {
    }

    public static class HoldStill extends Goal {
        private final Scp163Castaway Owner;

        public HoldStill(Scp163Castaway Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Owner.isGesturing() || Owner.isQuirking();
        }

        @Override
        public void start() {
            Owner.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Owner.getNavigation().stop();
        }
    }

    public static class StepOff extends Goal {
        public static final int Patience = 60;
        private final Scp163Castaway Owner;
        private final double SpeedModifier;
        @Nullable
        private Vec3 Exit;
        private int Timer;

        public StepOff(Scp163Castaway Owner, double SpeedModifier) {
            this.Owner = Owner;
            this.SpeedModifier = SpeedModifier;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (Owner.isPassenger()) {
                return false;
            }
            BlockPos Perch = Owner.perch();
            if (Perch == null) {
                return false;
            }
            BlockPos Cell = Scp163Footing.side(Owner.level(), Perch, Owner.position(), true);
            Exit = Cell == null ? null : Vec3.atBottomCenterOf(Cell);
            return Exit != null;
        }

        @Override
        public void start() {
            Timer = Patience;
            Owner.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return Exit != null && Timer > 0 && !Owner.isPassenger() && (!Owner.onGround() || Owner.isPerched());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Timer--;
            if (Exit != null) {
                Owner.getMoveControl().setWantedPosition(Exit.x, Exit.y, Exit.z, SpeedModifier);
            }
        }

        @Override
        public void stop() {
            Exit = null;
        }
    }

    public static class Survey extends Goal {
        public static final int WalkTimeout = 200;
        public static final int WalkRetries = 3;
        public static final int AlignTimeout = 40;
        public static final double Arrive = 0.7;
        public static final double Settle = 0.12;
        public static final double AlignSpeed = 0.5;
        private final Scp163Castaway Owner;
        private final double SpeedModifier;
        @Nullable
        private Scp163Survey Scan;
        private Stage Phase = Stage.Done;
        private List<Scp163Survey.Spot> Plan = List.of();
        private int Index;
        private int Timer;
        private int Retries;
        @Nullable
        private Scp163Survey.Spot Target;
        private Vec3 Stand = Vec3.ZERO;

        private enum Stage {
            Scan,
            Walk,
            Align,
            Tap,
            Done
        }

        public Survey(Scp163Castaway Owner, double SpeedModifier) {
            this.Owner = Owner;
            this.SpeedModifier = SpeedModifier;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Owner.surveyReady() && !Owner.isActing() && !Owner.isGasping() && !Owner.isLeashed() && Owner.onGround() && !Owner.isPerched()
                    && Owner.level() instanceof ServerLevel;
        }

        @Override
        public void start() {
            Scan = Owner.beginSurvey();
            Phase = Stage.Scan;
            Plan = List.of();
            Index = 0;
            Owner.getNavigation().stop();
            Owner.setAction(Scp163Castaway.ActionLookUp, Scp163Quirk.LookUp.ticks());
        }

        @Override
        public boolean canContinueToUse() {
            return Phase != Stage.Done && Scan != null && !Owner.isGasping() && !Owner.isLeashed() && !Owner.isGesturing();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Scan == null || !(Owner.level() instanceof ServerLevel World)) {
                Phase = Stage.Done;
                return;
            }
            switch (Phase) {
                case Scan -> {
                    Owner.getNavigation().stop();
                    if (!Scan.isDone()) {
                        Scan.step(World, Scp163Survey.Budget);
                    }
                    if (Scan.isDone() && !Owner.isPlaying(Scp163Castaway.ActionLookUp)) {
                        Plan = Scan.plan(Owner.blockPosition(), Owner.getRandom());
                        next();
                    }
                }
                case Walk -> walk();
                case Align -> align();
                case Tap -> {
                    face();
                    if (!Owner.isPlaying(Scp163Castaway.ActionTap)) {
                        next();
                    }
                }
                default -> {
                }
            }
        }

        private void next() {
            if (Index >= Plan.size()) {
                Phase = Stage.Done;
                return;
            }
            Target = Plan.get(Index++);
            Direction Wall = Target.Wall();
            Vec3 Centre = Vec3.atBottomCenterOf(Target.Cell());
            Stand = Centre.add(-Wall.getStepX() * Scp163Castaway.StandBack, 0.0, -Wall.getStepZ() * Scp163Castaway.StandBack);
            Timer = WalkTimeout;
            Retries = 0;
            Phase = Stage.Walk;
            Owner.getNavigation().moveTo(Centre.x, Centre.y, Centre.z, SpeedModifier);
        }

        private void walk() {
            if (Target == null) {
                next();
                return;
            }
            Vec3 Centre = Vec3.atBottomCenterOf(Target.Cell());
            double Dx = Owner.getX() - Centre.x;
            double Dz = Owner.getZ() - Centre.z;
            if (Dx * Dx + Dz * Dz <= Arrive * Arrive) {
                Owner.getNavigation().stop();
                Timer = AlignTimeout;
                Phase = Stage.Align;
                return;
            }
            if (--Timer <= 0) {
                next();
                return;
            }
            if (Owner.getNavigation().isDone()) {
                if (++Retries > WalkRetries) {
                    next();
                    return;
                }
                Owner.getNavigation().moveTo(Centre.x, Centre.y, Centre.z, SpeedModifier);
            }
        }

        private void align() {
            face();
            double Dx = Owner.getX() - Stand.x;
            double Dz = Owner.getZ() - Stand.z;
            if (Dx * Dx + Dz * Dz <= Settle * Settle || --Timer <= 0) {
                Owner.getNavigation().stop();
                Owner.setAction(Scp163Castaway.ActionTap, Scp163Quirk.Tap.ticks());
                Phase = Stage.Tap;
                return;
            }
            Owner.sidestep(Stand.x, Stand.z, AlignSpeed);
        }

        private void face() {
            if (Target == null) {
                return;
            }
            float Yaw = Target.Wall().toYRot();
            Owner.setYRot(Mth.approachDegrees(Owner.getYRot(), Yaw, 15.0f));
            Owner.yBodyRot = Owner.getYRot();
            Owner.yHeadRot = Owner.getYRot();
        }

        @Override
        public void stop() {
            if (Scan != null && Scan.isDone() && !Owner.isGasping()) {
                Owner.finishSurvey(Scan);
            } else {
                Owner.abandonSurvey();
            }
            Owner.endAction(Scp163Castaway.ActionLookUp);
            Owner.endAction(Scp163Castaway.ActionTap);
            Scan = null;
            Target = null;
            Phase = Stage.Done;
        }
    }

    public static class SeekAir extends Goal {
        public static final int Repath = 40;
        public static final int Hesitate = 10;
        private final Scp163Castaway Owner;
        private final double SpeedModifier;
        @Nullable
        private BlockPos Target;
        private int Wait;

        public SeekAir(Scp163Castaway Owner, double SpeedModifier) {
            this.Owner = Owner;
            this.SpeedModifier = SpeedModifier;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Owner.isGasping() || Owner.isGesturing() || Owner.isLeashed() || !(Owner.level() instanceof ServerLevel World)) {
                return false;
            }
            Target = Owner.nearestAir(World);
            return Target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.isGasping() && Target != null && !Owner.isGesturing() && !Owner.isLeashed();
        }

        @Override
        public void start() {
            Wait = Repath;
            move();
        }

        private void move() {
            if (Target == null) {
                return;
            }
            BlockPos Cell = Scp163Footing.side(Owner.level(), Target, Owner.position(), false);
            Vec3 Spot = Cell == null ? Vec3.atBottomCenterOf(Target) : Vec3.atBottomCenterOf(Cell);
            Owner.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, SpeedModifier);
        }

        @Override
        public void tick() {
            if (Owner.getNavigation().isDone()) {
                Wait = Math.min(Wait, Hesitate);
            }
            if (--Wait <= 0) {
                Wait = Repath;
                if (Owner.level() instanceof ServerLevel World) {
                    Target = Owner.nearestAir(World);
                }
                move();
            }
        }

        @Override
        public void stop() {
            Target = null;
            Owner.getNavigation().stop();
        }
    }

    public static class Recoil extends Goal {
        private final Scp163Castaway Owner;
        private final double SpeedModifier;

        public Recoil(Scp163Castaway Owner, double SpeedModifier) {
            this.Owner = Owner;
            this.SpeedModifier = SpeedModifier;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity From = Owner.recoilFrom();
            if (From == null || Owner.isGasping() || Owner.isGesturing() || Owner.isLeashed()) {
                return false;
            }
            Vec3 Away = DefaultRandomPos.getPosAway(Owner, 7, 3, From.position());
            if (Away == null) {
                return false;
            }
            return Owner.getNavigation().moveTo(Away.x, Away.y, Away.z, SpeedModifier);
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.recoilFrom() != null && !Owner.getNavigation().isDone() && !Owner.isGesturing();
        }
    }

    public static class AvoidUnkind extends AvoidEntityGoal<Player> {
        private final Scp163Castaway Owner;

        public AvoidUnkind(Scp163Castaway Owner, double WalkModifier, double SprintModifier) {
            super(Owner, Player.class, Found -> Found instanceof Player User && !User.isCreative() && !User.isSpectator() && Owner.dislikes(User), 6.0f,
                    WalkModifier, SprintModifier, Found -> true);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return !Owner.isGasping() && !Owner.isActing() && super.canUse();
        }
    }

    public static class Fiddle extends Goal {
        public static final int ScanRadius = 8;
        public static final int ScanHeight = 2;
        public static final double ConsoleStand = 1.05;
        public static final double SignallerStand = 1.0;
        public static final double ArriveSlack = 0.45;
        public static final int Patience = 240;
        public static final int PulseInterval = 20;
        public static final int Repaths = 3;
        public static final double TurnReach = 0.3;
        private final Scp163Castaway Owner;
        private final double SpeedModifier;
        @Nullable
        private BlockPos Target;
        private Vec3 Stand = Vec3.ZERO;
        private int Timeout;
        private int Ticks;
        private int Retries;
        private boolean Working;

        public Fiddle(Scp163Castaway Owner, double SpeedModifier) {
            this.Owner = Owner;
            this.SpeedModifier = SpeedModifier;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!Owner.fiddleReady() || Owner.isGasping() || Owner.isActing() || Owner.isLeashed() || Owner.isPerched() || Owner.getRandom().nextInt(20) != 0) {
                return false;
            }
            if (!findDevice()) {
                Owner.endFiddle();
                return false;
            }
            return true;
        }

        private boolean findDevice() {
            BlockPos Centre = Owner.blockPosition();
            List<BlockPos> Found = new ArrayList<>();
            for (BlockPos Cursor : BlockPos.betweenClosed(Centre.offset(-ScanRadius, -ScanHeight, -ScanRadius), Centre.offset(ScanRadius, ScanHeight, ScanRadius))) {
                if (Scp163DeviceBlock.isDevice(Owner.level().getBlockState(Cursor)) && Owner.isWithinRestriction(Cursor)) {
                    Found.add(Cursor.immutable());
                }
            }
            Found.sort(Comparator.comparingDouble(Device -> Device.distSqr(Centre)));
            for (BlockPos Device : Found) {
                Vec3 Spot = standPoint(Device);
                if (Spot != null) {
                    Target = Device;
                    Stand = Spot;
                    return true;
                }
            }
            Target = null;
            return false;
        }

        @Nullable
        private Vec3 standPoint(BlockPos Device) {
            BlockState State = Owner.level().getBlockState(Device);
            Vec3 Centre = Vec3.atBottomCenterOf(Device);
            if (State.getBlock() instanceof Scp163DeviceBlock Instrument && Instrument.kind() == Scp163DeviceBlock.Kind.Console) {
                Direction Front = State.getValue(Scp163DeviceBlock.FACING);
                if (!Scp163Footing.isClear(Owner.level(), Device.relative(Front))) {
                    return null;
                }
                return Centre.add(Front.getStepX() * ConsoleStand, 0.0, Front.getStepZ() * ConsoleStand);
            }
            BlockPos Cell = Scp163Footing.side(Owner.level(), Device, Owner.position(), false);
            if (Cell == null || Cell.getY() != Device.getY()) {
                return null;
            }
            Vec3 Toward = Vec3.atBottomCenterOf(Cell).subtract(Centre);
            return Centre.add(Toward.normalize().scale(SignallerStand));
        }

        @Override
        public void start() {
            Timeout = Patience;
            Retries = 0;
            Working = false;
            Owner.getNavigation().moveTo(Stand.x, Stand.y, Stand.z, SpeedModifier);
        }

        @Override
        public boolean canContinueToUse() {
            if (Target == null || Timeout <= 0 || Owner.isGasping() || Owner.isGesturing() || Owner.isLeashed()) {
                return false;
            }
            if (!Scp163DeviceBlock.isDevice(Owner.level().getBlockState(Target))) {
                return false;
            }
            return !Working || Ticks > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Timeout--;
            if (Target == null) {
                return;
            }
            Vec3 Device = Vec3.atCenterOf(Target);
            Owner.getLookControl().setLookAt(Device.x, Device.y, Device.z);
            if (!Working) {
                double Dx = Owner.getX() - Stand.x;
                double Dz = Owner.getZ() - Stand.z;
                if (Dx * Dx + Dz * Dz <= ArriveSlack * ArriveSlack || Owner.getNavigation().isDone() && Owner.position().distanceTo(Device) < 2.0) {
                    Working = true;
                    Ticks = Scp163Castaway.FiddleTicks;
                    Owner.getNavigation().stop();
                    Owner.setAction(Scp163Castaway.ActionFiddle, Scp163Castaway.FiddleTicks + 2);
                } else if (Owner.getNavigation().isDone()) {
                    if (++Retries > Repaths) {
                        Timeout = 0;
                        return;
                    }
                    Owner.getNavigation().moveTo(Stand.x, Stand.y, Stand.z, SpeedModifier);
                }
                return;
            }
            double Dx = Device.x - Owner.getX();
            double Dz = Device.z - Owner.getZ();
            float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
            if (Dx * Dx + Dz * Dz > TurnReach * TurnReach && Math.abs(Mth.wrapDegrees(Yaw - Owner.getYRot())) > Scp163MoveControl.TurnDeadZone) {
                Owner.setYRot(Mth.approachDegrees(Owner.getYRot(), Yaw, 12.0f));
            }
            Owner.yBodyRot = Owner.getYRot();
            if (Ticks % PulseInterval == 0 && Owner.level() instanceof ServerLevel World) {
                Scp163DeviceBlock.pulse(World, Target, PulseInterval + 10);
                if (Owner.getRandom().nextInt(3) == 0) {
                    Owner.playLayers(Scp163Registry.ChitterLayers, 0.5f, 1.15f + Owner.getRandom().nextFloat() * 0.1f);
                }
            }
            Ticks--;
        }

        @Override
        public void stop() {
            Target = null;
            Working = false;
            Owner.endFiddle();
        }
    }

    public static class Greet extends Goal {
        public static final int Gaze = 24;
        private final Scp163Castaway Owner;
        @Nullable
        private LivingEntity Guest;
        private int Ticks;

        public Greet(Scp163Castaway Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Owner.isActing() || Owner.isGasping() || Owner.tickCount % 10 != 0) {
                return false;
            }
            AABB Area = Owner.getBoundingBox().inflate(Scp163Castaway.GreetRange);
            List<LivingEntity> Found = Owner.level().getEntitiesOfClass(LivingEntity.class, Area, Candidate -> candidate(Candidate));
            LivingEntity Best = null;
            double BestDistance = Double.MAX_VALUE;
            for (LivingEntity Candidate : Found) {
                double Distance = Candidate.distanceToSqr(Owner);
                if (Distance < BestDistance) {
                    BestDistance = Distance;
                    Best = Candidate;
                }
            }
            Guest = Best;
            return Guest != null;
        }

        private boolean candidate(LivingEntity Candidate) {
            if (Candidate == Owner || !Candidate.isAlive() || Candidate.isSpectator() || !Targeting.isTargetable(Candidate) || !Owner.mayGreet(Candidate) || !Owner.hasLineOfSight(Candidate)) {
                return false;
            }
            return Candidate instanceof Player || Candidate.getType().is(Scp163Registry.PersonLike);
        }

        @Override
        public void start() {
            Ticks = Gaze;
        }

        @Override
        public boolean canContinueToUse() {
            return Guest != null && Guest.isAlive() && Ticks > 0 && !Owner.isGasping();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Guest == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Guest, 30.0f, 30.0f);
            if (--Ticks == 0) {
                Owner.greet(Guest);
            }
        }

        @Override
        public void stop() {
            Guest = null;
        }
    }
}
