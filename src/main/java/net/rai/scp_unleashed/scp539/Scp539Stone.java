package net.rai.scp_unleashed.scp539;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Scp539Stone extends ThrowableItemProjectile {
    public static final double Gravity = 0.024;
    public static final float ThrowSpeed = 1.35f;
    public static final float ThrowSpread = 0.4f;
    public static final double SkimSpeed = 0.4;
    public static final double SkimSlope = 0.75;
    public static final int MaxSkips = 4;
    public static final double SkipKeep = 0.75;
    public static final double SkipBounce = 0.35;
    public static final double SkipLift = 0.05;
    public static final int MaxFlight = 200;
    public static final float HitDamage = 1.0f;
    public static final double LandBack = 0.12;
    public static final double LandHop = 0.08;
    public static final int PickupDelay = 10;
    public static final double VoidMargin = 16.0;
    public static final float LandVolume = 0.7f;
    public static final float SkipVolume = 0.6f;

    private int Skips;
    private int Flight;
    private boolean Plan;
    private boolean Landed;
    private Vec3 Heading = Vec3.ZERO;

    public Scp539Stone(EntityType<? extends Scp539Stone> Type, Level World) {
        super(Type, World);
    }

    public Scp539Stone(Level World, LivingEntity Thrower) {
        super(Scp539Registry.StoneEntity.get(), Thrower, World);
    }

    public static Scp539Stone thrown(ServerLevel World, LivingEntity Thrower, ItemStack Stack) {
        return thrown(World, Thrower, Stack, Thrower.getXRot(), Thrower.getYRot(), ThrowSpeed, ThrowSpread);
    }

    public static Scp539Stone thrown(ServerLevel World, LivingEntity Thrower, ItemStack Stack, float Pitch, float Yaw, float Speed, float Spread) {
        Scp539Stone Stone = new Scp539Stone(World, Thrower);
        Stone.setItem(Stack.isEmpty() ? new ItemStack(Scp539Registry.Stone.get()) : Stack.copyWithCount(1));
        Stone.shootFromRotation(Thrower, Pitch, Yaw, 0.0f, Speed, Spread);
        Vec3 Look = Stone.getDeltaMovement();
        Stone.Heading = flat(Look);
        if (Thrower instanceof ServerPlayer User && Scp539Plan.claim(User, Scp539Compulsion.now(World))) {
            Stone.Plan = true;
        }
        World.addFreshEntity(Stone);
        Scp539Compulsion.thrown(World, Stone);
        return Stone;
    }

    public static Vec3 flat(Vec3 Direction) {
        Vec3 Flat = new Vec3(Direction.x, 0.0, Direction.z);
        return Flat.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 0.0, 1.0) : Flat.normalize();
    }

    @Override
    protected Item getDefaultItem() {
        return Scp539Registry.Stone.get();
    }

    @Override
    protected double getDefaultGravity() {
        return Gravity;
    }

    public int flight() {
        return Flight;
    }

    public int skips() {
        return Skips;
    }

    public boolean isPlan() {
        return Plan;
    }

    public void markPlan(boolean Value) {
        Plan = Value;
    }

    public Vec3 heading() {
        return Heading.lengthSqr() < 1.0e-6 ? flat(getDeltaMovement()) : Heading;
    }

    @Override
    public void tick() {
        Flight++;
        skim();
        super.tick();
        if (isRemoved() || !(level() instanceof ServerLevel World)) {
            return;
        }
        Scp539Compulsion.flight(World, this);
        if (Flight > MaxFlight) {
            land(World, position(), null, null);
        } else if (getY() < World.getMinBuildHeight() - VoidMargin) {
            rescue(World);
        }
    }

    private void skim() {
        Vec3 Motion = getDeltaMovement();
        if (Motion.y >= 0.0 || Skips >= MaxSkips || isInWater()) {
            return;
        }
        double Across = Math.sqrt(Motion.x * Motion.x + Motion.z * Motion.z);
        if (Across < SkimSpeed || -Motion.y > Across * SkimSlope) {
            return;
        }
        Vec3 From = position();
        BlockHitResult Hit = level().clip(new ClipContext(From, From.add(Motion), ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, this));
        if (Hit.getType() != HitResult.Type.BLOCK || Hit.getDirection() != Direction.UP || !level().getFluidState(Hit.getBlockPos()).is(FluidTags.WATER)
                || !level().getBlockState(Hit.getBlockPos()).getCollisionShape(level(), Hit.getBlockPos()).isEmpty()) {
            return;
        }
        Vec3 Surface = Hit.getLocation();
        Skips++;
        setPos(Surface.x, Surface.y + 0.02, Surface.z);
        setDeltaMovement(Motion.x * SkipKeep, -Motion.y * SkipBounce + SkipLift, Motion.z * SkipKeep);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.SPLASH, Surface.x, Surface.y + 0.05, Surface.z, 14, 0.18, 0.02, 0.18, 0.12);
            World.sendParticles(ParticleTypes.BUBBLE, Surface.x, Surface.y - 0.1, Surface.z, 4, 0.12, 0.02, 0.12, 0.02);
            Scp539Registry.playLayers(World, Surface, Scp539Registry.SkipLayers, SoundSource.NEUTRAL, SkipVolume, 0.95f + Skips * 0.06f + random.nextFloat() * 0.08f);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        super.onHitEntity(Result);
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Entity Target = Result.getEntity();
        Target.hurt(damageSources().thrown(this, getOwner()), HitDamage);
        Vec3 Back = getDeltaMovement().lengthSqr() > 1.0e-6 ? getDeltaMovement().normalize().scale(-0.3) : Vec3.ZERO;
        land(World, position().add(Back), null, null);
    }

    @Override
    protected void onHitBlock(BlockHitResult Result) {
        super.onHitBlock(Result);
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Direction Face = Result.getDirection();
        Vec3 Out = new Vec3(Face.getStepX(), Face.getStepY(), Face.getStepZ()).scale(LandBack);
        land(World, Result.getLocation().add(Out), Face, Result.getBlockPos());
    }

    public void land(ServerLevel World, Vec3 Where, @Nullable Direction Face, @Nullable BlockPos Struck) {
        if (Landed) {
            return;
        }
        Landed = true;
        ItemStack Carried = getItem().isEmpty() ? new ItemStack(Scp539Registry.Stone.get()) : getItem().copy();
        ItemEntity Dropped = new ItemEntity(World, Where.x, Where.y, Where.z, Carried, 0.0, LandHop, 0.0);
        Dropped.setPickUpDelay(PickupDelay);
        Dropped.setUnlimitedLifetime();
        World.addFreshEntity(Dropped);
        Scp539Registry.playLayers(World, Where, Scp539Registry.LandLayers, SoundSource.NEUTRAL, LandVolume, 0.95f + random.nextFloat() * 0.1f);
        Scp539Compulsion.landed(World, this, Dropped);
        if (Plan) {
            Scp539Plan.launch(World, this, Where, Face, Struck);
        }
        discard();
    }

    private void rescue(ServerLevel World) {
        Landed = true;
        ItemStack Carried = getItem().isEmpty() ? new ItemStack(Scp539Registry.Stone.get()) : getItem().copy();
        Entity Owner = getOwner();
        if (Owner instanceof Player Holder && Holder.isAlive() && Holder.level() == World) {
            if (!Holder.getInventory().add(Carried)) {
                Holder.drop(Carried, false);
            }
        } else {
            BlockPos Spawn = World.getSharedSpawnPos();
            ItemEntity Dropped = new ItemEntity(World, Spawn.getX() + 0.5, Spawn.getY() + 1.0, Spawn.getZ() + 0.5, Carried);
            Dropped.setUnlimitedLifetime();
            World.addFreshEntity(Dropped);
        }
        Scp539Compulsion.forget(World, this);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putInt("Skips", Skips);
        Saved.putInt("Flight", Flight);
        Saved.putBoolean("Plan", Plan);
        Saved.putDouble("HeadingX", Heading.x);
        Saved.putDouble("HeadingZ", Heading.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Skips = Saved.getInt("Skips");
        Flight = Saved.getInt("Flight");
        Plan = Saved.getBoolean("Plan");
        Heading = new Vec3(Saved.getDouble("HeadingX"), 0.0, Saved.getDouble("HeadingZ"));
    }
}
