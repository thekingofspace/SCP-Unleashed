package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp354.Scp354Swim;
import org.jetbrains.annotations.Nullable;

public class Scp354Leaper extends Scp354Beast {
    public static final float Width = 2.4f;
    public static final float Height = 1.7f;
    public static final float EyeHeight = 1.45f;
    public static final float RenderScale = 2.0f;
    public static final double SwimSpeed = 0.16;
    public static final double Draft = 0.95;
    public static final double HaulReach = 2.4;
    public static final int HaulTicks = 14;
    public static final double Health = 190.0;
    public static final double Armour = 6.0;
    public static final double Toughness = 2.0;
    public static final double KnockbackResistance = 0.7;
    public static final double Speed = 0.27;
    public static final double ChaseModifier = 1.0;
    public static final double WanderModifier = 0.5;
    public static final float SlamDamage = 12.0f;
    public static final double SlamRadius = 3.5;
    public static final double SlamKnock = 1.0;
    public static final double SlamLift = 0.55;
    public static final int SlamTicks = 18;
    public static final int SlamHitTick = 1;
    public static final int SlamCooldown = 40;
    public static final double LeapMin = 5.0;
    public static final double LeapMax = 16.0;
    public static final int LeapLaunchTick = 9;
    public static final int LeapMaxTicks = 70;
    public static final int LeapCooldown = 100;
    public static final int TongueTicks = 16;
    public static final int TongueHitTick = 5;
    public static final double TongueMin = 3.0;
    public static final double TongueReach = 8.5;
    public static final float TongueDamage = 6.0f;
    public static final int TongueCooldown = 80;
    public static final double TonguePull = 0.155;
    public static final double TongueRestLength = 0.5;
    public static final double RunEnter = 0.16;
    public static final double RunExit = 0.12;
    public static final double IdleSeconds = 3.0;
    public static final double AirFriction = 0.91;
    public static final double VerticalDrag = 0.98;
    public static final double Gravity = 0.08;
    public static final byte ActionLeap = 10;
    public static final byte ActionSlam = 11;
    public static final byte ActionTongue = 12;
    private static final EntityDataAccessor<Float> TongueReachData = SynchedEntityData.defineId(Scp354Leaper.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TonguePitchData = SynchedEntityData.defineId(Scp354Leaper.class, EntityDataSerializers.FLOAT);
    private static final Scp354Stride[] Strides = {Scp354Gaits.LeaperWalk, Scp354Gaits.LeaperRun};
    @Nullable
    private LivingEntity StrikeTarget;
    private int LeapReady = 40;
    private int TongueReady = 30;
    private int SlamReady;
    private boolean Launched;

    public Scp354Leaper(EntityType<? extends Scp354Leaper> Type, Level World) {
        super(Type, World);
        xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, SlamDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance)
                .add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.STEP_HEIGHT, 1.25).add(Attributes.SAFE_FALL_DISTANCE, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(TongueReachData, 0.0f);
        Builder.define(TonguePitchData, 0.0f);
    }

