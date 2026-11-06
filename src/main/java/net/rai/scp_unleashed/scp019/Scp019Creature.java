package net.rai.scp_unleashed.scp019;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.rai.scp_unleashed.stranger.StrangerEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class Scp019Creature extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn {
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final byte ActionForm = 2;
    public static final byte ActionEnter = 3;
    public static final byte ActionSqueal = 4;
    public static final byte ActionSlam = 5;
    public static final int CheckTicks = 20;
    public static final double PuffViewRange = 160.0;
    protected static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp019Creature.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp019Creature.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> LockedYawData = SynchedEntityData.defineId(Scp019Creature.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos PotPos;
    @Nullable
    private ResourceKey<Level> PotDimension;
    private int Session;
    private boolean Owned;
    @Nullable
    private Vec3 Anchor;
    protected int ActionTicks;
    private float RenderPartial;
    private int TickOrigin;
    @Nullable
    private Scp019Gait Gait;

    protected Scp019Creature(EntityType<? extends Scp019Creature> Type, Level World) {
        super(Type, World);
        xpReward = 0;
        setPersistenceRequired();
    }

    public abstract int emergeTicks();

    public abstract int formTicks();

    public abstract double leash();

    protected abstract Scp019Gait.Shape gaitShape();

    public abstract String state();

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(LockedYawData, 0.0f);
    }

    public void bind(Scp019PotBlockEntity Pot) {
        PotPos = Pot.getBlockPos().immutable();
        PotDimension = level().dimension();
        Session = Pot.session();
        Owned = true;
        Anchor = Pot.centre();
        restrictTo(PotPos, (int) leash());
    }

    public boolean isOwned() {
        return Owned;
    }

    @Nullable
    public BlockPos potPos() {
        return PotPos;
    }

    @Nullable
    public ResourceKey<Level> potDimension() {
        return PotDimension;
    }

    @Nullable
    public Scp019PotBlockEntity home() {
        if (!Owned || PotPos == null) {
            return null;
        }
        Level World = level();
        if (PotDimension != null && !World.dimension().equals(PotDimension) || !World.isLoaded(PotPos)) {
            return null;
        }
        if (!(World.getBlockEntity(PotPos) instanceof Scp019PotBlockEntity Pot) || Pot.isRemoved() || !Pot.isOpen() || Pot.session() != Session || !Pot.owns(getUUID())) {
            return null;
        }
        return Pot;
    }

    public Vec3 anchor() {
        if (Anchor == null) {
            Anchor = position();
        }
        return Anchor;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean locksMovement() {
        byte Current = action();
        return Current != ActionNone && Current != ActionSlam;
    }

    public boolean locksYaw() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionForm || Current == ActionEnter;
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(LockedYawData, getYRot());
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
            applyLockedYaw();
        }
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    public int actionStart() {
        return entityData.get(ActionStartData);
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionEmerge -> "emerge";
            case ActionForm -> "form";
            case ActionEnter -> "enter";
            case ActionSqueal -> "squeal";
            case ActionSlam -> "attack";
            default -> null;
        };
    }

    public boolean isFreshlyForming() {
        byte Current = action();
        boolean Kind = Current == ActionEmerge && ActionTicks == emergeTicks() || Current == ActionForm && ActionTicks == formTicks();
        return Kind && actionStart() == tickCount;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    public static boolean isActionClip(String Name) {
        return "emerge".equals(Name) || "form".equals(Name) || "enter".equals(Name) || "squeal".equals(Name) || "attack".equals(Name);
    }

    public void applyLockedYaw() {
        if (!locksYaw()) {
            return;
        }
        float Yaw = entityData.get(LockedYawData);
        setYRot(Yaw);
        yRotO = Yaw;
        yBodyRot = Yaw;
        yBodyRotO = Yaw;
        yHeadRot = Yaw;
        yHeadRotO = Yaw;
    }

    public void beginEmerging(boolean FromMouth) {
        setAction(FromMouth ? ActionEmerge : ActionForm, FromMouth ? emergeTicks() : formTicks());
    }

    @Nullable
    public Scp019Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp019Gait(gaitShape());
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp019Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        Scp019Gait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    public void setGaitPartial(float Partial) {
        RenderPartial = Partial;
        Scp019Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void tick() {
        super.tick();
        applyLockedYaw();
        if (level().isClientSide) {
            Scp019Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
            return;
        }
        if (!isAlive()) {
            return;
        }
        if (ActionTicks > 0) {
            byte Current = action();
            if (locksYaw()) {
                setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            }
            actionTick(Current, ActionTicks);
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved()) {
                entityData.set(ActionData, ActionNone);
                actionFinished(Current);
            }
        }
        if (isRemoved()) {
            return;
        }
        if (Owned && (tickCount + getId()) % CheckTicks == 0 && home() == null) {
            puffAway();
            return;
        }
        serverTick();
    }

    protected void serverTick() {
    }

    protected void actionTick(byte Kind, int Remaining) {
    }

    protected void actionFinished(byte Kind) {
    }

    public void puffAway() {
        if (!(level() instanceof ServerLevel World) || isRemoved()) {
            return;
        }
        double Height = getBbHeight();
        double Width = getBbWidth();
        double Middle = getY() + Height * 0.5;
        int Poofs = (int) (8 + 16 * Width * Height);
        int Smoke = (int) (3 + 5 * Height);
        for (ServerPlayer Viewer : World.players()) {
            if (Viewer.distanceToSqr(this) < PuffViewRange * PuffViewRange) {
                World.sendParticles(Viewer, ParticleTypes.POOF, true, getX(), Middle, getZ(), Poofs, Width * 0.45, Height * 0.35, Width * 0.45, 0.03);
                World.sendParticles(Viewer, ParticleTypes.LARGE_SMOKE, true, getX(), Middle, getZ(), Smoke, Width * 0.35, Height * 0.3, Width * 0.35, 0.015);
            }
        }
        float Pitch = (float) (1.35 - Height * 0.25) * (0.92f + random.nextFloat() * 0.16f);
        World.playSound(null, getX(), Middle, getZ(), Scp019Registry.Puff.get(), SoundSource.HOSTILE, (float) (0.8 + Height * 0.25), Pitch);
        World.playSound(null, getX(), Middle, getZ(), Scp019Registry.PuffHiss.get(), SoundSource.HOSTILE, 0.5f, Pitch * 1.1f);
        discard();
    }

    public static boolean isValidTarget(@Nullable Entity Found, boolean SparePlayers) {
        if (!(Found instanceof LivingEntity Target) || Found instanceof Projectile || Found instanceof ArmorStand || Found instanceof Scp019Creature
                || Found instanceof StrangerEntity || Found instanceof WaterAnimal || Found instanceof AmbientCreature) {
            return false;
        }
        if (Target.isRemoved() || !Target.isAlive() || Target.isDeadOrDying() || Target.isInvisible() || Target.isSpectator() || !Target.canBeSeenAsEnemy()) {
            return false;
        }
        if (Target instanceof Player User) {
            return !SparePlayers && !User.isCreative();
        }
        return true;
    }

    public boolean isValidTarget(@Nullable Entity Found) {
        return isValidTarget(Found, sparesPlayers());
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
        if (!locksMovement()) {
            super.knockback(Strength, X, Z);
        }
    }

    public boolean sparesPlayers() {
        return level().getDifficulty() == Difficulty.PEACEFUL;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return Other instanceof Scp019Creature || super.isAlliedTo(Other);
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return isValidTarget(Target) && super.canAttack(Target);
    }

    @Override
    protected void pushEntities() {
        if (level().isClientSide()) {
            super.pushEntities();
            return;
        }
        for (Entity Other : level().getEntities(this, getBoundingBox(), EntitySelector.pushableBy(this))) {
            doPush(Other);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB Box = super.getBoundingBoxForCulling();
        return isActing() ? Box.inflate(2.5) : Box;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
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
    public boolean saveAsPassenger(CompoundTag Tag) {
        return !Owned && super.saveAsPassenger(Tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("Owned", Owned);
        Tag.putInt("Session", Session);
        if (PotPos != null) {
            Tag.put("PotPos", NbtUtils.writeBlockPos(PotPos));
        }
        if (PotDimension != null) {
            Tag.putString("PotDimension", PotDimension.location().toString());
        }
        if (Anchor != null) {
            Tag.putDouble("AnchorX", Anchor.x);
            Tag.putDouble("AnchorY", Anchor.y);
            Tag.putDouble("AnchorZ", Anchor.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Owned = Tag.getBoolean("Owned");
        Session = Tag.getInt("Session");
        PotPos = NbtUtils.readBlockPos(Tag, "PotPos").orElse(null);
        ResourceLocation Dimension = Tag.contains("PotDimension") ? ResourceLocation.tryParse(Tag.getString("PotDimension")) : null;
        PotDimension = Dimension != null ? ResourceKey.create(Registries.DIMENSION, Dimension) : null;
        if (Tag.contains("AnchorX")) {
            Anchor = new Vec3(Tag.getDouble("AnchorX"), Tag.getDouble("AnchorY"), Tag.getDouble("AnchorZ"));
        }
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
