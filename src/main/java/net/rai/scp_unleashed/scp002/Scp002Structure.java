package net.rai.scp_unleashed.scp002;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class Scp002Structure {
    public static final int MinX = -6;
    public static final int MinY = -5;
    public static final int MinZ = -7;
    public static final int Size = 13;
    public static final BlockPos DoorLower = new BlockPos(0, 1, 5);
    public static final BlockPos LampCell = new BlockPos(-1, 1, -2);
    public static final BlockPos InteriorLow = new BlockPos(-2, 1, -2);
    public static final BlockPos InteriorHigh = new BlockPos(2, 3, 4);
    public static final BlockPos ShellLow = new BlockPos(-3, 0, -3);
    public static final BlockPos ShellHigh = new BlockPos(3, 4, 4);
    public static final String RoomCodes = ".BbPMTC012345";
    public static final String[][] Layers = {
            {"             ", "             ", "             ", "             ", "     SSS     ", "    SSSSS    ", "    SSSSS    ", "    SSSSS    ", "     SSS     ", "             ", "             ", "             ", "             "},
            {"             ", "             ", "     SSS     ", "    SSSSS    ", "   SSFFFSS   ", "  SSFFFFFSS  ", "  SSFFFFFSS  ", "  SSFFFFFSS  ", "   SSFFFSS   ", "    SSSSS    ", "     SSS     ", "             ", "             "},
            {"             ", "     SSS     ", "   SSFFFSS   ", "  SSFFFFFSS  ", "  SFFFFFFFS  ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", "  SFFFFFFFS  ", "  SSFFFFFSS  ", "   SSFFFSS   ", "     SSS     ", "             "},
            {"             ", "    SSSSS    ", "  SSFFFFFSS  ", "  SFFFFFFFS  ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", "  SFFFFFFFS  ", "  SSFFFFFSS  ", "    SSSSS    ", "             "},
            {"     SSS     ", "   SSFFFSS   ", "  SFFFFFFFS  ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", "SFFFFFFFFFFFS", "SFFFFFFFFFFFS", "SFFFFFFFFFFFS", " SFFFFFFFFFS ", " SFFFFFFFFFS ", "  SFFFFFFFS  ", "   SSFFFSS   ", "     SSS     "},
            {"    SSSSS    ", "  SSFFFFFSS  ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", "SFFFFFFFFFFFS", "SFFFLLLLLFFFS", "SFFFLLLLLFFFS", "SFFFLLLLLFFFS", "SFFFLLLLLFFFS", " SFFLLLLLFFS ", " SFFFFLFFFS  ", "  SSFFLFFS   ", "    SFFFS    "},
            {"    SSSSS    ", "  SSFFFFFSS  ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", "SFFFWWWWWFFFS", "SFFWBPM..WFFS", "SFFWb..CTWFFS", "SFFW.012.WFFS", "SFFW.345.WFFS", " SFW.....WFS ", " SFFWW.WWFS  ", "  SSFW.WFSS  ", "    FFDFS    "},
            {"    SSSSS    ", "  SSFFFFFSS  ", "  SFFFFFFFFS ", " SFFFFFFFFFS ", "SFFFWWWWWFFFS", "SFFW.....WFFS", "SFFW.....WFFS", "SFFW.....WFFS", "SFFW.....WFFS", " SFW.....WFS ", " SFFWW.WWFS  ", "  SSFW.WFS   ", "    SFDFF    "},
            {"    SSSS     ", "   SFFFFSS   ", "  SFFFFFFFS  ", " SFFFFFFFFFS ", " SFFWWWWWFFFS", "SFFW.....WFFS", "SFFW.....WFFS", "SFFW.....WFFS", "SFFW.....WFS ", " SFW.....WFS ", "  SFWWWWWFS  ", "   SSFWFSS   ", "     FFF     "},
            {"             ", "    SSSSS    ", "   SFFFFFSS  ", "  SFFFFFFFFS ", " SFFFFFFFFFS ", " SFFWWWWWFFS ", " SFFWWWWWFFS ", " SFFWWWWWFFS ", " SFFWWWWWFFS ", " SFFWWWWWFS  ", "  SSFFFFFSS  ", "    SFFSS    ", "             "},
            {"             ", "     SSS     ", "    SSFFSS   ", "   SFFFFFSS  ", "  SFFFFFFFS  ", "  SFFFFFFFFS ", " SFFFFFFFFFS ", " SFFFFFFFFFS ", "  SFFFFFFFS  ", "  SSFFFFFFS  ", "   SSFFFSS   ", "     SSS     ", "             "},
            {"             ", "             ", "      SS     ", "    SSSSS    ", "   SSFFFSS   ", "   SFFFFFSS  ", "  SSFFFFFSS  ", "  SSFFFFFSS  ", "   SSFFFFS   ", "    SSSSSS   ", "     SSS     ", "             ", "             "},
            {"             ", "             ", "             ", "             ", "     SSS     ", "    SSSSS    ", "    SSSSS    ", "    SSSSS    ", "     SSSS    ", "             ", "             ", "             ", "             "}
    };

    private Scp002Structure() {
    }

    public static char codeAt(int X, int Y, int Z) {
        int Layer = Y - MinY;
        int Row = Z - MinZ;
        int Column = X - MinX;
        if (Layer < 0 || Layer >= Layers.length || Row < 0 || Row >= Size || Column < 0 || Column >= Size) {
            return ' ';
        }
        return Layers[Layer][Row].charAt(Column);
    }

    public static int height() {
        return Layers.length;
    }

    public static Rotation turnFor(Direction Front) {
        return switch (Front) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos toWorld(BlockPos Origin, Direction Front, int X, int Y, int Z) {
        return Origin.offset(new BlockPos(X, Y, Z).rotate(turnFor(Front)));
    }

    public static BlockPos toWorld(BlockPos Origin, Direction Front, BlockPos Local) {
        return toWorld(Origin, Front, Local.getX(), Local.getY(), Local.getZ());
    }

    public static Direction facing(Direction Front, Direction Local) {
        return turnFor(Front).rotate(Local);
    }

    public static BlockPos originFromDoor(BlockPos DoorPos, Direction Front) {
        return DoorPos.subtract(DoorLower.rotate(turnFor(Front)));
    }

    public static AABB box(BlockPos Origin, Direction Front, BlockPos Low, BlockPos High) {
        return AABB.encapsulatingFullBlocks(toWorld(Origin, Front, Low), toWorld(Origin, Front, High));
    }

    public static AABB interior(BlockPos Origin, Direction Front) {
        return box(Origin, Front, InteriorLow, InteriorHigh);
    }

    public static AABB footprint(BlockPos Origin, Direction Front) {
        return box(Origin, Front, new BlockPos(MinX, MinY, MinZ), new BlockPos(MinX + Size - 1, MinY + height() - 1, MinZ + Size - 1));
    }

    public static boolean isRoomCode(char Code) {
        return RoomCodes.indexOf(Code) >= 0;
    }

    private static BlockState surface(BlockState Base, Direction Front, int X, int Y, int Z) {
        BlockState State = Base;
        for (Direction Local : Direction.values()) {
            if (isRoomCode(codeAt(X + Local.getStepX(), Y + Local.getStepY(), Z + Local.getStepZ()))) {
                State = State.setValue(FleshSurfaceBlock.inside(facing(Front, Local)), true);
            }
        }
        return State;
    }

    @Nullable
    public static BlockState stateFor(char Code, Direction Front, int X, int Y, int Z) {
        return switch (Code) {
            case 'S' -> Scp002Registry.Shell.get().defaultBlockState();
            case 'F' -> Scp002Registry.Flesh.get().defaultBlockState();
            case 'W' -> surface(Scp002Registry.FleshWall.get().defaultBlockState(), Front, X, Y, Z);
            case 'L' -> surface(Scp002Registry.FleshFloor.get().defaultBlockState(), Front, X, Y, Z);
            case 'D' -> Scp002Registry.Door.get().defaultBlockState().setValue(BulkheadDoorBlock.Facing, Front)
                    .setValue(BulkheadDoorBlock.Half, Y == DoorLower.getY() ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            case 'B' -> Scp002Registry.Bed.get().defaultBlockState().setValue(FleshBedBlock.Facing, facing(Front, Direction.SOUTH)).setValue(FleshBedBlock.Part, BedPart.HEAD);
            case 'b' -> Scp002Registry.Bed.get().defaultBlockState().setValue(FleshBedBlock.Facing, facing(Front, Direction.SOUTH)).setValue(FleshBedBlock.Part, BedPart.FOOT);
            case 'P' -> Scp002Registry.Lamp.get().defaultBlockState();
            case 'M' -> Scp002Registry.Dresser.get().defaultBlockState().setValue(FleshFixtureBlock.Facing, facing(Front, Direction.SOUTH));
            case 'T' -> Scp002Registry.Desk.get().defaultBlockState().setValue(FleshFixtureBlock.Facing, facing(Front, Direction.WEST));
            case 'C' -> Scp002Registry.Chair.get().defaultBlockState().setValue(FleshFixtureBlock.Facing, facing(Front, Direction.EAST));
            case '0', '1', '2', '3', '4', '5' -> Scp002Registry.Rug.get().defaultBlockState().setValue(FleshRugBlock.Facing, facing(Front, Direction.SOUTH))
                    .setValue(FleshRugBlock.Part, Code - '0');
            case '.' -> Blocks.AIR.defaultBlockState();
            default -> null;
        };
    }

    public static int build(Level World, BlockPos Origin, Direction Front, boolean ClearAbove, Predicate<BlockPos> Allowed) {
        int Placed = 0;
        for (int Pass = 0; Pass < 2; Pass++) {
            for (int Y = MinY; Y < MinY + height(); Y++) {
                for (int Z = MinZ; Z < MinZ + Size; Z++) {
                    for (int X = MinX; X < MinX + Size; X++) {
                        char Code = codeAt(X, Y, Z);
                        boolean Solid = "SFWL".indexOf(Code) >= 0;
                        if (Pass == 0 && !Solid && Code != ' ' || Pass == 1 && (Solid || Code == ' ')) {
                            continue;
                        }
                        BlockPos Target = toWorld(Origin, Front, X, Y, Z);
                        if (World.isOutsideBuildHeight(Target) || !Allowed.test(Target)) {
                            continue;
                        }
                        BlockState State = stateFor(Code, Front, X, Y, Z);
                        if (State == null) {
                            if (!ClearAbove || Y < 1 || World.getBlockState(Target).isAir()) {
                                continue;
                            }
                            State = Blocks.AIR.defaultBlockState();
                        }
                        World.setBlock(Target, State, Block.UPDATE_CLIENTS);
                        Placed++;
                    }
                }
            }
        }
        return Placed;
    }

    public static void setAwake(Level World, BlockPos Origin, Direction Front, boolean Awake) {
        for (int Y = ShellLow.getY(); Y <= ShellHigh.getY(); Y++) {
            for (int Z = ShellLow.getZ(); Z <= ShellHigh.getZ(); Z++) {
                for (int X = ShellLow.getX(); X <= ShellHigh.getX(); X++) {
                    BlockPos Target = toWorld(Origin, Front, X, Y, Z);
                    BlockState State = World.getBlockState(Target);
                    if (State.getBlock() instanceof FleshSurfaceBlock && State.getValue(FleshSurfaceBlock.Awake) != Awake) {
                        World.setBlock(Target, State.setValue(FleshSurfaceBlock.Awake, Awake), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
