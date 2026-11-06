package net.rai.scp_unleashed.scp163;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class Scp163Ship {
    public static final int MinX = -8;
    public static final int MinY = -2;
    public static final int MinZ = -5;
    public static final int Width = 17;
    public static final int Depth = 11;
    public static final BlockPos HatchLower = new BlockPos(0, 1, 4);
    public static final BlockPos SpawnCell = new BlockPos(0, 1, 0);
    public static final BlockPos InteriorLow = new BlockPos(-6, 1, -3);
    public static final BlockPos InteriorHigh = new BlockPos(6, 3, 3);
    public static final String SolidCodes = "HSLD";
    public static final String[][] Layers = {
            {"                 ", "                 ", "                 ", "       HHH       ", "    HHHHHHHHH    ", "    HHHHHHHHH    ", "    HHHHHHHHH    ", "       HHH       ", "                 ", "                 ", "                 "},
            {"                 ", "                 ", "      HHHHH      ", "    HHHHHHHHH    ", "   HHHHHHHHHHH   ", "  HHHHHHHHHHHHH  ", "   HHHHHHHHHHH   ", "    HHHHHHHHH    ", "      HHHHH      ", "                 ", "                 "},
            {"                 ", "       HHH       ", "    HHHDDDHHH    ", "  HHDDDDDDDDDHH  ", "  HDDDDDDDDDDDH  ", " HDDDDDDDDDDDDDH ", "  HDDDDDDDDDDDH  ", "  HHDDDDDDDDDHH  ", "    HHHDDDHHH    ", "       HHH       ", "                 "},
            {"                 ", "     HHHHHHH     ", "   HHHH...HHHH   ", "  HH.......G.HH  ", " HH..........CHH ", " H.A...........H ", " HH...........HH ", "  HH.........HH  ", "   HHHH...HHHH   ", "     HHHhHHH     ", "                 "},
            {"                 ", "    SSSSSSSSS    ", "  SSHH.....HHSS  ", " SH...........HS ", " L.............L ", " L.............L ", " L.............L ", " SH...........HS ", "  SSHH.....HHSS  ", "    SSSShSSSS    ", "                 "},
            {"                 ", "     HHHHHHH     ", "   HHHH...HHHH   ", "  HH.........HH  ", " HH...........HH ", " H.............H ", " HH...........HH ", "  HH.........HH  ", "   HHHH...HHHH   ", "     HHHHHHH     ", "                 "},
            {"                 ", "        H        ", "    HHHHHHHHH    ", "   HHHHHHHHHHH   ", "  HHHHHHHHHHHHH  ", "  HHLHHHLHHHLHH  ", "  HHHHHHHHHHHHH  ", "   HHHHHHHHHHH   ", "    HHHHHHHHH    ", "        H        ", "                 "},
            {"                 ", "                 ", "        H        ", "     HHHHHHH     ", "    HHHHHHHHH    ", "   HHHHHHHHHHH   ", "    HHHHHHHHH    ", "     HHHHHHH     ", "        H        ", "                 ", "                 "},
            {"                 ", "                 ", "                 ", "                 ", "       SSS       ", "      SSSSS      ", "       SSS       ", "                 ", "                 ", "                 ", "                 "}
    };

    private Scp163Ship() {
    }

    public static char codeAt(int X, int Y, int Z) {
        int Layer = Y - MinY;
        int Row = Z - MinZ;
        int Column = X - MinX;
        if (Layer < 0 || Layer >= Layers.length || Row < 0 || Row >= Depth || Column < 0 || Column >= Width) {
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

    public static AABB box(BlockPos Origin, Direction Front, BlockPos Low, BlockPos High) {
        return AABB.encapsulatingFullBlocks(toWorld(Origin, Front, Low), toWorld(Origin, Front, High));
    }

    public static AABB interior(BlockPos Origin, Direction Front) {
        return box(Origin, Front, InteriorLow, InteriorHigh);
    }

    public static AABB footprint(BlockPos Origin, Direction Front) {
        return box(Origin, Front, new BlockPos(MinX, MinY, MinZ), new BlockPos(MinX + Width - 1, MinY + height() - 1, MinZ + Depth - 1));
    }

    @Nullable
    public static BlockState stateFor(char Code, Direction Front, int Y) {
        return switch (Code) {
            case 'H' -> Scp163Registry.Hull.get().defaultBlockState();
            case 'S' -> Scp163Registry.HullSeam.get().defaultBlockState();
            case 'L' -> Scp163Registry.HullLight.get().defaultBlockState();
            case 'D' -> Scp163Registry.Deck.get().defaultBlockState();
            case 'h' -> Scp163Registry.Hatch.get().defaultBlockState().setValue(DoorBlock.FACING, facing(Front, Direction.NORTH)).setValue(DoorBlock.OPEN, false)
                    .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.HALF, Y == HatchLower.getY() ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            case 'A' -> Scp163Registry.Apparatus.get().defaultBlockState();
            case 'C' -> Scp163Registry.Console.get().defaultBlockState().setValue(Scp163DeviceBlock.FACING, facing(Front, Direction.WEST));
            case 'G' -> Scp163Registry.Signaller.get().defaultBlockState().setValue(Scp163DeviceBlock.FACING, facing(Front, Direction.SOUTH));
            case '.' -> Blocks.AIR.defaultBlockState();
            default -> null;
        };
    }

    public static int build(Level World, BlockPos Origin, Direction Front, Predicate<BlockPos> Allowed) {
        int Placed = 0;
        for (int Pass = 0; Pass < 2; Pass++) {
            for (int Y = MinY; Y < MinY + height(); Y++) {
                for (int Z = MinZ; Z < MinZ + Depth; Z++) {
                    for (int X = MinX; X < MinX + Width; X++) {
                        char Code = codeAt(X, Y, Z);
                        boolean Solid = SolidCodes.indexOf(Code) >= 0;
                        if (Code == ' ' || Pass == 0 && !Solid || Pass == 1 && Solid) {
                            continue;
                        }
                        BlockPos Target = toWorld(Origin, Front, X, Y, Z);
                        BlockState State = stateFor(Code, Front, Y);
                        if (State == null || World.isOutsideBuildHeight(Target) || !Allowed.test(Target)) {
                            continue;
                        }
                        World.setBlock(Target, State, Block.UPDATE_CLIENTS);
                        Placed++;
                    }
                }
            }
        }
        return Placed;
    }
}
