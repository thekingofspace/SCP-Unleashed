package net.rai.scp_unleashed.scp1730.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.block.Scp1730Decor;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Builder;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Scp1730WallAudit {
    public static final double Touch = 0.0625;
    private static final Set<String> Mounted = new HashSet<>();
    private static final Set<String> Hung = new HashSet<>();
    private static final Set<String> Lying = new HashSet<>();
    private static final List<String> MountedPrefixes = List.of("scp_unleashed:facility_sign_");

    static {
        for (String Each : List.of("scp1730_control", "scp1730_laser_emitter", "scp1730_symbol", "fire_extinguisher", "wall_clock", "security_camera", "lcz_wall_pipes",
                "keycard_scanner", "hcz_caged_lamp", "fluorescent_light", "containment_room_hazard_sign", "containment_room_cell_plaque")) {
            Mounted.add("scp_unleashed:" + Each);
        }
        for (String Each : net.rai.scp_unleashed.scp1730.plan.Scp1730SupportCheck.WallOnly) {
            Mounted.add("scp_unleashed:" + Each);
        }
        for (Scp1730Decor Each : Scp1730Decor.values()) {
            if (Each.kind() == Scp1730Decor.Kind.Decal || Each.kind() == Scp1730Decor.Kind.Light
                    || Each.kind() == Scp1730Decor.Kind.Prop && Each.shape() == Scp1730Decor.Shape.Wall) {
                Mounted.add("scp_unleashed:" + Each.id());
            }
        }
        Hung.add("scp_unleashed:scp1730_turret");
        Lying.add("scp_unleashed:scp1730_card");
    }

    private Scp1730WallAudit() {
    }

    public static boolean mounted(String Id) {
        if (Mounted.contains(Id) || Hung.contains(Id) || Lying.contains(Id)) {
            return true;
        }
        for (String Prefix : MountedPrefixes) {
            if (Id.startsWith(Prefix)) {
                return true;
            }
        }
        return false;
    }

    public static Direction backOf(String Id, BlockState State) {
        if (Hung.contains(Id)) {
            return Direction.UP;
        }
        if (Lying.contains(Id)) {
            return Direction.DOWN;
        }
        if (State.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            AttachFace Face = State.getValue(BlockStateProperties.ATTACH_FACE);
            if (Face == AttachFace.FLOOR) {
                return Direction.DOWN;
            }
            if (Face == AttachFace.CEILING) {
                return Direction.UP;
            }
            return State.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        }
        if (State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return State.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        }
        if (State.hasProperty(BlockStateProperties.FACING)) {
            return State.getValue(BlockStateProperties.FACING).getOpposite();
        }
        return null;
    }

    public static boolean hugs(BlockState State, Direction Side) {
        VoxelShape Shape = State.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (Shape.isEmpty()) {
            return true;
        }
        return Side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Shape.max(Side.getAxis()) >= 1.0 - Touch : Shape.min(Side.getAxis()) <= Touch;
    }

    public static boolean solid(String Text, Direction Toward) {
        if (Text == null) {
            return false;
        }
        BlockState Backing = Scp1730World.state(Text);
        return Backing.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Toward);
    }

    public static List<String> audit(long Seed) {
        Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
        List<int[]> Spots = new ArrayList<>();
        List<String> Texts = new ArrayList<>();
        Plan.paint(0, 0, Scp1730Plan.Width - 1, Scp1730Plan.Depth - 1, (X, Y, Z, Text) -> {
            int Cut = Text.indexOf('[');
            if (mounted(Cut < 0 ? Text : Text.substring(0, Cut))) {
                Spots.add(new int[]{X, Y, Z});
                Texts.add(Text);
            }
        });
        List<String> Problems = new ArrayList<>();
        for (int Index = 0; Index < Spots.size(); Index++) {
            int[] At = Spots.get(Index);
            String Text = Texts.get(Index);
            int Cut = Text.indexOf('[');
            String Id = Cut < 0 ? Text : Text.substring(0, Cut);
            BlockState State = Scp1730World.state(Text);
            if (State.isAir() || !BuiltInRegistries.BLOCK.getKey(State.getBlock()).toString().equals(Id)) {
                Problems.add("seed " + Seed + " unknown piece " + Text);
                continue;
            }
            Direction Back = backOf(Id, State);
            if (Back == null) {
                Problems.add("seed " + Seed + " no facing on " + Text);
                continue;
            }
            if (!hugs(State, Back)) {
                Problems.add("seed " + Seed + " " + Text + " at " + At[0] + "," + At[1] + "," + At[2] + " is not flush with its back " + Back);
                continue;
            }
            String Behind = Plan.stateAt(At[0] + Back.getStepX(), At[1] + Back.getStepY(), At[2] + Back.getStepZ());
            if (!solid(Behind, Back.getOpposite())) {
                Problems.add("seed " + Seed + " " + Text + " at " + At[0] + "," + At[1] + "," + At[2] + " has no solid backing " + Back + " (" + Behind + ")");
                continue;
            }
            if (Scp1730Builder.wideSign(Text)) {
                Direction Along = Back.getClockWise();
                for (int Side = -1; Side <= 1; Side += 2) {
                    int Sx = At[0] + Along.getStepX() * Side;
                    int Sz = At[2] + Along.getStepZ() * Side;
                    String Beside = Plan.stateAt(Sx, At[1], Sz);
                    String Wall = Plan.stateAt(Sx + Back.getStepX(), At[1], Sz + Back.getStepZ());
                    if (Beside != null || !solid(Wall, Back.getOpposite())) {
                        Problems.add("seed " + Seed + " wide sign " + Text + " at " + At[0] + "," + At[1] + "," + At[2] + " is cut off at its side (" + Beside + " / " + Wall + ")");
                    }
                }
            }
        }
        return Problems;
    }
}
