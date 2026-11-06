package net.rai.scp_unleashed.scp553;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp553Goals {
    public static final int Repath = 12;
    public static final double OrbitRadius = 2.6;
    public static final double OrbitSpread = 1.2;
    public static final double OrbitRise = 1.1;
    public static final double OrbitLift = 1.4;
    public static final double OrbitSpin = 0.07;
    public static final double DiveRange = 6.0;
    public static final int DiveMin = 40;
    public static final int DiveMax = 80;
    public static final int DiveLimit = 26;
    public static final int RecoverMin = 12;
    public static final int RecoverMax = 22;
    public static final int BreedCheck = 20;
    public static final int BreedChance = 3;
    public static final int BreedPatience = 220;
    public static final double HoverReach = 0.45;
    public static final int PerchChance = 160;
    public static final int PerchDepth = 6;
    public static final int RestMin = 200;
    public static final int RestMax = 600;
    public static final double StartleRange = 2.0;
    public static final int DriftChance = 6;
    public static final int DriftReach = 6;
    public static final int DriftRise = 2;
    public static final int DriftPatience = 100;
    public static final double ScatterReach = 6.0;

    private Scp553Goals() {
    }

    public static boolean clearLine(Scp553Butterfly Butterfly, Vec3 To) {
        Vec3 From = Butterfly.position().add(0.0, Butterfly.getBbHeight() * 0.5, 0.0);
        HitResult Hit = Butterfly.level().clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, Butterfly));
        return Hit.getType() == HitResult.Type.MISS;
    }

    public static boolean roomAt(Scp553Butterfly Butterfly, Vec3 Spot) {
        AABB Box = Butterfly.getDimensions(Butterfly.getPose()).makeBoundingBox(Spot);
        return Butterfly.level().noCollision(Butterfly, Box) && !Butterfly.level().containsAnyLiquid(Box.inflate(0.2));
    }

    public static void head(Scp553Butterfly Butterfly, Vec3 Spot, double Modifier, int[] Clock) {
        Vec3 Centre = Spot.add(0.0, Butterfly.getBbHeight() * 0.5, 0.0);
        if (clearLine(Butterfly, Centre)) {
            Butterfly.getNavigation().stop();
            Butterfly.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, Modifier);
            return;
        }
        if (--Clock[0] <= 0) {
            Clock[0] = Repath;
            Butterfly.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Modifier);
        }
    }

    public static void face(Scp553Butterfly Butterfly, Vec3 Spot, float Step) {
        float Yaw = (float) (Mth.atan2(Spot.z - Butterfly.getZ(), Spot.x - Butterfly.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        Butterfly.setYRot(Mth.approachDegrees(Butterfly.getYRot(), Yaw, Step));
        Butterfly.yBodyRot = Butterfly.getYRot();
        Butterfly.yHeadRot = Butterfly.getYRot();
    }

    public static final class Seek extends NearestAttackableTargetGoal<LivingEntity> {
        private final Scp553Butterfly Butterfly;

        public Seek(Scp553Butterfly Butterfly) {
            super(Butterfly, LivingEntity.class, 10, true, false, Scp553Butterfly::isQuarry);
            this.Butterfly = Butterfly;
        }

        @Override
        public boolean canUse() {
            return Butterfly.isAgitated() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Butterfly.isAgitated() && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            LivingEntity Target = Butterfly.getTarget();
            if (Target != null) {
                Butterfly.rally(Target);
            }
        }
    }

    public static final class Swarm extends Goal {
        private final Scp553Butterfly Butterfly;
        private final int[] Clock = {0};
        private int Timer;
        private int Phase;
        @Nullable
        private Vec3 Away;

        public Swarm(Scp553Butterfly Butterfly) {
            this.Butterfly = Butterfly;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean valid(@Nullable LivingEntity Target) {
            return Target != null && Target.isAlive() && Butterfly.isAgitated() && Scp553Butterfly.isQuarry(Target);
        }

        @Override
        public boolean canUse() {
            return valid(Butterfly.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Butterfly.setPerched(false);
            Butterfly.setGliding(false);
            Phase = 0;
            Timer = Scp553Goals.DiveMin / 2 + Butterfly.getRandom().nextInt(DiveMax - DiveMin + 1);
        }

        @Override
        public void stop() {
            if (Butterfly.isDiving()) {
                Butterfly.clearAction();
            }
            Butterfly.getNavigation().stop();
            Away = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private Vec3 orbitPoint(LivingEntity Target) {
            int Id = Butterfly.getId();
            double Angle = Butterfly.tickCount * OrbitSpin + Id * 2.39996;
            double Radius = OrbitRadius + (Id % 3) * OrbitSpread * 0.5;
            double Rise = OrbitLift + ((Id * 7) % 5) / 4.0 * OrbitRise;
            return new Vec3(Target.getX() + Math.cos(Angle) * Radius, Target.getY() + Rise, Target.getZ() + Math.sin(Angle) * Radius);
        }

        private Vec3 aim(LivingEntity Target) {
            return new Vec3(Target.getX(), Target.getY() + Target.getBbHeight() * 0.6 - Butterfly.getBbHeight() * 0.5, Target.getZ());
        }

        private void recover(LivingEntity Target) {
            Phase = 2;
            RandomSource Random = Butterfly.getRandom();
            Timer = RecoverMin + Random.nextInt(RecoverMax - RecoverMin + 1);
            Butterfly.clearAction();
            Vec3 Out = Butterfly.position().subtract(Target.position());
            double Angle = Out.horizontalDistanceSqr() > 1.0e-4 ? Math.atan2(Out.z, Out.x) : Random.nextDouble() * Math.PI * 2.0;
            Angle += (Random.nextDouble() - 0.5) * 1.2;
            Vec3 Spot = new Vec3(Target.getX() + Math.cos(Angle) * 3.5, Target.getY() + 2.0 + Random.nextDouble(), Target.getZ() + Math.sin(Angle) * 3.5);
            Away = roomAt(Butterfly, Spot) ? Spot : Butterfly.position().add(0.0, 1.0, 0.0);
        }

        @Override
        public void tick() {
            LivingEntity Target = Butterfly.getTarget();
            if (!valid(Target)) {
                return;
            }
            Vec3 Aim = aim(Target);
            if (Phase == 1) {
                face(Butterfly, Target.position(), 40.0f);
                Butterfly.getNavigation().stop();
                Butterfly.getMoveControl().setWantedPosition(Aim.x, Aim.y, Aim.z, Scp553Butterfly.DiveModifier);
                if (Butterfly.touching(Target)) {
                    Butterfly.strike(Target);
                    recover(Target);
                    return;
                }
                if (--Timer <= 0) {
                    recover(Target);
                }
                return;
            }
            if (Phase == 2) {
                if (Away != null) {
                    Butterfly.getMoveControl().setWantedPosition(Away.x, Away.y, Away.z, Scp553Butterfly.RecoverModifier);
                }
                if (--Timer <= 0) {
                    Phase = 0;
                    Away = null;
                    Timer = DiveMin + Butterfly.getRandom().nextInt(DiveMax - DiveMin + 1);
                }
                return;
            }
            head(Butterfly, orbitPoint(Target), Scp553Butterfly.OrbitModifier, Clock);
            double Distance = Butterfly.position().distanceTo(Aim);
            if (Distance < 4.0) {
                face(Butterfly, Target.position(), 20.0f);
            }
            if (--Timer <= 0 && Distance < DiveRange && Butterfly.hasLineOfSight(Target)) {
                Phase = 1;
                Timer = DiveLimit;
                Butterfly.setAction(Scp553Butterfly.ActionDive, DiveLimit);
                Butterfly.playLayers(Scp553Registry.DiveLayers, 0.6f, 1.0f + Butterfly.getRandom().nextFloat() * 0.2f);
            } else if (Timer < -40) {
                Timer = DiveMin / 2;
            }
        }
    }

    public static final class Scatter extends Goal {
        private final Scp553Butterfly Butterfly;
        @Nullable
        private Vec3 Spot;

        public Scatter(Scp553Butterfly Butterfly) {
            this.Butterfly = Butterfly;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Butterfly.spooked() || Butterfly.isAgitated()) {
                return false;
            }
            Spot = pick();
            return Spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Spot != null && Butterfly.spooked() && !Butterfly.isAgitated() && Butterfly.position().distanceToSqr(Spot) > 0.3;
        }

        @Override
        public void start() {
            Butterfly.setPerched(false);
            Butterfly.setGliding(false);
        }

        @Override
        public void stop() {
            Spot = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Spot != null) {
                Butterfly.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, Scp553Butterfly.ScatterModifier);
            }
        }

        @Nullable
        private Vec3 pick() {
            RandomSource Random = Butterfly.getRandom();
            Vec3 From = Butterfly.spookedFrom() != null ? Butterfly.spookedFrom() : Butterfly.position().add(Random.nextDouble() - 0.5, 0.0, Random.nextDouble() - 0.5);
            Vec3 Away = Butterfly.position().subtract(From);
            double Base = Away.horizontalDistanceSqr() > 1.0e-4 ? Math.atan2(Away.z, Away.x) : Random.nextDouble() * Math.PI * 2.0;
            for (int Attempt = 0; Attempt < 10; Attempt++) {
                double Angle = Base + (Random.nextDouble() - 0.5) * 1.6;
                double Reach = ScatterReach * (0.6 + Random.nextDouble() * 0.4);
                Vec3 Candidate = Butterfly.position().add(Math.cos(Angle) * Reach, 0.5 + Random.nextDouble() * 1.5, Math.sin(Angle) * Reach);
                if (roomAt(Butterfly, Candidate) && clearLine(Butterfly, Candidate.add(0.0, Butterfly.getBbHeight() * 0.5, 0.0))) {
                    return Candidate;
                }
            }
            return null;
        }
    }

    public static final class Breed extends Goal {
        private final Scp553Butterfly Butterfly;
        private final int[] Clock = {0};
        private int Check;
        private int Patience;
        private boolean Launched;
        @Nullable
        private BlockPos Nest;
        @Nullable
        private Vec3 Hover;

        public Breed(Scp553Butterfly Butterfly) {
            this.Butterfly = Butterfly;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!Butterfly.readyToBreed() || Butterfly.spooked() || !(Butterfly.level() instanceof ServerLevel World)) {
                return false;
            }
            if (--Check > 0) {
                return false;
            }
            Check = BreedCheck;
            if (Butterfly.getRandom().nextInt(BreedChance) != 0 || !Scp553Colony.dark(World, Butterfly.blockPosition())) {
                return false;
            }
            if (!Scp553Colony.roomToBreed(World, Butterfly.blockPosition())) {
                Butterfly.bred(false);
                return false;
            }
            Nest = Scp553Colony.findNest(World, Butterfly);
            return Nest != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Nest != null && Patience > 0 && Butterfly.getTarget() == null && !Butterfly.spooked();
        }

        @Override
        public void start() {
            Patience = BreedPatience;
            Launched = false;
            Butterfly.setPerched(false);
            Butterfly.setGliding(false);
            Hover = Nest != null ? Scp553Colony.hoverBelow(Butterfly.level(), Nest, Butterfly) : null;
        }

        @Override
        public void stop() {
            if (!Launched) {
                Butterfly.bred(false);
            }
            if (Butterfly.isSpitting()) {
                Butterfly.clearAction();
            }
            Butterfly.getNavigation().stop();
            Nest = null;
            Hover = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Nest == null || !(Butterfly.level() instanceof ServerLevel World)) {
                return;
            }
            Patience--;
            Vec3 Face = Scp553Colony.faceOf(Nest);
            face(Butterfly, Face, 25.0f);
            Butterfly.getLookControl().setLookAt(Face.x, Face.y, Face.z);
            if (Butterfly.isSpitting()) {
                Butterfly.setDeltaMovement(Butterfly.getDeltaMovement().scale(0.5));
                if (!Launched && Butterfly.actionAge() >= Scp553Butterfly.SpitLaunch) {
                    Launched = true;
                    boolean Seeded = launch(World, Nest);
                    Butterfly.bred(Seeded);
                    Patience = Math.min(Patience, Scp553Butterfly.SpitTicks - Scp553Butterfly.SpitLaunch);
                }
                return;
            }
            if (Launched) {
                Patience = 0;
                return;
            }
            Vec3 Eye = Butterfly.getEyePosition();
            boolean Near = Hover != null && Butterfly.position().distanceTo(Hover) < HoverReach;
            boolean Close = Eye.distanceTo(Face) < Scp553Colony.SpitReach && Scp553Colony.clearShot(World, Eye, Nest, Butterfly);
            if (Near && Close || Close && Patience < BreedPatience / 2) {
                Butterfly.getNavigation().stop();
                Butterfly.setAction(Scp553Butterfly.ActionSpit, Scp553Butterfly.SpitTicks);
                Butterfly.playLayers(Scp553Registry.HockLayers, 0.8f, 0.95f + Butterfly.getRandom().nextFloat() * 0.15f);
                return;
            }
            if (Hover != null) {
                head(Butterfly, Hover, Scp553Butterfly.DriftModifier * 1.4, Clock);
            }
        }

        private boolean launch(ServerLevel World, BlockPos Spot) {
            Vec3 Mouth = Butterfly.getEyePosition().add(Butterfly.getLookAngle().scale(0.15));
            Scp553Colony.spit(World, Butterfly, Mouth);
            Scp553Colony.spitTrail(World, Mouth, Scp553Colony.faceOf(Spot));
            if (!Scp553Colony.roomToBreed(World, Spot)) {
                return false;
            }
            return Scp553Colony.seed(World, Spot);
        }
    }

    public static final class Perch extends Goal {
        private final Scp553Butterfly Butterfly;
        @Nullable
        private Vec3 Spot;
        private int Left;
        private boolean Landed;

        public Perch(Scp553Butterfly Butterfly) {
            this.Butterfly = Butterfly;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        private boolean calm() {
            return !Butterfly.isAgitated() && Butterfly.getTarget() == null && !Butterfly.spooked() && !Butterfly.isInWater() && Butterfly.action() == Scp553Butterfly.ActionNone;
        }

        private boolean startled() {
            return !Butterfly.level().getEntitiesOfClass(Player.class, Butterfly.getBoundingBox().inflate(StartleRange),
                    Found -> !Found.isSpectator() && !Found.isShiftKeyDown()).isEmpty();
        }

        @Override
        public boolean canUse() {
            if (!calm() || Butterfly.getRandom().nextInt(reducedTickDelay(PerchChance)) != 0) {
                return false;
            }
            Spot = ground();
            return Spot != null;
        }

        @Nullable
        private Vec3 ground() {
            Vec3 From = Butterfly.position();
            BlockHitResult Hit = Butterfly.level().clip(new ClipContext(From, From.subtract(0.0, PerchDepth, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, Butterfly));
            if (Hit.getType() != HitResult.Type.BLOCK || Hit.getDirection() != Direction.UP || !Butterfly.level().getFluidState(Hit.getBlockPos()).isEmpty()) {
                return null;
            }
            Vec3 Place = new Vec3(From.x, Hit.getLocation().y + 0.001, From.z);
            return roomAt(Butterfly, Place) ? Place : null;
        }

        @Override
        public boolean canContinueToUse() {
            return Spot != null && Left > 0 && calm() && !(Landed && startled());
        }

        @Override
        public void start() {
            Left = RestMin + Butterfly.getRandom().nextInt(RestMax - RestMin + 1);
            Landed = false;
            Butterfly.setGliding(false);
            Butterfly.setLanding(true);
        }

        @Override
        public void stop() {
            if (Landed) {
                Butterfly.setDeltaMovement(0.0, 0.08, 0.0);
            }
            Butterfly.setPerched(false);
            Butterfly.setLanding(false);
            Spot = null;
            Landed = false;
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
            if (!Landed) {
                Butterfly.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, Scp553Butterfly.DriftModifier);
                if (Butterfly.position().distanceToSqr(Spot) < 0.01) {
                    Landed = true;
                    Butterfly.setPos(Spot.x, Spot.y, Spot.z);
                    Butterfly.setPerched(true);
                }
                return;
            }
            Left--;
            Butterfly.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static final class Drift extends Goal {
        private final Scp553Butterfly Butterfly;
        @Nullable
        private Vec3 Spot;
        private int Patience;

        public Drift(Scp553Butterfly Butterfly) {
            this.Butterfly = Butterfly;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Butterfly.getTarget() != null || Butterfly.isPerched() || Butterfly.getRandom().nextInt(reducedTickDelay(DriftChance)) != 0) {
                return false;
            }
            Spot = pick();
            return Spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Spot != null && Patience > 0 && Butterfly.getTarget() == null && Butterfly.position().distanceToSqr(Spot) > 0.3;
        }

        @Override
        public void start() {
            Patience = DriftPatience;
        }

        @Override
        public void stop() {
            Spot = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Patience--;
            if (Spot != null) {
                double Modifier = Butterfly.isAgitated() ? Scp553Butterfly.DriftModifier * 1.6 : Scp553Butterfly.DriftModifier;
                Butterfly.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, Modifier);
            }
        }

        @Nullable
        private Vec3 pick() {
            RandomSource Random = Butterfly.getRandom();
            Vec3 Centre = Butterfly.position();
            BlockPos Home = Butterfly.home();
            if (Home != null && Vec3.atCenterOf(Home).distanceTo(Centre) > Scp553Butterfly.HomeRange) {
                Vec3 Back = Vec3.atCenterOf(Home).subtract(Centre).normalize().scale(DriftReach * 0.8);
                Centre = Centre.add(Back);
            }
            for (int Attempt = 0; Attempt < 10; Attempt++) {
                Vec3 Candidate = Centre.add((Random.nextDouble() * 2.0 - 1.0) * DriftReach, (Random.nextDouble() * 2.0 - 1.0) * DriftRise, (Random.nextDouble() * 2.0 - 1.0) * DriftReach);
                if (roomAt(Butterfly, Candidate) && clearLine(Butterfly, Candidate.add(0.0, Butterfly.getBbHeight() * 0.5, 0.0))) {
                    return Candidate;
                }
            }
            return null;
        }
    }
}
