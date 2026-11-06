package net.rai.scp_unleashed.scp2845;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Scp2845Spirit extends Entity implements GeoEntity {
    public static final float Width = 1.1f;
    public static final float Height = 2.2f;
    public static final float RenderScale = 0.8f;
    public static final int Delay = 12;
    public static final int Life = 64;
    public static final int Fade = 10;
    public static final double Speed = 0.42;
    public static final float Damage = 6.0f;
    public static final double Gallop = 2.75 * RenderScale;
    private static final EntityDataAccessor<Vector3f> HeadingData = SynchedEntityData.defineId(Scp2845Spirit.class, EntityDataSerializers.VECTOR3);
    private static final RawAnimation GallopLoop = RawAnimation.begin().thenLoop("gallop");
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> Struck = new HashSet<>();
    @Nullable
    private UUID OwnerId;
    private double Cycle;
    private double CycleO;

    public Scp2845Spirit(EntityType<? extends Scp2845Spirit> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    public static Scp2845Spirit charge(ServerLevel World, Vec3 At, Vec3 Way, @Nullable Entity Owner) {
        Scp2845Spirit Made = new Scp2845Spirit(Scp2845Registry.SpiritType.get(), World);
        Vec3 Flat = new Vec3(Way.x, 0.0, Way.z).normalize();
        float Yaw = yawOf(Flat);
        Made.entityData.set(HeadingData, new Vector3f((float) Flat.x, 0.0f, (float) Flat.z));
        Made.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Made.yRotO = Yaw;
        Made.OwnerId = Owner == null ? null : Owner.getUUID();
        World.addFreshEntity(Made);
        World.sendParticles(ParticleTypes.END_ROD, At.x, At.y + 1.0, At.z, 10, 0.5, 0.8, 0.5, 0.02);
        return Made;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(HeadingData, new Vector3f(0.0f, 0.0f, 1.0f));
    }

    public Vec3 heading() {
        Vector3f Value = entityData.get(HeadingData);
        return new Vec3(Value.x(), 0.0, Value.z());
    }

    public static float yawOf(Vec3 Way) {
        return (float) (Mth.atan2(Way.z, Way.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public float headingYaw() {
        return yawOf(heading());
    }

    public double cycle(float Partial) {
        double Value = CycleO + (Cycle - CycleO) * Partial;
        return Value - Math.floor(Value);
    }

    private float RenderPartial;

    public void setRenderPartial(float Value) {
        RenderPartial = Value;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public double cycles() {
        return Cycle;
    }

    public boolean charging() {
        return tickCount >= Delay;
    }

    public float alpha(float Partial) {
        float Age = tickCount + Partial;
        float In = Mth.clamp(Age / Delay, 0.0f, 1.0f);
        float Out = Mth.clamp((Life - Age) / Fade, 0.0f, 1.0f);
        return 0.62f * Math.min(In, Out);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 Way = heading();
        float Yaw = yawOf(Way);
        setYRot(Yaw);
        yRotO = Yaw;
        CycleO = Cycle;
        if (charging()) {
            Vec3 Before = position();
            setPos(Before.add(Way.scale(Speed)));
            Cycle += position().subtract(Before).horizontalDistance() / Gallop;
        }
        if (level().isClientSide) {
            if (charging() && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5), getY() + random.nextDouble() * 1.6, getZ() + (random.nextDouble() - 0.5),
                        -Way.x * 0.05, 0.01, -Way.z * 0.05);
            }
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (charging()) {
            trample(World);
        }
        if (tickCount >= Life) {
            discard();
        }
    }

    private void trample(ServerLevel World) {
        Entity Owner = OwnerId == null ? null : World.getEntity(OwnerId);
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.4), Candidate -> Candidate.isAlive() && !(Candidate instanceof Scp2845))) {
            if (Each instanceof Player User && (User.isCreative() || User.isSpectator()) || !Struck.add(Each.getUUID())) {
                continue;
            }
            Each.hurt(World.damageSources().indirectMagic(this, Owner), Damage);
            Vec3 Way = heading();
            Each.push(Way.x * 1.1, 0.5, Way.z * 1.1);
            Each.hurtMarked = true;
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
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
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp2845SpiritGait(this, "body", 2, Event -> Event.setAndContinue(charging() ? GallopLoop : IdleLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
