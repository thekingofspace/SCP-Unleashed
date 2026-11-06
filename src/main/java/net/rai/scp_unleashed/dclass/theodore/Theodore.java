package net.rai.scp_unleashed.dclass.theodore;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.DClassScores;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.labware.LabContamination;
import net.rai.scp_unleashed.scp020.Scp020Mold;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class Theodore extends DClassLike implements ControlRefusing {
    public static final byte StateWandering = 0;
    public static final byte StateSeeking = 1;
    public static final byte StateEating = 2;
    public static final byte StateCloning = 3;
    public static final byte StateClone = 4;
    public static final byte StateMerging = 5;
    public static final int ClusterSize = 16;
    public static final int SeekRange = 12;
    public static final int EatTicks = 40;
    public static final int EmergeTicks = 15;
    public static final int CloneMin = 30;
    public static final int CloneSpread = 21;
    public static final int FirstClone = 30;
    public static final int GatherLimit = 160;
    public static final int SquashTicks = 40;
    public static final int RecallTicks = 40;
    public static final double MergeReach = 2.5;
    public static final float Glance = 0.35f;
    public static final DustParticleOptions Flesh = new DustParticleOptions(new Vector3f(0.62f, 0.12f, 0.12f), 1.4f);
    public static final DustParticleOptions Poison = new DustParticleOptions(new Vector3f(0.55f, 0.72f, 0.28f), 1.0f);
    protected static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(Theodore.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> CountData = SynchedEntityData.defineId(Theodore.class, EntityDataSerializers.INT);
    public static final List<Predicate<ItemStack>> ArsenicLike = new CopyOnWriteArrayList<>();

    @Nullable
    private UUID Cluster;
    private boolean Eater;
    private int NextClone;
    private int EatLeft;
    private int EmergeLeft;
    private boolean Merging;
    private int MergeTicks;
    private int SquashLeft;
    @Nullable
    private Vec3 MergePoint;
    private long MergeSeen;

    public Theodore(EntityType<? extends Theodore> Type, Level World) {
        super(Type, World);
        setAppearance(DClassAppearance.Theodore);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(StateData, StateWandering);
        Builder.define(CountData, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new TheodoreGoals.Hold(this));
        goalSelector.addGoal(1, new TheodoreGoals.Gather(this));
        goalSelector.addGoal(2, new TheodoreGoals.SeekArsenic(this));
        goalSelector.addGoal(3, new TheodoreGoals.Huddle(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f, 0.01f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        equipUniform();
        return Result;
    }

    public void equipUniform() {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.MtfHelmet.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.MtfVest.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.MtfPants.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.MtfBoots.get()));
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            setDropChance(Slot, 0.0f);
        }
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.theodore.refuses");
    }

    @Override
    public float glanceRate() {
        return Glance;
    }

    public byte state() {
        return entityData.get(StateData);
    }

    private void setState(byte Value) {
        entityData.set(StateData, Value);
    }

    public int clusterCount() {
        return entityData.get(CountData);
    }

    @Nullable
    public UUID cluster() {
        return Cluster;
    }

    public boolean isEater() {
        return Eater;
    }

    public int iq() {
        return DClassScores.TheodoreIq;
    }

    public int athletic() {
        return DClassScores.TheodoreAthletic;
    }

    public boolean isBusy() {
        return EatLeft > 0 || EmergeLeft > 0 || action() == ActionMerging;
    }

    public static boolean isArsenic(ItemStack Stack) {
        if (Stack.is(TheodoreRegistry.Arsenic.get())) {
            return true;
        }
        for (Predicate<ItemStack> Test : ArsenicLike) {
            if (Test.test(Stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean canEatArsenic() {
        return isAlive() && Cluster == null && EatLeft <= 0 && !Merging;
    }

    @Nullable
    public Vec3 mergePoint() {
        return MergePoint;
    }

    public InteractionResult offerArsenic(Player User, InteractionHand Hand) {
        if (!canEatArsenic()) {
            return InteractionResult.PASS;
        }
        if (level() instanceof ServerLevel) {
            User.getItemInHand(Hand).consume(1, User);
            beginEating();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void beginEating() {
        EatLeft = EatTicks;
        setAction(ActionEating);
        setState(StateEating);
        getNavigation().stop();
        playLayers(TheodoreRegistry.EatLayers, 0.9f, 1.0f);
    }

    public void markSeeking(boolean Seeking) {
        if (Cluster == null && EatLeft <= 0) {
            setState(Seeking ? StateSeeking : StateWandering);
        }
    }

    public void joinCluster(UUID Id) {
        Cluster = Id;
        Eater = false;
        setState(StateClone);
    }

    public void emerge() {
        EmergeLeft = EmergeTicks;
        setAction(ActionEmerging);
    }

    public void callTo(Vec3 Point, long Time) {
        MergePoint = Point;
        MergeSeen = Time;
        if (!Eater) {
            setState(StateMerging);
        }
    }

    public void squash() {
        setAction(ActionMerging);
        getNavigation().stop();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel World && isAlive()) {
            serverTick(World);
        }
    }

    private void serverTick(ServerLevel World) {
        LabContamination.tick(this);
        if (Math.floorMod(tickCount + getId(), Scp020Mold.BreatheTicks) == 0) {
            Scp020Mold.breathe(this);
        }
        if (EatLeft > 0) {
            EatLeft--;
            if (EatLeft % 8 == 0) {
                playLayers(TheodoreRegistry.EatLayers, 0.8f, 0.9f + random.nextFloat() * 0.2f);
                Vec3 Mouth = getEyePosition().add(getLookAngle().scale(0.35)).subtract(0.0, 0.2, 0.0);
                World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(TheodoreRegistry.Arsenic.get())), Mouth.x, Mouth.y, Mouth.z, 4, 0.08, 0.05, 0.08, 0.05);
            }
            if (EatLeft == 0) {
                finishEating(World);
            }
        }
        if (EmergeLeft > 0) {
            EmergeLeft--;
            if (EmergeLeft == 0 && action() == ActionEmerging) {
                setAction(ActionNone);
            }
        }
        if (Eater) {
            eaterTick(World);
        } else if (MergePoint != null && World.getGameTime() - MergeSeen > RecallTicks) {
            MergePoint = null;
            if (action() == ActionMerging) {
                setAction(ActionNone);
            }
            setState(Cluster == null ? StateWandering : StateClone);
        }
    }

    private void finishEating(ServerLevel World) {
        setAction(ActionNone);
        Cluster = getUUID();
        Eater = true;
        NextClone = FirstClone;
        setState(StateCloning);
        entityData.set(CountData, 1);
        World.sendParticles(Poison, getX(), getY() + 1.4, getZ(), 12, 0.25, 0.3, 0.25, 0.0);
        playSound(TheodoreRegistry.Eat.get(), 1.0f, 0.6f);
    }

    private void eaterTick(ServerLevel World) {
        if (Merging) {
            mergeTick(World);
            return;
        }
        if (NextClone > 0) {
            NextClone--;
            return;
        }
        NextClone = CloneMin + random.nextInt(CloneSpread);
        List<Theodore> Members = TheodoreClusters.members(World, this);
        entityData.set(CountData, Members.size());
        if (Members.size() >= ClusterSize) {
            if (TheodoreClusters.canMerge(World)) {
                beginMerge(World, Members);
            }
            return;
        }
        if (EatLeft > 0 || TheodoreClusters.population(World) >= TheodoreClusters.WorldCap) {
            return;
        }
        if (spawnClone(World)) {
            entityData.set(CountData, Members.size() + 1);
        }
    }

    private boolean spawnClone(ServerLevel World) {
        Theodore Clone = TheodoreRegistry.TheodoreType.get().create(World);
        if (Clone == null || Cluster == null) {
            return false;
        }
        double Angle = random.nextDouble() * Math.PI * 2.0;
        double Dx = Math.cos(Angle);
        double Dz = Math.sin(Angle);
        Clone.moveTo(getX() + Dx * 0.3, getY(), getZ() + Dz * 0.3, random.nextFloat() * 360.0f, 0.0f);
        if (!World.noCollision(Clone)) {
            Clone.moveTo(getX(), getY(), getZ(), getYRot(), 0.0f);
        }
        Clone.yHeadRot = Clone.getYRot();
        Clone.yBodyRot = Clone.getYRot();
        Clone.joinCluster(Cluster);
        Clone.equipUniform();
        Clone.emerge();
        Clone.setDeltaMovement(Dx * 0.2, 0.28, Dz * 0.2);
        if (!World.addFreshEntity(Clone)) {
            return false;
        }
        float Pitch = 0.9f + random.nextFloat() * 0.25f;
        playLayers(TheodoreRegistry.SplitLayers, 1.0f, Pitch);
        World.sendParticles(Flesh, getX(), getY() + 1.0, getZ(), 18, 0.35, 0.5, 0.35, 0.0);
        World.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.9, getZ(), 10, 0.3, 0.4, 0.3, 0.05);
        World.sendParticles(ParticleTypes.POOF, Clone.getX(), Clone.getY() + 0.6, Clone.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
        return true;
    }

    private void beginMerge(ServerLevel World, List<Theodore> Members) {
        Vec3 Sum = Vec3.ZERO;
        for (Theodore Member : Members) {
            Sum = Sum.add(Member.position());
        }
        Vec3 Centre = Sum.scale(1.0 / Members.size());
        MergePoint = new Vec3(Centre.x, getY(), Centre.z);
        Merging = true;
        MergeTicks = 0;
        SquashLeft = 0;
        setState(StateMerging);
        playSound(TheodoreRegistry.MergeGrunt.get(), 1.4f, 0.7f);
    }

    private void abortMerge() {
        Merging = false;
        MergeTicks = 0;
        SquashLeft = 0;
        MergePoint = null;
        if (action() == ActionMerging) {
            setAction(ActionNone);
        }
        setState(StateCloning);
    }

    private void mergeTick(ServerLevel World) {
        Vec3 Point = MergePoint;
        List<Theodore> Members = TheodoreClusters.members(World, this);
        entityData.set(CountData, Members.size());
        if (Point == null || Members.size() < ClusterSize) {
            abortMerge();
            return;
        }
        MergeTicks++;
        boolean Gathered = true;
        long Time = World.getGameTime();
        for (Theodore Member : Members) {
            Member.callTo(Point, Time);
            if (Member.position().distanceToSqr(Point) > MergeReach * MergeReach) {
                Gathered = false;
            }
        }
        if (SquashLeft == 0 && (Gathered || MergeTicks > GatherLimit)) {
            SquashLeft = SquashTicks;
            for (Theodore Member : Members) {
                Member.squash();
            }
            playSound(TheodoreRegistry.MergeGrunt.get(), 1.6f, 0.6f);
        }
        if (SquashLeft <= 0) {
            return;
        }
        SquashLeft--;
        for (Theodore Member : Members) {
            Vec3 Pull = Point.subtract(Member.position()).multiply(0.12, 0.0, 0.12);
            Member.setDeltaMovement(Pull.x, Member.getDeltaMovement().y, Pull.z);
        }
        if (SquashLeft % 5 == 0) {
            float Rise = 1.0f - SquashLeft / (float) SquashTicks;
            World.sendParticles(Flesh, Point.x, Point.y + 1.0, Point.z, 24, 0.8, 0.7, 0.8, 0.0);
            World.sendParticles(ParticleTypes.ITEM_SLIME, Point.x, Point.y + 0.8, Point.z, 8, 0.7, 0.5, 0.7, 0.05);
            World.playSound(null, Point.x, Point.y + 1.0, Point.z, TheodoreRegistry.MergeGrunt.get(), SoundSource.HOSTILE, 1.0f, 0.55f + Rise * 0.5f);
        }
        if (SquashLeft == 0) {
            fuse(World, Point, Members);
        }
    }

    private void fuse(ServerLevel World, Vec3 Point, List<Theodore> Members) {
        TheodoreAmalgamation Mass = TheodoreRegistry.AmalgamationType.get().create(World);
        if (Mass == null) {
            abortMerge();
            return;
        }
        Mass.moveTo(Point.x, Point.y, Point.z, getYRot(), 0.0f);
        Mass.finalizeSpawn(World, World.getCurrentDifficultyAt(Mass.blockPosition()), MobSpawnType.CONVERSION, null);
        Mass.rise();
        for (Theodore Member : Members) {
            Member.discard();
        }
        World.addFreshEntity(Mass);
        World.sendParticles(ParticleTypes.FLASH, Point.x, Point.y + 1.5, Point.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(ParticleTypes.EXPLOSION, Point.x, Point.y + 1.2, Point.z, 3, 0.6, 0.6, 0.6, 0.0);
        World.sendParticles(Flesh, Point.x, Point.y + 1.5, Point.z, 80, 1.2, 1.0, 1.2, 0.0);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : TheodoreRegistry.MergeLayers) {
            World.playSound(null, Point.x, Point.y + 1.5, Point.z, Layer.get(), SoundSource.HOSTILE, 3.0f, 0.6f);
        }
    }

    private void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY() + 1.0, getZ(), Layer.get(), SoundSource.NEUTRAL, Volume, Pitch);
        }
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 1.0f;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return TheodoreRegistry.Mutter.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return TheodoreRegistry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TheodoreRegistry.Death.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (Cluster != null) {
            Saved.putUUID("Cluster", Cluster);
        }
        Saved.putBoolean("Eater", Eater);
        Saved.putInt("NextClone", NextClone);
        Saved.putInt("EatLeft", EatLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        setAppearance(DClassAppearance.Theodore);
        Cluster = Saved.hasUUID("Cluster") ? Saved.getUUID("Cluster") : null;
        Eater = Cluster != null && Saved.getBoolean("Eater");
        NextClone = Saved.getInt("NextClone");
        EatLeft = Mth.clamp(Saved.getInt("EatLeft"), 0, EatTicks);
        if (EatLeft > 0) {
            setAction(ActionEating);
            setState(StateEating);
        } else {
            setState(Eater ? StateCloning : Cluster != null ? StateClone : StateWandering);
        }
    }
}
