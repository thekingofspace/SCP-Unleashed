package net.rai.scp_unleashed.scp008;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class Scp008Zombie extends Zombie {
    public static final int Lifespan = 24000;
    public static final int CollapseWarning = 200;
    public static final int CheckTicks = 20;
    public static final double Health = 24.0;
    public static final double Damage = 4.0;
    public static final double Speed = 0.25;
    public static final double Armour = 2.0;
    public static final double Follow = 32.0;
    public static final String TexturesKey = "textures";

    private static final EntityDataAccessor<String> SkinName = SynchedEntityData.defineId(Scp008Zombie.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> SkinId = SynchedEntityData.defineId(Scp008Zombie.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> SkinTextures = SynchedEntityData.defineId(Scp008Zombie.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SkinSignature = SynchedEntityData.defineId(Scp008Zombie.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> Collapsing = SynchedEntityData.defineId(Scp008Zombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FromVillager = SynchedEntityData.defineId(Scp008Zombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> BodyLook = SynchedEntityData.defineId(Scp008Zombie.class, EntityDataSerializers.COMPOUND_TAG);

    private long BornAt = -1L;

    public Scp008Zombie(EntityType<? extends Scp008Zombie> Type, Level World) {
        super(Type, World);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, Health)
                .add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.ARMOR, Armour)
                .add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(SkinName, "");
        Builder.define(SkinId, Optional.empty());
        Builder.define(SkinTextures, "");
        Builder.define(SkinSignature, "");
        Builder.define(Collapsing, false);
        Builder.define(FromVillager, false);
        Builder.define(BodyLook, new CompoundTag());
    }

    public void adoptProfile(Player Source) {
        GameProfile Profile = Source.getGameProfile();
        entityData.set(SkinName, Profile.getName() == null ? "" : Profile.getName());
        entityData.set(SkinId, Optional.of(Source.getUUID()));
        Property Textures = Profile.getProperties().get(TexturesKey).stream().findFirst().orElse(null);
        entityData.set(SkinTextures, Textures == null ? "" : Textures.value());
        entityData.set(SkinSignature, Textures == null || Textures.signature() == null ? "" : Textures.signature());
        setCustomName(Component.translatable("entity.scp_unleashed.scp008_zombie.named", Source.getName()));
    }

    public void adoptVillager(AbstractVillager Source) {
        entityData.set(FromVillager, true);
        setBaby(Source.isBaby());
        if (Source.hasCustomName()) {
            setCustomName(Component.translatable("entity.scp_unleashed.scp008_zombie.named", Source.getCustomName()));
        }
    }

    public void adoptBody(LivingEntity Source) {
        entityData.set(BodyLook, ControlBodies.lookOf(Source));
        setCustomName(Component.translatable("entity.scp_unleashed.scp008_zombie.named", Source.getName()));
    }

    public CompoundTag getBodyLook() {
        return entityData.get(BodyLook);
    }

    public boolean isFromVillager() {
        return entityData.get(FromVillager);
    }

    public boolean hasAdoptedLook() {
        return isFromVillager() || !getSkinName().isEmpty() || !getBodyLook().isEmpty();
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

    public boolean isCollapsing() {
        return entityData.get(Collapsing);
    }

    public int getRemaining() {
        if (BornAt < 0L) {
            return Lifespan;
        }
        return (int) Math.max(0L, Lifespan - (level().getGameTime() - BornAt));
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected boolean supportsBreakDoorGoal() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setCanPickUpLoot(false);
        BornAt = World.getLevel().getGameTime();
        return Group;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        long Now = level().getGameTime();
        if (BornAt < 0L) {
            BornAt = Now;
        }
        if ((tickCount + getId()) % CheckTicks != 0) {
            return;
        }
        long Age = Now - BornAt;
        if (Age >= Lifespan) {
            Scp008Events.collapse(this);
            return;
        }
        if (Age >= Lifespan - CollapseWarning && !isCollapsing()) {
            entityData.set(Collapsing, true);
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Landed = super.doHurtTarget(Target);
        if (Landed && !level().isClientSide && Target instanceof LivingEntity Victim) {
            Scp008Infection.infect(Victim);
        }
        return Landed;
    }

    @Override
    public boolean killedEntity(ServerLevel World, LivingEntity Victim) {
        if (Victim instanceof AbstractVillager) {
            return true;
        }
        return super.killedEntity(World, Victim);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp008Registry.ZombieAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp008Registry.ZombieHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp008Registry.ZombieDeath.get();
    }

    @Override
    protected SoundEvent getStepSound() {
        return Scp008Registry.ZombieStep.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putLong("BornAt", BornAt);
        Tag.putString("SkinName", getSkinName());
        getSkinId().ifPresent(Found -> Tag.putUUID("SkinId", Found));
        Tag.putString("SkinTextures", getSkinTextures());
        Tag.putString("SkinSignature", getSkinSignature());
        Tag.putBoolean("FromVillager", isFromVillager());
        Tag.put("BodyLook", getBodyLook());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        BornAt = Tag.contains("BornAt") ? Tag.getLong("BornAt") : -1L;
        entityData.set(SkinName, Tag.getString("SkinName"));
        entityData.set(SkinId, Tag.hasUUID("SkinId") ? Optional.of(Tag.getUUID("SkinId")) : Optional.empty());
        entityData.set(SkinTextures, Tag.getString("SkinTextures"));
        entityData.set(SkinSignature, Tag.getString("SkinSignature"));
        entityData.set(FromVillager, Tag.getBoolean("FromVillager"));
        entityData.set(BodyLook, Tag.getCompound("BodyLook"));
    }
}
