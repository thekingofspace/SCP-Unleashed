package net.rai.scp_unleashed.scp003;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.stranger.StrangerEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public abstract class Scp003Creature extends Monster implements GeoEntity {
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final byte ActionTransform = 2;
    public static final byte ActionPlace = 3;
    public static final byte ActionRitual = 4;
    public static final byte ActionChannel = 5;
    public static final byte ActionPromote = 6;
    public static final int EmergeTicks = 30;
    public static final int PromoteTicks = 60;
    public static final double AlertRange = 20.0;
    protected static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp003Creature.class, EntityDataSerializers.BYTE);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID Lineage;
    protected int Kills;
    protected int ActionTicks;
    protected int LifeTicks;
    private boolean Transferred;
    @Nullable
    private Scp003Gait Gait;

    protected Scp003Creature(EntityType<? extends Scp003Creature> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
    }

    public abstract int stage();

    public abstract void writeJade(CompoundTag Data);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
    }

    public UUID lineage() {
        if (Lineage == null) {
            Lineage = UUID.randomUUID();
        }
        return Lineage;
    }

    public int kills() {
        return Kills;
    }

    public int lifeTicks() {
        return LifeTicks;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean locksMovement() {
        byte Current = action();
        return Current != ActionNone && Current != ActionChannel;
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
    }

    @Nullable
    protected Scp003Gait.Profile gaitProfile() {
        return null;
    }

    @Nullable
    public Scp003Gait gait() {
        if (Gait == null && level().isClientSide) {
            Scp003Gait.Profile Shape = gaitProfile();
            if (Shape != null) {
                Gait = new Scp003Gait(Shape);
            }
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp003Gait Current = gait();
        return Current != null && Current.striding();
    }

    public void setGaitPartial(float Partial) {
        Scp003Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    public double gaitSpeed(double Reference) {
        if (Reference <= 0.0 || walkAnimation.speed() < 0.05f) {
            return 1.0;
        }
        return Math.max(0.6, Math.min(3.0, walkAnimation.speed() * 0.25 / Reference));
    }

    public void beginEmerging() {
        setAction(ActionEmerge, EmergeTicks);
    }

    public void inherit(Scp003Creature Source) {
        Lineage = Source.lineage();
        Kills = Source.Kills;
        LifeTicks = Source.LifeTicks;
        if (Source.hasCustomName()) {
            setCustomName(Source.getCustomName());
            setCustomNameVisible(Source.isCustomNameVisible());
        }
        setPersistenceRequired();
    }

    public void markTransferred() {
        Transferred = true;
    }

    public boolean isTransferred() {
        return Transferred;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp003Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
            return;
        }
        if (isAlive() && !Transferred) {
            LifeTicks++;
            if (ActionTicks > 0) {
                byte Current = action();
                actionTick(Current, ActionTicks);
                ActionTicks--;
                if (ActionTicks <= 0 && !Transferred && isAlive()) {
                    entityData.set(ActionData, ActionNone);
                    actionFinished(Current);
                }
            }
            if (!Transferred && isAlive()) {
                serverTick();
            }
        }
    }

    protected void serverTick() {
    }

    protected void actionTick(byte Kind, int Remaining) {
    }

    protected void actionFinished(byte Kind) {
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (Hit) {
            triggerAnim("action", "attack");
        }
        return Hit;
    }

    @Override
    public boolean killedEntity(ServerLevel World, LivingEntity Victim) {
        if (!(Victim instanceof Scp003Creature)) {
            Kills++;
        }
        return super.killedEntity(World, Victim);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide && Source.getEntity() instanceof LivingEntity Attacker && !(Attacker instanceof Scp003Creature) && Attacker.isAlive()) {
            if (!(Attacker instanceof Player User) || !User.isCreative() && !User.isSpectator()) {
                alertKin(Attacker);
            }
        }
        return Hurt;
    }

    protected void alertKin(LivingEntity Attacker) {
        for (Scp003Creature Kin : level().getEntitiesOfClass(Scp003Creature.class, getBoundingBox().inflate(AlertRange))) {
            if (Kin != this && Kin.stage() > 1 && Kin.getTarget() == null && Kin.isAlive()) {
                Kin.setTarget(Attacker);
            }
        }
    }

    public static boolean isQuarry(LivingEntity Target) {
        return Target.isAlive() && !(Target instanceof Scp003Creature) && !(Target instanceof StrangerEntity) && !(Target instanceof ArmorStand)
                && !(Target instanceof WaterAnimal) && !(Target instanceof AmbientCreature);
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return Other instanceof Scp003Creature || super.isAlliedTo(Other);
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return !(Target instanceof Scp003Creature) && super.canAttack(Target);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (dead && level() instanceof ServerLevel World) {
            Scp003Colony.get(World.getServer()).creatureGone(this, World);
        }
    }

    @Override
    public void remove(Entity.RemovalReason Reason) {
        if (Reason == Entity.RemovalReason.DISCARDED && !Transferred && !dead && level() instanceof ServerLevel World) {
            Scp003Colony.get(World.getServer()).creatureGone(this, World);
        }
        super.remove(Reason);
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
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putUUID("Lineage", lineage());
        Tag.putInt("Kills", Kills);
        Tag.putInt("LifeTicks", LifeTicks);
        Tag.putByte("Action", action());
        Tag.putInt("ActionTicks", ActionTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.hasUUID("Lineage")) {
            Lineage = Tag.getUUID("Lineage");
        }
        Kills = Tag.getInt("Kills");
        LifeTicks = Tag.getInt("LifeTicks");
        byte Saved = Tag.getByte("Action");
        int Ticks = Tag.getInt("ActionTicks");
        entityData.set(ActionData, Ticks > 0 ? Saved : ActionNone);
        ActionTicks = Math.max(0, Ticks);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
