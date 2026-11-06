package net.rai.scp_unleashed.scp439;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class Scp439Queen extends Scp439Creature {
    public static final float Width = 1.2f;
    public static final float Height = 0.65f;
    public static final float EyeHeight = 0.4f;
    public static final float RenderScale = 1.0f;
    public static final byte ActionBite = 2;
    public static final byte ActionBurrow = 3;
    public static final int BiteTicks = 20;
    public static final int BiteHitTick = 11;
    public static final int BurrowTicks = 24;
    public static final int EmergeTicks = 30;
    public static final int DeathTicks = 32;
    public static final double HuntRange = 48.0;
    public static final double BiteReach = 1.0;
    public static final double StrikeReach = 1.8;
    public static final float BiteDamage = 1.0f;
    public static final int MinTravel = 48;
    public static final int TravelMin = 64;
    public static final int TravelMax = 128;
    public static final int RebuffTicks = 6000;
    public static final int RestTicks = 600;
    public static final int StingGap = 60;
    public static final double RunEnter = 0.075;
    public static final double RunExit = 0.06;

    @Nullable
    private UUID Prey;
    @Nullable
    private UUID Burrowing;
    private boolean Daughter;
    @Nullable
    private BlockPos Origin;
    @Nullable
    private BlockPos Waypoint;
    @Nullable
    private UUID Ignored;
    private long IgnoreUntil;
    private long RestUntil;
    private int Generation;
    private int StingCooldown;

    public Scp439Queen(EntityType<? extends Scp439Queen> Type, Level World) {
        super(Type, World);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Scp439Creature.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, HuntRange).add(Attributes.KNOCKBACK_RESISTANCE, 0.5).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp439Goals.QueenHunt(this));
        goalSelector.addGoal(2, new Scp439Goals.QueenTravel(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected Scp439Stride[] strides() {
        return new Scp439Stride[]{Scp439Gaits.QueenWalk, Scp439Gaits.QueenRun};
    }

    @Override
    protected double runEnter() {
        return RunEnter;
    }

    @Override
    protected double runExit() {
        return RunExit;
    }

    @Override
    protected double idleSeconds() {
        return 3.6;
    }

    @Override
    public float renderScale() {
        return RenderScale;
    }

    @Override
    public String modelName() {
        return "scp439_queen";
    }

    @Override
    @Nullable
    protected String clipFor(byte Action) {
        return Action == ActionBite ? "bite" : Action == ActionBurrow ? "burrow" : null;
    }

    @Override
    protected int emergeTicks() {
        return EmergeTicks;
    }

    @Override
    protected int deathTicks() {
        return DeathTicks;
    }

    @Override
    protected byte memberKind() {
        return -1;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> idleSounds() {
        return Scp439Registry.QueenIdle;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> hurtSounds() {
        return Scp439Registry.QueenHurt;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> deathSounds() {
        return Scp439Registry.QueenDeath;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> stepSounds() {
        return Scp439Registry.QueenStep;
    }

    @Override
    protected float stepVolume() {
        return 0.45f;
    }

    public boolean isDaughter() {
        return Daughter;
    }

    @Nullable
    public BlockPos origin() {
        return Origin;
    }

    @Nullable
    public BlockPos waypoint() {
        return Waypoint;
    }

    public int generation() {
        return Generation;
    }

    public void makeDaughter(BlockPos From, int Born) {
        Daughter = true;
        Origin = From.immutable();
        Generation = Born;
        double Angle = random.nextDouble() * Mth.TWO_PI;
        double Reach = TravelMin + random.nextDouble() * (TravelMax - TravelMin);
        Waypoint = From.offset(Mth.floor(Math.cos(Angle) * Reach), 0, Mth.floor(Math.sin(Angle) * Reach));
    }

    public void repickWaypoint() {
        if (Origin != null) {
            makeDaughter(Origin, Generation);
        }
    }

    public double travelled() {
        return Origin == null ? Double.MAX_VALUE : Math.sqrt(Origin.distToCenterSqr(getX(), Origin.getY() + 0.5, getZ()));
    }

    public boolean stillTravelling() {
        return Daughter && Origin != null && travelled() < MinTravel;
    }

    public void rebuff(@Nullable LivingEntity From) {
        if (From != null) {
            Ignored = From.getUUID();
            IgnoreUntil = level().getGameTime() + RebuffTicks;
        }
        RestUntil = level().getGameTime() + RestTicks;
    }

    public boolean resting() {
        return level().getGameTime() < RestUntil;
    }

    public void adopt(CompoundTag Carried) {
        Generation = Carried.getInt("Generation");
        if (Carried.contains("CustomName")) {
            setCustomName(Component.Serializer.fromJson(Carried.getString("CustomName"), registryAccess()));
        }
    }

    public CompoundTag hostTag() {
        CompoundTag Carried = new CompoundTag();
        Carried.putInt("Generation", Generation);
        Component Named = getCustomName();
        if (Named != null) {
            Carried.putString("CustomName", Component.Serializer.toJson(Named, registryAccess()));
        }
        return Carried;
    }

    public static boolean canHost(LivingEntity Found) {
        if (!Found.isAlive() || Found.isRemoved() || Scp439Infection.isInfected(Found) || isColony(Found) || !Scp439Infection.canCarry(Found)) {
            return false;
        }
        if (Found instanceof Player User) {
            return !User.isCreative() && !User.isSpectator() && !Scp268.isNeglected(User);
        }
        return true;
    }

    public boolean wants(LivingEntity Found) {
        if (!canHost(Found) || Found.level() != level()) {
            return false;
        }
        if (Ignored != null && Ignored.equals(Found.getUUID()) && level().getGameTime() < IgnoreUntil) {
            return false;
        }
        return !Daughter || Origin == null || Math.sqrt(Origin.distToCenterSqr(Found.getX(), Origin.getY() + 0.5, Found.getZ())) >= MinTravel;
    }

    @Nullable
    public LivingEntity choosePrey() {
        AABB Area = getBoundingBox().inflate(HuntRange, 16.0, HuntRange);
        LivingEntity Best = null;
        double BestDistance = HuntRange * HuntRange;
        for (LivingEntity Found : level().getEntitiesOfClass(LivingEntity.class, Area, this::wants)) {
            double Distance = distanceToSqr(Found);
            if (Distance < BestDistance) {
                Best = Found;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    @Nullable
    public LivingEntity prey() {
        if (Prey == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(Prey) instanceof LivingEntity Found && wants(Found) ? Found : null;
    }

    public void setPrey(@Nullable LivingEntity Found) {
        Prey = Found == null ? null : Found.getUUID();
    }

    @Nullable
    public UUID preyId() {
        return Prey;
    }

    public boolean canSting() {
        return canAct() && StingCooldown <= 0 && !resting();
    }

    public boolean inBiteReach(LivingEntity Target) {
        return reachTo(Target) <= BiteReach && Math.abs(Target.getY() - getY()) < 1.6;
    }

    public void beginBite(LivingEntity Target) {
        faceToward(Target, 180.0f);
        setPrey(Target);
        setAction(ActionBite, BiteTicks);
        playLayers(Scp439Registry.QueenHiss, 1.0f, 1.0f);
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Kind == ActionBite) {
            LivingEntity Target = prey();
            if (Target != null && Elapsed < BiteHitTick) {
                faceToward(Target, 30.0f);
            }
            if (Elapsed == BiteHitTick) {
                StingCooldown = StingGap;
                if (Target != null && reachTo(Target) <= StrikeReach && Math.abs(Target.getY() - getY()) < 2.0) {
                    playLayers(Scp439Registry.QueenBite, 1.0f, 1.0f);
                    Target.hurt(damageSources().mobAttack(this), BiteDamage);
                    if (Target.isAlive() && Scp439Infection.infect(Target, this)) {
                        beginBurrow(Target);
                    }
                }
            }
        } else if (Kind == ActionBurrow) {
            Entity Host = Burrowing == null ? null : World.getEntity(Burrowing);
            if (Host != null && Host.isAlive()) {
                Vec3 Feet = Host.position();
                setPos(Mth.lerp(0.35, getX(), Feet.x), Mth.lerp(0.35, getY(), Feet.y), Mth.lerp(0.35, getZ(), Feet.z));
            }
            if (Elapsed % 4 == 0) {
                World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + 0.2, getZ(), 4, 0.3, 0.15, 0.3, 0.03);
            }
        }
    }

    public void beginBurrow(LivingEntity Host) {
        Burrowing = Host.getUUID();
        setAction(ActionBurrow, BurrowTicks);
        playLayers(Scp439Registry.QueenBurrow, 1.0f, 1.0f);
    }

    @Nullable
    public UUID burrowing() {
        return action() == ActionBurrow ? Burrowing : null;
    }

    public void abandonBurrow() {
        Burrowing = null;
        if (action() == ActionBurrow) {
            super.finishAction(ActionBurrow);
        }
        if (level() instanceof ServerLevel World) {
            Scp439Colonies.get(World).seeQueen(getUUID(), World.getGameTime());
        }
    }

    @Override
    protected void finishAction(byte Kind) {
        super.finishAction(Kind);
        if (Kind == ActionBurrow && level() instanceof ServerLevel World) {
            World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + 0.3, getZ(), 12, 0.3, 0.2, 0.3, 0.04);
            Scp439Colonies.get(World).removeQueen(getUUID());
            discard();
        }
    }

    @Override
    public void beginEmerge() {
        super.beginEmerge();
        playLayers(Scp439Registry.QueenEmerge, 1.0f, 1.0f);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return action() == ActionBurrow || super.isInvulnerableTo(Source);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isAlive()) {
            StingCooldown = Math.max(0, StingCooldown - 1);
            if (tickCount % SeenEvery == 0 && level() instanceof ServerLevel World) {
                Scp439Colonies.get(World).seeQueen(getUUID(), World.getGameTime());
            }
        }
    }

    @Override
    protected void forgetSelf(ServerLevel World) {
        Scp439Colonies.get(World).removeQueen(getUUID());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("Daughter", Daughter);
        if (Origin != null) {
            Tag.put("Origin", NbtUtils.writeBlockPos(Origin));
        }
        if (Waypoint != null) {
            Tag.put("Waypoint", NbtUtils.writeBlockPos(Waypoint));
        }
        if (Ignored != null) {
            Tag.putUUID("Ignored", Ignored);
            Tag.putLong("IgnoreUntil", IgnoreUntil);
        }
        Tag.putLong("RestUntil", RestUntil);
        Tag.putInt("Generation", Generation);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Daughter = Tag.getBoolean("Daughter");
        Origin = NbtUtils.readBlockPos(Tag, "Origin").orElse(null);
        Waypoint = NbtUtils.readBlockPos(Tag, "Waypoint").orElse(null);
        Ignored = Tag.hasUUID("Ignored") ? Tag.getUUID("Ignored") : null;
        IgnoreUntil = Tag.getLong("IgnoreUntil");
        RestUntil = Tag.getLong("RestUntil");
        Generation = Tag.getInt("Generation");
    }
}
