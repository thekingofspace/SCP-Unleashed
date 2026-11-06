package net.rai.scp_unleashed.scp407.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containment.ContainmentRegistry;
import net.rai.scp_unleashed.containment.ReinforcedGlassBlock;
import net.rai.scp_unleashed.scp407.Scp407BloomBlock;
import net.rai.scp_unleashed.scp407.Scp407Carpets;
import net.rai.scp_unleashed.scp407.Scp407Faces;
import net.rai.scp_unleashed.scp407.Scp407GrowthBlock;
import net.rai.scp_unleashed.scp407.Scp407Growth;
import net.rai.scp_unleashed.scp407.Scp407PlantBlock;
import net.rai.scp_unleashed.scp407.Scp407PodBlock;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Session;
import net.rai.scp_unleashed.scp407.Scp407Sheets;
import net.rai.scp_unleashed.scp407.Scp407ShelfBlock;
import net.rai.scp_unleashed.scp407.Scp407Sessions;
import net.rai.scp_unleashed.scp407.Scp407TrunkBlock;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import net.rai.scp_unleashed.wire.WireMessage;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp407BiosphereGameTests {
    public static final String Arena = Scp407GameTests.Arena;
    public static final int HallMin = 3;
    public static final int HallSize = 13;
    public static final int HallHeight = 6;
    public static final int HallCalls = 3200;
    public static final int SaturationRounds = 40;

    private Scp407BiosphereGameTests() {
    }

    private static void hall(GameTestHelper Helper) {
        BlockState Wall = Blocks.STONE_BRICKS.defaultBlockState();
        for (int X = HallMin - 1; X <= HallMin + HallSize; X++) {
            for (int Z = HallMin - 1; Z <= HallMin + HallSize; Z++) {
                for (int Y = 2; Y <= HallHeight + 2; Y++) {
                    boolean Shell = X < HallMin || Z < HallMin || X >= HallMin + HallSize || Z >= HallMin + HallSize || Y == HallHeight + 2;
                    Helper.setBlock(new BlockPos(X, Y, Z), Shell ? Wall : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static List<BlockPos> cells(GameTestHelper Helper) {
        List<BlockPos> Found = new ArrayList<>();
        for (int X = HallMin; X < HallMin + HallSize; X++) {
            for (int Z = HallMin; Z < HallMin + HallSize; Z++) {
                for (int Y = 2; Y < 2 + HallHeight; Y++) {
                    Found.add(Helper.absolutePos(new BlockPos(X, Y, Z)));
                }
            }
        }
        return Found;
    }

    private static List<Direction> skirts(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        List<Direction> Out = new ArrayList<>();
        if (State.getBlock() instanceof Scp407GrowthBlock Growth) {
            Growth.skirts(World, Pos, State, Out);
        }
        return Out;
    }

    private static String bareFace(ServerLevel World, List<BlockPos> Cells) {
        for (BlockPos Pos : Cells) {
            BlockState State = World.getBlockState(Pos);
            if (!(State.getBlock() instanceof Scp407GrowthBlock Growth)) {
                continue;
            }
            List<Direction> Covered = new ArrayList<>();
            Growth.carpetFaces(State, Covered);
            Growth.skirts(World, Pos, State, Covered);
            if (State.getBlock() instanceof Scp407PodBlock && State.getValue(Scp407PodBlock.Shape) != Scp407PodBlock.Part.Corner
                    && State.getValue(Scp407PodBlock.Shape) != Scp407PodBlock.Part.Shoulder) {
                continue;
            }
            for (Direction Side : Direction.values()) {
                if (Scp407Faces.holds(World, Pos, Side) && !Covered.contains(Side)) {
                    return State.getBlock() + " at " + Pos + " leaves its " + Side + " face bare";
                }
            }
        }
        return null;
    }

    private static String trunkProblem(ServerLevel World, List<BlockPos> Cells) {
        Set<BlockPos> Trunks = new HashSet<>();
        for (BlockPos Pos : Cells) {
            if (World.getBlockState(Pos).getBlock() instanceof Scp407TrunkBlock) {
                Trunks.add(Pos);
            }
        }
        if (Trunks.isEmpty()) {
            return "no trunks grew";
        }
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        Set<BlockPos> Reached = new HashSet<>();
        for (BlockPos Pos : Trunks) {
            BlockState State = World.getBlockState(Pos);
            if (Scp407TrunkBlock.linkCount(State) == 0) {
                return "a trunk at " + Pos + " has no joints";
            }
            for (Direction Side : Direction.values()) {
                if (!Scp407TrunkBlock.linked(State, Side)) {
                    continue;
                }
                BlockPos Next = Pos.relative(Side);
                BlockState Other = World.getBlockState(Next);
                if (Other.getBlock() instanceof Scp407TrunkBlock) {
                    if (!Scp407TrunkBlock.linked(Other, Side.getOpposite())) {
                        return "a joint from " + Pos + " to " + Next + " is drawn on one side only";
                    }
                } else if (Other.getBlock() instanceof Scp407GrowthBlock Growth) {
                    if (Growth.base(Other) != Side.getOpposite()) {
                        return "a trunk at " + Pos + " reaches toward " + Other.getBlock() + " that does not hang from it";
                    }
                } else if (!(Side.getAxis() == Direction.Axis.Y && Scp407Faces.holds(World, Pos, Side))) {
                    return "a trunk at " + Pos + " reaches " + Side + " into nothing";
                }
            }
            if (Scp407TrunkBlock.linked(State, Direction.DOWN) && Scp407Faces.holds(World, Pos, Direction.DOWN)) {
                Reached.add(Pos);
                Queue.add(Pos);
                if (!State.getValue(Scp407GrowthBlock.Carpet)) {
                    return "a trunk root at " + Pos + " stands on bare floor";
                }
            }
        }
        while (!Queue.isEmpty()) {
            BlockPos Pos = Queue.poll();
            BlockState State = World.getBlockState(Pos);
            for (Direction Side : Direction.values()) {
                BlockPos Next = Pos.relative(Side);
                if (Scp407TrunkBlock.linked(State, Side) && Trunks.contains(Next) && Reached.add(Next)) {
                    Queue.add(Next);
                }
            }
        }
        if (Reached.size() != Trunks.size()) {
            for (BlockPos Pos : Trunks) {
                if (!Reached.contains(Pos)) {
                    return "a trunk piece at " + Pos + " floats free of any root";
                }
            }
        }
        for (BlockPos Pos : Cells) {
            BlockState State = World.getBlockState(Pos);
            if (State.getBlock() instanceof Scp407BloomBlock) {
                Direction Toward = State.getValue(Scp407BloomBlock.Attached);
                BlockState Holder = World.getBlockState(Pos.relative(Toward));
                if (Holder.getBlock() instanceof Scp407TrunkBlock && !Scp407TrunkBlock.linked(Holder, Toward.getOpposite())) {
                    return "a bloom at " + Pos + " is not joined to the branch it grows from";
                }
            }
        }
        return null;
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp407_biosphere_carpeted")
    public static void stemsStandOnCarpet(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp407GameTests.room(Helper, 3, 3);
        SpeakerBlockEntity Speaker = Scp407GameTests.speaker(Helper, new BlockPos(2, 4, 6), Direction.EAST);
        Mob Body = Scp407GameTests.dclass(Helper, 8.5, 2, 8.5);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Body.discard();
            BlockPos Root = Helper.absolutePos(new BlockPos(5, 2, 5));
            BlockPos Stem = Root.above();
            BlockPos Arm = Stem.east();
            BlockPos Tip = Arm.east();
            BlockPos Hanger = Arm.below();
            BlockPos Crown = Stem.above();
            BlockPos Wall = Helper.absolutePos(new BlockPos(3, 3, 8));
            BlockPos Corner = Helper.absolutePos(new BlockPos(8, 2, 4));
            BlockPos Floor = Helper.absolutePos(new BlockPos(4, 2, 8));
            BlockState Trunk = Scp407Registry.Trunk.get().defaultBlockState();
            Helper.assertTrue(Scp407Growth.place(World, Session, Root, Trunk) && Scp407Growth.place(World, Session, Stem, Trunk), "Could not grow the trunk");
            Helper.assertTrue(Scp407Growth.place(World, Session, Arm, Trunk), "Could not grow the branch");
            Helper.assertTrue(Scp407Growth.place(World, Session, Tip, Scp407Registry.Bloom.get().defaultBlockState().setValue(Scp407BloomBlock.Attached, Direction.WEST)),
                    "Could not grow the branch bloom");
            Helper.assertTrue(Scp407Growth.place(World, Session, Crown, Scp407Registry.Bloom.get().defaultBlockState().setValue(Scp407BloomBlock.Attached, Direction.DOWN)),
                    "Could not grow the crown bloom");
            Helper.assertTrue(Scp407Growth.place(World, Session, Hanger, Scp407Registry.Bulbs.get().defaultBlockState()), "Could not hang bulbs from the branch");
            Helper.assertTrue(Scp407Growth.place(World, Session, Wall, Scp407Registry.Bloom.get().defaultBlockState().setValue(Scp407BloomBlock.Attached, Direction.WEST)),
                    "Could not grow the wall bloom");
            Helper.assertTrue(Scp407Growth.place(World, Session, Corner, Scp407Registry.Pod.get().defaultBlockState().setValue(Scp407PodBlock.Shape, Scp407PodBlock.Part.Corner)),
                    "Could not grow the pod corner");
            Helper.assertTrue(Scp407Growth.place(World, Session, Floor, Scp407Registry.Fern.get().defaultBlockState()), "Could not grow the fern");
            BlockState RootState = World.getBlockState(Root);
            BlockState StemState = World.getBlockState(Stem);
            BlockState ArmState = World.getBlockState(Arm);
            Helper.assertTrue(RootState.getValue(Scp407GrowthBlock.Carpet) && skirts(World, Root).equals(List.of(Direction.DOWN)),
                    "The trunk root is not carpeted: " + RootState + " skirts " + skirts(World, Root));
            Helper.assertTrue(Scp407TrunkBlock.linked(RootState, Direction.DOWN) && Scp407TrunkBlock.linked(RootState, Direction.UP), "The root is not joined to floor and stem");
            Helper.assertTrue(!StemState.getValue(Scp407GrowthBlock.Carpet), "A raised trunk piece drew a carpet");
            Helper.assertTrue(Scp407TrunkBlock.linked(StemState, Direction.DOWN) && Scp407TrunkBlock.linked(StemState, Direction.UP) && Scp407TrunkBlock.linked(StemState, Direction.EAST),
                    "The stem is not joined to root, crown and branch: " + StemState);
            Helper.assertTrue(Scp407TrunkBlock.linked(ArmState, Direction.WEST) && Scp407TrunkBlock.linked(ArmState, Direction.EAST) && Scp407TrunkBlock.linked(ArmState, Direction.DOWN)
                    && !Scp407TrunkBlock.linked(ArmState, Direction.UP), "The branch is not joined to its trunk, bloom and hanging bulbs: " + ArmState);
            Helper.assertTrue(!World.getBlockState(Hanger).getValue(Scp407GrowthBlock.Carpet), "Bulbs hanging from a branch drew a ceiling carpet");
            Helper.assertTrue(World.getBlockState(Wall).getValue(Scp407GrowthBlock.Carpet), "A wall bloom is not carpeted");
            Helper.assertTrue(!World.getBlockState(Tip).getValue(Scp407GrowthBlock.Carpet), "A bloom on a branch drew a carpet");
            Helper.assertTrue(World.getBlockState(Corner).getValue(Scp407GrowthBlock.Carpet), "A pod corner on the floor is not carpeted");
            BlockState Fern = World.getBlockState(Floor);
            Helper.assertTrue(Fern.getValue(Scp407GrowthBlock.Carpet) && skirts(World, Floor).contains(Direction.DOWN), "A fern is not carpeted");
            BlockPos Hug = Helper.absolutePos(new BlockPos(9, 2, 3));
            Helper.assertTrue(Scp407Growth.place(World, Session, Hug, Trunk) && Scp407Growth.place(World, Session, Hug.above(), Trunk), "Could not grow the corner trunk");
            List<Direction> Low = skirts(World, Hug);
            List<Direction> High = skirts(World, Hug.above());
            Helper.assertTrue(Low.containsAll(List.of(Direction.DOWN, Direction.EAST, Direction.NORTH)) && Low.size() == 3, "The corner trunk root left walls bare: " + Low);
            Helper.assertTrue(High.containsAll(List.of(Direction.EAST, Direction.NORTH)) && High.size() == 2, "The corner trunk left walls bare behind it: " + High);
            Helper.assertTrue(skirts(World, Wall).contains(Direction.WEST) && skirts(World, Corner).contains(Direction.DOWN), "A wall bloom or pod corner has no carpet behind it");
            String Problem = trunkProblem(World, List.of(Root, Stem, Arm, Tip, Crown, Hanger));
            Helper.assertTrue(Problem == null, "The small tree is not joined: " + Problem);
            int[][] Probes = {{0, 0, 0, 3}, {-37, 64, 129, 2}, {1000, -20, -5000, 4}, {12, 5, -7, 2}, {-3, 70, -3, 0}};
            for (int[] Probe : Probes) {
                int Found = Scp407Carpets.variantAt(new BlockPos(Probe[0], Probe[1], Probe[2]));
                Helper.assertTrue(Found == Probe[3], "Carpet patches differ from the preview map at " + Probe[0] + " " + Probe[1] + " " + Probe[2] + ": " + Found);
            }
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp407_biosphere_hall")
    public static void biosphereFillsAndWithers(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        hall(Helper);
        Helper.setBlock(new BlockPos(9, 2, 12), Blocks.OAK_PLANKS.defaultBlockState());
        SpeakerBlockEntity Speaker = Scp407GameTests.speaker(Helper, new BlockPos(2, 4, 9), Direction.EAST);
        Mob Body = Scp407GameTests.dclass(Helper, 9.5, 2, 9.5);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Map<Block, Integer> Counts = new HashMap<>();
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The hall did not start the first act");
            Body.discard();
            for (int Call = 0; Call < HallCalls; Call++) {
                Scp407Growth.tick(World, Session, Session.ActTick + Scp407Growth.RampTicks);
            }
            List<BlockPos> Cells = cells(Helper);
            for (int Round = 0; Round < SaturationRounds && bareFace(World, Cells) != null; Round++) {
                for (int Call = 0; Call < 500; Call++) {
                    Scp407Growth.tick(World, Session, Session.ActTick + Scp407Growth.RampTicks);
                }
            }
            String Bare = bareFace(World, Cells);
            Helper.assertTrue(Bare == null, "The carpet never closed: " + Bare);
            int Grown = 0;
            for (BlockPos Pos : Cells) {
                BlockState State = World.getBlockState(Pos);
                if (State.getBlock() instanceof Scp407GrowthBlock) {
                    Grown++;
                    Counts.merge(State.getBlock(), 1, Integer::sum);
                    Helper.assertTrue(Session.Grown.contains(Pos.asLong()), "A growth block at " + Pos + " is not tracked");
                    if (State.getBlock() instanceof Scp407PlantBlock Plant && Plant.tall() || State.getBlock() instanceof Scp407TrunkBlock) {
                        int Reach = Math.max(Math.abs(Pos.getX() - Session.Origin.getX()), Math.abs(Pos.getZ() - Session.Origin.getZ()));
                        Helper.assertTrue(Reach > Scp407Growth.ClearingRadius || Math.abs(Pos.getY() - Session.Origin.getY()) > 3, "A tall plant grew in the speaker clearing at " + Pos);
                    }
                }
            }
            List<DeferredBlock<? extends Block>> Expected = new ArrayList<>(Scp407Registry.Plants);
            Expected.addAll(List.of(Scp407Registry.Mat, Scp407Registry.Sprig, Scp407Registry.Trunk, Scp407Registry.Bloom));
            for (DeferredBlock<? extends Block> Kind : Expected) {
                Helper.assertTrue(Counts.getOrDefault(Kind.get(), 0) > 0, "No " + Kind.getId() + " grew in the hall, counts " + Counts);
            }
            Helper.assertTrue(Grown == Session.Grown.size() && Session.Spread == Grown, "Tracked growth " + Session.Grown.size() + " and counter " + Session.Spread + " differ from " + Grown);
            String Problem = trunkProblem(World, Cells);
            Helper.assertTrue(Problem == null, "The hall trees are not joined: " + Problem);
            Helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(9, 2, 12));
            Speaker.receiveMessage(new WireMessage(Speaker.getChannel(), SoundMessages.stopPayload(), Speaker.getBlockPos()));
        }).thenWaitUntil(Scp407GameTests.within(Helper, 300, "The withering did not finish", () -> Scp407Sessions.get(World).byId(Session.Id) == null)).thenExecute(() -> {
            for (BlockPos Pos : cells(Helper)) {
                Helper.assertTrue(!(World.getBlockState(Pos).getBlock() instanceof Scp407GrowthBlock), "Growth was left behind at " + Pos);
            }
            Helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(9, 2, 12));
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp407_biosphere_cap")
    public static void growthStopsAtTheCap(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        hall(Helper);
        SpeakerBlockEntity Speaker = Scp407GameTests.speaker(Helper, new BlockPos(2, 4, 9), Direction.EAST);
        Mob Body = Scp407GameTests.dclass(Helper, 9.5, 2, 9.5);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Session.Cap = 120;
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The hall did not start the first act");
            Body.discard();
            for (int Call = 0; Call < 1500; Call++) {
                Scp407Growth.tick(World, Session, Session.ActTick + Scp407Growth.RampTicks);
            }
            int Grown = 0;
            for (BlockPos Pos : cells(Helper)) {
                if (World.getBlockState(Pos).getBlock() instanceof Scp407GrowthBlock) {
                    Grown++;
                }
            }
            Helper.assertTrue(Session.Spread == Session.Cap && Grown == Session.Cap && Session.Grown.size() == Session.Cap,
                    "The cap of " + Session.Cap + " was not held: counter " + Session.Spread + ", blocks " + Grown + ", tracked " + Session.Grown.size());
            Helper.assertTrue(Scp407Growth.MaxSpread == 3000, "The default cap changed to " + Scp407Growth.MaxSpread);
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp407_biosphere_glass")
    public static void padsShowFromBothSides(GameTestHelper Helper) {
        for (Direction Face : Direction.values()) {
            String Problem = padProblem(Face);
            Helper.assertTrue(Problem == null, "The carpet pad on the " + Face.getName() + " side " + Problem);
        }
        ServerLevel World = Helper.getLevel();
        BlockState Glass = ContainmentRegistry.ReinforcedGlass.get().defaultBlockState();
        for (int X = 3; X <= 7; X++) {
            for (int Y = 2; Y <= 4; Y++) {
                Helper.setBlock(new BlockPos(X, Y, 4), Glass);
            }
            for (int Z = 5; Z <= 7; Z++) {
                Helper.setBlock(new BlockPos(X, 1, Z), Glass.setValue(ReinforcedGlassBlock.WindowAxis, Direction.Axis.Y));
            }
        }
        Helper.setBlock(new BlockPos(4, 3, 5), Scp407Registry.Shelf.get().defaultBlockState().setValue(Scp407ShelfBlock.Attached, Direction.NORTH));
        Helper.setBlock(new BlockPos(5, 2, 5), Scp407Registry.Trunk.get().defaultBlockState());
        Helper.setBlock(new BlockPos(6, 2, 6), Scp407Registry.Puffball.get().defaultBlockState());
        List<Direction> Shelf = skirts(World, Helper.absolutePos(new BlockPos(4, 3, 5)));
        List<Direction> Trunk = skirts(World, Helper.absolutePos(new BlockPos(5, 2, 5)));
        List<Direction> Puffball = skirts(World, Helper.absolutePos(new BlockPos(6, 2, 6)));
        Helper.assertTrue(Shelf.contains(Direction.NORTH), "A shelf cap on reinforced glass has no carpet behind it: " + Shelf);
        Helper.assertTrue(Trunk.containsAll(List.of(Direction.DOWN, Direction.NORTH)), "A trunk on a glass floor against a glass wall left the glass bare: " + Trunk);
        Helper.assertTrue(Puffball.contains(Direction.DOWN), "Puffballs on a glass floor have no carpet under them: " + Puffball);
        Helper.assertTrue(Scp407Faces.holds(World, Helper.absolutePos(new BlockPos(3, 2, 5)), Direction.NORTH), "Carpet cannot grow on reinforced glass");
        Helper.succeed();
    }

    private static String padProblem(Direction Face) {
        List<Scp407Sheets.Sheet> Sheets = Scp407Sheets.pad(Face);
        if (Sheets.size() != 6) {
            return "is not a closed slab";
        }
        Scp407Sheets.Sheet Front = Sheets.get(0);
        Scp407Sheets.Sheet Back = Sheets.get(1);
        if (Front.Normal() != Face.getOpposite() || Back.Normal() != Face) {
            return "has no back face";
        }
        int Axis = Face.getAxis().ordinal();
        float Support = Face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0f : 0.0f;
        float FrontGap = gap(Front, Axis, Support);
        float BackGap = gap(Back, Axis, Support);
        if (FrontGap < 0.0f || BackGap < 0.0f) {
            return "is not flat";
        }
        if (BackGap < 0.01f || BackGap >= FrontGap) {
            return "has its back on the support face or in front of the pad";
        }
        if (!spans(Front, Axis) || !spans(Back, Axis)) {
            return "does not cover the whole face";
        }
        for (Scp407Sheets.Sheet Sheet : Sheets) {
            if (Scp407Sheets.facing(Sheet.Points(), Sheet.Normal()) <= 0.0f) {
                return "has a sheet wound away from its normal " + Sheet.Normal().getName();
            }
        }
        Set<Direction> Rims = new HashSet<>();
        for (Scp407Sheets.Sheet Rim : Sheets.subList(2, 6)) {
            Rims.add(Rim.Normal());
            float Low = Float.MAX_VALUE;
            float High = -Float.MAX_VALUE;
            for (float[] Point : Rim.Points()) {
                float Gap = Math.abs(Point[Axis] - Support) * 16.0f;
                Low = Math.min(Low, Gap);
                High = Math.max(High, Gap);
            }
            if (Math.abs(Low - BackGap) > 1.0e-4f || Math.abs(High - FrontGap) > 1.0e-4f) {
                return "leaves a slit between back and front on the " + Rim.Normal().getName() + " edge";
            }
        }
        if (!Rims.equals(Set.of(Scp407Carpets.edges(Face)))) {
            return "is open on an edge";
        }
        return null;
    }

    private static float gap(Scp407Sheets.Sheet Sheet, int Axis, float Support) {
        float First = Sheet.Points()[0][Axis];
        for (float[] Point : Sheet.Points()) {
            if (Math.abs(Point[Axis] - First) > 1.0e-6f) {
                return -1.0f;
            }
        }
        return Math.abs(First - Support) * 16.0f;
    }

    private static boolean spans(Scp407Sheets.Sheet Sheet, int Axis) {
        for (int Index = 0; Index < 3; Index++) {
            if (Index == Axis) {
                continue;
            }
            float Low = Float.MAX_VALUE;
            float High = -Float.MAX_VALUE;
            for (float[] Point : Sheet.Points()) {
                Low = Math.min(Low, Point[Index]);
                High = Math.max(High, Point[Index]);
            }
            if (Low > 1.0e-6f || High < 1.0f - 1.0e-6f) {
                return false;
            }
        }
        return true;
    }
}
