package net.rai.scp_unleashed.scp538;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.stasis.StasisPods;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public final class Scp538Swarm {
    public static final int PodSwarm = 6;
    public static final int BurstMax = 20;
    public static final double AreaRadius = 24.0;
    public static final int AreaCap = 32;
    public static final int LevelCap = 96;
    public static final double Spread = 0.7;
    public static final double ScatterRange = 32.0;
    public static final DustParticleOptions Shade = new DustParticleOptions(new Vector3f(0.07f, 0.05f, 0.1f), 1.2f);

    private Scp538Swarm() {
    }

    public static int countNear(ServerLevel World, Vec3 Where, double Radius) {
        return World.getEntitiesOfClass(Scp538Spider.class, new AABB(Where, Where).inflate(Radius),
                Each -> Each.isAlive() && Each.distanceToSqr(Where) <= Radius * Radius).size();
    }

    public static int countAll(ServerLevel World) {
        return World.getEntities(Scp538Registry.Spider.get(), Entity::isAlive).size();
    }

    public static int allowed(ServerLevel World, Vec3 Where, int Wanted) {
        return allowedFor(Wanted, countNear(World, Where, AreaRadius), countAll(World));
    }

    public static int allowedFor(int Wanted, int Near, int Total) {
        return Mth.clamp(Math.min(Wanted, Math.min(AreaCap - Near, LevelCap - Total)), 0, Math.max(0, Wanted));
    }

    public static int spawn(ServerLevel World, Vec3 Where, int Wanted, float Yaw) {
        int Count = allowed(World, Where, Wanted);
        int Made = 0;
        for (int Index = 0; Index < Count; Index++) {
            Scp538Spider Spider = Scp538Registry.Spider.get().create(World);
            if (Spider == null) {
                break;
            }
            double Angle = Index * 2.39996 + World.random.nextDouble() * 0.6;
            double Reach = Spread * Math.sqrt((Index + 0.5) / Math.max(1, Count));
            Vec3 Spot = new Vec3(Where.x + Math.cos(Angle) * Reach, Where.y, Where.z + Math.sin(Angle) * Reach);
            float Facing = Mth.wrapDegrees(Yaw + (float) Math.toDegrees(Angle));
            Spider.moveTo(Spot.x, Spot.y, Spot.z, Facing, 0.0f);
            if (!World.noCollision(Spider)) {
                Vec3 Room = StasisPods.findRoom(World, Spider, Where, Facing);
                Spider.moveTo(Room.x, Room.y, Room.z, Facing, 0.0f);
            }
            Spider.finalizeSpawn(World, World.getCurrentDifficultyAt(Spider.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            Spider.setYHeadRot(Facing);
            Spider.setYBodyRot(Facing);
            if (World.addFreshEntity(Spider)) {
                Made++;
            }
        }
        return Made;
    }

    public static int releaseFromPod(ServerLevel World, Vec3 Where, float Yaw, @Nullable Player User) {
        int Made = spawn(World, Where, PodSwarm, Yaw);
        if (Made > 0) {
            World.sendParticles(Shade, Where.x, Where.y + 0.15, Where.z, 18, 0.35, 0.08, 0.35, 0.0);
            playLayers(World, Where, Scp538Registry.SkitterLayers, 0.8f, 1.0f);
        }
        return Made;
    }

    public static int burst(ServerLevel World, LivingEntity Dead) {
        Vec3 Where = Dead.position();
        double Middle = Dead.getBbHeight() * 0.5;
        World.sendParticles(Shade, Where.x, Where.y + Middle, Where.z, 46, Dead.getBbWidth() * 0.6, Dead.getBbHeight() * 0.4, Dead.getBbWidth() * 0.6, 0.02);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Where.x, Where.y + Middle, Where.z, 16, Dead.getBbWidth() * 0.5, Dead.getBbHeight() * 0.35, Dead.getBbWidth() * 0.5, 0.03);
        World.sendParticles(ParticleTypes.SQUID_INK, Where.x, Where.y + Middle, Where.z, 22, Dead.getBbWidth() * 0.45, Dead.getBbHeight() * 0.3, Dead.getBbWidth() * 0.45, 0.06);
        playLayers(World, Where.add(0.0, Middle, 0.0), Scp538Registry.BurstLayers, 1.0f, 1.0f);
        return spawn(World, Where, BurstMax, Dead.getYRot());
    }

    public static boolean isPerson(Entity Target) {
        return Target instanceof Player || ControlBodies.isPersonLike(Target);
    }

    public static boolean canBite(@Nullable LivingEntity Target) {
        if (Target == null || !Target.isAlive() || Target.isRemoved() || !isPerson(Target)) {
            return false;
        }
        if (Target instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return false;
        }
        return !Scp538Infection.isInfected(Target) && !Scp538Infection.isExempt(Target);
    }

    @Nullable
    public static LivingEntity nearestPrey(ServerLevel World, Scp538Spider Spider, double Range) {
        LivingEntity Best = null;
        double BestDistance = Range * Range;
        for (ServerPlayer User : World.players()) {
            double Distance = User.distanceToSqr(Spider);
            if (Distance < BestDistance && canBite(User) && Spider.getSensing().hasLineOfSight(User)) {
                Best = User;
                BestDistance = Distance;
            }
        }
        List<LivingEntity> People = World.getEntitiesOfClass(LivingEntity.class, Spider.getBoundingBox().inflate(Range),
                Each -> !(Each instanceof Player) && ControlBodies.isPersonLike(Each) && canBite(Each));
        for (LivingEntity Person : People) {
            double Distance = Person.distanceToSqr(Spider);
            if (Distance < BestDistance && Spider.getSensing().hasLineOfSight(Person)) {
                Best = Person;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    @Nullable
    public static Vec3 threat(Scp538Spider Spider) {
        double Range = Scp538Spider.FleeRange;
        LivingEntity Nearest = null;
        double Best = Range * Range;
        for (LivingEntity Each : Spider.level().getEntitiesOfClass(LivingEntity.class, Spider.getBoundingBox().inflate(Range),
                Each -> Each.isAlive() && Scp538Infection.isInfected(Each))) {
            double Distance = Each.distanceToSqr(Spider);
            if (Distance < Best) {
                Best = Distance;
                Nearest = Each;
            }
        }
        return Nearest == null ? null : Nearest.position();
    }

    public static int scatter(ServerLevel World, LivingEntity Victim) {
        List<Scp538Spider> Near = World.getEntitiesOfClass(Scp538Spider.class, Victim.getBoundingBox().inflate(ScatterRange), Entity::isAlive);
        for (Scp538Spider Spider : Near) {
            Spider.scare(Victim.position(), Scp538Spider.ScareTicks);
            Spider.setTarget(null);
        }
        return Near.size();
    }

    public static void play(ServerLevel World, Vec3 At, SoundEvent Event, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Event, SoundSource.HOSTILE, Volume, Pitch);
    }

    public static void playLayers(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            play(World, At, Layer.get(), Volume, Pitch);
        }
    }
}
