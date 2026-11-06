package net.rai.scp_unleashed.scp106;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class Scp106PocketLayout {
    public static final int Signature = 4;
    public static final int BaseY = 64;
    public static final int Spacing = 64;
    public static final int Arrival = 0;
    public static final int Corridor = 1;
    public static final int Hall = 2;
    public static final int Junction = 3;
    public static final int Trench = 4;
    public static final int Warren = 5;
    public static final int RoomCount = 6;
    public static final List<Integer> Wandering = List.of(Arrival, Corridor, Hall, Junction);
    public static final int TrenchFall = 11;
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final long Seed = 1061061L;

    private Scp106PocketLayout() {
    }

    public static int originX(int Room) {
        return Room * Spacing;
    }

    public static int roomAt(double X) {
        return Math.floorDiv((int) Math.floor(X) + Spacing / 2, Spacing);
    }

    public static Vec3 arrival(int Room) {
        int Ox = originX(Room);
        return switch (Room) {
            case Corridor -> new Vec3(Ox + 1.5, BaseY, 1.5);
            case Hall -> new Vec3(Ox + 7.5, BaseY, 12.5);
            case Junction -> new Vec3(Ox + 7.5, BaseY, 7.5);
            case Trench -> new Vec3(Ox + 4.5, BaseY + TrenchFall, 4.5);
            case Warren -> new Vec3(Ox + 8.0, BaseY, 8.0);
            default -> new Vec3(Ox + 4.5, BaseY, 4.5);
        };
    }

    public static float arrivalYaw(int Room) {
        return switch (Room) {
            case Corridor -> 0.0f;
            case Hall -> 180.0f;
            case Junction -> 90.0f;
            default -> 180.0f;
        };
    }

    public static Vec3 warrenSpot(RandomSource Random) {
        int Ox = originX(Warren);
        return new Vec3(Ox + 3.5 + Random.nextInt(10), BaseY, 3.5 + Random.nextInt(10));
    }

    public static int warrenChunkX() {
        return (originX(Warren) + 8) >> 4;
    }

    public static int warrenChunkZ() {
        return 8 >> 4;
    }

    public static void build(ServerLevel World) {
        RandomSource Random = RandomSource.create(Seed);
        arrivalRoom(World, Random);
        corridorRoom(World, Random);
        hallRoom(World, Random);
        junctionRoom(World, Random);
        trenchRoom(World, Random);
        warrenRoom(World, Random);
    }

    private static BlockState stone() {
        return Scp106Registry.PocketStone.get().defaultBlockState();
    }

    private static BlockState corroded() {
        return Scp106Registry.CorrodedStone.get().defaultBlockState();
    }

    private static BlockState tiles() {
        return Scp106Registry.PocketTiles.get().defaultBlockState();
    }

    private static BlockState seep() {
        return Scp106Registry.PocketSeep.get().defaultBlockState();
    }

    private static BlockState wall(RandomSource Random) {
        float Roll = Random.nextFloat();
        return Roll < 0.38f ? corroded() : stone();
    }

    private static BlockState floor(RandomSource Random, boolean Tiled) {
        float Roll = Random.nextFloat();
        if (Roll < 0.3f) {
            return corroded();
        }
        return Tiled ? tiles() : stone();
    }

    private static void set(ServerLevel World, int X, int Y, int Z, BlockState State) {
        BlockPos Pos = new BlockPos(X, Y, Z);
        if (World.getBlockState(Pos) != State) {
            World.setBlock(Pos, State, Flags);
        }
    }

    private static void box(ServerLevel World, RandomSource Random, int X0, int Y0, int Z0, int X1, int Y1, int Z1, boolean Tiled) {
        for (int X = X0 - 1; X <= X1 + 1; X++) {
            for (int Y = Y0 - 1; Y <= Y1 + 1; Y++) {
                for (int Z = Z0 - 1; Z <= Z1 + 1; Z++) {
                    boolean Inside = X >= X0 && X <= X1 && Y >= Y0 && Y <= Y1 && Z >= Z0 && Z <= Z1;
                    if (Inside) {
                        set(World, X, Y, Z, Blocks.AIR.defaultBlockState());
                    } else if (Y == Y0 - 1) {
                        set(World, X, Y, Z, floor(Random, Tiled));
                    } else {
                        set(World, X, Y, Z, wall(Random));
                    }
                }
            }
        }
    }

    private static void doorway(ServerLevel World, int X, int Y, int Z, Direction.Axis Along) {
        int Dx = Along == Direction.Axis.X ? 1 : 0;
        int Dz = Along == Direction.Axis.Z ? 1 : 0;
        for (int Row = 0; Row < Scp106DoorwayBlock.Rows; Row++) {
            set(World, X, Y + Row, Z, Scp106Registry.Doorway.get().piece(Along, Scp106DoorwayPart.atRow(Row)));
            set(World, X - Dx, Y + Row, Z - Dz, corroded());
            set(World, X + Dx, Y + Row, Z + Dz, corroded());
        }
        int Lintel = Y + Scp106DoorwayBlock.Rows;
        set(World, X - Dx, Lintel, Z - Dz, corroded());
        set(World, X, Lintel, Z, corroded());
        set(World, X + Dx, Lintel, Z + Dz, corroded());
    }

    private static void seeps(ServerLevel World, RandomSource Random, int X0, int Y0, int Z0, int X1, int Y1, int Z1, int Count) {
        for (int Index = 0; Index < Count; Index++) {
            int Side = Random.nextInt(4);
            int Y = Y0 + Random.nextInt(Math.max(1, Y1 - Y0 + 1));
            int X;
            int Z;
            switch (Side) {
                case 0 -> {
                    X = X0 - 1;
                    Z = Z0 + Random.nextInt(Z1 - Z0 + 1);
                }
                case 1 -> {
                    X = X1 + 1;
                    Z = Z0 + Random.nextInt(Z1 - Z0 + 1);
                }
                case 2 -> {
                    X = X0 + Random.nextInt(X1 - X0 + 1);
                    Z = Z0 - 1;
                }
                default -> {
                    X = X0 + Random.nextInt(X1 - X0 + 1);
                    Z = Z1 + 1;
                }
            }
            BlockPos Pos = new BlockPos(X, Y, Z);
            if (World.getBlockState(Pos).getBlock() instanceof Scp106PocketBlock && !(World.getBlockState(Pos).getBlock() instanceof Scp106DoorwayBlock)) {
                set(World, X, Y, Z, seep());
            }
        }
    }

    private static void arrivalRoom(ServerLevel World, RandomSource Random) {
        int Ox = originX(Arrival);
        int Y = BaseY;
        box(World, Random, Ox, Y, 0, Ox + 8, Y + 3, 8, false);
        for (int Index = 0; Index < 6; Index++) {
            int X = Ox + Random.nextInt(9);
            int Z = Random.nextInt(9);
            if (Math.abs(X - (Ox + 4)) + Math.abs(Z - 4) > 3 && X != Ox + 4 && Z != 4) {
                set(World, X, Y, Z, corroded());
            }
        }
        for (int Z = 2; Z <= 6; Z++) {
            set(World, Ox + 4, Y - 1, Z, seep());
        }
        doorway(World, Ox + 4, Y, -1, Direction.Axis.X);
        doorway(World, Ox + 9, Y, 4, Direction.Axis.Z);
        doorway(World, Ox - 1, Y, 4, Direction.Axis.Z);
        seeps(World, Random, Ox, Y + 1, 0, Ox + 8, Y + 3, 8, 5);
    }

    private static void corridorRoom(ServerLevel World, RandomSource Random) {
        int Ox = originX(Corridor);
        int Y = BaseY;
        box(World, Random, Ox, Y, 0, Ox + 2, Y + 3, 24, true);
        for (int Z = 4; Z <= 22; Z += 4) {
            for (int X = Ox; X <= Ox + 2; X++) {
                set(World, X, Y + 3, Z, corroded());
            }
            set(World, Ox - 1, Y + 1, Z, seep());
            set(World, Ox + 3, Y + 2, Z + 2, Random.nextBoolean() ? seep() : corroded());
        }
        doorway(World, Ox + 1, Y, 25, Direction.Axis.X);
        doorway(World, Ox - 1, Y, 9, Direction.Axis.Z);
        doorway(World, Ox + 3, Y, 17, Direction.Axis.Z);
        set(World, Ox + 1, Y, 13, corroded());
    }

    private static void hallRoom(ServerLevel World, RandomSource Random) {
        int Ox = originX(Hall);
        int Y = BaseY;
        box(World, Random, Ox, Y, 0, Ox + 14, Y + 5, 14, false);
        int[] Corners = {3, 10};
        for (int Cx : Corners) {
            for (int Cz : Corners) {
                for (int Dy = 0; Dy <= 5; Dy++) {
                    for (int Dx = 0; Dx < 2; Dx++) {
                        for (int Dz = 0; Dz < 2; Dz++) {
                            boolean Seep = Dy == 2 && Dx == 0 && Dz == 0;
                            set(World, Ox + Cx + Dx, Y + Dy, Cz + Dz, Seep ? seep() : wall(Random));
                        }
                    }
                }
            }
        }
        doorway(World, Ox + 7, Y, -1, Direction.Axis.X);
        doorway(World, Ox + 7, Y, 15, Direction.Axis.X);
        doorway(World, Ox - 1, Y, 7, Direction.Axis.Z);
        doorway(World, Ox + 15, Y, 7, Direction.Axis.Z);
        seeps(World, Random, Ox, Y + 2, 0, Ox + 14, Y + 4, 14, 6);
    }

    private static void junctionRoom(ServerLevel World, RandomSource Random) {
        int Ox = originX(Junction);
        int Y = BaseY;
        for (int X = Ox - 1; X <= Ox + 15; X++) {
            for (int Dy = -1; Dy <= 4; Dy++) {
                for (int Z = -1; Z <= 15; Z++) {
                    boolean ArmX = X >= Ox && X <= Ox + 14 && Z >= 6 && Z <= 8;
                    boolean ArmZ = Z >= 0 && Z <= 14 && X >= Ox + 6 && X <= Ox + 8;
                    boolean Inside = (ArmX || ArmZ) && Dy >= 0 && Dy <= 3;
                    if (Inside) {
                        set(World, X, Y + Dy, Z, Blocks.AIR.defaultBlockState());
                    } else if (Dy == -1) {
                        set(World, X, Y + Dy, Z, floor(Random, true));
                    } else {
                        set(World, X, Y + Dy, Z, wall(Random));
                    }
                }
            }
        }
        set(World, Ox + 7, Y + 4, 7, seep());
        set(World, Ox + 5, Y + 1, 5, seep());
        set(World, Ox + 9, Y + 2, 9, seep());
        doorway(World, Ox - 1, Y, 7, Direction.Axis.Z);
        doorway(World, Ox + 15, Y, 7, Direction.Axis.Z);
        doorway(World, Ox + 7, Y, -1, Direction.Axis.X);
        doorway(World, Ox + 7, Y, 15, Direction.Axis.X);
    }

    private static void trenchRoom(ServerLevel World, RandomSource Random) {
        int Ox = originX(Trench);
        int Y = BaseY;
        box(World, Random, Ox, Y, 0, Ox + 8, Y + TrenchFall + 3, 8, false);
        for (int X = Ox + 2; X <= Ox + 6; X++) {
            for (int Z = 2; Z <= 6; Z++) {
                set(World, X, Y - 1, Z, (X + Z) % 3 == 0 ? seep() : corroded());
            }
        }
        doorway(World, Ox + 4, Y, -1, Direction.Axis.X);
        doorway(World, Ox + 4, Y, 9, Direction.Axis.X);
        seeps(World, Random, Ox, Y + 3, 0, Ox + 8, Y + TrenchFall + 2, 8, 8);
    }

    private static void warrenRoom(ServerLevel World, RandomSource Random) {
        int Ox = originX(Warren);
        int Y = BaseY;
        box(World, Random, Ox + 2, Y, 2, Ox + 13, Y + 3, 13, false);
        seeps(World, Random, Ox + 2, Y + 1, 2, Ox + 13, Y + 2, 13, 4);
    }
}
