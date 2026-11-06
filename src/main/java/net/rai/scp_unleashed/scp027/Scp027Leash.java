package net.rai.scp_unleashed.scp027;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Scp027Leash {
    public static final int CheckEvery = 10;
    public static final int MissingGrace = 100;
    public static final int ScatterTicks = 50;
    public static final double LeashRange = 18.0;
    public static final double HeelRange = 12.0;
    public static final double LostRange = 40.0;

    @Nullable
    private UUID Owner;
    @Nullable
    private Vec3 Home;
    private int Missing;
    private int ScatterLeft = -1;
    @Nullable
    private Scp027Entity Cached;

    @Nullable
    public UUID owner() {
        return Owner;
    }

    public void bind(Scp027Entity Master) {
        Owner = Master.getUUID();
        Home = Master.position();
        Cached = Master;
        Missing = 0;
        ScatterLeft = -1;
    }

    public boolean ownedBy(Scp027Entity Master) {
        return Owner != null && Owner.equals(Master.getUUID());
    }

    public boolean scattering() {
        return ScatterLeft >= 0;
    }

    @Nullable
    public Vec3 home() {
        return Home;
    }

    @Nullable
    public Scp027Entity master() {
        return Cached != null && Cached.isAlive() && !Cached.isRemoved() ? Cached : null;
    }

    public void scatter(Mob Body) {
        if (scattering()) {
            return;
        }
        ScatterLeft = ScatterTicks + Body.getRandom().nextInt(ScatterTicks / 2);
        Body.setTarget(null);
        Body.getNavigation().stop();
    }

    public boolean tick(Mob Body) {
        if (!(Body.level() instanceof ServerLevel World)) {
            return false;
        }
        if (scattering()) {
            Body.setTarget(null);
            if (--ScatterLeft <= 0) {
                vanish(World, Body);
                return true;
            }
            return false;
        }
        if (Owner == null) {
            scatter(Body);
            return false;
        }
        if (Body.tickCount % CheckEvery == 0 || Cached == null) {
            Entity Found = World.getEntity(Owner);
            Cached = Found instanceof Scp027Entity Master ? Master : null;
        }
        Scp027Entity Master = master();
        if (Master == null) {
            Missing++;
            if (Missing >= MissingGrace || Cached != null) {
                scatter(Body);
            }
            return false;
        }
        Missing = 0;
        Home = Master.position();
        if (Master.isDeadOrDying()) {
            scatter(Body);
            return false;
        }
        double Distance = Body.distanceToSqr(Master);
        if (Distance > LostRange * LostRange) {
            vanish(World, Body);
            return true;
        }
        LivingEntity Target = Body.getTarget();
        if (Target != null && (!Target.isAlive() || Target.distanceToSqr(Master) > LeashRange * LeashRange)) {
            Body.setTarget(null);
        }
        return false;
    }

    public boolean straying(Mob Body) {
        Scp027Entity Master = master();
        return Master != null && Body.distanceToSqr(Master) > HeelRange * HeelRange;
    }

    public static void vanish(ServerLevel World, Mob Body) {
        World.sendParticles(ParticleTypes.POOF, Body.getX(), Body.getY() + Body.getBbHeight() * 0.5, Body.getZ(), 4, 0.15, 0.1, 0.15, 0.02);
        Body.discard();
    }

    public void save(CompoundTag Saved) {
        CompoundTag Tag = new CompoundTag();
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
        }
        if (Home != null) {
            Tag.putDouble("HomeX", Home.x);
            Tag.putDouble("HomeY", Home.y);
            Tag.putDouble("HomeZ", Home.z);
        }
        Tag.putInt("ScatterLeft", ScatterLeft);
        Saved.put("Scp027Leash", Tag);
    }

    public void load(CompoundTag Saved) {
        CompoundTag Tag = Saved.getCompound("Scp027Leash");
        Owner = Tag.hasUUID("Owner") ? Tag.getUUID("Owner") : null;
        Home = Tag.contains("HomeX") ? new Vec3(Tag.getDouble("HomeX"), Tag.getDouble("HomeY"), Tag.getDouble("HomeZ")) : null;
        ScatterLeft = Tag.contains("ScatterLeft") ? Tag.getInt("ScatterLeft") : -1;
        Missing = 0;
        Cached = null;
    }
}
