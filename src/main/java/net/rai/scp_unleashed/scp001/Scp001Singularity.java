package net.rai.scp_unleashed.scp001;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Scp001Singularity extends Projectile {
    public static final float Width = 0.4f;
    public static final double Speed = 0.9;
    public static final double Range = 40.0;
    public static final int FlightLimit = 100;
    public static final int FormTicks = 6;
    public static final int CollapseTicks = 6;
    public static final double HitRadius = 0.3;
    public static final double OrbitRadius = 1.9;
    public static final double OrbitSpin = 0.24;
    public static final double OrbitHeight = 2.4;
    public static final double OrbitBob = 0.35;
    public static final float SwallowFloor = 1000.0f;
    public static final int CraterRadius = 1;
    public static final byte StateForming = 0;
    public static final byte StateOrbiting = 1;
    public static final byte StateFlying = 2;
    public static final byte StateCollapsing = 3;
    public static final Vector3f Gold = new Vector3f(1.0f, 0.76f, 0.24f);
    public static final Vector3f Dark = new Vector3f(0.18f, 0.08f, 0.26f);

    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(Scp001Singularity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> SlotData = SynchedEntityData.defineId(Scp001Singularity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> CountData = SynchedEntityData.defineId(Scp001Singularity.class, EntityDataSerializers.BYTE);

    private int StateAge;
    private double Travelled;
    private Vec3 Heading = Vec3.ZERO;
    private Vec3 Seed = Vec3.ZERO;

    public Scp001Singularity(EntityType<? extends Scp001Singularity> Type, Level World) {
        super(Type, World);
        noPhysics = true;
    }

    public static Scp001Singularity form(ServerLevel World, Scp001Prototype Owner, int Slot, int Count) {
        Scp001Singularity Orb = new Scp001Singularity(Scp001Registry.Orb.get(), World);
        Orb.setOwner(Owner);
        Orb.entityData.set(SlotData, (byte) Slot);
        Orb.entityData.set(CountData, (byte) Count);
        Orb.Seed = Owner.orbPosition();
        Orb.setPos(Orb.Seed);
        World.addFreshEntity(Orb);
        World.playSound(null, Orb.getX(), Orb.getY(), Orb.getZ(), Scp001Registry.SingularityForm.get(), SoundSource.HOSTILE, 1.1f, 0.85f + Slot * 0.06f);
        World.sendParticles(ParticleTypes.REVERSE_PORTAL, Orb.getX(), Orb.getY(), Orb.getZ(), 8, 0.1, 0.1, 0.1, 0.04);
        return Orb;
    }

    public static Scp001Singularity fire(ServerLevel World, Vec3 From, Vec3 Aim, @Nullable Entity Owner) {
        Scp001Singularity Orb = new Scp001Singularity(Scp001Registry.Orb.get(), World);
        Orb.setOwner(Owner);
        Orb.Seed = From;
        Orb.setPos(From);
        World.addFreshEntity(Orb);
        Orb.launch(Aim);
        return Orb;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(StateData, StateForming);
        Builder.define(SlotData, (byte) 0);
        Builder.define(CountData, (byte) 1);
    }

    public byte state() {
        return entityData.get(StateData);
    }

    public int slot() {
        return entityData.get(SlotData);
    }

    public int count() {
        return Math.max(1, entityData.get(CountData));
    }

    public int stateAge() {
        return StateAge;
    }

    public boolean flying() {
        return state() == StateFlying;
    }

    public boolean waiting() {
        byte Current = state();
        return Current == StateForming || Current == StateOrbiting;
    }

    public float size(float Partial) {
        float Age = StateAge + Partial;
        return switch (state()) {
            case StateForming -> Mth.clamp(Age / FormTicks, 0.1f, 1.0f);
            case StateCollapsing -> Mth.clamp(1.0f - Age / CollapseTicks, 0.0f, 1.0f);
            default -> 1.0f;
        };
    }

    private void setState(byte Next) {
        entityData.set(StateData, Next);
        StateAge = 0;
    }

    @Override
    public void tick() {
        super.tick();
        StateAge++;
        if (level().isClientSide) {
            swirl();
            return;
        }
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        switch (state()) {
            case StateForming, StateOrbiting -> hold(World);
            case StateFlying -> fly(World);
            default -> {
                if (StateAge >= CollapseTicks) {
                    discard();
                }
            }
        }
    }

    private void hold(ServerLevel World) {
        if (!(getOwner() instanceof Scp001Prototype Owner) || !Owner.isAlive() || Owner.isRemoved() || !Owner.casting()) {
            collapse(World, false);
            return;
        }
        Vec3 Orbit = Owner.orbitPoint(slot(), count(), Owner.tickCount);
        if (state() == StateForming) {
            float Share = Mth.clamp((float) StateAge / FormTicks, 0.0f, 1.0f);
            Share = Share * Share * (3.0f - 2.0f * Share);
            setPos(Seed.lerp(Orbit, Share));
            if (StateAge >= FormTicks) {
                setState(StateOrbiting);
            }
            return;
        }
        setPos(Orbit);
    }

    public void launch(Vec3 Aim) {
        if (!(level() instanceof ServerLevel World) || !waiting()) {
            return;
        }
        Vec3 Toward = Aim.subtract(position());
        if (Toward.lengthSqr() < 1.0E-6) {
            Toward = new Vec3(0.0, 0.0, 1.0);
        }
        Heading = Toward.normalize();
        Travelled = 0.0;
        setDeltaMovement(Heading.scale(Speed));
        setState(StateFlying);
        World.playSound(null, getX(), getY(), getZ(), Scp001Registry.SingularityLaunch.get(), SoundSource.HOSTILE, 1.4f, 0.9f + random.nextFloat() * 0.2f);
    }

    public void collapse(ServerLevel World, boolean Loud) {
        if (state() == StateCollapsing) {
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        setState(StateCollapsing);
        playLayers(World, position(), Loud ? Scp001Registry.SingularityImplodeLayers : Scp001Registry.SingularityFizzleLayers, Loud ? 2.2f : 0.8f);
        if (!Loud) {
            World.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 10, 0.15, 0.15, 0.15, 0.03);
        }
    }

    private void fly(ServerLevel World) {
        Vec3 Start = position();
        Vec3 End = Start.add(Heading.scale(Speed));
        BlockHitResult Wall = World.clip(new ClipContext(Start, End, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double Limit = Wall.getType() == HitResult.Type.MISS ? Speed : Start.distanceTo(Wall.getLocation());
        AABB Sweep = new AABB(Start, End).inflate(HitRadius + 1.0);
        LivingEntity Victim = null;
        double Nearest = Limit;
        Vec3 Contact = End;
        for (Entity Other : World.getEntities(this, Sweep, Candidate -> Candidate.isAlive() && !(Candidate instanceof Scp001Singularity))) {
            Optional<Vec3> Touch = Other.getBoundingBox().inflate(HitRadius).clip(Start, End);
            if (Touch.isEmpty() && !Other.getBoundingBox().inflate(HitRadius).contains(Start)) {
                continue;
            }
            Vec3 Point = Touch.orElse(Start);
            double Along = Start.distanceTo(Point);
            if (Other instanceof ItemEntity || Other instanceof ExperienceOrb) {
                if (Along <= Limit) {
                    Other.discard();
                }
                continue;
            }
            if (Other instanceof LivingEntity Living && blocks(Living) && Along <= Nearest) {
                Nearest = Along;
                Victim = Living;
                Contact = Point;
            }
        }
        if (Victim != null) {
            setPos(Contact);
            swallow(World, Victim, this, getOwner());
            implode(World, Contact, false);
            collapse(World, true);
            return;
        }
        if (Wall.getType() != HitResult.Type.MISS) {
            setPos(Wall.getLocation());
            consume(World, Wall.getBlockPos(), this, getOwner());
            implode(World, Vec3.atCenterOf(Wall.getBlockPos()), true);
            collapse(World, true);
            return;
        }
        setPos(End);
        setDeltaMovement(Heading.scale(Speed));
        Travelled += Speed;
        if (Travelled >= Range || StateAge >= FlightLimit) {
            collapse(World, false);
        }
    }

    public static boolean blocks(LivingEntity Living) {
        return Living.isAlive() && !(Living instanceof Scp001Prototype) && !Living.isSpectator() && !Living.isInvulnerable();
    }

    public static boolean swallow(ServerLevel World, LivingEntity Victim, @Nullable Entity Direct, @Nullable Entity Owner) {
        if (!blocks(Victim)) {
            return false;
        }
        DamageSource Source = World.damageSources().source(Scp001Registry.Singularity, Direct, Owner);
        float Amount = Math.max(SwallowFloor, Victim.getMaxHealth() * 4.0f + Victim.getAbsorptionAmount());
        return Victim.hurt(Source, Amount);
    }

    public static int consume(ServerLevel World, BlockPos Core, @Nullable Entity Direct, @Nullable Entity Owner) {
        AABB Area = new AABB(Core).inflate(CraterRadius);
        for (Entity Other : World.getEntities((Entity) null, Area, Candidate -> Candidate.isAlive() && !(Candidate instanceof Scp001Singularity))) {
            if (Other instanceof ItemEntity || Other instanceof ExperienceOrb) {
                Other.discard();
            } else if (Other instanceof LivingEntity Living) {
                swallow(World, Living, Direct, Owner);
            }
        }
        if (!EventHooks.canEntityGrief(World, Owner != null ? Owner : Direct)) {
            return 0;
        }
        List<BlockPos> Taken = new ArrayList<>();
        for (BlockPos Pos : BlockPos.betweenClosed(Core.offset(-CraterRadius, -CraterRadius, -CraterRadius), Core.offset(CraterRadius, CraterRadius, CraterRadius))) {
            BlockState State = World.getBlockState(Pos);
            if (State.isAir() || !edible(World, Pos, State)) {
                continue;
            }
            BlockPos Kept = Pos.immutable();
            World.removeBlockEntity(Kept);
            World.setBlock(Kept, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            Taken.add(Kept);
        }
        for (BlockPos Pos : Taken) {
            World.updateNeighborsAt(Pos, Blocks.AIR);
            Blocks.AIR.defaultBlockState().updateNeighbourShapes(World, Pos, Block.UPDATE_ALL);
        }
        return Taken.size();
    }

    public static boolean edible(ServerLevel World, BlockPos Pos, BlockState State) {
        return State.getDestroySpeed(World, Pos) >= 0.0f && !State.is(Scp001Registry.SingularityProof);
    }

    public static void implode(ServerLevel World, Vec3 Centre, boolean Crater) {
        World.sendParticles(ParticleTypes.FLASH, Centre.x, Centre.y, Centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(new DustParticleOptions(Gold, 1.4f), Centre.x, Centre.y, Centre.z, Crater ? 40 : 18, 0.7, 0.7, 0.7, 0.0);
        World.sendParticles(ParticleTypes.REVERSE_PORTAL, Centre.x, Centre.y, Centre.z, Crater ? 30 : 14, 0.2, 0.2, 0.2, 0.25);
        int Spokes = Crater ? 28 : 16;
        for (int Index = 0; Index < Spokes; Index++) {
            double Angle = Index * Mth.TWO_PI / Spokes;
            World.sendParticles(ParticleTypes.END_ROD, Centre.x, Centre.y, Centre.z, 0, Math.cos(Angle), 0.05, Math.sin(Angle), Crater ? 0.45 : 0.3);
            World.sendParticles(new DustParticleOptions(Dark, 1.8f), Centre.x + Math.cos(Angle) * 1.4, Centre.y, Centre.z + Math.sin(Angle) * 1.4, 1, 0.05, 0.05, 0.05, 0.0);
        }
        World.sendParticles(ParticleTypes.PORTAL, Centre.x, Centre.y, Centre.z, Crater ? 50 : 24, 1.2, 1.2, 1.2, 0.6);
    }

    private static void playLayers(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        float Pitch = 0.9f + World.getRandom().nextFloat() * 0.2f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    private void swirl() {
        byte Current = state();
        if (Current == StateCollapsing) {
            return;
        }
        for (int Index = 0; Index < 2; Index++) {
            double Angle = (tickCount * 0.6 + Index * Math.PI) + random.nextDouble() * 0.6;
            double Radius = 0.45 + random.nextDouble() * 0.3;
            double Lift = (random.nextDouble() - 0.5) * 0.25;
            double Ox = Math.cos(Angle) * Radius;
            double Oz = Math.sin(Angle) * Radius;
            level().addParticle(new DustParticleOptions(Gold, 0.45f), getX() + Ox, getY() + Lift, getZ() + Oz, -Oz * 0.6 - Ox * 0.4, 0.0, Ox * 0.6 - Oz * 0.4);
        }
        if (random.nextInt(2) == 0) {
            double Ox = (random.nextDouble() - 0.5) * 1.6;
            double Oy = (random.nextDouble() - 0.5) * 1.6;
            double Oz = (random.nextDouble() - 0.5) * 1.6;
            level().addParticle(ParticleTypes.PORTAL, getX(), getY(), getZ(), Ox, Oy, Oz);
        }
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
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 96.0 * 96.0;
    }
}
