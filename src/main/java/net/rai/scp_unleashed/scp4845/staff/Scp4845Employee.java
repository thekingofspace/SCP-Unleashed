package net.rai.scp_unleashed.scp4845.staff;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.HumanoidGaitController;
import net.rai.scp_unleashed.scp4845.order.Scp4845Orders;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

public class Scp4845Employee extends DClassLike {
    public static final float Width = 0.6f;
    public static final float Height = 1.8f;
    public static final float EyeHeight = 1.62f;
    public static final double MaxHealth = 20.0;
    public static final double MoveSpeed = 0.3;
    public static final int TransitionTicks = 5;
    public static final int ReactCooldown = 30;
    public static final byte RoleCounter = 0;
    public static final byte RoleKitchen = 1;
    public static final byte RoleManager = 2;
    public static final byte RoleDrifter = 3;
    public static final int Looks = 4;
    public static final int Names = 12;
    public static final byte ManagerName = -1;
    private static final EntityDataAccessor<Byte> RoleData = SynchedEntityData.defineId(Scp4845Employee.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> LookData = SynchedEntityData.defineId(Scp4845Employee.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> TaskData = SynchedEntityData.defineId(Scp4845Employee.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> TaskStartData = SynchedEntityData.defineId(Scp4845Employee.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> NameData = SynchedEntityData.defineId(Scp4845Employee.class, EntityDataSerializers.BYTE);

    private Scp4845Brain Brain;
    private boolean Bound;
    @Nullable
    private BlockPos Home;
    private long ReactReady;

    public Scp4845Employee(EntityType<? extends Scp4845Employee> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(RoleData, RoleDrifter);
        Builder.define(LookData, (byte) 0);
        Builder.define(TaskData, Scp4845Task.None.Id);
        Builder.define(TaskStartData, 0);
        Builder.define(NameData, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, brain());
    }

    public Scp4845Brain brain() {
        if (Brain == null) {
            Brain = new Scp4845Brain(this);
        }
        return Brain;
    }

    public byte role() {
        return entityData.get(RoleData);
    }

    public int look() {
        return entityData.get(LookData);
    }

    public int nameIndex() {
        return entityData.get(NameData);
    }

    public boolean isManager() {
        return role() == RoleManager;
    }

    public boolean isBound() {
        return Bound;
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public void setup(byte Role, int Look, int Name, boolean Hired) {
        entityData.set(RoleData, Role);
        entityData.set(LookData, (byte) Look);
        entityData.set(NameData, (byte) Name);
        Bound = Hired;
        Home = blockPosition();
        brain().reset();
    }

    public Scp4845Task task() {
        return Scp4845Task.byId(entityData.get(TaskData));
    }

    public int taskStart() {
        return entityData.get(TaskStartData);
    }

    public void setTask(Scp4845Task Task) {
        if (task() != Task) {
            entityData.set(TaskData, Task.Id);
            entityData.set(TaskStartData, (int) level().getGameTime());
        }
    }

    public Component speakerName() {
        if (nameIndex() == ManagerName) {
            return Component.translatable("entity.scp_unleashed.scp4845_employee.name.manager");
        }
        return Component.translatable("entity.scp_unleashed.scp4845_employee.name." + Math.floorMod(nameIndex(), Names));
    }

    public Component roleName() {
        String Key = switch (role()) {
            case RoleCounter -> "counter";
            case RoleKitchen -> "kitchen";
            case RoleManager -> "manager";
            default -> "drifter";
        };
        return Component.translatable("entity.scp_unleashed.scp4845_employee.role." + Key);
    }

    public void handOver(int Ticks) {
        brain().flash(Scp4845Task.HandOver, Ticks);
    }

    @Override
    protected RawAnimation bodyAnimation() {
        Scp4845Task Current = task();
        if (Current != Scp4845Task.None && (Current.Seated || !isStriding())) {
            return Current.Animation;
        }
        return super.bodyAnimation();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new HumanoidGaitController<>(this, "body", TransitionTicks, Event -> Event.setAndContinue(bodyAnimation())));
    }

    @Override
    public boolean eyesShut() {
        return !isAlive();
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Data) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Data);
        if (!Bound) {
            setup(RoleDrifter, getRandom().nextInt(Looks), getRandom().nextInt(Names), false);
        }
        return Result;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(Source);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        if (level() instanceof ServerLevel World && Source.getEntity() instanceof ServerPlayer Attacker && World.getGameTime() >= ReactReady) {
            ReactReady = World.getGameTime() + ReactCooldown;
            Scp4845Lines.say(this, Attacker, Scp4845Lines.Hurt);
            brain().notice(Attacker);
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND || User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (User instanceof ServerPlayer Customer) {
            return Scp4845Orders.interact(this, Customer) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isAlive() && getHealth() < getMaxHealth()) {
            setHealth(getMaxHealth());
        }
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
    public void checkDespawn() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(Entity Vehicle) {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return Other instanceof Scp4845Employee || super.isAlliedTo(Other);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putByte("Role", role());
        Saved.putByte("Look", (byte) look());
        Saved.putByte("Name", (byte) nameIndex());
        Saved.putBoolean("Bound", Bound);
        if (Home != null) {
            Saved.put("Home", NbtUtils.writeBlockPos(Home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        entityData.set(RoleData, Saved.getByte("Role"));
        entityData.set(LookData, Saved.getByte("Look"));
        entityData.set(NameData, Saved.getByte("Name"));
        Bound = Saved.getBoolean("Bound");
        Home = NbtUtils.readBlockPos(Saved, "Home").orElse(null);
        brain().reset();
    }
}
