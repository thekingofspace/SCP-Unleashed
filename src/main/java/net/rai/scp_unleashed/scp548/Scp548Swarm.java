package net.rai.scp_unleashed.scp548;

import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.cooler.CoolerIndex;
import net.rai.scp_unleashed.stasis.StasisPods;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp548Swarm {
    public static final int PodCount = 2;
    public static final int PodChill = 600;
    public static final double AreaRadius = 16.0;
    public static final int AreaCap = 12;
    public static final int LevelCap = 40;
    public static final double Spread = 0.9;
    public static final BlockParticleOption Shards = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());

    private Scp548Swarm() {
    }

    public static int countNear(ServerLevel World, Vec3 Where, double Radius) {
        return World.getEntitiesOfClass(Scp548Spider.class, new AABB(Where, Where).inflate(Radius),
                Each -> Each.isAlive() && Each.distanceToSqr(Where) <= Radius * Radius).size();
    }

    public static int countAll(ServerLevel World) {
        return World.getEntities(Scp548Registry.Spider.get(), Entity::isAlive).size();
    }

    public static int allowed(ServerLevel World, Vec3 Where, int Wanted) {
        return allowedFor(Wanted, countNear(World, Where, AreaRadius), countAll(World));
    }

    public static int allowedFor(int Wanted, int Near, int Total) {
        return Mth.clamp(Math.min(Wanted, Math.min(AreaCap - Near, LevelCap - Total)), 0, Math.max(0, Wanted));
    }

    public static boolean crowded(ServerLevel World, Vec3 Where) {
        return allowed(World, Where, 1) <= 0;
    }

    public static int spawn(ServerLevel World, Vec3 Where, int Wanted, float Yaw, int Chill) {
        int Count = allowed(World, Where, Wanted);
        int Made = 0;
        for (int Index = 0; Index < Count; Index++) {
            Scp548Spider Spider = Scp548Registry.Spider.get().create(World);
            if (Spider == null) {
                break;
            }
            double Angle = Index * 2.39996 + World.random.nextDouble() * 0.6;
            double Reach = Spread * Math.sqrt((Index + 0.5) / Math.max(1, Count));
            Vec3 Spot = new Vec3(Where.x + Math.cos(Angle) * Reach, Where.y, Where.z + Math.sin(Angle) * Reach);
            float Facing = Mth.wrapDegrees(Yaw + (float) Math.toDegrees(Angle) * 0.25f);
            Spider.moveTo(Spot.x, Spot.y, Spot.z, Facing, 0.0f);
            if (!World.noCollision(Spider)) {
                Vec3 Room = StasisPods.findRoom(World, Spider, Where, Facing);
                Spider.moveTo(Room.x, Room.y, Room.z, Facing, 0.0f);
            }
            Spider.finalizeSpawn(World, World.getCurrentDifficultyAt(Spider.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            Spider.setYHeadRot(Facing);
            Spider.setYBodyRot(Facing);
            Spider.chillFor(Chill);
            if (World.addFreshEntity(Spider)) {
                Made++;
            }
        }
        return Made;
    }

    public static int releaseFromPod(ServerLevel World, Vec3 Where, float Yaw, @Nullable Player User) {
        int Made = spawn(World, Where, PodCount, Yaw, PodChill);
        if (Made > 0) {
            World.sendParticles(ParticleTypes.SNOWFLAKE, Where.x, Where.y + 0.3, Where.z, 30, 0.45, 0.2, 0.45, 0.03);
            World.sendParticles(Shards, Where.x, Where.y + 0.2, Where.z, 18, 0.35, 0.1, 0.35, 0.05);
            playLayers(World, Where, Scp548Registry.FrostLayers, 0.9f, 0.9f);
            playLayers(World, Where, Scp548Registry.ChitterLayers, 0.7f, 1.0f);
        }
        return Made;
    }

    public static boolean isPerson(Entity Target) {
        return Target instanceof Player || ControlBodies.isPersonLike(Target);
    }

    public static boolean isPrey(@Nullable LivingEntity Target) {
        if (Target == null || !Target.isAlive() || Target.isRemoved() || !isPerson(Target) || !Targeting.isTargetable(Target)) {
            return false;
        }
        return !(Target instanceof Player User) || !User.isCreative() && !User.isSpectator();
    }

    @Nullable
    public static LivingEntity nearestPrey(ServerLevel World, Scp548Spider Spider, double Range) {
        LivingEntity Best = null;
        double BestDistance = Range * Range;
        for (ServerPlayer User : World.players()) {
            double Distance = User.distanceToSqr(Spider);
            if (Distance < BestDistance && isPrey(User) && Spider.getSensing().hasLineOfSight(User)) {
                Best = User;
                BestDistance = Distance;
            }
        }
        List<LivingEntity> People = World.getEntitiesOfClass(LivingEntity.class, Spider.getBoundingBox().inflate(Range),
                Each -> !(Each instanceof Player) && isPrey(Each));
        for (LivingEntity Person : People) {
            double Distance = Person.distanceToSqr(Spider);
            if (Distance < BestDistance && Spider.getSensing().hasLineOfSight(Person)) {
                Best = Person;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public static boolean cooled(ServerLevel World, Vec3 Where) {
        return CoolerIndex.cooling(World, Where);
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
