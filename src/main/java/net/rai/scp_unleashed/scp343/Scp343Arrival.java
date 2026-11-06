package net.rai.scp_unleashed.scp343;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp343Arrival {
    public static final int NearMin = 6;
    public static final int NearMax = 16;
    public static final int Tries = 4;

    private Scp343Arrival() {
    }

    public static boolean arrive(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        Optional<Scp343Walls.Emergence> Spot = Scp343Walls.around(World, Found.Pos(), World.getRandom());
        if (Spot.isEmpty()) {
            Spot = Scp343Walls.near(World, Found.Pos(), 0, 4, World.getRandom(), false, null, 0.0);
        }
        return Spot.isPresent() && place(World, Spot.get(), Found.Investigator()).isPresent();
    }

    public static Optional<Scp343God> place(ServerLevel World, Scp343Walls.Emergence Spot, @Nullable ServerPlayer Greet) {
        if (!Scp343Walls.usable(World, Spot, Scp343Walls.Anywhere)) {
            return Optional.empty();
        }
        Scp343God God = Scp343Registry.God.get().create(World);
        if (God == null) {
            return Optional.empty();
        }
        God.moveTo(Spot.to().x, Spot.to().y, Spot.to().z, Spot.yaw(), 0.0f);
        God.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot.Stand()), MobSpawnType.EVENT, null);
        God.prepareEmergence(Spot);
        God.seek(Greet == null ? null : Greet.getUUID());
        if (!World.addFreshEntity(God)) {
            return Optional.empty();
        }
        God.announceEmergence(World);
        return Optional.of(God);
    }

    public static Optional<Scp343God> arriveNear(ServerLevel World, BlockPos Centre, @Nullable ServerPlayer Greet) {
        Optional<Scp343Walls.Emergence> Spot = Scp343Walls.open(World, Centre, NearMin, NearMax, World.getRandom(), null, 0.0, Tries);
        if (Spot.isEmpty()) {
            Spot = Scp343Walls.near(World, Centre, 2, NearMax, World.getRandom(), false, null, 0.0);
        }
        return Spot.flatMap(Found -> place(World, Found, Greet));
    }
}
