package net.rai.scp_unleashed.scp001day.entity;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp001day.Scp001DaySunlight;
import net.rai.scp_unleashed.scp001day.Scp001DayVoice;
import net.rai.scp_unleashed.scp001day.Scp001DayWorld;

public class Scp001DayRift extends Entity {
    public static final float Width = 1.2f;
    public static final float Height = 2.6f;
    public static final int Lifetime = 4800;
    public static final int OpenTicks = 60;
    public static final int CloseTicks = 80;
    public static final int AfterUse = 200;
    public static final int HumTicks = 90;
    private static final EntityDataAccessor<Integer> AgeData = SynchedEntityData.defineId(Scp001DayRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LifeData = SynchedEntityData.defineId(Scp001DayRift.class, EntityDataSerializers.INT);
    private int Used = -1;

    public Scp001DayRift(EntityType<? extends Scp001DayRift> Type, Level World) {
        super(Type, World);
        this.noPhysics = true;
    }

    public static Scp001DayRift create(ServerLevel World, double X, double Y, double Z) {
        Scp001DayRift Made = new Scp001DayRift(Scp001DayRegistry.RiftType.get(), World);
        Made.moveTo(X, Y, Z, World.random.nextFloat() * 360.0f, 0.0f);
        return Made;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(AgeData, 0);
        Builder.define(LifeData, Lifetime);
    }

    public int age() {
        return entityData.get(AgeData);
    }

    public int life() {
        return entityData.get(LifeData);
    }

    public float openness(float Partial) {
        float Age = age() + Partial;
        float Opening = Math.min(1.0f, Age / OpenTicks);
        float Closing = Math.min(1.0f, Math.max(0.0f, (life() - Age) / CloseTicks));
        return Math.max(0.0f, Math.min(Opening, Closing));
    }

    public boolean used() {
        return Used >= 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        int Age = age() + 1;
        entityData.set(AgeData, Age);
        if (Age >= life()) {
            discard();
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Age % HumTicks == 0) {
            World.playSound(null, getX(), getY() + 1.2, getZ(), Scp001DayRegistry.RiftHum.get(), SoundSource.AMBIENT, 1.2f, 0.8f + 0.1f * random.nextFloat());
        }
        if (Age < OpenTicks / 2) {
            return;
        }
        List<ServerPlayer> Touching = World.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().deflate(0.15), ServerPlayer::isAlive);
        for (ServerPlayer User : Touching) {
            pass(World, User);
        }
    }

    public void pass(ServerLevel World, ServerPlayer User) {
        if (!Scp001DayWorld.inside(User) || User.isSpectator()) {
            return;
        }
        World.playSound(null, getX(), getY() + 1.2, getZ(), Scp001DayRegistry.RiftPass.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        Scp001DaySunlight.forget(User);
        Scp001DayVoice.stop(User.getUUID());
        GatewayTravel.leave(User);
        if (Used < 0) {
            Used = age();
            entityData.set(LifeData, Math.min(life(), age() + AfterUse));
        }
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
    protected void readAdditionalSaveData(CompoundTag Tag) {
        entityData.set(AgeData, Tag.getInt("Age"));
        entityData.set(LifeData, Tag.contains("Life") ? Tag.getInt("Life") : Lifetime);
        Used = Tag.contains("Used") ? Tag.getInt("Used") : -1;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putInt("Age", age());
        Tag.putInt("Life", life());
        Tag.putInt("Used", Used);
    }
}
