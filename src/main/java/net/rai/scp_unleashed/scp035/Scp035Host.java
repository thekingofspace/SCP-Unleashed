package net.rai.scp_unleashed.scp035;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp035Host extends Monster {
    public static final double Health = 30.0;
    public static final double Damage = 5.0;
    public static final double Speed = 0.26;
    public static final double Armour = 2.0;
    public static final double Follow = 32.0;
    public static final double Steadiness = 0.2;
    public static final int DecayTicks = 48000;
    public static final int DecaySync = 100;
    public static final int IdleInterval = 220;
    public static final int LookLimit = 16384;
    public static final String TexturesKey = "textures";
    public static final List<String> Stripped = List.of("Brain", "Offers", "Inventory", "Gossips", "attributes", "Attributes", "active_effects", "ActiveEffects",
            "leash", "Leash", "Pos", "Motion", "Rotation", "UUID", "Passengers", "HandDropChances", "ArmorDropChances", "body_armor_drop_chance", "body_armor_item",
            "ArmorItems", "HandItems", "Xp", "LastRestock", "RestocksToday", "LastGossipDecay", "Tags", "NeoForgeData", "neoforge:attachments", "ForgeCaps",
            "ForgeData", "DeathLootTable", "DeathLootTableSeed", "Owner", "Trusted", "AngerTime", "AngryAt", "anger", "listener", "CustomName",
            "CustomNameVisible", "HurtByTimestamp", "Health", "HurtTime", "DeathTime", "Fire", "Air", "FallDistance", "PortalCooldown", "PersistenceRequired",
            "CanPickUpLoot", "Invulnerable", "NoAI", "Silent", "Glowing", "Pack", "Selected", "Food", "XpLevel", "XpProgress", "XpTotal", "Incarnation",
            "Resting");

    private static final EntityDataAccessor<String> SkinName = SynchedEntityData.defineId(Scp035Host.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> SkinId = SynchedEntityData.defineId(Scp035Host.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> SkinTextures = SynchedEntityData.defineId(Scp035Host.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SkinSignature = SynchedEntityData.defineId(Scp035Host.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> VictimType = SynchedEntityData.defineId(Scp035Host.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> VictimLook = SynchedEntityData.defineId(Scp035Host.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> Decay = SynchedEntityData.defineId(Scp035Host.class, EntityDataSerializers.FLOAT);

    private long Age;
    @Nullable
    private Entity Dummy;
    private boolean DummyFailed;

    public Scp035Host(EntityType<? extends Scp035Host> Type, Level World) {
        super(Type, World);
        setDropChance(EquipmentSlot.HEAD, 0.0f);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, Health)
                .add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.ARMOR, Armour)
                .add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.KNOCKBACK_RESISTANCE, Steadiness);
    }

    @Nullable
    public static Scp035Host rise(ServerLevel World, ServerPlayer Victim, ItemStack Mask) {
        Scp035Host Host = Scp035Registry.Host.get().create(World);
        if (Host == null) {
            Scp035Mask.place(World, Victim.position(), Victim.getYRot(), Mask, true);
            return null;
        }
        Host.adoptPlayer(Victim);
        return arrive(World, Host, Victim, Mask);
    }

    @Nullable
    public static Scp035Host rise(ServerLevel World, Mob Victim, ItemStack Mask, boolean Replace) {
        Scp035Host Host = Scp035Registry.Host.get().create(World);
        if (Host == null) {
            Scp035Mask.place(World, Victim.position(), Victim.getYRot(), Mask, true);
            return null;
        }
        Host.adoptVictim(Victim);
        if (Replace) {
            Victim.discard();
        }
        return arrive(World, Host, Victim, Mask);
    }

    @Nullable
    private static Scp035Host arrive(ServerLevel World, Scp035Host Host, LivingEntity Victim, ItemStack Mask) {
        Host.moveTo(Victim.getX(), Victim.getY(), Victim.getZ(), Victim.getYRot(), Victim.getXRot());
        Host.yHeadRot = Victim.yHeadRot;
        Host.yBodyRot = Victim.yBodyRot;
        Host.setItemSlot(EquipmentSlot.HEAD, Scp035MaskItem.is(Mask) ? Mask.copyWithCount(1) : Scp035MaskItem.fresh());
        Host.finalizeSpawn(World, World.getCurrentDifficultyAt(Host.blockPosition()), MobSpawnType.CONVERSION, null);
        if (!World.addFreshEntity(Host)) {
            Scp035Mask.place(World, Victim.position(), Victim.getYRot(), Mask, true);
            return null;
        }
        Vec3 Head = Host.getEyePosition();
        World.sendParticles(ParticleTypes.SMOKE, Head.x, Head.y - 0.4, Head.z, 18, 0.25, 0.5, 0.25, 0.01);
        World.sendParticles(Scp035Registry.Weep.get(), Head.x, Head.y, Head.z, 8, 0.12, 0.1, 0.12, 0.0);
        World.playSound(null, Head.x, Head.y, Head.z, Scp035Registry.Rise.get(), SoundSource.HOSTILE, 1.0f, 0.9f + World.random.nextFloat() * 0.1f);
        return Host;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(SkinName, "");
        Builder.define(SkinId, Optional.empty());
        Builder.define(SkinTextures, "");
        Builder.define(SkinSignature, "");
        Builder.define(VictimType, "");
        Builder.define(VictimLook, new CompoundTag());
        Builder.define(Decay, 0.0f);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, Target -> Scp035Sight.isPerson(Target)));
    }

    public void adoptPlayer(ServerPlayer Source) {
        GameProfile Profile = Source.getGameProfile();
        entityData.set(SkinName, Profile.getName() == null ? "" : Profile.getName());
        entityData.set(SkinId, Optional.of(Source.getUUID()));
        Property Textures = Profile.getProperties().get(TexturesKey).stream().findFirst().orElse(null);
        entityData.set(SkinTextures, Textures == null ? "" : Textures.value());
        entityData.set(SkinSignature, Textures == null || Textures.signature() == null ? "" : Textures.signature());
        setCustomName(Component.translatable("entity.scp_unleashed.scp035_host.named", Source.getName()));
    }

    public void adoptVictim(Mob Source) {
        entityData.set(VictimType, BuiltInRegistries.ENTITY_TYPE.getKey(Source.getType()).toString());
        CompoundTag Saved = new CompoundTag();
        try {
            Source.saveWithoutId(Saved);
        } catch (RuntimeException Problem) {
            Saved = new CompoundTag();
        }
        for (String Key : Stripped) {
            Saved.remove(Key);
        }
        entityData.set(VictimLook, Saved.sizeInBytes() > LookLimit ? new CompoundTag() : Saved);
        setBaby(Source.isBaby());
        if (Source.hasCustomName() || Scp035Sight.isPerson(Source)) {
            setCustomName(Component.translatable("entity.scp_unleashed.scp035_host.named", Source.getName()));
        }
    }

    public String getSkinName() {
        return entityData.get(SkinName);
    }

    public Optional<UUID> getSkinId() {
        return entityData.get(SkinId);
    }

    public String getSkinTextures() {
        return entityData.get(SkinTextures);
    }

    public String getSkinSignature() {
        return entityData.get(SkinSignature);
    }

    public String getVictimType() {
        return entityData.get(VictimType);
    }

    public CompoundTag getVictimLook() {
        return entityData.get(VictimLook);
    }

    public float decay() {
        return entityData.get(Decay);
    }

    public boolean wasPlayer() {
        return getSkinId().isPresent();
    }

    @Nullable
    public Entity dummy() {
        return Dummy;
    }

    public void setDummy(@Nullable Entity Shown) {
        Dummy = Shown;
    }

    public boolean dummyFailed() {
        return DummyFailed;
    }

    public void markDummyFailed() {
        DummyFailed = true;
        Dummy = null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        Age++;
        if (Age % DecaySync == 0L) {
            entityData.set(Decay, Math.min(1.0f, (float) Age / DecayTicks));
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setCanPickUpLoot(false);
        return Group;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
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
    public int getAmbientSoundInterval() {
        return IdleInterval;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp035Registry.HostIdle.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp035Registry.HostHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp035Registry.HostDeath.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putLong("HostAge", Age);
        Tag.putString("SkinName", getSkinName());
        getSkinId().ifPresent(Found -> Tag.putUUID("SkinId", Found));
        Tag.putString("SkinTextures", getSkinTextures());
        Tag.putString("SkinSignature", getSkinSignature());
        Tag.putString("VictimType", getVictimType());
        Tag.put("VictimLook", getVictimLook().copy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Age = Tag.getLong("HostAge");
        entityData.set(SkinName, Tag.getString("SkinName"));
        entityData.set(SkinId, Tag.hasUUID("SkinId") ? Optional.of(Tag.getUUID("SkinId")) : Optional.empty());
        entityData.set(SkinTextures, Tag.getString("SkinTextures"));
        entityData.set(SkinSignature, Tag.getString("SkinSignature"));
        entityData.set(VictimType, Tag.getString("VictimType"));
        entityData.set(VictimLook, Tag.getCompound("VictimLook"));
        entityData.set(Decay, Math.min(1.0f, (float) Age / DecayTicks));
    }
}
