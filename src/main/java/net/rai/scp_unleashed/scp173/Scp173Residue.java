package net.rai.scp_unleashed.scp173;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class Scp173Residue {
    public static final int NearTries = 6;

    private Scp173Residue() {
    }

    public static boolean canHold(ServerLevel World, BlockPos Pos) {
        BlockState Here = World.getBlockState(Pos);
        if (!Here.isAir() || !World.getFluidState(Pos).isEmpty()) {
            return false;
        }
        BlockPos Below = Pos.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    public static boolean place(ServerLevel World, BlockPos Pos) {
        if (!canHold(World, Pos)) {
            return false;
        }
        return World.setBlock(Pos, Scp173Registry.Residue.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    public static boolean placeNear(ServerLevel World, BlockPos Centre, Vec3 Origin, RandomSource Random) {
        Scp173Passage Way = Scp173Passage.of(World);
        for (int Attempt = 0; Attempt < NearTries; Attempt++) {
            BlockPos Spot = Attempt == 0 ? Centre : Centre.offset(Random.nextInt(3) - 1, 0, Random.nextInt(3) - 1);
            if (!canHold(World, Spot) || !Way.passes(Origin, new Vec3(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5))) {
                continue;
            }
            if (place(World, Spot)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isResidue(BlockState State) {
        return State.is(Scp173Registry.Residue.get());
    }

    public static List<BlockPos> find(ServerLevel World, BlockPos Centre, int Radius, int Height) {
        return Scp173ResidueIndex.get(World).near(World, Centre, Radius, Height);
    }

    public static int count(ServerLevel World, BlockPos Centre, int Radius, int Height) {
        return find(World, Centre, Radius, Height).size();
    }

    public static int seed(ServerLevel World, BlockPos Centre, int Radius, int Height) {
        Scp173ResidueIndex Index = Scp173ResidueIndex.get(World);
        int Found = 0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dy = -Height; Dy <= Height; Dy++) {
            for (int Dx = -Radius; Dx <= Radius; Dx++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    Cursor.set(Centre.getX() + Dx, Centre.getY() + Dy, Centre.getZ() + Dz);
                    if (World.isLoaded(Cursor) && isResidue(World.getBlockState(Cursor))) {
                        Index.add(Cursor.immutable());
                        Found++;
                    }
                }
            }
        }
        return Found;
    }
}
