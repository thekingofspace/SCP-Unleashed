package net.rai.scp_unleashed.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.faction.FactionGround;
import org.jetbrains.annotations.Nullable;

public final class RaidGround {
    private RaidGround() {
    }

    public static boolean standable(ServerLevel World, BlockPos Feet) {
        BlockPos Floor = Feet.below();
        BlockState Under = World.getBlockState(Floor);
        if (!Under.isFaceSturdy(World, Floor, Direction.UP) || !World.getFluidState(Feet).isEmpty() || !World.getFluidState(Floor).isEmpty()) {
            return false;
        }
        return World.getBlockState(Feet).getCollisionShape(World, Feet).isEmpty() && World.getBlockState(Feet.above()).getCollisionShape(World, Feet.above()).isEmpty();
    }

    @Nullable
    public static BlockPos standNear(ServerLevel World, int X, int Z, int AroundY, int Span) {
        if (!World.isLoaded(new BlockPos(X, AroundY, Z))) {
            return null;
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Step = 0; Step <= Span; Step++) {
            for (int Sign = -1; Sign <= 1; Sign += 2) {
                if (Step == 0 && Sign > 0) {
                    continue;
                }
                int Y = AroundY + Step * Sign;
                if (Y <= World.getMinBuildHeight() || Y >= World.getMaxBuildHeight() - 2) {
                    continue;
                }
                Cursor.set(X, Y, Z);
                if (standable(World, Cursor)) {
                    return Cursor.immutable();
                }
            }
        }
        return null;
    }

    @Nullable
    public static BlockPos standAround(ServerLevel World, BlockPos Centre, double MinRadius, double MaxRadius, int AroundY, int Span, RandomSource Random, int Tries,
                                       boolean Wild) {
        for (int Attempt = 0; Attempt < Tries; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = MinRadius + Random.nextDouble() * Math.max(0.0, MaxRadius - MinRadius);
            int X = Centre.getX() + Mth.floor(Math.cos(Angle) * Radius);
            int Z = Centre.getZ() + Mth.floor(Math.sin(Angle) * Radius);
            BlockPos Found = standNear(World, X, Z, AroundY, Span);
            if (Found != null && (!Wild || !FactionGround.madeByHand(World.getBlockState(Found.below())))) {
                return Found;
            }
        }
        return null;
    }

    @Nullable
    public static BlockPos surfaceNear(ServerLevel World, int X, int Z, int AroundY, int Drift, boolean Wild) {
        BlockPos Probe = new BlockPos(X, AroundY, Z);
        if (!World.isLoaded(Probe)) {
            return null;
        }
        int Top = FactionGround.ground(World, X, Z) + 1;
        if (Math.abs(Top - AroundY) > Drift) {
            return standNear(World, X, Z, AroundY, Drift);
        }
        BlockPos Feet = new BlockPos(X, Top, Z);
        if (!standable(World, Feet) || Wild && FactionGround.madeByHand(World.getBlockState(Feet.below()))) {
            return null;
        }
        return Feet;
    }
}
