package net.rai.scp_unleashed.z0002;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Z0002Slash extends Entity {
    public static final float Damage = 14.0f;
    public static final float Pierce = 0.4f;
    public static final double Speed = 2.0;
    public static final double Range = 24.0;
    public static final double ArcRadius = 3.6;
    public static final float ArcSpan = 70.0f;
    public static final int ArcPoints = 13;
    public static final double HitInflate = 0.3;
    public static final double Knock = 0.35;
    public static final float MaxRoll = 38.0f;
    public static final float MinRoll = 10.0f;
    public static final double CutHeight = 0.8;
    public static final double LaunchDrop = 0.35;
    public static final double LaunchAhead = 0.9;
    private static final EntityDataAccessor<Float> RollData = SynchedEntityData.defineId(Z0002Slash.class, EntityDataSerializers.FLOAT);
    @Nullable
    private UUID OwnerId;
    @Nullable
    private UUID DomainId;
    private double Travelled;
    private final Set<Integer> Struck = new HashSet<>();

    public Z0002Slash(EntityType<? extends Z0002Slash> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(RollData, 0.0f);
    }

    public static Z0002Slash launch(ServerLevel World, ServerPlayer Owner, Z0002Domain Domain) {
        Z0002Slash Made = new Z0002Slash(Z0002Registry.Slash.get(), World);
        Vec3 Look = Owner.getLookAngle();
        Vec3 Start = Owner.getEyePosition().add(0.0, -LaunchDrop, 0.0).add(Look.scale(LaunchAhead));
        Made.moveTo(Start.x, Start.y, Start.z, Owner.getYRot(), Owner.getXRot());
        float Roll = (World.random.nextFloat() * 2.0f - 1.0f) * MaxRoll;
        if (Math.abs(Roll) < MinRoll) {
            Roll = Roll < 0.0f ? -MinRoll : MinRoll;
        }
        Made.entityData.set(RollData, Roll);
        Made.OwnerId = Owner.getUUID();
        Made.DomainId = Domain.Id;
        Made.setDeltaMovement(Look.scale(Speed));
        World.addFreshEntity(Made);
        return Made;
    }

    public float roll() {
        return entityData.get(RollData);
    }

    @Nullable
    public UUID ownerId() {
        return OwnerId;
    }

    public Vec3 forward() {
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    public Vec3 side() {
        Vec3 Ahead = forward();
        Vec3 Flat = Ahead.cross(new Vec3(0.0, 1.0, 0.0));
        if (Flat.lengthSqr() < 1.0E-6) {
            Flat = new Vec3(1.0, 0.0, 0.0);
        }
        Flat = Flat.normalize();
        Vec3 Lift = Flat.cross(Ahead).normalize();
        float Turn = roll() * Mth.DEG_TO_RAD;
        return Flat.scale(Mth.cos(Turn)).add(Lift.scale(Mth.sin(Turn)));
    }

    public Vec3 arcPoint(Vec3 Tip, double Along) {
        double Angle = Along * ArcSpan * Mth.DEG_TO_RAD;
        return Tip.add(forward().scale(ArcRadius * (Math.cos(Angle) - 1.0))).add(side().scale(ArcRadius * Math.sin(Angle)));
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 From = position();
        Vec3 Step = getDeltaMovement();
        if (Step.lengthSqr() < 1.0E-6) {
            Step = forward().scale(Speed);
        }
        Vec3 To = From.add(Step);
        if (level() instanceof ServerLevel World) {
            Z0002Domain Domain = DomainId == null ? null : Z0002Domains.byId(World.getServer(), DomainId);
            if (Domain == null || !Domain.active() || tickCount > Range / Speed + 4) {
                discard();
                return;
            }
            sweep(World, From, To);
            cut(World, Domain, To);
            Travelled += Step.length();
            if (Travelled >= Range || Domain.Region.reach(To) > Domain.Region.Radius() - 0.8 || To.y < Domain.Region.floorY() + 0.5) {
                fade(World, To);
                discard();
                return;
            }
        } else {
            trail(From, To);
        }
        setPos(To.x, To.y, To.z);
    }

    private void sweep(ServerLevel World, Vec3 From, Vec3 To) {
        AABB Area = new AABB(From, To).inflate(ArcRadius + 1.0);
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Area, Each -> Each.isAlive() && !Each.getUUID().equals(OwnerId) && !Struck.contains(Each.getId()))) {
            AABB Box = Target.getBoundingBox().inflate(HitInflate);
            for (int Index = 0; Index < ArcPoints; Index++) {
                double Along = -1.0 + 2.0 * Index / (ArcPoints - 1);
                Vec3 Start = arcPoint(From, Along);
                Vec3 End = arcPoint(To, Along);
                if (Box.contains(Start) || Box.contains(End) || Box.clip(Start, End).isPresent()) {
                    strike(World, Target);
                    break;
                }
            }
        }
    }

    private void strike(ServerLevel World, LivingEntity Target) {
        Struck.add(Target.getId());
        Entity Owner = OwnerId == null ? null : World.getEntity(OwnerId);
        DamageSource Source = World.damageSources().source(Z0002Registry.SlashDamage, this, Owner);
        float Armour = Target.getArmorValue();
        float Toughness = (float) Target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float Amount = Damage * Pierce + CombatRules.getDamageAfterAbsorb(Target, Damage * (1.0f - Pierce), Source, Armour, Toughness);
        if (Target.hurt(Source, Amount)) {
            Vec3 Push = forward();
            Target.knockback(Knock, -Push.x, -Push.z);
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Z0002Registry.HitLayers) {
            World.playSound(null, Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), Layer.get(), SoundSource.PLAYERS, 1.0f, 0.9f + random.nextFloat() * 0.2f);
        }
        World.sendParticles(Z0002Registry.Spark.get(), Target.getX(), Target.getY() + Target.getBbHeight() * 0.6, Target.getZ(), 14, 0.3, 0.4, 0.3, 0.25);
    }

    private void cut(ServerLevel World, Z0002Domain Domain, Vec3 Tip) {
        double FloorTop = Domain.Region.floorY() + 1.0;
        for (int Index = 0; Index < ArcPoints; Index++) {
            Vec3 Point = arcPoint(Tip, -1.0 + 2.0 * Index / (ArcPoints - 1));
            if (Point.y - FloorTop > CutHeight) {
                continue;
            }
            BlockPos Floor = BlockPos.containing(Point.x, Domain.Region.floorY(), Point.z);
            BlockState State = World.getBlockState(Floor);
            if (Domain.Region.isFloor(Floor) && Z0002FloorBlock.is(State) && !State.getValue(Z0002FloorBlock.Cut)) {
                World.setBlock(Floor, State.setValue(Z0002FloorBlock.Cut, true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private void fade(ServerLevel World, Vec3 Tip) {
        for (int Index = 0; Index < ArcPoints; Index += 3) {
            Vec3 Point = arcPoint(Tip, -1.0 + 2.0 * Index / (ArcPoints - 1));
            World.sendParticles(Z0002Registry.Spark.get(), Point.x, Point.y, Point.z, 3, 0.1, 0.1, 0.1, 0.05);
        }
    }

    private void trail(Vec3 From, Vec3 To) {
        for (int Index = 0; Index < ArcPoints; Index++) {
            if (random.nextFloat() < 0.45f) {
                continue;
            }
            double Along = -1.0 + 2.0 * Index / (ArcPoints - 1);
            double Mix = random.nextDouble();
            Vec3 Point = arcPoint(From.lerp(To, Mix), Along);
            level().addParticle(Z0002Registry.Spark.get(), Point.x, Point.y, Point.z, (random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.5) * 0.04);
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
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 96.0 * 96.0;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(ArcRadius + 1.0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
    }
}
