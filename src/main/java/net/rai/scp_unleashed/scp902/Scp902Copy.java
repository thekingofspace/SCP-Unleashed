package net.rai.scp_unleashed.scp902;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.playerclone.PlayerSamples;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class Scp902Copy extends LivingEntity {
    public static final int CheckTicks = 40;
    public static final String OwnerKey = "Owner";
    public static final String SampleKey = "Sample";
    public static final String HandsKey = "HandItems";
    public static final String ArmourKey = "ArmorItems";
    public static final String LeftKey = "LeftHanded";
    private static final EntityDataAccessor<Optional<UUID>> OwnerData = SynchedEntityData.defineId(Scp902Copy.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<CompoundTag> SampleData = SynchedEntityData.defineId(Scp902Copy.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> LeftData = SynchedEntityData.defineId(Scp902Copy.class, EntityDataSerializers.BOOLEAN);
    private final NonNullList<ItemStack> Hands = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> Armour = NonNullList.withSize(4, ItemStack.EMPTY);
    @Nullable
    private CompoundTag Decoded;
    @Nullable
    private ResolvableProfile Profile;

    public Scp902Copy(EntityType<? extends Scp902Copy> Type, Level World) {
        super(Type, World);
        setNoGravity(true);
        setInvulnerable(true);
        noCulling = false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public static Scp902Copy of(ServerPlayer Source) {
        Scp902Copy Made = of(Source, Source.level());
        Made.moveTo(Source.getX(), Source.getY(), Source.getZ(), Source.getYRot(), Source.getXRot());
        Made.setYHeadRot(Source.getYHeadRot());
        Made.setYBodyRot(Source.yBodyRot);
        Made.yBodyRotO = Source.yBodyRot;
        Made.yHeadRotO = Source.getYHeadRot();
        return Made;
    }

    public void stand(Vec3 Spot, float Yaw, float Pitch) {
        moveTo(Spot.x, Spot.y, Spot.z, Yaw, Pitch);
        setYHeadRot(Yaw);
        setYBodyRot(Yaw);
        yBodyRotO = Yaw;
        yHeadRotO = Yaw;
        setDeltaMovement(Vec3.ZERO);
    }

    public static Scp902Copy of(ServerPlayer Source, Level Home) {
        Scp902Copy Made = new Scp902Copy(Scp902Registry.Copy.get(), Home);
        GameProfile Known = Source.getGameProfile();
        GameProfile Copied = new GameProfile(Known.getId(), Known.getName());
        Copied.getProperties().putAll(Known.getProperties());
        Made.entityData.set(OwnerData, Optional.of(Source.getUUID()));
        Made.entityData.set(SampleData, PlayerSamples.write(new ResolvableProfile(Copied)));
        Made.entityData.set(LeftData, Source.getMainArm() == HumanoidArm.LEFT);
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            if (Slot.getType() == EquipmentSlot.Type.HAND || Slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                Made.dress(Slot, Source.getItemBySlot(Slot));
            }
        }
        return Made;
    }

    public static ItemStack visual(ItemStack Source) {
        return Source.isEmpty() ? ItemStack.EMPTY : Source.copyWithCount(1);
    }

    private void dress(EquipmentSlot Slot, ItemStack Source) {
        ItemStack Shown = visual(Source);
        switch (Slot.getType()) {
            case HAND -> Hands.set(Slot.getIndex(), Shown);
            case HUMANOID_ARMOR -> Armour.set(Slot.getIndex(), Shown);
            default -> {
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(OwnerData, Optional.empty());
        Builder.define(SampleData, new CompoundTag());
        Builder.define(LeftData, false);
    }

    @Nullable
    public UUID owner() {
        return entityData.get(OwnerData).orElse(null);
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

    public String ownerName() {
        return PlayerSamples.nameOf(profile());
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return Hands;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Armour;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot Slot) {
        return switch (Slot.getType()) {
            case HAND -> Hands.get(Slot.getIndex());
            case HUMANOID_ARMOR -> Armour.get(Slot.getIndex());
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot Slot, ItemStack Stack) {
        if (level().isClientSide) {
            dress(Slot, Stack);
        }
    }

    @Override
    public boolean canTakeItem(ItemStack Stack) {
        return false;
    }

    @Override
    public HumanoidArm getMainArm() {
        return entityData.get(LeftData) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    @Override
    public void tick() {
        setPose(Pose.STANDING);
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel World && (tickCount + getId()) % CheckTicks == 0 && !Scp902State.get(World.getServer()).wants(owner(), getUUID())) {
            discard();
        }
    }

    @Override
    public void travel(Vec3 Motion) {
    }

    @Override
    public void move(MoverType Type, Vec3 Motion) {
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return true;
    }

    @Override
    public void kill() {
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
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
    protected void doPush(Entity Other) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity Attacker) {
        return true;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
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
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition Transition) {
        return null;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    protected boolean canRide(Entity Vehicle) {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        UUID Owner = owner();
        if (Owner != null) {
            Saved.putUUID(OwnerKey, Owner);
        }
        Saved.put(SampleKey, entityData.get(SampleData).copy());
        Saved.putBoolean(LeftKey, entityData.get(LeftData));
        Saved.put(HandsKey, writeStacks(Hands));
        Saved.put(ArmourKey, writeStacks(Armour));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        entityData.set(OwnerData, Saved.hasUUID(OwnerKey) ? Optional.of(Saved.getUUID(OwnerKey)) : Optional.empty());
        entityData.set(SampleData, Saved.getCompound(SampleKey).copy());
        entityData.set(LeftData, Saved.getBoolean(LeftKey));
        readStacks(Saved.getList(HandsKey, Tag.TAG_COMPOUND), Hands);
        readStacks(Saved.getList(ArmourKey, Tag.TAG_COMPOUND), Armour);
        setNoGravity(true);
        setInvulnerable(true);
    }

    private ListTag writeStacks(NonNullList<ItemStack> Stacks) {
        ListTag Written = new ListTag();
        for (ItemStack Stack : Stacks) {
            Written.add(Stack.isEmpty() ? new CompoundTag() : Stack.save(registryAccess()));
        }
        return Written;
    }

    private void readStacks(ListTag Written, NonNullList<ItemStack> Stacks) {
        for (int Slot = 0; Slot < Stacks.size(); Slot++) {
            CompoundTag Entry = Slot < Written.size() ? Written.getCompound(Slot) : new CompoundTag();
            Stacks.set(Slot, Entry.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registryAccess(), Entry));
        }
    }
}
