package net.rai.scp_unleashed.rocketry.rocket;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class RocketTravel {
    public static final double EarthDrop = 180.0;
    public static final double MinimumDrop = 32.0;
    public static final int AboveLimit = 48;
    public static final int TicketRadius = 2;
    public static final int TicketTicks = 900;
    public static final TicketType<ChunkPos> ArrivalTicket = TicketType.create("scp_unleashed_rocket_arrival", Comparator.comparingLong(ChunkPos::toLong), TicketTicks);
    private static final Logger Log = LogUtils.getLogger();

    private RocketTravel() {
    }

    public static void recordLaunch(Entity Rider, GlobalPos Site) {
        if (Rider instanceof Player && Site.dimension() == Level.OVERWORLD) {
            Rider.setData(RocketryRegistry.LastLaunch.get(), new LaunchRecord(Optional.of(Site)));
        }
    }

    public static Optional<GlobalPos> recordedLaunch(@Nullable Entity Rider) {
        if (!(Rider instanceof Player)) {
            return Optional.empty();
        }
        return Rider.getData(RocketryRegistry.LastLaunch.get()).Site().filter(Site -> Site.dimension() == Level.OVERWORLD);
    }

    public static GlobalPos returnTarget(MinecraftServer Server, @Nullable GlobalPos Carried, @Nullable Entity Rider) {
        if (Carried != null && Carried.dimension() == Level.OVERWORLD) {
            return Carried;
        }
        Optional<GlobalPos> Recorded = recordedLaunch(Rider);
        if (Recorded.isPresent()) {
            return Recorded.get();
        }
        if (Rider instanceof ServerPlayer Traveller && Traveller.getRespawnPosition() != null && Traveller.getRespawnDimension() == Level.OVERWORLD) {
            return GlobalPos.of(Level.OVERWORLD, Traveller.getRespawnPosition());
        }
        return GlobalPos.of(Level.OVERWORLD, Server.overworld().getSharedSpawnPos());
    }

    public record Arrival(ServerLevel Target, BlockPos Site, DropPodEntity.Kit Kit, @Nullable GlobalPos Carried, int Fuel, double Above) {
    }

    public static boolean loaded(ServerLevel Target, BlockPos Site) {
        return Target.getChunkSource().getChunkNow(Site.getX() >> 4, Site.getZ() >> 4) != null;
    }

    public static boolean hasGround(ServerLevel Target, BlockPos Site) {
        return loaded(Target, Site) && Target.getHeight(Heightmap.Types.MOTION_BLOCKING, Site.getX(), Site.getZ()) > Target.getMinBuildHeight();
    }

    public static BlockPos groundedSite(ServerLevel Target, BlockPos Wanted) {
        for (BlockPos Candidate : List.of(Wanted, Target.getSharedSpawnPos(), BlockPos.ZERO)) {
            if (hasGround(Target, Candidate)) {
                return Candidate;
            }
        }
        return Wanted;
    }

    public static Vec3 dropPoint(ServerLevel Target, BlockPos Site, double Above) {
        int Surface = loaded(Target, Site) ? Target.getHeight(Heightmap.Types.MOTION_BLOCKING, Site.getX(), Site.getZ()) : Target.getSeaLevel();
        double Y = Math.min(Surface + Above, Target.getMaxBuildHeight() + AboveLimit);
        Y = Math.max(Y, Surface + MinimumDrop);
        return new Vec3(Site.getX() + 0.5, Y, Site.getZ() + 0.5);
    }

    public static Arrival plan(RocketEntity Rocket) {
        ServerLevel From = (ServerLevel) Rocket.level();
        MinecraftServer Server = From.getServer();
        Entity Lead = Rocket.getPassengers().isEmpty() ? null : Rocket.getPassengers().getFirst();
        RocketDestination Goal = RocketDestinations.byId(Rocket.getDestinationId());
        if (Goal == null) {
            Goal = RocketDestinations.portableRoute(Server, From.dimension());
        }
        ServerLevel Target = Goal == null ? null : Server.getLevel(Goal.Dimension());
        BlockPos Pad = BlockPos.containing(Rocket.getX(), Rocket.getStandY(), Rocket.getZ());
        if (Target == null || Target == From) {
            return new Arrival(From, Pad, DropPodEntity.Kit.Plain, null, Rocket.getStoredFuel(), EarthDrop);
        }
        if (Goal.isEarth()) {
            return new Arrival(Target, returnTarget(Server, Rocket.getReturnSite(), Lead).pos(), DropPodEntity.Kit.Plain, null, Rocket.getStoredFuel(), Goal.ArrivalHeight());
        }
        GlobalPos Launched = Rocket.getLaunchSite();
        GlobalPos Carried = Launched != null && Launched.dimension() == Level.OVERWORLD ? Launched : recordedLaunch(Lead).orElse(Rocket.getReturnSite());
        int Fuel = Math.max(Rocket.getStoredFuel(), RocketDestinations.earth().Fuel());
        return new Arrival(Target, Pad, DropPodEntity.Kit.Return, Carried, Fuel, Goal.ArrivalHeight());
    }

    public static void reserve(Arrival Plan) {
        for (BlockPos Spot : List.of(Plan.Site(), Plan.Target().getSharedSpawnPos())) {
            ChunkPos Chunk = new ChunkPos(Spot);
            Plan.Target().getChunkSource().addRegionTicket(ArrivalTicket, Chunk, TicketRadius, Chunk);
        }
    }

    public static boolean ready(Arrival Plan) {
        return loaded(Plan.Target(), Plan.Site());
    }

    public static void transfer(RocketEntity Rocket) {
        if (Rocket.isRemoved() || !(Rocket.level() instanceof ServerLevel From)) {
            return;
        }
        Arrival Plan = Rocket.getArrival() != null ? Rocket.getArrival() : plan(Rocket);
        List<Entity> Riders = new ArrayList<>(Rocket.getPassengers());
        ServerLevel Target = Plan.Target();
        if (!loaded(Target, Plan.Site())) {
            Log.warn("Rocket landing area in {} was still not generated after waiting; loading it now", Target.dimension().location());
            Target.getChunk(Plan.Site().getX() >> 4, Plan.Site().getZ() >> 4);
        }
        Vec3 Drop = dropPoint(Target, groundedSite(Target, Plan.Site()), Plan.Above());
        DropPodEntity Pod = DropPodEntity.falling(Target, Drop, Rocket.getYRot(), Plan.Kit(), Plan.Carried(), Plan.Fuel());
        if (!Target.addFreshEntity(Pod)) {
            Log.warn("Rocket drop pod could not be added to {}; returning the rocket to its pad", Target.dimension().location());
            abort(Rocket, From, Riders);
            return;
        }
        List<Entity> Moved = new ArrayList<>();
        for (Entity Rider : Riders) {
            Rider.stopRiding();
            Entity Arrived = move(Rider, Target, Drop);
            if (Arrived == null) {
                Log.warn("Rocket passenger {} could not reach {}; returning it to the pad", Rider.getName().getString(), Target.dimension().location());
                putDown(Rocket, From, Rider);
                continue;
            }
            Moved.add(Arrived);
        }
        for (Entity Arrived : Moved) {
            Arrived.startRiding(Pod, true);
        }
        Rocket.discard();
    }

    @Nullable
    private static Entity move(Entity Rider, ServerLevel Target, Vec3 At) {
        Rider.fallDistance = 0.0f;
        if (Rider instanceof ServerPlayer Traveller) {
            Traveller.teleportTo(Target, At.x, At.y, At.z, Traveller.getYRot(), Traveller.getXRot());
            if (Traveller.level() != Target) {
                return null;
            }
            Traveller.setDeltaMovement(Vec3.ZERO);
            Traveller.fallDistance = 0.0f;
            return Traveller;
        }
        if (Rider.level() == Target) {
            Rider.teleportTo(At.x, At.y, At.z);
            return Rider;
        }
        return Rider.changeDimension(new DimensionTransition(Target, At, Vec3.ZERO, Rider.getYRot(), Rider.getXRot(), DimensionTransition.DO_NOTHING));
    }

    private static void putDown(RocketEntity Rocket, ServerLevel From, Entity Rider) {
        Vec3 Spot = new Vec3(Rocket.getX() + 1.6, Rocket.getStandY() + 0.1, Rocket.getZ());
        if (Rider instanceof ServerPlayer Traveller) {
            Traveller.teleportTo(From, Spot.x, Spot.y, Spot.z, Traveller.getYRot(), Traveller.getXRot());
        } else if (Rider.level() == From) {
            Rider.teleportTo(Spot.x, Spot.y, Spot.z);
        }
        Rider.fallDistance = 0.0f;
        Rider.setDeltaMovement(Vec3.ZERO);
    }

    private static void abort(RocketEntity Rocket, ServerLevel From, List<Entity> Riders) {
        for (Entity Rider : Riders) {
            Rider.stopRiding();
            putDown(Rocket, From, Rider);
        }
        Vec3 Spot = new Vec3(Rocket.getX(), Rocket.getStandY() + 0.5, Rocket.getZ());
        Rocket.discard();
        ItemEntity Dropped = new ItemEntity(From, Spot.x, Spot.y, Spot.z, Rocket.toItem());
        Dropped.setDefaultPickUpDelay();
        From.addFreshEntity(Dropped);
    }
}
