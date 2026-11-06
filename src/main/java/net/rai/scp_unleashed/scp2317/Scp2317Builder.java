package net.rai.scp_unleashed.scp2317;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp2317Builder {
    public static final int RingReach = 17;
    public static final int CaveReach = 21;
    private static final Map<ServerLevel, Boolean> Built = Collections.synchronizedMap(new WeakHashMap<>());

    private Scp2317Builder() {
    }

    public static void ensure(ServerLevel World) {
        if (!Scp2317World.standingIn() || Built.containsKey(World)) {
            return;
        }
        build(World);
        Built.put(World, true);
    }

    public static void forget() {
        Built.clear();
    }

    public static void build(ServerLevel World) {
        int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = -RingReach; X <= RingReach; X++) {
            for (int Z = -RingReach; Z <= RingReach; Z++) {
                for (int Y = Scp2317Plan.Ground - 2; Y <= Scp2317Plan.Ground + 18; Y++) {
                    set(World, Cursor.set(Scp2317World.baseX() + X, Y, Scp2317World.baseZ() + Z), Scp2317Plan.state(X, Y, Z), Flags);
                }
            }
        }
        for (int X = -2; X <= 3; X++) {
            for (int Z = Scp2317Plan.DoorZ - 5; Z <= Scp2317Plan.DoorZ + 2; Z++) {
                for (int Y = Scp2317Plan.Ground - 1; Y <= Scp2317Plan.Ground + 4; Y++) {
                    set(World, Cursor.set(Scp2317World.baseX() + X, Y, Scp2317World.baseZ() + Z), Scp2317Plan.state(X, Y, Z), Flags);
                }
            }
        }
        for (int X = -CaveReach; X <= CaveReach; X++) {
            for (int Z = -CaveReach; Z <= CaveReach; Z++) {
                for (int Y = Scp2317Plan.CaveFloor - 1; Y <= Scp2317Plan.CaveFloor + 3; Y++) {
                    set(World, Cursor.set(Scp2317World.baseX() + X, Y, Scp2317World.baseZ() + Z), Scp2317Plan.state(X, Y, Z), Flags);
                }
            }
        }
        placeFeatures(World, 0, 0, Flags);
    }

    private static void set(ServerLevel World, BlockPos Pos, BlockState State, int Flags) {
        if (World.getBlockState(Pos) != State) {
            World.setBlock(Pos, State, Flags);
        }
    }

    public static void placeFeatures(LevelAccessor World, int ChunkMinX, int ChunkMinZ, int Flags) {
        World.setBlock(Scp2317World.vasePos(), Scp2317Registry.Vase.get().defaultBlockState(), Flags);
        World.setBlock(Scp2317World.kingPos(), Scp2317Registry.King.get().defaultBlockState(), Flags);
        Scp2317DoorBlock.placeWhole(World, Scp2317World.farDoor(), Scp2317Plan.DoorFront, Scp2317Registry.FarDoor.get(), Flags);
    }
}
