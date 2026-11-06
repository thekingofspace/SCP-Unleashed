package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public class AntlerStalker extends CourtMinion {
    public static final int Leap = SpecialA;
    public static final int LeapFlight = 16;
    public static final int DropFlight = 20;
    public static final double DropReach = 10.0;
    public static final double Elevated = 4.0;
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private boolean Struck;
    private Vec3 Landing = Vec3.ZERO;
    private int Flight = LeapFlight;

    public AntlerStalker(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.AntlerStalker);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Pounce(this));
        behaviour(3, new Slash(this));
        behaviour(4, new Flank(this));
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double Y, boolean Ground, BlockState State, BlockPos Pos) {
        if (Ground) {
            fallDistance = 0.0f;
        }
    }

    public static Vec3 ballistic(Vec3 From, Vec3 To, int Ticks) {
        double Gravity = 0.08;
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        double Dy = To.y - From.y;
        double Vy = (Dy + 0.5 * Gravity * Ticks * Ticks) / Ticks;
        double Drag = 0.91;
        double Across = (1.0 - Math.pow(Drag, Ticks)) / (1.0 - Drag);
        return new Vec3(Dx / Across, Math.min(1.6, Vy), Dz / Across);
    }

    @Override
    protected void actionTick(int Action, int Elapsed) {
        LivingEntity Target = getTarget();
        if (Action == Leap && Target != null && level() instanceof ServerLevel World) {
            if (Elapsed == 1) {
                Landing = landing(World, Target);
                if (!CourtKeeper.fits(World, Landing, getBbWidth(), getBbHeight()) && ScarletCourt.Instance.active()) {
                    Landing = CourtKeeper.clearOr(World, ScarletCourt.Instance.dais(), Landing, getBbWidth(), getBbHeight());
                }
            }
            if (Elapsed < 12) {
                getNavigation().stop();
                getLookControl().setLookAt(Target, 40.0f, 40.0f);
                if (Elapsed % 3 == 0) {
                    CourtFx.ring(World, CourtFx.Crimson, Landing, 1.2, 10, 0.1);
                }
            } else if (Elapsed == 12) {
                setDeltaMovement(ballistic(position(), Landing, Flight));
                hurtMarked = true;
                Struck = false;
                play("leap", 1.1f, 1.0f);
            } else if (!Struck && distanceToSqr(Target) < 2.2 * 2.2) {
                Struck = true;
                CourtFx.strike(this, Target, (float) Kind.damage() * 1.2f);
            }
            return;
        }
        if (Action == Attack && Elapsed == 2) {
            play("idle", 0.9f, 1.3f);
        }
        super.actionTick(Action, Elapsed);
    }

    private Vec3 landing(ServerLevel World, LivingEntity Target) {
        if (getY() - Target.getY() <= Elevated) {
            Flight = LeapFlight;
            return Target.position();
        }
        Flight = DropFlight;
        Vec3 Way = CourtFx.flat(Target.position().subtract(position()));
        double Across = Math.min(DropReach, Math.sqrt(Math.pow(Target.getX() - getX(), 2) + Math.pow(Target.getZ() - getZ(), 2)));
        Vec3 Spot = position().add(Way.scale(Across));
        BlockPos Ground = CourtArena.ground(World, BlockPos.containing(Spot.x, Target.getY(), Spot.z), 8);
        return Ground != null ? Vec3.atBottomCenterOf(Ground) : new Vec3(Spot.x, Target.getY(), Spot.z);
    }

    public boolean elevated(LivingEntity Target) {
        return getY() - Target.getY() > Elevated;
    }

    @Override
    protected RawAnimation moving() {
        Vec3 Motion = getDeltaMovement();
        return Motion.x * Motion.x + Motion.z * Motion.z > 0.03 ? RunLoop : WalkLoop;
    }

    @Override
    protected String clip(int Action) {
        return switch (Action) {
            case Attack -> "attack";
            case Leap -> "leap";
            default -> null;
        };
    }

    public static class Pounce extends Goal {
        private final AntlerStalker Owner;
        private int Wait = 20;

        public Pounce(AntlerStalker Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (--Wait > 0) {
                return false;
            }
            LivingEntity Target = Owner.getTarget();
            if (Target == null || Owner.busy() || Owner.rising() || !Owner.onGround() || !CourtFx.validTarget(Target)) {
                return false;
            }
            Vec3 Gap = Target.position().subtract(Owner.position());
            double Across = Math.sqrt(Gap.x * Gap.x + Gap.z * Gap.z);
            if (Owner.elevated(Target)) {
                return Across >= 2.0 && Across <= 80.0;
            }
            return Across >= 4.0 && Across <= 16.0 && Owner.hasLineOfSight(Target);
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.action() == Leap;
        }

        @Override
        public void start() {
            Owner.startAction(Leap, 12 + (Owner.elevated(Owner.getTarget()) ? DropFlight : LeapFlight) + 8);
            Wait = 100;
        }
    }

    public static class Slash extends CourtGoals.Melee {
        public Slash(CourtMinion Owner) {
            super(Owner, 1.2, 18, 14);
        }
    }

    public static class Flank extends Goal {
        private final AntlerStalker Owner;
        private int Repath;

        public Flank(AntlerStalker Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Target != null && !Owner.busy() && Owner.distanceTo(Target) > 5.0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Owner.getTarget();
            if (Target == null || --Repath > 0) {
                return;
            }
            Repath = 10;
            Vec3 Behind = Target.position().subtract(Vec3.directionFromRotation(0.0f, Target.getYRot()).scale(3.5));
            Owner.getNavigation().moveTo(Behind.x, Behind.y, Behind.z, 1.25);
        }
    }
}
