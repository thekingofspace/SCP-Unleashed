package net.rai.scp_unleashed.scp087;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class Scp087Face extends Entity implements GeoEntity {
    public static final float Width = 0.5f;
    public static final float Height = 0.6f;
    public static final int MaxLife = 900;
    public static final int SeenNeeded = 3;
    public static final int AwayLimit = 8;
    public static final int FadeIn = 14;
    public static final int FadeOut = 5;
    public static final double ViewCone = 0.5;
    public static final RawAnimation DriftLoop = RawAnimation.begin().thenLoop("drift");
    private static final EntityDataAccessor<Boolean> VanishingData = SynchedEntityData.defineId(Scp087Face.class, EntityDataSerializers.BOOLEAN);
    @Nullable
    private UUID Token;
    @Nullable
    private UUID Watcher;
    private float Reach = 6.0f;
    private float LaughOdds;
    private int Life;
    private int Seen;
    private int Away;
    private int VanishAt = -1;
    private int ClientVanish = -1;
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);

    public Scp087Face(EntityType<? extends Scp087Face> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(VanishingData, false);
    }

    public void setup(UUID InstanceToken, UUID Viewer, float VanishReach, float Laugh) {
        Token = InstanceToken;
        Watcher = Viewer;
        Reach = VanishReach;
        LaughOdds = Laugh;
    }

    public void bind(UUID InstanceToken) {
        Token = InstanceToken;
    }

    @Nullable
    public UUID token() {
        return Token;
    }

    public boolean vanishing() {
        return entityData.get(VanishingData);
    }

    public float fade(float PartialTick) {
        float Time = tickCount + PartialTick;
        float In = Mth.clamp(Time / FadeIn, 0.0f, 1.0f);
        if (ClientVanish >= 0) {
            In *= Mth.clamp(1.0f - (Time - ClientVanish) / FadeOut, 0.0f, 1.0f);
        }
        return In;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (VanishingData.equals(Accessor) && level().isClientSide && vanishing() && ClientVanish < 0) {
            ClientVanish = tickCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (VanishAt >= 0) {
            if (tickCount >= VanishAt) {
                discard();
            }
            return;
        }
        Life++;
        Player Found = Watcher == null ? null : World.getPlayerByUUID(Watcher);
        if (!(Found instanceof ServerPlayer Viewer) || !Viewer.isAlive() || !Targeting.canWitness(Viewer) || Viewer.level() != World || Life > MaxLife || !Scp087Descent.isLive(Token)) {
            discard();
            return;
        }
        Vec3 Eye = CameraViews.eye(Viewer);
        Vec3 Centre = position().add(0.0, Height / 2.0, 0.0);
        setYRot(Scp087Encounters.yawToward(Centre, Eye));
        double Gap = Eye.distanceTo(Centre);
        if (Gap < Reach) {
            vanish(World, true);
            return;
        }
        boolean Facing = !Senses.isBlindfolded(Viewer) && Centre.subtract(Eye).normalize().dot(CameraViews.look(Viewer)) > ViewCone;
        boolean Clear = Facing && World.clip(new ClipContext(Eye, Centre, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer)).getType() == HitResult.Type.MISS;
        if (Clear) {
            Seen++;
            Away = 0;
        } else if (Seen >= SeenNeeded) {
            Away++;
            if (Away >= AwayLimit) {
                vanish(World, false);
            }
        }
    }

    private void vanish(ServerLevel World, boolean Approached) {
        VanishAt = tickCount + FadeOut;
        entityData.set(VanishingData, true);
        Vec3 Centre = position().add(0.0, Height / 2.0, 0.0);
        if (Approached) {
            Scp087Sounds.play(World, Centre, Scp087Registry.FaceVanish, SoundSource.HOSTILE, 0.5f, 0.9f + random.nextFloat() * 0.15f);
        }
        if (LaughOdds > 0.0f && random.nextFloat() < LaughOdds) {
            Scp087Sounds.laugh(World, Centre.add(0.0, -3.0, 0.0), Approached ? 1.2f : 0.9f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 48.0 * 48.0;
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
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
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
        Controllers.add(new AnimationController<>(this, "drift", 0, Event -> Event.setAndContinue(DriftLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
