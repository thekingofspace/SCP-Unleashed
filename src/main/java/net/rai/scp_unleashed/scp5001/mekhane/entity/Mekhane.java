package net.rai.scp_unleashed.scp5001.mekhane.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneTimeline;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumMap;
import java.util.Map;

public class Mekhane extends Entity implements GeoEntity {
    public static final float Width = 6.0f;
    public static final float Height = 48.0f;
    public static final float RenderScale = 7.5f;
    public static final float TiltPivot = 4.0f;
    public static final float Faded = 0.02f;
    public static final float DiveLow = -26.0f;
    public static final float DiveHigh = 32.0f;
    public static final float BankLimit = 32.0f;
    public static final double CullReach = 75.0;
    public static final double CullBelow = 20.0;
    public static final double CullAbove = 98.0;
    public static final double AimReach = 40.0;
    public static final double PortalReach = 56.0;
    public static final int TrackChunks = 40;
    private static final EntityDataAccessor<Integer> ActData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DamageData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ChargeData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AlphaData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LaserData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3f> TargetData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> PortalData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> PortalOpenData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DiveData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BankData = SynchedEntityData.defineId(Mekhane.class, EntityDataSerializers.FLOAT);
    private static final RawAnimation Gears = RawAnimation.begin().thenLoop("gears");
    private static final Map<MekhaneTimeline.Act, RawAnimation> Clips = new EnumMap<>(MekhaneTimeline.Act.class);

    static {
        for (MekhaneTimeline.Act Each : MekhaneTimeline.Act.values()) {
            Clips.put(Each, Each.loops() ? RawAnimation.begin().thenLoop(Each.Clip) : RawAnimation.begin().thenPlayAndHold(Each.Clip));
        }
    }

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private float ChargeO;
    private float AlphaO;
    private float LaserO;
    private float PortalOpenO;
    private float DiveO;
    private float BankO;
    private float Dive;
    private float Bank;
    private Vec3 Surge = Vec3.ZERO;
    private boolean Primed;
    private boolean Ticked;
    private int Drawn = -1;

    public Mekhane(EntityType<? extends Mekhane> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(ActData, MekhaneTimeline.Act.Descend.ordinal());
        Builder.define(DamageData, 0);
        Builder.define(ChargeData, 0.0f);
        Builder.define(AlphaData, 1.0f);
        Builder.define(LaserData, 0.0f);
        Builder.define(TargetData, new Vector3f());
        Builder.define(PortalData, new Vector3f());
        Builder.define(PortalOpenData, 0.0f);
        Builder.define(DiveData, 0.0f);
        Builder.define(BankData, 0.0f);
    }

    public void place(Vec3 At, float Yaw) {
        moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        setYRot(Yaw);
        setYHeadRot(Yaw);
    }

    @Override
    public void setYHeadRot(float Value) {
    }

    public void glide(Vec3 Goal, Vec3 Look, float Rate) {
        Vec3 Toward = Look.subtract(Goal);
        float Yaw = getYRot();
        if (Toward.horizontalDistanceSqr() > 1.0E-4) {
            float Wanted = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
            Yaw = Mth.approachDegrees(Yaw, Wanted, Rate);
        }
        moveTo(Goal.x, Goal.y, Goal.z, Yaw, 0.0f);
        setYRot(Yaw);
    }

    public Vec3 surge(Vec3 Wanted, double Limit) {
        Vec3 Change = Wanted.subtract(Surge);
        double Length = Change.length();
        Surge = Length > Limit ? Surge.add(Change.scale(Limit / Length)) : Wanted;
        return Surge;
    }

    public void tilt(float WantedDive, float WantedBank) {
        Dive += (Mth.clamp(WantedDive, DiveLow, DiveHigh) - Dive) * 0.15f;
        Bank += (Mth.clamp(WantedBank, -BankLimit, BankLimit) - Bank) * 0.15f;
        if (Math.abs(entityData.get(DiveData) - Dive) > 0.05f) {
            entityData.set(DiveData, Dive);
        }
        if (Math.abs(entityData.get(BankData) - Bank) > 0.05f) {
            entityData.set(BankData, Bank);
        }
    }

    public float dive(float Partial) {
        return Primed ? Mth.lerp(Partial, DiveO, entityData.get(DiveData)) : entityData.get(DiveData);
    }

    public float bank(float Partial) {
        return Primed ? Mth.lerp(Partial, BankO, entityData.get(BankData)) : entityData.get(BankData);
    }

