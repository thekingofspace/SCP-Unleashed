package net.rai.scp_unleashed.scp066;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp066.network.Scp066ChordPayload;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp066Entity extends PathfinderMob implements GeoEntity {
    public static final float Width = 0.85f;
    public static final float Height = 0.95f;
    public static final float EyeHeight = 0.7f;
    public static final double Middle = 0.45;
    public static final double Crown = 1.0;
    public static final byte Calm = 0;
    public static final byte Changing = 1;
    public static final byte Hostile = 2;
    public static final int GreyTicks = 20;
    public static final int ShakeEnd = 60;
    public static final int EyesEnd = 75;
    public static final int ChangeTicks = 90;
    public static final int EyeCount = 6;
    public static final float EyeSpacing = 2.5f;
    public static final double RollRadius = 0.5;
    public static final double StillSpeed = 0.003;
    public static final float SettleRate = 0.3f;
    public static final int SettleDelay = 2;
    public static final int BumpChance = 3;
    public static final double Health = 40.0;
    public static final double Armor = 2.0;
    public static final double Damage = 5.0;
    public static final double Speed = 0.3;
    public static final double Resistance = 0.5;
    public static final double FollowRange = 32.0;
    public static final double CalmPace = 0.6;
    public static final double RoamPace = 0.85;
    public static final double HuntPace = 1.2;
    public static final int TreatCooldown = 400;
    public static final float CandyChance = 0.7f;
    public static final int TuneGapMin = 120;
    public static final int TuneGapSpread = 160;
    public static final int HuntGapMin = 70;
    public static final int HuntGapSpread = 60;
    public static final double ChordRange = 48.0;
    public static final float HitKnockback = 0.4f;
    public static final int LashCooldown = 20;
    public static final ResourceKey<DamageType> ChordDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp066_chord"));
    private static final EntityDataAccessor<Byte> MoodData = SynchedEntityData.defineId(Scp066Entity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ChangeData = SynchedEntityData.defineId(Scp066Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SingingData = SynchedEntityData.defineId(Scp066Entity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SingLoop = RawAnimation.begin().thenLoop("sing");
    private static final RawAnimation HostileLoop = RawAnimation.begin().thenLoop("hostile");
    private static final RawAnimation ChangeLoop = RawAnimation.begin().thenLoop("change");
    private static final RawAnimation AttackOnce = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation GiftOnce = RawAnimation.begin().thenPlay("gift");
    private static final Vector3f GreyDust = new Vector3f(0.62f, 0.62f, 0.64f);
    private static final Vector3f RedDust = new Vector3f(0.82f, 0.08f, 0.1f);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Long> TreatReady = new HashMap<>();
    private final List<Scp066Tunes.Playback> Playing = new ArrayList<>();
    @Nullable
    private UUID Instigator;
    @Nullable
    private String LastMelody;
    private int TuneGap = TuneGapMin;
    private int HuntGap = HuntGapMin;
    private int LashTicks;
    private final Quaternionf RollNow = new Quaternionf();
    private final Quaternionf RollBefore = new Quaternionf();
    private int StillTicks;
    private boolean RollReady;

    public Scp066Entity(EntityType<? extends Scp066Entity> Type, Level World) {
        super(Type, World);
        xpReward = 5;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Health)
                .add(Attributes.ARMOR, Armor)
                .add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, Resistance)
                .add(Attributes.FOLLOW_RANGE, FollowRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(MoodData, Calm);
        Builder.define(ChangeData, 0);
        Builder.define(SingingData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp066Goals.Hunt(this));
        goalSelector.addGoal(5, new Scp066Goals.Stroll(this, Calm, CalmPace, 50));
        goalSelector.addGoal(5, new Scp066Goals.Stroll(this, Hostile, RoamPace, 30));
        targetSelector.addGoal(1, new Scp066Goals.Prey<>(this, Player.class, null));
        targetSelector.addGoal(2, new Scp066Goals.Prey<>(this, LivingEntity.class, Target -> Target.getType().is(Scp066Registry.PersonLike)));
    }

    public byte mood() {
        return entityData.get(MoodData);
    }

    public boolean isCalm() {
        return mood() == Calm;
    }

    public boolean isChanging() {
        return mood() == Changing;
    }

    public boolean isHostile() {
        return mood() == Hostile;
    }

    public int changeTicks() {
        return entityData.get(ChangeData);
    }

    public boolean isSinging() {
        return entityData.get(SingingData);
    }

    public int treatSeconds(Player User) {
        Long Ready = TreatReady.get(User.getUUID());
        if (Ready == null) {
            return 0;
        }
        long Left = Ready - level().getGameTime();
        return Left <= 0L ? 0 : (int) Math.ceil(Left / 20.0);
    }

    public static Quaternionf bodyTurn(float Yaw) {
        return new Quaternionf().rotationY((180.0f - Yaw) * Mth.DEG_TO_RAD);
    }

    private void rollTick() {
        if (!RollReady) {
            RollNow.set(bodyTurn(yBodyRot));
            RollBefore.set(RollNow);
            RollReady = true;
            return;
        }
        RollBefore.set(RollNow);
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Distance > StillSpeed) {
            StillTicks = 0;
            RollNow.premul(new Quaternionf().rotationAxis((float) (Distance / RollRadius), (float) (Dz / Distance), 0.0f, (float) (-Dx / Distance)));
            RollNow.normalize();
        } else if (++StillTicks >= SettleDelay) {
            RollNow.slerp(bodyTurn(yBodyRot), SettleRate);
            RollNow.normalize();
        }
    }

    public Quaternionf roll(float Partial, float Yaw) {
        Quaternionf Mass = RollReady ? new Quaternionf(RollBefore).slerp(RollNow, Partial) : bodyTurn(Yaw);
        return bodyTurn(Yaw).conjugate().mul(Mass);
    }

    public static boolean isPrey(LivingEntity Target) {
        if (Target instanceof Player User) {
            return !User.isSpectator();
        }
        return Target.getType().is(Scp066Registry.PersonLike);
    }

    private boolean provokes(DamageSource Source) {
        return Source.getEntity() instanceof LivingEntity Attacker && isPrey(Attacker);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            rollTick();
            return;
        }
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (LashTicks > 0) {
            LashTicks--;
        }
        advanceScores(World);
        if (!isAlive()) {
            return;
        }
        byte Mood = mood();
        if (Mood == Changing) {
            changeTick(World);
        } else if (Mood == Calm) {
            calmTick(World);
        } else {
            hostileTick(World);
        }
    }

    private void advanceScores(ServerLevel World) {
        boolean Sparkle = isCalm();
        Iterator<Scp066Tunes.Playback> Each = Playing.iterator();
        while (Each.hasNext()) {
            if (!Each.next().advance(World, this, Sparkle)) {
                Each.remove();
            }
        }
        boolean Singing = Sparkle && Playing.stream().anyMatch(Entry -> Scp066Tunes.Melodies.contains(Entry.score()));
        if (Singing != isSinging()) {
            entityData.set(SingingData, Singing);
        }
    }

    private void start(Scp066Tunes.Score Played) {
        Playing.removeIf(Entry -> Entry.score() == Played);
        Playing.add(new Scp066Tunes.Playback(Played, 0));
    }

    private void calmTick(ServerLevel World) {
        if (isSinging()) {
            return;
        }
        boolean Moving = getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 || !getNavigation().isDone();
        TuneGap -= Moving ? 2 : 1;
        if (TuneGap > 0) {
            return;
        }
        TuneGap = TuneGapMin + random.nextInt(TuneGapSpread);
        List<Scp066Tunes.Score> Choices = Scp066Tunes.Melodies.stream().filter(Entry -> !Entry.Name().equals(LastMelody)).toList();
        Scp066Tunes.Score Picked = Choices.get(random.nextInt(Choices.size()));
        LastMelody = Picked.Name();
        start(Picked);
    }

    private void hostileTick(ServerLevel World) {
        LivingEntity Target = getTarget();
        if (Target != null && !Target.isAlive()) {
            setTarget(null);
            Target = null;
        }
        if (Target == null && Instigator != null) {
            Entity Grudge = World.getEntity(Instigator);
            if (Grudge instanceof LivingEntity Living && Living.isAlive() && canAttack(Living) && distanceToSqr(Living) < FollowRange * FollowRange) {
                setTarget(Living);
                Target = Living;
            }
            Instigator = null;
        }
        if (Target != null && --HuntGap <= 0) {
            HuntGap = HuntGapMin + random.nextInt(HuntGapSpread);
            start(Scp066Tunes.Hunt);
        }
        if (tickCount % 10 == 0 && random.nextInt(3) == 0) {
            World.sendParticles(new DustParticleOptions(RedDust, 0.8f), getX(), getY() + Middle, getZ(), 1, 0.2, 0.2, 0.2, 0.0);
        }
    }

    private void changeTick(ServerLevel World) {
        int Ticks = changeTicks() + 1;
        entityData.set(ChangeData, Ticks);
        getNavigation().stop();
        setTarget(null);
        double Centre = getY() + Middle;
        if (Ticks < GreyTicks && Ticks % 3 == 0) {
            World.sendParticles(new DustParticleOptions(GreyDust, 1.0f), getX(), Centre, getZ(), 2, 0.2, 0.2, 0.2, 0.0);
        } else if (Ticks >= GreyTicks && Ticks < ShakeEnd) {
            if (Ticks % 2 == 0) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAY_WOOL.defaultBlockState()), getX(), Centre, getZ(), 2, 0.2, 0.15, 0.2, 0.05);
            }
            if (Ticks % 4 == 0) {
                World.playSound(null, getX(), Centre, getZ(), Scp066Registry.Shake.get(), SoundSource.HOSTILE, 0.5f + 0.4f * (Ticks - GreyTicks) / (ShakeEnd - GreyTicks),
                        0.9f + random.nextFloat() * 0.3f);
            }
        } else if (Ticks == EyesEnd) {
            World.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), Centre + 0.2, getZ(), 4, 0.3, 0.2, 0.3, 0.0);
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_WOOL.defaultBlockState()), getX(), Centre, getZ(), 24, 0.25, 0.25, 0.25, 0.15);
        } else if (Ticks > EyesEnd && Ticks % 3 == 0) {
            World.sendParticles(new DustParticleOptions(RedDust, 1.0f), getX(), Centre, getZ(), 2, 0.25, 0.25, 0.25, 0.0);
        }
        if (Ticks >= ChangeTicks) {
            becomeHostile();
        }
    }

    public boolean beginChange(@Nullable LivingEntity Cause) {
        if (!isCalm() || level().isClientSide) {
            return false;
        }
        entityData.set(MoodData, Changing);
        entityData.set(ChangeData, 0);
        Instigator = Cause != null ? Cause.getUUID() : null;
        getNavigation().stop();
        setTarget(null);
        Playing.clear();
        start(Scp066Tunes.Change);
        playSound(Scp066Registry.Hurt.get(), 0.8f, 0.8f);
        return true;
    }

    private void becomeHostile() {
        entityData.set(MoodData, Hostile);
        entityData.set(ChangeData, ChangeTicks);
        HuntGap = 20;
    }

    public void resetCalm() {
        entityData.set(MoodData, Calm);
        entityData.set(ChangeData, 0);
        Instigator = null;
        setTarget(null);
        Playing.clear();
        TuneGap = 40;
    }

    public boolean giveTreat(Player User, boolean IgnoreCooldown) {
        if (!(level() instanceof ServerLevel World) || !isCalm()) {
            return false;
        }
        long Now = World.getGameTime();
        Long Ready = TreatReady.get(User.getUUID());
        if (!IgnoreCooldown && Ready != null && Ready > Now) {
            return false;
        }
        TreatReady.put(User.getUUID(), Now + TreatCooldown);
        ItemStack Treat = new ItemStack(random.nextFloat() < CandyChance ? Scp066Registry.Candy.get() : Scp066Registry.Cupcake.get());
        double Top = getY() + Crown;
        ItemEntity Drop = new ItemEntity(World, getX(), Top, getZ(), Treat);
        Vec3 Toward = new Vec3(User.getX() - getX(), 0.0, User.getZ() - getZ());
        Toward = Toward.lengthSqr() > 1.0E-4 ? Toward.normalize() : Vec3.ZERO;
        Drop.setDeltaMovement(Toward.x * 0.16, 0.26, Toward.z * 0.16);
        Drop.setPickUpDelay(8);
        World.addFreshEntity(Drop);
        Playing.removeIf(Entry -> Scp066Tunes.Melodies.contains(Entry.score()));
        start(Scp066Tunes.Treat);
        World.playSound(null, getX(), Top, getZ(), Scp066Registry.TreatPop.get(), SoundSource.NEUTRAL, 0.6f, 1.2f + random.nextFloat() * 0.2f);
        World.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), Top, getZ(), 8, 0.3, 0.2, 0.3, 0.0);
        World.sendParticles(ParticleTypes.NOTE, getX(), Top + 0.2, getZ(), 0, 0.5, 0.0, 0.0, 1.0);
        triggerAnim("action", "gift");
        TuneGap = Math.max(TuneGap, 60);
        return true;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (isChanging()) {
            return InteractionResult.PASS;
        }
        if (isHostile()) {
            if (!level().isClientSide) {
                lashOut(User);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (User.isSecondaryUseActive() && User.getMainHandItem().isEmpty()) {
            if (!level().isClientSide) {
                scoop(User);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!giveTreat(User, false)) {
            start(Scp066Tunes.NotYet);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp066.not_yet", treatSeconds(User)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResult.CONSUME;
    }

    private void lashOut(Player User) {
        if (canAttack(User)) {
            setTarget(User);
        }
        if (LashTicks <= 0 && isWithinMeleeAttackRange(User) && canAttack(User)) {
            LashTicks = LashCooldown;
            swing(InteractionHand.MAIN_HAND);
            doHurtTarget(User);
        }
    }

    private void scoop(Player User) {
        ItemStack Bundle = new ItemStack(Scp066Registry.BallItem.get());
        if (hasCustomName()) {
            Bundle.set(DataComponents.CUSTOM_NAME, getCustomName());
        }
        playSound(Scp066Registry.Scoop.get(), 0.8f, 1.0f);
        if (!User.getInventory().add(Bundle)) {
            spawnAtLocation(Bundle);
        }
        discard();
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        if (!(level() instanceof ServerLevel World)) {
            return false;
        }
        float Amount = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * (Target instanceof LivingEntity Hearer && Senses.isDeafened(Hearer) ? Senses.MuffledShare : 1.0f);
        DamageSource Source = damageSources().source(ChordDamage, this);
        boolean Hit = Target.hurt(Source, Amount);
        if (!Hit) {
            return false;
        }
        if (Target instanceof LivingEntity Living) {
            Living.knockback(HitKnockback, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
        }
        setLastHurtMob(Target);
        triggerAnim("action", "attack");
        double Burst = Target.getY() + Target.getBbHeight() * 0.7;
        for (int Index = 0; Index < 6; Index++) {
            World.sendParticles(ParticleTypes.NOTE, Target.getX() + (random.nextDouble() - 0.5) * 0.8, Burst + random.nextDouble() * 0.4,
                    Target.getZ() + (random.nextDouble() - 0.5) * 0.8, 0, random.nextInt(25) / 24.0, 0.0, 0.0, 1.0);
        }
        int Voicing = (int) Math.floorMod(World.getGameTime() / 20L + getId(), 3L);
        PacketDistributor.sendToPlayersNear(World, null, getX(), getY() + Middle, getZ(), ChordRange, new Scp066ChordPayload(getX(), getY() + Middle, getZ(), Voicing));
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(0.6, 0.45, 0.6);
    }

    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(0.0, 0.35, 0.0);
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        if (!isHostile() || !isPrey(Target)) {
            return false;
        }
        if (Target instanceof Player && level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        return super.canAttack(Target);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level().isClientSide) {
            return false;
        }
        if (isChanging() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (isCalm() && provokes(Source) && !isInvulnerableTo(Source)) {
            beginChange((LivingEntity) Source.getEntity());
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && isAlive()) {
            if (isHostile() && Source.getEntity() instanceof LivingEntity Attacker && canAttack(Attacker)) {
                setTarget(Attacker);
            }
            start(isHostile() ? Scp066Tunes.Snarl : Scp066Tunes.Ouch);
        }
        return Hurt;
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
        if (!isChanging()) {
            super.knockback(Strength, X, Z);
        }
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (dead && level() instanceof ServerLevel World) {
            Playing.clear();
            start(Scp066Tunes.Unravel);
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, (isCalm() ? Blocks.PINK_WOOL : Blocks.RED_WOOL).defaultBlockState()),
                    getX(), getY() + Middle, getZ(), 30, 0.25, 0.25, 0.25, 0.1);
        }
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canDrownInFluidType(FluidType Type) {
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
    protected void playStepSound(BlockPos Pos, BlockState State) {
        float Weight = isHostile() ? 1.0f : 0.6f;
        playSound(Scp066Registry.Roll.get(), 0.35f * Weight, (isHostile() ? 0.8f : 1.0f) + random.nextFloat() * 0.1f);
        if (random.nextInt(BumpChance) == 0) {
            playSound(Scp066Registry.Bump.get(), 0.3f * Weight, (isHostile() ? 0.75f : 0.95f) + random.nextFloat() * 0.15f);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp066Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp066Registry.Death.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Data) {
        super.addAdditionalSaveData(Data);
        Data.putByte("Mood", mood());
        Data.putInt("Change", changeTicks());
        Data.putInt("TuneGap", TuneGap);
        if (Instigator != null) {
            Data.putUUID("Instigator", Instigator);
        }
        ListTag Treats = new ListTag();
        for (Map.Entry<UUID, Long> Entry : TreatReady.entrySet()) {
            CompoundTag Record = new CompoundTag();
            Record.putUUID("Player", Entry.getKey());
            Record.putLong("Ready", Entry.getValue());
            Treats.add(Record);
        }
        Data.put("Treats", Treats);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Data) {
        super.readAdditionalSaveData(Data);
        byte Mood = Data.getByte("Mood");
        if (Mood < Calm || Mood > Hostile) {
            Mood = Calm;
        }
        int Change = Mth.clamp(Data.getInt("Change"), 0, ChangeTicks);
        entityData.set(MoodData, Mood);
        entityData.set(ChangeData, Mood == Hostile ? ChangeTicks : Mood == Changing ? Change : 0);
        TuneGap = Math.max(20, Data.getInt("TuneGap"));
        Instigator = Data.hasUUID("Instigator") ? Data.getUUID("Instigator") : null;
        TreatReady.clear();
        for (Tag Entry : Data.getList("Treats", Tag.TAG_COMPOUND)) {
            CompoundTag Record = (CompoundTag) Entry;
            if (Record.hasUUID("Player")) {
                TreatReady.put(Record.getUUID("Player"), Record.getLong("Ready"));
            }
        }
        Playing.clear();
        if (Mood == Changing) {
            Playing.add(new Scp066Tunes.Playback(Scp066Tunes.Change, Change));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Registrar) {
        Registrar.add(new AnimationController<>(this, "body", 4, State -> {
            byte Mood = mood();
            if (Mood == Hostile) {
                return State.setAndContinue(HostileLoop);
            }
            if (Mood == Changing) {
                return State.setAndContinue(ChangeLoop);
            }
            return State.setAndContinue(isSinging() ? SingLoop : IdleLoop);
        }));
        Registrar.add(new AnimationController<>(this, "action", 0, State -> PlayState.STOP)
                .triggerableAnim("attack", AttackOnce)
                .triggerableAnim("gift", GiftOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
