package net.rai.scp_unleashed.scp003;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class Scp003Alpha extends Scp003Creature {
    public static final double HealRange = 16.0;
    public static final float HealAmount = 5.0f;
    public static final int HealInterval = 40;
    public static final int ChannelTicks = 16;
    public static final double KinRange = 32.0;
    public static final double KinComfort = 10.0;
    public static final double QuarryRange = 16.0;
    public static final Scp003Gait.Profile AlphaGait = new Scp003Gait.Profile(32.5, 0.0667, 0.5, 3.0, 5, new double[]{0.974, 0.474}, new float[]{1.0f, 1.0f},
            Scp003Registry.AlphaStep, Scp003Registry.AlphaThud, Scp003Registry.BarkCreak, Scp003Registry.RootShuffle,
            0.24f, 0.5f, 0.19f, 0.5f, 0.95f, 0.3f);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation PromotePlay = RawAnimation.begin().thenPlayAndHold("promote");
    private static final RawAnimation AttackOnce = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation ChannelOnce = RawAnimation.begin().thenPlay("channel");
    private int HealCooldown = HealInterval;
    private int PatientId = -1;
    private int Healed;

    public Scp003Alpha(EntityType<? extends Scp003Alpha> Type, Level World) {
        super(Type, World);
        xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 8.0).add(Attributes.ARMOR_TOUGHNESS, 2.0).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public int stage() {
        return 4;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp003Goals.HoldStill(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(4, new StayWithKinGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 14.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                Target -> isQuarry(Target) && Target.distanceToSqr(this) < QuarryRange * QuarryRange));
    }

    public int healed() {
        return Healed;
    }

    @Nullable
    private Scp003Creature patient() {
        return PatientId >= 0 && level().getEntity(PatientId) instanceof Scp003Creature Patient && Patient.isAlive() ? Patient : null;
    }

    @Nullable
    private Scp003Creature findPatient() {
        List<Scp003Creature> Hurt = level().getEntitiesOfClass(Scp003Creature.class, getBoundingBox().inflate(HealRange),
                Kin -> Kin != this && Kin.isAlive() && !Kin.isTransferred() && Kin.getHealth() < Kin.getMaxHealth() - 0.5f && hasLineOfSight(Kin));
        return Hurt.stream().min(Comparator.comparingDouble(Kin -> Kin.getHealth() / Kin.getMaxHealth())).orElse(null);
    }

    @Override
    protected void serverTick() {
        if (tickCount % 20 == 0 && level() instanceof ServerLevel World) {
            Scp003Colony.get(World.getServer()).seeAlpha(this, World);
        }
        if (HealCooldown > 0) {
            HealCooldown--;
            return;
        }
        if (locksMovement() || action() == ActionChannel) {
            return;
        }
        Scp003Creature Patient = findPatient();
        if (Patient == null) {
            HealCooldown = 10;
            return;
        }
        PatientId = Patient.getId();
        HealCooldown = HealInterval;
        setAction(ActionChannel, ChannelTicks);
        triggerAnim("action", "channel");
        playSound(Scp003Registry.AlphaHeal.get(), 0.9f, 0.95f + random.nextFloat() * 0.1f);
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind != ActionChannel || !(level() instanceof ServerLevel World)) {
            return;
        }
        Scp003Creature Patient = patient();
        if (Patient == null) {
            return;
        }
        getLookControl().setLookAt(Patient, 30.0f, 30.0f);
        if (Remaining % 2 == 0) {
            tether(World, Patient, (float) Remaining / ChannelTicks);
        }
    }

    private void tether(ServerLevel World, Scp003Creature Patient, float Phase) {
        Vec3 From = position().add(0.0, getBbHeight() * 0.62, 0.0);
        Vec3 To = Patient.position().add(0.0, Patient.getBbHeight() * 0.55, 0.0);
        Vec3 Span = To.subtract(From);
        int Steps = Math.max(4, (int) (Span.length() * 2.0));
        for (int Step = 0; Step <= Steps; Step++) {
            double Along = (double) Step / Steps;
            double Sag = Math.sin(Along * Math.PI) * 0.35 * (0.5 + 0.5 * Math.sin(Phase * Math.PI * 4.0 + Along * 6.0));
            Vec3 Point = From.add(Span.scale(Along)).add(0.0, Sag, 0.0);
            World.sendParticles(Scp003Registry.Heal.get(), Point.x, Point.y, Point.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind != ActionChannel || !(level() instanceof ServerLevel World)) {
            return;
        }
        Scp003Creature Patient = patient();
        PatientId = -1;
        if (Patient == null || Patient.distanceToSqr(this) > HealRange * HealRange * 1.2) {
            return;
        }
        Patient.heal(HealAmount);
        Healed++;
        World.sendParticles(Scp003Registry.Heal.get(), Patient.getX(), Patient.getY() + Patient.getBbHeight() * 0.5, Patient.getZ(), 14,
                Patient.getBbWidth() * 0.5, Patient.getBbHeight() * 0.3, Patient.getBbWidth() * 0.5, 0.02);
        World.playSound(null, Patient.getX(), Patient.getY() + 0.5, Patient.getZ(), Scp003Registry.AlphaHealTarget.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
    }

    @Override
    public void remove(Entity.RemovalReason Reason) {
        if ((Reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || Reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) && level() instanceof ServerLevel World) {
            Scp003Colony.get(World.getServer()).seeAlpha(this, World);
        }
        super.remove(Reason);
    }

    @Override
    public void writeJade(CompoundTag Data) {
        Data.putInt("Healed", Healed);
        Data.putBoolean("Channeling", action() == ActionChannel);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp003Registry.AlphaAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp003Registry.AlphaHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp003Registry.AlphaDeath.get();
    }

    @Override
    protected Scp003Gait.Profile gaitProfile() {
        return AlphaGait;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (Hit) {
            playSound(Scp003Registry.AlphaAttack.get(), 1.0f, 0.9f + random.nextFloat() * 0.15f);
        }
        return Hit;
    }

    @Override
    protected float getSoundVolume() {
        return 1.3f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Healed", Healed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Healed = Tag.getInt("Healed");
        if (action() == ActionChannel) {
            setAction(ActionNone, 0);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp003GaitController<>(this, "body", 5, Event -> {
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionPromote) {
                return Event.setAndContinue(PromotePlay);
            }
            return Event.setAndContinue(isStriding() ? WalkLoop : IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 3, Event -> PlayState.STOP)
                .triggerableAnim("attack", AttackOnce)
                .triggerableAnim("channel", ChannelOnce));
    }

    private static final class StayWithKinGoal extends Goal {
        private final Scp003Alpha Leader;
        @Nullable
        private Scp003Creature Follow;

        private StayWithKinGoal(Scp003Alpha Leader) {
            this.Leader = Leader;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Leader.getTarget() != null || Leader.locksMovement() || Leader.getRandom().nextInt(40) != 0) {
                return false;
            }
            List<Scp003Creature> Kin = Leader.level().getEntitiesOfClass(Scp003Creature.class, Leader.getBoundingBox().inflate(KinRange), Other -> Other != Leader && Other.isAlive());
            if (Kin.isEmpty()) {
                return false;
            }
            Scp003Creature Nearest = Kin.stream().min(Comparator.comparingDouble(Other -> Other.distanceToSqr(Leader))).orElse(null);
            if (Nearest == null || Nearest.distanceToSqr(Leader) < KinComfort * KinComfort) {
                return false;
            }
            Follow = Nearest;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Follow != null && Follow.isAlive() && Leader.getTarget() == null && Follow.distanceToSqr(Leader) > 16.0 && !Leader.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (Follow != null) {
                Leader.getNavigation().moveTo(Follow, 0.85);
            }
        }

        @Override
        public void stop() {
            Follow = null;
        }
    }
}
