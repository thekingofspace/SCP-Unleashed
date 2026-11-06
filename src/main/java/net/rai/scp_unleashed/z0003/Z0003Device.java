package net.rai.scp_unleashed.z0003;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.z0003.client.Z0003FieldEffects;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class Z0003Device extends Projectile {
    public static final float Width = 0.5f;
    public static final float Height = 0.5f;
    public static final byte Idle = 0;
    public static final byte Armed = 1;
    public static final byte Active = 2;
    public static final byte Returning = 3;
    public static final double ExpandSpeed = 0.25;
    public static final int HoldTicks = 40;
    public static final int FadeTicks = 20;
    public static final double GravityPull = 0.09;
    public static final double AirDrag = 0.96;
    public static final double WaterDrag = 0.8;
    public static final double GroundFriction = 0.5;
    public static final double BounceKeep = 0.18;
    public static final double BounceCarry = 0.6;
    public static final double BounceSpeed = 0.18;
    public static final double WallScrape = 0.5;
    public static final double RestSpeed = 0.01;
    public static final int MaxBounces = 1;
    public static final double TetherLength = 48.0;
    public static final double TautPull = 0.6;
    public static final double YankSearch = TetherLength + 8.0;
    public static final double HolderSearch = TetherLength * 2.0 + 32.0;
    public static final double ReturnSpeed = 1.2;
    public static final double ReturnReach = 1.6;
    public static final float MinThrowSpeed = 1.2f;
    public static final float MaxThrowSpeed = 2.1f;
    public static final float ThrowLoft = 12.0f;
    public static final float ThrowSpread = 0.5f;
    public static final float TumbleSpeed = 22.0f;
    public static final float HoverSpeed = 3.0f;
    public static final double HearMargin = 24.0;
    public static final int SwellEvery = 6;

    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(Z0003Device.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> RadiusData = SynchedEntityData.defineId(Z0003Device.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> FieldStartData = SynchedEntityData.defineId(Z0003Device.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> HolderData = SynchedEntityData.defineId(Z0003Device.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DueData = SynchedEntityData.defineId(Z0003Device.class, EntityDataSerializers.LONG);

    private long Due;
    @Nullable
    private UUID TetherId;
    private boolean LeadSpent;
    private boolean CreativeCopy;
    private int Bounces;
    private ItemStack Kept = ItemStack.EMPTY;
    public float Spin;
    public float SpinO;

    public Z0003Device(EntityType<? extends Z0003Device> Type, Level World) {
        super(Type, World);
    }

    public static float throwSpeed(float Charge) {
        return Mth.lerp(Mth.clamp(Charge, 0.0f, 1.0f), MinThrowSpeed, MaxThrowSpeed);
    }

    public static Z0003Device thrown(ServerLevel World, LivingEntity Thrower, ItemStack Stack, float Charge) {
        Z0003Device Made = new Z0003Device(Z0003Registry.DeviceType.get(), World);
        Made.setOwner(Thrower);
        Made.keep(Stack);
        Made.CreativeCopy = Thrower instanceof Player User && User.getAbilities().instabuild;
        Made.setPos(Thrower.getX(), Thrower.getEyeY() - 0.1 - Height * 0.5, Thrower.getZ());
        Made.shootFromRotation(Thrower, Mth.clamp(Thrower.getXRot() - ThrowLoft, -90.0f, 90.0f), Thrower.getYRot(), 0.0f, throwSpeed(Charge), ThrowSpread);
        Z0003Arming Setting = Stack.get(Z0003Registry.Arming.get());
        if (Setting != null) {
            Made.arm(Setting);
        }
        World.addFreshEntity(Made);
        return Made;
    }

    public static Z0003Device released(ServerLevel World, Vec3 Spot, Vec3 Motion, @Nullable Entity Owner, ItemStack Stack) {
        Z0003Device Made = loose(World, Spot, Motion, Owner, Stack);
        World.addFreshEntity(Made);
        return Made;
    }

    public static Z0003Device loose(Level World, Vec3 Spot, Vec3 Motion, @Nullable Entity Owner, ItemStack Stack) {
        Z0003Device Made = new Z0003Device(Z0003Registry.DeviceType.get(), World);
        if (Owner != null) {
            Made.setOwner(Owner);
        }
        Made.keep(Stack);
        Made.setPos(Spot.x, Spot.y, Spot.z);
        Made.setDeltaMovement(Motion);
        Z0003Arming Setting = Stack.get(Z0003Registry.Arming.get());
        if (Setting != null) {
            Made.arm(Setting);
        }
        return Made;
    }

    public int bounces() {
        return Bounces;
    }

    public boolean isCreativeCopy() {
        return CreativeCopy;
    }

    private void keep(ItemStack Stack) {
        Kept = Stack.copyWithCount(1);
        Kept.remove(Z0003Registry.Arming.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(StateData, Idle);
        Builder.define(RadiusData, 0.0f);
        Builder.define(FieldStartData, 0L);
        Builder.define(HolderData, -1);
        Builder.define(DueData, 0L);
    }

    public byte state() {
        return entityData.get(StateData);
    }

    private void setState(byte Value) {
        entityData.set(StateData, Value);
    }

    public float radius() {
        return entityData.get(RadiusData);
    }

    public long fieldStart() {
        return entityData.get(FieldStartData);
    }

    public boolean isActive() {
        return state() == Active;
    }

    public boolean isArmed() {
        return state() == Armed;
    }

    public long due() {
        return level().isClientSide ? entityData.get(DueData) : Due;
    }

    private void setDue(long Value) {
        Due = Value;
        entityData.set(DueData, Value);
    }

    public ItemStack shown() {
        return Kept.isEmpty() ? new ItemStack(Z0003Registry.Device.get()) : Kept;
    }

    public static int growTicks(float Radius) {
        return Mth.ceil(Radius / ExpandSpeed);
    }

    public static float reach(double Elapsed, float Radius) {
        return (float) Math.min(Radius, Math.max(0.0, Elapsed) * ExpandSpeed);
    }

    public static int lifeTicks(float Radius) {
        return growTicks(Radius) + HoldTicks + FadeTicks;
    }

    public float fieldReach(float Partial) {
        if (!isActive()) {
            return 0.0f;
        }
        return reach(level().getGameTime() - fieldStart() + Partial, radius());
    }

    public float fieldFade(float Partial) {
        if (!isActive()) {
            return 0.0f;
        }
        double Elapsed = level().getGameTime() - fieldStart() + Partial;
        double Fading = Elapsed - growTicks(radius()) - HoldTicks;
        return Fading <= 0.0 ? 1.0f : (float) Mth.clamp(1.0 - Fading / FadeTicks, 0.0, 1.0);
    }

    public Vec3 fieldCentre() {
        return position().add(0.0, Height * 0.5, 0.0);
    }

    public boolean arm(Z0003Arming Setting) {
        byte Current = state();
        if (Current == Active || Current == Returning) {
            return false;
        }
        entityData.set(RadiusData, Setting.Radius());
        setDue(Setting.Due());
        setState(Armed);
        return true;
    }

    public void disarm() {
        if (state() == Armed) {
            setState(Idle);
        }
        setDue(0L);
    }

    @Nullable
    public Z0003Arming arming() {
        return state() == Armed ? new Z0003Arming(radius(), Due) : null;
    }

    public void activate() {
        if (!(level() instanceof ServerLevel World) || state() == Active) {
            return;
        }
        if (radius() <= 0.0f) {
            entityData.set(RadiusData, Z0003Phrase.MinRadius);
        }
        setDue(0L);
        entityData.set(FieldStartData, World.getGameTime());
        setState(Active);
        setDeltaMovement(Vec3.ZERO);
        hasImpulse = true;
        Z0003Registry.play(World, fieldCentre(), Z0003Registry.ActivateLayers, SoundSource.NEUTRAL, 1.0f, 1.0f);
        Z0003Registry.playAround(World, fieldCentre(), 0.0, hearing(radius()), Z0003Registry.SurgeLayers, SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    public static double hearing(float Radius) {
        return Radius + HearMargin;
    }

    private void fieldSounds(ServerLevel World, long Elapsed, float Radius) {
        int Grow = growTicks(Radius);
        double Range = hearing(Radius);
        Vec3 Centre = fieldCentre();
        if (Elapsed > 0 && Elapsed < Grow && Elapsed % SwellEvery == 0) {
            float Share = Elapsed / (float) Grow;
            Z0003Registry.playAround(World, Centre, reach(Elapsed, Radius), Range, Z0003Registry.SwellLayers, SoundSource.NEUTRAL, 0.55f + 0.45f * Share,
                    0.7f + 0.6f * Share);
        } else if (Elapsed == Grow) {
            Z0003Registry.playAround(World, Centre, Radius, Range, Z0003Registry.HoldLayers, SoundSource.NEUTRAL, 0.7f, 1.0f);
        } else if (Elapsed == Grow + HoldTicks) {
            Z0003Registry.stopAround(World, Centre, Range + HearMargin, Z0003Registry.HoldLayers, SoundSource.NEUTRAL);
            Z0003Registry.playAround(World, Centre, Radius, Range, Z0003Registry.ReleaseLayers, SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
    }

    private void settle(ServerLevel World) {
        setState(Idle);
        setDue(0L);
        Z0003Registry.play(World, fieldCentre(), Z0003Registry.SettleLayers, SoundSource.NEUTRAL, 0.8f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        SpinO = Spin;
        byte Current = state();
        if (level().isClientSide) {
            visuals(Current);
        }
        if (level() instanceof ServerLevel World) {
            tickTether(World);
            Current = state();
        }
        if (Current == Returning) {
            noPhysics = true;
            if (level() instanceof ServerLevel World) {
                returning(World);
            } else {
                setPos(position().add(getDeltaMovement()));
            }
            return;
        }
        noPhysics = false;
        if (Current == Active) {
            setDeltaMovement(Vec3.ZERO);
            if (level() instanceof ServerLevel World) {
                field(World);
            }
            return;
        }
        physics();
        if (Current == Armed && level() instanceof ServerLevel World && World.getGameTime() >= Due) {
            activate();
        }
    }

    private void physics() {
        Vec3 Before = getDeltaMovement();
        if (!isNoGravity()) {
            Before = Before.add(0.0, -GravityPull, 0.0);
        }
        if (onGround() && Before.horizontalDistanceSqr() < RestSpeed * RestSpeed && Before.y <= 0.0) {
            setDeltaMovement(0.0, Before.y, 0.0);
            move(MoverType.SELF, new Vec3(0.0, Before.y, 0.0));
            setDeltaMovement(Vec3.ZERO);
            taut();
            return;
        }
        setDeltaMovement(Before);
        move(MoverType.SELF, Before);
        Vec3 After = getDeltaMovement();
        double X = After.x;
        double Y = After.y;
        double Z = After.z;
        boolean Rang = false;
        if (horizontalCollision) {
            if (X == 0.0 && Before.x != 0.0) {
                Z *= WallScrape;
            }
            if (Z == 0.0 && Before.z != 0.0) {
                X *= WallScrape;
            }
            Rang |= Before.horizontalDistance() > BounceSpeed;
        }
        if (verticalCollision && Before.y < 0.0) {
            Y = 0.0;
            if (Before.y < -BounceSpeed) {
                Rang = true;
                if (Bounces < MaxBounces) {
                    Bounces++;
                    Y = -Before.y * BounceKeep;
                    X *= BounceCarry;
                    Z *= BounceCarry;
                }
            }
        } else if (verticalCollision && Before.y > 0.0) {
            Y = 0.0;
        }
        if (onGround() && Y <= 0.0) {
            X *= GroundFriction;
            Z *= GroundFriction;
        } else if (isInWater()) {
            X *= WaterDrag;
            Y *= WaterDrag;
            Z *= WaterDrag;
        } else {
            X *= AirDrag;
            Y *= AirDrag;
            Z *= AirDrag;
        }
        setDeltaMovement(X, Y, Z);
        taut();
        if (Rang && level() instanceof ServerLevel World) {
            float Pitch = 1.0f + World.random.nextFloat() * 0.2f;
            Z0003Registry.play(World, position(), Z0003Registry.BounceLayers, SoundSource.NEUTRAL, (float) Mth.clamp(Before.length(), 0.2, 1.0), Pitch);
        }
    }

    public static Vec3 anchorOf(Entity Holder) {
        return Holder.position().add(0.0, Holder.getBbHeight() * 0.5, 0.0);
    }

    private void taut() {
        Entity Holder = tetherHolder();
        if (Holder == null || Holder.level() != level()) {
            return;
        }
        Vec3 Offset = fieldCentre().subtract(anchorOf(Holder));
        double Distance = Offset.length();
        if (Distance <= TetherLength || Distance < 1.0E-6) {
            return;
        }
        Vec3 Out = Offset.scale(1.0 / Distance);
        move(MoverType.SELF, Out.scale(-Math.min(Distance - TetherLength, TautPull)));
        Vec3 Motion = getDeltaMovement();
        double Outward = Motion.dot(Out);
        if (Outward > 0.0) {
            setDeltaMovement(Motion.subtract(Out.scale(Outward)));
        }
    }

    private void field(ServerLevel World) {
        long Elapsed = World.getGameTime() - fieldStart();
        float Radius = radius();
        fieldSounds(World, Elapsed, Radius);
        if (Elapsed < growTicks(Radius) + HoldTicks) {
            petrifyWithin(World, reach(Elapsed + 1, Radius));
        }
        if (Elapsed >= lifeTicks(Radius)) {
            settle(World);
        }
    }

    public List<LivingEntity> petrifyWithin(ServerLevel World, double Reach) {
        Vec3 Centre = fieldCentre();
        AABB Box = new AABB(Centre, Centre).inflate(Reach);
        List<LivingEntity> Caught = World.getEntitiesOfClass(LivingEntity.class, Box, Candidate -> Z0003Petrify.canPetrify(Candidate)
                && distanceTo(Centre, Candidate.getBoundingBox()) <= Reach);
        for (LivingEntity Target : Caught) {
            Z0003Petrify.petrify(Target);
        }
        return Caught;
    }

    public static double distanceTo(Vec3 Point, AABB Box) {
        double Dx = Math.max(Math.max(Box.minX - Point.x, 0.0), Point.x - Box.maxX);
        double Dy = Math.max(Math.max(Box.minY - Point.y, 0.0), Point.y - Box.maxY);
        double Dz = Math.max(Math.max(Box.minZ - Point.z, 0.0), Point.z - Box.maxZ);
        return Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz);
    }

    private void visuals(byte Current) {
        if (Current == Active) {
            Spin += HoverSpeed;
        } else if (!onGround()) {
            Spin += TumbleSpeed;
        }
        Z0003FieldEffects.tick(this, Current);
    }

    public void tether(Player Holder, boolean Spent) {
        TetherId = Holder.getUUID();
        LeadSpent = Spent;
        entityData.set(HolderData, Holder.getId());
    }

    public boolean isTethered() {
        return TetherId != null;
    }

    public boolean isTetheredTo(Player Holder) {
        return TetherId != null && TetherId.equals(Holder.getUUID());
    }

    @Nullable
    public Entity tetherHolder() {
        int Id = entityData.get(HolderData);
        return Id < 0 ? null : level().getEntity(Id);
    }

    private void tickTether(ServerLevel World) {
        if (TetherId == null) {
            return;
        }
        Player Holder = World.getPlayerByUUID(TetherId);
        if (Holder == null || !Holder.isAlive()) {
            if (entityData.get(HolderData) != -1) {
                entityData.set(HolderData, -1);
            }
            if (World.getServer().getPlayerList().getPlayer(TetherId) == null) {
                detach(World, null);
            }
            return;
        }
        if (entityData.get(HolderData) != Holder.getId()) {
            entityData.set(HolderData, Holder.getId());
        }
    }

    public void detach(ServerLevel World, @Nullable Player Receiver) {
        if (TetherId == null) {
            return;
        }
        TetherId = null;
        entityData.set(HolderData, -1);
        if (LeadSpent) {
            ItemStack Lead = new ItemStack(Items.LEAD);
            if (Receiver != null) {
                give(Receiver, Lead);
            } else {
                spawnAtLocation(Lead);
            }
        }
        LeadSpent = false;
        if (state() == Returning) {
            noPhysics = false;
            setState(Idle);
        }
    }

    public boolean cut(Player Holder) {
        if (!isTetheredTo(Holder) || !(level() instanceof ServerLevel World)) {
            return false;
        }
        detach(World, Holder.getAbilities().instabuild ? null : Holder);
        Z0003Registry.play(World, position(), Z0003Registry.SnapLayers, SoundSource.NEUTRAL, 0.9f, 1.0f);
        Z0003Registry.play(World, Holder.position(), Z0003Registry.SnapLayers, SoundSource.PLAYERS, 0.7f, 1.15f);
        return true;
    }

    public static int cutAll(Player Holder) {
        if (!(Holder.level() instanceof ServerLevel World)) {
            return 0;
        }
        int Cut = 0;
        for (Z0003Device Found : tetheredTo(World, Holder)) {
            if (Found.cut(Holder)) {
                Cut++;
            }
        }
        return Cut;
    }

    public static int releaseAll(Player Holder) {
        if (!(Holder.level() instanceof ServerLevel World)) {
            return 0;
        }
        List<Z0003Device> Found = tetheredTo(World, Holder);
        for (Z0003Device Each : Found) {
            Each.detach(World, Holder);
        }
        return Found.size();
    }

    public static List<Z0003Device> tetheredTo(ServerLevel World, Player Holder) {
        return World.getEntitiesOfClass(Z0003Device.class, Holder.getBoundingBox().inflate(HolderSearch), Candidate -> Candidate.isTetheredTo(Holder));
    }

    private void untether(Player Receiver) {
        TetherId = null;
        entityData.set(HolderData, -1);
        if (LeadSpent && !Receiver.getAbilities().instabuild) {
            give(Receiver, new ItemStack(Items.LEAD));
        }
        LeadSpent = false;
    }

    public boolean yank(Player Holder) {
        if (!isTetheredTo(Holder) || !(level() instanceof ServerLevel World)) {
            return false;
        }
        if (state() == Active) {
            Z0003Registry.stopAround(World, fieldCentre(), hearing(radius()) + HearMargin, Z0003Registry.HoldLayers, SoundSource.NEUTRAL);
        }
        setDue(0L);
        setState(Returning);
        noPhysics = true;
        Z0003Registry.play(World, Holder.position(), Z0003Registry.YankLayers, SoundSource.PLAYERS, 0.9f, 1.0f);
        Z0003Registry.play(World, position(), Z0003Registry.YankLayers, SoundSource.NEUTRAL, 0.6f, 1.2f);
        return true;
    }

    public static int yankAll(Player Holder) {
        if (!(Holder.level() instanceof ServerLevel World)) {
            return 0;
        }
        int Pulled = 0;
        for (Z0003Device Found : tetheredTo(World, Holder)) {
            if (Found.yank(Holder)) {
                Pulled++;
            }
        }
        return Pulled;
    }

    public static boolean hasTetherNear(Player Holder) {
        return !Holder.level().getEntitiesOfClass(Z0003Device.class, Holder.getBoundingBox().inflate(HolderSearch), Candidate -> Candidate.tetherHolder() == Holder)
                .isEmpty();
    }

    private void returning(ServerLevel World) {
        Player Holder = TetherId == null ? null : World.getPlayerByUUID(TetherId);
        if (Holder == null) {
            noPhysics = false;
            setState(Idle);
            return;
        }
        Vec3 Target = Holder.position().add(0.0, Holder.getBbHeight() * 0.6, 0.0);
        Vec3 Offset = Target.subtract(fieldCentre());
        double Distance = Offset.length();
        if (Distance < ReturnReach) {
            deliver(World, Holder);
            return;
        }
        Vec3 Step = Offset.scale(Math.min(Distance, ReturnSpeed) / Distance);
        setDeltaMovement(Step);
        setPos(position().add(Step));
        hasImpulse = true;
    }

    private void deliver(ServerLevel World, Player Holder) {
        ItemStack Back = toItem(false);
        untether(Holder);
        giveBack(Holder, Back);
        Z0003Registry.play(World, Holder.position(), Z0003Registry.PickupLayers, SoundSource.PLAYERS, 0.8f, 1.0f);
        discard();
    }

    private void giveBack(Player Receiver, ItemStack Stack) {
        if (CreativeCopy && Receiver.getAbilities().instabuild) {
            return;
        }
        give(Receiver, Stack);
    }

    private static void give(Player Receiver, ItemStack Stack) {
        if (!Receiver.getInventory().add(Stack) && !Stack.isEmpty()) {
            Receiver.drop(Stack, false);
        }
    }

    public ItemStack toItem(boolean KeepArming) {
        ItemStack Back = Kept.isEmpty() ? new ItemStack(Z0003Registry.Device.get()) : Kept.copyWithCount(1);
        Back.remove(Z0003Registry.Arming.get());
        Z0003Arming Setting = arming();
        if (KeepArming && Setting != null) {
            Back.set(Z0003Registry.Arming.get(), Setting);
        }
        return Back;
    }

    public boolean canPickUp() {
        byte Current = state();
        return Current != Active && Current != Returning;
    }

    @Override
    public InteractionResult interact(Player User, InteractionHand Hand) {
        if (isTetheredTo(User) && User.getItemInHand(Hand).is(Items.SHEARS)) {
            if (!level().isClientSide && cut(User)) {
                User.getItemInHand(Hand).hurtAndBreak(1, User, LivingEntity.getSlotForHand(Hand));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTetheredTo(User) && User.getItemInHand(Hand).is(Items.LEAD)) {
            if (!level().isClientSide) {
                yank(User);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!canPickUp() || isRemoved()) {
            return InteractionResult.PASS;
        }
        if (level() instanceof ServerLevel World) {
            pickUp(World, User);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void pickUp(ServerLevel World, Player User) {
        ItemStack Back = toItem(true);
        if (TetherId != null) {
            if (isTetheredTo(User)) {
                untether(User);
            } else {
                Player Holder = World.getPlayerByUUID(TetherId);
                detach(World, Holder != null && !Holder.getAbilities().instabuild ? Holder : null);
            }
        }
        giveBack(User, Back);
        Z0003Registry.play(World, position(), Z0003Registry.PickupLayers, SoundSource.PLAYERS, 0.8f, 1.0f);
        discard();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Range = isActive() ? 64.0 + radius() : 64.0;
        if (entityData.get(HolderData) >= 0) {
            Range = Math.max(Range, HolderSearch);
        }
        return Distance < Range * Range;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB Box = isActive() ? getBoundingBox().inflate(radius() + 1.0) : super.getBoundingBoxForCulling();
        Entity Holder = tetherHolder();
        return Holder == null ? Box : Box.minmax(Holder.getBoundingBox().inflate(1.0));
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putByte("State", state());
        Saved.putFloat("Radius", radius());
        Saved.putLong("Due", Due);
        Saved.putLong("FieldStart", fieldStart());
        if (TetherId != null) {
            Saved.putUUID("Tether", TetherId);
        }
        Saved.putBoolean("LeadSpent", LeadSpent);
        Saved.putBoolean("CreativeCopy", CreativeCopy);
        if (!Kept.isEmpty()) {
            Saved.put("Item", Kept.save(registryAccess()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        byte Loaded = Saved.getByte("State");
        setState(Loaded == Returning ? Idle : Loaded);
        entityData.set(RadiusData, Saved.getFloat("Radius"));
        setDue(Saved.getLong("Due"));
        entityData.set(FieldStartData, Saved.getLong("FieldStart"));
        TetherId = Saved.hasUUID("Tether") ? Saved.getUUID("Tether") : null;
        LeadSpent = Saved.getBoolean("LeadSpent");
        CreativeCopy = Saved.getBoolean("CreativeCopy");
        Kept = Saved.contains("Item") ? ItemStack.parseOptional(registryAccess(), Saved.getCompound("Item")) : ItemStack.EMPTY;
    }
}
