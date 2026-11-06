package net.rai.scp_unleashed.scp024;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;

import java.util.ArrayList;
import java.util.List;

public final class Scp024Palette {
    public static final int Quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    public static final int LightLevel = 15;
    public static final int DimLevel = 5;

    private Scp024Palette() {
    }

    public static Direction side(int Facing) {
        return Direction.from3DDataValue(Facing);
    }

    private static Direction flat(int Facing) {
        Direction Side = side(Facing);
        return Side.getAxis().isHorizontal() ? Side : Direction.SOUTH;
    }

    public static boolean attached(Scp024Mat Mat) {
        return switch (Mat) {
            case StoneButton, Ladder, StonePlate, IronTrapdoor, Lamp, Sign, Bumper, Light, DimLight, Lava -> true;
            default -> false;
        };
    }

    public static BlockState state(Scp024Mat Mat, int Facing, int Variant) {
        return switch (Mat) {
            case Air -> Blocks.AIR.defaultBlockState();
            case Barrier -> Blocks.BARRIER.defaultBlockState();
            case Light -> Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LightLevel);
            case DimLight -> Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, DimLevel);
            case Siding -> Scp024Registry.Siding.get().defaultBlockState();
            case Concrete -> Scp024Registry.Concrete.get().defaultBlockState();
            case Roof -> Scp024Registry.Roof.get().defaultBlockState();
            case Trim -> Scp024Registry.Trim.get().defaultBlockState();
            case DockDoor -> Scp024Registry.DockDoor.get().defaultBlockState();
            case Bumper -> Scp024Registry.Bumper.get().defaultBlockState().setValue(Scp024MountBlock.Facing, flat(Facing));
            case Lamp -> Scp024Registry.Lamp.get().defaultBlockState().setValue(Scp024MountBlock.Facing, flat(Facing));
            case Sign -> Scp024Registry.Sign.get().defaultBlockState().setValue(Scp024MountBlock.Facing, flat(Facing))
                    .setValue(Scp024SignBlock.Part, Math.min(Scp024SignBlock.Parts - 1, Variant));
            case StageDoor -> door(Scp024Registry.StageDoor.get(), Facing, Variant);
            case ExitDoor -> door(Scp024Registry.ExitDoor.get(), Facing, Variant);
            case StageFloor -> Scp024Registry.StageFloor.get().defaultBlockState();
            case StageWall -> Scp024Registry.StageWall.get().defaultBlockState();
            case Marquee -> Scp024Registry.Marquee.get().defaultBlockState();
            case Curtain -> Scp024Registry.Curtain.get().defaultBlockState();
            case BlackConcrete -> Blocks.BLACK_CONCRETE.defaultBlockState();
            case WhiteConcrete -> Blocks.WHITE_CONCRETE.defaultBlockState();
            case RedConcrete -> Blocks.RED_CONCRETE.defaultBlockState();
            case YellowConcrete -> Blocks.YELLOW_CONCRETE.defaultBlockState();
            case LimeConcrete -> Blocks.LIME_CONCRETE.defaultBlockState();
            case BlueConcrete -> Blocks.BLUE_CONCRETE.defaultBlockState();
            case OrangeConcrete -> Blocks.ORANGE_CONCRETE.defaultBlockState();
            case PurpleConcrete -> Blocks.PURPLE_CONCRETE.defaultBlockState();
            case GrayConcrete -> Blocks.GRAY_CONCRETE.defaultBlockState();
            case LightBlueConcrete -> Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
            case MagentaConcrete -> Blocks.MAGENTA_CONCRETE.defaultBlockState();
            case GreenConcrete -> Blocks.GREEN_CONCRETE.defaultBlockState();
            case CyanConcrete -> Blocks.CYAN_CONCRETE.defaultBlockState();
            case PinkConcrete -> Blocks.PINK_CONCRETE.defaultBlockState();
            case OrangeTerracotta -> Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            case PackedIce -> Blocks.PACKED_ICE.defaultBlockState();
            case BlueIce -> Blocks.BLUE_ICE.defaultBlockState();
            case Magma -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case GoldBlock -> Blocks.GOLD_BLOCK.defaultBlockState();
            case SeaLantern -> Blocks.SEA_LANTERN.defaultBlockState();
            case IronBars -> Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
            case StonePlate -> Blocks.STONE_PRESSURE_PLATE.defaultBlockState();
            case StoneButton -> side(Facing) == Direction.UP
                    ? Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.FLOOR).setValue(ButtonBlock.FACING, Direction.SOUTH)
                    : Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, flat(Facing));
            case IronTrapdoor -> Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.FACING, flat(Facing));
            case Target -> Blocks.TARGET.defaultBlockState();
            case Ladder -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, flat(Facing));
            case Seat -> Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, flat(Facing));
            case Dispenser -> Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, side(Facing));
            case SmoothSlab -> Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
            case Lava -> Blocks.LAVA.defaultBlockState();
        };
    }

    private static BlockState door(Block Door, int Facing, int Variant) {
        return Door.defaultBlockState().setValue(Scp024DoorBlock.Facing, flat(Facing))
                .setValue(Scp024DoorBlock.Column, Math.min(Scp024DoorBlock.Columns - 1, Variant / Scp024DoorBlock.Rows))
                .setValue(Scp024DoorBlock.Row, Variant % Scp024DoorBlock.Rows);
    }

    public static void clear(Level World, BlockPos Origin, Scp024Plan Plan) {
        int[] Low = Plan.low();
        int[] High = Plan.high();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        BlockState Empty = Blocks.AIR.defaultBlockState();
        for (int Y = High[1]; Y >= Low[1]; Y--) {
            for (int Z = Low[2]; Z <= High[2]; Z++) {
                for (int X = Low[0]; X <= High[0]; X++) {
                    Cursor.set(Origin.getX() + X, Origin.getY() + Y, Origin.getZ() + Z);
                    if (World.isOutsideBuildHeight(Cursor)) {
                        continue;
                    }
                    if (!World.getBlockState(Cursor).isAir()) {
                        World.setBlock(Cursor, Empty, Quiet);
                    }
                }
            }
        }
    }

    public static void build(Level World, BlockPos Origin, Scp024Plan Plan) {
        List<Runnable> Later = new ArrayList<>();
        Plan.forEach((X, Y, Z, Mat, Facing, Variant) -> {
            BlockPos Target = Origin.offset(X, Y, Z);
            if (World.isOutsideBuildHeight(Target)) {
                return;
            }
            BlockState State = state(Mat, Facing, Variant);
            if (attached(Mat)) {
                Later.add(() -> World.setBlock(Target, State, Quiet));
            } else {
                World.setBlock(Target, State, Quiet);
            }
        });
        Later.forEach(Runnable::run);
    }

    public static void restore(Level World, BlockPos Origin, Scp024Plan Plan, int X, int Y, int Z) {
        World.setBlock(Origin.offset(X, Y, Z), state(Plan.matAt(X, Y, Z), Plan.facingAt(X, Y, Z), Plan.variantAt(X, Y, Z)), Quiet);
    }
}
