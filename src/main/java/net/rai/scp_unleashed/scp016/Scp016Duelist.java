package net.rai.scp_unleashed.scp016;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class Scp016Duelist extends Scp016Spawn {
    public static final byte ActionLunge = 3;
    public static final byte ActionGuard = 4;
    public static final byte ActionParry = 5;
    public static final byte ActionStagger = 6;
    public static final double Health = 34.0;
    public static final double Speed = 0.29;
    public static final double Damage = 6.0;
    public static final double Armour = 6.0;
    public static final double Toughness = 1.0;
    public static final double Steadiness = 0.5;
    public static final int ComboTicks = 32;
    public static final int[] ComboHits = {26, 17, 7};
    public static final float[] ComboShares = {1.0f, 1.0f, 1.35f};
    public static final double ComboReach = 2.3;
    public static final int ComboCooldown = 26;
    public static final int LungeTicks = 20;
    public static final int LungeDashAt = 14;
    public static final int LungeHitUntil = 8;
    public static final double LungeMin = 4.0;
    public static final double LungeMax = 6.5;
    public static final double LungeSpeed = 1.2;
    public static final float LungeShare = 1.4f;
    public static final double LungeLift = 0.15;
    public static final double LungeReach = 1.6;
    public static final int LungeCooldown = 90;
    public static final int GuardTicks = 24;
    public static final int GuardCooldown = 70;
    public static final int GuardAfterParry = 40;
    public static final int GuardBrokenCooldown = 100;
    public static final double GuardArc = 0.35;
    public static final double CloseGuard = 3.5;
    public static final int CloseGuardChance = 30;
    public static final double ProjectileWatch = 8.0;
    public static final double ProjectileSpeed = 0.6;
    public static final int ParryTicks = 10;
    public static final int RiposteAt = 6;
    public static final double RiposteReach = 2.6;
    public static final int StaggerTicks = 30;
    public static final int ChallengeEvery = 20;
    public static final double ChallengeMargin = 2.0;
    public static final Scp016Gait.Profile DuelistGait = new Scp016Gait.Profile(20.0, 0.0625, 0.5, 3.5, 4,
            new double[]{0.989, 0.542}, new float[]{1.0f, 0.8f},
            Scp016Registry.DuelistStep, Scp016Registry.DuelistDrag, null, null, 0.35f, 0.25f, 0.0f, 0.0f, 0.95f, 0.0f);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ShudderLoop = RawAnimation.begin().thenLoop("shudder");
    private static final RawAnimation GuardLoop = RawAnimation.begin().thenLoop("guard");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation ComboPlay = RawAnimation.begin().thenPlayAndHold("combo");
    private static final RawAnimation LungePlay = RawAnimation.begin().thenPlayAndHold("lunge");
    private static final RawAnimation ParryPlay = RawAnimation.begin().thenPlayAndHold("parry");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    @Nullable
    private LivingEntity Foe;
    private int ComboWait;
    private int LungeWait;
    private int GuardWait;

    public Scp016Duelist(EntityType<? extends Scp016Duelist> Type, Level World) {
        super(Type, World);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, Steadiness)
                .add(Attributes.FOLLOW_RANGE, Follow).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public String variant() {
        return "duelist";
    }

    @Override
    public boolean hasDeathAnimation() {
        return true;
    }

    @Override
    protected void registerGoals() {
        registerKinGoals(true, false);
        targetSelector.addGoal(2, new Scp016Goals.Challenge(this, ChallengeEvery, ChallengeMargin));
        goalSelector.addGoal(2, new Scp016Goals.Duel(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
    }

    public static double threat(LivingEntity Target) {
        return Target.getAttributeValue(Attributes.ARMOR) + Target.getAttributeValue(Attributes.ARMOR_TOUGHNESS) * 0.5
                + (Target.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? Target.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0);
    }

    public boolean isGuarding() {
        return action() == ActionGuard;
    }

    @Override
    public boolean locksMovement() {
        byte Current = action();
        return Current != ActionNone && Current != ActionLunge;
    }

    public boolean readyToCombo() {
        return ComboWait <= 0 && !isActing();
    }

    public boolean readyToLunge() {
        return LungeWait <= 0 && !isActing() && onGround();
    }

    public boolean readyToGuard() {
        return GuardWait <= 0 && !isActing();
    }

    public void beginCombo(LivingEntity Target) {
        Foe = Target;
        setAction(ActionStrike, ComboTicks);
    }

    public void beginLunge(LivingEntity Target) {
        Foe = Target;
        setAction(ActionLunge, LungeTicks);
        playSound(Scp016Registry.DuelistGuard.get(), 0.6f, 1.3f + random.nextFloat() * 0.1f);
    }

    public void beginGuard() {
        setAction(ActionGuard, GuardTicks);
        GuardWait = GuardCooldown;
        playSound(Scp016Registry.DuelistGuard.get(), 0.9f, 0.85f + random.nextFloat() * 0.1f);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (ComboWait > 0) {
            ComboWait--;
        }
        if (LungeWait > 0) {
            LungeWait--;
        }
        if (GuardWait > 0) {
            GuardWait--;
        }
        if (readyToGuard() && (tickCount + getId()) % 2 == 0) {
            LivingEntity Target = getTarget();
            if (incoming() || Target != null && Target.isAlive() && distanceTo(Target) <= CloseGuard && ComboWait > 0 && random.nextInt(CloseGuardChance) == 0) {
                beginGuard();
            }
        }
    }

    private boolean incoming() {
        Vec3 Centre = getBoundingBox().getCenter();
        List<Projectile> Flying = level().getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(ProjectileWatch), Found -> !isKin(Found.getOwner()) && Found.getOwner() != this);
        for (Projectile Found : Flying) {
            Vec3 Motion = Found.getDeltaMovement();
            Vec3 Toward = Centre.subtract(Found.position());
            if (Motion.length() > ProjectileSpeed && Motion.normalize().dot(Toward.normalize()) > 0.8 && inFront(Found.position())) {
                return true;
            }
        }
        return false;
    }

    public boolean inFront(Vec3 Point) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        Vec3 Toward = Point.subtract(position()).multiply(1.0, 0.0, 1.0);
        return Toward.lengthSqr() < 1.0E-4 || Facing.dot(Toward.normalize()) >= GuardArc;
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind == ActionStrike) {
            faceFoe();
            for (int Index = 0; Index < ComboHits.length; Index++) {
                if (Remaining == ComboHits[Index]) {
                    cut(ComboShares[Index], Index == ComboHits.length - 1);
                }
            }
        } else if (Kind == ActionLunge) {
            if (Remaining > LungeDashAt) {
                faceFoe();
            } else if (Remaining == LungeDashAt) {
                Vec3 Forward = Vec3.directionFromRotation(0.0f, getYRot());
                setDeltaMovement(Forward.x * LungeSpeed, LungeLift, Forward.z * LungeSpeed);
                hasImpulse = true;
                sounds(Scp016Registry.LungeLayers, 1.0f);
            } else if (Remaining >= LungeHitUntil && Foe != null && Foe.isAlive() && distanceTo(Foe) <= LungeReach + Foe.getBbWidth() * 0.5) {
                swing(InteractionHand.MAIN_HAND);
                strike(Foe, LungeShare);
                setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
                LungeWait = LungeCooldown;
                Foe = null;
            }
        } else if (Kind == ActionParry && Remaining == RiposteAt && Foe != null && Foe.isAlive() && distanceTo(Foe) <= RiposteReach + Foe.getBbWidth() * 0.5) {
            swing(InteractionHand.MAIN_HAND);
            strike(Foe, 1.0f);
            sounds(Scp016Registry.SlashLayers, 0.8f);
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind == ActionStrike) {
            ComboWait = ComboCooldown;
        } else if (Kind == ActionLunge) {
            LungeWait = LungeCooldown;
        }
        Foe = null;
    }

    private void faceFoe() {
        if (Foe == null || !Foe.isAlive()) {
            return;
        }
        getLookControl().setLookAt(Foe, 30.0f, 30.0f);
        double Dx = Foe.getX() - getX();
        double Dz = Foe.getZ() - getZ();
        float Facing = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Facing);
        setYBodyRot(Facing);
        setYHeadRot(Facing);
    }

    private void cut(float Share, boolean Last) {
        sounds(Scp016Registry.SlashLayers, Last ? 1.0f : 0.85f);
        if (Foe == null || !Foe.isAlive() || distanceTo(Foe) > ComboReach + Foe.getBbWidth() * 0.5 || !inFront(Foe.position()) || !getSensing().hasLineOfSight(Foe)) {
            return;
        }
        swing(InteractionHand.MAIN_HAND);
        strike(Foe, Share);
    }

    private boolean strike(LivingEntity Target, float Share) {
        float Amount = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * Share;
        boolean Landed = Target.hurt(damageSources().mobAttack(this), Amount);
        if (Landed) {
            Vec3 Push = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (Push.lengthSqr() > 1.0E-6) {
                Push = Push.normalize();
                Target.knockback(0.35, -Push.x, -Push.z);
            }
            Scp016Affliction.afflict(Target);
            setLastHurtMob(Target);
        }
        return Landed;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && isGuarding() && !Source.is(DamageTypeTags.BYPASSES_SHIELD) && Source.getSourcePosition() != null && inFront(Source.getSourcePosition())) {
            Entity Attacker = Source.getEntity();
            if (Attacker instanceof LivingEntity Living && Source.getDirectEntity() == Living && Living.canDisableShield()) {
                breakGuard();
                return super.hurt(Source, Amount);
            }
            parry(Source);
            return false;
        }
        return super.hurt(Source, Amount);
    }

    private void parry(DamageSource Source) {
        sounds(Scp016Registry.ParryLayers, 1.0f);
        if (level() instanceof ServerLevel World) {
            Vec3 Point = position().add(Vec3.directionFromRotation(0.0f, yBodyRot).scale(0.6)).add(0.0, getBbHeight() * 0.55, 0.0);
            World.sendParticles(Scp016Registry.Fleck.get(), Point.x, Point.y, Point.z, 10, 0.2, 0.2, 0.2, 0.08);
            World.sendParticles(ParticleTypes.CRIT, Point.x, Point.y, Point.z, 6, 0.2, 0.2, 0.2, 0.1);
        }
        Entity Direct = Source.getDirectEntity();
        if (Direct instanceof Projectile Shot) {
            Shot.setDeltaMovement(Shot.getDeltaMovement().scale(-0.25).add(0.0, 0.1, 0.0));
        }
        Foe = Source.getEntity() instanceof LivingEntity Living && Source.getDirectEntity() == Living && isQuarry(Living) ? Living : null;
        setAction(ActionParry, ParryTicks);
        GuardWait = GuardAfterParry;
    }

    private void breakGuard() {
        playSound(Scp016Registry.DuelistBreak.get(), 1.0f, 0.8f + random.nextFloat() * 0.1f);
        setAction(ActionStagger, StaggerTicks);
        GuardWait = GuardBrokenCooldown;
    }

    private void sounds(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        float Pitch = 0.92f + random.nextFloat() * 0.16f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    public boolean lungeWindow(LivingEntity Target) {
        double Distance = distanceTo(Target);
        return Distance >= LungeMin && Distance <= LungeMax && Math.abs(Target.getY() - getY()) < 1.5 && getSensing().hasLineOfSight(Target)
                && level().noCollision(this, getBoundingBox().move(Target.position().subtract(position()).multiply(1.0, 0.0, 1.0).normalize().scale(1.5)));
    }

    public double comboReach(LivingEntity Target) {
        return ComboReach + Target.getBbWidth() * 0.5;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp016Registry.DuelistAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp016Registry.DuelistHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp016Registry.DuelistDeath.get();
    }

    @Override
    protected Scp016Gait.Profile gaitProfile() {
        return DuelistGait;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (action() != ActionEmerge) {
            setAction(ActionNone, 0);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp016GaitController<>(this, "body", 3, Event -> {
            if (isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionStrike) {
                return Event.setAndContinue(ComboPlay);
            }
            if (Current == ActionLunge) {
                return Event.setAndContinue(LungePlay);
            }
            if (Current == ActionGuard) {
                return Event.setAndContinue(GuardLoop);
            }
            if (Current == ActionParry) {
                return Event.setAndContinue(ParryPlay);
            }
            if (Current == ActionStagger) {
                return Event.setAndContinue(ShudderLoop);
            }
            if (isStriding()) {
                return Event.setAndContinue(WalkLoop);
            }
            return Event.setAndContinue(isCollapsing() ? ShudderLoop : IdleLoop);
        }));
    }
}
