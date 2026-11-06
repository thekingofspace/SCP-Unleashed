package net.rai.scp_unleashed.scp428;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp428Arrival {
    public static final int NearbyRadius = 10;
    public static final int NearbyTries = 8;
    public static final double RoomWidth = 1.2;
    public static final double RoomHeight = 2.0;

    private Scp428Arrival() {
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Surface = Found.Pos();
        RandomSource Dice = World.getRandom();
        Vec3 Toward = Found.Investigator().position();
        if (roomy(World, Surface) && place(World, Vec3.atBottomCenterOf(Surface), facing(Vec3.atBottomCenterOf(Surface), Toward)) != null) {
            return true;
        }
        for (int Attempt = 0; Attempt < NearbyTries; Attempt++) {
            Optional<BlockPos> Near = LeadPlanner.standableNear(World, Surface, 2, NearbyRadius, 6, Dice);
            if (Near.isPresent() && roomy(World, Near.get())) {
                Vec3 Spot = Vec3.atBottomCenterOf(Near.get());
                if (place(World, Spot, facing(Spot, Toward)) != null) {
                    return true;
                }
            }
        }
        Vec3 Last = Vec3.atBottomCenterOf(Surface);
        return place(World, Last, facing(Last, Toward)) != null;
    }

    public static float facing(Vec3 From, Vec3 Toward) {
        return (float) Math.toDegrees(Math.atan2(Toward.z - From.z, Toward.x - From.x)) - 90.0f;
    }

    public static boolean roomy(ServerLevel World, BlockPos Pos) {
        Vec3 Base = Vec3.atBottomCenterOf(Pos);
        AABB Box = new AABB(Base.x - RoomWidth, Base.y, Base.z - RoomWidth, Base.x + RoomWidth, Base.y + RoomHeight, Base.z + RoomWidth);
        return World.noCollision(Box) && !World.containsAnyLiquid(Box) && World.getBlockState(Pos.below()).isFaceSturdy(World, Pos.below(), Direction.UP);
    }

    @Nullable
    public static Scp428Core place(ServerLevel World, Vec3 Spot, float Yaw) {
        Scp428Core Core = Scp428Registry.Core.get().create(World);
        if (Core == null) {
            return null;
        }
        Core.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Core.setYBodyRot(Yaw);
        Core.setYHeadRot(Yaw);
        if (!World.noCollision(Core)) {
            return null;
        }
        Core.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        Core.setPersistenceRequired();
        return World.addFreshEntity(Core) ? Core : null;
    }
}
