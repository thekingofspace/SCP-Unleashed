package net.rai.scp_unleashed.scp439;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp439Worker extends Scp439Creature {
    public static final float Width = 0.55f;
    public static final float Height = 0.4f;
    public static final float EyeHeight = 0.25f;
    public static final float RenderScale = 1.0f;
    public static final byte ActionBite = 2;
    public static final int BiteTicks = 10;
    public static final int BiteHitTick = 5;
    public static final int EmergeTicks = 24;
    public static final int DeathTicks = 24;
    public static final double ForageRadius = 32.0;
    public static final double PreySight = 16.0;
    public static final double ItemSight = 10.0;
    public static final double BiteReach = 0.6;
    public static final double DeliverReach = 1.6;
    public static final double DetourReach = 4.0;
    public static final double PickReach = 1.6;
    public static final double CloseApproach = 9.0;
    private static final EntityDataAccessor<ItemStack> CarryData = SynchedEntityData.defineId(Scp439Worker.class, EntityDataSerializers.ITEM_STACK);
    private static final RawAnimation CarryLoop = RawAnimation.begin().thenLoop("carry");
    private final Map<UUID, Long> Shunned = new HashMap<>();

    public Scp439Worker(EntityType<? extends Scp439Worker> Type, Level World) {
        super(Type, World);
        xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Scp439Creature.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 2.5)
                .add(Attributes.ARMOR, 1.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(CarryData, ItemStack.EMPTY);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp439Goals.Bite(this, 1.25));
        goalSelector.addGoal(2, new Scp439Goals.Deliver(this));
        goalSelector.addGoal(3, new Scp439Goals.Collect(this));
        goalSelector.addGoal(4, new Scp439Goals.Homeward(this, ForageRadius + 6.0, 1.1));
        goalSelector.addGoal(5, new Scp439Goals.Wander(this, 12, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(Scp439Worker.class, Scp439Brute.class));
        targetSelector.addGoal(2, new Scp439Goals.Forage(this));
    }

    public static boolean isPrey(LivingEntity Found) {
        if (!Found.isAlive() || Found.isRemoved() || Found.isInvulnerable() || !Targeting.isTargetable(Found) || isColony(Found) || Found instanceof Player || Found instanceof AbstractVillager
                || ControlBodies.isPersonLike(Found) || Found instanceof Creeper || Found instanceof EnderDragon || Found instanceof WitherBoss) {
            return false;
        }
        if (Found instanceof TamableAnimal Pet && Pet.isTame()) {
            return false;
        }
        return Found instanceof Animal || Found instanceof Enemy;
    }

    public ItemStack carried() {
        return entityData.get(CarryData);
    }

    public boolean isCarrying() {
        return !carried().isEmpty();
    }

    public void setCarried(ItemStack Stack) {
        entityData.set(CarryData, Stack.copy());
    }

    public boolean canTake(ItemStack Lying) {
        if (!Scp439Food.wanted(Lying)) {
            return false;
        }
        ItemStack Held = carried();
        return Held.isEmpty() || ItemStack.isSameItemSameComponents(Held, Lying) && Held.getCount() < Scp439Food.CarryLimit;
    }

    public void approach(ItemEntity Found, double Speed, boolean Repath) {
        PathNavigation Navigation = getNavigation();
        if (Navigation.isDone() && distanceToSqr(Found) <= CloseApproach) {
            getMoveControl().setWantedPosition(Found.getX(), Found.getY(), Found.getZ(), Speed);
            return;
        }
        if (Navigation.isDone() || Repath) {
            Path Route = Navigation.createPath(Found, 0);
            if (Route != null) {
                Navigation.moveTo(Route, Speed);
            }
        }
    }

    public boolean pickUp(ItemEntity Found) {
        ItemStack Lying = Found.getItem();
        if (!Found.isAlive() || !canTake(Lying)) {
            return false;
        }
        ItemStack Held = carried();
        int Taken = Math.min(Scp439Food.CarryLimit - Held.getCount(), Lying.getCount());
        setCarried(Held.isEmpty() ? Lying.copyWithCount(Taken) : Held.copyWithCount(Held.getCount() + Taken));
        Lying.shrink(Taken);
        if (Lying.isEmpty()) {
            Found.discard();
        } else {
            Found.setItem(Lying);
        }
        playLayers(Scp439Registry.WorkerPickup, 0.8f, 1.0f);
        return true;
    }

    public boolean deliver() {
        Scp439Hive Nest = hive();
        if (Nest == null || !isCarrying()) {
            return false;
        }
        Nest.feed(carried());
        setCarried(ItemStack.EMPTY);
        playLayers(Scp439Registry.WorkerDeliver, 0.9f, 1.0f);
        return true;
    }

    public boolean inBiteReach(LivingEntity Target) {
        return reachTo(Target) <= BiteReach && Math.abs(Target.getY() - getY()) < 1.5;
    }

    public void beginBite(LivingEntity Target) {
        faceToward(Target, 180.0f);
        setAction(ActionBite, BiteTicks);
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind != ActionBite) {
            return;
        }
        LivingEntity Target = getTarget();
        if (Target == null) {
            return;
        }
        faceToward(Target, 40.0f);
        if (Elapsed == BiteHitTick && reachTo(Target) <= BiteReach + 0.5) {
            playLayers(Scp439Registry.WorkerBite, 0.8f, 1.0f);
            strike(Target, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    @Override
    public boolean locksMovement() {
        return isEmerging();
    }

    @Override
    public void die(DamageSource Source) {
        if (!level().isClientSide && isCarrying()) {
            spawnAtLocation(carried());
            setCarried(ItemStack.EMPTY);
        }
        super.die(Source);
    }

    @Override
    protected Scp439Stride[] strides() {
        return new Scp439Stride[]{Scp439Gaits.WorkerWalk, Scp439Gaits.WorkerRun};
    }

    @Override
    protected double runEnter() {
        return 0.11;
    }

    @Override
    protected double runExit() {
        return 0.085;
    }

    @Override
    protected double idleSeconds() {
        return 2.5;
    }

    @Override
    public float renderScale() {
        return RenderScale;
    }

    @Override
    public String modelName() {
        return "scp439_worker";
    }

    @Override
    @Nullable
    protected String clipFor(byte Action) {
        return Action == ActionBite ? "bite" : null;
    }

    @Override
    protected int emergeTicks() {
        return EmergeTicks;
    }

    @Override
    protected int deathTicks() {
        return DeathTicks;
    }

    @Override
    protected byte memberKind() {
        return Scp439Colonies.Workers;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> idleSounds() {
        return Scp439Registry.WorkerIdle;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> hurtSounds() {
        return Scp439Registry.WorkerHurt;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> deathSounds() {
        return Scp439Registry.WorkerDeath;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> stepSounds() {
        return Scp439Registry.WorkerStep;
    }

    @Override
    protected float stepVolume() {
        return 0.3f;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Nullable
    public ItemEntity nearestLoot(boolean Settling) {
        return nearestLoot(Settling, ItemSight);
    }

    @Nullable
    public ItemEntity detourLoot() {
        return isCarrying() ? nearestLoot(false, DetourReach) : null;
    }

    @Nullable
    public ItemEntity nearestLoot(boolean Settling, double Reach) {
        AABB Area = getBoundingBox().inflate(Reach, 4.0, Reach);
        long Now = level().getGameTime();
        ItemEntity Best = null;
        double BestDistance = Reach * Reach;
        for (ItemEntity Found : level().getEntitiesOfClass(ItemEntity.class, Area, Lying -> Lying.isAlive() && (Settling || !Lying.hasPickUpDelay())
                && canTake(Lying.getItem()) && nearHome(Lying.position()) && Shunned.getOrDefault(Lying.getUUID(), Long.MIN_VALUE) < Now)) {
            double Distance = distanceToSqr(Found);
            if (Distance < BestDistance) {
                Best = Found;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public void shun(ItemEntity Found, int Ticks) {
        long Now = level().getGameTime();
        Shunned.values().removeIf(Until -> Until < Now);
        Shunned.put(Found.getUUID(), Now + Ticks);
    }

    public boolean nearHome(Vec3 Spot) {
        return Home == null || Spot.distanceToSqr(Vec3.atBottomCenterOf(Home)) <= ForageRadius * ForageRadius;
    }

    @Override
    protected void extraControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "carry", 4, Event -> isCarrying() && !isActing() && deathTime <= 0 ? Event.setAndContinue(CarryLoop) : PlayState.STOP));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (isCarrying()) {
            Tag.put("Carry", carried().save(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setCarried(Tag.contains("Carry") ? ItemStack.parseOptional(registryAccess(), Tag.getCompound("Carry")) : ItemStack.EMPTY);
    }
}
