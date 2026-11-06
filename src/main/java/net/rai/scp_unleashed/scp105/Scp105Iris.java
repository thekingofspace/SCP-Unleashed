package net.rai.scp_unleashed.scp105;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp105.photo096.Scp105Photo096Gift;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Scp105Iris extends DClassLike implements ControlRefusing {
    public static final float Width = 0.6f;
    public static final float Height = 1.8f;
    public static final float EyeHeight = 1.62f;
    public static final double MaxHealth = 40.0;
    public static final double MoveSpeed = 0.25;
    public static final double StrollSpeed = 0.6;
    public static final double HomeSpeed = 0.7;
    public static final double PanicSpeed = 1.45;
    public static final double FleeWalk = 1.15;
    public static final double FleeRun = 1.45;
    public static final float FleeRange = 10.0f;
    public static final int StrollInterval = 80;
    public static final int HomeRadius = 20;
    public static final int HomeDrift = 96;
    public static final float LookRange = 10.0f;
    public static final float LookChance = 0.08f;
    public static final float Glance = 1.1f;
    public static final float DamageScale = 0.5f;
    public static final float DamageCap = 3.0f;
    public static final int RegenDelay = 100;
    public static final int RegenInterval = 20;
    public static final float RegenAmount = 1.0f;
    public static final float WaterCost = 8.0f;
    public static final int Lines = 10;
    public static final int GiftLines = 3;
    public static final int LineCooldown = 30;
    public static final int AttendTicks = 80;
    public static final int TalkTicks = 40;
    public static final int GiveTicks = 26;
    public static final int GiveHandTick = 12;
    public static final int SnapTicks = 32;
    public static final int SnapClick = 15;
    public static final int EjectDelay = 5;
    public static final float GiftChance = 0.25f;
    public static final long GiftCooldown = 12000L;
    public static final long RollSpacing = 200L;
    public static final int PityRolls = 5;
    public static final double GiftReach = 8.0;
    public static final byte ActNone = 0;
    public static final byte ActTalk = 1;
    public static final byte ActSnap = 2;
    public static final byte ActGive = 3;
    public static final RawAnimation SnapPlay = RawAnimation.begin().thenPlayAndHold("snap");
    public static final RawAnimation GivePlay = RawAnimation.begin().thenPlayAndHold("give");
    public static final RawAnimation TalkLoop = RawAnimation.begin().thenLoop("talk");
    public static final String Prefix = "message.scp_unleashed.scp105.";
    protected static final EntityDataAccessor<Byte> ActData = SynchedEntityData.defineId(Scp105Iris.class, EntityDataSerializers.BYTE);

    private int ActLeft;
    private int ActStart;
    @Nullable
    private UUID Listener;
    private int AttendLeft;
    private int LineReady;
    private int LastLine = -1;
    @Nullable
    private UUID GiftFor;
    private int GiftAt;
    private int EjectAt = -1;
    private int Calm;
    private int NextSnap = 200;
    private final Map<UUID, Acquaintance> Known = new HashMap<>();

    public Scp105Iris(EntityType<? extends Scp105Iris> Type, Level World) {
        super(Type, World);
        setPathfindingMalus(PathType.WATER, WaterCost);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActData, ActNone);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, PanicSpeed));
        goalSelector.addGoal(1, new Scp105Goals.Flee(this, FleeRange, FleeWalk, FleeRun));
        goalSelector.addGoal(2, new Scp105Goals.Attend(this));
        goalSelector.addGoal(3, new Scp105Goals.Snap(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, HomeSpeed));
        goalSelector.addGoal(5, new Scp105Goals.Wander(this, StrollSpeed, StrollInterval));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, LookRange, LookChance));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        restrictTo(blockPosition(), HomeRadius);
        return Result;
    }

    @Override
    public float glanceRate() {
        return Glance;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable(Prefix + "refuses");
    }

    public static boolean isThreat(LivingEntity Other) {
        if (!(Other instanceof Enemy) || !Other.isAlive()) {
            return false;
        }
        return !(Other instanceof NeutralMob Neutral) || Neutral.isAngry();
    }

    public byte act() {
        return entityData.get(ActData);
    }

    public int actAge() {
        return tickCount - ActStart;
    }

    public void startAct(byte Kind, int Ticks) {
        ActLeft = Ticks;
        ActStart = tickCount;
        entityData.set(ActData, Kind);
    }

    public void stopAct() {
        ActLeft = 0;
        GiftFor = null;
        EjectAt = -1;
        entityData.set(ActData, ActNone);
    }

    public boolean isBusy() {
        return act() == ActSnap || act() == ActGive;
    }

    public boolean isSnapping() {
        return act() == ActSnap;
    }

    public boolean isGiving() {
        return act() == ActGive;
    }

    public boolean isTalking() {
        return act() == ActTalk;
    }

    public int snapReady() {
        return NextSnap;
    }

    public void deferSnap(int Ticks) {
        NextSnap = tickCount + Ticks;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (ActData.equals(Accessor)) {
            ActStart = tickCount;
        }
    }

    @Override
    protected RawAnimation bodyAnimation() {
        byte Current = act();
        if (!isSwimmingPose() && !isSittingPose()) {
            if (Current == ActSnap) {
                return SnapPlay;
            }
            if (Current == ActGive) {
                return GivePlay;
            }
            if (Current == ActTalk && !isStriding()) {
                return TalkLoop;
            }
        }
        return super.bodyAnimation();
    }

    public Vec3 cameraPosition() {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYHeadRot());
        return getEyePosition().add(Facing.x * 0.55, -0.05, Facing.z * 0.55);
    }

    public Vec3 handPosition() {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        return position().add(Facing.x * 0.75, 1.3, Facing.z * 0.75);
    }

    public void click() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Vec3 Lens = cameraPosition();
        Scp105Sounds.play(World, Lens, Scp105Registry.ShutterLayers, SoundSource.NEUTRAL, 0.9f, 0.95f + getRandom().nextFloat() * 0.1f);
        World.sendParticles(Scp105Registry.Flash.get(), Lens.x, Lens.y, Lens.z, 1, 0.0, 0.0, 0.0, 0.0);
        EjectAt = tickCount + EjectDelay;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND || User.isSpectator() || User.getMainHandItem().is(Items.NAME_TAG)) {
            return InteractionResult.PASS;
        }
        if (User instanceof ServerPlayer Speaker) {
            listen(Speaker);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void listen(ServerPlayer Speaker) {
        Listener = Speaker.getUUID();
        AttendLeft = AttendTicks;
        getNavigation().stop();
        if (tickCount < LineReady || isGiving()) {
            return;
        }
        LineReady = tickCount + LineCooldown;
        if (!isSnapping()) {
            startAct(ActTalk, TalkTicks);
        }
        if (tryGift(Speaker)) {
            say(Speaker, Component.translatable(Prefix + "gift." + getRandom().nextInt(GiftLines)));
            return;
        }
        int Line = getRandom().nextInt(Lines);
        if (Line == LastLine) {
            Line = (Line + 1 + getRandom().nextInt(Lines - 1)) % Lines;
        }
        LastLine = Line;
        say(Speaker, Component.translatable(Prefix + "line." + Line));
    }

    public void say(ServerPlayer Speaker, Component Spoken) {
        Component Name = Component.translatable(Prefix + "speaker");
        Speaker.displayClientMessage(Senses.heard(Speaker, Component.translatable(Prefix + "speech", Name, Spoken).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)), true);
    }

    private Acquaintance acquaintance(UUID Id) {
        return Known.computeIfAbsent(Id, Key -> new Acquaintance());
    }

    private boolean tryGift(ServerPlayer Speaker) {
        long Now = level().getGameTime();
        Acquaintance Friend = acquaintance(Speaker.getUUID());
        if (GiftFor != null || Now < Friend.NextGift || Now < Friend.NextRoll) {
            return false;
        }
        Friend.NextRoll = Now + RollSpacing;
        boolean Lucky = Friend.Misses >= PityRolls || getRandom().nextFloat() < GiftChance;
        if (!Lucky) {
            Friend.Misses++;
            return false;
        }
        beginGift(Speaker);
        return true;
    }

    public void beginGift(ServerPlayer Speaker) {
        Acquaintance Friend = acquaintance(Speaker.getUUID());
        Friend.Misses = 0;
        Friend.NextGift = level().getGameTime() + GiftCooldown;
        Listener = Speaker.getUUID();
        AttendLeft = Math.max(AttendLeft, GiveTicks + 10);
        getNavigation().stop();
        startAct(ActGive, GiveTicks);
        GiftFor = Speaker.getUUID();
        GiftAt = tickCount + GiveHandTick;
    }

    private void handOver() {
        if (!(level() instanceof ServerLevel World) || GiftFor == null) {
            return;
        }
        Player Target = World.getPlayerByUUID(GiftFor);
        GiftFor = null;
        if (Scp105Photo096Gift.handOver(World, this, Target)) {
            return;
        }
        ItemStack Photo = Scp105PhotographItem.create(Scp105Subjects.roll(getRandom(), Scp105PhotographItem.peaceful(World)));
        Vec3 Hand = handPosition();
        Scp105Sounds.play(World, Hand, Scp105Registry.GiveLayers, SoundSource.NEUTRAL, 0.8f, 1.0f);
        if (Target != null && Target.isAlive() && !Target.isSpectator() && Target.distanceToSqr(this) < GiftReach * GiftReach && Target.getInventory().add(Photo)) {
            return;
        }
        ItemEntity Drop = new ItemEntity(World, Hand.x, Hand.y, Hand.z, Photo);
        Vec3 Toward = Target != null ? Target.position().subtract(position()).normalize().scale(0.18) : Vec3.directionFromRotation(0.0f, yBodyRot).scale(0.18);
        Drop.setDeltaMovement(Toward.x, 0.15, Toward.z);
        Drop.setDefaultPickUpDelay();
        World.addFreshEntity(Drop);
    }

    @Nullable
    public Player listener() {
        if (Listener == null || AttendLeft <= 0) {
            return null;
        }
        Player Found = level().getPlayerByUUID(Listener);
        return Found != null && Found.isAlive() && !Found.isSpectator() && Found.distanceToSqr(this) < 64.0 ? Found : null;
    }

    public boolean isAttending() {
        return listener() != null;
    }

    public void forget() {
        Listener = null;
        AttendLeft = 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel) || !isAlive()) {
            return;
        }
        if (AttendLeft > 0) {
            AttendLeft--;
        }
        if (AttendLeft <= 0) {
            Listener = null;
        }
        if (ActLeft > 0 && --ActLeft == 0) {
            entityData.set(ActData, ActNone);
        }
        if (GiftFor != null && tickCount >= GiftAt) {
            handOver();
        }
        if (EjectAt >= 0 && tickCount >= EjectAt) {
            EjectAt = -1;
            Scp105Sounds.play(level(), cameraPosition(), Scp105Registry.EjectLayers, SoundSource.NEUTRAL, 0.7f, 1.0f);
        }
        if (!hasRestriction() || getRestrictCenter().distSqr(blockPosition()) > (double) HomeDrift * HomeDrift) {
            restrictTo(blockPosition(), HomeRadius);
        }
        Calm = Math.min(Calm + 1, RegenDelay + 1);
        if (Calm > RegenDelay && tickCount % RegenInterval == 0 && getHealth() < getMaxHealth()) {
            heal(RegenAmount);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        boolean Hit = super.hurt(Source, Math.min(DamageCap, Amount * DamageScale));
        if (Hit && !level().isClientSide) {
            Calm = 0;
            forget();
            if (act() != ActNone) {
                stopAct();
            }
        }
        return Hit;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return Effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL && super.canBeAffected(Effect);
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
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (hasRestriction()) {
            Saved.putLong("Home", getRestrictCenter().asLong());
        }
        Saved.putInt("Calm", Calm);
        ListTag Friends = new ListTag();
        long Now = level().getGameTime();
        for (Map.Entry<UUID, Acquaintance> Entry : Known.entrySet()) {
            Acquaintance Friend = Entry.getValue();
            if (Friend.NextGift <= Now && Friend.NextRoll <= Now && Friend.Misses == 0) {
                continue;
            }
            CompoundTag Memory = new CompoundTag();
            Memory.putUUID("Id", Entry.getKey());
            Memory.putLong("Gift", Friend.NextGift);
            Memory.putLong("Roll", Friend.NextRoll);
            Memory.putInt("Misses", Friend.Misses);
            Friends.add(Memory);
        }
        Saved.put("Friends", Friends);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        if (Saved.contains("Home")) {
            restrictTo(BlockPos.of(Saved.getLong("Home")), HomeRadius);
        }
        Calm = Saved.getInt("Calm");
        Known.clear();
        ListTag Friends = Saved.getList("Friends", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Friends.size(); Index++) {
            CompoundTag Entry = Friends.getCompound(Index);
            if (!Entry.hasUUID("Id")) {
                continue;
            }
            Acquaintance Friend = new Acquaintance();
            Friend.NextGift = Entry.getLong("Gift");
            Friend.NextRoll = Entry.getLong("Roll");
            Friend.Misses = Entry.getInt("Misses");
            Known.put(Entry.getUUID("Id"), Friend);
        }
        setPersistenceRequired();
    }

    public static final class Acquaintance {
        public long NextGift;
        public long NextRoll;
        public int Misses;
    }
}
