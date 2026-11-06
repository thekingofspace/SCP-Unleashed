package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp354.Scp354Basin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp354Brute extends Scp354Beast {
    public static final float Width = 2.6f;
    public static final float Height = 3.4f;
    public static final float EyeHeight = 2.5f;
    public static final float RenderScale = 2.0f;
    public static final double SwimSpeed = 0.11;
    public static final double Draft = 1.5;
    public static final double HaulReach = 2.2;
    public static final int HaulTicks = 18;
    public static final double Health = 280.0;
    public static final double Armour = 10.0;
    public static final double Toughness = 4.0;
    public static final double KnockbackResistance = 0.85;
    public static final double Speed = 0.3;
    public static final double ChaseModifier = 1.0;
    public static final double WanderModifier = 0.55;
    public static final double FollowRange = 40.0;
    public static final float SwingDamage = 16.0f;
    public static final double SwingReach = 2.2;
    public static final double SwingKnock = 1.4;
    public static final double SwingLift = 0.25;
    public static final int SwingTicks = 18;
    public static final int SwingHitTick = 8;
    public static final int SwingCooldown = 24;
    public static final float PoundDamage = 14.0f;
    public static final double PoundRadius = 5.0;
    public static final double PoundTrigger = 3.5;
    public static final double PoundKnock = 1.2;
    public static final double PoundLift = 0.7;
    public static final int PoundTicks = 30;
    public static final int PoundHitTick = 16;
    public static final int PoundCooldown = 140;
    public static final int HurlTicks = 30;
    public static final int HurlRipTick = 6;
    public static final int HurlReleaseTick = 18;
    public static final int HurlCooldown = 160;
    public static final double HurlMin = 6.0;
    public static final double HurlMax = 22.0;
    public static final double RunEnter = 0.17;
    public static final double RunExit = 0.13;
    public static final double IdleSeconds = 4.0;
    public static final byte ActionSwing = 10;
    public static final byte ActionPound = 11;
    public static final byte ActionHurl = 12;
    public static final int ClodShownFrom = 6;
    public static final int ClodShownUntil = 18;
    private static final Scp354Stride[] Strides = {Scp354Gaits.BruteWalk, Scp354Gaits.BruteRun};
    private static final List<String> Hidden = List.of("clod_right");
    @Nullable
    private LivingEntity StrikeTarget;
    private int SwingReady;
    private int PoundReady = 40;
    private int HurlReady = 60;
    private BlockState Clod = Blocks.COARSE_DIRT.defaultBlockState();

    public Scp354Brute(EntityType<? extends Scp354Brute> Type, Level World) {
        super(Type, World);
        xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, SwingDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    protected void registerGoals() {
        registerBaseGoals();
        goalSelector.addGoal(3, new Scp354BeastGoals.Chase(this, ChaseModifier));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
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
        return Scp354BeastRegistry.BruteVoice;
    }

    @Override
    public double acquireRange() {
        return 32.0;
    }

    @Override
    public List<String> hiddenBones() {
        return Hidden;
    }

    @Override
    public boolean showsBone(String Bone) {
        if (action() != ActionHurl) {
            return false;
        }
        double Elapsed = actionElapsed();
        return Elapsed >= ClodShownFrom && Elapsed < ClodShownUntil;
    }

    @Nullable
    @Override
    protected String clipFor(byte Action) {
        return switch (Action) {
            case ActionSwing -> "swing";
            case ActionPound -> "pound";
            case ActionHurl -> "hurl";
            default -> null;
        };
    }

    @Override
    public boolean locksMovement() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionPound || Current == ActionHurl || Current == ActionSwing;
    }

    @Override
    public boolean locksYaw() {
        return action() == ActionEmerge || action() == ActionPound;
    }

    public boolean canSwing() {
        return SwingReady <= 0 && canAct();
    }

    public boolean canPound() {
        return PoundReady <= 0 && canAct() && onGround();
    }

    public boolean canHurl() {
        return HurlReady <= 0 && canAct() && onGround();
    }

    @Override
    public void engage(LivingEntity Target) {
        if (!canAct() || !hasLineOfSight(Target)) {
            return;
        }
        double Reach = reachTo(Target);
        if (canPound() && Reach <= PoundTrigger && Math.abs(Target.getY() - getY()) < 3.0) {
            int Crowd = victims(getBoundingBox().inflate(PoundRadius, 2.0, PoundRadius), Found -> true).size();
            if (Crowd >= 2 || random.nextInt(3) == 0) {
                beginPound(Target);
                return;
            }
        }
        if (canSwing() && Reach <= SwingReach) {
            beginSwing(Target);
            return;
        }
        if (canHurl() && Reach >= HurlMin && Reach <= HurlMax) {
            beginHurl(Target);
        }
    }

    public void beginSwing(LivingEntity Target) {
        StrikeTarget = Target;
        SwingReady = SwingTicks + SwingCooldown;
        setAction(ActionSwing, SwingTicks);
        playLayers(Scp354BeastRegistry.BruteSwing.subList(1, 2), 1.2f, 0.8f);
    }

    public void beginPound(@Nullable LivingEntity Target) {
        StrikeTarget = Target;
        PoundReady = PoundTicks + PoundCooldown;
        if (Target != null) {
            faceToward(Target, 180.0f);
        }
        setAction(ActionPound, PoundTicks);
        playLayers(voice().Idle(), 1.8f, 0.8f);
    }

    public void beginHurl(LivingEntity Target) {
        StrikeTarget = Target;
        HurlReady = HurlTicks + HurlCooldown;
        BlockState Below = level().getBlockState(BlockPos.containing(getX(), getY() - 0.3, getZ()));
        Clod = Below.isAir() || Below.getRenderShape() != RenderShape.MODEL || Below.hasBlockEntity() || Below.getDestroySpeed(level(), blockPosition()) < 0.0f && !Scp354Basin.isOwn(Below)
                ? Blocks.COARSE_DIRT.defaultBlockState() : Below;
        setAction(ActionHurl, HurlTicks);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            SwingReady = Math.max(0, SwingReady - 1);
            PoundReady = Math.max(0, PoundReady - 1);
            HurlReady = Math.max(0, HurlReady - 1);
        }
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        LivingEntity Target = StrikeTarget;
        if (Target != null && Target.isAlive() && Kind != ActionPound) {
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
            faceToward(Target, Kind == ActionHurl && Elapsed < HurlReleaseTick ? 12.0f : 20.0f);
        }
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Kind == ActionSwing && Elapsed == SwingHitTick) {
            swingHit(Target);
        } else if (Kind == ActionPound && Elapsed == PoundHitTick) {
            pound(World);
        } else if (Kind == ActionHurl && Elapsed == HurlRipTick) {
            playLayers(Scp354BeastRegistry.BruteRip, 1.4f, 0.8f);
            crackGround(World, handPoint(), 18, 0.4);
        } else if (Kind == ActionHurl && Elapsed == HurlReleaseTick) {
            hurl(World, Target);
        }
    }

    private Vec3 handPoint() {
        Vec3 Forward = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Side = new Vec3(-Forward.z, 0.0, Forward.x);
        return position().add(Forward.scale(1.5)).add(Side.scale(1.2));
    }

    private void swingHit(@Nullable LivingEntity Target) {
        playLayers(Scp354BeastRegistry.BruteSwing.subList(0, 1), 1.3f, 0.85f);
        Vec3 Forward = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Centre = position().add(Forward.scale(getBbWidth() * 0.5 + SwingReach * 0.5));
        AABB Arc = new AABB(Centre, Centre).inflate(SwingReach * 0.5 + 0.9, 1.6, SwingReach * 0.5 + 0.9).move(0.0, 1.0, 0.0);
        boolean Hit = false;
        for (LivingEntity Victim : victims(Arc, Found -> hasLineOfSight(Found))) {
            Hit |= strike(Victim, SwingDamage, SwingKnock, SwingLift);
        }
        if (!Hit && Target != null && Target.isAlive() && reachTo(Target) <= SwingReach + 0.5) {
            strike(Target, SwingDamage, SwingKnock, SwingLift);
        }
    }

    public void pound(ServerLevel World) {
        Vec3 Centre = position().add(Vec3.directionFromRotation(0.0f, getYRot()).scale(1.4));
        playLayers(Scp354BeastRegistry.BrutePound, 2.0f, 0.75f);
        for (int Ring = 1; Ring <= 3; Ring++) {
            double Radius = PoundRadius * Ring / 3.0;
            int Count = 10 * Ring;
            for (int Index = 0; Index < Count; Index++) {
                double Angle = Math.PI * 2.0 * Index / Count;
                crackGround(World, Centre.add(Math.cos(Angle) * Radius, 0.0, Math.sin(Angle) * Radius), 3, 0.15);
            }
        }
        World.sendParticles(Scp354BeastRegistry.Splash.get(), Centre.x, getY() + 0.05, Centre.z, 4, 1.2, 0.0, 1.2, 0.0);
        AABB Area = new AABB(Centre, Centre).inflate(PoundRadius, 2.5, PoundRadius);
        for (LivingEntity Victim : victims(Area, Found -> Found.onGround() || Found.getY() - getY() < 1.5)) {
            double Distance = Math.sqrt(Victim.distanceToSqr(Centre.x, Victim.getY(), Centre.z));
            if (Distance > PoundRadius) {
                continue;
            }
            float Share = (float) (1.0 - 0.5 * Distance / PoundRadius);
            if (Victim.hurt(damageSources().mobAttack(this), PoundDamage * Share)) {
                Vec3 Away = new Vec3(Victim.getX() - Centre.x, 0.0, Victim.getZ() - Centre.z);
                Away = Away.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0.0f, getYRot()) : Away.normalize();
                double Resist = 1.0 - Mth.clamp(Victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                Victim.setDeltaMovement(Victim.getDeltaMovement().add(Away.scale(PoundKnock * Share * Resist)).add(0.0, PoundLift * Share * Math.max(0.3, Resist), 0.0));
                Victim.hurtMarked = true;
            }
        }
    }

    private void hurl(ServerLevel World, @Nullable LivingEntity Target) {
        Vec3 From = position().add(0.0, getBbHeight() * 1.05, 0.0).add(Vec3.directionFromRotation(0.0f, getYRot()).scale(0.8));
        Scp354HurledEarth Thrown = new Scp354HurledEarth(World, this, Clod);
        Thrown.setPos(From.x, From.y, From.z);
        Vec3 Aim = Target != null && Target.isAlive() ? Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0)
                : From.add(Vec3.directionFromRotation(0.0f, getYRot()).scale(12.0));
        Thrown.launchAt(Aim);
        World.addFreshEntity(Thrown);
        playLayers(Scp354BeastRegistry.BruteThrow, 1.4f, 0.8f);
    }
}