    public void drive(MekhaneTimeline.Act Act, int Damage, float Charge, float Alpha) {
        if (entityData.get(ActData) != Act.ordinal()) {
            entityData.set(ActData, Act.ordinal());
        }
        if (entityData.get(DamageData) != Damage) {
            entityData.set(DamageData, Damage);
        }
        if (Math.abs(entityData.get(ChargeData) - Charge) > 0.004f) {
            entityData.set(ChargeData, Charge);
        }
        if (Math.abs(entityData.get(AlphaData) - Alpha) > 0.004f) {
            entityData.set(AlphaData, Alpha);
        }
    }

    public void aim(Vec3 Target, float Laser) {
        Vector3f Packed = new Vector3f((float) Target.x, (float) Target.y, (float) Target.z);
        if (entityData.get(TargetData).distanceSquared(Packed) > 0.04f) {
            entityData.set(TargetData, Packed);
        }
        if (Math.abs(entityData.get(LaserData) - Laser) > 0.004f) {
            entityData.set(LaserData, Laser);
        }
    }

    public Vec3 portal() {
        Vector3f Value = entityData.get(PortalData);
        return new Vec3(Value.x, Value.y, Value.z);
    }

    public void portal(Vec3 At) {
        entityData.set(PortalData, new Vector3f((float) At.x, (float) At.y, (float) At.z));
    }

    public float portalOpen() {
        return entityData.get(PortalOpenData);
    }

    public void opening(float Value) {
        if (Math.abs(entityData.get(PortalOpenData) - Value) > 0.004f) {
            entityData.set(PortalOpenData, Value);
        }
    }

    public MekhaneTimeline.Act act() {
        return MekhaneTimeline.Act.of(entityData.get(ActData));
    }

    public int damage() {
        return entityData.get(DamageData);
    }

    public float yaw(float Partial) {
        return Mth.rotLerp(Partial, yRotO, getYRot());
    }

    public Vec3 target() {
        Vector3f Value = entityData.get(TargetData);
        return new Vec3(Value.x, Value.y, Value.z);
    }

    public float charge(float Partial) {
        return Primed ? Mth.lerp(Partial, ChargeO, entityData.get(ChargeData)) : entityData.get(ChargeData);
    }

    public float alpha(float Partial) {
        return Primed ? Mth.lerp(Partial, AlphaO, entityData.get(AlphaData)) : entityData.get(AlphaData);
    }

    public float laser(float Partial) {
        return Primed ? Mth.lerp(Partial, LaserO, entityData.get(LaserData)) : entityData.get(LaserData);
    }

    public float portalOpen(float Partial) {
        return Primed ? Mth.lerp(Partial, PortalOpenO, entityData.get(PortalOpenData)) : entityData.get(PortalOpenData);
    }

    public boolean primed() {
        return Primed;
    }

    public boolean ticked() {
        return Ticked;
    }

    public void settle() {
        Ticked = false;
    }

    public int drawn() {
        return Drawn;
    }

    public void drawn(int Frame) {
        Drawn = Frame;
    }

    @Override
    public void tick() {
        ChargeO = entityData.get(ChargeData);
        AlphaO = entityData.get(AlphaData);
        LaserO = entityData.get(LaserData);
        PortalOpenO = entityData.get(PortalOpenData);
        DiveO = entityData.get(DiveData);
        BankO = entityData.get(BankData);
        Primed = true;
        Ticked = true;
        super.tick();
        if (level().isClientSide && random.nextInt(3) == 0) {
            double Scale = RenderScale / 16.0;
            float Radians = getYRot() * Mth.DEG_TO_RAD;
            double Ox = (random.nextDouble() - 0.5) * 18.0 * Scale;
            double Oy = (50.0 + random.nextDouble() * 40.0) * Scale;
            double Oz = 6.0 * Scale;
            double X = getX() + Ox * Mth.cos(Radians) - Oz * Mth.sin(Radians);
            double Z = getZ() + Ox * Mth.sin(Radians) + Oz * Mth.cos(Radians);
            level().addParticle(ParticleTypes.END_ROD, X, getY() + Oy, Z, 0.0, -0.05, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB Around = body(getX(), getY(), getZ());
        if (laser(1.0f) > 0.0f) {
            Vec3 Aim = target();
            Around = Around.minmax(new AABB(Aim, Aim).inflate(AimReach));
        }
        if (portalOpen() > 0.0f) {
            Vec3 Gate = portal();
            Around = Around.minmax(new AABB(Gate, Gate).inflate(PortalReach));
        }
        return Around;
    }

    public static AABB body(double X, double Y, double Z) {
        return new AABB(X - CullReach, Y - CullBelow, Z - CullReach, X + CullReach, Y + CullAbove, Z + CullReach);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 6, State -> State.setAndContinue(Clips.get(act()))));
        Controllers.add(new AnimationController<>(this, "gears", 0, State -> {
            State.getController().setAnimation(Gears);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
