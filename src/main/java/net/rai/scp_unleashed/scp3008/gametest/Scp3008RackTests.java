package net.rai.scp_unleashed.scp3008.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.block.Scp3008BeamBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008RackPostBlock;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import net.rai.scp_unleashed.scp3008.world.Scp3008Healing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008RackTests {
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final int Top = Scp3008Plan.Stand + 14;

    private Scp3008RackTests() {
    }

    private static Scp3008ShowroomTests.Site site(GameTestHelper Helper) {
        Scp3008ShowroomTests.Site Area = Scp3008ShowroomTests.anyPlot(Scp3008Plan.RackPlot);
        Helper.assertTrue(Area != null, "no rack plot nearby");
        return Area;
    }

    private static Map<BlockPos, BlockState> posts(ServerLevel World, Scp3008ShowroomTests.Site Area) {
        Map<BlockPos, BlockState> Found = new HashMap<>();
        for (BlockPos Pos : BlockPos.betweenClosed(Area.minX(), Scp3008Plan.Stand, Area.minZ(), Area.maxX(), Top, Area.maxZ())) {
            BlockState State = World.getBlockState(Pos);
            if (State.is(Scp3008Registry.RackPost.get())) {
                Found.put(Pos.immutable(), State);
            }
        }
        return Found;
    }

    private static List<ChunkPos> chunks(Scp3008ShowroomTests.Site Area) {
        List<ChunkPos> Found = new ArrayList<>();
        for (int Cx = Area.minX() >> 4; Cx <= Area.maxX() >> 4; Cx++) {
            for (int Cz = Area.minZ() >> 4; Cz <= Area.maxZ() >> 4; Cz++) {
                Found.add(new ChunkPos(Cx, Cz));
            }
        }
        return Found;
    }

    private static int joints(GameTestHelper Helper, ServerLevel World, Scp3008ShowroomTests.Site Area, String Where) {
        int Checked = 0;
        for (BlockPos Pos : BlockPos.betweenClosed(Area.minX(), Scp3008Plan.Stand, Area.minZ(), Area.maxX(), Top, Area.maxZ())) {
            BlockState Beam = World.getBlockState(Pos);
            if (!Beam.is(Scp3008Registry.RackBeam.get())) {
                continue;
            }
            Direction.Axis Along = Beam.getValue(Scp3008BeamBlock.Axis);
            for (Direction Side : new Direction[]{Scp3008RackPostBlock.lowSide(Along), Scp3008RackPostBlock.highSide(Along)}) {
                BlockPos Next = Pos.relative(Side);
                BlockState Post = World.getBlockState(Next);
                if (!Post.is(Scp3008Registry.RackPost.get())) {
                    continue;
                }
                boolean Joined = Side == Scp3008RackPostBlock.highSide(Along) ? Post.getValue(Scp3008RackPostBlock.Low) : Post.getValue(Scp3008RackPostBlock.High);
                Helper.assertTrue(Joined, Where + ": the post at " + Next + " does not join the beam at " + Pos);
                Helper.assertTrue(Post.getValue(Scp3008RackPostBlock.Axis) == Along, Where + ": the uprights at " + Next + " are turned away from the rails at " + Pos);
                Checked++;
            }
        }
        for (Map.Entry<BlockPos, BlockState> Entry : posts(World, Area).entrySet()) {
            BlockState Expected = Scp3008Registry.RackPost.get().joined(World, Entry.getKey(), Entry.getValue());
            Helper.assertTrue(Expected.equals(Entry.getValue()), Where + ": the post at " + Entry.getKey() + " is " + Entry.getValue() + " but its neighbours say " + Expected);
        }
        return Checked;
    }

    @GameTest(template = Scp3008GameTests.Arena, timeoutTicks = 1200, batch = "scp3008_rack_generated")
    public static void generatedRacksJoin(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008ShowroomTests.Site Area = site(Helper);
        Scp3008ShowroomTests.build(World, Area, Top);
        int Checked = joints(Helper, World, Area, "generated rack");
        Helper.assertTrue(Checked > 40, "only " + Checked + " beam ends met a post in the generated racks");
        int Footed = 0;
        for (Map.Entry<BlockPos, BlockState> Entry : posts(World, Area).entrySet()) {
            boolean Floor = Entry.getKey().getY() == Scp3008Plan.Stand;
            Helper.assertTrue(Entry.getValue().getValue(Scp3008RackPostBlock.Foot) == Floor, "the post at " + Entry.getKey() + " has the wrong footplate");
            Footed += Floor ? 1 : 0;
        }
        Helper.assertTrue(Footed > 0, "no rack posts were generated");
        Helper.succeed();
    }

    private static void column(ServerLevel World, BlockPos Base, int Height) {
        Scp3008RackPostBlock Post = Scp3008Registry.RackPost.get();
        for (int Y = 0; Y < Height; Y++) {
            World.setBlock(Base.above(Y), Post.joined(World, Base.above(Y), Post.defaultBlockState()), Block.UPDATE_ALL);
        }
    }

    private static void handRun(GameTestHelper Helper, Direction.Axis Along) {
        ServerLevel World = Helper.getLevel();
        Direction Forward = Scp3008RackPostBlock.highSide(Along);
        BlockPos Start = Helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos End = Start.relative(Forward, 4);
        column(World, Start, 4);
        column(World, End, 4);
        BlockState Beam = Scp3008Registry.RackBeam.get().defaultBlockState().setValue(Scp3008BeamBlock.Axis, Along);
        for (int Step = 1; Step <= 3; Step++) {
            World.setBlock(Start.relative(Forward, Step).above(2), Beam, Block.UPDATE_ALL);
        }
        String Where = "hand-built run along " + Along;
        for (BlockPos Base : new BlockPos[]{Start, End}) {
            for (int Y = 0; Y < 4; Y++) {
                BlockState Post = World.getBlockState(Base.above(Y));
                Helper.assertTrue(Post.getValue(Scp3008RackPostBlock.Axis) == Along, Where + ": the post at height " + Y + " faces " + Post.getValue(Scp3008RackPostBlock.Axis));
                Helper.assertTrue(Post.getValue(Scp3008RackPostBlock.Foot) == (Y == 0), Where + ": the footplate is wrong at height " + Y);
                boolean Level = Y == 2;
                Helper.assertTrue(Post.getValue(Scp3008RackPostBlock.High) == (Level && Base.equals(Start)), Where + ": the high joint is wrong at height " + Y);
                Helper.assertTrue(Post.getValue(Scp3008RackPostBlock.Low) == (Level && Base.equals(End)), Where + ": the low joint is wrong at height " + Y);
            }
        }
        World.setBlock(Start.relative(Forward).above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Helper.assertFalse(World.getBlockState(Start.above(2)).getValue(Scp3008RackPostBlock.High), Where + ": removing the beam kept its joint");
        Helper.assertTrue(World.getBlockState(End.above(2)).getValue(Scp3008RackPostBlock.Low), Where + ": removing the far beam dropped the other joint");
    }

    @GameTest(template = Scp3008GameTests.Arena, timeoutTicks = 100, batch = "scp3008_rack_hand_x")
    public static void handRunAlongX(GameTestHelper Helper) {
        handRun(Helper, Direction.Axis.X);
        Helper.succeed();
    }

    @GameTest(template = Scp3008GameTests.Arena, timeoutTicks = 100, batch = "scp3008_rack_hand_z")
    public static void handRunAlongZ(GameTestHelper Helper) {
        handRun(Helper, Direction.Axis.Z);
        Helper.succeed();
    }

    @GameTest(template = Scp3008GameTests.Arena, timeoutTicks = 1200, batch = "scp3008_rack_migrate")
    public static void oldRacksMigrate(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008ShowroomTests.Site Area = site(Helper);
        Scp3008ShowroomTests.build(World, Area, Top);
        Map<BlockPos, BlockState> Expected = posts(World, Area);
        Helper.assertTrue(!Expected.isEmpty(), "no rack posts were generated");
        BlockState Old = Scp3008Registry.RackPost.get().defaultBlockState();
        for (BlockPos Pos : Expected.keySet()) {
            World.setBlock(Pos, Old, Flags);
        }
        for (ChunkPos Chunk : chunks(Area)) {
            Scp3008Healing.heal(World, Chunk);
        }
        for (Map.Entry<BlockPos, BlockState> Entry : Expected.entrySet()) {
            BlockState Now = World.getBlockState(Entry.getKey());
            Helper.assertTrue(Now.equals(Entry.getValue()), "migration left " + Now + " at " + Entry.getKey() + " instead of " + Entry.getValue());
        }
        joints(Helper, World, Area, "migrated rack");
        for (ChunkPos Chunk : chunks(Area)) {
            Scp3008Healing.heal(World, Chunk);
        }
        Helper.assertTrue(posts(World, Area).equals(Expected), "a second heal changed the migrated posts");
        Helper.succeed();
    }
}
