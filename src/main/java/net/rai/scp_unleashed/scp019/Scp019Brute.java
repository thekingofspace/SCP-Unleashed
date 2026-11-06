package net.rai.scp_unleashed.scp019;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.UUID;

public class Scp019Brute extends Scp019Creature {
    public static final double Speed = 0.3;
    public static final double GuardSpeed = 0.6;
    public static final double ReturnSpeed = 0.75;
    public static final double ChaseSpeed = 1.05;
    public static final double Health = 60.0;
    public static final double Damage = 9.0;
    public static final int EmergeTicks = 48;
    public static final int FormTicks = 40;
    public static final int SlamTicks = 14;
    public static final int SlamHitTick = 6;
    public static final double SlamReach = 1.2;
    public static final int LostTicks = 60;
    public static final int SearchTicks = 90;
    public static final int OrderTicks = 900;
    public static final double GuardRadius = 4.5;
    public static final double PostReach = 2.0;
    public static final Scp019Gait.Shape BruteGait = new Scp019Gait.Shape(
            new Scp019Gait.Profile(14.0, 0.0595, 0.5, 2.6, new double[]{0.984, 0.484}, new float[]{1.0f, 1.0f}),
            new Scp019Gait.Profile(10.0, 0.1953, 0.6, 2.0, new double[]{0.987, 0.08, 0.427, 0.516}, new float[]{1.0f, 0.85f, 0.75f, 0.65f}),
            0.15, 0.11, 3, Scp019Registry.BruteStep, Scp019Registry.BruteThud, 0.5f, 0.35f, 1.0f, 28.0, false);
    private static final EntityDataAccessor<Boolean> GuardingData = SynchedEntityData.defineId(Scp019Brute.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation GuardLoop = RawAnimation.begin().thenLoop("guard");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation FormPlay = RawAnimation.begin().thenPlayAndHold("form");
    private static final RawAnimation SlamPlay = RawAnimation.begin().thenPlayAndHold("attack");

    @Nullable
    private UUID OrderTarget;
    @Nullable
    private Vec3 OrderSpot;
    private long OrderExpires;
    private int LastSeenTick;
    @Nullable
    private Vec3 LastSeenPos;
    private final int Slot;
    @Nullable
    private LivingEntity SlamTarget;

    public Scp019Brute(EntityType<? extends Scp019Brute> Type, Level World) {
        super(Type, World);
        Slot = random.nextInt(6);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.ATTACK_KNOCKBACK, 0.6).add(Attributes.ARMOR, 6.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(GuardingData, false);
    }

    @Override
    public int emergeTicks() {
        return EmergeTicks;
    }

    @Override
    public int formTicks() {
        return FormTicks;
    }

    @Override
    public double leash() {
        return Scp019PotBlockEntity.BruteLeash;
    }

    @Override
    protected Scp019Gait.Shape gaitShape() {
        return BruteGait;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp019Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp019Goals.BruteMelee(this, ChaseSpeed));
        goalSelector.addGoal(3, new Scp019Goals.BruteHunt(this));
        goalSelector.addGoal(4, new Scp019Goals.BruteReturn(this));
        goalSelector.addGoal(5, new Scp019Goals.BruteGuard(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new Scp019Goals.BruteDefend(this));
    }

    @Override
    public String state() {
        byte Current = action();
        if (Current == ActionEmerge || Current == ActionForm) {
            return "forming";
        }
        if (getTarget() != null) {
            return "attacking";
        }
        if (OrderSpot != null) {
            return "tracking";
        }
        return "guarding";
    }

    public boolean isGuarding() {
        return entityData.get(GuardingData);
    }

    public void setGuarding(boolean Value) {
        if (entityData.get(GuardingData) != Value) {
            entityData.set(GuardingData, Value);
        }
    }

    public Vec3 centre() {
        Scp019PotBlockEntity Home = home();
        return Home != null ? Home.centre() : anchor();
    }

    public Vec3 post() {
        Vec3 Centre = centre();
        double Angle = Slot * Math.PI / 3.0 + 0.4;
        return new Vec3(Centre.x + Math.cos(Angle) * GuardRadius, Centre.y, Centre.z + Math.sin(Angle) * GuardRadius);
    }

    public boolean inLeash(Vec3 Point) {
        return Scp019PotBlockEntity.inLeash(centre(), Point, leash());
    }

    @Nullable
    public Vec3 orderSpot() {
        return OrderSpot;
    }

    @Nullable
    public LivingEntity orderTarget() {
        if (OrderTarget == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(OrderTarget) instanceof LivingEntity Found && Found.isAlive() ? Found : null;
    }

    public void clearOrder() {
        OrderTarget = null;
        OrderSpot = null;
    }

    public boolean hasOrder() {
        if (OrderSpot != null && level().getGameTime() > OrderExpires) {
            clearOrder();
        }
        return OrderSpot != null;
    }

    public boolean order(@Nullable LivingEntity Target, Vec3 LastSeen) {
        LivingEntity Current = getTarget();
        if (Current != null && Current.isAlive() && Current != Target) {
            return false;
        }
        if (Current == null && hasOrder() && OrderTarget != null && (Target == null || !OrderTarget.equals(Target.getUUID())) && isValidTarget(orderTarget())) {
            return false;
        }
        if (Target != null && !isValidTarget(Target)) {
            return false;
        }
        if (!inLeash(LastSeen)) {
            return false;
        }
        OrderTarget = Target != null ? Target.getUUID() : null;
        OrderSpot = LastSeen;
        OrderExpires = level().getGameTime() + OrderTicks;
        setGuarding(false);
        if (Target != null && !locksMovement() && getSensing().hasLineOfSight(Target)) {
            setTarget(Target);
            LastSeenTick = tickCount;
            LastSeenPos = Target.position();
        }
        return true;
    }

    public void roar() {
        float Pitch = 0.92f + random.nextFloat() * 0.12f;
        playSound(Scp019Registry.BruteRoar.get(), 2.6f, Pitch);
        playSound(Scp019Registry.BruteRoarLow.get(), 2.0f, Pitch);
    }

    @Override
    protected void serverTick() {
        if ((tickCount + getId()) % 5 != 0) {
            return;
        }
        LivingEntity Target = getTarget();
        if (Target == null) {
            return;
        }
        Vec3 Centre = centre();
        if (!isValidTarget(Target) || !Scp019PotBlockEntity.inLeash(Centre, Target.position(), leash())
                || !Scp019PotBlockEntity.inLeash(Centre, position(), leash() + 4.0)) {
            setTarget(null);
            clearOrder();
            return;
        }
        if (getSensing().hasLineOfSight(Target)) {
            LastSeenTick = tickCount;
            LastSeenPos = Target.position();
            setGuarding(false);
        } else if (tickCount - LastSeenTick > LostTicks) {
            Vec3 Spot = LastSeenPos != null ? LastSeenPos : Target.position();
            setTarget(null);
            OrderTarget = Target.getUUID();
            OrderSpot = Spot;
            OrderExpires = level().getGameTime() + OrderTicks;
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity Target) {
        if (Target != null && !isValidTarget(Target)) {
            Target = null;
        }
        super.setTarget(Target);
        if (Target != null) {
            LastSeenTick = tickCount;
            LastSeenPos = Target.position();
        }
    }

    public void beginSlam(LivingEntity Target) {
        SlamTarget = Target;
        setAction(ActionSlam, SlamTicks);
        playSound(Scp019Registry.BruteAttackSwing.get(), 0.9f, 0.85f + random.nextFloat() * 0.15f);
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind != ActionSlam) {
            return;
        }
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        LivingEntity Target = SlamTarget;
        if (Target != null) {
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
        }
        if (SlamTicks - Remaining == SlamHitTick && Target != null && Target.isAlive()) {
            double Reach = getBbWidth() * 0.5 + Target.getBbWidth() * 0.5 + SlamReach;
            if (distanceToSqr(Target) <= Reach * Reach && getSensing().hasLineOfSight(Target)) {
                doHurtTarget(Target);
            }
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind == ActionSlam) {
            SlamTarget = null;
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (Hit) {
            playSound(Scp019Registry.BruteAttack.get(), 1.2f, 0.9f + random.nextFloat() * 0.15f);
        }
        return Hit;
    }

    @Override
    public void die(DamageSource Source) {
        Scp019PotBlockEntity Home = home();
        super.die(Source);
        if (dead && Home != null) {
            Home.bruteFell(this);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isActing() ? null : Scp019Registry.BruteGrunt.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp019Registry.BruteHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp019Registry.BruteDeath.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    public double distanceToPost() {
        Vec3 Post = post();
        double Dx = Post.x - getX();
        double Dz = Post.z - getZ();
        return Mth.sqrt((float) (Dx * Dx + Dz * Dz));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp019GaitController<>(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Current == ActionNone ? 4 : Scp019GaitController.blendFor(Current));
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionForm) {
                return Event.setAndContinue(FormPlay);
            }
            if (Current == ActionSlam) {
                return Event.setAndContinue(SlamPlay);
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? RunLoop : WalkLoop);
            }
            return Event.setAndContinue(isGuarding() ? GuardLoop : IdleLoop);
        }));
    }
}