    @Override
    protected void registerGoals() {
        registerBaseGoals();
        goalSelector.addGoal(3, new Scp354BeastGoals.Chase(this, ChaseModifier));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected Scp354Stride[] strides() {
        return Strides;
    }

    @Override
    protected double runEnter() {
        return RunEnter;
    }

    @Override
    protected double runExit() {
        return RunExit;
    }

    @Override
    protected double idleSeconds() {
        return IdleSeconds;
    }

    @Override
    public float renderScale() {
        return RenderScale;
    }

    @Override
    public double swimSpeed() {
        return SwimSpeed;
    }

    @Override
    public double draft() {
        return Draft;
    }

    @Override
    public double haulReach() {
        return HaulReach;
    }

    @Override
    public int haulTicks() {
        return HaulTicks;
    }

    @Override
    public Scp354Voice voice() {
        return Scp354BeastRegistry.LeaperVoice;
    }

    @Nullable
    @Override
    protected String clipFor(byte Action) {
        return switch (Action) {
            case ActionLeap -> "leap";
            case ActionSlam -> "slam";
            case ActionTongue -> "tongue";
            default -> null;
        };
    }

    @Override
    public boolean locksMovement() {
        byte Current = action();
        return Current != ActionNone;
    }

    @Override
    public boolean locksYaw() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionLeap && Launched;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    public float tongueReach() {
        return entityData.get(TongueReachData);
    }

    public float tonguePitch() {
        return entityData.get(TonguePitchData);
    }

    public static float tongueExtension(double Ticks) {
        if (Ticks < 2.0) {
            return 0.0f;
        }
        if (Ticks < 5.0) {
            return (float) smooth((Ticks - 2.0) / 3.0);
        }
        if (Ticks < 7.0) {
            return 1.0f;
        }
        if (Ticks < 12.0) {
            return (float) (1.0 - smooth((Ticks - 7.0) / 5.0));
        }
        return 0.0f;
    }

    private static double smooth(double Value) {
        double Clamped = Mth.clamp(Value, 0.0, 1.0);
        return Clamped * Clamped * (3.0 - 2.0 * Clamped);
    }

    public boolean canLeap() {
        return LeapReady <= 0 && canAct() && onGround();
    }

    public boolean canTongue() {
        return TongueReady <= 0 && canAct();
    }

    @Override
    public void engage(LivingEntity Target) {
        if (!canAct() || !hasLineOfSight(Target)) {
            return;
        }
        double Reach = reachTo(Target);
        if (SlamReady <= 0 && Reach <= 1.0 && onGround()) {
            beginSlam();
        } else if (canTongue() && Reach >= TongueMin && distanceTo(Target) <= TongueReach) {
            beginTongue(Target);
        } else if (canLeap() && Reach >= LeapMin && Reach <= LeapMax && Math.abs(Target.getY() - getY()) < 6.0
                && !Scp354Swim.crossesPool(level(), position(), Target.position())) {
            beginLeap(Target);
        }
    }

    public void beginLeap(LivingEntity Target) {
        StrikeTarget = Target;
        LeapReady = LeapCooldown;
        Launched = false;
        faceToward(Target, 180.0f);
        setAction(ActionLeap, LeapMaxTicks);
    }

    public void beginSlam() {
        SlamReady = SlamCooldown;
        setAction(ActionSlam, SlamTicks);
    }

    public void beginTongue(LivingEntity Target) {
        StrikeTarget = Target;
        TongueReady = TongueTicks + TongueCooldown;
        faceToward(Target, 180.0f);
        Vec3 Mouth = mouth();
        Vec3 Aim = Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0).subtract(Mouth);
        entityData.set(TongueReachData, (float) Aim.length());
        entityData.set(TonguePitchData, (float) (Mth.atan2(Aim.y, Math.sqrt(Aim.x * Aim.x + Aim.z * Aim.z)) * Mth.RAD_TO_DEG));
        setAction(ActionTongue, TongueTicks);
        playLayers(Scp354BeastRegistry.LeaperTongue.subList(0, 1), 1.3f, 1.0f);
    }

