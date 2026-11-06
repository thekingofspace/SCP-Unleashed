package net.rai.scp_unleashed.dclass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.rai.scp_unleashed.npc.doors.NpcDoorNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.BodyAttachments;
import net.rai.scp_unleashed.control.BodyState;
import net.rai.scp_unleashed.control.ControlRoster;
import net.rai.scp_unleashed.control.Controllable;
import net.rai.scp_unleashed.guards.GuardGoals;
import net.rai.scp_unleashed.dclass.duty.DClassDutyGoals;
import net.rai.scp_unleashed.labware.LabContamination;
import net.rai.scp_unleashed.scp012.Scp012Registry;
import net.rai.scp_unleashed.scp020.Scp020Mold;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DClass extends DClassLike implements Controllable {
    public static final int MainSize = 36;
    public static final int HotbarSize = 9;
    public static final int ArmorSize = 4;
    public static final double ReachSqr = 64.0;
    public static final int ShieldTicks = 50;
    private static final EntityDataAccessor<Byte> TraitData = SynchedEntityData.defineId(DClass.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> NumberData = SynchedEntityData.defineId(DClass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> SelectedData = SynchedEntityData.defineId(DClass.class, EntityDataSerializers.BYTE);

    private final NonNullList<ItemStack> Main = NonNullList.withSize(MainSize, ItemStack.EMPTY);
    private final NonNullList<ItemStack> Armor = NonNullList.withSize(ArmorSize, ItemStack.EMPTY);
    private ItemStack Offhand = ItemStack.EMPTY;
    private final DClassFood Food = new DClassFood();
    private final DClassExposure Exposure = new DClassExposure();
    private final DClassPack Pack = new DClassPack(this);
    private int XpLevel;
    private float XpProgress;
    private int XpTotal;
    private DClassCrime Crime = DClassCrime.Smuggling;
    private int Incarnation;
    private int Iq;
    private int Athletic;
    private int ShieldTimer;
    private double LastX;
    private double LastZ;
    private boolean WakeCheckPending;
    private boolean Identified;

    public DClass(EntityType<? extends DClass> Type, Level World) {
        super(Type, World);
        setCanPickUpLoot(true);
        setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation Ground) {
            Ground.setCanOpenDoors(true);
            Ground.setCanPassDoors(true);
        }
        GuardGoals.install(this, goalSelector, targetSelector);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new NpcDoorNavigation(this, World);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.ATTACK_SPEED, 4.0).add(Attributes.LUCK, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(TraitData, (byte) 0);
        Builder.define(NumberData, 0);
        Builder.define(SelectedData, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new DClassCompulsion(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(1, new DClassGoals.Panic(this));
        goalSelector.addGoal(2, new DClassDutyGoals.Orders(this));
        goalSelector.addGoal(2, new DClassDutyGoals.Follow(this));
        goalSelector.addGoal(4, new DClassDutyGoals.Janitor(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
            @Override
            public boolean canUse() {
                return trait().fightsBack() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, Target -> Target.distanceTo(this) <= trait().avoidRange(), 12.0f, 0.75, 1.2,
                Target -> !trait().fightsBack() || getHealth() < getMaxHealth() * 0.5f));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, Target -> trait() == DClassTrait.Paranoid && Target.distanceTo(this) < 3.5f, 4.0f, 0.7, 0.9,
                Target -> !((Player) Target).isSpectator()));
        goalSelector.addGoal(3, new DClassGoals.Sleep(this));
        goalSelector.addGoal(4, new DClassGoals.Eat(this));
        goalSelector.addGoal(5, new DClassGoals.FollowPlayer(this));
        goalSelector.addGoal(5, new DClassGoals.SeekItems(this));
        goalSelector.addGoal(6, new DClassGoals.Approach(this));
        goalSelector.addGoal(7, new DClassGoals.Stroll(this));
        goalSelector.addGoal(8, new DClassGoals.Watch(this));
        goalSelector.addGoal(9, new DClassGoals.Glance(this));
        targetSelector.addGoal(1, new DClassGoals.Defend(this));
    }

    public void identify(DClassTrait Trait, DClassCrime NewCrime, int Appearance) {
        entityData.set(TraitData, (byte) Trait.ordinal());
        Crime = NewCrime;
        setAppearance(Appearance);
        Identified = true;
        if (Iq <= 0) {
            Iq = DClassScores.rollIq(random);
        }
        if (Athletic <= 0) {
            Athletic = DClassScores.rollAthletic(random);
        }
    }

    public void setScores(int NewIq, int NewAthletic) {
        Iq = NewIq;
        Athletic = NewAthletic;
    }

    public int iq() {
        return Iq;
    }

    public int athletic() {
        return Athletic;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Data) {
        if (!Identified) {
            identify(DClassTrait.random(random), DClassCrime.random(random), DClassAppearance.random(random));
        }
        return super.finalizeSpawn(World, Difficulty, Reason, Data);
    }

    public DClassTrait trait() {
        return DClassTrait.byIndex(entityData.get(TraitData));
    }

    public DClassCrime crime() {
        return Crime;
    }

    public int number() {
        return entityData.get(NumberData);
    }

    public boolean keepsRecords() {
        return true;
    }

    public String designation() {
        return DClassNumbers.format(number());
    }

    public int assignNumber() {
        if (number() <= 0 && level() instanceof ServerLevel World) {
            entityData.set(NumberData, DClassNumbers.get(World.getServer()).next(random));
        }
        return number();
    }

    public DClassFood food() {
        return Food;
    }

    public DClassPack pack() {
        return Pack;
    }

    public int selected() {
        return Mth.clamp(entityData.get(SelectedData), 0, HotbarSize - 1);
    }

    public DClassExposure exposure() {
        return Exposure;
    }

    public NonNullList<ItemStack> main() {
        return Main;
    }

    public int xpLevel() {
        return XpLevel;
    }

    @Override
    public float glanceRate() {
        return trait().glanceRate();
    }

    @Override
    public Component getName() {
        if (hasCustomName()) {
            return super.getName();
        }
        return number() > 0 ? Component.literal(DClassNumbers.format(number())) : super.getName();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot Slot) {
        return switch (Slot) {
            case MAINHAND -> Main.get(selected());
            case OFFHAND -> Offhand;
            case FEET, LEGS, CHEST, HEAD -> Armor.get(Slot.getIndex());
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot Slot, ItemStack Stack) {
        verifyEquippedItem(Stack);
        switch (Slot) {
            case MAINHAND -> onEquipItem(Slot, Main.set(selected(), Stack), Stack);
            case OFFHAND -> {
                ItemStack Old = Offhand;
                Offhand = Stack;
                onEquipItem(Slot, Old, Stack);
            }
            case FEET, LEGS, CHEST, HEAD -> onEquipItem(Slot, Armor.set(Slot.getIndex(), Stack), Stack);
            default -> {
            }
        }
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Armor;
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return List.of(Main.get(selected()), Offhand);
    }

    @Override
    public Iterable<ItemStack> getArmorAndBodyArmorSlots() {
        return Armor;
    }

    public void setSelected(int Slot) {
        entityData.set(SelectedData, (byte) Mth.clamp(Slot, 0, HotbarSize - 1));
    }

    @Override
    public boolean wantsToPickUp(ItemStack Stack) {
        return trait().picksUpItems() && canPickUpLoot() && !isRestingPose() && (wantsArmor(Stack) || roomFor(Stack));
    }

    @Override
    public boolean canHoldItem(ItemStack Stack) {
        return roomFor(Stack);
    }

    private boolean wantsArmor(ItemStack Stack) {
        EquipmentSlot Slot = getEquipmentSlotForItem(Stack);
        return Slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && betterArmor(Stack, getItemBySlot(Slot));
    }

    private static boolean betterArmor(ItemStack Offered, ItemStack Worn) {
        if (!(Offered.getItem() instanceof ArmorItem New)) {
            return false;
        }
        if (Worn.isEmpty()) {
            return true;
        }
        if (EnchantmentHelper.has(Worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return false;
        }
        if (!(Worn.getItem() instanceof ArmorItem Old)) {
            return false;
        }
        if (New.getDefense() != Old.getDefense()) {
            return New.getDefense() > Old.getDefense();
        }
        return New.getToughness() > Old.getToughness();
    }

    public boolean roomFor(ItemStack Stack) {
        for (ItemStack Held : Main) {
            if (Held.isEmpty() || ItemStack.isSameItemSameComponents(Held, Stack) && Held.getCount() < Held.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack insert(ItemStack Stack) {
        for (int Slot = 0; Slot < MainSize && !Stack.isEmpty(); Slot++) {
            ItemStack Held = Main.get(Slot);
            if (!Held.isEmpty() && ItemStack.isSameItemSameComponents(Held, Stack) && Held.getCount() < Held.getMaxStackSize()) {
                int Moved = Math.min(Stack.getCount(), Held.getMaxStackSize() - Held.getCount());
                Held.grow(Moved);
                Stack.shrink(Moved);
            }
        }
        for (int Slot = 0; Slot < MainSize && !Stack.isEmpty(); Slot++) {
            if (Main.get(Slot).isEmpty()) {
                if (Slot == selected()) {
                    setItemSlot(EquipmentSlot.MAINHAND, Stack.copyAndClear());
                } else {
                    Main.set(Slot, Stack.copyAndClear());
                }
            }
        }
        return Stack;
    }

    @Override
    protected void pickUpItem(ItemEntity Drop) {
        ItemStack Stack = Drop.getItem();
        ItemStack Copy = Stack.copy();
        int Before = Copy.getCount();
        if (wantsArmor(Copy)) {
            EquipmentSlot Slot = getEquipmentSlotForItem(Copy);
            ItemStack Old = getItemBySlot(Slot);
            setItemSlot(Slot, Copy.split(1));
            if (!Old.isEmpty()) {
                ItemStack Left = insert(Old);
                if (!Left.isEmpty()) {
                    spawnAtLocation(Left);
                }
            }
        }
        ItemStack Rest = insert(Copy);
        int Taken = Before - Rest.getCount();
        if (Taken <= 0) {
            return;
        }
        onItemPickup(Drop);
        take(Drop, Taken);
        Stack.shrink(Taken);
        if (Stack.isEmpty()) {
            Drop.discard();
        } else {
            Drop.setItem(Stack);
        }
    }

    public int findFood(boolean Desperate) {
        int Best = -1;
        int BestValue = -1;
        for (int Slot = 0; Slot < MainSize; Slot++) {
            ItemStack Stack = Main.get(Slot);
            FoodProperties Properties = Stack.getFoodProperties(this);
            if (Properties == null) {
                continue;
            }
            boolean Harmful = Properties.effects().stream().anyMatch(Effect -> !Effect.effect().getEffect().value().isBeneficial());
            if (Harmful && !Desperate) {
                continue;
            }
            int Value = Properties.nutrition() * 10 + Math.round(Properties.saturation() * 4) - (Harmful ? 100 : 0) + (Slot < HotbarSize ? 1 : 0);
            if (Value > BestValue) {
                BestValue = Value;
                Best = Slot;
            }
        }
        return Best;
    }

    public void beginEating(int Slot) {
        if (Slot < 0 || Slot >= MainSize) {
            return;
        }
        if (Slot < HotbarSize) {
            setSelected(Slot);
        } else {
            ItemStack Food = Main.get(Slot);
            Main.set(Slot, Main.get(selected()));
            Main.set(selected(), Food);
        }
        startUsingItem(InteractionHand.MAIN_HAND);
    }

    @Override
    public ItemStack eat(Level World, ItemStack Stack, FoodProperties Properties) {
        Food.eat(Properties);
        World.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.5f, World.random.nextFloat() * 0.1f + 0.9f);
        return super.eat(World, Stack, Properties);
    }

    public boolean isBedtime() {
        long Day = level().getDayTime() % 24000L;
        int Bed = trait().bedtime();
        int Wake = trait().wakeTime();
        return Bed < Wake ? Day >= Bed && Day < Wake : Day >= Bed || Day < Wake;
    }

    public void lieDown() {
        getNavigation().stop();
        setResting(true);
    }

    public void wake() {
        if (isSleeping()) {
            stopSleeping();
        }
        setResting(false);
    }

    @Nullable
    @Override
    public Direction getBedOrientation() {
        BlockPos Where = getSleepingPos().orElse(null);
        if (Where != null && !level().getBlockState(Where).isBed(level(), Where, this)) {
            DClassBeds.Spot Found = DClassBeds.spotAt(level(), Where);
            if (Found != null) {
                return Found.Toward();
            }
        }
        return super.getBedOrientation();
    }

    @Override
    public void stopSleeping() {
        DClassBeds.Spot Found = DClassBeds.sleepingSpot(this);
        super.stopSleeping();
        if (Found != null && !Found.Vanilla()) {
            Vec3 Stand = BedBlock.findStandUpPosition(getType(), level(), Found.Head(), Found.Toward(), getYRot())
                    .orElseGet(() -> Vec3.atBottomCenterOf(Found.Head().above()));
            setPos(Stand.x, Stand.y, Stand.z);
            setXRot(0.0f);
        }
        if (level() instanceof ServerLevel World) {
            DClassBeds.release(World, this);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (WakeCheckPending) {
            WakeCheckPending = false;
            if ((isSleeping() || isResting()) && !isBedtime()) {
                wake();
            }
        }
        Food.tick(this);
        tickPack();
        Exposure.tick(this);
        LabContamination.tick(this);
        if (Math.floorMod(tickCount + getId(), Scp020Mold.BreatheTicks) == 0) {
            Scp020Mold.breathe(this);
        }
        double Moved = Math.sqrt(Mth.square(getX() - LastX) + Mth.square(getZ() - LastZ));
        LastX = getX();
        LastZ = getZ();
        if (Moved > 0.0 && Moved < 2.0) {
            if (isSwimming() || isInWater()) {
                Food.addExhaustion(0.01f * (float) Moved);
            } else if (onGround() && isSprinting()) {
                Food.addExhaustion(0.1f * (float) Moved);
            }
        }
        if (isResting()) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
        if (isSleeping() && tickCount % 20 == 0 && level() instanceof ServerLevel World) {
            DClassBeds.Spot Found = DClassBeds.sleepingSpot(this);
            if (Found != null && !Found.Vanilla()) {
                DClassBeds.claim(World, Found, this);
            }
        }
        if (ShieldTimer > 0 && --ShieldTimer == 0 && isUsingItem() && getUseItem().getUseAnimation() == UseAnim.BLOCK) {
            stopUsingItem();
        }
        if (!onGround() && fallDistance > 3.5f && !isFallFlying() && getItemBySlot(EquipmentSlot.CHEST).canElytraFly(this) && !hasEffect(MobEffects.LEVITATION) && !isInWater()) {
            setSharedFlag(7, true);
        }
    }

    private void tickPack() {
        Level World = level();
        int Held = selected();
        for (int Slot = 0; Slot < MainSize; Slot++) {
            ItemStack Stack = Main.get(Slot);
            if (!Stack.isEmpty()) {
                Stack.inventoryTick(World, this, Slot, Slot == Held);
            }
        }
        for (int Slot = 0; Slot < ArmorSize; Slot++) {
            ItemStack Stack = Armor.get(Slot);
            if (!Stack.isEmpty()) {
                Stack.inventoryTick(World, this, Slot, false);
            }
        }
        if (!Offhand.isEmpty()) {
            Offhand.inventoryTick(World, this, 0, false);
        }
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        if (!level().isClientSide) {
            Food.addExhaustion(isSprinting() ? 0.2f : 0.05f);
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (Hit) {
            Food.addExhaustion(0.1f);
            swing(InteractionHand.MAIN_HAND);
        }
        return Hit;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && isResting()) {
            setResting(false);
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide && isAlive() && trait().fightsBack() && Source.getEntity() instanceof LivingEntity && !isUsingItem()) {
            raiseShield();
        }
        return Hurt;
    }

    private void raiseShield() {
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Held = getItemInHand(Hand);
            if (Held.getUseAnimation() == UseAnim.BLOCK) {
                startUsingItem(Hand);
                ShieldTimer = ShieldTicks;
                return;
            }
        }
    }

    @Override
    protected void actuallyHurt(DamageSource Source, float Amount) {
        super.actuallyHurt(Source, Amount);
        if (!isInvulnerableTo(Source)) {
            Food.addExhaustion(Source.getFoodExhaustion());
        }
    }

    @Override
    protected void hurtArmor(DamageSource Source, float Amount) {
        doHurtEquipment(Source, Amount, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    }

    @Override
    protected void hurtHelmet(DamageSource Source, float Amount) {
        doHurtEquipment(Source, Amount, EquipmentSlot.HEAD);
    }

    @Override
    protected void hurtCurrentlyUsedShield(float Amount) {
        if (Amount < 3.0f || !(level() instanceof ServerLevel World)) {
            return;
        }
        InteractionHand Hand = getUsedItemHand();
        EquipmentSlot Slot = Hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        ItemStack Shield = getUseItem();
        Shield.hurtAndBreak(1 + Mth.floor(Amount), World, this, Item -> {
            onEquippedItemBroken(Item, Slot);
            stopUsingItem();
        });
        if (Shield.isEmpty()) {
            setItemSlot(Slot, ItemStack.EMPTY);
            playSound(SoundEvents.SHIELD_BREAK, 0.8f, 0.8f + random.nextFloat() * 0.4f);
        }
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        for (int Slot = 0; Slot < MainSize; Slot++) {
            dropStack(Main.get(Slot));
            Main.set(Slot, ItemStack.EMPTY);
        }
        for (int Slot = 0; Slot < ArmorSize; Slot++) {
            dropStack(Armor.get(Slot));
            Armor.set(Slot, ItemStack.EMPTY);
        }
        dropStack(Offhand);
        Offhand = ItemStack.EMPTY;
    }

    private void dropStack(ItemStack Stack) {
        if (Stack.isEmpty() || EnchantmentHelper.has(Stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
            return;
        }
        ItemEntity Dropped = spawnAtLocation(Stack, 0.5f);
        if (Dropped != null) {
            Dropped.setDeltaMovement(Dropped.getDeltaMovement().add((random.nextFloat() - random.nextFloat()) * 0.1f, random.nextFloat() * 0.05f, (random.nextFloat() - random.nextFloat()) * 0.1f));
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel World, DamageSource Source, boolean RecentlyHit) {
    }

    @Override
    public boolean isAlwaysExperienceDropper() {
        return true;
    }

    @Override
    protected int getBaseExperienceReward() {
        return Math.min(XpLevel * 7, 100);
    }

    @Override
    public void die(DamageSource Source) {
        boolean Before = dead;
        super.die(Source);
        if (!Before && dead && level() instanceof ServerLevel World) {
            ControlRoster.get(World.getServer()).dead(getUUID());
            DClassInventoryMenu.closeFor(this);
            removeData(Scp012Registry.Composer.get());
            Exposure.forget();
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (!level().isClientSide) {
            DClassInventoryMenu.closeFor(this);
        }
        if (level() instanceof ServerLevel World) {
            DClassBeds.release(World, this);
        }
        super.remove(Reason);
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand == InteractionHand.MAIN_HAND && User.isSecondaryUseActive() && User.getItemInHand(Hand).isEmpty() && isAlive()) {
            if (User instanceof ServerPlayer Server) {
                Server.openMenu(new SimpleMenuProvider((Id, Inventory, Viewer) -> new DClassInventoryMenu(Id, Inventory, this),
                        Component.translatable("container.scp_unleashed.dclass", getDisplayName())), Buffer -> Buffer.writeVarInt(getId()));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(User, Hand);
    }

    public boolean stillValidFor(Player User) {
        return isAlive() && !isRemoved() && User.distanceToSqr(this) <= ReachSqr;
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
        return isRestingPose() ? DClassRegistry.Snore.get() : DClassRegistry.Mutter.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return isRestingPose() ? 100 : trait() == DClassTrait.Cheerful ? 160 : 320;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return DClassRegistry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DClassRegistry.Death.get();
    }

    @Override
    public CompoundTag saveBody(HolderLookup.Provider Registries) {
        CompoundTag Body = new CompoundTag();
        Body.put(BodyState.InventoryKey, BodyState.saveItems(Registries, Main, Armor, Offhand));
        Body.putInt(BodyState.SelectedKey, selected());
        Body.putFloat(BodyState.HealthKey, getHealth());
        Body.putFloat(BodyState.AbsorptionKey, getAbsorptionAmount());
        Body.put(BodyState.FoodKey, Food.save());
        Body.putInt(BodyState.LevelKey, XpLevel);
        Body.putFloat(BodyState.ProgressKey, XpProgress);
        Body.putInt(BodyState.TotalKey, XpTotal);
        Body.put(BodyState.EffectsKey, BodyState.saveEffects(this));
        Body.putInt(BodyState.AirKey, getAirSupply());
        Body.putInt(BodyState.FireKey, getRemainingFireTicks());
        Body.putInt(BodyState.FrozenKey, getTicksFrozen());
        Body.putInt(BodyState.ArrowsKey, getArrowCount());
        Body.put(BodyState.AttachmentsKey, BodyAttachments.capture(this));
        return Body;
    }

    @Override
    public void loadBody(CompoundTag Body, HolderLookup.Provider Registries) {
        if (isUsingItem()) {
            stopUsingItem();
        }
        Offhand = BodyState.loadItems(Registries, Body.getList(BodyState.InventoryKey, Tag.TAG_COMPOUND), Main, Armor);
        setSelected(Body.getInt(BodyState.SelectedKey));
        Food.load(Body.getCompound(BodyState.FoodKey));
        XpLevel = Math.max(0, Body.getInt(BodyState.LevelKey));
        XpProgress = Mth.clamp(Body.getFloat(BodyState.ProgressKey), 0.0f, 1.0f);
        XpTotal = Math.max(0, Body.getInt(BodyState.TotalKey));
        BodyState.loadEffects(this, Body.getList(BodyState.EffectsKey, Tag.TAG_COMPOUND));
        BodyAttachments.apply(this, Body.getCompound(BodyState.AttachmentsKey));
        setAirSupply(Body.contains(BodyState.AirKey) ? Body.getInt(BodyState.AirKey) : getMaxAirSupply());
        setRemainingFireTicks(Body.getInt(BodyState.FireKey));
        setTicksFrozen(Body.getInt(BodyState.FrozenKey));
        setArrowCount(Body.getInt(BodyState.ArrowsKey));
        setHealth(Body.contains(BodyState.HealthKey) ? Body.getFloat(BodyState.HealthKey) : getMaxHealth());
        setAbsorptionAmount(Body.getFloat(BodyState.AbsorptionKey));
    }

    @Override
    public CompoundTag saveIdentity() {
        CompoundTag Identity = new CompoundTag();
        Identity.putInt("Number", number());
        Identity.putString("Trait", trait().getId());
        Identity.putString("Crime", Crime.getId());
        Identity.putInt("Appearance", appearance());
        Identity.putInt("Iq", Iq);
        Identity.putInt("Athletic", Athletic);
        if (hasCustomName()) {
            Identity.putString("CustomName", Component.Serializer.toJson(getCustomName(), registryAccess()));
        }
        return Identity;
    }

    @Override
    public void loadIdentity(CompoundTag Identity) {
        setScores(Identity.getInt("Iq"), Identity.getInt("Athletic"));
        identify(DClassTrait.byId(Identity.getString("Trait")), DClassCrime.byId(Identity.getString("Crime")), Identity.getInt("Appearance"));
        entityData.set(NumberData, Identity.getInt("Number"));
        if (Identity.contains("CustomName")) {
            setCustomName(Component.Serializer.fromJson(Identity.getString("CustomName"), registryAccess()));
        }
    }

    @Override
    public Component controlName() {
        return Component.literal(DClassNumbers.format(number()));
    }

    @Override
    public int controlIncarnation() {
        return Incarnation;
    }

    @Override
    public void setControlIncarnation(int Value) {
        Incarnation = Value;
    }

    @Override
    public void beforeControl(ServerPlayer User) {
        wake();
        DClassInventoryMenu.closeFor(this);
        if (isUsingItem()) {
            stopUsingItem();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.put("Pack", BodyState.saveItems(registryAccess(), Main, Armor, Offhand));
        Saved.putInt("Selected", selected());
        Saved.put("Food", Food.save());
        Saved.putInt("XpLevel", XpLevel);
        Saved.putFloat("XpProgress", XpProgress);
        Saved.putInt("XpTotal", XpTotal);
        Saved.putString("Trait", trait().getId());
        Saved.putString("Crime", Crime.getId());
        Saved.putInt("Number", number());
        Saved.putInt("Incarnation", Incarnation);
        Saved.putInt("Iq", Iq);
        Saved.putInt("Athletic", Athletic);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Offhand = BodyState.loadItems(registryAccess(), Saved.getList("Pack", Tag.TAG_COMPOUND), Main, Armor);
        setSelected(Saved.getInt("Selected"));
        Food.load(Saved.getCompound("Food"));
        XpLevel = Saved.getInt("XpLevel");
        XpProgress = Saved.getFloat("XpProgress");
        XpTotal = Saved.getInt("XpTotal");
        setScores(Saved.getInt("Iq"), Saved.getInt("Athletic"));
        if (Saved.contains("Trait")) {
            identify(DClassTrait.byId(Saved.getString("Trait")), DClassCrime.byId(Saved.getString("Crime")), appearance());
        }
        entityData.set(NumberData, Saved.getInt("Number"));
        Incarnation = Saved.getInt("Incarnation");
        LastX = getX();
        LastZ = getZ();
        WakeCheckPending = true;
    }

    public static boolean isFiled(Entity Target) {
        return Target instanceof DClass Subject && Subject.number() > 0;
    }
}
