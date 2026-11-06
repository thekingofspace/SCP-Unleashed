package net.rai.scp_unleashed.dclass.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassBeds;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.scp3008.furniture.bedroom.Scp3008FurnitureBBlock;
import net.rai.scp_unleashed.scp3008.furniture.children.Scp3008FurnitureCBlock;
import net.rai.scp_unleashed.zoneprops.PairPart;
import net.rai.scp_unleashed.zoneprops.ZonePairedPropBlock;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class DClassBedGameTests {
    public static final String Arena = "dclassroles_arena";
    public static final double Floor = 2.0;
    public static final int Ground = 2;
    public static final long Night = 13500L;
    public static final long Morning = 1000L;
    public static final int Awake = 400;
    public static final float Tolerance = 0.02f;

    private DClassBedGameTests() {
    }

    private static Block block(String Name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name));
    }

    private static void quiet(GameTestHelper Helper) {
        Helper.onEachTick(() -> {
            for (Monster Stray : Helper.getLevel().getEntitiesOfClass(Monster.class, Helper.getBounds().inflate(8.0))) {
                if ("minecraft".equals(EntityType.getKey(Stray.getType()).getNamespace())) {
                    Stray.discard();
                }
            }
        });
    }

    private static DClass person(GameTestHelper Helper, double X, double Z) {
        DClass Made = Helper.spawn(DClassRegistry.DClassType.get(), new Vec3(X, Floor, Z));
        Made.identify(DClassTrait.Stoic, DClassCrime.Smuggling, 0);
        Made.assignNumber();
        Made.tickCount = Awake;
        return Made;
    }

    private static void setTime(GameTestHelper Helper, long Time) {
        ServerLevel World = Helper.getLevel();
        World.setDayTime(World.getDayTime() - World.getDayTime() % 24000L + Time);
    }

    private static BlockPos bunk(GameTestHelper Helper, BlockPos Foot, Direction Facing) {
        Block Bunk = block("lcz_cell_bunk");
        BlockState Main = Bunk.defaultBlockState().setValue(ZonePairedPropBlock.FACING, Facing).setValue(ZonePairedPropBlock.Pair, PairPart.Main);
        Helper.setBlock(Foot, Main);
        Helper.setBlock(Foot.relative(Facing), Main.setValue(ZonePairedPropBlock.Pair, PairPart.Extension));
        return Helper.absolutePos(Foot.relative(Facing));
    }

    private static int sleepersIn(DClassBeds.Spot Bed, List<DClass> People) {
        int Count = 0;
        for (DClass Person : People) {
            if (Person.isSleeping() && Person.getSleepingPos().map(Bed.cells()::contains).orElse(false)) {
                Count++;
            }
        }
        return Count;
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "dclass_sleepsInBunk")
    public static void dclassSleepsInCellBunk(GameTestHelper Helper) {
        quiet(Helper);
        setTime(Helper, Night);
        BlockPos Foot = new BlockPos(12, Ground, 12);
        BlockPos Head = bunk(Helper, Foot, Direction.NORTH);
        DClass Sleeper = person(Helper, 15.5, 13.5);
        ServerLevel World = Helper.getLevel();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Sleeper.isSleeping(), "the Class-D never got into the cell bunk"))
                .thenExecute(() -> {
                    Helper.assertTrue(Sleeper.getSleepingPos().map(Head::equals).orElse(false), "the Class-D is not sleeping on the bunk head " + Sleeper.getSleepingPos());
                    Helper.assertFalse(Sleeper.isResting(), "the Class-D lies on the floor instead of the bunk");
                    Helper.assertTrue(Sleeper.getBedOrientation() == Direction.NORTH, "the Class-D does not lie along the bunk: " + Sleeper.getBedOrientation());
                    DClassBeds.Spot Bed = DClassBeds.sleepingSpot(Sleeper);
                    Helper.assertTrue(Bed != null && !Bed.Vanilla() && Helper.absolutePos(Foot).equals(Bed.Foot()), "the bunk is not resolved as a two block bed");
                    Helper.assertTrue(DClassBeds.claimedBy(World, Helper.absolutePos(Foot), Sleeper) && DClassBeds.claimedBy(World, Head, Sleeper), "both bunk halves are not claimed");
                    double Height = Sleeper.getY() - Head.getY();
                    Helper.assertTrue(Height > 0.5 && Height < 0.8, "the Class-D is not on the mattress: " + Height);
                })
                .thenIdle(60)
                .thenExecute(() -> Helper.assertTrue(Sleeper.isSleeping(), "the Class-D was thrown out of the cell bunk"))
                .thenExecute(() -> setTime(Helper, Morning))
                .thenWaitUntil(() -> Helper.assertFalse(Sleeper.isSleeping(), "the Class-D never woke up"))
                .thenExecute(() -> {
                    Helper.assertFalse(DClassBeds.claimedBy(World, Head, Sleeper), "the bunk claim was not released");
                    BlockPos Standing = Sleeper.blockPosition();
                    Helper.assertFalse(Standing.equals(Head) || Standing.equals(Helper.absolutePos(Foot)), "the Class-D stood up inside the bunk");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "dclass_bunkNotShared")
    public static void dclassDoNotShareOneBunk(GameTestHelper Helper) {
        quiet(Helper);
        setTime(Helper, Night);
        BlockPos Foot = new BlockPos(12, Ground, 12);
        BlockPos Head = bunk(Helper, Foot, Direction.EAST);
        DClass First = person(Helper, 12.5, 15.5);
        DClass Second = person(Helper, 13.5, 9.5);
        List<DClass> People = List.of(First, Second);
        DClassBeds.Spot Bed = DClassBeds.spotAt(Helper.getLevel(), Head);
        Helper.assertTrue(Bed != null, "the cell bunk has no sleeping spot");
        Helper.onEachTick(() -> Helper.assertTrue(sleepersIn(Bed, People) <= 1, "two Class-D share one bunk"));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(sleepersIn(Bed, People) == 1, "nobody got into the bunk"))
                .thenWaitUntil(() -> Helper.assertTrue(First.isRestingPose() && Second.isRestingPose(), "the second Class-D never settled down"))
                .thenIdle(40)
                .thenExecute(() -> {
                    Helper.assertTrue(sleepersIn(Bed, People) == 1, "the bunk lost its sleeper");
                    DClass Other = First.isSleeping() ? Second : First;
                    Helper.assertTrue(Other.isResting() && !Other.isSleeping(), "the Class-D without a bunk should lie on the floor");
                    setTime(Helper, Morning);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "dclass_vanillaBed")
    public static void dclassStillUseVanillaBeds(GameTestHelper Helper) {
        quiet(Helper);
        setTime(Helper, Night);
        BlockPos Foot = new BlockPos(12, Ground, 12);
        BlockState Bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH);
        Helper.setBlock(Foot, Bed.setValue(BedBlock.PART, BedPart.FOOT));
        Helper.setBlock(Foot.south(), Bed.setValue(BedBlock.PART, BedPart.HEAD));
        BlockPos Head = Helper.absolutePos(Foot.south());
        DClass Sleeper = person(Helper, 15.5, 11.5);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Sleeper.isSleeping(), "the Class-D never got into the vanilla bed"))
                .thenExecute(() -> {
                    Helper.assertTrue(Sleeper.getSleepingPos().map(Head::equals).orElse(false), "the Class-D is not sleeping on the bed head");
                    Helper.assertTrue(Helper.getLevel().getBlockState(Head).getValue(BedBlock.OCCUPIED), "the vanilla bed is not marked occupied");
                    Helper.assertTrue(Sleeper.getBedOrientation() == Direction.SOUTH, "the vanilla bed orientation changed");
                    setTime(Helper, Morning);
                })
                .thenWaitUntil(() -> Helper.assertFalse(Sleeper.isSleeping(), "the Class-D never left the vanilla bed"))
                .thenExecute(() -> Helper.assertFalse(Helper.getLevel().getBlockState(Head).getValue(BedBlock.OCCUPIED), "the vanilla bed stayed occupied"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "dclass_noBedFloor")
    public static void dclassWithoutBedLieOnFloor(GameTestHelper Helper) {
        quiet(Helper);
        setTime(Helper, Night);
        DClass Sleeper = person(Helper, 12.5, 12.5);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Sleeper.isResting(), "the Class-D without a bed never lay down"))
                .thenExecute(() -> {
                    Helper.assertFalse(Sleeper.isSleeping(), "the Class-D claims to sleep in a bed that does not exist");
                    setTime(Helper, Morning);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "dclass_bedSpots")
    public static void dclassBedSpotsResolve(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos BunkHead = bunk(Helper, new BlockPos(3, Ground, 3), Direction.EAST);
        DClassBeds.Spot Bunk = DClassBeds.spotAt(World, BunkHead.west());
        Helper.assertTrue(Bunk != null && Bunk.Head().equals(BunkHead) && Bunk.Toward() == Direction.EAST && !Bunk.Vanilla(), "the cell bunk foot does not resolve to its head");
        Helper.assertTrue(Math.abs(Bunk.Mattress() - 10.0f / 16.0f) < Tolerance, "the cell bunk mattress height is wrong: " + Bunk.Mattress());
        Block Hospital = block("hospital_bed");
        BlockState HospitalState = Hospital.defaultBlockState().setValue(BedBlock.FACING, Direction.WEST);
        Helper.setBlock(new BlockPos(8, Ground, 3), HospitalState.setValue(BedBlock.PART, BedPart.FOOT));
        Helper.setBlock(new BlockPos(7, Ground, 3), HospitalState.setValue(BedBlock.PART, BedPart.HEAD));
        DClassBeds.Spot HospitalSpot = DClassBeds.spotAt(World, Helper.absolutePos(new BlockPos(8, Ground, 3)));
        Helper.assertTrue(HospitalSpot != null && HospitalSpot.Vanilla() && HospitalSpot.Head().equals(Helper.absolutePos(new BlockPos(7, Ground, 3))),
                "the hospital bed is not treated as a real bed");
        Helper.assertTrue(World.getBlockState(Helper.absolutePos(new BlockPos(8, Ground, 3))).is(DClassBeds.Beds), "the hospital bed is missing from the tag");
        Block Single = block("scp3008_bedroom_single_bed");
        BlockPos SingleMain = Helper.absolutePos(new BlockPos(12, Ground, 4));
        Scp3008FurnitureBBlock.placeWhole(World, SingleMain, Single.defaultBlockState().setValue(Scp3008FurnitureBBlock.FACING, Direction.SOUTH));
        DClassBeds.Spot SingleSpot = DClassBeds.spotAt(World, SingleMain);
        Helper.assertTrue(SingleSpot != null && SingleSpot.Head().equals(SingleMain.north()) && SingleMain.equals(SingleSpot.Foot())
                && SingleSpot.Toward() == Direction.NORTH, "the single bed head is not at its headboard: " + SingleSpot);
        Block Bunks = block("scp3008_children_bunk_bed");
        BlockPos BunkMain = Helper.absolutePos(new BlockPos(16, Ground, 4));
        Scp3008FurnitureCBlock.placeWhole(World, BunkMain, Bunks.defaultBlockState().setValue(Scp3008FurnitureCBlock.FACING, Direction.SOUTH));
        DClassBeds.Spot Lower = DClassBeds.spotAt(World, BunkMain);
        DClassBeds.Spot Upper = DClassBeds.spotAt(World, BunkMain.above());
        Helper.assertTrue(Lower != null && Upper != null && Lower.Head().equals(BunkMain.north()) && Upper.Head().equals(BunkMain.north().above()),
                "the bunk bed does not offer an upper and a lower spot: " + Lower + " " + Upper);
        Block Frame = block("scp5001_hab_bunk_frame");
        BlockPos FramePos = Helper.absolutePos(new BlockPos(4, Ground, 10));
        BlockState FrameState = Frame.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        World.setBlock(FramePos, FrameState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
        World.setBlock(FramePos.above(), FrameState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        DClassBeds.Spot FrameLower = DClassBeds.spotAt(World, FramePos);
        DClassBeds.Spot FrameUpper = DClassBeds.spotAt(World, FramePos.above());
        Helper.assertTrue(FrameLower != null && FrameUpper != null && FrameLower.Foot() == null, "the bunk frame does not offer two one block spots");
        Block Pod = block("scp5001_hab_sleep_pod");
        BlockPos PodPos = Helper.absolutePos(new BlockPos(8, Ground, 10));
        BlockState PodState = Pod.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        World.setBlock(PodPos, PodState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
        World.setBlock(PodPos.above(), PodState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        DClassBeds.Spot PodSpot = DClassBeds.spotAt(World, PodPos);
        Helper.assertTrue(PodSpot != null && PodSpot.Mattress() < 0.5f, "the sleep pod has no spot inside it");
        Helper.assertTrue(World.getBlockState(PodPos.above()).is(Pod), "the sleep pod upper half is missing");
        for (String Name : List.of("scp5001_hab_med_cot", "scp5001_hab_bedroll", "scp5001_hab_med_bed_scanner", "scp5001_hab_rest_cradle")) {
            BlockPos Pos = Helper.absolutePos(new BlockPos(12, Ground, 10));
            Block Prop = block(Name);
            BlockState State = Prop.defaultBlockState();
            if (State.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                World.setBlock(Pos.above(), State.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
            } else {
                World.setBlock(Pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            World.setBlock(Pos, State, Block.UPDATE_CLIENTS);
            Helper.assertTrue(State.is(DClassBeds.Beds), Name + " is missing from the bed tag");
            Helper.assertTrue(DClassBeds.spotAt(World, Pos) != null, Name + " has no sleeping spot");
        }
        Helper.setBlock(new BlockPos(20, Ground, 20), Blocks.STONE);
        Helper.assertTrue(DClassBeds.spotAt(World, Helper.absolutePos(new BlockPos(20, Ground, 20))) == null, "stone counts as a bed");
        Helper.succeed();
    }
}
