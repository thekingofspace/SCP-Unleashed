package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionSites;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.labware.LabExposure;

public class Scp610Spewer extends Scp610Creature {
    public static final int GrowTicks = 60;
    public static final int SpewTicks = 32;
    public static final int ReleaseTick = 16;
    public static final int SpewMin = 140;
    public static final int SpewMax = 220;
    public static final double Sense = 12.0;
    public static final double CloudRadius = 6.0;
    public static final int CloudTicks = 60;
    public static final int CloudPulse = 10;
    public static final int RegenTicks = 40;
    public static final int RegenFlesh = 32;
    private int Hive = -1;
    private int SpewReady = 60;
    private int CloudLeft;

    public Scp610Spewer(EntityType<? extends Scp610Spewer> Type, Level World) {
        super(Type, World, Scp610Form.Spewer);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp610Goals.HoldStill(this));
    }

    @Override
    public String spawnClip() {
        return "grow";
    }

    @Override
    public int spawnTicks() {
        return GrowTicks;
    }

    public int hive() {
        return Hive;
    }

    public void setHive(int Id) {
        Hive = Id;
    }

    public Vec3 vent() {
        return position().add(0.0, getBbHeight() * 0.95, 0.0);
    }

    @Override
    public boolean isNoGravity() {
        return false;
    }

    @Override
    protected void serverTick() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        if (Hive < 0 && tickCount > 2) {
            FactionState State = Scp610Hives.state(World.getServer());
            if (State.phase() == FactionPhase.Defeated || State.phase() == FactionPhase.Dormant) {
                if (State.phase() == FactionPhase.Defeated) {
                    FactionControl.reset(World.getServer(), Scp610Faction.Instance);
                }
                FactionControl.awaken(World, Scp610Faction.Instance, blockPosition(), null);
                State = Scp610Hives.state(World.getServer());
            }
            Scp610Founding.adopt(World, State, this);
        }
        if (tickCount % RegenTicks == 0 && getHealth() < getMaxHealth()) {
            Scp610Hive Owner = Scp610Hives.hive(World.getServer(), Hive);
            if (Owner != null && Owner.flesh() >= RegenFlesh) {
                heal(1.0f);
            }
        }
        if (CloudLeft > 0) {
            CloudLeft--;
            if (CloudLeft % CloudPulse == 0) {
                Vec3 Centre = position().add(0.0, getBbHeight() * 0.6, 0.0);
                Scp610Infection.cloud(World, Centre, CloudRadius, 40);
                Scp610Infection.exposeAround(World, Centre, LabExposure.NothingIgnored, CloudRadius);
            }
        }
        SpewReady = Math.max(0, SpewReady - 1);
        if (SpewReady <= 0 && canAct()) {
            SpewReady = SpewMin + random.nextInt(SpewMax - SpewMin + 1);
            if (sensesPrey(World)) {
                spew();
            }
        }
    }

    public boolean sensesPrey(ServerLevel World) {
        AABB Area = getBoundingBox().inflate(Sense, 6.0, Sense);
        return !World.getEntitiesOfClass(LivingEntity.class, Area, Found -> isQuarry(this, Found) && Scp610Infection.exposable(Found)).isEmpty();
    }

    public void spew() {
        setAction(ActionSpew, SpewTicks);
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionSpew && Elapsed == ReleaseTick && level() instanceof ServerLevel World) {
            release(World);
            return;
        }
        super.actionTick(Kind, Elapsed);
    }

    public void release(ServerLevel World) {
        Vec3 Vent = vent();
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp610Sounds.SpewLayers) {
            World.playSound(null, Vent.x, Vent.y, Vent.z, Layer.get(), SoundSource.HOSTILE, 2.0f, 0.8f + random.nextFloat() * 0.1f);
        }
        World.sendParticles(Scp610Infection.Spore, Vent.x, Vent.y, Vent.z, 30, 0.6, 0.8, 0.6, 0.12);
        World.sendParticles(Scp610Registry.MawParticle.get(), Vent.x, Vent.y + 0.4, Vent.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(Scp610Registry.SporeParticle.get(), Vent.x, Vent.y + 0.2, Vent.z, 36, 0.7, 0.6, 0.7, 0.05);
        Vec3 Centre = position().add(0.0, getBbHeight() * 0.6, 0.0);
        Scp610Infection.exposeAround(World, Centre, LabExposure.NothingIgnored, CloudRadius);
        CloudLeft = CloudTicks;
    }

    @Override
    public void die(DamageSource Source) {
        boolean Fresh = !isRemoved() && !dead;
        super.die(Source);
        if (!Fresh || !(level() instanceof ServerLevel World)) {
            return;
        }
        FactionState State = Scp610Hives.state(World.getServer());
        if (Source.getEntity() instanceof Player) {
            Scp610Pact.breakPact(World.getServer(), State, Scp610Pact.Breach.Hive);
        }
        FactionSite Site = State.site(Hive);
        if (Site != null && Site.is(Scp610Hive.Kind)) {
            FactionSites.raze(World, Scp610Faction.Instance, Site, blockPosition());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Hive", Hive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Hive = Tag.contains("Hive") ? Tag.getInt("Hive") : -1;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    public BlockPos base() {
        return blockPosition().below();
    }
}
