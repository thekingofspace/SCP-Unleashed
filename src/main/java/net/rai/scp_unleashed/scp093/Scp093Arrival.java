package net.rai.scp_unleashed.scp093;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class Scp093Arrival {
    public static final int ReuseReach = 32;
    public static final int SearchReach = 24;
    public static final int SearchStep = 2;
    public static final int MinimumY = 4;
    public static final double StandOff = 2.5;

    public record Gate(BlockPos Lower, Vec3 Spot, float Yaw) {
    }

    private Scp093Arrival() {
    }

    public static Gate gateFor(ServerLevel Wastes, int X, int Z) {
        Scp093World Data = Scp093World.get(Wastes.getServer());
        BlockPos Known = Data.nearestGate(X, Z, ReuseReach);
        while (Known != null) {
            Wastes.getChunk(Known.getX() >> 4, Known.getZ() >> 4);
            if (Wastes.getBlockState(Known).getBlock() instanceof Scp093ReturnMirrorBlock) {
                return standing(Wastes, Known, Wastes.getBlockState(Known).getValue(Scp093ReturnMirrorBlock.FACING));
            }
            Data.removeGate(Known);
            Known = Data.nearestGate(X, Z, ReuseReach);
        }
        BlockPos Site = findSite(Wastes, X, Z);
        build(Wastes, Site, Direction.SOUTH);
        Data.addGate(Site);
        return standing(Wastes, Site, Direction.SOUTH);
    }

    public static Gate standing(ServerLevel Wastes, BlockPos Lower, Direction Front) {
        Vec3 Spot = new Vec3(Lower.getX() + 0.5 + Front.getStepX() * StandOff, Lower.getY(), Lower.getZ() + 0.5 + Front.getStepZ() * StandOff);
        BlockPos Feet = BlockPos.containing(Spot);
        int Ground = Wastes.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Feet.getX(), Feet.getZ());
        if (Math.abs(Ground - Lower.getY()) <= 2) {
            Spot = new Vec3(Spot.x, Ground, Spot.z);
        }
        return new Gate(Lower, Spot, Front.getOpposite().toYRot());
    }

    private static boolean natural(BlockState Ground) {
        return Ground.is(BlockTags.DIRT) || Ground.is(BlockTags.SAND) || Ground.is(BlockTags.TERRACOTTA) || Ground.is(Blocks.PACKED_MUD) || Ground.is(Blocks.SANDSTONE);
    }

    private static int surface(ServerLevel Wastes, int X, int Z) {
        return Wastes.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z);
    }

    private static boolean suitable(ServerLevel Wastes, int X, int Z) {
        int Base = surface(Wastes, X, Z);
        if (Base < MinimumY) {
            return false;
        }
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 3; Dz++) {
                int Height = surface(Wastes, X + Dx, Z + Dz);
                if (Math.abs(Height - Base) > 1 || !natural(Wastes.getBlockState(new BlockPos(X + Dx, Height - 1, Z + Dz)))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static BlockPos findSite(ServerLevel Wastes, int X, int Z) {
        for (int Ring = 0; Ring <= SearchReach; Ring += SearchStep) {
            for (int Dx = -Ring; Dx <= Ring; Dx += SearchStep) {
                for (int Dz = -Ring; Dz <= Ring; Dz += SearchStep) {
                    if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Ring) {
                        continue;
                    }
                    int Cx = X + Dx;
                    int Cz = Z + Dz;
                    Wastes.getChunk(Cx >> 4, Cz >> 4);
                    if (suitable(Wastes, Cx, Cz)) {
                        return new BlockPos(Cx, surface(Wastes, Cx, Cz), Cz);
                    }
                }
            }
        }
        Wastes.getChunk(X >> 4, Z >> 4);
        return new BlockPos(X, Math.max(MinimumY, surface(Wastes, X, Z)), Z);
    }

    public static void build(ServerLevel Wastes, BlockPos Lower, Direction Front) {
        int Flags = Block.UPDATE_ALL;
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 3; Dz++) {
                BlockPos Floor = Lower.offset(Dx, -1, Dz);
                if (!Wastes.getBlockState(Floor).isFaceSturdy(Wastes, Floor, Direction.UP)) {
                    Wastes.setBlock(Floor, Blocks.COARSE_DIRT.defaultBlockState(), Flags);
                }
                for (int Dy = 0; Dy <= 2; Dy++) {
                    BlockPos Clear = Lower.offset(Dx, Dy, Dz);
                    if (!Wastes.getBlockState(Clear).isAir()) {
                        Wastes.setBlock(Clear, Blocks.AIR.defaultBlockState(), Flags);
                    }
                }
            }
        }
        Wastes.setBlock(Lower.below(), Blocks.COARSE_DIRT.defaultBlockState(), Flags);
        Scp093ReturnMirrorBlock.place(Wastes, Lower, Front, Flags);
    }
}
