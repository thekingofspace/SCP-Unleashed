package net.rai.scp_unleashed.scp001day;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayRift;

public final class Scp001DayRifts {
    public static final int CheckTicks = 400;
    public static final int Odds = 30;
    public static final int MinRange = 24;
    public static final int MaxRange = 72;
    public static final int Attempts = 24;
    public static final double NearCap = 128.0;

    private Scp001DayRifts() {
    }

    public static void tick(ServerLevel World) {
        if (World.getGameTime() % CheckTicks != 0) {
            return;
        }
        for (ServerPlayer User : World.players()) {
            if (User.isSpectator() || nearby(World, User) || World.random.nextInt(Odds) != 0) {
                continue;
            }
            spawnNear(World, User, World.random);
        }
    }

    public static boolean nearby(ServerLevel World, ServerPlayer User) {
        return !World.getEntitiesOfClass(Scp001DayRift.class, new AABB(User.blockPosition()).inflate(NearCap), Rift -> Rift.isAlive()).isEmpty();
    }

    @Nullable
    public static Scp001DayRift spawnNear(ServerLevel World, ServerPlayer User, RandomSource Random) {
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = MinRange + Random.nextDouble() * (MaxRange - MinRange);
            int X = (int) Math.floor(User.getX() + Math.cos(Angle) * Distance);
            int Z = (int) Math.floor(User.getZ() + Math.sin(Angle) * Distance);
            BlockPos Feet = standing(World, X, Z, Random);
            if (Feet != null) {
                Scp001DayRift Made = Scp001DayRift.create(World, Feet.getX() + 0.5, Feet.getY(), Feet.getZ() + 0.5);
                World.addFreshEntity(Made);
                return Made;
            }
        }
        return null;
    }

    @Nullable
    public static BlockPos standing(ServerLevel World, int X, int Z, RandomSource Random) {
        if (!World.hasChunk(X >> 4, Z >> 4)) {
            return null;
        }
        int Top = Math.min(World.getMaxBuildHeight() - 3, World.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, X, Z) + 1);
        int Found = 0;
        int Chosen = Integer.MIN_VALUE;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Y = World.getMinBuildHeight() + 1; Y <= Top; Y++) {
            Cursor.set(X, Y, Z);
            if (World.getBlockState(Cursor).isAir() && World.getBlockState(Cursor.above()).isAir() && World.getBlockState(Cursor.above(2)).isAir()
                    && World.getBlockState(Cursor.below()).isFaceSturdy(World, Cursor.below(), net.minecraft.core.Direction.UP)) {
                Found++;
                if (Random.nextInt(Found) == 0) {
                    Chosen = Y;
                }
            }
        }
        return Chosen == Integer.MIN_VALUE ? null : new BlockPos(X, Chosen, Z);
    }
}
