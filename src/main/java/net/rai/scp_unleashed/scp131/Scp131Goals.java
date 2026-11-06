package net.rai.scp_unleashed.scp131;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public final class Scp131Goals {
    public static final int RepathTicks = 10;
    public static final int PersonRestMin = 300;
    public static final int PersonRestSpread = 500;
    public static final int PersonCheck = 20;
    public static final int PersonChance = 4;
    public static final double PersonRange = 12.0;
    public static final double PersonLose = 22.0;
    public static final int InterestMin = 420;
    public static final int InterestSpread = 520;
    public static final double GazeDistance = 2.6;
    public static final double FollowDistance = 4.8;
    public static final double LoopRadius = 2.0;
    public static final double LoopStep = 0.12;
    public static final int LoopChance = 220;
    public static final double SprintSpeed = 0.2;
    public static final double CalmSpeed = 0.12;
    public static final int CalmTicks = 20;
    public static final double RaceSide = 1.4;
    public static final double RaceLead = 1.0;
    public static final int RaceChirp = 34;
    public static final int ZoomRestMin = 160;
    public static final int ZoomRestSpread = 260;
    public static final int ZoomChance = 30;
    public static final int ZoomTicks = 120;
    public static final int ZoomReach = 16;
    public static final int ZoomMin = 7;
    public static final int WanderInterval = 50;
    public static final int SadWanderInterval = 140;
    public static final double SadModifier = 0.7;
    public static final double KeepAwayRange = 7.0;
    public static final double KeepAwayModifier = 0.9;
    public static final int MournCheck = 40;
    public static final double MournRange = 40.0;
    public static final double MournRing = 1.6;
    public static final int MournLingerMin = 300;
    public static final int MournLingerSpread = 260;
    public static final double MournModifier = 0.6;
    public static final int GlanceChance = 50;
    public static final double GlanceRange = 9.0;
    public static final int GlanceMin = 40;
    public static final int GlanceSpread = 60;
    public static final float BodyTurn = 18.0f;

    private Scp131Goals() {
    }

    public static boolean moving(Entity Body) {
        double Dx = Body.getX() - Body.xo;
        double Dz = Body.getZ() - Body.zo;
        return Dx * Dx + Dz * Dz > 2.5000003E-7;
    }

    public static final class Turning extends BodyRotationControl {
        private final Scp131Pod Pod;

        public Turning(Scp131Pod Pod) {
            super(Pod);
            this.Pod = Pod;
        }

        @Override
        public void clientTick() {
            if (moving(Pod)) {
                Pod.yBodyRot = Pod.getYRot();
                Pod.yHeadRot = Mth.rotateIfNecessary(Pod.yHeadRot, Pod.yBodyRot, 1.0f);
                return;
            }
            Pod.yBodyRot = Mth.approachDegrees(Pod.yBodyRot, Pod.yHeadRot, BodyTurn);
        }
    }

    public static final class Flee extends Goal {
        private final Scp131Pod Pod;
        private int Repath;

        public Flee(Scp131Pod Pod) {
            this.Pod = Pod;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Pod.fleeingFrom() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Pod.fleeingFrom() != null;
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Pod.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            UUID From = Pod.fleeingFrom();
            if (From == null || !(Pod.level() instanceof ServerLevel World)) {
                return;
            }
            Entity Attacker = World.getEntity(From);
            Vec3 Threat = Attacker != null ? Attacker.position() : Pod.position().add(Vec3.directionFromRotation(0.0f, Pod.getYRot()));
            if (--Repath <= 0 || Pod.getNavigation().isDone()) {
                Repath = RepathTicks * 2;
                Vec3 Away = DefaultRandomPos.getPosAway(Pod, 10, 4, Threat);
                if (Away != null && Scp131MoveControl.safeColumn(Pod.level(), BlockPos.containing(Away.x, Away.y + 0.2, Away.z))) {
                    Pod.getNavigation().moveTo(Away.x, Away.y, Away.z, Scp131Pod.FleeModifier);
                }
            }
        }
    }

    public static final class KeepAway extends Goal {
        private final Scp131Pod Pod;
        @Nullable
        private LivingEntity Threat;
        private int Repath;

        public KeepAway(Scp131Pod Pod) {
            this.Pod = Pod;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Nullable
        private LivingEntity nearest() {
            AABB Around = Pod.getBoundingBox().inflate(KeepAwayRange, 3.0, KeepAwayRange);
            return Pod.level().getEntitiesOfClass(LivingEntity.class, Around, Pod::isWaryOf).stream()
                    .min(Comparator.comparingDouble(Found -> Found.distanceToSqr(Pod))).orElse(null);
        }

        @Override
        public boolean canUse() {
            if (Pod.tickCount % 5 != 0) {
                return false;
            }
            Threat = nearest();
            return Threat != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Threat != null && Threat.isAlive() && Pod.isWaryOf(Threat) && Threat.distanceToSqr(Pod) < (KeepAwayRange + 3.0) * (KeepAwayRange + 3.0);
        }

        @Override
        public void start() {
            Repath = 0;
            if (Pod.session() != null) {
                Pod.session().end(false);
            }
        }

        @Override
        public void stop() {
            Threat = null;
            Pod.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (Threat == null) {
                return;
            }
            Pod.setFocus(Threat);
            if (--Repath <= 0 || Pod.getNavigation().isDone()) {
                Repath = RepathTicks;
                Vec3 Away = DefaultRandomPos.getPosAway(Pod, 9, 4, Threat.position());
                if (Away != null) {
                    Pod.getNavigation().moveTo(Away.x, Away.y, Away.z, KeepAwayModifier);
                }
            }
        }
    }

    public static final class Mourn extends Goal {
        private final Scp131Pod Pod;
        @Nullable
        private Scp131Grief.Spot Spot;
        private Vec3 Stand = Vec3.ZERO;
        private int Linger;
        private int Repath;

        public Mourn(Scp131Pod Pod) {
            this.Pod = Pod;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!Pod.isSad() || (Pod.tickCount + Pod.getId()) % MournCheck != 0 || !(Pod.level() instanceof ServerLevel World)) {
                return false;
            }
            Optional<Scp131Grief.Spot> Found = Scp131Grief.get(World.getServer()).nearestSpot(World, Pod.position(), MournRange);
            if (Found.isEmpty() || Pod.hasMourned(Found.get().Id())) {
                return false;
            }
            Spot = Found.get();
            double Turn = (Pod.getUUID().getLeastSignificantBits() & 1023) / 1024.0 * Math.PI * 2.0;
            Stand = Spot.Where().add(Math.cos(Turn) * MournRing, 0.0, Math.sin(Turn) * MournRing);
            Linger = MournLingerMin + Pod.getRandom().nextInt(MournLingerSpread);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Spot != null && Pod.isSad() && Linger > 0;
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            if (Spot != null) {
                Pod.markMourned(Spot.Id());
            }
            Spot = null;
            Pod.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Spot == null) {
                return;
            }
            Vec3 Where = Spot.Where();
            double Distance = Pod.position().distanceToSqr(Stand.x, Pod.getY(), Stand.z);
            if (Distance < 0.8 || Pod.position().distanceToSqr(Where) < MournRing * MournRing + 0.5) {
                Linger--;
                Pod.getNavigation().stop();
                Pod.getLookControl().setLookAt(Where.x, Where.y, Where.z, 10.0f, 30.0f);
                return;
            }
            if (--Repath <= 0 || Pod.getNavigation().isDone()) {
                Repath = RepathTicks * 2;
                if (!Pod.getNavigation().moveTo(Stand.x, Stand.y, Stand.z, MournModifier)) {
                    Linger -= RepathTicks * 2;
                }
            }
        }
    }

    public static final class Play extends Goal {
        private final Scp131Pod Pod;

        public Play(Scp131Pod Pod) {
            this.Pod = Pod;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Scp131Play Session = Pod.session();
            if (Session != null) {
                return !Session.isOver();
            }
            if (!Pod.canPlay() || (Pod.tickCount + Pod.getId()) % Scp131Play.CheckInterval != 0 || Pod.getRandom().nextInt(Scp131Play.StartChance) != 0) {
                return false;
            }
            return Scp131Play.tryStart(Pod);
        }

        @Override
        public boolean canContinueToUse() {
            Scp131Play Session = Pod.session();
            return Session != null && !Session.isOver();
        }

        @Override
        public void stop() {
            Scp131Play Session = Pod.session();
            if (Session != null) {
                Session.end(false);
            }
            Pod.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp131Play Session = Pod.session();
            if (Session != null) {
                Session.tick(Pod);
            }
        }
    }

    public static final class Person extends Goal {
        private static final int Approach = 0;
        private static final int Gaze = 1;
        private static final int Follow = 2;
        private static final int Loop = 3;
        private static final int Race = 4;
        private final Scp131Pod Pod;
        @Nullable
        private LivingEntity Target;
        private int Phase;
        private int Interest;
        private int Repath;
        private int Calm;
        private int Lap;
        private double LoopAngle;
        private int Side = 1;
        private boolean Eager;

        public Person(Scp131Pod Pod) {
            this.Pod = Pod;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!Pod.isFree() || !(Pod.level() instanceof ServerLevel World)) {
                return false;
            }
            UUID Called = Pod.beckoned();
            if (Called != null) {
                Entity Found = World.getEntity(Called);
                if (Found instanceof LivingEntity Living && Scp131Pod.isPerson(Living) && !Pod.isWaryOf(Living)) {
                    Target = Living;
                    Eager = true;
                    return true;
                }
                if (Found == null && Pod.tickCount % PersonCheck == 0) {
                    Pod.clearBeckon();
                }
            }
            if (!Pod.personReady() || (Pod.tickCount + Pod.getId()) % PersonCheck != 0 || Pod.getRandom().nextInt(PersonChance) != 0) {
                return false;
            }
            AABB Around = Pod.getBoundingBox().inflate(PersonRange, 4.0, PersonRange);
            Target = World.getEntitiesOfClass(LivingEntity.class, Around, Found -> Scp131Pod.isPerson(Found) && !Pod.isWaryOf(Found) && Pod.hasLineOfSight(Found)).stream()
                    .min(Comparator.comparingDouble(Found -> Found.distanceToSqr(Pod))).orElse(null);
            Eager = false;
            return Target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Target != null && Target.isAlive() && !Target.isSpectator() && Pod.isFree() && Interest > 0 && Pod.distanceToSqr(Target) < PersonLose * PersonLose
                    && !Pod.isWaryOf(Target) && Target.level() == Pod.level();
        }

        @Override
        public void start() {
            Phase = Approach;
            Interest = InterestMin + Pod.getRandom().nextInt(InterestSpread);
            Repath = 0;
            Calm = 0;
            Pod.setCurious(true);
            Pod.chirp(Scp131Registry.TrillLayers, Scp131Pod.ChirpVolume);
            Pod.clearBeckon();
        }

        @Override
        public void stop() {
            Target = null;
            Pod.setCurious(false);
            Pod.setZooming(false);
            Pod.restAfterPerson(Pod.getRandom());
            Pod.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private double speedOf(LivingEntity Person) {
            Vec3 Motion = Person.position().subtract(Person.xo, Person.yo, Person.zo);
            return Motion.horizontalDistance();
        }

        private boolean sprinting(LivingEntity Person) {
            return Person instanceof Player User ? User.isSprinting() && speedOf(User) > SprintSpeed * 0.7 : speedOf(Person) > SprintSpeed;
        }

        @Override
        public void tick() {
            if (Target == null) {
                return;
            }
            Interest--;
            Pod.setFocus(Target);
            Pod.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            double Distance = Pod.distanceTo(Target);
            if (Phase != Race && sprinting(Target) && Distance < 10.0) {
                Phase = Race;
                Calm = 0;
                Vec3 Heading = Target.getDeltaMovement().multiply(1.0, 0.0, 1.0);
                Vec3 Offset = Pod.position().subtract(Target.position());
                Side = Heading.x * Offset.z - Heading.z * Offset.x > 0.0 ? 1 : -1;
                Pod.chirp(Scp131Registry.ZoomLayers, Scp131Pod.ChirpVolume);
            }
            switch (Phase) {
                case Approach -> approach(Distance);
                case Gaze -> gaze(Distance);
                case Follow -> follow(Distance);
                case Loop -> loop();
                default -> race();
            }
        }

        private void approach(double Distance) {
            if (Distance <= GazeDistance) {
                Phase = Gaze;
                Eager = false;
                Pod.setZooming(false);
                Pod.getNavigation().stop();
                return;
            }
            Pod.setZooming(Eager);
            if (--Repath <= 0 || Pod.getNavigation().isDone()) {
                Repath = RepathTicks;
                Pod.getNavigation().moveTo(Target, Eager ? Scp131Pod.ZoomModifier : Scp131Pod.CruiseModifier);
            }
        }

        private void gaze(double Distance) {
            Pod.getNavigation().stop();
            if (Distance > FollowDistance) {
                Phase = Follow;
                Repath = 0;
                return;
            }
            if (Pod.getRandom().nextInt(LoopChance) == 0 && roomToLoop()) {
                Phase = Loop;
                Lap = 0;
                LoopAngle = Math.atan2(Pod.getZ() - Target.getZ(), Pod.getX() - Target.getX());
                Pod.chirp(Scp131Registry.ChirpLayers, Scp131Pod.ChirpVolume);
            }
        }

        private boolean roomToLoop() {
            for (int Step = 0; Step < 8; Step++) {
                double Turn = Step * Math.PI / 4.0;
                BlockPos Spot = BlockPos.containing(Target.getX() + Math.cos(Turn) * LoopRadius, Pod.getY() + 0.2, Target.getZ() + Math.sin(Turn) * LoopRadius);
                if (!Scp131MoveControl.safeColumn(Pod.level(), Spot) || !Pod.level().getBlockState(Spot).getCollisionShape(Pod.level(), Spot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        private void follow(double Distance) {
            if (Distance <= GazeDistance + 0.4) {
                Phase = Gaze;
                Pod.getNavigation().stop();
                return;
            }
            if (--Repath <= 0 || Pod.getNavigation().isDone()) {
                Repath = RepathTicks;
                Pod.getNavigation().moveTo(Target, Distance > FollowDistance * 2.0 ? Scp131Pod.ZoomModifier : Scp131Pod.CruiseModifier);
            }
        }

        private void loop() {
            Pod.getNavigation().stop();
            LoopAngle += LoopStep;
            Lap++;
            double X = Target.getX() + Math.cos(LoopAngle) * LoopRadius;
            double Z = Target.getZ() + Math.sin(LoopAngle) * LoopRadius;
            Pod.getMoveControl().setWantedPosition(X, Pod.getY(), Z, Scp131Pod.CruiseModifier);
            if (Lap * LoopStep >= Math.PI * 2.0) {
                Phase = Gaze;
                Pod.cheer(Scp131Pod.HappyTicks / 2);
            }
        }

        private void race() {
            double Speed = speedOf(Target);
            Calm = Speed < CalmSpeed ? Calm + 1 : 0;
            if (Calm >= CalmTicks) {
                Phase = Gaze;
                Pod.setZooming(false);
                Pod.cheer(Scp131Pod.HappyTicks);
                Pod.chirp(Scp131Registry.ChirpLayers, Scp131Pod.ChirpVolume);
                return;
            }
            Pod.setZooming(true);
            Vec3 Heading = Target.position().subtract(Target.xo, Target.yo, Target.zo).multiply(1.0, 0.0, 1.0);
            if (Heading.lengthSqr() < 1.0e-6) {
                Heading = Vec3.directionFromRotation(0.0f, Target.getYRot());
            }
            Heading = Heading.normalize();
            Vec3 Right = new Vec3(-Heading.z, 0.0, Heading.x);
            Vec3 Spot = Target.position().add(Heading.scale(RaceLead)).add(Right.scale(Side * RaceSide));
            double Lag = Spot.subtract(Pod.position()).horizontalDistance();
            double Base = Speed / Math.max(0.01, Pod.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.2);
            Pod.getNavigation().stop();
            Pod.getMoveControl().setWantedPosition(Spot.x, Target.getY(), Spot.z, Mth.clamp(Base + Lag * 0.35, 0.4, Scp131Pod.RaceLimit));
            if (Pod.tickCount % RaceChirp == 0 && Pod.chirpReady()) {
                Pod.chirp(Scp131Registry.ZoomLayers, Scp131Pod.ChirpVolume * 0.8f);
            }
        }
    }

    public static final class Zoom extends Goal {
        private final Scp131Pod Pod;
        private int Left;

        public Zoom(Scp131Pod Pod) {
            this.Pod = Pod;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Pod.isFree() || !Pod.zoomReady() || !Pod.onGround() || Pod.getRandom().nextInt(ZoomChance) != 0) {
                return false;
            }
            for (int Attempt = 0; Attempt < 4; Attempt++) {
                Vec3 Found = LandRandomPos.getPos(Pod, ZoomReach, 5);
                if (Found == null || Found.distanceToSqr(Pod.position()) < ZoomMin * ZoomMin) {
                    continue;
                }
                if (Scp131MoveControl.safeColumn(Pod.level(), BlockPos.containing(Found.x, Found.y + 0.2, Found.z))
                        && Pod.getNavigation().moveTo(Found.x, Found.y, Found.z, Scp131Pod.ZoomModifier)) {
                    return true;
                }
            }
            Pod.restAfterZoom(Pod.getRandom());
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return Left > 0 && !Pod.getNavigation().isDone() && Pod.isFree();
        }

        @Override
        public void start() {
            Left = ZoomTicks;
            Pod.setZooming(true);
            Pod.chirp(Scp131Registry.ZoomLayers, Scp131Pod.ChirpVolume);
        }

        @Override
        public void tick() {
            Left--;
        }

        @Override
        public void stop() {
            Pod.setZooming(false);
            Pod.restAfterZoom(Pod.getRandom());
        }
    }

    public static final class Wander extends WaterAvoidingRandomStrollGoal {
        private final Scp131Pod Pod;

        public Wander(Scp131Pod Pod) {
            super(Pod, Scp131Pod.WanderModifier, 0.001f);
            this.Pod = Pod;
            setInterval(WanderInterval);
        }

        @Override
        public boolean canUse() {
            setInterval(Pod.isSad() ? SadWanderInterval : WanderInterval);
            return Pod.session() == null && super.canUse();
        }

        @Override
        public void start() {
            Pod.getNavigation().moveTo(wantedX, wantedY, wantedZ, Pod.isSad() ? SadModifier : Scp131Pod.WanderModifier);
        }

        @Override
        @Nullable
        protected Vec3 getPosition() {
            Vec3 Found = super.getPosition();
            if (Found == null || !Scp131MoveControl.safeColumn(Pod.level(), BlockPos.containing(Found.x, Found.y + 0.2, Found.z))) {
                return null;
            }
            return Found;
        }
    }

    public static final class Glance extends Goal {
        private final Scp131Pod Pod;
        @Nullable
        private Entity Target;
        private int Left;

        public Glance(Scp131Pod Pod) {
            this.Pod = Pod;
            setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Pod.getRandom().nextInt(GlanceChance) != 0) {
                return false;
            }
            AABB Around = Pod.getBoundingBox().inflate(GlanceRange, 3.0, GlanceRange);
            Target = Pod.level().getEntities(Pod, Around, Found -> (Scp131Pod.isPerson(Found) || Scp131Pod.isPod(Found)) && Found.isAlive()).stream()
                    .min(Comparator.comparingDouble(Found -> Found.distanceToSqr(Pod) * (Scp131Pod.isPerson(Found) ? 0.6 : 1.0))).orElse(null);
            return Target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Target != null && Target.isAlive() && Left > 0 && Pod.distanceToSqr(Target) < GlanceRange * GlanceRange * 1.5;
        }

        @Override
        public void start() {
            Left = GlanceMin + Pod.getRandom().nextInt(GlanceSpread);
        }

        @Override
        public void stop() {
            Target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Target == null) {
                return;
            }
            Left--;
            Pod.setFocus(Target);
            Pod.getLookControl().setLookAt(Target.getX(), Target.getEyeY(), Target.getZ(), 20.0f, 30.0f);
        }
    }
}
