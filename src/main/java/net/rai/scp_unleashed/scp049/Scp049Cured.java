package net.rai.scp_unleashed.scp049;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp008.Scp008Zombie;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Scp049Cured extends Zombie implements VillagerDataHolder {
    public static final double Health = 24.0;
    public static final double Damage = 3.0;
    public static final double Speed = 0.21;
    public static final double Armour = 2.0;
    public static final double Follow = 32.0;
    public static final int RiseTicks = 30;
    public static final int MistEvery = 3;
    public static final int GuardRadius = 10;
    public static final double GuardAlert = 8.0;
    public static final int OrderTicks = 1200;
    public static final int ZombieOrderTicks = 400;
    public static final double ZombieLeash = 40.0;
    public static final double PlagueBane = 1.0;
    public static final ResourceLocation PlagueBaneId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_plague_bane");
    public static final byte LookPlain = 0;
    public static final byte LookVillager = 1;
    public static final byte LookPlayer = 2;
    public static final byte LookMimic = 3;
    public static final String TexturesKey = "textures";
    private static final Set<String> Stripped = Set.of("Pos", "Motion", "Rotation", "UUID", "FallDistance", "Fire", "Air", "OnGround", "Invulnerable", "PortalCooldown",
            "Passengers", "leash", "Leash", "Brain", "ActiveEffects", "active_effects", "HurtTime", "HurtByTimestamp", "DeathTime", "AbsorptionAmount", "Health",
            "neoforge:attachments", "NoAI", "PersistenceRequired", "Tags", "CustomNameVisible", "Glowing", "TicksFrozen", "SleepingX", "SleepingY", "SleepingZ",
            "attributes", "Attributes", "Silent", "NoGravity", "HasVisualFire", "FallFlying", "Offers", "Gossips", "Xp", "LastRestock", "LastGossipDecay",
            "RestocksToday", "AssignProfessionWhenSpawned");

    private static final EntityDataAccessor<Byte> LookData = SynchedEntityData.defineId(Scp049Cured.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<VillagerData> VillagerLookData = SynchedEntityData.defineId(Scp049Cured.class, EntityDataSerializers.VILLAGER_DATA);
    private static final EntityDataAccessor<String> SkinName = SynchedEntityData.defineId(Scp049Cured.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> SkinId = SynchedEntityData.defineId(Scp049Cured.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> SkinTextures = SynchedEntityData.defineId(Scp049Cured.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SkinSignature = SynchedEntityData.defineId(Scp049Cured.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MimicType = SynchedEntityData.defineId(Scp049Cured.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> MimicTag = SynchedEntityData.defineId(Scp049Cured.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> RiseStartData = SynchedEntityData.defineId(Scp049Cured.class, EntityDataSerializers.INT);

    @Nullable
    private UUID Master;
    @Nullable
    private BlockPos TrayPos;
    @Nullable
    private UUID OrderTarget;
    private long OrderExpires;
    private boolean OrderZombie;
    private int RiseLeft;
    private boolean Masterless;

    public Scp049Cured(EntityType<? extends Scp049Cured> Type, Level World) {
        super(Type, World);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.ATTACK_DAMAGE, Damage).add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.ARMOR, Armour).add(Attributes.FOLLOW_RANGE, Follow).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(LookData, LookPlain);
        Builder.define(VillagerLookData, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
        Builder.define(SkinName, "");
        Builder.define(SkinId, Optional.empty());
        Builder.define(SkinTextures, "");
        Builder.define(SkinSignature, "");
        Builder.define(MimicType, "");
        Builder.define(MimicTag, new CompoundTag());
        Builder.define(RiseStartData, -RiseTicks * 4);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp049CuredGoals.Rising(this));
        goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.9));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new Scp049CuredGoals.Ordered(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this, Scp049Doctor.class, Scp049Cured.class));
        targetSelector.addGoal(3, new Scp049CuredGoals.Guard(this));
        targetSelector.addGoal(4, new Scp049CuredGoals.Masterless(this));
    }

    @Override
    protected void addBehaviourGoals() {
    }

    public static CompoundTag lookOf(LivingEntity Victim) {
        CompoundTag Look = new CompoundTag();
        if (Victim.hasCustomName() && Victim.getCustomName() != null) {
            ComponentSerialization.CODEC.encodeStart(Victim.registryAccess().createSerializationContext(NbtOps.INSTANCE), Victim.getCustomName()).result()
                    .ifPresent(Encoded -> Look.put("Name", Encoded));
        }
        if (Victim instanceof Player User) {
            GameProfile Profile = User.getGameProfile();
            Look.putByte("Kind", LookPlayer);
            Look.putString("SkinName", Profile.getName() == null ? "" : Profile.getName());
            Look.putUUID("SkinId", User.getUUID());
            Property Textures = Profile.getProperties().get(TexturesKey).stream().findFirst().orElse(null);
            Look.putString("SkinTextures", Textures == null ? "" : Textures.value());
            Look.putString("SkinSignature", Textures == null || Textures.signature() == null ? "" : Textures.signature());
            ComponentSerialization.CODEC.encodeStart(Victim.registryAccess().createSerializationContext(NbtOps.INSTANCE), User.getName()).result()
                    .ifPresent(Encoded -> Look.put("Name", Encoded));
            return Look;
        }
        if (Victim instanceof Villager Patient) {
            Look.putByte("Kind", LookVillager);
            VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, Patient.getVillagerData()).result().ifPresent(Encoded -> Look.put("Villager", Encoded));
            return Look;
        }
        ResourceLocation Type = BuiltInRegistries.ENTITY_TYPE.getKey(Victim.getType());
        CompoundTag Body = Victim.saveWithoutId(new CompoundTag());
        for (String Key : Stripped) {
            Body.remove(Key);
        }
        Look.putByte("Kind", LookMimic);
        Look.putString("Type", Type.toString());
        Look.put("Body", Body);
        return Look;
    }

    public void adoptLook(CompoundTag Look) {
        byte Kind = Look.getByte("Kind");
        entityData.set(LookData, Kind);
        if (Kind == LookPlayer) {
            entityData.set(SkinName, Look.getString("SkinName"));
            entityData.set(SkinId, Look.hasUUID("SkinId") ? Optional.of(Look.getUUID("SkinId")) : Optional.empty());
            entityData.set(SkinTextures, Look.getString("SkinTextures"));
            entityData.set(SkinSignature, Look.getString("SkinSignature"));
        } else if (Kind == LookVillager && Look.contains("Villager")) {
            VillagerData.CODEC.parse(NbtOps.INSTANCE, Look.get("Villager")).result().ifPresent(this::setVillagerData);
        } else if (Kind == LookMimic) {
            entityData.set(MimicType, Look.getString("Type"));
            entityData.set(MimicTag, Look.getCompound("Body"));
        }
        if (Look.contains("Name")) {
            ComponentSerialization.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), Look.get("Name")).result()
                    .ifPresent(Name -> setCustomName(Component.translatable("entity.scp_unleashed.scp049_2.named", Name)));
        }
    }

    @Nullable
    public static Scp049Cured rise(ServerLevel World, CompoundTag Look, Vec3 Spot, float Yaw, @Nullable Scp049Doctor Doctor, @Nullable BlockPos Tray) {
        Scp049Cured Risen = Scp049Registry.Cured.get().create(World);
        if (Risen == null) {
            return null;
        }
        Risen.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Risen.setYHeadRot(Yaw);
        Risen.setYBodyRot(Yaw);
        Risen.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.MOB_SUMMONED, null);
        Risen.adoptLook(Look);
        if (Doctor != null) {
            Risen.bind(Doctor, Tray);
        }
        Risen.beginRise();
        Risen.setPersistenceRequired();
        if (!World.addFreshEntity(Risen)) {
            return null;
        }
        World.playSound(null, Spot.x, Spot.y, Spot.z, Scp049Registry.CuredRise.get(), SoundSource.HOSTILE, 1.0f, 0.9f + World.random.nextFloat() * 0.15f);
        World.sendParticles(ParticleTypes.HAPPY_VILLAGER, Spot.x, Spot.y + 0.3, Spot.z, 12, 0.5, 0.2, 0.5, 0.0);
        World.sendParticles(ParticleTypes.POOF, Spot.x, Spot.y + 0.2, Spot.z, 6, 0.4, 0.1, 0.4, 0.01);
        return Risen;
    }

    public void beginRise() {
        RiseLeft = RiseTicks;
        entityData.set(RiseStartData, tickCount);
    }

    public boolean isRising() {
        return RiseLeft > 0;
    }

    public float riseProgress(float Partial) {
        float Elapsed = tickCount - entityData.get(RiseStartData) + Partial;
        return Mth.clamp(Elapsed / RiseTicks, 0.0f, 1.0f);
    }

    public void bind(Scp049Doctor Doctor, @Nullable BlockPos Tray) {
        Master = Doctor.getUUID();
        Masterless = false;
        TrayPos = Tray != null ? Tray.immutable() : null;
        if (TrayPos != null) {
            restrictTo(TrayPos, GuardRadius);
        }
        Doctor.adopt(this);
    }

    public void orphan() {
        Master = null;
        Masterless = true;
        OrderTarget = null;
        OrderZombie = false;
    }

    public boolean isMasterless() {
        return Masterless;
    }

    @Nullable
    public UUID master() {
        return Master;
    }

    @Nullable
    public BlockPos trayPos() {
        return TrayPos;
    }

    public byte lookKind() {
        return entityData.get(LookData);
    }

    public boolean hasAdoptedLook() {
        return lookKind() != LookPlain;
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

    public String mimicType() {
        return entityData.get(MimicType);
    }

    public CompoundTag mimicTag() {
        return entityData.get(MimicTag);
    }

    @Override
    public VillagerData getVillagerData() {
        return entityData.get(VillagerLookData);
    }

    @Override
    public void setVillagerData(VillagerData Data) {
        entityData.set(VillagerLookData, Data);
    }

    public void order(LivingEntity Target) {
        if (!isValidOrder(Target)) {
            return;
        }
        OrderTarget = Target.getUUID();
        OrderZombie = false;
        OrderExpires = level().getGameTime() + OrderTicks;
        if (!isRising()) {
            setTarget(Target);
        }
    }

    public boolean orderAgainst(LivingEntity Zombie) {
        if (!isValidZombieOrder(Zombie) || hasPersonOrder()) {
            return false;
        }
        OrderTarget = Zombie.getUUID();
        OrderZombie = true;
        OrderExpires = level().getGameTime() + ZombieOrderTicks;
        if (!isRising()) {
            setTarget(Zombie);
        }
        return true;
    }

    public void recall() {
        if (OrderZombie) {
            return;
        }
        OrderTarget = null;
        LivingEntity Current = getTarget();
        if (Current != null && !nearTray(Current.position(), GuardAlert)) {
            setTarget(null);
        }
    }

    public boolean isValidOrder(@Nullable LivingEntity Target) {
        return Target != null && Scp049Persons.isPerson(Target, Scp049Persons.sparesPlayers(this)) && !Scp049Hold.isHeld(Target);
    }

    public boolean isValidZombieOrder(@Nullable LivingEntity Target) {
        if (Target == null || !Scp049Persons.isZombieFoe(Target)) {
            return false;
        }
        return TrayPos != null ? nearTray(Target.position(), ZombieLeash) : distanceToSqr(Target) <= ZombieLeash * ZombieLeash;
    }

    public boolean hasPersonOrder() {
        return !OrderZombie && orderTarget() != null;
    }

    public boolean isOrderedAgainst(Entity Target) {
        return OrderZombie && Target.getUUID().equals(OrderTarget) && orderTarget() != null;
    }

    @Nullable
    public LivingEntity orderTarget() {
        if (OrderTarget == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        if (World.getGameTime() > OrderExpires) {
            OrderTarget = null;
            return null;
        }
        if (!(World.getEntity(OrderTarget) instanceof LivingEntity Found) || !(OrderZombie ? isValidZombieOrder(Found) : isValidOrder(Found))) {
            OrderTarget = null;
            OrderZombie = false;
            return null;
        }
        return Found;
    }

    public boolean nearTray(Vec3 Point, double Range) {
        return TrayPos != null && Point.distanceToSqr(Vec3.atCenterOf(TrayPos)) <= Range * Range;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (isAlive() && riseProgress(0.0f) < 1.0f && tickCount % MistEvery == 0) {
                for (int Index = 0; Index < 2; Index++) {
                    level().addParticle(Scp049Registry.Miasma.get(), getX() + (random.nextDouble() - 0.5) * 0.9, getY() + 0.1 + random.nextDouble() * 0.4,
                            getZ() + (random.nextDouble() - 0.5) * 0.9, 0.0, 0.015 + random.nextDouble() * 0.01, 0.0);
                }
            }
            return;
        }
        if (!isAlive()) {
            return;
        }
        if (Scp008Infection.isInfected(this)) {
            Scp008Infection.cure(this);
        }
        if (RiseLeft > 0) {
            RiseLeft--;
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
        LivingEntity Current = getTarget();
        if (Current != null && Scp049Hold.isHeld(Current)) {
            setTarget(null);
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        AttributeInstance Strength = getAttribute(Attributes.ATTACK_DAMAGE);
        boolean Bane = Target instanceof Scp008Zombie && Strength != null && !Strength.hasModifier(PlagueBaneId);
        if (Bane) {
            Strength.addTransientModifier(new AttributeModifier(PlagueBaneId, PlagueBane, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        try {
            return super.doHurtTarget(Target);
        } finally {
            if (Bane) {
                Strength.removeModifier(PlagueBaneId);
            }
        }
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        if (Target instanceof Scp049Doctor || Target instanceof Scp049Cured || Scp049Hold.isHeld(Target)) {
            return false;
        }
        return super.canAttack(Target);
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return Other instanceof Scp049Doctor || Other instanceof Scp049Cured || super.isAlliedTo(Other);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setCanPickUpLoot(false);
        setBaby(false);
        return Group;
    }

    @Override
    public void setBaby(boolean Value) {
        super.setBaby(false);
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
    public boolean killedEntity(ServerLevel World, LivingEntity Victim) {
        if (Victim instanceof Scp008Zombie && Master != null && World.getEntity(Master) instanceof Scp049Doctor Doctor) {
            Doctor.notePlagueKill();
        }
        return true;
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

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp049Registry.CuredGroan.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp049Registry.CuredHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp049Registry.CuredDeath.get();
    }

    @Override
    protected SoundEvent getStepSound() {
        return Scp049Registry.CuredStep.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("Look", lookKind());
        VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, getVillagerData()).result().ifPresent(Encoded -> Tag.put("VillagerLook", Encoded));
        Tag.putString("SkinName", getSkinName());
        getSkinId().ifPresent(Found -> Tag.putUUID("SkinId", Found));
        Tag.putString("SkinTextures", getSkinTextures());
        Tag.putString("SkinSignature", getSkinSignature());
        Tag.putString("MimicType", mimicType());
        Tag.put("MimicTag", mimicTag().copy());
        if (Master != null) {
            Tag.putUUID("Master", Master);
        }
        Tag.putBoolean("Masterless", Masterless);
        if (TrayPos != null) {
            Tag.put("Tray", NbtUtils.writeBlockPos(TrayPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(LookData, Tag.getByte("Look"));
        if (Tag.contains("VillagerLook")) {
            VillagerData.CODEC.parse(NbtOps.INSTANCE, Tag.get("VillagerLook")).result().ifPresent(this::setVillagerData);
        }
        entityData.set(SkinName, Tag.getString("SkinName"));
        entityData.set(SkinId, Tag.hasUUID("SkinId") ? Optional.of(Tag.getUUID("SkinId")) : Optional.empty());
        entityData.set(SkinTextures, Tag.getString("SkinTextures"));
        entityData.set(SkinSignature, Tag.getString("SkinSignature"));
        entityData.set(MimicType, Tag.getString("MimicType"));
        entityData.set(MimicTag, Tag.getCompound("MimicTag"));
        Master = Tag.hasUUID("Master") ? Tag.getUUID("Master") : null;
        Masterless = Tag.getBoolean("Masterless");
        TrayPos = NbtUtils.readBlockPos(Tag, "Tray").orElse(null);
        if (TrayPos != null) {
            restrictTo(TrayPos, GuardRadius);
        }
    }
}
