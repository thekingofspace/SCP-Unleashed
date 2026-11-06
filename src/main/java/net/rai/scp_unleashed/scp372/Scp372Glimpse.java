package net.rai.scp_unleashed.scp372;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Supplier;

public class Scp372Glimpse extends Entity implements GeoEntity {
    public static final float Width = 0.36f;
    public static final float Height = 0.24f;
    public static final RawAnimation ClingLoop = RawAnimation.begin().thenLoop("glimpse_cling");
    public static final RawAnimation LeapPlay = RawAnimation.begin().thenPlayAndHold("glimpse_leap");
    public static final int LifeMin = 4;
    public static final int LifeMax = 10;
    public static final int LeapTicks = 5;
    public static final double VanishCone = 35.0;
    public static final double LeapSpeed = 0.33;
    public static final double LeapAcceleration = 0.18;
    public static final double LeapLift = 0.06;
    public static final double CentreLift = 0.21;
    public static final double CullReach = 1.56;
    public static final int Trail = 3;
    public static final float SkitterVolume = 0.35f;
    public static Supplier<Player> ClientViewer = () -> null;

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Vec3[] Behind = new Vec3[Trail];
    private Vec3 Normal = new Vec3(0.0, 1.0, 0.0);
    private Vec3 Heading = new Vec3(0.0, 0.0, 1.0);
    private int Age;
    private int Lifetime = LifeMax;
    private int Leap = -1;

    public Scp372Glimpse(EntityType<? extends Scp372Glimpse> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
    }

    public void setup(Vec3 Surface, Vec3 Head, int Life) {
        Normal = Surface.normalize();
        Heading = Head.subtract(Normal.scale(Head.dot(Normal))).normalize();
        Lifetime = Mth.clamp(Life, LifeMin, LifeMax);
        Age = 0;
        Leap = -1;
        for (int Index = 0; Index < Trail; Index++) {
            Behind[Index] = position();
        }
    }

    public Vec3 normal() {
        return Normal;
    }

    public Vec3 heading() {
        return Heading;
    }

    public int age() {
        return Age;
    }

    public int lifetime() {
        return Lifetime;
    }

    public boolean isLeaping() {
        return Leap >= 0;
    }

    public float leapProgress(float Partial) {
        return Leap < 0 ? 0.0f : Mth.clamp((Leap + Partial) / LeapTicks, 0.0f, 1.0f);
    }

    public Vec3 trail(int Index) {
        Vec3 Found = Behind[Mth.clamp(Index, 0, Trail - 1)];
        return Found == null ? position() : Found;
    }

    public Vec3 centre() {
        return position().add(Normal.scale(CentreLift));
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
        Player Viewer = ClientViewer.get();
        if (Viewer == null || Viewer.level() != level() || Viewer.isSpectator()) {
            discard();
            return;
        }
        Age++;
        for (int Index = Trail - 1; Index > 0; Index--) {
            Behind[Index] = Behind[Index - 1];
        }
        Behind[0] = position();
        if (Leap >= 0) {
            Leap++;
            double Speed = LeapSpeed + LeapAcceleration * Leap;
            Vec3 Step = Heading.scale(Speed).add(Normal.scale(Leap < 3 ? LeapLift : -LeapLift * 0.5));
            setPos(position().add(Step));
            if (Leap >= LeapTicks) {
                discard();
            }
            return;
        }
        if (Age >= Lifetime || offView(Viewer) < VanishCone) {
            leap();
        }
    }

    public double offView(Player Viewer) {
        Vec3 Toward = centre().subtract(Viewer.getEyePosition());
        double Length = Toward.length();
        if (Length < 1.0E-4) {
            return 0.0;
        }
        double Cosine = Mth.clamp(Viewer.getViewVector(1.0f).dot(Toward.scale(1.0 / Length)), -1.0, 1.0);
        return Math.toDegrees(Math.acos(Cosine));
    }

    public void leap() {
        if (Leap >= 0) {
            return;
        }
        Leap = 0;
        float Pitch = 1.0f + random.nextFloat() * 0.15f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp372Registry.SkitterLayers) {
            level().playLocalSound(getX(), getY() + CentreLift, getZ(), Layer.get(), SoundSource.HOSTILE, SkitterVolume, Pitch, false);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(CullReach);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 1, State -> State.setAndContinue(Leap >= 0 ? LeapPlay : ClingLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
