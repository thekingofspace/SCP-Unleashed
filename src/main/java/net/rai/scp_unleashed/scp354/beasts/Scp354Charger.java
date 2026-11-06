package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Scp354Charger extends Scp354Beast {
    public static final float Width = 2.0f;
    public static final float Height = 2.0f;
    public static final float EyeHeight = 1.3f;
    public static final float RenderScale = 2.0f;
    public static final double SwimSpeed = 0.12;
    public static final double Draft = 1.3;
    public static final double HaulReach = 1.8;
    public static final int HaulTicks = 16;
    public static final double Health = 230.0;
    public static final double Armour = 12.0;
    public static final double Toughness = 4.0;
    public static final double KnockbackResistance = 0.9;
    public static final double Speed = 0.29;
    public static final double ChaseModifier = 1.0;
    public static final double WanderModifier = 0.5;
    public static final int PawTicks = 24;
    public static final int ChargeTicks = 44;
    public static final double ChargeSpeed = 0.62;
    public static final float ChargeDamage = 18.0f;
    public static final double ChargeKnock = 2.4;
    public static final double ChargeLift = 0.45;
    public static final int ChargeCooldown = 90;
    public static final double ChargeMin = 5.0;
    public static final double ChargeMax = 26.0;
    public static final int CrashTicks = 16;
    public static final int StunTicks = 50;
    public static final float StunnedDamageScale = 1.5f;
    public static final int TossTicks = 14;
    public static final int TossHitTick = 6;
    public static final int TossCooldown = 30;
    public static final float TossDamage = 12.0f;
    public static final double TossReach = 1.8;
    public static final double RunEnter = 0.17;
    public static final double RunExit = 0.13;
    public static final double IdleSeconds = 3.5;
    public static final byte ActionPaw = 10;
    public static final byte ActionCharge = 11;
    public static final byte ActionCrash = 12;
    public static final byte ActionStunned = 13;
    public static final byte ActionToss = 14;
    private static final Scp354Stride[] Strides = {Scp354Gaits.ChargerWalk, Scp354Gaits.ChargerRun, Scp354Gaits.ChargerCharge};
    private final Set<UUID> Trampled = new HashSet<>();
    @Nullable
    private LivingEntity StrikeTarget;
    private int ChargeReady = 40;
    private int TossReady;
    private float Heading;
    private Vec3 ChargeStart = Vec3.ZERO;

    public Scp354Charger(EntityType<? extends Scp354Charger> Type, Level World) {
        super(Type, World);
        xpReward = 55;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, TossDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance)
                .add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.STEP_HEIGHT, 1.25);
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
        return Scp354BeastRegistry.ChargerVoice;
    }

    @Nullable
    @Override
    protected String clipFor(byte Action) {
        return switch (Action) {
            case ActionPaw -> "paw";
            case ActionCrash -> "crash";
            case ActionToss -> "toss";
            default -> null;
        };
    }

    @Nullable
    @Override
    protected String loopClip() {
        return switch (action()) {
            case ActionCharge -> "charge";
            case ActionStunned -> "stunned";
            default -> null;
        };
    }

    @Override
    public boolean locksMovement() {
        return action() != ActionNone;
    }

    @Override
    public boolean locksYaw() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionCharge || Current == ActionCrash || Current == ActionStunned;
    }

    public boolean isStunned() {
        return action() == ActionStunned || action() == ActionCrash;
    }

    public boolean canCharge() {
        return ChargeReady <= 0 && canAct() && onGround();
    }

    @Override
    public void engage(LivingEntity Target) {
        if (!canAct() || !hasLineOfSight(Target)) {
            return;
        }
        double Reach = reachTo(Target);
        if (TossReady <= 0 && Reach <= TossReach) {
            beginToss(Target);
        } else if (canCharge() && Reach >= ChargeMin && Reach <= ChargeMax && Math.abs(Target.getY() - getY()) < 3.0
                && !Scp354Swim.crossesPool(level(), position(), Target.position())) {
            beginPaw(Target);
        }
    }

    public void beginPaw(LivingEntity Target) {
        StrikeTarget = Target;
        ChargeReady = ChargeCooldown;
        faceToward(Target, 180.0f);
        setAction(ActionPaw, PawTicks);
        playLayers(Scp354BeastRegistry.ChargerPaw, 1.2f, 1.0f);
    }

    public void beginCharge(float Yaw, @Nullable LivingEntity Target) {
        Yaw = Mth.wrapDegrees(Yaw);
        StrikeTarget = Target;
        Heading = Yaw;
        setYRot(Yaw);
        yBodyRot = Yaw;
        Trampled.clear();
        ChargeStart = position();
        setAction(ActionCharge, ChargeTicks);
        lockYawTo(Yaw);
        playLayers(Scp354BeastRegistry.ChargerBellow, 2.0f, 1.0f);
    }

    public void beginToss(LivingEntity Target) {
        StrikeTarget = Target;
        TossReady = TossTicks + TossCooldown;
        setAction(ActionToss, TossTicks);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            ChargeReady = Math.max(0, ChargeReady - 1);
            TossReady = Math.max(0, TossReady - 1);
        }
    }

    @Override
    protected void finishAction(byte Kind) {
        super.finishAction(Kind);
        if (Kind == ActionPaw && isAlive()) {
            LivingEntity Target = StrikeTarget;
            float Yaw = Target != null && Target.isAlive() ? (float) (Math.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * 180.0 / Math.PI) - 90.0f : getYRot();
            beginCharge(Yaw, Target);
        } else if (Kind == ActionCrash && isAlive()) {
            setAction(ActionStunned, StunTicks);
            playLayers(Scp354BeastRegistry.ChargerDaze, 1.2f, 1.0f);
        }
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        LivingEntity Target = StrikeTarget;
        if (Kind == ActionPaw) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Target != null && Target.isAlive()) {
                faceToward(Target, 15.0f);
                getLookControl().setLookAt(Target, 30.0f, 30.0f);
            }
            if (Elapsed == 6 || Elapsed == 14) {
                playLayers(Scp354BeastRegistry.ChargerPaw, 1.0f, 1.1f);
                if (level() instanceof ServerLevel World) {
                    crackGround(World, position().add(Vec3.directionFromRotation(0.0f, getYRot()).scale(0.8)), 8, 0.3);
                }
            }
        } else if (Kind == ActionCharge) {
            chargeTick(Elapsed);
        } else if (Kind == ActionCrash || Kind == ActionStunned) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        } else if (Kind == ActionToss) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Target != null && Target.isAlive()) {
                faceToward(Target, 25.0f);
                if (Elapsed == TossHitTick && reachTo(Target) <= TossReach + 0.6 && strike(Target, TossDamage, 0.6, 0.6)) {
                    playLayers(Scp354BeastRegistry.ChargerToss, 1.3f, 1.0f);
                }
            }
        }
    }

    private void chargeTick(int Elapsed) {
        Vec3 Forward = Vec3.directionFromRotation(0.0f, Heading);
        Vec3 Velocity = getDeltaMovement();
        if (Scp354Swim.brink(this, Forward)) {
            endAction();
            setDeltaMovement(Velocity.x * 0.2, Velocity.y, Velocity.z * 0.2);
            return;
        }
        setDeltaMovement(Forward.x * ChargeSpeed, Velocity.y, Forward.z * ChargeSpeed);
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Elapsed % 3 == 0) {
            crackGround(World, position(), 4, 0.5);
        }
        Vec3 Front = position().add(Forward.scale(getBbWidth() * 0.6));
        AABB Ram = new AABB(Front, Front).inflate(getBbWidth() * 0.6, 0.0, getBbWidth() * 0.6).expandTowards(0.0, getBbHeight(), 0.0);
        for (LivingEntity Victim : victims(Ram, Found -> !Trampled.contains(Found.getUUID()))) {
            Trampled.add(Victim.getUUID());
            if (Victim.hurt(damageSources().mobAttack(this), ChargeDamage)) {
                double Resist = 1.0 - Math.min(1.0, Math.max(0.0, Victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
                Victim.setDeltaMovement(Victim.getDeltaMovement().add(Forward.scale(ChargeKnock * Math.max(0.35, Resist))).add(0.0, ChargeLift, 0.0));
                Victim.hurtMarked = true;
                playLayers(Scp354BeastRegistry.ChargerImpact, 1.8f, 0.9f);
                setLastHurtMob(Victim);
            }
            if (Victim == StrikeTarget) {
                endAction();
                setDeltaMovement(getDeltaMovement().scale(0.3));
                return;
            }
        }
        if (Elapsed > 2 && horizontalCollision && position().distanceTo(ChargeStart) > 1.0) {
            crash(World);
        }
    }

    public void crash(ServerLevel World) {
        setDeltaMovement(Vec3.directionFromRotation(0.0f, Heading).scale(-0.35).add(0.0, 0.2, 0.0));
        playLayers(Scp354BeastRegistry.ChargerCrash, 2.0f, 0.9f);
        Vec3 Front = position().add(Vec3.directionFromRotation(0.0f, Heading).scale(getBbWidth() * 0.6 + 0.3)).add(0.0, getBbHeight() * 0.5, 0.0);
        crackGround(World, Front, 30, 0.4);
        setAction(ActionCrash, CrashTicks);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return super.hurt(Source, isStunned() ? Amount * StunnedDamageScale : Amount);
    }
}
