package net.rai.scp_unleashed.scp610;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.faction.FactionState;
import org.jetbrains.annotations.Nullable;

public class Scp610HiveLord extends Scp610Creature {
    public static final float HitCap = 12.0f;
    public static final int BlinkTicks = 10;
    public static final int BlinkMin = 80;
    public static final int BlinkMax = 200;
    public static final double Watch = 16.0;
    public static final double Close = 6.0;
    public static final int RegenTicks = 10;
    public static final int AgitatedTicks = 200;
    private static final EntityDataAccessor<Integer> WatchedData = SynchedEntityData.defineId(Scp610HiveLord.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> AgitatedData = SynchedEntityData.defineId(Scp610HiveLord.class, EntityDataSerializers.BOOLEAN);
    private int NextBlink = 100;
    private int Calm;

    public Scp610HiveLord(EntityType<? extends Scp610HiveLord> Type, Level World) {
        super(Type, World, Scp610Form.HiveLord);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(WatchedData, -1);
        Builder.define(AgitatedData, false);
    }

    @Override
    protected void registerGoals() {
    }

    @Nullable
    @Override
    public String spawnClip() {
        return null;
    }

    @Nullable
    public Entity watched() {
        int Id = entityData.get(WatchedData);
        return Id < 0 ? null : level().getEntity(Id);
    }

    public boolean agitated() {
        return entityData.get(AgitatedData);
    }

    public float dilation(float Partial) {
        Entity Seen = watched();
        float Base = agitated() ? 1.35f : 1.0f;
        if (Seen != null) {
            double Gap = Seen.position().distanceTo(position().add(0.0, getBbHeight() * 0.5, 0.0));
            if (Gap < Close + getBbWidth() * 0.5) {
                Base = Math.max(Base, 1.4f);
            }
        }
        if (Seen == null && !agitated()) {
            Base = 0.7f;
        }
        return Base;
    }

    public Vec3 eye() {
        return position().add(0.0, getBbHeight() * 0.5, 0.0);
    }

    @Override
    protected void serverTick() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        if (tickCount % 5 == 0) {
            ServerPlayer Seen = nearest(World);
            entityData.set(WatchedData, Seen == null ? -1 : Seen.getId());
            if (Seen == null && tickCount % RegenTicks == 0 && getHealth() < getMaxHealth()) {
                heal(1.0f);
            }
        }
        if (Calm > 0 && --Calm == 0) {
            entityData.set(AgitatedData, false);
        }
        if (--NextBlink <= 0 && canAct()) {
            NextBlink = BlinkMin + random.nextInt(BlinkMax - BlinkMin);
            setAction(ActionBlink, BlinkTicks);
        }
    }

    @Nullable
    private ServerPlayer nearest(ServerLevel World) {
        FactionState State = Scp610Hives.state(World.getServer());
        AABB Room = Scp610Lord.bounds(State);
        AABB Area = Room != null ? Room.inflate(2.0) : getBoundingBox().inflate(Watch);
        ServerPlayer Best = null;
        double Gap = Double.MAX_VALUE;
        for (ServerPlayer User : World.players()) {
            if (User.isSpectator() || !Area.contains(User.position())) {
                continue;
            }
            double Found = User.distanceToSqr(eye());
            if (Found < Gap) {
                Gap = Found;
                Best = User;
            }
        }
        return Best;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Math.min(HitCap, Amount));
        if (Hurt && level() instanceof ServerLevel World) {
            entityData.set(AgitatedData, true);
            Calm = AgitatedTicks;
            if (Source.getEntity() instanceof Player) {
                Scp610Pact.breakPact(World.getServer(), Scp610Hives.state(World.getServer()), Scp610Pact.Breach.Lord);
            }
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        boolean Fresh = !isRemoved() && !dead;
        super.die(Source);
        if (Fresh && level() instanceof ServerLevel World) {
            Scp610Lord.slainBy(World, this, Source.getEntity());
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= 40 && !isRemoved()) {
            if (level() instanceof ServerLevel World) {
                World.sendParticles(Gore, getX(), getY() + getBbHeight() * 0.5, getZ(), 80, getBbWidth() * 0.4, getBbHeight() * 0.4, getBbWidth() * 0.4, 0.0);
            }
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
