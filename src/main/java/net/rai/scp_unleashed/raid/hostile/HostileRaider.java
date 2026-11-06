package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.npc.doors.NpcDoorNavigation;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidMark;
import net.rai.scp_unleashed.raid.RaidRegistry;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.RaidWorld;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public abstract class HostileRaider extends DClassLike implements Enemy {
    public static final byte StateActive = 0;
    public static final byte StateRising = 1;
    public static final byte StateSinking = 2;
    public static final byte StateRoping = 3;
    public static final byte StateWaiting = 4;
    public static final byte StateBoarding = 5;
    public static final byte RoleFighter = 0;
    public static final byte RoleSeeker = 1;
    public static final byte SlotNone = 0;
    public static final byte SlotHidden = 1;
    public static final byte SlotHand = 2;
    public static final byte SlotHead = 3;
    public static final int RiseTicks = 30;
    public static final int SinkTicks = 40;
    public static final double RiseDepth = 1.9;
    public static final double SinkDepth = 2.0;
    public static final int GlideTicks = 12;
    public static final int OrphanLimit = 60;
    public static final int HurtMemory = 100;
    public static final int[] SafeStyles = {DClassAppearance.Bald, DClassAppearance.Buzz, DClassAppearance.Short, DClassAppearance.Parted, DClassAppearance.Long};
    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(HostileRaider.class, EntityDataSerializers.BYTE);

    private int Rank = 1;
    private int BurstLeft;
    private int Pause;
    private byte Role = RoleFighter;
    private int Slot;
    private int StateTicks;
    private int Reaction;
    private int OrphanTicks;
    private boolean Hauling;
    private ItemStack Stowed = ItemStack.EMPTY;
    private ItemStack Hidden = ItemStack.EMPTY;
    private int Issued = -1;
    private byte IssuedSlot = SlotNone;
    @Nullable
    private Vec3 Anchor;
    @Nullable
    private Vec3 SinkFrom;

    protected HostileRaider(EntityType<? extends HostileRaider> Type, Level World) {
        super(Type, World);
        setCanPickUpLoot(false);
        setPersistenceRequired();
        for (EquipmentSlot Each : EquipmentSlot.values()) {
            setDropChance(Each, 0.0f);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.ARMOR, 0.0).add(Attributes.ARMOR_TOUGHNESS, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    public abstract HostileFaction faction();

    protected abstract void dress(int Level, int Index, RandomSource Random);

    protected abstract double healthFor(int Level);

    protected abstract DustParticleOptions mote();

    public ResourceLocation factionId() {
        return faction().id();
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new NpcDoorNavigation(this, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(StateData, StateActive);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(1, new HostileRaiderGoals.Withdraw(this));
        addSeekGoals();
        goalSelector.addGoal(3, new HostileRaiderGoals.Engage(this));
        goalSelector.addGoal(6, new HostileRaiderGoals.Prowl(this));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return active() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HostileRaiderGoals.Hunt(this));
    }

    protected void addSeekGoals() {
    }

    public void configure(int Level, byte Duty, int Index, RandomSource Random) {
        Rank = Mth.clamp(Level, 1, HostileLevels.MaxLevel);
        Role = Duty;
        Slot = Index;
        int Style = SafeStyles[Random.nextInt(SafeStyles.length)];
        int Facial = Random.nextInt(10) < 6 ? 0 : 1 + Random.nextInt(DClassAppearance.Facials - 1);
        setAppearance(DClassAppearance.pack(Random.nextInt(DClassAppearance.Skins), Style, Random.nextInt(DClassAppearance.HairColours - 1),
                Random.nextInt(DClassAppearance.EyeColours), Random.nextInt(DClassAppearance.Brows), Facial, 0, DClassAppearance.Fatigues));
        dress(Rank, Index, Random);
        int Tier = HostileLevels.keycardTier(Rank);
        setItemSlot(EquipmentSlot.OFFHAND, Tier > 0 ? new ItemStack(DoorRegistry.Keycards.get(Tier - 1).get()) : ItemStack.EMPTY);
        for (EquipmentSlot Each : EquipmentSlot.values()) {
            setDropChance(Each, 0.0f);
        }
        base(Attributes.MAX_HEALTH, healthFor(Rank));
        base(Attributes.ARMOR, HostileLevels.extraArmour(Rank));
        setHealth(getMaxHealth());
    }

    protected void base(Holder<Attribute> Stat, double Value) {
        AttributeInstance Instance = getAttribute(Stat);
        if (Instance != null) {
            Instance.setBaseValue(Value);
        }
    }

    public int rank() {
        return Rank;
    }

    public byte role() {
        return Role;
    }

    public boolean seeker() {
        return Role == RoleSeeker;
    }

    public void setRole(byte Duty) {
        Role = Duty;
    }

    public int slot() {
        return Slot;
    }

    public byte state() {
        return entityData.get(StateData);
    }

    public void setState(byte Value) {
        entityData.set(StateData, Value);
        StateTicks = 0;
    }

    public int stateTicks() {
        return StateTicks;
    }

    public boolean active() {
        return state() == StateActive;
    }

    public boolean hauling() {
        return Hauling;
    }

    public int reaction() {
        return Reaction;
    }

    public void delayReaction() {
        int Base = HostileLevels.reactionTicks(Rank);
        Reaction = Base + getRandom().nextInt(Base / 2 + 1);
    }

    public boolean triggerReady() {
        return Pause <= 0;
    }

    public int pauseLeft() {
        return Pause;
    }

    public void firedShot() {
        if (BurstLeft <= 0) {
            BurstLeft = HostileLevels.burst(Rank, getRandom());
        }
        BurstLeft--;
        if (BurstLeft <= 0) {
            Pause = HostileLevels.pause(Rank, getRandom());
        }
    }

    public float accuracy() {
        float Base = HostileLevels.accuracy(Rank);
        if (getDeltaMovement().horizontalDistanceSqr() > 0.004) {
            Base *= 0.9f;
        }
        return Mth.clamp(Base, 0.05f, 1.0f);
    }

    public boolean strafes() {
        return Rank >= HostileLevels.StrafeLevel;
    }

    public boolean kites() {
        return Rank >= HostileLevels.KiteLevel;
    }

    public boolean recentlyHurt() {
        return getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < HurtMemory;
    }

    public void carry(ItemStack Visual) {
        if (!Hauling) {
            Stowed = getMainHandItem().copy();
        }
        Hauling = true;
        setItemSlot(EquipmentSlot.MAINHAND, Visual.copy());
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        setTarget(null);
        setAggressive(false);
    }

    public void unload() {
        Hauling = false;
        setItemSlot(EquipmentSlot.MAINHAND, Stowed.copy());
        Stowed = ItemStack.EMPTY;
    }

    public void issue(int EntryId, ItemStack Stack, byte Where) {
        Issued = EntryId;
        IssuedSlot = Where;
        switch (Where) {
            case SlotHand -> setItemSlot(EquipmentSlot.MAINHAND, Stack.copy());
            case SlotHead -> setItemSlot(EquipmentSlot.HEAD, Stack.copy());
            default -> Hidden = Stack.copy();
        }
        for (EquipmentSlot Each : EquipmentSlot.values()) {
            setDropChance(Each, 0.0f);
        }
    }

    public int issued() {
        return Issued;
    }

    public byte issuedSlot() {
        return IssuedSlot;
    }

    public ItemStack issuedStack() {
        return switch (IssuedSlot) {
            case SlotHand -> Hauling ? Stowed : getMainHandItem();
            case SlotHead -> getItemBySlot(EquipmentSlot.HEAD);
            case SlotHidden -> Hidden;
            default -> ItemStack.EMPTY;
        };
    }

    public ItemStack takeIssued() {
        ItemStack Found = issuedStack().copy();
        switch (IssuedSlot) {
            case SlotHand -> {
                if (Hauling) {
                    Stowed = ItemStack.EMPTY;
                } else {
                    setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
            }
            case SlotHead -> setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            default -> Hidden = ItemStack.EMPTY;
        }
        Issued = -1;
        IssuedSlot = SlotNone;
        return Found;
    }

    public ItemStack hidden() {
        return Hidden;
    }

    public void clearIssue() {
        Issued = -1;
        IssuedSlot = SlotNone;
        Hidden = ItemStack.EMPTY;
    }

    @Nullable
    public Vec3 anchor() {
        return Anchor;
    }

    public void setAnchor(@Nullable Vec3 Point) {
        Anchor = Point;
    }

    @Nullable
    public Raid raid() {
        if (!(level() instanceof ServerLevel World) || !hasData(RaidRegistry.Mark)) {
            return null;
        }
        RaidMark Mark = getData(RaidRegistry.Mark);
        if (!Mark.marked()) {
            return null;
        }
        Raid Found = RaidWorld.get(World.getServer()).raid(Mark.Raid());
        return Found != null && Found.live() ? Found : null;
    }

    public boolean retreating() {
        Raid Current = raid();
        return Current != null && Current.stage() == RaidStage.Retreating;
    }

    public void beginRise(Vec3 Spot) {
        Anchor = Spot;
        noPhysics = true;
        setNoGravity(true);
        setPos(Spot.x, Spot.y - RiseDepth, Spot.z);
        setDeltaMovement(Vec3.ZERO);
        setState(StateRising);
    }

    public void beginSink() {
        beginSink(null);
    }

    public void beginSink(@Nullable Vec3 Centre) {
        if (state() == StateSinking) {
            return;
        }
        SinkFrom = position();
        Anchor = Centre == null ? position() : new Vec3(Centre.x, getY(), Centre.z);
        getNavigation().stop();
        setTarget(null);
        setAggressive(false);
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        setState(StateSinking);
        HostileRaidStyle.sinking(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        StateTicks++;
        if (Reaction > 0) {
            Reaction--;
        }
        if (Pause > 0) {
            Pause--;
        }
        switch (state()) {
            case StateRising -> rise();
            case StateSinking -> sink();
            default -> stateTick();
        }
        if (tickCount % 20 == 0) {
            orphan();
        }
    }

    protected void stateTick() {
    }

    private void rise() {
        if (Anchor == null) {
            settle();
            return;
        }
        float Share = Mth.clamp(StateTicks / (float) RiseTicks, 0.0f, 1.0f);
        setPos(Anchor.x, Anchor.y - RiseDepth * (1.0f - Share), Anchor.z);
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        if (StateTicks % 3 == 0 && level() instanceof ServerLevel World) {
            World.sendParticles(mote(), getX(), Anchor.y + 0.1, getZ(), 4, 0.35, 0.05, 0.35, 0.0);
        }
        if (StateTicks >= RiseTicks) {
            settle();
            delayReaction();
        }
    }

    private void settle() {
        if (Anchor != null) {
            setPos(Anchor.x, Anchor.y, Anchor.z);
        }
        noPhysics = false;
        setNoGravity(false);
        setState(StateActive);
    }

    private void sink() {
        if (Anchor == null) {
            Anchor = position();
        }
        if (SinkFrom == null) {
            SinkFrom = Anchor;
        }
        float Share = Mth.clamp(StateTicks / (float) SinkTicks, 0.0f, 1.0f);
        float Glide = Mth.clamp(StateTicks / (float) GlideTicks, 0.0f, 1.0f);
        setPos(Mth.lerp(Glide, SinkFrom.x, Anchor.x), Anchor.y - SinkDepth * Share, Mth.lerp(Glide, SinkFrom.z, Anchor.z));
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        if (StateTicks % 2 == 0 && level() instanceof ServerLevel World) {
            World.sendParticles(mote(), getX(), Anchor.y + 0.1, getZ(), 5, 0.35, 0.05, 0.35, 0.0);
            World.sendParticles(mote(), getX(), getY() + 1.2, getZ(), 2, 0.25, 0.4, 0.25, 0.0);
        }
        if (StateTicks >= SinkTicks) {
            HostileRaidStyle.departed(this);
        }
    }

    private void orphan() {
        if (!hasData(RaidRegistry.Mark) || !getData(RaidRegistry.Mark).marked() || !(level() instanceof ServerLevel World)) {
            OrphanTicks = 0;
            return;
        }
        Raid Found = RaidWorld.get(World.getServer()).raid(getData(RaidRegistry.Mark).Raid());
        if (Found == null || !Found.live() || !Found.raiders().contains(getUUID())) {
            OrphanTicks += 20;
            if (OrphanTicks >= OrphanLimit) {
                discard();
            }
        } else {
            OrphanTicks = 0;
        }
    }

    public Vec3 exitPoint(Raid Current) {
        return Vec3.atBottomCenterOf(RaidDirector.exitOf(Current));
    }

    public double exitReach() {
        return 2.0;
    }

    public void arrive() {
        HostileRaidStyle.arrived(this);
    }

    public boolean useArsenal(LivingEntity Target, double Distance, boolean Visible) {
        return false;
    }

    public boolean ally(@Nullable Entity Other) {
        return Other instanceof HostileRaider Raider && Raider.factionId().equals(factionId());
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return ally(Other) || super.isAlliedTo(Other);
    }

    @Override
    public boolean isPushable() {
        return active() && super.isPushable();
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return active() && super.causeFallDamage(Distance, Multiplier, Source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel World, DamageSource Source, boolean Recent) {
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return true;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 5 + Rank * 2;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        return InteractionResult.PASS;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public float glanceRate() {
        return 0.7f;
    }

    public static DustParticleOptions dust(float Red, float Green, float Blue, float Size) {
        return new DustParticleOptions(new Vector3f(Red, Green, Blue), Size);
    }
}
