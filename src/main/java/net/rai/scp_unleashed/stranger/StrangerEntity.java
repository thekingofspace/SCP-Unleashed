package net.rai.scp_unleashed.stranger;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.stranger.network.StrangerDialoguePayload;
import net.rai.scp_unleashed.stranger.network.StrangerVanishPayload;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

public class StrangerEntity extends PathfinderMob implements GeoEntity {
    public static final byte StateAppearing = 0;
    public static final byte StateIdle = 1;
    public static final byte StateTalking = 2;
    public static final byte StateVanishing = 3;
    public static final byte StateFading = 4;
    public static final int AppearTicks = 50;
    public static final int VanishTicks = 36;
    public static final int FadeTicks = 40;
    public static final int TalkTimeout = 2400;
    public static final int DemoLinger = 6000;
    public static final double TalkReach = 10.0;
    public static final double WatchRange = 48.0;
    public static final float TurnSpeed = 6.0f;
    private static final EntityDataAccessor<String> VariantData = SynchedEntityData.defineId(StrangerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(StrangerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> OwnerData = SynchedEntityData.defineId(StrangerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation TalkLoop = RawAnimation.begin().thenLoop("talk");
    private static final RawAnimation VanishHold = RawAnimation.begin().thenPlayAndHold("vanish");
    private static final RawAnimation AppearPlay = RawAnimation.begin().thenPlay("appear").thenLoop("idle");
    private static final RawAnimation TiltOnce = RawAnimation.begin().thenPlay("tilt");
    private static final RawAnimation AdjustOnce = RawAnimation.begin().thenPlay("adjust");
    private static final RawAnimation NodOnce = RawAnimation.begin().thenPlay("nod");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID EncounterId;
    private int StateTicks;
    private int IdleCue = 160;
    private boolean Greeted;
    private boolean Demo;
    private byte ClientState = -1;
    private int ClientTicks;

    public StrangerEntity(EntityType<? extends StrangerEntity> Type, Level World) {
        super(Type, World);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public static StrangerEntity spawn(ServerLevel World, BlockPos Pos, float Yaw, StrangerVariant Variant, @Nullable UUID Owner, @Nullable UUID EncounterId) {
        StrangerEntity Figure = new StrangerEntity(StrangerRegistry.Figure.get(), World);
        Figure.moveTo(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5, Yaw, 0.0f);
        Figure.setYHeadRot(Yaw);
        Figure.setYBodyRot(Yaw);
        Figure.entityData.set(VariantData, Variant.Id());
        Figure.entityData.set(OwnerData, Optional.ofNullable(Owner));
        Figure.EncounterId = EncounterId;
        Figure.Demo = EncounterId == null;
        World.addFreshEntity(Figure);
        return Figure;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(VariantData, StrangerVariants.FoundationId);
        Builder.define(StateData, StateAppearing);
        Builder.define(OwnerData, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new WatchOwnerGoal(this));
    }

    public StrangerVariant variant() {
        return StrangerVariants.get(entityData.get(VariantData));
    }

    public byte state() {
        return entityData.get(StateData);
    }

    private void setState(byte Next) {
        if (state() != Next) {
            entityData.set(StateData, Next);
            StateTicks = 0;
        }
    }

    public boolean isTalking() {
        return state() == StateTalking;
    }

    public boolean isLeaving() {
        byte Current = state();
        return Current == StateVanishing || Current == StateFading;
    }

    @Nullable
    public UUID ownerId() {
        return entityData.get(OwnerData).orElse(null);
    }

    public boolean isOwner(Player User) {
        UUID Owner = ownerId();
        return Owner != null && Owner.equals(User.getUUID());
    }

    @Nullable
    public UUID encounterId() {
        return EncounterId;
    }

    @Nullable
    private Player owner() {
        UUID Owner = ownerId();
        return Owner == null ? null : level().getPlayerByUUID(Owner);
    }

    public float fade(float PartialTick) {
        float Ticks = ClientTicks + PartialTick;
        return switch (ClientState) {
            case StateAppearing -> Mth.clamp(Ticks / AppearTicks, 0.0f, 1.0f);
            case StateVanishing -> 1.0f - Mth.clamp(Ticks / VanishTicks, 0.0f, 1.0f);
            case StateFading -> 1.0f - Mth.clamp(Ticks / FadeTicks, 0.0f, 1.0f);
            default -> 1.0f;
        };
    }

    public byte clientState() {
        return ClientState;
    }

    public int clientTicks() {
        return ClientTicks;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            byte Current = state();
            if (Current != ClientState) {
                ClientState = Current;
                ClientTicks = 0;
            } else {
                ClientTicks++;
            }
            StrangerParticles.tick(this);
            return;
        }
        StateTicks++;
        faceOwner();
        switch (state()) {
            case StateAppearing -> {
                if (StateTicks >= AppearTicks) {
                    setState(StateIdle);
                }
            }
            case StateTalking -> {
                if (StateTicks >= TalkTimeout) {
                    setState(StateIdle);
                }
            }
            case StateVanishing -> {
                if (StateTicks >= VanishTicks) {
                    discard();
                }
                return;
            }
            case StateFading -> {
                if (StateTicks >= FadeTicks) {
                    discard();
                }
                return;
            }
            default -> {
            }
        }
        idleCues();
        if (tickCount % 20 == 0) {
            checkClaim();
        }
    }

    private void idleCues() {
        if (state() != StateIdle) {
            return;
        }
        Player Owner = owner();
        if (Owner != null && !Greeted && Owner.distanceToSqr(this) < 64.0) {
            Greeted = true;
            triggerAnim("action", "nod");
            playSound(StrangerRegistry.Greet.get(), 0.5f, 0.9f + random.nextFloat() * 0.1f);
            return;
        }
        if (--IdleCue <= 0) {
            IdleCue = 160 + random.nextInt(140);
            triggerAnim("action", random.nextBoolean() ? "tilt" : "adjust");
        }
    }

    private void checkClaim() {
        if (Demo) {
            if (ownerId() == null) {
                Player Nearest = level().getNearestPlayer(this, 16.0);
                if (Nearest != null) {
                    entityData.set(OwnerData, Optional.of(Nearest.getUUID()));
                }
            }
            if (tickCount > DemoLinger && !isTalking()) {
                fadeAway();
            }
            return;
        }
        if (getServer() == null || EncounterId == null || !StrangerEncounters.get(getServer()).owns(EncounterId, getUUID())) {
            fadeAway();
        }
    }

    private void faceOwner() {
        Player Owner = owner();
        if (Owner == null || Owner.distanceToSqr(this) > WatchRange * WatchRange || isLeaving()) {
            return;
        }
        double Dx = Owner.getX() - getX();
        double Dz = Owner.getZ() - getZ();
        float Wanted = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        float Turned = Mth.approachDegrees(getYRot(), Wanted, TurnSpeed);
        setYRot(Turned);
        setYBodyRot(Turned);
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        boolean Ready = state() == StateIdle || state() == StateAppearing;
        if (level().isClientSide) {
            return Ready && isOwner(User) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (!(User instanceof ServerPlayer Reader)) {
            return InteractionResult.PASS;
        }
        if (!isOwner(Reader)) {
            Reader.displayClientMessage(Component.translatable("stranger.scp_unleashed.ignores").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        if (!Ready) {
            return InteractionResult.CONSUME;
        }
        StrangerVariant Variant = variant();
        String Dialogue = Variant.Dialogue();
        int Pages = Variant.Pages();
        if (EncounterId != null && getServer() != null) {
            Optional<StrangerEncounters.Encounter> Entry = StrangerEncounters.get(getServer()).forStranger(getUUID());
            if (Entry.isPresent()) {
                Dialogue = Entry.get().getPlan().Dialogue();
                Pages = Entry.get().getPlan().Pages();
            }
        }
        setState(StateTalking);
        Greeted = true;
        triggerAnim("action", "nod");
        PacketDistributor.sendToPlayer(Reader, new StrangerDialoguePayload(getId(), Variant.Id(), Dialogue, Pages));
        return InteractionResult.SUCCESS;
    }

    public void reply(ServerPlayer Reader, boolean Finished) {
        if (!isOwner(Reader) || state() != StateTalking) {
            return;
        }
        if (!Finished || !StrangerEncounters.canReceive(Reader) || Reader.level() != level() || distanceTo(Reader) > TalkReach) {
            setState(StateIdle);
            return;
        }
        if (EncounterId != null && getServer() != null) {
            StrangerEncounters.get(getServer()).deliver(Reader, this);
        } else {
            vanish(Reader);
        }
    }

    public void vanish(@Nullable ServerPlayer Reader) {
        if (isLeaving()) {
            return;
        }
        setState(StateVanishing);
        playLayer(StrangerRegistry.Vanish.get(), 1.0f, 1.0f);
        playLayer(StrangerRegistry.VanishLow.get(), 0.9f, 1.0f);
        playLayer(StrangerRegistry.VanishChime.get(), 0.45f, 1.0f);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(StrangerRegistry.Shadow.get(), getX(), getY() + 1.0, getZ(), 36, 0.3, 0.55, 0.3, 0.015);
        }
        if (Reader != null) {
            PacketDistributor.sendToPlayer(Reader, new StrangerVanishPayload(variant().VanishColour()));
        }
    }

    public void fadeAway() {
        if (isLeaving()) {
            return;
        }
        setState(StateFading);
        playLayer(StrangerRegistry.Fade.get(), 0.5f, 1.0f);
    }

    private void playLayer(SoundEvent Sound, float Volume, float Pitch) {
        level().playSound(null, getX(), getY() + 1.0, getZ(), Sound, SoundSource.NEUTRAL, Volume, Pitch);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (getServer() != null && EncounterId != null) {
            StrangerEncounters.get(getServer()).strangerLost(getUUID());
        }
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
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean shouldShowName() {
        return hasCustomName();
    }

    @Override
    protected Component getTypeName() {
        return variant().displayName();
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(DamageSource Source) {
        return null;
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putString("Variant", entityData.get(VariantData));
        Tag.putByte("State", state());
        Tag.putInt("StateTicks", StateTicks);
        Tag.putBoolean("Greeted", Greeted);
        Tag.putBoolean("Demo", Demo);
        UUID Owner = ownerId();
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
        }
        if (EncounterId != null) {
            Tag.putUUID("Encounter", EncounterId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Variant")) {
            entityData.set(VariantData, Tag.getString("Variant"));
        }
        byte Saved = Tag.getByte("State");
        entityData.set(StateData, Saved == StateTalking ? StateIdle : Saved);
        StateTicks = Tag.getInt("StateTicks");
        Greeted = Tag.getBoolean("Greeted");
        Demo = Tag.getBoolean("Demo");
        entityData.set(OwnerData, Tag.hasUUID("Owner") ? Optional.of(Tag.getUUID("Owner")) : Optional.empty());
        EncounterId = Tag.hasUUID("Encounter") ? Tag.getUUID("Encounter") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 6, Event -> {
            byte Current = state();
            if (Current == StateVanishing || Current == StateFading) {
                return Event.setAndContinue(VanishHold);
            }
            if (Current == StateTalking) {
                return Event.setAndContinue(TalkLoop);
            }
            if (Current == StateAppearing) {
                return Event.setAndContinue(AppearPlay);
            }
            return Event.setAndContinue(IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 4, Event -> PlayState.STOP)
                .triggerableAnim("tilt", TiltOnce)
                .triggerableAnim("adjust", AdjustOnce)
                .triggerableAnim("nod", NodOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    private static final class WatchOwnerGoal extends Goal {
        private final StrangerEntity Watcher;

        private WatchOwnerGoal(StrangerEntity Watcher) {
            this.Watcher = Watcher;
            setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player Owner = Watcher.owner();
            return Owner != null && !Watcher.isLeaving() && Owner.distanceToSqr(Watcher) < WatchRange * WatchRange;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player Owner = Watcher.owner();
            if (Owner != null) {
                Watcher.getLookControl().setLookAt(Owner.getX(), Owner.getEyeY(), Owner.getZ(), 12.0f, 40.0f);
            }
        }
    }
}
