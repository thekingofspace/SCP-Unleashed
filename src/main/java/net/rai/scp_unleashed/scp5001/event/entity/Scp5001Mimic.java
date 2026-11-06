package net.rai.scp_unleashed.scp5001.event.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.playerclone.PlayerSamples;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.voice.VoiceArchive;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class Scp5001Mimic extends Monster {
    public static final ResourceLocation StalkId = Scp5001EventRegistry.location("scp5001_mimic_stalk");
    public static final double Watched = -0.55;
    public static final double Unwatched = 0.45;
    public static final double WatchDot = 0.94;
    public static final int VoiceMin = 500;
    public static final int VoiceSpread = 700;
    private static final EntityDataAccessor<CompoundTag> Sample = SynchedEntityData.defineId(Scp5001Mimic.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<String> Source = SynchedEntityData.defineId(Scp5001Mimic.class, EntityDataSerializers.STRING);
    @Nullable
    private UUID SourceId;
    @Nullable
    private CompoundTag Decoded;
    @Nullable
    private ResolvableProfile Profile;
    private int Freeze;
    private int VoiceIn = VoiceMin;
    public boolean Phantom;
    public int PhantomLife;
    @Nullable
    public LivingEntity Puppet;

    public Scp5001Mimic(EntityType<? extends Scp5001Mimic> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 28.0).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(Sample, new CompoundTag());
        Builder.define(Source, "");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Scp5001Mimic.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void copy(LivingEntity Victim) {
        if (Victim instanceof Player Donor) {
            entityData.set(Sample, PlayerSamples.write(new ResolvableProfile(Donor.getGameProfile())));
            SourceId = Donor.getUUID();
            for (EquipmentSlot Slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack Worn = Donor.getItemBySlot(Slot);
                if (!Worn.isEmpty()) {
                    setItemSlot(Slot, Worn.copyWithCount(1));
                    setDropChance(Slot, 0.0f);
                }
            }
            setCustomName(Donor.getName().copy());
            setCustomNameVisible(true);
        } else {
            entityData.set(Source, BuiltInRegistries.ENTITY_TYPE.getKey(Victim.getType()).toString());
            refreshDimensions();
            AttributeInstance Health = getAttribute(Attributes.MAX_HEALTH);
            if (Health != null) {
                Health.setBaseValue(Math.max(10.0, Math.min(60.0, Victim.getMaxHealth() * 1.2)));
            }
        }
        setHealth(getMaxHealth());
        moveTo(Victim.getX(), Victim.getY(), Victim.getZ(), Victim.getYRot(), Victim.getXRot());
        setYHeadRot(Victim.getYHeadRot());
        setYBodyRot(Victim.yBodyRot);
    }

    public void copyProfile(@Nullable ResolvableProfile Value) {
        entityData.set(Sample, PlayerSamples.write(Value));
    }

    public static boolean copiable(LivingEntity Victim) {
        return Victim instanceof Player || Victim instanceof Mob && !(Victim instanceof Scp610Creature) && !(Victim instanceof Scp5001Mimic) && !(Victim instanceof ArmorStand);
    }

    @Nullable
    public static Scp5001Mimic rise(ServerLevel World, LivingEntity Victim) {
        if (!copiable(Victim)) {
            return null;
        }
        Scp5001Mimic Made = Scp5001EventRegistry.MimicType.get().create(World);
        if (Made == null) {
            return null;
        }
        Made.copy(Victim);
        if (!World.addFreshEntity(Made)) {
            return null;
        }
        World.sendParticles(Scp5001Vessel.FieldLight, Victim.getX(), Victim.getY() + Victim.getBbHeight() * 0.5, Victim.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        World.playSound(null, Victim.getX(), Victim.getY(), Victim.getZ(), Scp5001EventRegistry.Unmade.get(), SoundSource.HOSTILE, 1.2f, 0.9f);
        return Made;
    }

    @Nullable
    public ResolvableProfile profile() {
        CompoundTag Current = entityData.get(Sample);
        if (Current != Decoded) {
            Decoded = Current;
            Profile = PlayerSamples.read(Current);
        }
        return Profile;
    }

    public String source() {
        return entityData.get(Source);
    }

    public boolean mimicsMob() {
        return !source().isEmpty();
    }

    @Nullable
    public EntityType<?> sourceType() {
        String Id = source();
        if (Id.isEmpty()) {
            return null;
        }
        ResourceLocation Key = ResourceLocation.tryParse(Id);
        return Key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(Key).orElse(null);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose Stance) {
        EntityType<?> Type = sourceType();
        if (Type != null) {
            return Type.getDimensions().scale(1.08f, 1.12f);
        }
        return super.getDefaultDimensions(Stance);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (Source.equals(Key)) {
            refreshDimensions();
            Puppet = null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (Puppet != null) {
                Puppet.tickCount = tickCount;
                Puppet.walkAnimation.update(walkAnimation.speed(), 1.0f);
            }
            if (Phantom && --PhantomLife <= 0) {
                discard();
            }
            return;
        }
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (tickCount % 20 == 0) {
            Scp5001State.Phase Now = Scp5001State.get(World.getServer()).phase();
            if (Now == Scp5001State.Phase.Dormant || Now == Scp5001State.Phase.Resetting) {
                World.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.5, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                World.sendParticles(Scp5001Vessel.FieldLight, getX(), getY() + getBbHeight() * 0.5, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                discard();
                return;
            }
        }
        stalk();
        if (Freeze > 0) {
            Freeze--;
            getNavigation().stop();
            if (getTarget() != null) {
                getLookControl().setLookAt(getTarget(), 60.0f, 60.0f);
            }
        } else if (getTarget() != null && random.nextInt(260) == 0) {
            Freeze = 20 + random.nextInt(30);
        }
        if (SourceId != null && --VoiceIn <= 0) {
            VoiceIn = VoiceMin + random.nextInt(VoiceSpread);
            VoiceArchive.replay(Scp5001EventRegistry.VoiceId, this, Optional.of(SourceId), Optional.empty(), 0.92f);
        }
    }

    private void stalk() {
        AttributeInstance Speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed == null) {
            return;
        }
        LivingEntity Prey = getTarget();
        double Wanted = 0.0;
        if (Prey != null) {
            Vec3 Look = Prey.getViewVector(1.0f).normalize();
            Vec3 Toward = position().add(0.0, getBbHeight() * 0.6, 0.0).subtract(Prey.getEyePosition()).normalize();
            boolean Seen = Look.dot(Toward) > WatchDot && Prey.hasLineOfSight(this);
            Wanted = Seen ? Watched : Unwatched;
        }
        AttributeModifier Current = Speed.getModifier(StalkId);
        if (Current == null || Current.amount() != Wanted) {
            Speed.removeModifier(StalkId);
            if (Wanted != 0.0) {
                Speed.addTransientModifier(new AttributeModifier(StalkId, Wanted, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    public boolean frozen() {
        return Freeze > 0;
    }

    @Override
    public boolean shouldShowName() {
        return !mimicsMob() && hasCustomName();
    }

    @Override
    public Component getName() {
        return hasCustomName() ? super.getName() : getTypeName();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp5001EventRegistry.MimicAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp5001EventRegistry.MimicHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp5001EventRegistry.MimicDeath.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.put("Sample", entityData.get(Sample).copy());
        Tag.putString("Source", source());
        if (SourceId != null) {
            Tag.putUUID("SourceId", SourceId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(Sample, Tag.getCompound("Sample").copy());
        entityData.set(Source, Tag.getString("Source"));
        SourceId = Tag.hasUUID("SourceId") ? Tag.getUUID("SourceId") : null;
        refreshDimensions();
    }
}
