package net.rai.scp_unleashed.scp009;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.SynchedEntityData;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp009Statue extends Entity implements IEntityWithComplexSpawn {
    public static final float DefaultWidth = 0.6f;
    public static final float DefaultHeight = 1.8f;
    public static final float BigVolume = 2.0f;
    public static final float FragmentsPerVolume = 4.0f;
    public static final int MaxFragments = 6;
    public static final int SmallHits = 2;
    public static final int BigHits = 3;
    public static final int GlintChance = 24;
    public static final int SnapshotLimit = 32768;
    public static final double Drag = 0.98;
    public static final double WaterDrag = 0.8;
    public static final float MaxStride = 0.6f;
    public static final List<String> Stripped = List.of("Brain", "Offers", "Inventory", "Gossips", "attributes", "Attributes", "active_effects", "ActiveEffects",
            "leash", "Leash", "Pos", "Motion", "Rotation", "UUID", "Passengers", "HandDropChances", "ArmorDropChances", "body_armor_drop_chance", "Xp",
            "LastRestock", "RestocksToday", "LastGossipDecay", "Tags", "NeoForgeData", "neoforge:attachments", "ForgeCaps", "ForgeData", "DeathLootTable",
            "DeathLootTableSeed", "Owner", "Trusted", "AngerTime", "AngryAt", "anger", "listener", "CustomName", "CustomNameVisible", "HurtByTimestamp",
            "Health", "Fire", "Air", "FallDistance", "PortalCooldown", "PersistenceRequired", "LeftHanded", "CanPickUpLoot", "Invulnerable");
    private ResourceLocation SourceType = ResourceLocation.withDefaultNamespace("pig");
    private CompoundTag Snapshot = new CompoundTag();
    private CompoundTag Equipment = new CompoundTag();
    private String SourceName = "";
    private boolean PlayerShaped;
    @Nullable
    private UUID ProfileId;
    private String ProfileName = "";
    private String SkinValue = "";
    private String SkinSignature = "";
    private float Width = DefaultWidth;
    private float Height = DefaultHeight;
    private float BodyYaw;
    private float HeadYaw;
    private float HeadPitch;
    private float LimbPosition;
    private float LimbSpeed;
    private float Swing;
    private boolean Crouching;
    private int Age;
    private long CreatedAt;
    private int Hits;
    @Nullable
    private Entity Dummy;
    private boolean DummyFailed;

    public Scp009Statue(EntityType<? extends Scp009Statue> Type, Level World) {
        super(Type, World);
    }

    public static Scp009Statue of(ServerLevel World, LivingEntity Dead) {
        Scp009Statue Statue = new Scp009Statue(Scp009Registry.Statue.get(), World);
        Statue.SourceType = BuiltInRegistries.ENTITY_TYPE.getKey(Dead.getType());
        Statue.Width = Dead.getBbWidth();
        Statue.Height = Dead.getBbHeight();
        Statue.BodyYaw = Dead.yBodyRot;
        Statue.HeadYaw = Dead.yHeadRot;
        Statue.HeadPitch = Dead.getXRot();
        Statue.Swing = Dead.attackAnim;
        Statue.LimbSpeed = Dead.walkAnimation.speed();
        Statue.LimbPosition = Dead.walkAnimation.position();
        if (Statue.LimbSpeed < 0.01f) {
            double Moved = Math.sqrt(Mth.square(Dead.getX() - Dead.xo) + Mth.square(Dead.getZ() - Dead.zo));
            Statue.LimbSpeed = (float) Math.min(1.0, Moved * 4.0);
            Statue.LimbPosition = 0.0f;
        }
        Statue.LimbSpeed = Math.min(Statue.LimbSpeed, MaxStride);
        Statue.Crouching = Dead.isCrouching();
        Statue.Age = Dead.tickCount;
        Statue.CreatedAt = World.getGameTime();
        Statue.SourceName = Dead.hasCustomName() && Dead.getCustomName() != null ? Dead.getCustomName().getString() : "";
        if (Dead instanceof ServerPlayer Victim) {
            Statue.PlayerShaped = true;
            GameProfile Profile = Victim.getGameProfile();
            Statue.ProfileId = Profile.getId();
            Statue.ProfileName = Profile.getName();
            Statue.SourceName = Profile.getName();
            Collection<Property> Textures = Profile.getProperties().get("textures");
            if (!Textures.isEmpty()) {
                Property Skin = Textures.iterator().next();
                Statue.SkinValue = Skin.value();
                Statue.SkinSignature = Skin.signature() == null ? "" : Skin.signature();
            }
            for (EquipmentSlot Slot : EquipmentSlot.values()) {
                ItemStack Worn = Victim.getItemBySlot(Slot);
                if (!Worn.isEmpty()) {
                    Statue.Equipment.put(Slot.getName(), Worn.copy().save(World.registryAccess()));
                }
            }
        } else {
            CompoundTag Saved = new CompoundTag();
            try {
                Dead.saveWithoutId(Saved);
            } catch (RuntimeException Problem) {
                Saved = new CompoundTag();
            }
            for (String Key : Stripped) {
                Saved.remove(Key);
            }
            Statue.Snapshot = Saved.sizeInBytes() > SnapshotLimit ? new CompoundTag() : Saved;
        }
        Statue.moveTo(Dead.getX(), Dead.getY(), Dead.getZ(), Dead.getYRot(), 0.0f);
        Statue.refreshDimensions();
        return Statue;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    public EntityDimensions getDimensions(Pose Stance) {
        return EntityDimensions.scalable(Width, Height);
    }

    public float volume() {
        return Width * Width * Height;
    }

    public int fragments() {
        return Mth.clamp(Math.round(volume() * FragmentsPerVolume), 1, MaxFragments);
    }

    public boolean isBig() {
        return volume() >= BigVolume;
    }

    public long createdAt() {
        return CreatedAt;
    }

    public ResourceLocation sourceType() {
        return SourceType;
    }

    public CompoundTag snapshot() {
        return Snapshot;
    }

    public CompoundTag equipment() {
        return Equipment;
    }

    public boolean isPlayerShaped() {
        return PlayerShaped;
    }

    public Optional<GameProfile> profile() {
        if (!PlayerShaped || ProfileId == null) {
            return Optional.empty();
        }
        GameProfile Profile = new GameProfile(ProfileId, ProfileName);
        if (!SkinValue.isEmpty()) {
            Profile.getProperties().put("textures", new Property("textures", SkinValue, SkinSignature.isEmpty() ? null : SkinSignature));
        }
        return Optional.of(Profile);
    }

    public float bodyYaw() {
        return BodyYaw;
    }

    public float headYaw() {
        return HeadYaw;
    }

    public float headPitch() {
        return HeadPitch;
    }

    public float limbPosition() {
        return LimbPosition;
    }

    public float limbSpeed() {
        return LimbSpeed;
    }

    public float swing() {
        return Swing;
    }

    public boolean crouching() {
        return Crouching;
    }

    public int age() {
        return Age;
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

    public Component sourceName() {
        if (!SourceName.isEmpty()) {
            return Component.literal(SourceName);
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(SourceType).map(EntityType::getDescription).orElse(Component.literal(SourceType.toString()));
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.scp_unleashed.scp009_statue.of", sourceName());
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        super.tick();
        applyGravity();
        move(MoverType.SELF, getDeltaMovement());
        double Slow = isInWater() ? WaterDrag : Drag;
        Vec3 Motion = getDeltaMovement().multiply(Slow, Slow, Slow);
        if (onGround()) {
            Motion = new Vec3(Motion.x * 0.5, Math.max(0.0, Motion.y), Motion.z * 0.5);
        }
        setDeltaMovement(Motion);
        if (level().isClientSide && random.nextInt(GlintChance) == 0) {
            level().addParticle(Scp009Registry.Glint.get(), getRandomX(0.6), getY() + random.nextDouble() * Height, getRandomZ(0.6), 0.0, 0.004, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (isRemoved() || level().isClientSide || isInvulnerableTo(Source)) {
            return false;
        }
        boolean Creative = Source.getEntity() instanceof Player Breaker && Breaker.getAbilities().instabuild;
        if (Creative) {
            shatter(false);
            return true;
        }
        if (Source.is(DamageTypeTags.IS_EXPLOSION)) {
            shatter(true);
            return true;
        }
        if (!(Source.getEntity() instanceof Player) && !Source.is(DamageTypeTags.IS_PROJECTILE)) {
            return false;
        }
        Hits++;
        if (Hits >= (isBig() ? BigHits : SmallHits)) {
            shatter(true);
            return true;
        }
        if (level() instanceof ServerLevel World) {
            World.playSound(null, getX(), getY() + Height * 0.5, getZ(), Scp009Registry.Crackle.get(), SoundSource.BLOCKS, 0.9f, 0.8f + random.nextFloat() * 0.3f);
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Scp009Registry.RedIce.get().defaultBlockState()), getX(), getY() + Height * 0.5, getZ(),
                    8, Width * 0.3, Height * 0.25, Width * 0.3, 0.1);
        }
        return true;
    }

    public void shatter(boolean Drops) {
        if (level() instanceof ServerLevel World) {
            double Middle = getY() + Height * 0.5;
            if (Drops) {
                int Count = fragments();
                for (int Index = 0; Index < Count; Index++) {
                    ItemEntity Drop = new ItemEntity(World, getX(), getY() + Height * (0.2 + random.nextDouble() * 0.6), getZ(), new ItemStack(Scp009Registry.Fragment.get()));
                    Drop.setDeltaMovement(random.nextGaussian() * 0.08, 0.15 + random.nextDouble() * 0.12, random.nextGaussian() * 0.08);
                    Drop.setDefaultPickUpDelay();
                    World.addFreshEntity(Drop);
                }
                if (isBig()) {
                    spawnAtLocation(new ItemStack(Scp009Registry.RedIceItem.get()), Height * 0.5f);
                }
            }
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Scp009Registry.RedIce.get().defaultBlockState()), getX(), Middle, getZ(),
                    Math.round(20 + 20 * Math.min(volume(), 2.0f)), Width * 0.4, Height * 0.35, Width * 0.4, 0.15);
            World.sendParticles(Scp009Registry.Glint.get(), getX(), Middle, getZ(), 16, Width * 0.4, Height * 0.35, Width * 0.4, 0.06);
            World.playSound(null, getX(), Middle, getZ(), Scp009Registry.Shatter.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            World.playSound(null, getX(), Middle, getZ(), Scp009Registry.ShatterChime.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        discard();
    }

    public void crumble() {
        if (level() instanceof ServerLevel World) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Scp009Registry.RedIce.get().defaultBlockState()), getX(), getY() + Height * 0.5, getZ(),
                    12, Width * 0.4, Height * 0.35, Width * 0.4, 0.05);
            World.playSound(null, getX(), getY() + Height * 0.5, getZ(), Scp009Registry.Crackle.get(), SoundSource.BLOCKS, 0.6f, 0.7f);
        }
        discard();
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return ItemStack.EMPTY;
    }

    public CompoundTag saveStatue() {
        CompoundTag Saved = new CompoundTag();
        Saved.putString("SourceType", SourceType.toString());
        Saved.put("Snapshot", Snapshot);
        Saved.put("Equipment", Equipment);
        Saved.putString("SourceName", SourceName);
        Saved.putBoolean("PlayerShaped", PlayerShaped);
        if (ProfileId != null) {
            Saved.putUUID("ProfileId", ProfileId);
        }
        Saved.putString("ProfileName", ProfileName);
        Saved.putString("SkinValue", SkinValue);
        Saved.putString("SkinSignature", SkinSignature);
        Saved.putFloat("Width", Width);
        Saved.putFloat("Height", Height);
        Saved.putFloat("BodyYaw", BodyYaw);
        Saved.putFloat("HeadYaw", HeadYaw);
        Saved.putFloat("HeadPitch", HeadPitch);
        Saved.putFloat("LimbPosition", LimbPosition);
        Saved.putFloat("LimbSpeed", LimbSpeed);
        Saved.putFloat("Swing", Swing);
        Saved.putBoolean("Crouching", Crouching);
        Saved.putInt("Age", Age);
        Saved.putLong("CreatedAt", CreatedAt);
        Saved.putInt("Hits", Hits);
        return Saved;
    }

    public void loadStatue(CompoundTag Saved) {
        ResourceLocation Type = ResourceLocation.tryParse(Saved.getString("SourceType"));
        SourceType = Type == null ? ResourceLocation.withDefaultNamespace("pig") : Type;
        Snapshot = Saved.getCompound("Snapshot");
        Equipment = Saved.getCompound("Equipment");
        SourceName = Saved.getString("SourceName");
        PlayerShaped = Saved.getBoolean("PlayerShaped");
        ProfileId = Saved.hasUUID("ProfileId") ? Saved.getUUID("ProfileId") : null;
        ProfileName = Saved.getString("ProfileName");
        SkinValue = Saved.getString("SkinValue");
        SkinSignature = Saved.getString("SkinSignature");
        Width = Saved.contains("Width", Tag.TAG_FLOAT) ? Mth.clamp(Saved.getFloat("Width"), 0.1f, Scp009Statues.MaxWidth) : DefaultWidth;
        Height = Saved.contains("Height", Tag.TAG_FLOAT) ? Mth.clamp(Saved.getFloat("Height"), 0.1f, Scp009Statues.MaxHeight) : DefaultHeight;
        BodyYaw = Saved.getFloat("BodyYaw");
        HeadYaw = Saved.getFloat("HeadYaw");
        HeadPitch = Saved.getFloat("HeadPitch");
        LimbPosition = Saved.getFloat("LimbPosition");
        LimbSpeed = Saved.getFloat("LimbSpeed");
        Swing = Saved.getFloat("Swing");
        Crouching = Saved.getBoolean("Crouching");
        Age = Saved.getInt("Age");
        CreatedAt = Saved.getLong("CreatedAt");
        Hits = Saved.getInt("Hits");
        Dummy = null;
        DummyFailed = false;
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
        Saved.put("Statue", saveStatue());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
        if (Saved.contains("Statue", Tag.TAG_COMPOUND)) {
            loadStatue(Saved.getCompound("Statue"));
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeNbt(saveStatue());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        CompoundTag Saved = Buffer.readNbt();
        if (Saved != null) {
            loadStatue(Saved);
        }
    }
}
