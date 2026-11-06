package net.rai.scp_unleashed.scp407.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp407.Scp407Faces;
import net.rai.scp_unleashed.scp407.Scp407Growth;
import net.rai.scp_unleashed.scp407.Scp407MatBlock;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Session;
import net.rai.scp_unleashed.scp407.Scp407Sessions;
import net.rai.scp_unleashed.scp407.Scp407SprigBlock;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import net.rai.scp_unleashed.wire.WireMessage;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp407CarpetGameTests {
    public static final String Arena = Scp407GameTests.Arena;
    public static final int GrowthCalls = 1500;

    private Scp407CarpetGameTests() {
    }

    private static List<BlockPos> carpets(GameTestHelper Helper) {
        List<BlockPos> Found = new ArrayList<>();
        ServerLevel World = Helper.getLevel();
        for (int X = 3; X < 3 + Scp407GameTests.RoomSize; X++) {
            for (int Z = 3; Z < 3 + Scp407GameTests.RoomSize; Z++) {
                for (int Y = 2; Y <= Scp407GameTests.RoomHeight + 1; Y++) {
                    BlockPos Pos = Helper.absolutePos(new BlockPos(X, Y, Z));
                    if (Scp407Growth.carpet(World.getBlockState(Pos))) {
                        Found.add(Pos);
                    }
                }
            }
        }
        return Found;
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp407_carpet_corners")
    public static void carpetWrapsCornersAndWithers(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp407GameTests.room(Helper, 3, 3);
        Helper.setBlock(new BlockPos(6, 2, 8), Blocks.OAK_PLANKS.defaultBlockState());
        Helper.setBlock(new BlockPos(8, 3, 4), Blocks.BOOKSHELF.defaultBlockState());
        SpeakerBlockEntity Speaker = Scp407GameTests.speaker(Helper, new BlockPos(2, 4, 6), Direction.EAST);
        Mob Body = Scp407GameTests.dclass(Helper, 6.5, 2, 6.5);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        List<BlockPos> Wrapped = new ArrayList<>();
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Body.discard();
            for (int Call = 0; Call < GrowthCalls; Call++) {
                Scp407Growth.tick(World, Session, Session.ActTick + Scp407Growth.RampTicks);
            }
            int Two = 0;
            int Three = 0;
            int Faces = 0;
            List<BlockPos> Cells = carpets(Helper);
            for (BlockPos Pos : Cells) {
                BlockState State = World.getBlockState(Pos);
                int Count = Scp407Faces.count(State) + (State.getBlock() instanceof Scp407SprigBlock ? 1 : 0);
                Helper.assertTrue(Count >= 1, "A carpet cell at " + Pos + " has no faces");
                for (Direction Side : Direction.values()) {
                    if (Scp407Faces.has(State, Side)) {
                        Faces++;
                        Helper.assertTrue(Scp407Faces.holds(World, Pos, Side), "A carpet face at " + Pos + " " + Side + " has no sturdy surface behind it");
                    }
                }
                if (State.getBlock() instanceof Scp407SprigBlock) {
                    Helper.assertTrue(Scp407Faces.holds(World, Pos, Direction.DOWN), "A sprig at " + Pos + " has no floor");
                }
                if (Count == 2) {
                    Two++;
                }
                if (Count >= 3) {
                    Three++;
                }
                if (Count >= 2) {
                    Wrapped.add(Pos);
                }
                Helper.assertTrue(Session.Grown.contains(Pos.asLong()), "A carpet cell at " + Pos + " is not tracked");
            }
            int Corners = 0;
            for (int X : new int[]{3, 9}) {
                for (int Z : new int[]{3, 9}) {
                    BlockState State = World.getBlockState(Helper.absolutePos(new BlockPos(X, 2, Z)));
                    boolean Mat = State.getBlock() instanceof Scp407MatBlock && Scp407Faces.has(State, Direction.DOWN) && Scp407Faces.count(State) == 3;
                    boolean Sprig = State.getBlock() instanceof Scp407SprigBlock && Scp407Faces.count(State) == 2;
                    if (Mat || Sprig) {
                        Corners++;
                    }
                }
            }
            Helper.assertTrue(Two > 0, "No carpet cell joined two faces, out of " + Cells.size());
            Helper.assertTrue(Three > 0 && Corners > 0, "No floor corner carried floor and both walls, " + Three + " cells with three faces");
            Helper.assertTrue(Faces > Cells.size(), "The carpet did not wrap, " + Faces + " faces on " + Cells.size() + " cells");
            Helper.assertTrue(Session.Spread == Session.Grown.size(), "The growth cap counter " + Session.Spread + " does not count cells " + Session.Grown.size());
            Helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(6, 2, 8));
            Helper.assertBlockPresent(Blocks.BOOKSHELF, new BlockPos(8, 3, 4));
            Helper.assertTrue(Scp407GameTests.growthIn(Helper, 3, 3) == Session.Grown.size(), "Tracked growth does not match the blocks in the room");
            Speaker.receiveMessage(new WireMessage(Speaker.getChannel(), SoundMessages.stopPayload(), Speaker.getBlockPos()));
        }).thenWaitUntil(Scp407GameTests.within(Helper, 300, "The withering did not finish", () -> Scp407Sessions.get(World).byId(Session.Id) == null)).thenExecute(() -> {
            for (BlockPos Pos : Wrapped) {
                Helper.assertTrue(World.getBlockState(Pos).isAir(), "A multi face cell at " + Pos + " was left behind");
            }
            Helper.assertTrue(carpets(Helper).isEmpty() && Scp407GameTests.growthIn(Helper, 3, 3) == 0, "Growth remained after withering");
            Helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(6, 2, 8));
            Helper.assertBlockPresent(Blocks.BOOKSHELF, new BlockPos(8, 3, 4));
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp407_carpet_support")
    public static void carpetFacesNeedSupport(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (int X = 3; X <= 9; X++) {
            for (int Z = 3; Z <= 9; Z++) {
                Helper.setBlock(new BlockPos(X, 2, Z), Blocks.STONE.defaultBlockState());
                for (int Y = 3; Y <= 5; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        Helper.setBlock(new BlockPos(5, 3, 5), Blocks.STONE.defaultBlockState());
        Helper.setBlock(new BlockPos(5, 3, 8), Blocks.STONE.defaultBlockState());
        BlockPos Mat = new BlockPos(6, 3, 5);
        BlockPos Sprig = new BlockPos(6, 3, 8);
        BlockPos Mid = new BlockPos(7, 3, 6);
        RandomSource Dice = RandomSource.create(407L);
        BlockState Wrapped = Scp407MatBlock.covering(List.of(Direction.DOWN));
        for (int Roll = 0; Roll < 20; Roll++) {
            Wrapped = Scp407Growth.wrap(World, Helper.absolutePos(Mat), Wrapped, Dice);
        }
        Helper.assertTrue(Scp407Faces.has(Wrapped, Direction.DOWN) && Scp407Faces.has(Wrapped, Direction.WEST), "Wrapping did not reach the pillar beside the mat");
        for (Direction Side : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST}) {
            Helper.assertTrue(!Scp407Faces.has(Wrapped, Side), "Wrapping put a face on " + Side + " with nothing behind it");
        }
        Helper.setBlock(Mat, Wrapped);
        Helper.setBlock(Sprig, Scp407Faces.with(Scp407Registry.Sprig.get().defaultBlockState(), Direction.WEST, true));
        Helper.setBlock(Mid, Scp407MatBlock.covering(List.of(Direction.DOWN)));
        Helper.setBlock(new BlockPos(5, 3, 5), Blocks.AIR.defaultBlockState());
        Helper.setBlock(new BlockPos(5, 3, 8), Blocks.AIR.defaultBlockState());
        BlockState MatLeft = World.getBlockState(Helper.absolutePos(Mat));
        Helper.assertTrue(MatLeft.getBlock() instanceof Scp407MatBlock && Scp407Faces.has(MatLeft, Direction.DOWN) && !Scp407Faces.has(MatLeft, Direction.WEST),
                "Removing the pillar did not strip the wall face from the mat: " + MatLeft);
        BlockState SprigLeft = World.getBlockState(Helper.absolutePos(Sprig));
        Helper.assertTrue(SprigLeft.getBlock() instanceof Scp407SprigBlock && !Scp407Faces.has(SprigLeft, Direction.WEST),
                "Removing the pillar did not strip the wall face from the sprig: " + SprigLeft);
        Helper.setBlock(new BlockPos(6, 2, 5), Blocks.AIR.defaultBlockState());
        Helper.assertTrue(World.getBlockState(Helper.absolutePos(Mat)).isAir(), "A mat with no supported face stayed");
        Helper.setBlock(new BlockPos(7, 2, 6), Blocks.AIR.defaultBlockState());
        Helper.assertTrue(World.getBlockState(Helper.absolutePos(Mid)).isAir(), "A floor mat stayed after its floor was removed");
        Helper.succeed();
    }
}
