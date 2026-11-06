package net.rai.scp_unleashed.scp001day.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.scp001day.Scp001DayDestination;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayCode;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayDressing;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;
import net.rai.scp_unleashed.scp001day.world.Scp001DayChunkGenerator;
import net.rai.scp_unleashed.scp001day.world.Scp001DayExtras;
import net.rai.scp_unleashed.scp001day.world.Scp001DayMend;
import net.rai.scp_unleashed.scp001day.world.Scp001DayPalette;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp001DayVoidGameTests {
    public static final int Around = 2;
    public static final int Built = 1;
    public static final int Checked = 64;
    public static final int[] Samples = {0, 5, 10, 15};
    public static final BlockPos Keepsake = new BlockPos(2, 57, 2);
    public static final BlockPos Bridge = new BlockPos(2, 30, 2);
    public static final BlockPos Stash = new BlockPos(3, 57, 2);
    private static final Map<ChunkPos, int[][]> Expected = new ConcurrentHashMap<>();

    private Scp001DayVoidGameTests() {
    }

    public static FakePlayer traveller(GameTestHelper Helper, String Name) {
        return GatewayGameTests.person(Helper, 4.5, 4.5, Name);
    }

    public static GatewayStash.Spot home(GameTestHelper Helper) {
        return new GatewayStash.Spot(Helper.getLevel().dimension(), Helper.absoluteVec(new Vec3(4.5, GatewayGameTests.Floor, 4.5)), 0.0f);
    }

    public static ChunkPos arrivalChunk() {
        return new ChunkPos(BlockPos.containing(Scp001DayDestination.arrivalSpot(0)));
    }

    public static String describe(ServerLevel World, ChunkPos Pos) {
        ChunkHolder Holder = World.getChunkSource().chunkMap.getVisibleChunkIfPresent(Pos.toLong());
        ChunkAccess Now = World.getChunkSource().getChunkNow(Pos.x, Pos.z);
        int Filled = 0;
        if (Now != null) {
            for (int Index = 0; Index < Now.getSectionsCount(); Index++) {
                Filled += Now.getSection(Index).hasOnlyAir() ? 0 : 1;
            }
        }
        return "chunk " + Pos.x + ", " + Pos.z + " status " + (Holder == null ? "none" : String.valueOf(Holder.getLatestStatus())) + " loaded " + (Now != null) + " filled sections " + Filled;
    }

    public static int mismatches(ServerLevel World, ChunkPos Pos, Set<BlockPos> Skipped) {
        LevelChunk Chunk = World.getChunkSource().getChunkNow(Pos.x, Pos.z);
        if (Chunk == null) {
            return -1;
        }
        int[][] Columns = Expected.computeIfAbsent(Pos, Key -> Scp001DayChunkGenerator.codes(Key.x, Key.z));
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        int Wrong = 0;
        for (int Lx : Samples) {
            for (int Lz : Samples) {
                int[] Column = Columns[Lx * 16 + Lz];
                for (int Y = 0; Y < Checked; Y++) {
                    BlockState Wanted = Scp001DayPalette.state(Column[Y]);
                    if (Column[Y] == Scp001DayCode.Air || Wanted.hasBlockEntity()) {
                        continue;
                    }
                    Cursor.set(Pos.getMinBlockX() + Lx, Y, Pos.getMinBlockZ() + Lz);
                    if (!Skipped.contains(Cursor) && !Chunk.getBlockState(Cursor).is(Wanted.getBlock())) {
                        Wrong++;
                    }
                }
            }
        }
        return Wrong;
    }

    public static void cityAt(GameTestHelper Helper, ServerLevel World, ChunkPos Pos) {
        int Wrong = mismatches(World, Pos, Set.of());
        Helper.assertTrue(Wrong == 0, "the city is missing in " + describe(World, Pos) + " (" + Wrong + " wrong blocks)");
    }

    public static void groundUnder(GameTestHelper Helper, ServerLevel World, Vec3 Feet) {
        BlockPos Column = BlockPos.containing(Feet);
        Helper.assertTrue(Scp001DayDestination.grounded(World, Feet), "no ground under the arrival at " + Column + " in " + describe(World, new ChunkPos(Column)));
        for (int Depth = 1; Depth <= Scp001DayDestination.GroundReach; Depth++) {
            BlockPos Below = Column.below(Depth);
            BlockState Found = World.getBlockState(Below);
            if (Found.isAir()) {
                continue;
            }
            BlockState Wanted = Scp001DayPalette.state(Scp001DayPlan.code(Below.getX(), Below.getY(), Below.getZ()));
            Helper.assertTrue(Found.is(Wanted.getBlock()), "the ground under the arrival is not the city: " + Found + " at " + Below);
            break;
        }
        BlockPos Floor = new BlockPos(Column.getX(), 0, Column.getZ());
        Helper.assertTrue(World.getBlockState(Floor).is(Blocks.BEDROCK), "no bedrock under the arrival at " + Floor);
    }

    public static void built(GameTestHelper Helper, ServerLevel World, int Radius) {
        ChunkPos Centre = arrivalChunk();
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                cityAt(Helper, World, new ChunkPos(Centre.x + Dx, Centre.z + Dz));
            }
        }
    }

    public static void sendable(GameTestHelper Helper, ServerLevel World) {
        ChunkPos Centre = arrivalChunk();
        Helper.assertTrue(World.getChunkSource().chunkMap.getChunkToSend(Centre.toLong()) != null, "the arrival chunk is never ready to send: " + describe(World, Centre));
    }

    public static void absent(GameTestHelper Helper, ServerLevel World) {
        ChunkPos Centre = arrivalChunk();
        for (int Dx = -Built; Dx <= Built; Dx++) {
            for (int Dz = -Built; Dz <= Built; Dz++) {
                Helper.assertTrue(World.getChunkSource().getChunkNow(Centre.x + Dx, Centre.z + Dz) == null, "the destination was already loaded: " + describe(World, new ChunkPos(Centre.x + Dx, Centre.z + Dz)));
            }
        }
    }

    public static Vec3 enter(GameTestHelper Helper, Scp001DayCityLevel.City Here, FakePlayer User) {
        Helper.assertTrue(GatewayTravel.enter(User, Here.Destination(), home(Helper)), "the gateway refused to enter");
        Helper.assertTrue(User.level() == Here.World(), "the traveller did not arrive in the city");
        Vec3 Arrival = User.position();
        Helper.assertTrue(Arrival.distanceTo(Scp001DayDestination.arrivalSpot(0)) < 3.0, "the traveller did not arrive at the entry point: " + Arrival);
        groundUnder(Helper, Here.World(), Arrival);
        built(Helper, Here.World(), Built);
        return Arrival;
    }

    public static void settled(GameTestHelper Helper, ServerLevel World, FakePlayer User) {
        built(Helper, World, Around);
        sendable(Helper, World);
        groundUnder(Helper, World, User.position());
    }

    public static void planAir(GameTestHelper Helper, BlockPos Pos) {
        Helper.assertTrue(Scp001DayPlan.code(Pos.getX(), Pos.getY(), Pos.getZ()) == Scp001DayCode.Air, "the keepsake spot is not open air in the plan: " + Pos);
    }

    @GameTest(template = Scp001DayGameTests.Arena, timeoutTicks = 6000, batch = "sdayvoid_freshEntry")
    public static void freshEntry(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Scp001DayCityLevel.City Here = Scp001DayCityLevel.city(Helper.getLevel().getServer(), "fresh");
            absent(Helper, Here.World());
            FakePlayer User = traveller(Helper, "sdayvoid_fresh");
            enter(Helper, Here, User);
            Helper.succeedWhen(() -> settled(Helper, Here.World(), User));
        });
    }

    @GameTest(template = Scp001DayGameTests.Arena, timeoutTicks = 12000, batch = "sdayvoid_restartEntry")
    public static void restartEntry(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        Scp001DayCityLevel.City[] Here = {Scp001DayCityLevel.city(Server, "restart")};
        FakePlayer First = traveller(Helper, "sdayvoid_before");
        FakePlayer Second = traveller(Helper, "sdayvoid_after");
        Helper.startSequence()
                .thenExecute(() -> {
                    enter(Helper, Here[0], First);
                    planAir(Helper, Keepsake);
                    Here[0].World().setBlock(Keepsake, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                })
                .thenWaitUntil(() -> settled(Helper, Here[0].World(), First))
                .thenExecute(() -> {
                    GatewayTravel.leave(First);
                    Helper.assertTrue(First.level() == Helper.getLevel(), "the first traveller did not leave the city");
                })
                .thenWaitUntil(() -> absent(Helper, Here[0].World()))
                .thenExecute(() -> {
                    Here[0] = new Scp001DayCityLevel.City(Here[0].Key(), Scp001DayCityLevel.restart(Server, Here[0].Key()), Here[0].Destination());
                    absent(Helper, Here[0].World());
                    enter(Helper, Here[0], Second);
                    Helper.assertTrue(Here[0].World().getBlockState(Keepsake).is(Blocks.GOLD_BLOCK), "a block placed before the restart was lost");
                })
                .thenWaitUntil(() -> settled(Helper, Here[0].World(), Second))
                .thenSucceed();
    }

    @GameTest(template = Scp001DayGameTests.Arena, timeoutTicks = 12000, batch = "sdayvoid_unloadedEntry")
    public static void unloadedEntry(GameTestHelper Helper) {
        Scp001DayCityLevel.City Here = Scp001DayCityLevel.city(Helper.getLevel().getServer(), "unloaded");
        FakePlayer First = traveller(Helper, "sdayvoid_scout");
        FakePlayer Second = traveller(Helper, "sdayvoid_late");
        Helper.startSequence()
                .thenExecute(() -> enter(Helper, Here, First))
                .thenWaitUntil(() -> settled(Helper, Here.World(), First))
                .thenExecute(() -> GatewayTravel.leave(First))
                .thenWaitUntil(() -> absent(Helper, Here.World()))
                .thenExecute(() -> enter(Helper, Here, Second))
                .thenWaitUntil(() -> settled(Helper, Here.World(), Second))
                .thenSucceed();
    }

    public static CompoundTag hollow(ServerLevel World, ChunkPos Pos, boolean Furnished) {
        LevelChunk Empty = new LevelChunk(World, Pos);
        if (Furnished) {
            Empty.getSection(Empty.getSectionIndex(Keepsake.getY())).setBlockState(Keepsake.getX() & 15, Keepsake.getY() & 15, Keepsake.getZ() & 15, Blocks.GOLD_BLOCK.defaultBlockState());
            Empty.getSection(Empty.getSectionIndex(Bridge.getY())).setBlockState(Bridge.getX() & 15, Bridge.getY() & 15, Bridge.getZ() & 15, Blocks.GOLD_BLOCK.defaultBlockState());
            BlockState Chest = Blocks.CHEST.defaultBlockState();
            Empty.getSection(Empty.getSectionIndex(Stash.getY())).setBlockState(Stash.getX() & 15, Stash.getY() & 15, Stash.getZ() & 15, Chest);
            ChestBlockEntity Box = new ChestBlockEntity(Stash, Chest);
            Box.setItem(0, new ItemStack(Items.DIAMOND, 3));
            Empty.setBlockEntity(Box);
        }
        Empty.setLightCorrect(true);
        return ChunkSerializer.write(World, Empty);
    }

    @GameTest(template = Scp001DayGameTests.Arena, timeoutTicks = 6000, batch = "sdayvoid_hollowChunkRepaired")
    public static void hollowChunkRepaired(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Scp001DayCityLevel.City Here = Scp001DayCityLevel.city(Helper.getLevel().getServer(), "hollow");
            ServerLevel World = Here.World();
            ChunkPos Centre = arrivalChunk();
            ChunkPos East = new ChunkPos(Centre.x + 1, Centre.z);
            planAir(Helper, Keepsake);
            planAir(Helper, Stash);
            Helper.assertTrue(Scp001DayPlan.code(Bridge.getX(), Bridge.getY(), Bridge.getZ()) != Scp001DayCode.Air, "the bridge spot is not solid in the plan");
            CompoundTag Furnished = hollow(World, Centre, true);
            CompoundTag Bare = hollow(World, East, false);
            Helper.assertTrue(Scp001DayMend.hollow(Furnished) && Scp001DayMend.hollow(Bare), "a chunk saved without its ground is not recognised");
            World.getChunkSource().chunkMap.write(Centre, Furnished).join();
            World.getChunkSource().chunkMap.write(East, Bare).join();
            int Before = Scp001DayMend.mended();
            FakePlayer User = traveller(Helper, "sdayvoid_hollow");
            enter(Helper, Here, User);
            Helper.assertTrue(Scp001DayMend.mended() >= Before + 2, "the hollow chunks were not rebuilt");
            Helper.assertTrue(mismatches(World, Centre, Set.of(Bridge)) == 0 && mismatches(World, East, Set.of()) == 0, "the rebuilt chunks do not match the city");
            Helper.assertTrue(World.getBlockState(Keepsake).is(Blocks.GOLD_BLOCK), "a block built in the hollow chunk was lost");
            Helper.assertTrue(World.getBlockState(Bridge).is(Blocks.GOLD_BLOCK), "a block built where the city has ground was overwritten");
            Helper.assertTrue(World.getBlockEntity(Stash) instanceof ChestBlockEntity Box && Box.getItem(0).is(Items.DIAMOND) && Box.getItem(0).getCount() == 3, "a chest built in the hollow chunk lost its contents");
            CompoundTag Whole = ChunkSerializer.write(World, World.getChunk(Centre.x, Centre.z));
            Helper.assertTrue(!Scp001DayMend.hollow(Whole), "a whole city chunk is treated as hollow");
            Helper.succeedWhen(() -> settled(Helper, World, User));
        });
    }

    public static ChunkPos framed(List<Scp001DayDressing.Extra> Found) {
        ChunkPos Best = null;
        List<Scp001DayDressing.Extra> Most = new ArrayList<>();
        for (int Cx = -4; Cx <= 4; Cx++) {
            for (int Cz = -4; Cz <= 4; Cz++) {
                List<Scp001DayDressing.Extra> Frames = new ArrayList<>();
                Scp001DayDressing.dress(Cx, Cz, (X, Y, Z, Code, Mode) -> {
                }, Item -> {
                    if (Item.Kind() == 0) {
                        Frames.add(Item);
                    }
                });
                if (Frames.size() > Most.size()) {
                    Most = Frames;
                    Best = new ChunkPos(Cx, Cz);
                }
            }
        }
        Found.addAll(Most);
        return Best;
    }

    @GameTest(template = Scp001DayGameTests.Arena, timeoutTicks = 6000, batch = "sdayvoid_framesNoStall")
    public static void framesNoStall(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Scp001DayCityLevel.City Here = Scp001DayCityLevel.city(Helper.getLevel().getServer(), "frames");
            ServerLevel World = Here.World();
            List<Scp001DayDressing.Extra> Planned = new ArrayList<>();
            ChunkPos Target = framed(Planned);
            Helper.assertTrue(Target != null && !Planned.isEmpty(), "no chunk near the arrival holds a left behind frame");
            World.setChunkForced(Target.x, Target.z, true);
            GatewayGameTests.whenDone(Helper, () -> World.setChunkForced(Target.x, Target.z, false));
            AABB Area = new AABB(Target.getMinBlockX(), World.getMinBuildHeight(), Target.getMinBlockZ(), Target.getMaxBlockX() + 1, World.getMaxBuildHeight(), Target.getMaxBlockZ() + 1);
            Helper.succeedWhen(() -> {
                cityAt(Helper, World, Target);
                List<ItemFrame> Frames = World.getEntitiesOfClass(ItemFrame.class, Area);
                Helper.assertTrue(!Frames.isEmpty(), "no left behind frames appeared in " + describe(World, Target));
                for (ItemFrame Frame : Frames) {
                    Scp001DayDressing.Extra Match = null;
                    for (Scp001DayDressing.Extra Item : Planned) {
                        if (Frame.getPos().equals(new BlockPos(Item.X(), Item.Y(), Item.Z()))) {
                            Match = Item;
                        }
                    }
                    Helper.assertTrue(Match != null, "a frame appeared where none was planned: " + Frame.getPos());
                    Helper.assertTrue(Frame.isInvisible() && Frame.getDirection() == Direction.UP && !Frame.getItem().isEmpty(), "a left behind frame lost its look at " + Frame.getPos());
                    Helper.assertTrue(Frame.getRotation() == Scp001DayExtras.rotation(Match), "a left behind frame has the wrong turn at " + Frame.getPos());
                }
            });
        });
    }
}
