package net.rai.scp_unleashed.control;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class VacantBody extends Entity {
    public static final int CheckTicks = 40;
    public static final EntityDimensions SeatedSize = EntityDimensions.scalable(0.6f, 1.25f).withEyeHeight(1.05f);
    public static final EntityDimensions LimpSize = EntityDimensions.scalable(0.6f, 1.8f).withEyeHeight(1.55f);
    private static final EntityDataAccessor<Optional<UUID>> OwnerData = SynchedEntityData.defineId(VacantBody.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OwnerNameData = SynchedEntityData.defineId(VacantBody.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SeatedData = SynchedEntityData.defineId(VacantBody.class, EntityDataSerializers.BOOLEAN);

    public VacantBody(EntityType<? extends VacantBody> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public static VacantBody create(ServerPlayer Owner) {
        VacantBody Body = new VacantBody(ControlRegistry.VacantBodyType.get(), Owner.level());
        Body.moveTo(Owner.getX(), Owner.getY(), Owner.getZ(), Owner.getYRot(), 0.0f);
        Body.setYHeadRot(Owner.getYHeadRot());
        Body.entityData.set(OwnerData, Optional.of(Owner.getUUID()));
        Body.entityData.set(OwnerNameData, Owner.getGameProfile().getName());
        Body.entityData.set(SeatedData, Owner.onGround() || Owner.isPassenger());
        Body.refreshName();
        return Body;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(OwnerData, Optional.empty());
        Builder.define(OwnerNameData, "");
        Builder.define(SeatedData, true);
    }

    @Nullable
    public UUID getOwnerId() {
        return entityData.get(OwnerData).orElse(null);
    }

    public String getOwnerName() {
        return entityData.get(OwnerNameData);
    }

    public boolean isSeated() {
        return entityData.get(SeatedData);
    }

    @Override
    public EntityDimensions getDimensions(Pose Current) {
        return isSeated() ? SeatedSize : LimpSize;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (SeatedData.equals(Accessor)) {
            refreshDimensions();
        }
    }

    private void refreshName() {
        setCustomName(Component.translatable("entity.scp_unleashed.vacant_body.named", getOwnerName()));
        setCustomNameVisible(true);
    }

    @Override
    public void tick() {
        xo = getX();
        yo = getY();
        zo = getZ();
        xRotO = getXRot();
        yRotO = getYRot();
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel World && (tickCount + getId()) % CheckTicks == 0 && !stillWanted(World)) {
            discard();
        }
    }

    private boolean stillWanted(ServerLevel World) {
        UUID Owner = getOwnerId();
        if (Owner == null) {
            return false;
        }
        UUID Expected = ControlRoster.get(World.getServer()).vacantOf(Owner);
        if (!getUUID().equals(Expected)) {
            return false;
        }
        Player Online = World.getServer().getPlayerList().getPlayer(Owner);
        if (Online == null) {
            return true;
        }
        ControlSession Current = ControlSessions.session(Online);
        return Current.active() && Current.Vacant().map(getUUID()::equals).orElse(false);
    }

    @Override
    public void move(MoverType Type, Vec3 Motion) {
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public void kill() {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity Attacker) {
        return true;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition Transition) {
        return null;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
        if (Saved.hasUUID("Owner")) {
            entityData.set(OwnerData, Optional.of(Saved.getUUID("Owner")));
        }
        entityData.set(OwnerNameData, Saved.getString("OwnerName"));
        entityData.set(SeatedData, !Saved.contains("Seated") || Saved.getBoolean("Seated"));
        refreshName();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
        UUID Owner = getOwnerId();
        if (Owner != null) {
            Saved.putUUID("Owner", Owner);
        }
        Saved.putString("OwnerName", getOwnerName());
        Saved.putBoolean("Seated", isSeated());
    }
}
