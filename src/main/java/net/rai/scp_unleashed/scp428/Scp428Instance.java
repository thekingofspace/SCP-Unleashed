package net.rai.scp_unleashed.scp428;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.playerclone.PlayerSamples;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class Scp428Instance extends DClassLike {
    public static final double Health = 12.0;
    public static final double Speed = 0.24;
    public static final double Damage = 1.0;
    public static final double SoftRange = 10.0;
    public static final double HardRange = 14.0;
    public static final double SnapRange = 28.0;
    public static final double Tug = 0.09;
    public static final int OrphanTicks = 100;
    public static final int LinkTicks = 20;
    public static final int SenseTicks = 20;
    public static final double SenseRange = 12.0;
    public static final double SenseThrough = 4.0;
    public static final double JoinReach = 1.6;
    public static final double ClutchSpeed = 1.35;
    public static final int RiseTicks = 24;
    public static final int GroanInterval = 420;
    public static final float Glance = 0.25f;
    public static final double SnapJump = 3.0;
    public static final double CordMargin = 1.2;
    public static final double RenderReach = 128.0;
    public static final byte Loose = 0;
    public static final byte Huddled = 1;
    public static final byte Guarding = 2;
    public static final byte Bracing = 3;
    public static final int SwipeTicks = 16;
    public static final int SwipeHit = 8;
    public static final int SwipeGiveUp = 240;
    private static final EntityDataAccessor<CompoundTag> SampleData = SynchedEntityData.defineId(Scp428Instance.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<String> NameData = SynchedEntityData.defineId(Scp428Instance.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> RiseData = SynchedEntityData.defineId(Scp428Instance.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HoldingData = SynchedEntityData.defineId(Scp428Instance.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OwnerData = SynchedEntityData.defineId(Scp428Instance.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> HuddleData = SynchedEntityData.defineId(Scp428Instance.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> SwipeData = SynchedEntityData.defineId(Scp428Instance.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> AimData = SynchedEntityData.defineId(Scp428Instance.class, EntityDataSerializers.BOOLEAN);

    private final Scp428Doorwork.Agent Agent = new Scp428Doorwork.Agent();
    @Nullable
    private UUID CoreId;
    @Nullable
    private UUID RecordId;
    @Nullable
    private Scp428Core Linked;
    private int Orphaned;
    private int HoldDelay;
    @Nullable
    private UUID HoldingVictim;
    private boolean Severed;
    @Nullable
    private CompoundTag Decoded;
    @Nullable
    private ResolvableProfile Profile;
    @Nullable
    private BlockPos SwipeAt;
    private int SwipeWaited;

    public Scp428Instance(EntityType<? extends Scp428Instance> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(SampleData, new CompoundTag());
        Builder.define(NameData, "");
        Builder.define(RiseData, -RiseTicks * 4);
        Builder.define(HoldingData, -1);
        Builder.define(OwnerData, -1);
        Builder.define(HuddleData, Loose);
        Builder.define(SwipeData, -1);
        Builder.define(AimData, false);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp428Navigation(this, World);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp428Goals.Grasp(this));
        goalSelector.addGoal(2, new Scp428Goals.Swipe(this));
        goalSelector.addGoal(3, new Scp428Goals.Leash(this));
        goalSelector.addGoal(4, new Scp428Goals.Clutch(this));
        goalSelector.addGoal(5, new Scp428Goals.Shoot(this));
        goalSelector.addGoal(6, new Scp428Goals.Herd(this));
        goalSelector.addGoal(7, new Scp428Goals.Huddle(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public void bind(Scp428Core Core, Scp428Record Entry) {
        CoreId = Core.getUUID();
        RecordId = Entry.id();
        Linked = Core;
        setAppearance(Entry.appearance());
        entityData.set(SampleData, Entry.sample());
        entityData.set(NameData, Entry.name());
        wear(Scp428Looks.gear(Entry.sample(), registryAccess()));
    }

    private void wear(Map<EquipmentSlot, ItemStack> Gear) {
        for (EquipmentSlot Slot : Scp428Looks.Worn) {
            setItemSlot(Slot, Gear.getOrDefault(Slot, ItemStack.EMPTY).copy());
            setDropChance(Slot, 0.0f);
        }
    }

    public void rise() {
        entityData.set(RiseData, tickCount);
    }

    public float riseProgress(float Partial) {
        return Mth.clamp((tickCount - entityData.get(RiseData) + Partial) / RiseTicks, 0.0f, 1.0f);
    }

    public void sever() {
        Severed = true;
    }

    @Nullable
    public UUID coreId() {
        return CoreId;
    }

    @Nullable
    public UUID recordId() {
        return RecordId;
    }

    public Scp428Doorwork.Agent agent() {
        return Agent;
    }

    public String formerName() {
        return entityData.get(NameData);
    }

    public CompoundTag sample() {
        return entityData.get(SampleData);
    }

    @Nullable
    public ResolvableProfile profile() {
        CompoundTag Current = entityData.get(SampleData);
        if (Current != Decoded) {
            Decoded = Current;
            Profile = PlayerSamples.read(Current);
        }
        return Profile;
    }

    public boolean isPlayerLook() {
        return profile() != null;
    }

    @Nullable
    public Scp428Core core() {
        if (Linked != null && Linked.isAlive() && !Linked.isRemoved()) {
            return Linked;
        }
        Linked = null;
        if (CoreId != null && level() instanceof ServerLevel World && World.getEntity(CoreId) instanceof Scp428Core Found && Found.isAlive()) {
            Linked = Found;
        }
        return Linked;
    }

    public boolean canGrab() {
        return HoldingVictim == null && HoldDelay <= 0 && riseProgress(0.0f) >= 1.0f;
    }

    public boolean isHolding() {
        return HoldingVictim != null;
    }

    public boolean isHolding(UUID Victim) {
        return Victim.equals(HoldingVictim);
    }

    @Nullable
    public UUID holdingVictim() {
        return HoldingVictim;
    }

    public int holdingId() {
        return entityData.get(HoldingData);
    }

    public int ownerId() {
        return entityData.get(OwnerData);
    }

    public byte huddle() {
        return entityData.get(HuddleData);
    }

    @Nullable
    public Scp428Record record() {
        Scp428Core Core = core();
        return Core == null ? null : Core.recordOf(RecordId);
    }

    public void arm(Scp428Record Entry) {
        ItemStack Weapon = Scp428Carry.weapon(Entry);
        setItemSlot(EquipmentSlot.MAINHAND, Weapon == null ? ItemStack.EMPTY : Weapon.copyWithCount(1));
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    @Nullable
    public ItemStack weapon() {
        Scp428Record Entry = record();
        return Entry == null ? null : Scp428Carry.weapon(Entry);
    }

    public boolean armed() {
        ItemStack Weapon = weapon();
        return Weapon != null && Scp428Arms.usable(this, Weapon);
    }

    public void assignSwipe(BlockPos Scanner) {
        SwipeAt = Scanner.immutable();
        SwipeWaited = 0;
    }

    @Nullable
    public BlockPos swipeTarget() {
        return SwipeAt;
    }

    public boolean waitSwipe() {
        return ++SwipeWaited > SwipeGiveUp;
    }

    public void clearSwipe() {
        SwipeAt = null;
        SwipeWaited = 0;
    }

    public int swipeStart() {
        return entityData.get(SwipeData);
    }

    public void startSwipe(int Now) {
        entityData.set(SwipeData, Now);
    }

    public void endSwipe() {
        entityData.set(SwipeData, -1);
    }

    public float swipeProgress(float Partial) {
        int Start = entityData.get(SwipeData);
        if (Start < 0) {
            return -1.0f;
        }
        float Into = (tickCount - Start + Partial) / SwipeTicks;
        return Into >= 0.0f && Into <= 1.0f ? Into : -1.0f;
    }

    public boolean aiming() {
        return entityData.get(AimData);
    }

    public void setAiming(boolean Aim) {
        if (entityData.get(AimData) != Aim) {
            entityData.set(AimData, Aim);
        }
    }

    public void setHuddle(byte Mode) {
        if (entityData.get(HuddleData) != Mode) {
            entityData.set(HuddleData, Mode);
        }
    }

    public void grab(LivingEntity Victim) {
        HoldingVictim = Victim.getUUID();
        entityData.set(HoldingData, Victim.getId());
        getNavigation().stop();
        swing(InteractionHand.MAIN_HAND, true);
    }

    public void release(int Delay) {
        HoldingVictim = null;
        entityData.set(HoldingData, -1);
        HoldDelay = Math.max(HoldDelay, Delay);
    }

    public Vec3 tethered(Vec3 Point) {
        Scp428Core Core = core();
        if (Core == null) {
            return Point;
        }
        Vec3 Offset = Point.subtract(Core.position());
        double Flat = Math.sqrt(Offset.x * Offset.x + Offset.z * Offset.z);
        if (Flat <= SoftRange) {
            return Point;
        }
        double Scale = SoftRange / Flat;
        return Core.position().add(Offset.x * Scale, Offset.y, Offset.z * Scale);
    }

    @Override
    public float glanceRate() {
        return Glance;
    }

    @Override
    public boolean eyesShut() {
        return !isAlive();
    }

    @Override
    public Component getName() {
        if (hasCustomName()) {
            return super.getName();
        }
        return getTypeName();
    }

    @Override
    public boolean shouldShowName() {
        return hasCustomName() && isCustomNameVisible();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive() || !(level() instanceof ServerLevel World)) {
            return;
        }
        if (HoldDelay > 0) {
            HoldDelay--;
        }
        if (riseProgress(0.0f) < 1.0f) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
        if ((tickCount + getId()) % LinkTicks == 0) {
            checkLink(World);
            if (isRemoved()) {
                return;
            }
        }
        Scp428Core Core = core();
        int Owner = Core == null ? -1 : Core.getId();
        if (entityData.get(OwnerData) != Owner) {
            entityData.set(OwnerData, Owner);
        }
        if (Core != null) {
            tether(World, Core);
            if ((tickCount + getId()) % SenseTicks == 0 && Core.getTarget() == null && !Core.isGripping()) {
                LivingEntity Found = Scp428Quarry.pick(World, this, SenseRange, SenseThrough);
                if (Found != null) {
                    Core.alert(Found);
                }
            }
        }
        Agent.tick(this, World);
    }

    @Override
    public void lerpTo(double X, double Y, double Z, float YRot, float XRot, int Steps) {
        boolean Jump = distanceToSqr(X, Y, Z) > SnapJump * SnapJump;
        super.lerpTo(X, Y, Z, YRot, XRot, Steps);
        if (Jump && level().isClientSide) {
            lerpSteps = 0;
            setPos(X, Y, Z);
            setYRot(YRot);
            setXRot(XRot);
            setOldPosAndRot();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB Own = super.getBoundingBoxForCulling();
        if (level().isClientSide && level().getEntity(ownerId()) instanceof Scp428Core Core && !Core.isRemoved()) {
            return Own.minmax(Core.getBoundingBox()).inflate(CordMargin);
        }
        return Own;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Reach = RenderReach * getViewScale();
        return Distance < Reach * Reach;
    }

    private void checkLink(ServerLevel World) {
        if (CoreId == null || RecordId == null) {
            Orphaned += LinkTicks;
        } else if (World.getEntity(CoreId) instanceof Scp428Core Found && !Found.isRemoved()) {
            if (!Found.isAlive()) {
                return;
            }
            if (!Found.claims(RecordId, getUUID())) {
                sever();
                discard();
                return;
            }
            Linked = Found;
            Orphaned = 0;
        } else {
            Linked = null;
            Orphaned += LinkTicks;
        }
        if (Orphaned >= OrphanTicks) {
            sever();
            discard();
        }
    }

    private void tether(ServerLevel World, Scp428Core Core) {
        double Gap = distanceTo(Core);
        if (Gap > SnapRange) {
            Vec3 Back = landingNear(World, Core);
            teleportTo(Back.x, Back.y, Back.z);
            getNavigation().stop();
            Agent.reset();
            return;
        }
        if (Gap > HardRange) {
            Vec3 Pull = Core.position().subtract(position());
            Vec3 Flat = new Vec3(Pull.x, 0.0, Pull.z);
            if (Flat.lengthSqr() > 1.0e-4) {
                Flat = Flat.normalize().scale(Tug * Math.min(2.0, Gap - HardRange));
                setDeltaMovement(getDeltaMovement().add(Flat.x, 0.0, Flat.z));
                hurtMarked = true;
            }
        }
    }

    private Vec3 landingNear(ServerLevel World, Scp428Core Core) {
        for (int Attempt = 0; Attempt < 10; Attempt++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = Core.radius() + 1.0 + random.nextDouble() * 2.0;
            Vec3 Spot = Core.position().add(Math.cos(Angle) * Reach, 0.0, Math.sin(Angle) * Reach);
            AABB Box = getType().getDimensions().makeBoundingBox(Spot);
            if (World.noCollision(Box) && !World.getBlockState(BlockPos.containing(Spot).below()).isAir()) {
                return Spot;
            }
        }
        return Core.position();
    }

    @Override
    public void die(DamageSource Source) {
        boolean Before = dead;
        super.die(Source);
        if (!Before && dead && !level().isClientSide && !Severed && RecordId != null) {
            Scp428Core Core = core();
            if (Core == null && CoreId != null && level() instanceof ServerLevel World && World.getEntity(CoreId) instanceof Scp428Core Found) {
                Core = Found;
            }
            if (Core != null) {
                Core.lose(RecordId, position());
            }
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        Entity Attacker = Source.getEntity();
        if (Attacker instanceof Scp428Core || Attacker instanceof Scp428Instance) {
            return false;
        }
        if (!level().isClientSide && Attacker != null && Scp428Holds.isHeld(Attacker)) {
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide && Attacker != null && HoldingVictim != null) {
            Scp428Core Core = core();
            if (Core != null) {
                Core.letGo(this, Scp428Core.HolderDelay);
            } else {
                release(Scp428Core.HolderDelay);
            }
        }
        return Hurt;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return Other instanceof Scp428Core || Other instanceof Scp428Instance || super.isAlliedTo(Other);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel World, DamageSource Source, boolean RecentlyHit) {
    }

    @Override
    protected void dropEquipment() {
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
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
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp428Registry.Groan.get();
    }

    @Override
    public void playAmbientSound() {
        if (isSilent()) {
            return;
        }
        playSound(Scp428Registry.Groan.get(), getSoundVolume(), getVoicePitch());
        playSound(Scp428Registry.GroanLow.get(), getSoundVolume() * 0.8f, getVoicePitch());
    }

    @Override
    public int getAmbientSoundInterval() {
        return GroanInterval;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    public float getVoicePitch() {
        return 0.85f + random.nextFloat() * 0.1f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp428Registry.InstanceHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp428Registry.InstanceDeath.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (CoreId != null) {
            Saved.putUUID("Core", CoreId);
        }
        if (RecordId != null) {
            Saved.putUUID("Record", RecordId);
        }
        Saved.put("Sample", entityData.get(SampleData).copy());
        Saved.putString("Former", formerName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        CoreId = Saved.hasUUID("Core") ? Saved.getUUID("Core") : null;
        RecordId = Saved.hasUUID("Record") ? Saved.getUUID("Record") : null;
        entityData.set(SampleData, Saved.getCompound("Sample").copy());
        entityData.set(NameData, Saved.getString("Former"));
        Linked = null;
    }
}
