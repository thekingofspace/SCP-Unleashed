package net.rai.scp_unleashed.rocketry.satellite;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.NoticeIcon;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.orbital.OrbitalLaunch;
import net.rai.scp_unleashed.rocketry.RocketryBursts;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public class SatelliteBoosterEntity extends Entity {
    public static final float Width = 0.8f;
    public static final float Height = 3.0f;
    public static final int IgnitionTicks = 40;
    public static final double Climb = 0.02;
    public static final double DeployAltitude = 160.0;
    public static final int DeployTicks = 260;
    public static final double RenderReach = 320.0;
    public static final NoticeIcon Icon = NoticeIcon.generic("satellite");

    private static final EntityDataAccessor<Integer> DataTicks = SynchedEntityData.defineId(SatelliteBoosterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DataStartY = SynchedEntityData.defineId(SatelliteBoosterEntity.class, EntityDataSerializers.FLOAT);
    private static Consumer<SatelliteBoosterEntity> ClientEffects = Booster -> {
    };

    private int FlightTicks;
    @Nullable
    private UUID Owner;
    @Nullable
    private String Named;
    @Nullable
    private String Payload;

    public SatelliteBoosterEntity(EntityType<? extends SatelliteBoosterEntity> Type, Level World) {
        super(Type, World);
        this.noPhysics = true;
        setNoGravity(true);
    }

    public static void setClientEffects(Consumer<SatelliteBoosterEntity> Effects) {
        ClientEffects = Effects;
    }

    public static SatelliteBoosterEntity launching(ServerLevel World, Vec3 At, float Yaw, @Nullable UUID Owner, @Nullable String Named) {
        SatelliteBoosterEntity Booster = new SatelliteBoosterEntity(RocketryRegistry.SatelliteBoosterType.get(), World);
        Booster.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Booster.entityData.set(DataStartY, (float) At.y);
        Booster.Owner = Owner;
        Booster.Named = Named;
        return Booster;
    }

    public SatelliteBoosterEntity withPayload(@Nullable String Id) {
        Payload = Id;
        return this;
    }

    @Nullable
    public String getPayload() {
        return Payload;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataTicks, 0);
        Builder.define(DataStartY, 0.0f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (level().isClientSide && Key.equals(DataTicks)) {
            FlightTicks = entityData.get(DataTicks);
        }
    }

    public int getFlightTicks() {
        return FlightTicks;
    }

    public double getStartY() {
        return entityData.get(DataStartY);
    }

    public boolean isClimbing() {
        return FlightTicks > IgnitionTicks;
    }

    public static double altitude(double Ticks) {
        double Burn = Ticks - IgnitionTicks;
        return Burn <= 0.0 ? 0.0 : 0.5 * Climb * Burn * Burn;
    }

    @Nullable
    public UUID getOwner() {
        return Owner;
    }

    @Nullable
    public String getNamed() {
        return Named;
    }

    @Override
    public void tick() {
        super.tick();
        FlightTicks++;
        setPos(getX(), getStartY() + altitude(FlightTicks), getZ());
        if (level().isClientSide) {
            ClientEffects.accept(this);
            return;
        }
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (FlightTicks % 20 == 0) {
            entityData.set(DataTicks, FlightTicks);
        }
        if (FlightTicks == 1) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : RocketryRegistry.IgnitionLayers) {
                World.playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.BLOCKS, 1.2f, 1.3f);
            }
        }
        if (FlightTicks == IgnitionTicks) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : RocketryRegistry.BoosterLayers) {
                World.playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.BLOCKS, 2.5f, 1.0f);
            }
            World.broadcastEntityEvent(this, RocketryBursts.Ignition);
        }
        if (altitude(FlightTicks) >= DeployAltitude || FlightTicks >= DeployTicks) {
            deploy(World);
        }
    }

    public void deploy(ServerLevel World) {
        if (isRemoved()) {
            return;
        }
        discard();
        if (OrbitalLaunch.deployed(World, Owner, Payload)) {
            return;
        }
        MinecraftServer Server = World.getServer();
        UUID Holder = Owner == null ? Util.NIL_UUID : Owner;
        SatelliteNetwork.Satellite Made = SatelliteNetwork.get(Server).deploy(Named, Holder, Server.overworld().getGameTime());
        ServerPlayer Launcher = Owner == null ? null : Server.getPlayerList().getPlayer(Owner);
        if (Launcher != null) {
            Notices.send(Launcher, Notice.info(Component.translatable("notice.scp_unleashed.rocketry.satellite_deployed", Made.Name()))
                    .withDetail(Component.translatable("notice.scp_unleashed.rocketry.satellite_orbit"))
                    .withIcon(Icon));
            Launcher.playNotifySound(RocketryRegistry.Deployed.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    @Override
    public void handleEntityEvent(byte Id) {
        if (RocketryBursts.handles(Id)) {
            RocketryBursts.play(this, Id);
            return;
        }
        super.handleEntityEvent(Id);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
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
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void lerpTo(double X, double Y, double Z, float YRot, float XRot, int Steps) {
        setRot(YRot, XRot);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < RenderReach * RenderReach;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        FlightTicks = Tag.getInt("FlightTicks");
        entityData.set(DataTicks, FlightTicks);
        entityData.set(DataStartY, Tag.contains("StartY") ? Tag.getFloat("StartY") : (float) getY());
        Owner = Tag.hasUUID("Owner") ? Tag.getUUID("Owner") : null;
        Named = Tag.contains("Named") ? Tag.getString("Named") : null;
        Payload = Tag.contains("Payload") ? Tag.getString("Payload") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putInt("FlightTicks", FlightTicks);
        Tag.putFloat("StartY", (float) getStartY());
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
        }
        if (Named != null) {
            Tag.putString("Named", Named);
        }
        if (Payload != null) {
            Tag.putString("Payload", Payload);
        }
    }
}
