package net.rai.scp_unleashed.scp513;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp513Stalker extends Entity implements GeoEntity {
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation ReachHold = RawAnimation.begin().thenPlayAndHold("reach");
    public static final RawAnimation StrangleLoop = RawAnimation.begin().thenLoop("strangle");
    public static final Vector3f MiasmaColour = new Vector3f(0.04f, 0.035f, 0.045f);
    public static final Vector3f AshColour = new Vector3f(0.13f, 0.12f, 0.13f);
    public static final float MiasmaSize = 1.7f;
    public static final float AshSize = 0.9f;
    public static final int FadeTicks = 10;
    public static final float AlphaSteps = 8.0f;

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private Scp513Pose Shown = Scp513Pose.Idle;
    private int Age;
    private int Life = -1;
    private float BodyYaw;
    private float BodyYawO;

    public Scp513Stalker(EntityType<? extends Scp513Stalker> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
    }

    public void setLife(int Ticks) {
        Life = Ticks;
    }

    public void setShown(Scp513Pose Wanted) {
        Shown = Wanted;
    }

    public Scp513Pose shown() {
        return Shown;
    }

    public int age() {
        return Age;
    }

    public void place(Vec3 Where, float Yaw, boolean Snap) {
        BodyYawO = Snap ? Yaw : BodyYaw;
        BodyYaw = Yaw;
        if (Snap) {
            setPos(Where.x, Where.y, Where.z);
            xo = Where.x;
            yo = Where.y;
            zo = Where.z;
            xOld = Where.x;
            yOld = Where.y;
            zOld = Where.z;
        } else {
            setPos(Where.x, Where.y, Where.z);
        }
        setYRot(Yaw);
        yRotO = BodyYawO;
    }

    public float bodyYaw(float Partial) {
        return Mth.rotLerp(Partial, BodyYawO, BodyYaw);
    }

    public float fade(float Partial) {
        float In = Mth.clamp((Age + Partial) / FadeTicks, 0.0f, 1.0f);
        float Out = Life < 0 ? 1.0f : Mth.clamp((Life - Age - Partial) / FadeTicks, 0.0f, 1.0f);
        return Math.round(Math.min(In, Out) * AlphaSteps) / AlphaSteps;
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            discard();
            return;
        }
        xo = getX();
        yo = getY();
        zo = getZ();
        BodyYawO = BodyYaw;
        Age++;
        if (Life >= 0 && Age >= Life) {
            discard();
            return;
        }
        exhale(random);
    }

    private void exhale(RandomSource Random) {
        double Height = getBbHeight();
        int Puffs = Shown == Scp513Pose.Strangle ? 3 : 2;
        for (int Index = 0; Index < Puffs; Index++) {
            double X = getX() + (Random.nextDouble() - 0.5) * getBbWidth() * 1.4;
            double Y = getY() + Random.nextDouble() * Height;
            double Z = getZ() + (Random.nextDouble() - 0.5) * getBbWidth() * 1.4;
            level().addParticle(new DustParticleOptions(MiasmaColour, MiasmaSize), X, Y, Z, 0.0, 0.015, 0.0);
        }
        if (Random.nextInt(4) == 0) {
            level().addParticle(new DustParticleOptions(AshColour, AshSize), getX() + (Random.nextDouble() - 0.5) * 0.6, getY() + Height * (0.55 + Random.nextDouble() * 0.4),
                    getZ() + (Random.nextDouble() - 0.5) * 0.6, 0.0, 0.03, 0.0);
        }
        if (Random.nextInt(9) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX() + (Random.nextDouble() - 0.5) * 0.5, getY() + Random.nextDouble() * 0.4, getZ() + (Random.nextDouble() - 0.5) * 0.5,
                    0.0, 0.01, 0.0);
        }
    }

    public Vec3 hands() {
        return position().add(0.0, getBbHeight() * 0.55, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity Attacker) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return true;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 64.0 * 64.0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 4, State -> switch (Shown) {
            case Walk -> State.setAndContinue(WalkLoop);
            case Reach -> State.setAndContinue(ReachHold);
            case Strangle -> State.setAndContinue(StrangleLoop);
            default -> State.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
