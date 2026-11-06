package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class CourtProp extends Mob implements GeoEntity, CourtMember {
    public enum PropKind {
        ThornWall("scarlet_thorn_wall", "wither"),
        BriarCage("scarlet_briar_cage", "break"),
        ChainAnchor("scarlet_chain_anchor", null),
        ThornPatch("scarlet_thorn_patch", null);

        private final String Id;
        private final String EndClip;

        PropKind(String Id, String EndClip) {
            this.Id = Id;
            this.EndClip = EndClip;
        }

        public String id() {
            return Id;
        }

        public String endClip() {
            return EndClip;
        }

        public static PropKind of(EntityType<?> Type) {
            String Path = BuiltInRegistries.ENTITY_TYPE.getKey(Type).getPath();
            for (PropKind Each : values()) {
                if (Each.Id.equals(Path)) {
                    return Each;
                }
            }
            return ThornPatch;
        }
    }

    public static final int GrowTicks = 16;
    public static final int EndTicks = 20;
    public static final int CageGrip = 30;
    private static final EntityDataAccessor<Integer> AgeData = SynchedEntityData.defineId(CourtProp.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EndingData = SynchedEntityData.defineId(CourtProp.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation GrowOnce = RawAnimation.begin().thenPlay("grow").thenLoop("idle");
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final PropKind Kind;
    private int Life = 400;
    private int Ending;
    @Nullable
    private UUID OwnerId;
    @Nullable
    private UUID CaptiveId;

    public CourtProp(EntityType<? extends Mob> Type, Level World) {
        super(Type, World);
        Kind = PropKind.of(Type);
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Nullable
    public static CourtProp spawn(ServerLevel World, EntityType<CourtProp> Type, BlockPos Near, int Life) {
        BlockPos Spot = CourtArena.groundOr(World, Near, Near);
        CourtProp Made = Type.create(World);
        if (Made == null) {
            return null;
        }
        Made.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, 0.0f, 0.0f);
        Made.face(World.random.nextFloat() * 360.0f);
        Made.Life = Life;
        Made.addTag(CourtFx.Tag);
        World.addFreshEntity(Made);
        ScarletCourt.Instance.adopt(Made);
        return Made;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(AgeData, 0);
        Builder.define(EndingData, false);
    }

    public PropKind kind() {
        return Kind;
    }

    @Override
    public String courtId() {
        return Kind.id();
    }

    public void face(float Yaw) {
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
        yRotO = Yaw;
        yBodyRotO = Yaw;
        yHeadRotO = Yaw;
    }

    public void life(int Ticks) {
        Life = Ticks;
    }

    public int life() {
        return Life;
    }

    public void owner(@Nullable Entity Owner) {
        OwnerId = Owner == null ? null : Owner.getUUID();
    }

    @Nullable
    public UUID ownerId() {
        return OwnerId;
    }

    public void captive(@Nullable LivingEntity Victim) {
        CaptiveId = Victim == null ? null : Victim.getUUID();
    }

    @Nullable
    public LivingEntity captive() {
        if (CaptiveId == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(CaptiveId) instanceof LivingEntity Living ? Living : null;
    }

    public boolean ending() {
        return entityData.get(EndingData);
    }

    public void end() {
        if (!ending()) {
            entityData.set(EndingData, true);
            Ending = 0;
            if (level() instanceof ServerLevel World) {
                if (Kind == PropKind.BriarCage || Kind == PropKind.ThornWall) {
                    CourtFx.crumble(World, Blocks.DARK_OAK_LOG.defaultBlockState(), position().add(0.0, getBbHeight() * 0.5, 0.0), 24, getBbWidth() * 0.4, getBbHeight() * 0.3);
                    CourtFx.sound(World, position(), "thorns_cage", 0.8f, 1.4f);
                } else if (Kind == PropKind.ChainAnchor) {
                    CourtFx.crumble(World, Blocks.CHAIN.defaultBlockState(), position().add(0.0, 0.4, 0.0), 20, 0.4, 0.3);
                    CourtFx.sound(World, position(), "bride_chain", 1.0f, 1.3f);
                }
            }
        }
    }

    public float radius() {
        return switch (Kind) {
            case ThornPatch -> 0.9f;
            default -> getBbWidth() * 0.5f;
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        int Age = entityData.get(AgeData) + 1;
        if (Age <= GrowTicks + 1) {
            entityData.set(AgeData, Age);
        }
        ServerLevel World = (ServerLevel) level();
        if (ending()) {
            Ending++;
            if (Ending >= EndTicks || Kind.endClip() == null) {
                discard();
            }
            return;
        }
        if (--Life <= 0) {
            end();
            return;
        }
        switch (Kind) {
            case ThornWall -> wall(World);
            case BriarCage -> cage(World);
            case ThornPatch -> patch(World);
            default -> {
            }
        }
    }

    private void wall(ServerLevel World) {
        if (tickCount % 10 != 0) {
            return;
        }
        for (LivingEntity Victim : CourtFx.victims(World, getBoundingBox().inflate(0.35, 0.0, 0.35))) {
            CourtFx.strike(damageSources().thorns(this), Victim, 3.0f);
            CourtFx.push(Victim, position(), 0.45, 0.15);
        }
    }

    private void cage(ServerLevel World) {
        LivingEntity Victim = captive();
        if (Victim == null || !Victim.isAlive() || Victim.distanceTo(this) > 3.5) {
            if (tickCount > 40) {
                end();
            }
            return;
        }
        Vec3 Centre = position();
        double Dx = Victim.getX() - Centre.x;
        double Dz = Victim.getZ() - Centre.z;
        if (Dx * Dx + Dz * Dz > 0.2) {
            Victim.setDeltaMovement(-Dx * 0.25, Math.min(0.0, Victim.getDeltaMovement().y), -Dz * 0.25);
            Victim.hurtMarked = true;
        }
        Victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1, false, false, true));
        if (tickCount % CageGrip == 0) {
            CourtFx.strike(damageSources().thorns(this), Victim, 1.0f);
        }
    }

    private void patch(ServerLevel World) {
        if (tickCount % 10 != 0) {
            return;
        }
        for (LivingEntity Victim : CourtFx.victimsNear(World, position(), 1.0)) {
            if (Math.abs(Victim.getY() - getY()) < 0.9) {
                Victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false, true));
                if (tickCount % 20 == 0) {
                    CourtFx.strike(damageSources().thorns(this), Victim, 1.0f);
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        if (Kind != PropKind.BriarCage && Kind != PropKind.ChainAnchor || ending()) {
            return false;
        }
        Entity Attacker = Source.getEntity();
        if (Attacker == null || CourtFx.allied(this, Attacker)) {
            return false;
        }
        if (level() instanceof ServerLevel World) {
            CourtFx.crumble(World, Kind == PropKind.ChainAnchor ? Blocks.CHAIN.defaultBlockState() : Blocks.DARK_OAK_LOG.defaultBlockState(),
                    position().add(0.0, getBbHeight() * 0.5, 0.0), 6, 0.3, 0.3);
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public void die(DamageSource Source) {
        if (!level().isClientSide && !ending()) {
            end();
        }
        super.die(Source);
    }

    @Override
    protected void tickDeath() {
        if (++deathTime >= EndTicks && !level().isClientSide) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return Kind == PropKind.ThornWall && !ending();
    }

    @Override
    public boolean isPickable() {
        return Kind == PropKind.BriarCage || Kind == PropKind.ChainAnchor || Kind == PropKind.ThornWall;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return CourtFx.allied(this, Other) || super.isAlliedTo(Other);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel World, DamageSource Source, boolean Recent) {
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Life", Life);
        if (OwnerId != null) {
            Tag.putUUID("PropOwner", OwnerId);
        }
        if (CaptiveId != null) {
            Tag.putUUID("Captive", CaptiveId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Life = Tag.contains("Life") ? Tag.getInt("Life") : Life;
        OwnerId = Tag.hasUUID("PropOwner") ? Tag.getUUID("PropOwner") : null;
        CaptiveId = Tag.hasUUID("Captive") ? Tag.getUUID("Captive") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 3, State -> {
            CourtProp Owner = State.getAnimatable();
            if ((Owner.ending() || Owner.isDeadOrDying()) && Owner.Kind.endClip() != null) {
                return State.setAndContinue(RawAnimation.begin().thenPlayAndHold(Owner.Kind.endClip()));
            }
            if (Owner.entityData.get(AgeData) <= GrowTicks && (Owner.Kind == PropKind.ThornWall || Owner.Kind == PropKind.BriarCage)) {
                return State.setAndContinue(GrowOnce);
            }
            return State.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
