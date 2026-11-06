package net.rai.scp_unleashed.scp073;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp073Cain extends DClassLike implements ControlRefusing {
    public static final float Width = 0.6f;
    public static final float Height = 1.8f;
    public static final float EyeHeight = 1.62f;
    public static final double MaxHealth = 20.0;
    public static final double MoveSpeed = 0.25;
    public static final double StrollSpeed = 0.55;
    public static final double HomeSpeed = 0.6;
    public static final double EscapeSpeed = 1.5;
    public static final int StrollInterval = 50;
    public static final int HomeRadius = 40;
    public static final int HomeDrift = 96;
    public static final float LookRange = 10.0f;
    public static final float LookChance = 0.06f;
    public static final float Glance = 0.6f;
    public static final int Lines = 8;
    public static final int LineCooldown = 30;
    public static final int AttendTicks = 70;
    public static final float SandValue = 10.0f;
    public static final float DesertValue = 5.0f;
    public static final float WetValue = -20.0f;
    public static final float WaterBorderCost = 16.0f;

    private final Scp073Blight Blight = new Scp073Blight();
    @Nullable
    private UUID Listener;
    private int AttendLeft;
    private int LineReady;
    private int LastLine = -1;

    public Scp073Cain(EntityType<? extends Scp073Cain> Type, Level World) {
        super(Type, World);
        setPathfindingMalus(PathType.WATER, -1.0f);
        setPathfindingMalus(PathType.WATER_BORDER, WaterBorderCost);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp073Goals.LeaveWater(this));
        goalSelector.addGoal(2, new Scp073Goals.Attend(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, HomeSpeed));
        goalSelector.addGoal(5, new Scp073Goals.Wander(this, StrollSpeed, StrollInterval));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, LookRange, LookChance));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        restrictTo(blockPosition(), HomeRadius);
        return Result;
    }

    @Override
    public float glanceRate() {
        return Glance;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.scp073.refuses");
    }

    public static boolean isDesert(LevelReader World, BlockPos Pos) {
        return World.getBiome(Pos).is(Tags.Biomes.IS_DESERT);
    }

    @Override
    public float getWalkTargetValue(BlockPos Pos, LevelReader World) {
        float Value = 0.0f;
        if (World.getBlockState(Pos.below()).is(BlockTags.SAND)) {
            Value += SandValue;
        }
        if (isDesert(World, Pos)) {
            Value += DesertValue;
        }
        if (!World.getFluidState(Pos).isEmpty() || !World.getFluidState(Pos.below()).isEmpty()) {
            Value += WetValue;
        }
        return Value;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypes.GENERIC_KILL) || Source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(Source, Amount);
        }
        if (level() instanceof ServerLevel World && isAlive()) {
            Scp073Reflection.reflect(World, this, Source, Amount);
        }
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return Effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL && super.canBeAffected(Effect);
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
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
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND || !User.getMainHandItem().isEmpty() || User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (User instanceof ServerPlayer Speaker) {
            listen(Speaker);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void listen(ServerPlayer Speaker) {
        Listener = Speaker.getUUID();
        AttendLeft = AttendTicks;
        getNavigation().stop();
        if (tickCount < LineReady) {
            return;
        }
        LineReady = tickCount + LineCooldown;
        int Line = getRandom().nextInt(Lines);
        if (Line == LastLine) {
            Line = (Line + 1 + getRandom().nextInt(Lines - 1)) % Lines;
        }
        LastLine = Line;
        Component Spoken = Component.translatable("message.scp_unleashed.scp073.line." + Line);
        Speaker.displayClientMessage(Senses.heard(Speaker, Component.translatable("message.scp_unleashed.scp073.speech", getDisplayName(), Spoken).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)), true);
    }

    @Nullable
    public Player listener() {
        if (Listener == null || AttendLeft <= 0) {
            return null;
        }
        Player Found = level().getPlayerByUUID(Listener);
        return Found != null && Found.isAlive() && !Found.isSpectator() && Found.distanceToSqr(this) < 64.0 ? Found : null;
    }

    public boolean isAttending() {
        return listener() != null;
    }

    public void tickAttention() {
        if (AttendLeft > 0) {
            AttendLeft--;
        }
        if (AttendLeft <= 0) {
            Listener = null;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel World && isAlive()) {
            tickAttention();
            if (!hasRestriction() || getRestrictCenter().distSqr(blockPosition()) > (double) HomeDrift * HomeDrift) {
                restrictTo(blockPosition(), HomeRadius);
            }
            Blight.tick(World, this);
        }
    }

    @Override
    public void remove(Entity.RemovalReason Reason) {
        if (level() instanceof ServerLevel World) {
            Blight.clear(World, this);
        }
        super.remove(Reason);
    }

    public int witheringCount() {
        return Blight.activeCount();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (hasRestriction()) {
            Saved.putLong("Home", getRestrictCenter().asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        if (Saved.contains("Home")) {
            restrictTo(BlockPos.of(Saved.getLong("Home")), HomeRadius);
        }
        setPersistenceRequired();
    }
}
