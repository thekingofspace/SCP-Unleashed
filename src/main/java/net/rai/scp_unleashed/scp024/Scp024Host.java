package net.rai.scp_unleashed.scp024;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

public class Scp024Host extends PathfinderMob implements GeoEntity {
    public static final float TurnSpeed = 8.0f;
    public static final double WatchRange = 40.0;
    private static final EntityDataAccessor<Boolean> TalkingData = SynchedEntityData.defineId(Scp024Host.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> GuestData = SynchedEntityData.defineId(Scp024Host.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation TalkLoop = RawAnimation.begin().thenLoop("talk");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Fidget = 120;

    public Scp024Host(EntityType<? extends Scp024Host> Type, Level World) {
        super(Type, World);
        setInvulnerable(true);
        setPersistenceRequired();
        setNoAi(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(TalkingData, false);
        Builder.define(GuestData, Optional.empty());
    }

    public void setGuest(UUID Contestant) {
        entityData.set(GuestData, Optional.of(Contestant));
    }

    @Nullable
    public UUID guest() {
        return entityData.get(GuestData).orElse(null);
    }

    public boolean isTalking() {
        return entityData.get(TalkingData);
    }

    public void setTalking(boolean Talking) {
        entityData.set(TalkingData, Talking);
    }

    public void gesture(String Name) {
        triggerAnim("action", Name);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        UUID Guest = guest();
        Player Watched = Guest == null ? null : level().getPlayerByUUID(Guest);
        if (Watched != null && Watched.distanceToSqr(this) < WatchRange * WatchRange) {
            double Dx = Watched.getX() - getX();
            double Dz = Watched.getZ() - getZ();
            float Wanted = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
            float Turned = Mth.approachDegrees(getYRot(), Wanted, TurnSpeed);
            setYRot(Turned);
            setYBodyRot(Turned);
            setYHeadRot(Turned);
            getLookControl().setLookAt(Watched, 30.0f, 30.0f);
        }
        if (--Fidget <= 0) {
            Fidget = 140 + random.nextInt(160);
            gesture(isTalking() ? "point" : random.nextBoolean() ? "adjust" : "point");
        }
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (User instanceof ServerPlayer Contestant) {
            Scp024Runs.hostClicked(Contestant);
            gesture("bow");
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "base", 6, State -> State.setAndContinue(isTalking() ? TalkLoop : IdleLoop)));
        Controllers.add(new AnimationController<Scp024Host>(this, "action", 4, State -> PlayState.STOP)
                .triggerableAnim("point", RawAnimation.begin().thenPlay("point"))
                .triggerableAnim("laugh", RawAnimation.begin().thenPlay("laugh"))
                .triggerableAnim("bow", RawAnimation.begin().thenPlay("bow"))
                .triggerableAnim("adjust", RawAnimation.begin().thenPlay("adjust")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.scp_unleashed.scp024_host");
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
