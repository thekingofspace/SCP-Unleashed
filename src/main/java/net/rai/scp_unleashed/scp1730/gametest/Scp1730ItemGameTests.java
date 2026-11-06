package net.rai.scp_unleashed.scp1730.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.scp1730.guardian.Scp1730ContainedGuardian;
import net.rai.scp_unleashed.scp1730.guardian.Scp1730GuardianRegistry;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Blocks;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730PuzzlePlan;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp1730ItemGameTests {
    public static final String Arena = Scp1730GameTests.Arena;
    public static final int PlanRuns = 3;
    public static final int Spacing = 3;
    public static final int GridFirst = 2;
    public static final int GridLast = 29;
    public static final int PadY = 2;
    public static final int Settle = 100;
    public static final int Watch = 40;
    public static final double DropHeight = 1.0;
    public static final double Footprint = 0.125;
    public static final double TolerancePx = 1.5;
    public static final double PassablePx = 3.0;
    public static final double Rest = 0.02;
    public static final double Still = 0.002;
    public static final double[][] Probes = {{0.5, 0.5}, {0.25, 0.25}, {0.75, 0.25}, {0.25, 0.75}, {0.75, 0.75}};
    public static final ItemStack[] Samples = {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.STICK), new ItemStack(Items.APPLE), new ItemStack(Items.PAPER),
            new ItemStack(Items.FLINT)};
    public static final BlockPos BossSpot = new BlockPos(16, 2, 24);
    public static final int DriftWatch = 80;
    public static final int DriftSettle = 20;
    private static final Logger Log = LogUtils.getLogger();
    private static final AtomicInteger DropNames = new AtomicInteger();

    private Scp1730ItemGameTests() {
    }

    private record Probe(ItemEntity Drop, double CentreX, double CentreZ, double Expected, double CollisionPx, Scp1730ModelTops.Top Model, double OutlinePx, List<Vec3> Track) {
    }

    private record Pad(BlockState State, BlockPos At, List<Probe> Drops, boolean Owned) {
    }

    public static String describe(BlockState State) {
        return State.toString().replace("Block{", "").replace("}", "");
    }

    public static boolean wanted(BlockState State) {
        if (State.isAir() || !State.getFluidState().isEmpty()) {
            return false;
        }
        ResourceLocation Key = BuiltInRegistries.BLOCK.getKey(State.getBlock());
        return Scp_unleashed.ModId.equals(Key.getNamespace());
    }

    public static boolean facility(ResourceLocation Key) {
        return Scp_unleashed.ModId.equals(Key.getNamespace()) && (Key.getPath().startsWith("scp1730_") || Key.getPath().equals("reality_folder"));
    }

    public static boolean wallMounted(BlockState State) {
        String Key = BuiltInRegistries.BLOCK.getKey(State.getBlock()).toString();
        return Key.equals(Scp1730Blocks.Control) || Scp1730PuzzlePlan.WallDevices.contains(Key);
    }

    public static List<BlockState> padStates() {
        Map<String, BlockState> Chosen = new LinkedHashMap<>();
        Set<Block> Seen = new HashSet<>();
        for (int Run = 0; Run < PlanRuns; Run++) {
            Scp1730Plan Plan = Scp1730World.plan(Run);
            for (int X = 0; X < Scp1730Plan.Width; X++) {
                for (int Z = 0; Z < Scp1730Plan.Depth; Z++) {
                    for (int Y = 0; Y < 255; Y++) {
                        String Text = Plan.stateAt(X, Y, Z);
                        if (Text == null) {
                            continue;
                        }
                        String Up = Plan.stateAt(X, Y + 1, Z);
                        if (Up != null && !Scp1730Blocks.passable(Up)) {
                            continue;
                        }
                        BlockState Found = Scp1730World.state(Text);
                        Seen.add(Found.getBlock());
                        offer(Chosen, Found);
                    }
                }
            }
        }
        for (Block Each : BuiltInRegistries.BLOCK) {
            if (facility(BuiltInRegistries.BLOCK.getKey(Each)) && !Seen.contains(Each)) {
                for (BlockState Variant : Each.getStateDefinition().getPossibleStates()) {
                    offer(Chosen, Variant);
                }
            }
        }
        return new ArrayList<>(Chosen.values());
    }

    private static void offer(Map<String, BlockState> Chosen, BlockState State) {
        if (!wanted(State)) {
            return;
        }
        if (State.hasProperty(BlockStateProperties.WATERLOGGED) && State.getValue(BlockStateProperties.WATERLOGGED)) {
            return;
        }
        String Key;
        try {
            Key = BuiltInRegistries.BLOCK.getKey(State.getBlock()) + "|" + State.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()).toAabbs()
                    + "|" + State.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()).toAabbs() + "|" + Scp1730ModelTops.under(State, 0.5, 0.5);
        } catch (RuntimeException Problem) {
            Key = State.toString();
        }
        Chosen.putIfAbsent(Key, State);
    }

    public static double topPx(VoxelShape Shape, double CentreX, double CentreZ) {
        double Top = Double.NaN;
        for (AABB Box : Shape.toAabbs()) {
            if (Box.minX < CentreX + Footprint - 1.0e-6 && Box.maxX > CentreX - Footprint + 1.0e-6 && Box.minZ < CentreZ + Footprint - 1.0e-6
                    && Box.maxZ > CentreZ - Footprint + 1.0e-6) {
                Top = Double.isNaN(Top) ? Box.maxY * 16.0 : Math.max(Top, Box.maxY * 16.0);
            }
        }
        return Top;
    }

    public static ItemStack unique(ItemStack Stack) {
        ItemStack Made = Stack.copy();
        Made.set(DataComponents.CUSTOM_NAME, Component.literal("s1730items " + DropNames.incrementAndGet()));
        return Made;
    }

    public static ItemEntity drop(ServerLevel World, double X, double Y, double Z, ItemStack Stack) {
        ItemEntity Made = new ItemEntity(World, X, Y, Z, unique(Stack), 0.0, 0.0, 0.0);
        Made.setDefaultPickUpDelay();
        World.addFreshEntity(Made);
        return Made;
    }

    public static AABB reach(BlockPos Beside) {
        return EntityType.PLAYER.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Beside)).inflate(1.0, 0.5, 1.0);
    }

    private static double span(List<Vec3> Track, Vec3 Final) {
        double Most = 0.0;
        for (Vec3 Each : Track) {
            Most = Math.max(Most, Each.distanceTo(Final));
        }
        return Most;
    }

    private static final class Bench {
        private final GameTestHelper Helper;
        private final ServerLevel World;
        private final List<BlockState> States;
        private final List<Pad> Round = new ArrayList<>();
        private final List<String> Failures = new ArrayList<>();
        private final List<String> Notes = new ArrayList<>();
        private final Player Picker;
        private int Next;
        private int Clock;
        private int Checked;

        private Bench(GameTestHelper Helper, List<BlockState> States) {
            this.Helper = Helper;
            this.World = Helper.getLevel();
            this.States = States;
            this.Picker = Helper.makeMockPlayer(GameType.CREATIVE);
        }

        private void tick() {
            if (Round.isEmpty()) {
                if (Next >= States.size()) {
                    finish();
                    return;
                }
                layout();
                Clock = 0;
                return;
            }
            Clock++;
            if (Clock >= Settle - Watch) {
                for (Pad Each : Round) {
                    for (Probe Shot : Each.Drops()) {
                        Shot.Track().add(Shot.Drop().position());
                    }
                }
            }
            if (Clock >= Settle) {
                for (Pad Each : Round) {
                    judge(Each);
                }
                clear();
            }
        }

        private void layout() {
            for (int X = GridFirst; X <= GridLast && Next < States.size(); X += Spacing) {
                for (int Z = GridFirst; Z <= GridLast && Next < States.size(); Z += Spacing) {
                    BlockState State = States.get(Next++);
                    BlockPos At = Helper.absolutePos(new BlockPos(X, PadY, Z));
                    World.setBlock(At.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    World.setBlock(At, State, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    BlockState Placed = World.getBlockState(At);
                    ItemEntity Sample = new ItemEntity(World, 0.0, 0.0, 0.0, Samples[0].copy(), 0.0, 0.0, 0.0);
                    VoxelShape Collision = Placed.getCollisionShape(World, At, CollisionContext.of(Sample));
                    VoxelShape Outline = Placed.getShape(World, At, CollisionContext.empty());
                    double Highest = Math.max(Collision.isEmpty() ? 0.0 : Collision.max(Direction.Axis.Y),
                            Outline.isEmpty() ? 0.0 : Outline.max(Direction.Axis.Y));
                    Scp1730ModelTops.Top Whole = Scp1730ModelTops.under(Placed, 0.5, 0.5);
                    if (Whole.Known() && !Double.isNaN(Whole.Peak())) {
                        Highest = Math.max(Highest, Whole.Peak() / 16.0);
                    }
                    List<Probe> Drops = new ArrayList<>();
                    for (int Index = 0; Index < Probes.length; Index++) {
                        double Cx = Probes[Index][0];
                        double Cz = Probes[Index][1];
                        double Under = topPx(Collision, Cx, Cz);
                        double Expected = At.getY() + (Double.isNaN(Under) ? 0.0 : Under / 16.0);
                        ItemEntity Made = drop(World, At.getX() + Cx, At.getY() + Highest + DropHeight, At.getZ() + Cz, Samples[Index]);
                        Drops.add(new Probe(Made, Cx, Cz, Expected, Under, Scp1730ModelTops.under(Placed, Cx, Cz), topPx(Outline, Cx, Cz), new ArrayList<>()));
                    }
                    Round.add(new Pad(Placed, At, Drops, facility(BuiltInRegistries.BLOCK.getKey(Placed.getBlock())) && !wallMounted(Placed)));
                    if (Placed != State) {
                        Failures.add(describe(State) + ": placed as " + describe(Placed));
                    }
                }
            }
        }

        private void judge(Pad Each) {
            Checked++;
            String Name = describe(Each.State());
            List<String> Problems = new ArrayList<>();
            for (int Index = 0; Index < Each.Drops().size(); Index++) {
                Probe Shot = Each.Drops().get(Index);
                ItemEntity Dropped = Shot.Drop();
                String Where = Index == 0 ? "centre" : "corner " + Index;
                if (Dropped.isRemoved()) {
                    Problems.add(Where + " item vanished (" + Dropped.getRemovalReason() + ")");
                    continue;
                }
                Vec3 Final = Dropped.position();
                if (Final.y < Each.At().getY() - 0.01) {
                    Problems.add(Where + " item fell through to y " + fmt(Final.y - Each.At().getY()));
                    continue;
                }
                double Moved = span(Shot.Track(), Final);
                if (Moved > Still || !Dropped.onGround()) {
                    Problems.add(Where + " item keeps moving (span " + fmt(Moved) + ", ground " + Dropped.onGround() + ")");
                }
                if (Math.abs(Final.y - Shot.Expected()) > Rest) {
                    Problems.add(Where + " item rests at " + fmt((Final.y - Each.At().getY()) * 16.0) + " px, collision top is " + fmt((Shot.Expected() - Each.At().getY()) * 16.0)
                            + " px, drift " + fmt(Final.x - Each.At().getX() - Shot.CentreX()) + "," + fmt(Final.z - Each.At().getZ() - Shot.CentreZ()));
                }
                if (!World.noCollision(Dropped, Dropped.getBoundingBox().deflate(1.0e-4))) {
                    Problems.add(Where + " item is stuck inside the block");
                }
                if (!reach(Each.At().east()).intersects(Dropped.getBoundingBox())) {
                    Problems.add(Where + " item is out of reach of a player standing beside the block");
                }
                if (Index == 0) {
                    Picker.getInventory().clearContent();
                    Dropped.setNoPickUpDelay();
                    Dropped.playerTouch(Picker);
                    if (!Dropped.isRemoved()) {
                        Problems.add(Where + " item could not be picked up");
                    }
                    Picker.getInventory().clearContent();
                }
            }
            List<String> Looks = new ArrayList<>();
            for (int Index = 0; Index < Each.Drops().size(); Index++) {
                if (Index > 0 && !Each.Owned()) {
                    continue;
                }
                String Visual = visual(Each.Drops().get(Index));
                if (Visual != null) {
                    Looks.add((Index == 0 ? "centre" : "corner " + Index) + " " + Visual);
                }
            }
            if (Each.Owned()) {
                Problems.addAll(Looks);
            } else if (!Looks.isEmpty()) {
                Notes.add(Name + ": " + String.join("; ", Looks));
            }
            Probe Middle = Each.Drops().get(0);
            Log.info("S1730ITEMS pad {} collision {} model {} outline {} -> {}{}", Name, fmt(Middle.CollisionPx()), Middle.Model(), fmt(Middle.OutlinePx()),
                    Problems.isEmpty() ? "ok" : String.join("; ", Problems), Each.Owned() || Looks.isEmpty() ? "" : " (note: " + String.join("; ", Looks) + ")");
            if (!Problems.isEmpty()) {
                Failures.add(Name + ": " + String.join("; ", Problems));
            }
        }

        private String visual(Probe Shot) {
            Scp1730ModelTops.Top Model = Shot.Model();
            double Solid = Shot.CollisionPx();
            if (Double.isNaN(Solid)) {
                return null;
            }
            double Seen = Model.Known() ? (Double.isNaN(Model.Value()) ? 0.0 : Model.Value()) : Shot.OutlinePx();
            if (Double.isNaN(Seen)) {
                return Solid > PassablePx ? "items rest " + fmt(Solid) + " px up on an invisible surface" : null;
            }
            if (Solid - Seen > TolerancePx) {
                return "items float " + fmt(Solid - Seen) + " px above the model (" + fmt(Seen) + " px)";
            }
            if (Seen - Solid > TolerancePx) {
                return "items sink " + fmt(Seen - Solid) + " px into the model (" + fmt(Seen) + " px)";
            }
            return null;
        }

        private void clear() {
            for (Pad Each : Round) {
                for (Probe Shot : Each.Drops()) {
                    if (!Shot.Drop().isRemoved()) {
                        Shot.Drop().discard();
                    }
                }
                World.setBlock(Each.At(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            Round.clear();
            Helper.killAllEntitiesOfClass(ItemEntity.class);
        }

        private void finish() {
            Log.info("S1730ITEMS checked {} pads, {} failing, {} shared-furniture notes", Checked, Failures.size(), Notes.size());
            for (String Line : Notes) {
                Log.info("S1730ITEMS NOTE {}", Line);
            }
            if (Failures.isEmpty()) {
                Helper.succeed();
                return;
            }
            for (String Line : Failures) {
                Log.info("S1730ITEMS FAIL {}", Line);
            }
            Helper.fail(Failures.size() + " of " + Checked + " Site-13 surfaces mishandle dropped items, first: " + Failures.get(0));
        }
    }

    public static String fmt(double Value) {
        return Double.isNaN(Value) ? "none" : String.format(Locale.ROOT, "%.3f", Value);
    }

    @GameTest(template = Arena, timeoutTicks = 6000, batch = "s1730items_surfaces")
    public static void s1730itemsSurfaces(GameTestHelper Helper) {
        List<BlockState> States = padStates();
        Log.info("S1730ITEMS testing {} distinct Site-13 surfaces", States.size());
        Helper.assertTrue(States.size() > 40, "the Site-13 surface census found only " + States.size() + " states");
        Bench Run = new Bench(Helper, States);
        Helper.onEachTick(Run::tick);
    }

    private static List<ItemEntity> scatter(GameTestHelper Helper, double Y) {
        List<ItemEntity> Made = new ArrayList<>();
        double[][] Spots = {{16.5, 13.5}, {13.5, 15.5}, {19.5, 15.5}, {9.5, 22.5}, {23.5, 22.5}, {9.5, 26.5}, {23.5, 26.5}, {16.5, 30.5}};
        for (int Index = 0; Index < Spots.length; Index++) {
            Vec3 At = Helper.absoluteVec(new Vec3(Spots[Index][0], Y, Spots[Index][1]));
            Made.add(drop(Helper.getLevel(), At.x, At.y, At.z, Samples[Index % Samples.length]));
        }
        return Made;
    }

    private static void watchDrift(GameTestHelper Helper, Scp1730Guardian Boss, String What, boolean Lightens) {
        List<ItemEntity> Drops = scatter(Helper, 2.4);
        ServerPlayer User = Scp1730GameTests.player(Helper, Helper.getLevel(), Helper.absoluteVec(new Vec3(16.5, 2.0, 17.5)), "s1730items_floater");
        User.setGameMode(GameType.CREATIVE);
        double[] Low = new double[Drops.size()];
        double[] High = new double[Drops.size()];
        boolean[] Lifted = new boolean[Drops.size()];
        int[] Clock = {0};
        Helper.onEachTick(() -> {
            if (Clock[0] == 0 && !Drops.stream().allMatch(Each -> Each.isRemoved() || Each.onGround() && Each.getDeltaMovement().y <= 0.0)) {
                return;
            }
            Clock[0]++;
            if (Clock[0] < DriftSettle) {
                return;
            }
            for (int Index = 0; Index < Drops.size(); Index++) {
                ItemEntity Each = Drops.get(Index);
                double Y = Each.getY();
                if (Clock[0] == DriftSettle) {
                    Low[Index] = Y;
                    High[Index] = Y;
                }
                Low[Index] = Math.min(Low[Index], Y);
                High[Index] = Math.max(High[Index], Y);
                if (Each.getDeltaMovement().y > 1.0e-6) {
                    Lifted[Index] = true;
                }
            }
        });
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Clock[0] >= DriftSettle + DriftWatch, "still watching")).thenExecute(() -> {
            try {
                Helper.assertTrue(Boss.isAlive() && !Boss.anchored(), What + " was not running during the watch");
                AttributeInstance Gravity = User.getAttribute(Attributes.GRAVITY);
                Helper.assertTrue(!Lightens || Gravity != null && Gravity.hasModifier(Scp1730Guardian.GravityId), What + " no longer lightens a nearby player");
                for (int Index = 0; Index < Drops.size(); Index++) {
                    ItemEntity Each = Drops.get(Index);
                    Helper.assertTrue(Each.isAlive(), "an item near " + What + " vanished");
                    Helper.assertTrue(Each.onGround(), "an item near " + What + " is off the ground at y " + fmt(Each.getY() - Helper.absolutePos(BlockPos.ZERO).getY()));
                    Helper.assertTrue(!Lifted[Index] && High[Index] - Low[Index] < Still, "an item near " + What + " drifts: rose " + fmt(High[Index] - Low[Index])
                            + " blocks, lifted " + Lifted[Index]);
                    double Height = Each.getY() - Helper.absolutePos(BlockPos.ZERO).getY();
                    Helper.assertTrue(Math.abs(Height - 2.0) < Rest, "an item near " + What + " is not on the floor: y " + fmt(Height));
                }
            } finally {
                Boss.discard();
                Scp1730GameTests.dismiss(User);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "s1730items_guardian")
    public static void s1730itemsGuardianDrift(GameTestHelper Helper) {
        Scp1730Guardian Boss = Helper.spawn(Scp1730Registry.GuardianType.get(), BossSpot);
        watchDrift(Helper, Boss, "Malidramagiuan", true);
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "s1730items_contained")
    public static void s1730itemsContainedDrift(GameTestHelper Helper) {
        Scp1730ContainedGuardian Boss = Helper.spawn(Scp1730GuardianRegistry.ContainedType.get(), BossSpot);
        CompoundTag Saved = new CompoundTag();
        Boss.saveWithoutId(Saved);
        Saved.putBoolean(Scp1730ContainedGuardian.FreeKey, true);
        Boss.load(Saved);
        Helper.assertTrue(Boss.free(), "the contained Malidramagiuan did not start roaming");
        watchDrift(Helper, Boss, "the roaming contained Malidramagiuan", false);
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "s1730items_level")
    public static void s1730itemsLevel(GameTestHelper Helper) {
        ServerPlayer User = Scp1730GameTests.enter(Helper, "s1730items_dropper");
        ServerLevel Facility = Scp1730GameTests.facility(Helper);
        Helper.assertTrue(User.level() == Facility, "the dropper did not reach Site-13");
        Scp1730State State = Scp1730State.get(Helper.getLevel().getServer());
        int Run = State.run();
        int ChunkX = User.blockPosition().getX() >> 4;
        int ChunkZ = User.blockPosition().getZ() >> 4;
        List<long[]> Forced = new ArrayList<>();
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                if (Facility.setChunkForced(ChunkX + Dx, ChunkZ + Dz, true)) {
                    Forced.add(new long[]{ChunkX + Dx, ChunkZ + Dz});
                }
            }
        }
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Scp1730GameTests.ticking(Facility, User.blockPosition(), 2), "the arrival area is still loading"))
                .thenExecute(() -> {
                    List<ItemEntity> Situ = new ArrayList<>();
                    List<Double> Expected = new ArrayList<>();
                    List<String> Named = new ArrayList<>();
                    Map<String, Boolean> Seen = new LinkedHashMap<>();
                    Scp1730Plan Plan = Scp1730World.plan(Run);
                    int Lx = Scp1730World.localX(Run, User.getX());
                    int Lz = Scp1730World.localZ(User.getZ());
                    int Ly = (int) Math.floor(User.getY());
                    int Zone = Plan.zoneAt(Lx, Ly, Lz);
                    for (int X = Math.max(0, Lx - 28); X <= Math.min(Scp1730Plan.Width - 1, Lx + 28); X++) {
                        for (int Z = Math.max(0, Lz - 28); Z <= Math.min(Scp1730Plan.Depth - 1, Lz + 28); Z++) {
                            for (int Y = Math.max(1, Ly - 8); Y <= Ly + 8; Y++) {
                                if (Plan.zoneAt(X, Y, Z) != Zone) {
                                    continue;
                                }
                                BlockPos At = Scp1730World.world(Run, X, Y, Z);
                                BlockState Here = Facility.getBlockState(At);
                                if (!wanted(Here) || !Facility.getBlockState(At.above()).getCollisionShape(Facility, At.above()).isEmpty()
                                        || !Facility.getBlockState(At.above(2)).getCollisionShape(Facility, At.above(2)).isEmpty() || !Facility.getFluidState(At.above()).isEmpty()) {
                                    continue;
                                }
                                if (Seen.size() >= 160 || Seen.putIfAbsent(describe(Here), true) != null) {
                                    continue;
                                }
                                ItemEntity Sample = new ItemEntity(Facility, 0.0, 0.0, 0.0, Samples[0].copy(), 0.0, 0.0, 0.0);
                                VoxelShape Collision = Here.getCollisionShape(Facility, At, CollisionContext.of(Sample));
                                double Under = topPx(Collision, 0.5, 0.5);
                                double Rests = At.getY() + (Double.isNaN(Under) ? 0.0 : Under / 16.0);
                                if (Double.isNaN(Under)) {
                                    BlockState Below = Facility.getBlockState(At.below());
                                    double Lower = topPx(Below.getCollisionShape(Facility, At.below(), CollisionContext.of(Sample)), 0.5, 0.5);
                                    if (Double.isNaN(Lower)) {
                                        continue;
                                    }
                                    Rests = At.getY() - 1.0 + Lower / 16.0;
                                }
                                Situ.add(drop(Facility, At.getX() + 0.5, Rests + DropHeight, At.getZ() + 0.5, Samples[Situ.size() % Samples.length]));
                                Expected.add(Rests);
                                Named.add(describe(Here) + " at " + At.toShortString());
                            }
                        }
                    }
                    Helper.assertTrue(Situ.size() >= 10, "found only " + Situ.size() + " Site-13 surfaces near the arrival");
                    Log.info("S1730ITEMS dropped {} items on Site-13 surfaces in situ", Situ.size());
                    ItemEntity Tossed = User.drop(unique(new ItemStack(Items.IRON_INGOT)), false, true);
                    Helper.assertTrue(Tossed != null, "the dropper could not toss an item");
                    int Started = Tossed.getAge();
                    Helper.runAfterDelay(Settle, () -> {
                        List<String> Problems = new ArrayList<>();
                        try {
                            Helper.assertTrue(Tossed.isAlive(), "a tossed item vanished in Site-13 (" + Tossed.getRemovalReason() + ")");
                            Helper.assertTrue(Tossed.level() == Facility, "a tossed item left Site-13");
                            Helper.assertTrue(Tossed.getAge() - Started >= Settle - 2, "a tossed item aged only " + (Tossed.getAge() - Started) + " in " + Settle + " ticks");
                            Helper.assertTrue(Tossed.onGround(), "a tossed item never landed in Site-13");
                            Helper.assertTrue(Tossed.position().distanceTo(User.position()) < 6.0, "a tossed item travelled " + fmt(Tossed.position().distanceTo(User.position())));
                            for (int Index = 0; Index < Situ.size(); Index++) {
                                ItemEntity Each = Situ.get(Index);
                                if (!Each.isAlive()) {
                                    Problems.add(Named.get(Index) + ": vanished (" + Each.getRemovalReason() + ")");
                                } else if (Math.abs(Each.getY() - Expected.get(Index)) > Rest || !Each.onGround()) {
                                    Problems.add(Named.get(Index) + ": rests " + fmt((Each.getY() - Expected.get(Index)) * 16.0) + " px off its surface, ground " + Each.onGround());
                                }
                            }
                            for (String Line : Problems) {
                                Log.info("S1730ITEMS SITU {}", Line);
                            }
                            Helper.assertTrue(Problems.isEmpty(), Problems.size() + " of " + Situ.size() + " in-situ drops misbehave, first: " + (Problems.isEmpty() ? "" : Problems.get(0)));
                            CompoundTag Saved = new CompoundTag();
                            Tossed.saveWithoutId(Saved);
                            Saved.putShort("Age", (short) 5990);
                            Tossed.load(Saved);
                        } catch (RuntimeException Problem) {
                            cleanup(Situ, Tossed, Facility, Forced, User);
                            throw Problem;
                        }
                        Helper.runAfterDelay(6, () -> {
                            if (!Tossed.isAlive()) {
                                cleanup(Situ, Tossed, Facility, Forced, User);
                                Helper.fail("a tossed item despawned early in Site-13");
                            }
                        });
                        Helper.runAfterDelay(16, () -> {
                            boolean Gone = Tossed.isRemoved() && Tossed.getRemovalReason() == Entity.RemovalReason.DISCARDED;
                            cleanup(Situ, Tossed, Facility, Forced, User);
                            Helper.assertTrue(Gone, "a tossed item outlived its despawn time in Site-13");
                            Helper.succeed();
                        });
                });
                });
    }

    private static void cleanup(List<ItemEntity> Situ, ItemEntity Tossed, ServerLevel Facility, List<long[]> Forced, ServerPlayer User) {
        for (ItemEntity Each : Situ) {
            if (!Each.isRemoved()) {
                Each.discard();
            }
        }
        if (!Tossed.isRemoved()) {
            Tossed.discard();
        }
        for (long[] Each : Forced) {
            Facility.setChunkForced((int) Each[0], (int) Each[1], false);
        }
        Scp1730GameTests.dismiss(User);
    }
}