    public Vec3 mouth() {
        return position().add(Vec3.directionFromRotation(0.0f, getYRot()).scale(getBbWidth() * 0.55)).add(0.0, 0.8, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            LeapReady = Math.max(0, LeapReady - 1);
            TongueReady = Math.max(0, TongueReady - 1);
            SlamReady = Math.max(0, SlamReady - 1);
        }
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        LivingEntity Target = StrikeTarget;
        if (Kind == ActionLeap) {
            if (Elapsed < LeapLaunchTick) {
                setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
                if (Target != null && Target.isAlive()) {
                    faceToward(Target, 20.0f);
                }
            } else if (Elapsed == LeapLaunchTick) {
                launch(Target);
            } else if (Launched && Elapsed > LeapLaunchTick + 2 && (onGround() || isInWater())) {
                beginSlam();
            } else if (Elapsed >= LeapMaxTicks - 1) {
                beginSlam();
            }
            return;
        }
        if (Kind == ActionSlam) {
            setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
            if (Elapsed == SlamHitTick && level() instanceof ServerLevel World) {
                slam(World);
            }
            return;
        }
        if (Kind == ActionTongue) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Elapsed == TongueHitTick) {
                lash(Target);
            }
        }
    }

    private void launch(@Nullable LivingEntity Target) {
        Vec3 Goal = Target != null && Target.isAlive() ? Target.position() : position().add(Vec3.directionFromRotation(0.0f, getYRot()).scale(8.0));
        Vec3 Delta = Goal.subtract(position());
        double Flat = Math.sqrt(Delta.x * Delta.x + Delta.z * Delta.z);
        int Ticks = Mth.clamp((int) Math.round(Flat / 0.6) + 6, 12, 28);
        double Horizontal = 0.0;
        double Factor = 1.0;
        double Rise = 0.0;
        double Drop = 0.0;
        double Fall = 0.0;
        double Drag = 1.0;
        for (int Tick = 0; Tick < Ticks; Tick++) {
            Horizontal += Factor;
            Factor *= AirFriction;
            Rise += Drag;
            Drop += Fall;
            Fall = (Fall - Gravity) * VerticalDrag;
            Drag *= VerticalDrag;
        }
        double Lead = Flat < 1.0e-3 ? 0.0 : 1.0 / Horizontal;
        double Up = (Delta.y + 0.4 - Drop) / Rise;
        setDeltaMovement(Delta.x * Lead, Up, Delta.z * Lead);
        hasImpulse = true;
        Launched = true;
        setOnGround(false);
        playLayers(Scp354BeastRegistry.LeaperLeap, 1.6f, 1.0f);
        if (level() instanceof ServerLevel World) {
            crackGround(World, position(), 12, 0.6);
        }
    }

    public void slam(ServerLevel World) {
        playLayers(Scp354BeastRegistry.LeaperSlam, 1.8f, 0.9f);
        crackGround(World, position(), 30, 1.4);
        World.sendParticles(Scp354BeastRegistry.Splash.get(), getX(), getY() + 0.05, getZ(), 6, 1.2, 0.0, 1.2, 0.0);
        AABB Area = getBoundingBox().inflate(SlamRadius, 1.5, SlamRadius);
        for (LivingEntity Victim : victims(Area, Found -> true)) {
            double Distance = Math.sqrt(Victim.distanceToSqr(getX(), Victim.getY(), getZ())) - getBbWidth() * 0.5;
            if (Distance > SlamRadius) {
                continue;
            }
            float Share = (float) Mth.clamp(1.0 - 0.5 * Distance / SlamRadius, 0.5, 1.0);
            if (Victim.hurt(damageSources().mobAttack(this), SlamDamage * Share)) {
                Vec3 Away = new Vec3(Victim.getX() - getX(), 0.0, Victim.getZ() - getZ());
                Away = Away.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0.0f, getYRot()) : Away.normalize();
                double Resist = 1.0 - Mth.clamp(Victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                Victim.setDeltaMovement(Victim.getDeltaMovement().add(Away.scale(SlamKnock * Share * Resist)).add(0.0, SlamLift * Share * Math.max(0.3, Resist), 0.0));
                Victim.hurtMarked = true;
            }
        }
    }

    private void lash(@Nullable LivingEntity Target) {
        playLayers(Scp354BeastRegistry.LeaperTongue.subList(1, 2), 1.3f, 1.0f);
        if (Target == null || !Target.isAlive() || !hasLineOfSight(Target) || distanceTo(Target) > Math.max(TongueReach, tongueReach() + 1.5)) {
            return;
        }
        Target.hurt(damageSources().mobAttack(this), TongueDamage);
        Vec3 Toward = mouth().subtract(Target.position());
        double Length = Toward.length();
        if (Length < 1.0e-3) {
            return;
        }
        double Pull = Mth.clamp((Length - getBbWidth() * 0.5 - 0.8) * TonguePull, 0.2, 1.4);
        Vec3 Yank = Toward.scale(Pull / Length);
        Target.setDeltaMovement(Yank.x, Math.max(0.25, Yank.y + 0.25), Yank.z);
        Target.hurtMarked = true;
        Target.hasImpulse = true;
        setLastHurtMob(Target);
    }
}
