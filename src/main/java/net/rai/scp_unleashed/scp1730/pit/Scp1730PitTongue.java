package net.rai.scp_unleashed.scp1730.pit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp1730PitTongue extends Entity implements GeoEntity {
    public static final float BaseLength = 2.5f;
    private static final EntityDataAccessor<Float> DataLength = SynchedEntityData.defineId(Scp1730PitTongue.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DataOwner = SynchedEntityData.defineId(Scp1730PitTongue.class, EntityDataSerializers.INT);
    private static final RawAnimation Lash = RawAnimation.begin().thenLoop("lash");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Idle;
    public float LengthOld;

    public Scp1730PitTongue(EntityType<? extends Scp1730PitTongue> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    @Nullable
    public static Scp1730PitTongue spawn(ServerLevel World, Scp1730PitForm Owner) {
        Scp1730PitTongue Made = new Scp1730PitTongue(Scp1730PitRegistry.TongueType.get(), World);
        Vec3 Mouth = Owner.mouth();
        Made.moveTo(Mouth.x, Mouth.y, Mouth.z, Owner.getYRot(), 0.0f);
        Made.entityData.set(DataOwner, Owner.getId());
        Made.entityData.set(DataLength, 0.1f);
        return World.addFreshEntity(Made) ? Made : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataLength, 0.1f);
        Builder.define(DataOwner, -1);
    }

    public float length() {
        return entityData.get(DataLength);
    }

    public void follow(Vec3 Mouth, Vec3 Tip) {
        Vec3 Way = Tip.subtract(Mouth);
        double Flat = Math.sqrt(Way.x * Way.x + Way.z * Way.z);
        setPos(Mouth.x, Mouth.y, Mouth.z);
        setYRot((float) (Mth.atan2(Way.z, Way.x) * Mth.RAD_TO_DEG) - 90.0f);
        setXRot((float) -(Mth.atan2(Way.y, Math.max(1.0e-4, Flat)) * Mth.RAD_TO_DEG));
        entityData.set(DataLength, (float) Math.max(0.1, Way.length()));
        Idle = 0;
    }

    @Override
    public void tick() {
        LengthOld = length();
        super.tick();
        if (level().isClientSide) {
            return;
        }
        Idle++;
        Entity Owner = level().getEntity(entityData.get(DataOwner));
        if (!(Owner instanceof Scp1730PitForm) || !Owner.isAlive() || Idle > 6) {
            discard();
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
    protected void readAdditionalSaveData(CompoundTag Tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 0, Animated -> Animated.setAndContinue(Lash)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
