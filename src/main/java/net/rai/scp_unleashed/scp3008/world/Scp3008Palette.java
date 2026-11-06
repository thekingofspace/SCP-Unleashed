package net.rai.scp_unleashed.scp3008.world;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.rai.scp_unleashed.scp3008.Scp3008Department;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.block.Scp3008ArrowBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008AxisBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008BeamBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008CableBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008CounterBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008CrateBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008ExitDoorBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FacadeBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FacingSealedBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FixtureBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FloorBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008PillarBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008ShapedBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008SignBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008StoolBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008TrayBlock;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Code;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp3008Palette {
    private static final Map<Integer, BlockState> Cache = new ConcurrentHashMap<>();
    private static final BlockState Missing = Blocks.AIR.defaultBlockState();

    private Scp3008Palette() {
    }

    public static Direction direction(int Code) {
        Direction Found = Direction.from3DDataValue(Scp3008Code.dir(Code));
        return Found.getAxis().isHorizontal() ? Found : Direction.NORTH;
    }

    private static Direction.Axis axis(int Variant) {
        return Variant == 0 ? Direction.Axis.X : Direction.Axis.Z;
    }

    public static BlockState state(int Code) {
        BlockState Found = Cache.get(Code);
        if (Found == null) {
            Found = build(Code);
            Cache.put(Code, Found == null ? Missing : Found);
        }
        return Found == Missing ? null : Found;
    }

    private static BlockState build(int Code) {
        int Variant = Scp3008Code.variant(Code);
        Direction Facing = direction(Code);
        return switch (Scp3008Code.kind(Code)) {
            case Scp3008Code.Floor -> Scp3008Registry.Floor.get().defaultBlockState().setValue(Scp3008FloorBlock.Finish, Scp3008FloorBlock.Style.of(Variant));
            case Scp3008Code.Line -> Scp3008Registry.FloorLine.get().defaultBlockState().setValue(Scp3008AxisBlock.Axis, axis(Variant));
            case Scp3008Code.Arrow -> Scp3008Registry.FloorArrow.get().defaultBlockState().setValue(Scp3008ArrowBlock.Facing, Facing);
            case Scp3008Code.Hatch -> Scp3008Registry.FloorHatch.get().defaultBlockState();
            case Scp3008Code.Pillar -> Scp3008Registry.Pillar.get().defaultBlockState().setValue(Scp3008PillarBlock.Section, Scp3008PillarBlock.Part.of(Variant));
            case Scp3008Code.Ceiling -> Scp3008Registry.Ceiling.get().defaultBlockState();
            case Scp3008Code.Cable -> Scp3008Registry.Cable.get().defaultBlockState().setValue(Scp3008CableBlock.Fit, Scp3008CableBlock.Fitting.of(Variant));
            case Scp3008Code.Light -> Scp3008Registry.StoreLight.get().defaultBlockState().setValue(Scp3008AxisBlock.Axis, axis(Variant));
            case Scp3008Code.Sign -> Scp3008Registry.DepartmentSign.get().defaultBlockState()
                    .setValue(Scp3008SignBlock.Department, Scp3008Department.of(Scp3008Code.signDepartment(Variant)))
                    .setValue(Scp3008SignBlock.Part, Scp3008Code.signPart(Variant))
                    .setValue(Scp3008SignBlock.Axis, axis(Scp3008Code.signAxis(Variant)));
            case Scp3008Code.Emergency -> Scp3008Registry.EmergencyLight.get().defaultBlockState().setValue(Scp3008FacingSealedBlock.Facing, Facing)
                    .setValue(Scp3008FacingSealedBlock.Variant, Math.min(3, Variant));
            case Scp3008Code.DisplayWall -> Scp3008Registry.DisplayWall.get().defaultBlockState();
            case Scp3008Code.Shelf -> Scp3008Registry.Shelf.get().defaultBlockState().setValue(Scp3008FixtureBlock.FACING, Facing)
                    .setValue(Scp3008FixtureBlock.Variant, Math.min(7, Variant));
            case Scp3008Code.RackPost -> Scp3008Registry.RackPost.get().fromBits(Variant);
            case Scp3008Code.RackBeam -> Scp3008Registry.RackBeam.get().defaultBlockState().setValue(Scp3008BeamBlock.Axis, axis(Variant));
            case Scp3008Code.Crate -> Scp3008Registry.Crate.get().defaultBlockState().setValue(Scp3008CrateBlock.Facing, Facing);
            case Scp3008Code.Counter -> Scp3008Registry.Counter.get().defaultBlockState().setValue(Scp3008CounterBlock.Facing, Facing);
            case Scp3008Code.Menu -> Scp3008Registry.MenuBoard.get().defaultBlockState().setValue(Scp3008FixtureBlock.FACING, Facing)
                    .setValue(Scp3008FixtureBlock.Variant, Math.min(7, Variant));
            case Scp3008Code.FoodTable -> Scp3008Registry.FoodTable.get().defaultBlockState();
            case Scp3008Code.Tray -> Scp3008Registry.Tray.get().defaultBlockState().setValue(Scp3008TrayBlock.Serving, Scp3008TrayBlock.Food.of(Variant));
            case Scp3008Code.Stool -> Scp3008Registry.Stool.get().defaultBlockState().setValue(Scp3008StoolBlock.FACING, Facing);
            case Scp3008Code.Planter -> Scp3008Registry.Planter.get().defaultBlockState().setValue(Scp3008ShapedBlock.Variant, Math.min(3, Variant));
            case Scp3008Code.ExitDoor -> Scp3008Registry.ExitDoor.get().defaultBlockState().setValue(Scp3008ExitDoorBlock.Facing, Facing)
                    .setValue(Scp3008ExitDoorBlock.Half, (Variant & 1) != 0 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER)
                    .setValue(Scp3008ExitDoorBlock.Right, (Variant & 2) != 0);
            case Scp3008Code.ExitSign -> Scp3008Registry.ExitSign.get().defaultBlockState().setValue(Scp3008FacingSealedBlock.Facing, Facing)
                    .setValue(Scp3008FacingSealedBlock.Variant, Math.min(3, Variant));
            case Scp3008Code.ExitWall -> Scp3008Registry.Facade.get().defaultBlockState().setValue(Scp3008FacadeBlock.Surface, Scp3008FacadeBlock.Finish.Grey);
            case Scp3008Code.Rug -> carpet(Variant);
            case Scp3008Code.Rafter -> Scp3008Registry.Rafter.get().withArms(Variant);
            default -> null;
        };
    }

    private static BlockState carpet(int Variant) {
        Block Found = switch (DyeColor.byId(Variant)) {
            case WHITE -> Blocks.WHITE_CARPET;
            case ORANGE -> Blocks.ORANGE_CARPET;
            case MAGENTA -> Blocks.MAGENTA_CARPET;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CARPET;
            case YELLOW -> Blocks.YELLOW_CARPET;
            case LIME -> Blocks.LIME_CARPET;
            case PINK -> Blocks.PINK_CARPET;
            case GRAY -> Blocks.GRAY_CARPET;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CARPET;
            case CYAN -> Blocks.CYAN_CARPET;
            case PURPLE -> Blocks.PURPLE_CARPET;
            case BLUE -> Blocks.BLUE_CARPET;
            case BROWN -> Blocks.BROWN_CARPET;
            case GREEN -> Blocks.GREEN_CARPET;
            case RED -> Blocks.RED_CARPET;
            case BLACK -> Blocks.BLACK_CARPET;
        };
        return Found.defaultBlockState();
    }
}
