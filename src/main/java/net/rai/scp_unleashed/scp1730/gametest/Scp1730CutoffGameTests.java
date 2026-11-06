package net.rai.scp_unleashed.scp1730.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.door.DevicePulse;
import net.rai.scp_unleashed.door.DoorButtonBlock;
import net.rai.scp_unleashed.door.DoorGeometry;
import net.rai.scp_unleashed.door.DoorKind;
import net.rai.scp_unleashed.door.DoorLeverBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;
import net.rai.scp_unleashed.scp1730.Scp1730CutoffDoors;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.plan.Scp1730AnomalousCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730CutoffCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Cutoffs;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Layout;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730PlanCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730ReachCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730SectorCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730SupportCheck;
import net.rai.scp_unleashed.scp1730.rooms.gametest.Scp1730RoomsGameTests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp1730CutoffGameTests {
    public static final String Arena = Scp1730GameTests.Arena;
    public static final long SearchStart = 1730L;
    public static final int Search = 60;
    public static final int Reach = 5;
    public static final int Settle = 10;
    public static final int SweepSeeds = 320;
    public static final long SweepStart = 73000L;

    private Scp1730CutoffGameTests() {
    }

    record Sample(Scp1730Plan Plan, Scp1730Cutoffs.Cut Made) {
    }

    record Built(ServerLevel World, Scp1730Cutoffs.Cut Made, BlockPos Base, int[] Middle, int F) {
        BlockPos at(int[] Cell) {
            return Base.offset(Cell[0] - Middle[0], Cell[1] - F, Cell[2] - Middle[2]);
        }

        BlockPos origin() {
            return at(new int[]{Made.X(), Made.Y(), Made.Z()});
        }

        Direction front() {
            return Direction.byName(Scp1730Layout.Facings[Made.Front()]);
        }

        BlockPos control(int Side) {
            return at(Made.control(Side));
        }

        PanelDoorBlockEntity door() {
            return World.getBlockEntity(origin()) instanceof PanelDoorBlockEntity Panel ? Panel : null;
        }

        boolean open() {
            for (int[] Cell : Made.doorCells()) {
                BlockState State = World.getBlockState(at(Cell));
                if (!(State.getBlock() instanceof PanelDoorBlock) || !State.getValue(PanelDoorBlock.Open)) {
                    return false;
                }
            }
            return true;
        }

        boolean shut() {
            for (int[] Cell : Made.doorCells()) {
                BlockState State = World.getBlockState(at(Cell));
                if (!(State.getBlock() instanceof PanelDoorBlock) || State.getValue(PanelDoorBlock.Open)) {
                    return false;
                }
            }
            return true;
        }

        AABB walker(double Lateral) {
            Direction Right = DoorGeometry.rightOf(front());
            BlockPos Hall = at(Made.centre());
            Vec3 Spot = Vec3.atBottomCenterOf(Hall).add(Right.getStepX() * Lateral, 0.02, Right.getStepZ() * Lateral);
            return new AABB(Spot.x - 0.3, Spot.y, Spot.z - 0.3, Spot.x + 0.3, Spot.y + 1.8, Spot.z + 0.3);
        }

        double doorLateral() {
            return (Made.width() - 1) / 2.0 - (Made.lo() + 1);
        }

        boolean passes() {
            return World.noBlockCollision(null, walker(doorLateral()));
        }
    }

    static Sample find(Predicate<Scp1730Cutoffs.Cut> Wanted) {
        for (long Seed = SearchStart; Seed < SearchStart + Search; Seed++) {
            Scp1730Plan Plan = Scp1730Plan.of(Seed);
            for (Scp1730Cutoffs.Cut Each : Scp1730Cutoffs.cuts(Plan)) {
                if (Wanted.test(Each)) {
                    return new Sample(Plan, Each);
                }
            }
        }
        return null;
    }

    static Built build(GameTestHelper Helper, Sample Pick, BlockPos Spot) {
        ServerLevel World = Helper.getLevel();
        Scp1730Cutoffs.Cut Made = Pick.Made();
        int[] Middle = Made.centre();
        int F = Made.Y() - 1;
        BlockPos Base = Helper.absolutePos(Spot);
        List<BlockPos> Later = new ArrayList<>();
        List<BlockState> LaterStates = new ArrayList<>();
        for (int Dy = 0; Dy <= Scp1730Cutoffs.Height + 2; Dy++) {
            for (int Dx = -Reach; Dx <= Reach; Dx++) {
                for (int Dz = -Reach; Dz <= Reach; Dz++) {
                    String Text = Pick.Plan().stateAt(Middle[0] + Dx, F + Dy, Middle[2] + Dz);
                    BlockState State = Text == null ? Blocks.AIR.defaultBlockState() : Scp1730World.state(Text);
                    BlockPos At = Base.offset(Dx, Dy, Dz);
                    if (State.hasBlockEntity()) {
                        Later.add(At);
                        LaterStates.add(State);
                    } else {
                        World.setBlock(At, State, Scp1730World.PaintFlags);
                    }
                }
            }
        }
        for (int Index = 0; Index < Later.size(); Index++) {
            World.setBlock(Later.get(Index), LaterStates.get(Index), Scp1730World.PaintFlags);
        }
        Built Scene = new Built(World, Made, Base, Middle, F);
        Scp1730CutoffDoors.settle(World, Scene.origin(), Made.fixture());
        return Scene;
    }

    static void use(GameTestHelper Helper, Built Scene, BlockPos Pos) {
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        BlockState State = Scene.World().getBlockState(Pos);
        State.useWithoutItem(Scene.World(), User, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false));
    }

    static int travel(Scp1730Cutoffs.Cut Made) {
        return Scp1730Cutoffs.Travel[Made.Kind()] + Settle;
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "s1730cutoffs_lever")
    public static void cutoffLeverOpensAndCloses(GameTestHelper Helper) {
        Sample Pick = find(Each -> Each.Mode() == Scp1730Cutoffs.ModeLevers && !Each.Open());
        Helper.assertTrue(Pick != null, "no closed lever cutoff in " + Search + " seeds");
        Built Scene = build(Helper, Pick, new BlockPos(16, 1, 16));
        int Travel = travel(Scene.Made());
        Helper.runAfterDelay(3, () -> {
            Helper.assertTrue(Scene.shut() && !Scene.passes(), "the closed lever cutoff does not start shut");
            Helper.assertTrue(Scene.World().getBlockState(Scene.control(-1)).getBlock() instanceof DoorLeverBlock, "the back lever is missing");
            Helper.assertTrue(Scene.World().getBlockState(Scene.control(-1)).canSurvive(Scene.World(), Scene.control(-1)), "the back lever has no wall to hang on");
            use(Helper, Scene, Scene.control(-1));
        });
        Helper.runAfterDelay(3 + Travel, () -> {
            Helper.assertTrue(Scene.open() && Scene.door() != null && Scene.door().getEased() >= 0.999f, "the back lever did not open the door");
            Helper.assertTrue(Scene.passes(), "a player does not fit through the opened cutoff");
            use(Helper, Scene, Scene.control(-1));
        });
        Helper.runAfterDelay(3 + Travel * 2, () -> {
            Helper.assertTrue(Scene.shut() && Scene.door().getEased() <= 0.001f && !Scene.passes(), "the back lever did not close the door");
            use(Helper, Scene, Scene.control(1));
        });
        Helper.runAfterDelay(3 + Travel * 3, () -> {
            Helper.assertTrue(Scene.open() && Scene.passes(), "the front lever did not open the door");
            use(Helper, Scene, Scene.control(1));
        });
        Helper.runAfterDelay(3 + Travel * 4, () -> {
            Helper.assertTrue(Scene.shut() && !Scene.passes(), "the front lever did not close the door");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "s1730cutoffs_button")
    public static void cutoffButtonPulsesOpen(GameTestHelper Helper) {
        Sample Pick = find(Each -> Each.Mode() == Scp1730Cutoffs.ModeButtons);
        Helper.assertTrue(Pick != null, "no button cutoff in " + Search + " seeds");
        Built Scene = build(Helper, Pick, new BlockPos(16, 1, 16));
        int Travel = travel(Scene.Made());
        BlockState Button = Scene.World().getBlockState(Scene.control(-1));
        Helper.assertTrue(Button.getBlock() instanceof DoorButtonBlock, "the back button is missing: " + Button);
        int Pulse = DevicePulse.ticksOf(Button);
        Helper.assertTrue(Pulse > Travel, "the button pulse of " + Pulse + " ticks is shorter than the door travel");
        Helper.runAfterDelay(3, () -> {
            Helper.assertTrue(Scene.shut() && !Scene.passes(), "the button cutoff does not start shut");
            use(Helper, Scene, Scene.control(-1));
        });
        Helper.runAfterDelay(3 + Travel, () -> {
            Helper.assertTrue(Scene.open() && Scene.passes(), "the back button did not open the door");
        });
        Helper.runAfterDelay(3 + Pulse + Travel, () -> {
            Helper.assertTrue(Scene.shut() && !Scene.passes(), "the door did not close after the button pulse");
            use(Helper, Scene, Scene.control(1));
        });
        Helper.runAfterDelay(3 + Pulse + Travel * 2, () -> {
            Helper.assertTrue(Scene.open(), "the front button did not open the door");
        });
        Helper.runAfterDelay(3 + Pulse * 2 + Travel * 2, () -> {
            Helper.assertTrue(Scene.shut(), "the door did not close after the front button pulse");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "s1730cutoffs_startOpen")
    public static void cutoffLeverStartsOpen(GameTestHelper Helper) {
        Sample Pick = find(Each -> Each.Mode() == Scp1730Cutoffs.ModeLevers && Each.Open());
        Helper.assertTrue(Pick != null, "no open lever cutoff in " + Search + " seeds");
        Built Scene = build(Helper, Pick, new BlockPos(16, 1, 16));
        int Travel = travel(Scene.Made());
        Helper.assertTrue(Scene.open() && Scene.door() != null && Scene.door().getEased() >= 0.999f, "an open lever cutoff is not open the moment it is built");
        for (int Side = -1; Side <= 1; Side += 2) {
            BlockState Lever = Scene.World().getBlockState(Scene.control(Side));
            Helper.assertTrue(Lever.getBlock() instanceof DoorLeverBlock && Lever.getValue(DoorLeverBlock.Powered), "an open cutoff's lever is not thrown");
        }
        Helper.runAfterDelay(Settle, () -> {
            Helper.assertTrue(Scene.open() && Scene.passes() && Scene.door().isPowered(), "the open cutoff did not stay open on its levers");
            use(Helper, Scene, Scene.control(-1));
        });
        Helper.runAfterDelay(Settle + Travel, () -> {
            Helper.assertTrue(Scene.open(), "one lever closed a door the other lever still holds");
            use(Helper, Scene, Scene.control(1));
        });
        Helper.runAfterDelay(Settle + Travel * 2, () -> {
            Helper.assertTrue(Scene.shut() && !Scene.passes(), "both levers off did not close the door");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "s1730cutoffs_seal")
    public static void cutoffCrossSectionSealed(GameTestHelper Helper) {
        for (int Index = 0; Index < Scp1730Cutoffs.Doors.size(); Index++) {
            Block Listed = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Scp1730Cutoffs.Doors.get(Index)));
            Helper.assertTrue(Listed instanceof PanelDoorBlock, Scp1730Cutoffs.Doors.get(Index) + " is not a mod door");
            DoorKind Kind = ((PanelDoorBlock) Listed).getKind();
            Helper.assertTrue(Kind.getWidth() == Scp1730Cutoffs.Widths[Index] && Kind.getHeight() == Scp1730Cutoffs.Height && Kind.getTravelTicks() == Scp1730Cutoffs.Travel[Index],
                    Kind.getSerializedName() + " does not match the cutoff table");
            Helper.assertTrue(!Kind.isManual() && !Kind.isGlazed(), Kind.getSerializedName() + " can be opened by hand or seen through");
        }
        Sample Wide = find(Each -> !Each.filled() && !Each.Open());
        Sample Narrow = find(Each -> Each.filled() && !Each.Open());
        Helper.assertTrue(Wide != null && Narrow != null, "no closed cutoff of each width");
        List<Built> Scenes = List.of(build(Helper, Wide, new BlockPos(8, 1, 8)), build(Helper, Narrow, new BlockPos(24, 1, 24)));
        Helper.runAfterDelay(3, () -> {
            for (Built Scene : Scenes) {
                sealed(Helper, Scene);
            }
            Helper.succeed();
        });
    }

    static void sealed(GameTestHelper Helper, Built Scene) {
        ServerLevel World = Scene.World();
        Scp1730Cutoffs.Cut Made = Scene.Made();
        String Name = Made.name() + " cutoff";
        Helper.assertTrue(Scene.shut(), Name + " is not shut");
        Direction.Axis Across = DoorGeometry.rightOf(Scene.front()).getAxis();
        for (int[] Cell : Made.doorCells()) {
            BlockPos At = Scene.at(Cell);
            BlockState State = World.getBlockState(At);
            String Gap = gap(State.getShape(World, At), Across);
            Helper.assertTrue(Gap == null, Name + " part at " + At + " can be seen through " + Gap);
            Gap = gap(State.getCollisionShape(World, At), Across);
            Helper.assertTrue(Gap == null, Name + " part at " + At + " can be slipped through " + Gap);
        }
        for (double Lateral = -1.2; Lateral <= 1.2001; Lateral += 0.1) {
            Helper.assertFalse(World.noBlockCollision(null, Scene.walker(Lateral)), Name + " lets a player through at lateral " + String.format("%.1f", Lateral));
        }
        List<int[]> Walls = new ArrayList<>();
        for (int Row = 0; Row < Scp1730Cutoffs.Height; Row++) {
            Walls.add(Made.cell(Made.lo() - 1, Row, 0));
            Walls.add(Made.cell(Made.lo() + Scp1730Cutoffs.HallWidth, Row, 0));
            if (Made.filled()) {
                Walls.add(Made.cell(Made.filler(), Row, 0));
            }
        }
        for (int Column = Made.lo(); Column < Made.lo() + Scp1730Cutoffs.HallWidth; Column++) {
            Walls.add(Made.cell(Column, Scp1730Cutoffs.Height, 0));
            Walls.add(Made.cell(Column, -1, 0));
        }
        for (int[] Cell : Walls) {
            BlockPos At = Scene.at(Cell);
            BlockState State = World.getBlockState(At);
            Helper.assertTrue(State.isCollisionShapeFullBlock(World, At) && State.isSolidRender(World, At), Name + " frame cell at " + At + " is not solid wall: " + State);
        }
        for (int Side = -1; Side <= 1; Side += 2) {
            BlockState Control = World.getBlockState(Scene.control(Side));
            Helper.assertTrue(Control.getBlock() instanceof DoorLeverBlock || Control.getBlock() instanceof DoorButtonBlock, Name + " has no control on side " + Side);
            Helper.assertTrue(Control.canSurvive(World, Scene.control(Side)), Name + " control on side " + Side + " is not backed by a wall");
        }
    }

    static String gap(VoxelShape Shape, Direction.Axis Across) {
        List<AABB> Boxes = Shape.toAabbs();
        int Open = 0;
        for (int A = 0; A < 16; A++) {
            for (int B = 0; B < 16; B++) {
                double U = (A + 0.5) / 16.0;
                double V = (B + 0.5) / 16.0;
                boolean Covered = false;
                for (AABB Box : Boxes) {
                    double Low = Across == Direction.Axis.X ? Box.minX : Box.minZ;
                    double High = Across == Direction.Axis.X ? Box.maxX : Box.maxZ;
                    if (U > Low && U < High && V > Box.minY && V < Box.maxY) {
                        Covered = true;
                        break;
                    }
                }
                Open += Covered ? 0 : 1;
            }
        }
        return Open == 0 ? null : "in " + Open + " of 256 pixels";
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "s1730cutoffs_paint")
    public static void cutoffPaintedFromRun(GameTestHelper Helper) {
        ServerLevel Facility = Scp1730GameTests.facility(Helper);
        Helper.assertTrue(Facility != null, "the facility level is missing");
        int Run = -1;
        Scp1730Cutoffs.Cut Opened = null;
        Scp1730Cutoffs.Cut Closed = null;
        for (int Each = 300; Each < 340 && Run < 0; Each++) {
            Scp1730Cutoffs.Cut FoundOpen = null;
            Scp1730Cutoffs.Cut FoundClosed = null;
            for (Scp1730Cutoffs.Cut Made : Scp1730Cutoffs.cuts(Scp1730World.plan(Each))) {
                if (Made.Mode() == Scp1730Cutoffs.ModeLevers && Made.Open() && FoundOpen == null) {
                    FoundOpen = Made;
                }
                if (Made.levers() && !Made.Open() && FoundClosed == null) {
                    FoundClosed = Made;
                }
            }
            if (FoundOpen != null && FoundClosed != null) {
                Run = Each;
                Opened = FoundOpen;
                Closed = FoundClosed;
            }
        }
        Helper.assertTrue(Run >= 0, "no run holds both an open and a closed lever cutoff");
        int Chosen = Run;
        List<int[]> Forced = new ArrayList<>();
        Set<Long> Chunks = new HashSet<>();
        for (Scp1730Cutoffs.Cut Made : List.of(Opened, Closed)) {
            BlockPos Middle = Scp1730World.world(Chosen, Made.X(), Made.Y(), Made.Z());
            for (int Dx = -8; Dx <= 8; Dx += 8) {
                for (int Dz = -8; Dz <= 8; Dz += 8) {
                    int Cx = (Middle.getX() + Dx) >> 4;
                    int Cz = (Middle.getZ() + Dz) >> 4;
                    if (Chunks.add(((long) Cx << 32) ^ (Cz & 0xFFFFFFFFL)) && Facility.setChunkForced(Cx, Cz, true)) {
                        Forced.add(new int[]{Cx, Cz});
                    }
                }
            }
            Scp1730World.paint(Facility, Chosen, Middle.getX() - 6, Middle.getZ() - 6, Middle.getX() + 6, Middle.getZ() + 6);
        }
        Scp1730Cutoffs.Cut Open = Opened;
        Scp1730Cutoffs.Cut Shut = Closed;
        BlockPos FirstAt = Scp1730World.world(Chosen, Open.X(), Open.Y(), Open.Z());
        BlockPos SecondAt = Scp1730World.world(Chosen, Shut.X(), Shut.Y(), Shut.Z());
        PanelDoorBlockEntity First = Facility.getBlockEntity(FirstAt) instanceof PanelDoorBlockEntity Door ? Door : null;
        PanelDoorBlockEntity Second = Facility.getBlockEntity(SecondAt) instanceof PanelDoorBlockEntity Door ? Door : null;
        Helper.assertTrue(First != null && Second != null, "a painted cutoff has no door entity");
        Helper.assertTrue(First.getEased() >= 0.999f && First.isPowered(), "the painted open cutoff is not open");
        Helper.assertTrue(Second.getEased() <= 0.001f && !Second.isPowered(), "the painted closed cutoff is not shut");
        int[] Lever = Shut.control(Shut.lever(-1) ? -1 : 1);
        BlockPos LeverPos = Scp1730World.world(Chosen, Lever[0], Lever[1], Lever[2]);
        BlockState LeverState = Facility.getBlockState(LeverPos);
        Helper.assertTrue(LeverState.getBlock() instanceof DoorLeverBlock && !LeverState.getValue(DoorLeverBlock.Powered), "the painted closed cutoff has no idle lever: " + LeverState);
        Scp1730State Power = Scp1730State.get(Facility.getServer());
        for (int Zone = 0; Zone < 4; Zone++) {
            Power.setPower(Zone, false);
        }
        ((DoorLeverBlock) LeverState.getBlock()).pull(LeverState, Facility, LeverPos, null);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(First.getEased() >= 0.999f && First.isPowered() && Facility.getBlockState(FirstAt).getValue(PanelDoorBlock.Open),
                    "the painted open cutoff did not stay open on its levers");
            Helper.assertTrue(Second.getEased() >= 0.999f && Facility.getBlockState(SecondAt).getValue(PanelDoorBlock.Open), "the painted cutoff's lever did not open it: powered "
                    + Second.isPowered() + ", eased " + Second.getEased());
            for (int[] Each : Forced) {
                Facility.setChunkForced(Each[0], Each[1], false);
            }
            for (int Zone = 0; Zone < 4; Zone++) {
                Power.setPower(Zone, true);
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 4000, batch = "s1730cutoffs_sweep")
    public static void cutoffSweep(GameTestHelper Helper) {
        long[] Next = {SweepStart};
        long End = SweepStart + SweepSeeds;
        List<String> Problems = new CopyOnWriteArrayList<>();
        Set<String> Seen = ConcurrentHashMap.newKeySet();
        int[] Bare = new int[4];
        int[] Count = new int[4];
        Helper.onEachTick(() -> {
            for (int Step = 0; Step < 4 && Next[0] < End; Step++) {
                long Seed = Next[0]++;
                Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
                for (String Each : Scp1730CutoffCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " cutoff: " + Each);
                }
                for (String Each : Scp1730PlanCheck.reach(Plan)) {
                    Problems.add("seed " + Seed + " plan: " + Each);
                }
                for (String Each : Scp1730SupportCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " support: " + Each);
                }
                for (String Each : Scp1730AnomalousCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " anomalous: " + Each);
                }
                for (String Each : Scp1730ReachCheck.check(Plan, Scp1730RoomsGameTests.Collision)) {
                    Problems.add("seed " + Seed + " reach: " + Each);
                }
                for (String Each : Scp1730SectorCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " sector: " + Each);
                }
                int[] Here = new int[4];
                for (Scp1730Cutoffs.Cut Each : Scp1730Cutoffs.cuts(Plan)) {
                    Here[Each.Zone()]++;
                    Seen.add("mode " + Each.Mode());
                    Seen.add("open " + Each.Open());
                    Seen.add("door " + Each.name());
                    Seen.add("width " + Each.width());
                    for (int Side = -1; Side <= 1; Side += 2) {
                        String Text = Each.controlState(Side);
                        BlockState State = Scp1730World.state(Text);
                        if (State.isAir() || !(State.getBlock() instanceof DoorLeverBlock || State.getBlock() instanceof DoorButtonBlock)) {
                            Problems.add("seed " + Seed + " control state does not parse: " + Text);
                        }
                    }
                    BlockState Door = Scp1730World.state(Each.doorState(0, 0));
                    if (!(Door.getBlock() instanceof PanelDoorBlock)) {
                        Problems.add("seed " + Seed + " door state does not parse: " + Each.doorState(0, 0));
                    }
                }
                for (int Zone = 0; Zone < 4; Zone++) {
                    Count[Zone] += Here[Zone];
                    Bare[Zone] += Here[Zone] == 0 ? 1 : 0;
                }
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Next[0] >= End, "sweep still running at seed " + Next[0]);
            Helper.assertTrue(Problems.isEmpty(), Problems.size() + " problems over " + SweepSeeds + " seeds, e.g. " + Problems.subList(0, Math.min(4, Problems.size())));
            for (int Zone = 0; Zone < 3; Zone++) {
                Helper.assertTrue(Bare[Zone] * 20 <= SweepSeeds, "zone " + Zone + " had no cutoff in " + Bare[Zone] + " of " + SweepSeeds + " seeds");
            }
            Helper.assertTrue(Count[3] > 0, "the deep zone never got a cutoff");
            for (String Wanted : List.of("mode 0", "mode 1", "mode 2", "mode 3", "open true", "open false", "width 2", "width 3")) {
                Helper.assertTrue(Seen.contains(Wanted), "no cutoff with " + Wanted + " in " + SweepSeeds + " seeds");
            }
            com.mojang.logging.LogUtils.getLogger().info("s1730cutoffs sweep: {} seeds, cutoffs per zone {} {} {} {}, without {} {} {} {}", SweepSeeds, Count[0], Count[1], Count[2],
                    Count[3], Bare[0], Bare[1], Bare[2], Bare[3]);
        });
    }
}
