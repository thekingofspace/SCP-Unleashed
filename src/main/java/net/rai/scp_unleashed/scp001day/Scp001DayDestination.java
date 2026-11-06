package net.rai.scp_unleashed.scp001day;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;
import org.slf4j.Logger;

public final class Scp001DayDestination {
    public static final ResourceLocation Id = Scp001DayRegistry.location("scp001_day");
    public static final ResourceLocation StandinId = Scp001DayRegistry.location("scp001_day_standin");
    public static final int Tint = 0xFFE3A0;
    public static final int ShardVariant = 2;
    public static final int WarningLines = 3;
    public static final int BuildRadius = 1;
    public static final int GroundReach = 4;
    private static final Logger Log = LogUtils.getLogger();

    private Scp001DayDestination() {
    }

    public static GatewayDestination make() {
        return make(Id, Scp001DayWorld.Key);
    }

    public static GatewayDestination make(ResourceLocation Name, ResourceKey<Level> Dimension) {
        return new GatewayDestination(Name, Dimension, Tint, ShardVariant, WarningLines, Scp001DayDestination::entry, GatewayDestination.OpenTravel, new Hooks());
    }

    public static GatewayDestination standIn(ResourceKey<Level> Dimension) {
        GatewayDestination Found = GatewayDestinations.get(StandinId);
        if (Found == null) {
            Found = GatewayDestinations.register(make(StandinId, Dimension));
        }
        Scp001DayWorld.standIn(Found.Dimension());
        return Found;
    }

    public static Vec3 arrivalSpot(int Index) {
        return new Vec3(Scp001DayWorld.baseX() + Scp001DayPlan.arrivalX(Index) + 0.5, Scp001DayPlan.arrivalY(), Scp001DayWorld.baseZ() + Scp001DayPlan.arrivalZ(Index) + 0.5);
    }

    public static int arrivalIndex(ServerLevel Target) {
        return Target.players().size();
    }

    public static boolean build(ServerLevel Target, Vec3 Spot) {
        ChunkPos Centre = new ChunkPos(BlockPos.containing(Spot));
        for (int Dx = -BuildRadius; Dx <= BuildRadius; Dx++) {
            for (int Dz = -BuildRadius; Dz <= BuildRadius; Dz++) {
                Target.getChunk(Centre.x + Dx, Centre.z + Dz);
            }
        }
        return grounded(Target, Spot);
    }

    public static boolean grounded(ServerLevel Target, Vec3 Spot) {
        BlockPos Feet = BlockPos.containing(Spot);
        for (int Depth = 1; Depth <= GroundReach; Depth++) {
            BlockPos Below = Feet.below(Depth);
            BlockState Floor = Target.getBlockState(Below);
            if (Floor.isFaceSturdy(Target, Below, Direction.UP) || !Floor.isAir() && !Floor.liquid()) {
                return true;
            }
        }
        return false;
    }

    private static GatewayDestination.Arrival entry(ServerLevel Target, ServerPlayer User) {
        Vec3 Spot = arrivalSpot(arrivalIndex(Target));
        Target.getChunkAt(BlockPos.containing(Spot));
        return new GatewayDestination.Arrival(Spot, 135.0f);
    }

    private static final class Hooks implements GatewayDestination.Hooks {
        @Override
        public boolean canEnter(ServerPlayer User, ServerLevel Target) {
            Vec3 Spot = arrivalSpot(arrivalIndex(Target));
            if (build(Target, Spot)) {
                return true;
            }
            Log.warn("SCP-001 city: refused {} because the arrival at {} in {} has no ground", User.getGameProfile().getName(), BlockPos.containing(Spot), Target.dimension().location());
            return false;
        }

        @Override
        public void arrived(ServerPlayer User, ServerLevel Target) {
            Scp001DaySunlight.forget(User);
        }

        @Override
        public void left(ServerPlayer User, ServerLevel From) {
            Scp001DaySunlight.forget(User);
        }

        @Override
        public void died(ServerPlayer User, ServerLevel Where) {
            Scp001DaySunlight.forget(User);
        }
    }
}
