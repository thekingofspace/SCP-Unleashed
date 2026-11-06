package net.rai.scp_unleashed.scp001scarlet.king;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletCourtHook;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ScarletKing extends Monster implements GeoEntity {
    public static final float Width = 5.0f;
    public static final float Height = 22.0f;
    public static final float EyeHeight = 18.5f;
    public static final double VanillaHealth = 1000.0;
    public static final int Idle = 0;
    public static final int Lash = 1;
    public static final int Nova = 2;
    public static final int Slam = 3;
    public static final int Beam = 4;
    public static final int Thorns = 5;
    public static final int Shades = 6;
    public static final int Kneel = 7;
    public static final int Rise = 8;
    public static final int Snap = 9;
    public static final int Manifest = 10;
    public static final int Dying = 11;
    public static final int ManifestTicks = 120;
    public static final int KneelDown = 28;
    public static final int RiseTicks = 32;
    public static final int SnapTicks = 18;
    public static final int DeathTicks = 180;
    public static final int SlackTicks = 24;
    public static final int KneelTick = 50;
    public static final int SecondKneelTick = 80;
    public static final int DefianceTick = 96;
    public static final int CrownCrackTick = 100;
    public static final int ShatterTick = 107;
    public static final int CrumbleTick = 110;
    public static final int CollapseTick = 164;
    public static final int LootTick = 172;
    public static final int AllBands = 0b1111111;
    public static final int[][] StageBands = {{}, {0, 4}, {2, 6}, {1, 5}};
    public static final int[][] DaughterBands = {{0, 4}, {2, 6}, {1, 5}, {3}};
    public static final float WardCooldown = 2.5f;
    public static final double WardRadius = 9.0;
    public static final float[] Thresholds = {0.75f, 0.5f, 0.25f};
    public static final double NovaSpeed = 0.9;
    public static final double NovaStart = 3.0;
    public static final double NovaReach = 44.0;
    public static final double LashInner = 3.0;
    public static final double LashOuter = 30.0;
    public static final double LashHalfAngle = 70.0;
    public static final double SlamReach = 10.0;
    public static final double GrabRadius = 3.4;
    public static final double ShockRadius = 4.5;
    public static final double BeamRadius = 1.7;
    public static final double BeamSweep = 0.3;
    public static final float LashDamage = 14.0f;
    public static final float NovaDamage = 12.0f;
    public static final float GrabDamage = 6.0f;
    public static final float SlamDamage = 10.0f;
    public static final float ShockDamage = 8.0f;
    public static final float BeamDamage = 5.0f;
    public static final int MaxShades = 6;
    public static final int BaseCooldown = 50;
    public static final Vec3 HeadRest = new Vec3(0.0, 18.56, 0.0);
    public static final Vec3 HeadKneel = new Vec3(0.0, 9.06, 8.16);
    public static final Vec3 ChestRest = new Vec3(0.0, 14.25, 1.12);
    public static final Vec3 ChestKneel = new Vec3(0.0, 5.51, 5.59);
    public static final Vec3 HandRest = new Vec3(7.5, 10.31, 1.59);
    public static final Vec3 HandKneel = new Vec3(7.07, 1.89, 7.68);
    public static final Vec3 HaloRest = new Vec3(0.0, 24.2, 0.0);
    public static final ResourceLocation CultistId = ResourceLocation.fromNamespaceAndPath("scp_unleashed", "scarlet_cultist");
    public static final String ShadeTag = "scarlet_king_shade";

    private static final EntityDataAccessor<Integer> ActionData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ActionTickData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BandsData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ShownData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> StageData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NovaData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SecondNovaData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BeamStateData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> BeamData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> SpotData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<CompoundTag> ArenaData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> TargetData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WardData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WardHitData = SynchedEntityData.defineId(ScarletKing.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ScarletKingPart[] Parts;
    private final ScarletKingPart Hem;
    private final ScarletKingPart Chest;
    private final ScarletKingPart Head;
    private final ScarletKingPart HandA;
    private final ScarletKingPart HandB;
    private final List<int[]> Rings = new ArrayList<>();
    private final List<Set<UUID>> RingHits = new ArrayList<>();
    private float Pool = 1.0f;
    private float PoolMax = 1.0f;
    private int FightTicks;
    private int Cooldown = 60;
    private int ActionTime;
    private int KneelLeft;
    private int Generation;
    private int ManifestAge;
    private boolean Fighting;
    private boolean Binding;
    private boolean Applying;
    private boolean Simulating;
    private int LastAttack = -1;
    private int LastDaughters;
    private int Players = 1;
    @Nullable
    private UUID Grabbed;
    @Nullable
    private Player Target;
    private Vec3 Spot = Vec3.ZERO;
    @Nullable
    private ScarletArena Arena;
    private int SeenActionTick = -1;
    private int AnchorKey = -1;
    private float RenderPartial;

    public ScarletKing(EntityType<? extends ScarletKing> Type, Level World) {
        super(Type, World);
        Hem = new ScarletKingPart(this, "hem", 14.0f, 5.0f, 0.8f);
        Chest = new ScarletKingPart(this, "chest", 6.5f, 6.5f, 1.0f);
        Head = new ScarletKingPart(this, "head", 5.0f, 5.0f, 1.3f);
        HandA = new ScarletKingPart(this, "hand_a", 3.5f, 3.5f, 0.9f);
        HandB = new ScarletKingPart(this, "hand_b", 3.5f, 3.5f, 0.9f);
        Parts = new ScarletKingPart[]{Hem, Chest, Head, HandA, HandB};
        setId(ENTITY_COUNTER.getAndAdd(Parts.length + 1) + 1);
        setPersistenceRequired();
        setNoGravity(true);
        noPhysics = false;
        xpReward = 800;
    }

    @Nullable
    public ServerBossEvent bar() {
        if (!(level() instanceof ServerLevel World) || Generation != ScarletKingData.get(World.getServer()).generation()) {
            return null;
        }
        return ScarletKingBar.of(Generation);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, VanillaHealth).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 0.0).add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.ATTACK_DAMAGE, 14.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, Idle);
        Builder.define(ActionTickData, 0);
        Builder.define(BandsData, 0);
        Builder.define(ShownData, 0);
        Builder.define(StageData, 0);
        Builder.define(NovaData, -1000);
        Builder.define(SecondNovaData, -1000);
        Builder.define(BeamStateData, 0);
        Builder.define(BeamData, new Vector3f());
        Builder.define(SpotData, new Vector3f());
        Builder.define(ArenaData, new CompoundTag());
        Builder.define(TargetData, -1);
        Builder.define(WardData, false);
        Builder.define(WardHitData, -1000);
    }

    @Override
    public void setId(int Id) {
        super.setId(Id);
        if (Parts != null) {
            for (int Index = 0; Index < Parts.length; Index++) {
                Parts[Index].setId(Id + Index + 1);
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return Parts;
    }

    public ScarletKingPart[] parts() {
        return Parts;
    }

    public int action() {
        return entityData.get(ActionData);
    }

    public int actionTick() {
        return entityData.get(ActionTickData);
    }

    public int bands() {
        return entityData.get(BandsData);
    }

    public int shown() {
        return entityData.get(ShownData);
    }

    public int stage() {
        return entityData.get(StageData);
    }

    public int novaTick() {
        return entityData.get(NovaData);
    }

    public int secondNovaTick() {
        return entityData.get(SecondNovaData);
    }

    public int beamState() {
        return entityData.get(BeamStateData);
    }

    public Vec3 beamPoint() {
        Vector3f Point = entityData.get(BeamData);
        return new Vec3(Point.x(), Point.y(), Point.z());
    }

    public Vec3 spot() {
        Vector3f Point = entityData.get(SpotData);
        return new Vec3(Point.x(), Point.y(), Point.z());
    }

    public int targetId() {
        return entityData.get(TargetData);
    }

    public boolean warded() {
        return entityData.get(WardData);
    }

    public int wardHitTick() {
        return entityData.get(WardHitData);
    }

    @Nullable
    public static ScarletCourtHook court() {
        return ScarletCourtHook.Holder.get();
    }

    @Nullable
    public ScarletArena arena() {
        if (Arena == null) {
            CompoundTag Tag = entityData.get(ArenaData);
            if (Tag.contains("Throne")) {
                Arena = ScarletArena.load(Tag);
            }
        }
        return Arena;
    }

    public float pool() {
        return Pool;
    }

    public float poolMax() {
        return PoolMax;
    }

    public boolean fighting() {
        return Fighting;
    }

    public int fightTicks() {
        return FightTicks;
    }

    public int generation() {
        return Generation;
    }

    public void generation(int Value) {
        Generation = Value;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public int seenActionTick() {
        return SeenActionTick;
    }

    public void seenActionTick(int Value) {
        SeenActionTick = Value;
    }

    public void place(ScarletArena Where) {
        Arena = Where;
        entityData.set(ArenaData, Where.save());
    }

    public static float scaledPool(int Players) {
        return (float) (ScarletKingConfig.baseHealth() * (1.0 + ScarletKingConfig.extraPlayerShare() * Math.max(0, Players - 1)));
    }

    public float hitCap() {
        return (float) (PoolMax * ScarletKingConfig.hitCapShare());
    }

    public boolean kneeling() {
        int Action = action();
        return Action == Kneel || Action == Rise && ActionTime < RiseTicks / 2;
    }

    public boolean enraged() {
        return Fighting && FightTicks > ScarletKingConfig.enrageTicks();
    }

    public void beginBinding() {
        Binding = true;
    }

    public void beginFight(int Count) {
        Players = Math.max(1, Count);
        Fighting = true;
        Binding = false;
        PoolMax = scaledPool(Players);
        Pool = PoolMax;
        FightTicks = 0;
        Cooldown = 60;
        entityData.set(BandsData, AllBands);
        entityData.set(ShownData, ScarletArena.Count);
        entityData.set(StageData, 0);
        setAction(Idle);
        syncHealth();
        entityData.set(WardData, false);
        LastDaughters = 0;
        if (level() instanceof ServerLevel World) {
            ScarletCourtHook Hook = court();
            if (Hook != null) {
                ScarletArena Where = arena();
                Hook.begin(World, Where == null ? blockPosition() : Where.Throne(), this, Players);
                LastDaughters = Math.max(0, Hook.daughtersRemaining());
                entityData.set(WardData, LastDaughters > 0);
                int Bands = AllBands;
                for (int Fallen = 0; Fallen < Math.max(0, DaughterBands.length - LastDaughters); Fallen++) {
                    for (int Band : DaughterBands[Fallen]) {
                        Bands &= ~(1 << Band);
                    }
                }
                entityData.set(BandsData, LastDaughters > 0 ? Bands : 0);
            }
            sound(World, ScarletKingRegistry.Roar.get(), 6.0f, 0.8f);
            sound(World, ScarletKingRegistry.ChoirLow.get(), 6.0f, 0.6f);
        }
    }

    public void watchCourt(ServerLevel World) {
        ScarletCourtHook Hook = court();
        if (Hook == null) {
            if (warded()) {
                shatter(World);
            }
            return;
        }
        Hook.tick(World, this);
        int Remaining = Math.max(0, Hook.daughtersRemaining());
        while (LastDaughters > Remaining) {
            LastDaughters--;
            int Fallen = Math.min(DaughterBands.length - 1, Math.max(0, 3 - LastDaughters));
            int Bands = bands();
            for (int Band : DaughterBands[Fallen]) {
                Bands &= ~(1 << Band);
            }
            entityData.set(BandsData, Bands);
            sound(World, ScarletKingRegistry.Roar.get(), 8.0f, 0.55f);
            sound(World, ScarletKingRegistry.Snap.get(), 6.0f, 0.6f);
            if (LastDaughters > 0) {
                endAttack();
                setAction(Snap);
                KneelLeft = 0;
            }
        }
        LastDaughters = Remaining;
        if (warded() && Remaining == 0) {
            shatter(World);
        }
    }

    private void shatter(ServerLevel World) {
        entityData.set(WardData, false);
        entityData.set(BandsData, 0);
        endAttack();
        setAction(Snap);
        sound(World, ScarletKingRegistry.Snap.get(), 10.0f, 0.4f);
        sound(World, ScarletKingRegistry.Roar.get(), 10.0f, 0.5f);
        sound(World, ScarletKingRegistry.Chain.get(), 8.0f, 0.45f);
        World.playSound(null, getX(), getY() + 10.0, getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 8.0f, 0.5f);
        BlockState Glass = Blocks.RED_STAINED_GLASS.defaultBlockState();
        for (int Ring = 0; Ring < 10; Ring++) {
            double Lift = Ring * 2.6;
            double Radius = WardRadius * Math.sqrt(Math.max(0.05, 1.0 - Math.pow(Math.max(0.0, Lift - 14.0) / 13.0, 2.0)));
            for (int Index = 0; Index < 24; Index++) {
                double Angle = Index * Math.PI / 12.0;
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Glass), getX() + Math.cos(Angle) * Radius, getY() + Lift, getZ() + Math.sin(Angle) * Radius,
                        3, 0.4, 0.4, 0.4, 0.3);
            }
        }
        World.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.2f, 0.15f), 3.0f), getX(), getY() + 12.0, getZ(), 120, 8.0, 10.0, 8.0, 0.1);
    }

    public void setAction(int Action) {
        entityData.set(ActionData, Action);
        entityData.set(ActionTickData, (int) level().getGameTime());
        ActionTime = 0;
    }

    public float actionAge(float Partial) {
        return Math.max(0.0f, (float) (level().getGameTime() - (long) actionTick()) + Partial);
    }

    public static final float[][] EmergeKeys = {{0.0f, -150.0f}, {3.6f, -40.0f}, {5.0f, 0.0f}};

    public static float emergeDepth(float Seconds) {
        if (Seconds <= EmergeKeys[0][0]) {
            return EmergeKeys[0][1];
        }
        for (int Index = 1; Index < EmergeKeys.length; Index++) {
            if (Seconds <= EmergeKeys[Index][0]) {
                float Share = (Seconds - EmergeKeys[Index - 1][0]) / (EmergeKeys[Index][0] - EmergeKeys[Index - 1][0]);
                return Mth.lerp(Share, EmergeKeys[Index - 1][1], EmergeKeys[Index][1]);
            }
        }
        return 0.0f;
    }

    public static boolean movesRoot(int Action) {
        return Action == Kneel || Action == Rise || Action == Snap || Action == Slam || Action == Dying;
    }

    public void settle(ServerLevel World) {
        Vec3 Want = ground(World, position());
        if (Math.abs(Want.y - getY()) > 1.0E-3 || !World.noCollision(this, getBoundingBox().deflate(0.05))) {
            moveTo(Want.x, Want.y, Want.z, getYRot(), getXRot());
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private void syncHealth() {
        Applying = true;
        try {
            super.setHealth(Pool <= 0.0f ? 0.0f : Math.max(1.0f, (float) (VanillaHealth * Pool / PoolMax)));
        } finally {
            Applying = false;
        }
        ServerBossEvent Bar = bar();
        if (Bar != null) {
            Bar.setProgress(Mth.clamp(Pool / PoolMax, 0.0f, 1.0f));
        }
    }

    @Override
    public void setHealth(float Value) {
        if (Applying || level().isClientSide || tickCount <= 0 || PoolMax <= 1.0f || !Fighting) {
            super.setHealth(Value);
            return;
        }
        float Lost = getHealth() - Value;
        if (Lost > 0.0f) {
            applyDamage(null, (float) (Lost / VanillaHealth * PoolMax));
        }
    }

    public boolean vulnerable() {
        if (!Fighting || isDeadOrDying() || !(level() instanceof ServerLevel World)) {
            return false;
        }
        return ScarletState.get(World.getServer()).phase() == ScarletState.Phase.Fight;
    }

    public boolean hurtPart(ScarletKingPart Part, DamageSource Source, float Amount) {
        return hurtScaled(Source, Amount * Part.Multiplier);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return hurtScaled(Source, Amount * 0.85f);
    }

    private boolean hurtScaled(DamageSource Source, float Amount) {
        if (level().isClientSide) {
            return false;
        }
        if (Source.is(DamageTypes.GENERIC_KILL)) {
            Pool = 0.0f;
            Applying = true;
            try {
                return super.hurt(Source, Float.MAX_VALUE);
            } finally {
                Applying = false;
            }
        }
        if (warded() && Fighting && !isDeadOrDying()) {
            deflect(Source);
            return false;
        }
        if (!vulnerable() || Amount <= 0.0f) {
            return false;
        }
        float Share = shareFor(Source);
        if (Share <= 0.0f) {
            return false;
        }
        boolean Melee = Source.getDirectEntity() instanceof Player && Source.getDirectEntity() == Source.getEntity() && Source.is(DamageTypes.PLAYER_ATTACK);
        if (!Melee) {
            invulnerableTime = 0;
        }
        Applying = true;
        boolean Flinched;
        try {
            Flinched = super.hurt(Source, 0.01f);
        } finally {
            Applying = false;
        }
        if (!Flinched && Melee) {
            return false;
        }
        applyDamage(Source, Amount * Share);
        return true;
    }

    private void deflect(DamageSource Source) {
        if (!(level() instanceof ServerLevel World) || tickCount - wardHitTick() < 2) {
            return;
        }
        entityData.set(WardHitData, tickCount);
        Entity Attacker = Source.getEntity();
        Vec3 Toward = Attacker == null ? forward() : Attacker.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (Toward.lengthSqr() < 1.0E-4) {
            Toward = forward();
        }
        double Height = Attacker == null ? getY() + 8.0 : Mth.clamp(Attacker.getEyeY(), getY() + 1.0, getY() + 24.0);
        Vec3 Hit = position().add(Toward.normalize().scale(WardRadius)).add(0.0, Height - getY(), 0.0);
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Hit.x, Hit.y, Hit.z, 8, 0.3, 0.3, 0.3, 0.2);
        World.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.25f, 0.2f), 1.4f), Hit.x, Hit.y, Hit.z, 6, 0.3, 0.3, 0.3, 0.05);
        World.playSound(null, Hit.x, Hit.y, Hit.z, ScarletKingRegistry.WardHit.get(), SoundSource.HOSTILE, 3.0f, 0.9f + random.nextFloat() * 0.2f);
    }

    public float shareFor(DamageSource Source) {
        if (Source.is(DamageTypeTags.IS_FALL) || Source.is(DamageTypeTags.IS_DROWNING) || Source.is(DamageTypeTags.IS_FREEZING) || Source.is(DamageTypes.IN_WALL)
                || Source.is(DamageTypes.CRAMMING) || Source.is(DamageTypes.CACTUS) || Source.is(DamageTypes.SWEET_BERRY_BUSH) || Source.is(DamageTypes.STARVE)
                || Source.is(DamageTypes.FLY_INTO_WALL) || Source.is(DamageTypes.IN_FIRE) && Source.getEntity() == null) {
            return 0.0f;
        }
        if (Source.is(DamageTypeTags.IS_EXPLOSION)) {
            return 0.25f;
        }
        if (Source.is(DamageTypeTags.IS_FIRE)) {
            return 0.2f;
        }
        if (Source.is(DamageTypeTags.IS_LIGHTNING) || Source.is(DamageTypeTags.WITCH_RESISTANT_TO) || Source.is(DamageTypes.WITHER)) {
            return 0.25f;
        }
        return 1.0f;
    }

    public void applyDamage(@Nullable DamageSource Source, float Amount) {
        if (!Fighting || Pool <= 0.0f) {
            return;
        }
        float Final = Amount;
        if (kneeling()) {
            Final *= (float) ScarletKingConfig.kneelDamage();
        }
        Final = Math.min(Final, hitCap());
        Pool = Math.max(0.0f, Pool - Final);
        int Wanted = 0;
        for (float Threshold : Thresholds) {
            if (Pool / PoolMax <= Threshold) {
                Wanted++;
            }
        }
        if (Pool > 0.0f && Wanted > stage()) {
            for (int Next = stage() + 1; Next <= Wanted; Next++) {
                advanceStage(Next);
            }
        }
        syncHealth();
        if (Pool <= 0.0f) {
            Applying = true;
            try {
                super.setHealth(0.0f);
            } finally {
                Applying = false;
            }
            die(Source != null ? Source : damageSources().generic());
        }
    }

    private void advanceStage(int Next) {
        entityData.set(StageData, Next);
        ScarletCourtHook Hook = court();
        if (Hook != null && level() instanceof ServerLevel) {
            Hook.kingPhase(Next);
        }
        int Intact = bands();
        int Bands = Intact;
        for (int Band : StageBands[Math.min(Next, StageBands.length - 1)]) {
            Bands &= ~(1 << Band);
        }
        entityData.set(BandsData, Bands);
        endAttack();
        setAction(Snap);
        style();
        if (Simulating || !(level() instanceof ServerLevel World)) {
            return;
        }
        sound(World, ScarletKingRegistry.Snap.get(), 6.0f, 0.5f);
        sound(World, ScarletKingRegistry.Chain.get(), 6.0f, 0.5f);
        sound(World, ScarletKingRegistry.Roar.get(), 6.0f, 0.7f);
        ScarletArena Where = arena();
        if (Where == null) {
            return;
        }
        for (int Band : StageBands[Math.min(Next, StageBands.length - 1)]) {
            if ((Intact & 1 << Band) == 0) {
                continue;
            }
            BlockPos Pillar = Where.Pillars().get(Band);
            Vec3 From = bindPoint(Band, 0.0f);
            Vec3 To = Where.anchor(Band);
            for (int Step = 0; Step <= 24; Step++) {
                Vec3 At = From.lerp(To, Step / 24.0);
                World.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.35f), 2.0f), At.x, At.y, At.z, 3, 0.3, 0.3, 0.3, 0.02);
            }
            if (ScarletKingConfig.arenaChanges()) {
                crumble(World, Pillar, 5);
            }
        }
        if (Next == 2) {
            thornWall(World, Where, 18.0, 600);
            if (ScarletKingConfig.arenaChanges()) {
                for (BlockPos Seat : Where.Seats()) {
                    crumble(World, Seat.above(2), 2);
                }
            }
        }
        if (Next == 3) {
            thornWall(World, Where, 27.0, 600);
        }
    }

    private void crumble(ServerLevel World, BlockPos Top, int Depth) {
        BlockPos Highest = null;
        for (int Lift = 4; Lift >= -6; Lift--) {
            BlockPos At = Top.above(Lift);
            if (!World.getBlockState(At).isAir()) {
                Highest = At;
                break;
            }
        }
        if (Highest == null) {
            return;
        }
        for (int Down = 0; Down < Depth; Down++) {
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    BlockPos At = Highest.offset(Dx, -Down, Dz);
                    BlockState State = World.getBlockState(At);
                    if (State.isAir() || State.hasBlockEntity() || State.getDestroySpeed(World, At) < 0.0f || random.nextFloat() < 0.35f) {
                        continue;
                    }
                    FallingBlockEntity.fall(World, At, State);
                }
            }
        }
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, World.getBlockState(Highest.below(Depth))), Highest.getX() + 0.5, Highest.getY(),
                Highest.getZ() + 0.5, 40, 1.2, 1.2, 1.2, 0.1);
        World.playSound(null, Highest, ScarletKingRegistry.Impact.get(), SoundSource.HOSTILE, 4.0f, 0.6f);
    }

    private void thornWall(ServerLevel World, ScarletArena Where, double Radius, int Life) {
        for (int Wall = 0; Wall < 4; Wall++) {
            double Angle = Wall * Math.PI / 2.0 + random.nextDouble() * 0.8;
            for (int Step = -4; Step <= 4; Step++) {
                double Along = Angle + Step * 1.6 / Radius;
                Vec3 At = Where.centre().add(Math.sin(Along) * Radius, 0.0, -Math.cos(Along) * Radius);
                ScarletThorn.spawn(World, this, ground(World, At), 10 + Math.abs(Step) * 2, Life, true);
            }
        }
    }

    public static final int GroundAbove = 24;
    public static final int GroundBelow = 24;
    public static final int GroundClear = 3;

    public static Vec3 ground(ServerLevel World, Vec3 At) {
        BlockPos Column = BlockPos.containing(At.x, At.y + 6.0, At.z);
        for (int Down = 0; Down < 16; Down++) {
            BlockPos Pos = Column.below(Down);
            if (standable(World, Pos)) {
                return new Vec3(At.x, top(World, Pos), At.z);
            }
        }
        BlockPos Start = BlockPos.containing(At.x, At.y + GroundAbove, At.z);
        for (int Down = 0; Down <= GroundAbove + GroundBelow; Down++) {
            BlockPos Pos = Start.below(Down);
            if (standable(World, Pos)) {
                return new Vec3(At.x, top(World, Pos), At.z);
            }
        }
        return At;
    }

    private static boolean standable(ServerLevel World, BlockPos Pos) {
        if (World.getBlockState(Pos).getCollisionShape(World, Pos).isEmpty()) {
            return false;
        }
        for (int Up = 1; Up <= GroundClear; Up++) {
            if (!World.getBlockState(Pos.above(Up)).getCollisionShape(World, Pos.above(Up)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static double top(ServerLevel World, BlockPos Pos) {
        return Pos.getY() + World.getBlockState(Pos).getCollisionShape(World, Pos).max(Direction.Axis.Y);
    }

    public Vec3 forward() {
        float Yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(Yaw), 0.0, Mth.cos(Yaw));
    }

    public Vec3 local(Vec3 Offset) {
        Vec3 Forward = forward();
        Vec3 Right = new Vec3(-Forward.z, 0.0, Forward.x);
        return position().add(Right.scale(Offset.x)).add(0.0, Offset.y, 0.0).add(Forward.scale(Offset.z));
    }

    public float kneelShare(float Partial) {
        int Action = action();
        float Time = ActionTime + Partial;
        if (level().isClientSide) {
            Time = actionAge(Partial);
        }
        if (Action == Kneel || Action == Dying) {
            return Mth.clamp(Time / KneelDown, 0.0f, 1.0f);
        }
        if (Action == Rise) {
            return 1.0f - Mth.clamp(Time / RiseTicks, 0.0f, 1.0f);
        }
        return 0.0f;
    }

    public static final double[] BindRest = {10.3, 10.3, 15.8, 8.3, 8.3, 11.3, 16.6};
    public static final double[] BindKneel = {4.0, 4.0, 7.0, 3.3, 3.3, 5.0, 8.0};
    public static final double[] BindReach = {2.6, 2.6, 2.2, 3.0, 3.0, 2.4, 1.4};

    public Vec3 bindPoint(int Band, float Partial) {
        Vec3 Base = new Vec3(Mth.lerp(Partial, xo, getX()), Mth.lerp(Partial, yo, getY()), Mth.lerp(Partial, zo, getZ()));
        int Index = Mth.clamp(Band, 0, BindRest.length - 1);
        double Share = kneelShare(Partial);
        double Lift = Mth.lerp(Share, BindRest[Index], BindKneel[Index]);
        ScarletArena Where = arena();
        Vec3 Way = Where == null ? new Vec3(Math.sin(Index * Math.PI * 2.0 / 7.0), 0.0, -Math.cos(Index * Math.PI * 2.0 / 7.0)) : Where.anchor(Index).subtract(Base);
        Way = new Vec3(Way.x, 0.0, Way.z);
        Way = Way.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : Way.normalize();
        return Base.add(Way.scale(BindReach[Index])).add(0.0, Lift, 0.0);
    }

    public Vec3 anchor(int Band, float Partial) {
        float Share = kneelShare(Partial);
        Vec3 Hand = HandRest.lerp(HandKneel, Share);
        Vec3 Chest0 = ChestRest.lerp(ChestKneel, Share);
        return switch (Band) {
            case 0 -> local(new Vec3(Hand.x, Hand.y, Hand.z));
            case 1 -> local(new Vec3(-Hand.x, Hand.y, Hand.z));
            case 2 -> local(Chest0.add(0.0, 1.5, 0.0));
            case 3 -> local(new Vec3(1.9, 8.25 - Share * 5.0, 0.6));
            case 4 -> local(new Vec3(-1.9, 8.25 - Share * 5.0, 0.6));
            case 5 -> local(Chest0.add(0.0, -3.0, 0.2));
            default -> local(HeadRest.lerp(HeadKneel, Share).add(0.0, -2.0, 0.0));
        };
    }

    private void placeParts() {
        float Share = kneelShare(0.0f);
        Vec3 ChestAt = ChestRest.lerp(ChestKneel, Share);
        Vec3 HeadAt = HeadRest.lerp(HeadKneel, Share);
        Vec3 Hand = HandRest.lerp(HandKneel, Share);
        move(Hem, local(Vec3.ZERO).subtract(0.0, 0.0, 0.0), 0.0);
        move(Chest, local(ChestAt), Chest.getBbHeight() / 2.0);
        move(Head, local(HeadAt), Head.getBbHeight() / 2.0);
        move(HandA, local(new Vec3(Hand.x, Hand.y, Hand.z)), HandA.getBbHeight() / 2.0);
        Vec3 Second = local(new Vec3(-Hand.x, Hand.y, Hand.z));
        if (action() == Slam) {
            int Time = level().isClientSide ? (int) actionAge(0.0f) : ActionTime;
            float Reach = Time < 8 ? Time / 8.0f : Time < 45 ? 1.0f : Math.max(0.0f, 1.0f - (Time - 45) / 7.0f);
            Vec3 Down = spot().add(0.0, 1.5, 0.0);
            Second = Second.lerp(Down, Reach);
        }
        move(HandB, Second, HandB.getBbHeight() / 2.0);
    }

    private static void move(ScarletKingPart Part, Vec3 Centre, double Half) {
        Vec3 Old = Part.position();
        Part.setPos(Centre.x, Centre.y - Half, Centre.z);
        Part.xo = Old.x;
        Part.yo = Old.y;
        Part.zo = Old.z;
        Part.xOld = Old.x;
        Part.yOld = Old.y;
        Part.zOld = Old.z;
    }

    @Override
    public void tick() {
        super.tick();
        placeParts();
        if (level() instanceof ServerLevel World && !isDeadOrDying()) {
            serverTick(World);
        }
    }

    private void serverTick(ServerLevel World) {
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        if (Generation != Data.generation() || Data.king() != null && !Data.king().equals(getUUID())) {
            discard();
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        syncAnchors(World, false);
        if (!Fighting) {
            ManifestAge++;
            if (ManifestAge == 1) {
                setAction(Manifest);
            }
            if (action() == Manifest && ActionTime >= ManifestTicks) {
                setAction(Idle);
            }
            if (Binding && shown() < ScarletArena.Count && tickCount % 4 == 0) {
                entityData.set(ShownData, shown() + 1);
                entityData.set(BandsData, bands() | 1 << shown() - 1);
                sound(World, ScarletKingRegistry.Chain.get(), 4.0f, 0.8f + shown() * 0.05f);
            }
            ActionTime++;
            return;
        }
        watchCourt(World);
        if (!warded()) {
            FightTicks++;
        }
        if (tickCount % 10 == 0) {
            refreshBar(World);
        }
        if (tickCount % 20 == 0 || Target != null && !valid(Target)) {
            Target = pickTarget(World);
            entityData.set(TargetData, Target == null ? -1 : Target.getId());
        }
        tickRings(World);
        clock();
        int Action = action();
        if (Action == Idle) {
            faceTarget(2.0f);
            Cooldown--;
            if (Cooldown <= 0) {
                choose(World);
            }
        } else if (Action >= Lash && Action <= Shades) {
            tickAttack(World, Action);
        }
        if (tickCount % 30 == 0) {
            World.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getY() + 1.0, getZ(), 12, 6.0, 0.6, 6.0, 0.01);
        }
    }

    public void clock() {
        int Action = action();
        if (Action == Snap && ActionTime >= SnapTicks && warded()) {
            setAction(Idle);
            Cooldown = 40;
        } else if (Action == Snap && ActionTime >= SnapTicks) {
            KneelLeft = ScarletKingConfig.kneelTicks();
            setAction(Kneel);
            if (level() instanceof ServerLevel World) {
                sound(World, ScarletKingRegistry.Kneel.get(), 6.0f, 0.95f + random.nextFloat() * 0.1f);
            }
        } else if (Action == Kneel) {
            KneelLeft--;
            if (KneelLeft <= 0) {
                setAction(Rise);
                if (level() instanceof ServerLevel World) {
                    sound(World, ScarletKingRegistry.RiseUp.get(), 5.0f, 0.95f + random.nextFloat() * 0.1f);
                }
            }
        } else if (Action == Rise && ActionTime >= RiseTicks) {
            setAction(Idle);
            Cooldown = 30;
        }
        ActionTime++;
    }

    public void simulating(boolean Value) {
        Simulating = Value;
    }

    public void simulate(int Ticks) {
        for (int Index = 0; Index < Ticks; Index++) {
            FightTicks++;
            clock();
            if (action() >= Lash && action() <= Shades) {
                setAction(Idle);
            }
        }
    }

    private boolean valid(Player Candidate) {
        if (!Candidate.isAlive() || Candidate.isSpectator() || Candidate.isCreative() || Candidate.level() != level()) {
            return false;
        }
        ScarletArena Where = arena();
        Vec3 Centre = Where == null ? position() : Where.centre();
        return Candidate.position().distanceToSqr(Centre) < (ScarletArena.FightRadius + 16.0) * (ScarletArena.FightRadius + 16.0);
    }

    @Nullable
    private Player pickTarget(ServerLevel World) {
        Player Best = null;
        double Closest = Double.MAX_VALUE;
        for (ServerPlayer Candidate : World.players()) {
            if (!valid(Candidate)) {
                continue;
            }
            double Distance = Candidate.distanceToSqr(this);
            if (Distance < Closest) {
                Closest = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    private void faceTarget(float Speed) {
        if (Target == null) {
            return;
        }
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        float Wanted = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        float Turn = Mth.clamp(Mth.wrapDegrees(Wanted - getYRot()), -Speed, Speed);
        float Yaw = getYRot() + Turn;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
    }

    public void refreshBar(ServerLevel World) {
        ServerBossEvent Bar = bar();
        if (Bar == null) {
            return;
        }
        ScarletArena Where = arena();
        Vec3 Centre = Where == null ? position() : Where.centre();
        double Reach = ScarletKingConfig.playerRadius();
        List<ServerPlayer> Wanted = new ArrayList<>();
        for (ServerPlayer Player0 : World.players()) {
            if (!Player0.isRemoved() && Player0.isAlive() && Player0.level() == World && Player0.position().distanceToSqr(Centre) < Reach * Reach) {
                Wanted.add(Player0);
            }
        }
        ScarletKingBar.sync(Bar, Wanted);
        style();
    }

    private void style() {
        ServerBossEvent Bar = bar();
        if (Bar == null) {
            return;
        }
        int Stage = stage();
        Bar.setName(Component.translatable(enraged() ? "boss.scp_unleashed.scarlet_king.enraged" : "boss.scp_unleashed.scarlet_king." + Math.min(3, Stage)));
        Bar.setColor(Stage >= 3 ? BossEvent.BossBarColor.WHITE : Stage == 2 ? BossEvent.BossBarColor.PURPLE : Stage == 1 ? BossEvent.BossBarColor.PINK : BossEvent.BossBarColor.RED);
    }

    private int cooldown() {
        float Scale = 1.0f + 0.06f * Integer.bitCount(bands());
        if (stage() >= 3) {
            Scale *= 0.75f;
        }
        if (enraged()) {
            Scale *= 0.7f;
        }
        if (warded()) {
            Scale *= WardCooldown;
        }
        return Math.round(BaseCooldown * Scale) + random.nextInt(20);
    }

    private float scaled(float Damage) {
        return enraged() ? Damage * 1.5f : Damage;
    }

    private void choose(ServerLevel World) {
        if (Target == null) {
            Cooldown = 20;
            return;
        }
        List<Integer> Options = new ArrayList<>(List.of(Lash, Nova, Thorns));
        if (warded()) {
            Options.remove((Integer) LastAttack);
            startAttack(World, Options.get(random.nextInt(Options.size())));
            LastAttack = action();
            return;
        }
        if (stage() >= 1) {
            Options.add(Slam);
            Options.add(Shades);
        }
        if (stage() >= 2) {
            Options.add(Beam);
            Options.add(Beam);
        }
        if (Options.size() > 1) {
            Options.remove((Integer) LastAttack);
        }
        if (World.getEntitiesOfClass(Entity.class, getBoundingBox().inflate(ScarletArena.FightRadius), Each -> Each.getTags().contains(ShadeTag)).size() >= MaxShades) {
            Options.remove((Integer) Shades);
        }
        int Pick = Options.get(random.nextInt(Options.size()));
        LastAttack = Pick;
        startAttack(World, Pick);
    }

    public void startAttack(ServerLevel World, int Pick) {
        setAction(Pick);
        if (Target != null) {
            faceTarget(180.0f);
        }
        switch (Pick) {
            case Lash -> {
                sound(World, ScarletKingRegistry.Chain.get(), 5.0f, 0.6f);
                sound(World, ScarletKingRegistry.Growl.get(), 5.0f, 0.7f);
            }
            case Nova -> {
                sound(World, ScarletKingRegistry.ChoirLow.get(), 6.0f, 0.5f);
                sound(World, ScarletKingRegistry.ChoirBell.get(), 5.0f, 0.5f);
            }
            case Slam -> {
                Vec3 Aim = Target == null ? forward() : Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                double Distance = Mth.clamp(Aim.length(), 6.0, 16.0);
                Spot = ground(World, position().add(Aim.normalize().scale(Distance)));
                entityData.set(SpotData, new Vector3f((float) Spot.x, (float) Spot.y, (float) Spot.z));
                Grabbed = null;
                sound(World, ScarletKingRegistry.Growl.get(), 5.0f, 0.6f);
            }
            case Beam -> {
                Vec3 Start = Target == null ? position().add(forward().scale(12.0)) : Target.position();
                entityData.set(BeamData, new Vector3f((float) Start.x, (float) Start.y, (float) Start.z));
                entityData.set(BeamStateData, 1);
                sound(World, ScarletKingRegistry.ChoirBell.get(), 6.0f, 0.7f);
            }
            case Thorns, Shades -> sound(World, ScarletKingRegistry.Roar.get(), 5.0f, 0.65f);
            default -> {
            }
        }
    }

    private void endAttack() {
        entityData.set(BeamStateData, 0);
        Grabbed = null;
    }

    private void finishAttack() {
        endAttack();
        setAction(Idle);
        Cooldown = cooldown();
    }

    private void tickAttack(ServerLevel World, int Action) {
        int Time = ActionTime;
        switch (Action) {
            case Lash -> {
                if (Time < 19 && Time % 3 == 0) {
                    lashTelegraph(World);
                }
                if (Time == 19) {
                    lashStrike(World);
                }
                if (Time >= 36) {
                    finishAttack();
                }
            }
            case Nova -> {
                if (Time < 35 && Time % 4 == 0) {
                    ringParticles(World, NovaStart + (Time % 8) * 0.25, 24);
                }
                if (Time == 35) {
                    burst(World, NovaData);
                }
                if (Time == 50 && stage() >= 2) {
                    burst(World, SecondNovaData);
                }
                if (Time >= 52) {
                    finishAttack();
                }
            }
            case Slam -> tickSlam(World, Time);
            case Beam -> tickBeam(World, Time);
            case Thorns -> {
                if (Time == 14) {
                    thornLines(World);
                }
                if (Time >= 44) {
                    finishAttack();
                }
            }
            case Shades -> {
                if (Time == 20) {
                    summonShades(World);
                }
                if (Time >= 44) {
                    finishAttack();
                }
            }
            default -> finishAttack();
        }
    }

    private boolean grounded(Player Victim) {
        return Victim.onGround() && Victim.getY() - getY() < 2.5;
    }

    private void lashTelegraph(ServerLevel World) {
        Vec3 Forward = forward();
        double Base = Math.atan2(Forward.z, Forward.x);
        for (double Radius = LashInner + 2.0; Radius <= LashOuter; Radius += 2.5) {
            for (int Side = -1; Side <= 1; Side += 2) {
                double Angle = Base + Side * Math.toRadians(LashHalfAngle);
                World.sendParticles(new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.08f), 1.6f), getX() + Math.cos(Angle) * Radius, getY() + 0.2,
                        getZ() + Math.sin(Angle) * Radius, 1, 0.1, 0.0, 0.1, 0.0);
            }
        }
    }

    private void lashStrike(ServerLevel World) {
        Vec3 Forward = forward();
        sound(World, ScarletKingRegistry.Chain.get(), 6.0f, 0.5f);
        sound(World, ScarletKingRegistry.Impact.get(), 5.0f, 0.6f);
        double Base = Math.atan2(Forward.z, Forward.x);
        for (double Angle = -LashHalfAngle; Angle <= LashHalfAngle; Angle += 8.0) {
            double Turn = Base + Math.toRadians(Angle);
            for (double Radius = 6.0; Radius <= LashOuter; Radius += 6.0) {
                World.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(Turn) * Radius, getY() + 0.8, getZ() + Math.sin(Turn) * Radius, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        for (ServerPlayer Victim : World.players()) {
            Vec3 Offset = Victim.position().subtract(position());
            double Flat = Math.sqrt(Offset.x * Offset.x + Offset.z * Offset.z);
            if (Flat < LashInner || Flat > LashOuter || !valid(Victim) || !grounded(Victim)) {
                continue;
            }
            double Dot = (Offset.x * Forward.x + Offset.z * Forward.z) / Flat;
            if (Dot < Math.cos(Math.toRadians(LashHalfAngle))) {
                continue;
            }
            if (Victim.hurt(damageSources().mobAttack(this), scaled(LashDamage))) {
                push(Victim, Offset.normalize().scale(1.1), 0.45);
            }
        }
    }

    private void burst(ServerLevel World, EntityDataAccessor<Integer> Which) {
        entityData.set(Which, tickCount);
        Rings.add(new int[]{tickCount});
        RingHits.add(new HashSet<>());
        sound(World, ScarletKingRegistry.Nova.get(), 6.0f, 0.5f);
        sound(World, ScarletKingRegistry.Flame.get(), 6.0f, 0.6f);
    }

    private void tickRings(ServerLevel World) {
        for (int Index = Rings.size() - 1; Index >= 0; Index--) {
            double Radius = NovaStart + (tickCount - Rings.get(Index)[0]) * NovaSpeed;
            if (Radius > NovaReach) {
                Rings.remove(Index);
                RingHits.remove(Index);
                continue;
            }
            if ((tickCount - Rings.get(Index)[0]) % 3 == 0) {
                ringParticles(World, Radius, Math.max(24, (int) (Radius * 3.0)));
            }
            for (ServerPlayer Victim : World.players()) {
                if (RingHits.get(Index).contains(Victim.getUUID()) || !valid(Victim)) {
                    continue;
                }
                double Flat = Math.sqrt(Victim.distanceToSqr(getX(), Victim.getY(), getZ()));
                if (Math.abs(Flat - Radius) < 1.2 && grounded(Victim) && Victim.getY() - getY() < 1.2) {
                    RingHits.get(Index).add(Victim.getUUID());
                    if (Victim.hurt(damageSources().mobAttack(this), scaled(NovaDamage))) {
                        push(Victim, Victim.position().subtract(position()).normalize().scale(0.8), 0.6);
                        Victim.igniteForSeconds(2.0f);
                    }
                }
            }
        }
    }

    private void ringParticles(ServerLevel World, double Radius, int Count) {
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Index * Math.PI * 2.0 / Count;
            World.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(Angle) * Radius, getY() + 0.3, getZ() + Math.sin(Angle) * Radius, 1, 0.05, 0.1, 0.05, 0.01);
        }
    }

    private void tickSlam(ServerLevel World, int Time) {
        if (Time < 22 && Time % 3 == 0) {
            for (int Index = 0; Index < 16; Index++) {
                double Angle = Index * Math.PI / 8.0;
                World.sendParticles(new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.08f), 1.8f), Spot.x + Math.cos(Angle) * GrabRadius, Spot.y + 0.15,
                        Spot.z + Math.sin(Angle) * GrabRadius, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        if (Time == 22) {
            sound(World, ScarletKingRegistry.Impact.get(), 5.0f, 0.5f);
            for (ServerPlayer Victim : World.players()) {
                if (valid(Victim) && Victim.distanceToSqr(Spot.x, Victim.getY(), Spot.z) < GrabRadius * GrabRadius && Victim.getY() - Spot.y < 3.0) {
                    if (Victim.hurt(damageSources().mobAttack(this), scaled(GrabDamage))) {
                        Grabbed = Victim.getUUID();
                        Vec3 Toward = position().subtract(Victim.position()).multiply(1.0, 0.0, 1.0).normalize().scale(0.25);
                        Victim.setDeltaMovement(Toward.x, 1.45, Toward.z);
                        Victim.hurtMarked = true;
                    }
                    break;
                }
            }
        }
        if (Time == 41) {
            sound(World, ScarletKingRegistry.Slam.get(), 6.0f, 0.6f);
            World.sendParticles(ParticleTypes.EXPLOSION_EMITTER, Spot.x, Spot.y + 0.5, Spot.z, 1, 0.0, 0.0, 0.0, 0.0);
            World.sendParticles(ParticleTypes.LAVA, Spot.x, Spot.y + 0.5, Spot.z, 20, 2.0, 0.3, 2.0, 0.1);
            if (Grabbed != null && World.getPlayerByUUID(Grabbed) instanceof ServerPlayer Held && Held.isAlive()) {
                Held.setDeltaMovement(0.0, -2.2, 0.0);
                Held.hurtMarked = true;
                Held.fallDistance = 0.0f;
                Held.invulnerableTime = 0;
                Held.hurt(damageSources().mobAttack(this), scaled(SlamDamage));
            }
            for (ServerPlayer Victim : World.players()) {
                if (valid(Victim) && !Victim.getUUID().equals(Grabbed) && Victim.distanceToSqr(Spot.x, Victim.getY(), Spot.z) < ShockRadius * ShockRadius && grounded(Victim)) {
                    if (Victim.hurt(damageSources().mobAttack(this), scaled(ShockDamage))) {
                        push(Victim, Victim.position().subtract(Spot).normalize().scale(0.7), 0.7);
                    }
                }
            }
        }
        if (Time >= 52) {
            finishAttack();
        }
    }

    private void tickBeam(ServerLevel World, int Time) {
        Vec3 Halo = local(HaloRest);
        Vec3 Point = beamPoint();
        if (Time == 20) {
            entityData.set(BeamStateData, 2);
            sound(World, ScarletKingRegistry.Beam.get(), 6.0f, 0.6f);
            sound(World, ScarletKingRegistry.Flame.get(), 6.0f, 0.5f);
        }
        if (Time >= 20 && Time < 60) {
            if (Target != null) {
                Vec3 Toward = Target.position().subtract(Point);
                double Length = Toward.length();
                if (Length > 1.0E-3) {
                    Point = Point.add(Toward.scale(Math.min(BeamSweep, Length) / Length));
                    Point = ground(World, Point);
                    entityData.set(BeamData, new Vector3f((float) Point.x, (float) Point.y, (float) Point.z));
                }
            }
            if (Time % 2 == 0) {
                World.sendParticles(ParticleTypes.LAVA, Point.x, Point.y + 0.2, Point.z, 2, 0.5, 0.1, 0.5, 0.05);
                World.sendParticles(ParticleTypes.FLAME, Point.x, Point.y + 0.3, Point.z, 6, 0.6, 0.2, 0.6, 0.02);
            }
            if (Time % 4 == 0) {
                for (ServerPlayer Victim : World.players()) {
                    if (valid(Victim) && nearSegment(Victim.position().add(0.0, 0.9, 0.0), Halo, Point) < BeamRadius) {
                        if (Victim.hurt(damageSources().mobAttack(this), scaled(BeamDamage))) {
                            Victim.igniteForSeconds(2.0f);
                        }
                    }
                }
            }
        }
        if (Time == 60) {
            entityData.set(BeamStateData, 0);
        }
        if (Time >= 72) {
            finishAttack();
        }
    }

    public static double nearSegment(Vec3 Point, Vec3 From, Vec3 To) {
        Vec3 Span = To.subtract(From);
        double Length = Span.lengthSqr();
        if (Length < 1.0E-6) {
            return Point.distanceTo(From);
        }
        double Share = Mth.clamp(Point.subtract(From).dot(Span) / Length, 0.0, 1.0);
        return Point.distanceTo(From.add(Span.scale(Share)));
    }

    private void thornLines(ServerLevel World) {
        if (Target == null) {
            return;
        }
        Vec3 Aim = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (Aim.lengthSqr() < 1.0E-4) {
            Aim = forward();
        }
        double Base = Math.atan2(Aim.z, Aim.x);
        int Lines = stage() >= 2 ? 5 : 3;
        for (int Line = 0; Line < Lines; Line++) {
            double Angle = Base + (Line - (Lines - 1) / 2.0) * Math.toRadians(stage() >= 2 ? 22.0 : 28.0);
            for (int Step = 0; Step < 16; Step++) {
                double Radius = 6.0 + Step * 2.0;
                Vec3 At = position().add(Math.cos(Angle) * Radius, 0.0, Math.sin(Angle) * Radius);
                ScarletThorn.spawn(World, this, ground(World, At), 16 + Step, 40, false);
            }
        }
        sound(World, ScarletKingRegistry.Thorn.get(), 6.0f, 0.6f);
    }

    private void summonShades(ServerLevel World) {
        ScarletArena Where = arena();
        Vec3 Centre = Where == null ? position() : Where.centre();
        EntityType<?> Kind = BuiltInRegistries.ENTITY_TYPE.getOptional(CultistId).orElse(EntityType.VEX);
        int Alive = World.getEntitiesOfClass(Entity.class, new AABB(BlockPos.containing(Centre)).inflate(ScarletArena.FightRadius + 8.0),
                Each -> Each.getTags().contains(ShadeTag)).size();
        int Wanted = Math.min(3, MaxShades - Alive);
        for (int Index = 0; Index < Wanted; Index++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Radius = 14.0 + random.nextDouble() * 10.0;
            Vec3 At = ground(World, Centre.add(Math.cos(Angle) * Radius, 0.0, Math.sin(Angle) * Radius));
            Entity Made = Kind.spawn(World, BlockPos.containing(At), MobSpawnType.MOB_SUMMONED);
            if (Made == null) {
                continue;
            }
            Made.addTag(ShadeTag);
            if (Made instanceof Vex Spirit) {
                Spirit.setLimitedLife(1200);
            }
            if (Made instanceof Mob Minion && Target != null) {
                Minion.setTarget(Target);
                Minion.setPersistenceRequired();
            }
            World.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 1.0, At.z, 20, 0.4, 0.8, 0.4, 0.02);
            World.sendParticles(new DustParticleOptions(new Vector3f(0.8f, 0.05f, 0.05f), 2.0f), At.x, At.y + 1.0, At.z, 20, 0.4, 0.8, 0.4, 0.02);
        }
    }

    private static void push(LivingEntity Victim, Vec3 Away, double Lift) {
        Victim.setDeltaMovement(Victim.getDeltaMovement().add(Away.x, Lift, Away.z));
        Victim.hurtMarked = true;
    }

    public void sound(ServerLevel World, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, getX(), getY() + 12.0, getZ(), Sound, SoundSource.HOSTILE, Volume, Pitch);
    }

    @Override
    public void die(DamageSource Source) {
        if (dead) {
            return;
        }
        super.die(Source);
        endAttack();
        setAction(Dying);
        ServerBossEvent Bar = bar();
        if (Bar != null) {
            Bar.setProgress(0.0f);
        }
        if (level() instanceof ServerLevel World) {
            ScarletKingFall.fall(World, this, Source);
        }
    }

    public void clearBar() {
        if (!level().isClientSide) {
            ScarletKingBar.clear(Generation);
        }
    }

    public void syncAnchors(ServerLevel World, boolean All) {
        int Bands = All ? 0 : bands();
        int Shown = All ? ScarletArena.Count : shown();
        int Key = Bands | Shown << 8;
        if (Key == AnchorKey) {
            return;
        }
        AnchorKey = Key;
        ScarletArena Where = arena();
        if (Where == null) {
            return;
        }
        for (int Band = 0; Band < ScarletArena.Count; Band++) {
            BlockPos At = Where.Anchors().get(Band);
            if (!At.equals(Where.Pillars().get(Band))) {
                ScarletThrone.anchorState(World, At, Band < Shown && (Bands & 1 << Band) == 0);
            }
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel World) {
            if (deathTime == SlackTicks) {
                syncAnchors(World, true);
            }
            ScarletKingFall.dying(World, this, deathTime);
            if (deathTime == LootTick) {
                ScarletKingFall.finish(World, this);
            }
        }
        if (deathTime >= DeathTicks && !level().isClientSide) {
            clearBar();
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean isPickable() {
        return super.isPickable() && !isDeadOrDying();
    }

    @Override
    public boolean canBeHitByProjectile() {
        return !isDeadOrDying() && super.canBeHitByProjectile();
    }

    @Override
    public void remove(RemovalReason Reason) {
        clearBar();
        super.remove(Reason);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
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
    public void push(Entity Other) {
    }

    @Override
    public void travel(Vec3 Input) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    protected boolean canRide(Entity Vehicle) {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        ScarletArena Where = arena();
        AABB Own = getBoundingBox().inflate(10.0, 6.0, 10.0);
        if (Where == null) {
            return Own;
        }
        return Own.minmax(new AABB(Where.Throne()).inflate(ScarletArena.FightRadius + 4.0, 8.0, ScarletArena.FightRadius + 4.0).expandTowards(0.0, 30.0, 0.0));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 384.0 * 384.0;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return ScarletKingRegistry.Hurt.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putFloat("Pool", Pool);
        Tag.putFloat("PoolMax", PoolMax);
        Tag.putInt("FightTicks", FightTicks);
        Tag.putInt("Generation", Generation);
        Tag.putBoolean("Fighting", Fighting);
        Tag.putBoolean("Binding", Binding);
        Tag.putInt("Bands", bands());
        Tag.putInt("Shown", shown());
        Tag.putInt("Stage", stage());
        Tag.putInt("ManifestAge", ManifestAge);
        Tag.putBoolean("Warded", warded());
        Tag.putInt("Daughters", LastDaughters);
        Tag.putInt("Players", Players);
        ScarletArena Where = arena();
        if (Where != null) {
            Tag.put("Arena", Where.save());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Pool = Tag.contains("Pool") ? Tag.getFloat("Pool") : 1.0f;
        PoolMax = Math.max(1.0f, Tag.getFloat("PoolMax"));
        FightTicks = Tag.getInt("FightTicks");
        Generation = Tag.getInt("Generation");
        Fighting = Tag.getBoolean("Fighting");
        Binding = Tag.getBoolean("Binding");
        ManifestAge = Math.max(ManifestTicks + 1, Tag.getInt("ManifestAge"));
        entityData.set(WardData, Tag.getBoolean("Warded"));
        LastDaughters = Tag.getInt("Daughters");
        Players = Math.max(1, Tag.getInt("Players"));
        entityData.set(BandsData, Tag.getInt("Bands"));
        entityData.set(ShownData, Tag.getInt("Shown"));
        entityData.set(StageData, Tag.getInt("Stage"));
        if (Tag.contains("Arena")) {
            place(ScarletArena.load(Tag.getCompound("Arena")));
        }
        setAction(Idle);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 3, State -> {
            ScarletKing Owner = State.getAnimatable();
            if (Owner.actionTick() != Owner.seenActionTick()) {
                Owner.seenActionTick(Owner.actionTick());
                State.getController().forceAnimationReset();
            }
            return State.setAndContinue(animationFor(Owner.isDeadOrDying() ? Dying : Owner.action()));
        }));
    }

    public static RawAnimation animationFor(int Action) {
        return switch (Action) {
            case Lash -> RawAnimation.begin().thenPlay("lash").thenLoop("idle");
            case Nova -> RawAnimation.begin().thenPlay("nova").thenLoop("idle");
            case Slam -> RawAnimation.begin().thenPlay("slam").thenLoop("idle");
            case Beam -> RawAnimation.begin().thenPlay("beam").thenLoop("idle");
            case Thorns, Shades -> RawAnimation.begin().thenPlay("summon").thenLoop("idle");
            case Kneel -> RawAnimation.begin().thenPlayAndHold("kneel");
            case Rise -> RawAnimation.begin().thenPlay("rise").thenLoop("idle");
            case Snap -> RawAnimation.begin().thenPlay("snap").thenPlayAndHold("kneel");
            case Manifest -> RawAnimation.begin().thenPlay("manifest").thenLoop("idle");
            case Dying -> RawAnimation.begin().thenPlayAndHold("death");
            default -> RawAnimation.begin().thenLoop("idle");
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
