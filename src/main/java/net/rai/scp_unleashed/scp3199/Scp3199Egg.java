package net.rai.scp_unleashed.scp3199;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.sulfur.SulfurTags;
import net.rai.scp_unleashed.sulfur.acid.AcidContact;

import java.util.List;

public class Scp3199Egg extends Entity {
    public static final float Width = 0.3f;
    public static final float Height = 0.375f;
    public static final double Gravity = 0.08;
    public static final double Drag = 0.98;
    public static final double WaterDrag = 0.8;
    public static final double WaterGravity = 0.02;
    public static final double GroundFriction = 0.5;
    public static final int HatchTicks = 1500;
    public static final int WaitTicks = 100;
    public static final int AcidCheck = 5;
    public static final int FloodCheck = 10;
    public static final int PulseMin = 32;
    public static final int PulseSpread = 17;
    public static final double PulseRange = 5.0;
    public static final float PulseVolume = 0.3f;
    public static final int SquishWait = 20;
    public static final double SquishMotion = 1.0e-4;
    public static final int SettleTicks = 60;
    public static final long Unlanded = Long.MIN_VALUE;
    private static final EntityDataAccessor<Long> LandedData = SynchedEntityData.defineId(Scp3199Egg.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> MotherData = SynchedEntityData.defineId(Scp3199Egg.class, EntityDataSerializers.INT);

    private int Hatch = HatchTicks;
    private boolean Drowned;
    private int NextPulse = PulseMin;
    private long LastSquish = Long.MIN_VALUE;

    public Scp3199Egg(EntityType<? extends Scp3199Egg> Type, Level World) {
        super(Type, World);
        blocksBuilding = false;
    }

    public static Scp3199Egg laid(ServerLevel World, Vec3 Where, Vec3 Motion, float Yaw, int Mother) {
        Scp3199Egg Egg = Scp3199Registry.EggType.get().create(World);
        if (Egg == null) {
            return null;
        }
        Egg.moveTo(Where.x, Where.y, Where.z, Yaw, 0.0f);
        Egg.setDeltaMovement(Motion);
        Egg.entityData.set(MotherData, Mother);
        World.addFreshEntity(Egg);
        return Egg;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(LandedData, Unlanded);
        Builder.define(MotherData, -1);
    }

    public long landedAt() {
        return entityData.get(LandedData);
    }

    public boolean landed() {
        return landedAt() != Unlanded;
    }

    public int mother() {
        return entityData.get(MotherData);
    }

    public int hatchLeft() {
        return Hatch;
    }

    public void setHatch(int Ticks) {
        Hatch = Math.max(0, Ticks);
    }

    public boolean drowned() {
        return Drowned;
    }

    public void settle(long Now) {
        entityData.set(LandedData, Now);
    }

    @Override
    protected double getDefaultGravity() {
        return Gravity;
    }

    @Override
    public void tick() {
        super.tick();
        fall();
        if (level() instanceof ServerLevel World) {
            serverTick(World);
        }
    }

    private void fall() {
        boolean Wet = isInWater();
        Vec3 Motion = getDeltaMovement().add(0.0, -(Wet ? WaterGravity : Gravity), 0.0);
        move(MoverType.SELF, Motion);
        Motion = getDeltaMovement().scale(Wet ? WaterDrag : Drag);
        if (onGround()) {
            Motion = new Vec3(Motion.x * GroundFriction, Math.max(0.0, Motion.y), Motion.z * GroundFriction);
        }
        setDeltaMovement(Motion);
    }

    private void serverTick(ServerLevel World) {
        if (!landed() && (onGround() || isInWater())) {
            settle(World.getGameTime());
        }
        if (landed() && World.getGameTime() - landedAt() == SettleTicks) {
            Scp3199Sounds.play(World, position(), List.of(Scp3199Registry.Squish), SoundSource.HOSTILE, 0.4f, 0.75f);
        }
        if (tickCount % AcidCheck == 0 && touchesAcid(World)) {
            dissolve(World);
            return;
        }
        if (tickCount % FloodCheck == 0) {
            Drowned = flooded(World);
        }
        if (--NextPulse <= 0) {
            NextPulse = PulseMin + World.getRandom().nextInt(PulseSpread);
            if (World.getNearestPlayer(getX(), getY(), getZ(), PulseRange, Found -> !Found.isSpectator()) != null) {
                Scp3199Sounds.play(World, position().add(0.0, 0.2, 0.0), List.of(Scp3199Registry.Pulse), SoundSource.HOSTILE, PulseVolume, 0.9f + World.getRandom().nextFloat() * 0.2f);
            }
        }
        squish(World);
        if (!landed() || Drowned) {
            return;
        }
        if (Hatch > 0) {
            Hatch--;
            return;
        }
        if (World.getGameTime() % WaitTicks == 0 && Scp3199Rules.hatchAllowed(World)) {
            hatch(World);
        }
    }

    private void squish(ServerLevel World) {
        if (World.getGameTime() - LastSquish < SquishWait) {
            return;
        }
        for (Entity Visitor : World.getEntities(this, getBoundingBox(), Found -> Found instanceof LivingEntity && !(Found instanceof Scp3199) && !Found.isSpectator())) {
            Vec3 Motion = Visitor.getDeltaMovement();
            double Dx = Visitor.getX() - Visitor.xo;
            double Dz = Visitor.getZ() - Visitor.zo;
            if (Motion.horizontalDistanceSqr() >= SquishMotion || Dx * Dx + Dz * Dz >= SquishMotion) {
                LastSquish = World.getGameTime();
                Scp3199Sounds.play(World, position(), List.of(Scp3199Registry.Squish), SoundSource.BLOCKS, 0.45f, 0.9f + World.getRandom().nextFloat() * 0.2f);
                return;
            }
        }
    }

    public boolean touchesAcid(Level World) {
        BlockPos Here = blockPosition();
        if (World.getFluidState(Here).is(SulfurTags.Acid)) {
            return true;
        }
        for (Direction Side : Direction.values()) {
            if (World.getFluidState(Here.relative(Side)).is(SulfurTags.Acid)) {
                return true;
            }
        }
        for (BlockPos Inside : BlockPos.betweenClosed(BlockPos.containing(getBoundingBox().minX, getBoundingBox().minY, getBoundingBox().minZ),
                BlockPos.containing(getBoundingBox().maxX, getBoundingBox().maxY, getBoundingBox().maxZ))) {
            if (World.getFluidState(Inside).is(SulfurTags.Acid)) {
                return true;
            }
        }
        return false;
    }

    public boolean flooded(Level World) {
        BlockPos Here = blockPosition();
        if (World.getFluidState(Here).is(FluidTags.WATER) || World.getFluidState(Here.above()).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (World.getFluidState(Here.relative(Side)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    public void dissolve(ServerLevel World) {
        Vec3 Where = position();
        World.sendParticles(AcidContact.Fizz, Where.x, Where.y + 0.2, Where.z, 12, 0.22, 0.12, 0.22, 0.0);
        World.sendParticles(ParticleTypes.SMOKE, Where.x, Where.y + 0.25, Where.z, 4, 0.18, 0.08, 0.18, 0.01);
        Scp3199Sounds.play(World, Where, Scp3199Registry.DissolveLayers, SoundSource.BLOCKS, 0.9f, 0.95f + World.getRandom().nextFloat() * 0.15f);
        discard();
    }

    public boolean hatch(ServerLevel World) {
        Scp3199 Young = Scp3199Registry.Scp3199Type.get().create(World);
        if (Young == null) {
            return false;
        }
        Young.moveTo(getX(), getY(), getZ(), World.getRandom().nextFloat() * 360.0f, 0.0f);
        Young.finalizeSpawn(World, World.getCurrentDifficultyAt(blockPosition()), MobSpawnType.BREEDING, null);
        Young.makeJuvenile();
        discard();
        World.addFreshEntity(Young);
        Scp3199Sounds.play(World, position(), Scp3199Registry.HatchLayers, SoundSource.HOSTILE, 0.9f, 1.2f);
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
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public void move(MoverType Kind, Vec3 Motion) {
        if (Kind == MoverType.PISTON || Kind == MoverType.SHULKER_BOX || Kind == MoverType.SHULKER) {
            return;
        }
        super.move(Kind, Motion);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity Other) {
        return Other instanceof Scp3199Egg && Other != this;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        Hatch = Tag.contains("Hatch") ? Math.max(0, Tag.getInt("Hatch")) : HatchTicks;
        entityData.set(LandedData, Tag.contains("Landed") ? Tag.getLong("Landed") : Unlanded);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putInt("Hatch", Hatch);
        Tag.putLong("Landed", landedAt());
    }
}
