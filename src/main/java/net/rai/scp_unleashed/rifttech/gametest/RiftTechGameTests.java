package net.rai.scp_unleashed.rifttech.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.anomaly.RiftFragment;
import net.rai.scp_unleashed.archive.ArchiveBlockEntity;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.ArchivePool;
import net.rai.scp_unleashed.archive.document.DocumentRoller;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;
import net.rai.scp_unleashed.extraction.RiftExtractorItem;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftRegistry;
import net.rai.scp_unleashed.rifttech.ApocalypticKeys;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.rifttech.CollectorStatus;
import net.rai.scp_unleashed.rifttech.GridField;
import net.rai.scp_unleashed.rifttech.GridStatus;
import net.rai.scp_unleashed.rifttech.GridTuning;
import net.rai.scp_unleashed.rifttech.InstabilityPylonBlock;
import net.rai.scp_unleashed.rifttech.InstabilityPylonBlockEntity;
import net.rai.scp_unleashed.rifttech.PylonGrid;
import net.rai.scp_unleashed.rifttech.PylonPart;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlock;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlockEntity;
import net.rai.scp_unleashed.rifttech.RiftTechRegistry;
import net.rai.scp_unleashed.scp001.Scp001Registry;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class RiftTechGameTests {
    public static final String Arena = "rifttech_arena";
    private static final List<BlockPos> Square = List.of(new BlockPos(4, 1, 4), new BlockPos(12, 1, 4), new BlockPos(4, 1, 12), new BlockPos(12, 1, 12));

    private RiftTechGameTests() {
    }

    private static void pylon(GameTestHelper Helper, BlockPos Rel) {
        BlockState Base = RiftTechRegistry.InstabilityPylon.get().defaultBlockState();
        Helper.setBlock(Rel, Base);
        Helper.setBlock(Rel.above(), Base.setValue(InstabilityPylonBlock.Part, PylonPart.Shaft));
        Helper.setBlock(Rel.above(2), Base.setValue(InstabilityPylonBlock.Part, PylonPart.Crown));
    }

    private static void collector(GameTestHelper Helper, BlockPos Rel, Direction Front) {
        BlockState Lower = RiftTechRegistry.RiftCollector.get().defaultBlockState().setValue(RiftCollectorBlock.Facing, Front);
        Helper.setBlock(Rel, Lower);
        Helper.setBlock(Rel.above(), Lower.setValue(RiftCollectorBlock.Half, DoubleBlockHalf.UPPER));
    }

    private static void feed(GameTestHelper Helper, List<BlockPos> Devices, IntPredicate Active) {
        Helper.onEachTick(() -> {
            if (!Active.test((int) Helper.getTick())) {
                return;
            }
            for (BlockPos Rel : Devices) {
                if (Helper.getLevel().getBlockEntity(Helper.absolutePos(Rel)) instanceof WireDevice Device) {
                    Device.insertPower(Device.getPowerDemand());
                }
            }
        });
    }

    private static void feed(GameTestHelper Helper, List<BlockPos> Devices) {
        feed(Helper, Devices, Tick -> true);
    }

    private static RiftEntity rift(GameTestHelper Helper, Vec3 Bottom, FragmentRarity Grade) {
        ServerLevel World = Helper.getLevel();
        RiftEntity Rift = RiftRegistry.Rift.get().create(World);
        Vec3 Spot = Helper.absoluteVec(Bottom);
        Rift.setRarity(Grade);
        Rift.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addFreshEntity(Rift);
        return Rift;
    }

    private static ApocalypticRiftEntity apocalypticRift(GameTestHelper Helper, Vec3 Bottom) {
        ServerLevel World = Helper.getLevel();
        ApocalypticRiftEntity Rift = RiftTechRegistry.ApocalypticRift.get().create(World);
        Vec3 Spot = Helper.absoluteVec(Bottom);
        Rift.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addFreshEntity(Rift);
        return Rift;
    }

    private static InstabilityPylonBlockEntity pylonAt(GameTestHelper Helper, BlockPos Rel) {
        return Helper.getBlockEntity(Rel);
    }

    private static List<RiftEntity> riftsInArena(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(RiftEntity.class, Helper.getBounds().inflate(4.0), RiftEntity::isAlive);
    }

    @GameTest(template = Arena, timeoutTicks = 400)
    public static void collectorCollectsRiftInRange(GameTestHelper Helper) {
        BlockPos Base = new BlockPos(15, 1, 4);
        collector(Helper, Base, Direction.SOUTH);
        feed(Helper, List.of(Base));
        RiftEntity Rift = rift(Helper, new Vec3(15.5, 2.0, 15.5), FragmentRarity.Faint);
        Helper.succeedWhen(() -> {
            RiftCollectorBlockEntity Collector = Helper.getBlockEntity(Base);
            Helper.assertTrue(Collector.countStored() == 1, "collector has not stored a fragment yet");
            Helper.assertTrue(Collector.getBuffer().getStackInSlot(0).is(ExtractionRegistry.fragmentFor(FragmentRarity.Faint)), "collector stored the wrong item");
            Helper.assertTrue(Rift.isRemoved() || Rift.isCollapsing(), "the rift is still open");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300)
    public static void collectorIgnoresRiftOutOfRange(GameTestHelper Helper) {
        BlockPos Base = new BlockPos(15, 1, 4);
        collector(Helper, Base, Direction.SOUTH);
        feed(Helper, List.of(Base));
        RiftEntity Rift = rift(Helper, new Vec3(15.5, 2.0, 25.5), FragmentRarity.Faint);
        Helper.runAtTickTime(160, () -> {
            RiftCollectorBlockEntity Collector = Helper.getBlockEntity(Base);
            Helper.assertTrue(Collector.countStored() == 0, "collector took a rift beyond its range");
            Helper.assertTrue(Rift.getExtraction() == 0.0f, "collector pulled on a rift beyond its range");
            Helper.assertTrue(Collector.getStatus() == CollectorStatus.Scanning, "collector should be scanning, was " + Collector.getStatus());
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 500)
    public static void collectorRangeUpgradeReachesFurther(GameTestHelper Helper) {
        BlockPos Base = new BlockPos(15, 1, 4);
        collector(Helper, Base, Direction.SOUTH);
        RiftCollectorBlockEntity Collector = Helper.getBlockEntity(Base);
        Collector.installUpgrade(UpgradeType.Range);
        feed(Helper, List.of(Base));
        rift(Helper, new Vec3(15.5, 2.0, 25.5), FragmentRarity.Faint);
        Helper.succeedWhen(() -> Helper.assertTrue(Collector.countStored() == 1, "upgraded collector has not collected the far rift"));
    }

    @GameTest(template = Arena, timeoutTicks = 300)
    public static void collectorStopsWhenUnpowered(GameTestHelper Helper) {
        BlockPos Base = new BlockPos(15, 1, 4);
        collector(Helper, Base, Direction.SOUTH);
        feed(Helper, List.of(Base), Tick -> Tick < 40);
        RiftEntity Rift = rift(Helper, new Vec3(15.5, 2.0, 13.5), FragmentRarity.Faint);
        float[] Peak = new float[1];
        Helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    Peak[0] = Rift.getExtraction();
                    Helper.assertTrue(Peak[0] > 0.1f, "collector never started pulling, extraction " + Peak[0]);
                })
                .thenExecuteAfter(110, () -> {
                    RiftCollectorBlockEntity Collector = Helper.getBlockEntity(Base);
                    Helper.assertTrue(Collector.getStatus() == CollectorStatus.Unpowered, "collector should be unpowered, was " + Collector.getStatus());
                    Helper.assertTrue(Collector.countStored() == 0, "collector finished a rift without power");
                    Helper.assertTrue(Rift.isAlive() && !Rift.isCollapsing(), "rift should still be open");
                    Helper.assertTrue(Rift.getExtraction() < Peak[0], "extraction kept rising without power");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200)
    public static void unpoweredCollectorDoesNothing(GameTestHelper Helper) {
        BlockPos Base = new BlockPos(15, 1, 4);
        collector(Helper, Base, Direction.SOUTH);
        RiftEntity Rift = rift(Helper, new Vec3(15.5, 2.0, 13.5), FragmentRarity.Faint);
        Helper.runAtTickTime(120, () -> {
            RiftCollectorBlockEntity Collector = Helper.getBlockEntity(Base);
            Helper.assertTrue(Collector.getStatus() == CollectorStatus.Unpowered, "collector should be unpowered");
            Helper.assertTrue(Rift.getExtraction() == 0.0f, "unpowered collector pulled on a rift");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200)
    public static void rectangleOfPoweredPylonsForms(GameTestHelper Helper) {
        Square.forEach(Rel -> pylon(Helper, Rel));
        feed(Helper, Square);
        Helper.succeedWhen(() -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            PylonGrid.Layout Grid = Master.getGrid();
            Helper.assertTrue(Grid != null, "no grid formed");
            Helper.assertTrue(Grid.SizeX() == 8 && Grid.SizeZ() == 8 && !Grid.isBox(), "wrong layout " + Grid);
            for (BlockPos Rel : Square) {
                Helper.assertTrue(pylonAt(Helper, Rel).viewStatus().isFormed(), "corner " + Rel + " does not see the grid, " + pylonAt(Helper, Rel).viewStatus());
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200)
    public static void lShapeDoesNotForm(GameTestHelper Helper) {
        List<BlockPos> Three = Square.subList(0, 3);
        Three.forEach(Rel -> pylon(Helper, Rel));
        feed(Helper, Three);
        Helper.runAtTickTime(100, () -> {
            InstabilityPylonBlockEntity First = pylonAt(Helper, Square.getFirst());
            Helper.assertTrue(First.getGrid() == null, "an L shape formed a grid");
            Helper.assertTrue(First.viewStatus() == GridStatus.Searching, "status should be searching, was " + First.viewStatus());
            Helper.assertTrue(First.viewIssue() == PylonGrid.Problem.MissingCorner, "issue should be a missing corner, was " + First.viewIssue());
            Helper.assertTrue(Helper.absolutePos(Square.get(3)).equals(First.viewIssueAt()), "missing corner reported at " + First.viewIssueAt());
            Helper.assertTrue(riftsInArena(Helper).isEmpty(), "rifts appeared");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200)
    public static void skewedLayoutDoesNotForm(GameTestHelper Helper) {
        List<BlockPos> Skewed = List.of(Square.get(0), Square.get(1), Square.get(2), new BlockPos(13, 1, 13));
        Skewed.forEach(Rel -> pylon(Helper, Rel));
        feed(Helper, Skewed);
        Helper.runAtTickTime(100, () -> {
            for (BlockPos Rel : Skewed) {
                Helper.assertTrue(pylonAt(Helper, Rel).getGrid() == null, "skewed pylon " + Rel + " formed a grid");
                Helper.assertTrue(!pylonAt(Helper, Rel).viewStatus().isFormed(), "skewed pylon " + Rel + " reports a formed grid");
            }
            Helper.assertTrue(pylonAt(Helper, Square.getFirst()).viewIssue() == PylonGrid.Problem.MissingCorner, "first corner should report the missing corner");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200)
    public static void unpoweredCornerBlocksGrid(GameTestHelper Helper) {
        Square.forEach(Rel -> pylon(Helper, Rel));
        feed(Helper, Square.subList(0, 3));
        Helper.runAtTickTime(100, () -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            Helper.assertTrue(Master.getGrid() == null, "a grid formed with an unpowered corner");
            Helper.assertTrue(Master.viewStatus() == GridStatus.CornerUnpowered, "status should be corner unpowered, was " + Master.viewStatus());
            Helper.assertTrue(Helper.absolutePos(Square.get(3)).equals(Master.viewIssueAt()), "unpowered corner reported at " + Master.viewIssueAt());
            Helper.assertTrue(pylonAt(Helper, Square.get(3)).viewStatus() == GridStatus.Unpowered, "dark corner should report unpowered");
            Helper.assertTrue(Master.getPowerDemand() <= GridTuning.StandbyDraw, "a broken grid should only draw standby power");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1200)
    public static void gridRiftsOnlyFormInside(GameTestHelper Helper) {
        Square.forEach(Rel -> pylon(Helper, Rel));
        feed(Helper, Square);
        Set<UUID> Seen = new HashSet<>();
        Helper.onEachTick(() -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            PylonGrid.Layout Grid = Master.getGrid();
            if (Grid == null) {
                return;
            }
            if (!Master.isEnabled()) {
                Master.toggleEnabled();
            }
            Master.setCharge(GridTuning.costFor(FragmentRarity.Abyssal, false));
            List<RiftEntity> Rifts = riftsInArena(Helper);
            for (RiftEntity Rift : Rifts) {
                if (Seen.add(Rift.getUUID()) && !GridField.contains(Grid, Rift.getBoundingBox())) {
                    Helper.fail("rift formed outside the grid at " + Rift.position());
                }
            }
            if (Master.getStatus() == GridStatus.Saturated && !Rifts.isEmpty()) {
                Helper.assertTrue(Rifts.size() == Grid.cap(), "saturated with " + Rifts.size() + " rifts");
                Rifts.forEach(RiftEntity::discard);
            }
        });
        Helper.succeedWhen(() -> Helper.assertTrue(Seen.size() >= 12, "only " + Seen.size() + " rifts formed"));
    }

    @GameTest(template = Arena, timeoutTicks = 600)
    public static void boxOfEightPylonsWorks(GameTestHelper Helper) {
        List<BlockPos> Corners = new ArrayList<>(Square);
        for (BlockPos Rel : Square) {
            Corners.add(Rel.above(6));
        }
        Corners.forEach(Rel -> pylon(Helper, Rel));
        feed(Helper, Corners);
        Set<UUID> Seen = new HashSet<>();
        Helper.onEachTick(() -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            PylonGrid.Layout Grid = Master.getGrid();
            if (Grid == null) {
                return;
            }
            if (!Master.isEnabled()) {
                Master.toggleEnabled();
            }
            Master.setCharge(GridTuning.costFor(FragmentRarity.Severe, true));
            for (RiftEntity Rift : riftsInArena(Helper)) {
                if (Seen.add(Rift.getUUID()) && !GridField.contains(Grid, Rift.getBoundingBox())) {
                    Helper.fail("rift formed outside the box at " + Rift.position());
                }
            }
        });
        Helper.succeedWhen(() -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            PylonGrid.Layout Grid = Master.getGrid();
            Helper.assertTrue(Grid != null && Grid.isBox() && Grid.Rise() == 6, "box did not form, " + Grid);
            Helper.assertTrue(Grid.corners().size() == 8, "box should have eight corners");
            for (BlockPos Rel : Corners) {
                Helper.assertTrue(pylonAt(Helper, Rel).viewStatus().isFormed(), "box corner " + Rel + " is not linked");
            }
            Helper.assertTrue(pylonAt(Helper, Square.getFirst().above(6)).getGrid() == null, "the upper level formed its own grid");
            Helper.assertTrue(Master.getStatus() == GridStatus.Saturated && Seen.size() == GridTuning.BoxCap, "box made " + Seen.size() + " rifts, status " + Master.getStatus());
        });
    }

    @GameTest(template = Arena, timeoutTicks = 600)
    public static void apocalypticChargeCountsEnergy(GameTestHelper Helper) {
        Square.forEach(Rel -> pylon(Helper, Rel));
        feed(Helper, Square);
        long[] Start = {-1L, -1L};
        Helper.onEachTick(() -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            if (Master.getGrid() == null) {
                return;
            }
            if (!Master.isEnabled()) {
                Master.toggleEnabled();
            }
            if (!Master.isApocalypticMode()) {
                Master.toggleApocalyptic();
            }
            if (Start[0] < 0L && Master.getStatus() == GridStatus.Apocalyptic && Master.getApocalypticCharge() > 0L) {
                Start[0] = Helper.getTick();
                Start[1] = Master.getApocalypticCharge();
            }
        });
        Helper.succeedWhen(() -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            Helper.assertTrue(Start[0] >= 0L && Helper.getTick() >= Start[0] + 200, "still waiting for 200 ticks of charge");
            long Gained = Master.getApocalypticCharge() - Start[1];
            long Expected = (Helper.getTick() - Start[0]) * GridTuning.ApocalypticDraw;
            Helper.assertTrue(Math.abs(Gained - Expected) <= 2L * GridTuning.ApocalypticDraw, "charge gained " + Gained + " expected about " + Expected);
            Helper.assertTrue(riftsInArena(Helper).isEmpty(), "a rift formed before the charge was complete");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300)
    public static void apocalypticChargeStarvesOnWeakPower(GameTestHelper Helper) {
        Square.forEach(Rel -> pylon(Helper, Rel));
        Helper.onEachTick(() -> {
            for (BlockPos Rel : Square) {
                InstabilityPylonBlockEntity Pylon = pylonAt(Helper, Rel);
                Pylon.insertPower(Math.min(Pylon.getPowerDemand(), 1500));
            }
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            if (Master.getGrid() != null) {
                if (!Master.isEnabled()) {
                    Master.toggleEnabled();
                }
                if (!Master.isApocalypticMode()) {
                    Master.toggleApocalyptic();
                }
            }
        });
        Helper.runAtTickTime(200, () -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            Helper.assertTrue(Master.getGrid() != null, "grid should still form on weak power");
            Helper.assertTrue(Master.getStatus() == GridStatus.Starved, "status should be starved, was " + Master.getStatus());
            Helper.assertTrue(Master.getApocalypticCharge() == 0L, "weak power built charge " + Master.getApocalypticCharge());
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 600)
    public static void apocalypticChargeCompletesIntoOneGoldenRift(GameTestHelper Helper) {
        Square.forEach(Rel -> pylon(Helper, Rel));
        feed(Helper, Square);
        long Remaining = 40L * GridTuning.ApocalypticDraw;
        long[] Primed = {-1L};
        long[] Born = {-1L};
        Helper.onEachTick(() -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            if (Master.getGrid() == null) {
                return;
            }
            if (!Master.isEnabled()) {
                Master.toggleEnabled();
            }
            if (!Master.isApocalypticMode()) {
                Master.toggleApocalyptic();
            }
            if (Primed[0] < 0L && Master.getStatus() == GridStatus.Apocalyptic) {
                Primed[0] = Helper.getTick();
                Master.setApocalypticCharge(GridTuning.ApocalypticCost - Remaining);
            }
            List<ApocalypticRiftEntity> Golden = Helper.getLevel().getEntitiesOfClass(ApocalypticRiftEntity.class, Helper.getBounds().inflate(4.0), ApocalypticRiftEntity::isAlive);
            if (!Golden.isEmpty() && Born[0] < 0L) {
                Born[0] = Helper.getTick();
            }
            if (Primed[0] >= 0L && Born[0] < 0L && Master.getApocalypticCharge() >= GridTuning.ApocalypticCost) {
                Helper.assertTrue(Helper.getTick() - Primed[0] >= 39, "charge filled too early");
            }
        });
        Helper.succeedWhen(() -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            Helper.assertTrue(Born[0] >= 0L && Helper.getTick() >= Born[0] + 60, "waiting for the golden rift");
            Helper.assertTrue(Born[0] - Primed[0] >= 39, "golden rift formed after only " + (Born[0] - Primed[0]) + " ticks");
            List<ApocalypticRiftEntity> Golden = Helper.getLevel().getEntitiesOfClass(ApocalypticRiftEntity.class, Helper.getBounds().inflate(4.0), ApocalypticRiftEntity::isAlive);
            Helper.assertTrue(Golden.size() == 1, "expected one golden rift, found " + Golden.size());
            Helper.assertTrue(GridField.contains(Master.getGrid(), Golden.getFirst().getBoundingBox()), "golden rift is outside the grid");
            Helper.assertTrue(Master.getApocalypticCharge() == 0L, "charge did not reset");
            Helper.assertTrue(Master.getStatus() == GridStatus.Holding, "grid should hold while the golden rift is open, was " + Master.getStatus());
        });
    }

    @GameTest(template = Arena, timeoutTicks = 16500)
    public static void apocalypticFullChargeTakesTheWholeCost(GameTestHelper Helper) {
        Square.forEach(Rel -> pylon(Helper, Rel));
        feed(Helper, Square);
        long[] Started = {-1L};
        long[] Born = {-1L};
        long Needed = GridTuning.ApocalypticCost / GridTuning.ApocalypticDraw;
        Helper.onEachTick(() -> {
            InstabilityPylonBlockEntity Master = pylonAt(Helper, Square.getFirst());
            if (Master.getGrid() == null) {
                return;
            }
            if (!Master.isEnabled()) {
                Master.toggleEnabled();
            }
            if (!Master.isApocalypticMode()) {
                Master.toggleApocalyptic();
            }
            if (Started[0] < 0L && Master.getApocalypticCharge() > 0L) {
                Started[0] = Helper.getTick();
            }
            boolean Exists = !Helper.getLevel().getEntitiesOfClass(ApocalypticRiftEntity.class, Helper.getBounds().inflate(4.0), ApocalypticRiftEntity::isAlive).isEmpty();
            if (Exists && Born[0] < 0L) {
                Born[0] = Helper.getTick();
                Helper.assertTrue(Born[0] - Started[0] >= Needed - 2, "golden rift formed after " + (Born[0] - Started[0]) + " ticks, needs " + Needed);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Born[0] >= 0L, "golden rift has not formed yet");
            Helper.assertTrue(Born[0] - Started[0] <= Needed + 40, "golden rift took " + (Born[0] - Started[0]) + " ticks");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1000)
    public static void collectorTakesGoldenRift(GameTestHelper Helper) {
        BlockPos Base = new BlockPos(15, 1, 4);
        collector(Helper, Base, Direction.SOUTH);
        feed(Helper, List.of(Base));
        ApocalypticRiftEntity Rift = apocalypticRift(Helper, new Vec3(15.5, 1.3, 11.5));
        int[] Strikes = {0};
        Helper.onEachTick(() -> Strikes[0] = Math.max(Strikes[0], Rift.getStrikeCount()));
        Helper.succeedWhen(() -> {
            RiftCollectorBlockEntity Collector = Helper.getBlockEntity(Base);
            ItemStack Stored = Collector.getBuffer().getStackInSlot(0);
            Helper.assertTrue(Strikes[0] >= 10, "the golden rift only struck " + Strikes[0] + " times while the collector pulled it");
            Helper.assertTrue(Stored.is(RiftTechRegistry.ApocalypticFragment.get()), "collector has not stored the golden fragment, holds " + Stored);
            Helper.assertTrue(ApocalypticKeys.isKey(Stored), "golden fragment is missing from the apocalyptic fragment tag");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1000)
    public static void extractorTakesGoldenRift(GameTestHelper Helper) {
        ApocalypticRiftEntity Rift = apocalypticRift(Helper, new Vec3(15.5, 1.2, 15.5));
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 Stand = Helper.absoluteVec(new Vec3(15.5, 1.0, 11.0));
        User.moveTo(Stand.x, Stand.y, Stand.z, 0.0f, 0.0f);
        ItemStack Extractor = new ItemStack(ExtractionRegistry.RiftExtractor.get());
        User.setItemInHand(InteractionHand.MAIN_HAND, Extractor);
        Helper.onEachTick(() -> {
            if (Rift.isAlive() && !Rift.isCollapsing()) {
                ((RiftExtractorItem) Extractor.getItem()).onUseTick(Helper.getLevel(), User, Extractor, RiftExtractorItem.UseDuration);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(User.getInventory().countItem(RiftTechRegistry.ApocalypticFragment.get()) == 1, "player has no golden fragment");
            Helper.assertTrue(Rift.getStrikeCount() >= 10, "the golden rift only struck " + Rift.getStrikeCount() + " times while being extracted");
            Helper.assertTrue(User.getInventory().countItem(ExtractionRegistry.fragmentFor(FragmentRarity.Abyssal)) == 0, "player got an abyssal fragment instead");
            Helper.assertTrue(Helper.getTick() >= 500, "golden rift came out too fast, after " + Helper.getTick() + " ticks");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300)
    public static void idleGoldenRiftNeverStrikes(GameTestHelper Helper) {
        ApocalypticRiftEntity Rift = apocalypticRift(Helper, new Vec3(15.5, 1.3, 15.5));
        Helper.runAtTickTime(240, () -> {
            Helper.assertTrue(Rift.isAlive(), "golden rift vanished");
            Helper.assertTrue(Rift.getStrikeCount() == 0, "an untouched golden rift struck " + Rift.getStrikeCount() + " times");
            Helper.succeed();
        });
    }

    private static int countFires(GameTestHelper Helper) {
        int Fires = 0;
        for (BlockPos Rel : BlockPos.betweenClosed(0, -1, 0, 29, 4, 29)) {
            if (Helper.getBlockState(Rel).getBlock() instanceof BaseFireBlock) {
                Fires++;
            }
        }
        return Fires;
    }

    private static void strikeFloor(GameTestHelper Helper, ApocalypticRiftEntity Rift, int Count) {
        for (int Index = 0; Index < Count; Index++) {
            double X = 2.5 + (Index % 8) * 3.0;
            double Z = 2.5 + (Index / 8) * 3.0;
            Rift.strikeAt(Helper.absoluteVec(new Vec3(X, 0.0, Z)));
        }
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "rifttech_grief_off")
    public static void goldenStrikesSpareBlocksWithoutMobGriefing(GameTestHelper Helper) {
        GameRules.BooleanValue Rule = Helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean Before = Rule.get();
        Rule.set(false, Helper.getLevel().getServer());
        try {
            ApocalypticRiftEntity Rift = apocalypticRift(Helper, new Vec3(15.5, 6.0, 15.5));
            strikeFloor(Helper, Rift, 64);
            Helper.assertTrue(Rift.getStrikeCount() == 64, "strikes were not counted");
            Helper.assertTrue(countFires(Helper) == 0, "golden strikes lit fires with mobGriefing off");
        } finally {
            Rule.set(Before, Helper.getLevel().getServer());
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "rifttech_grief_on")
    public static void goldenStrikesNeverLightFires(GameTestHelper Helper) {
        GameRules.BooleanValue Rule = Helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean Before = Rule.get();
        Rule.set(true, Helper.getLevel().getServer());
        try {
            ApocalypticRiftEntity Rift = apocalypticRift(Helper, new Vec3(15.5, 6.0, 15.5));
            strikeFloor(Helper, Rift, 64);
            Helper.assertTrue(countFires(Helper) == 0, "golden strikes lit fires with mobGriefing on");
        } finally {
            Rule.set(Before, Helper.getLevel().getServer());
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100)
    public static void goldenStrikeHurtsAndBurns(GameTestHelper Helper) {
        Pig Victim = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, 1.0, 6.5));
        float Full = Victim.getHealth();
        ApocalypticRiftEntity Rift = apocalypticRift(Helper, new Vec3(15.5, 1.3, 15.5));
        Rift.strikeAt(Victim.position());
        Helper.assertTrue(Victim.getHealth() < Full, "the struck pig took no damage");
        Helper.assertTrue(Victim.getRemainingFireTicks() > 0, "the struck pig is not burning");
        Helper.assertTrue(Victim.getType() == EntityType.PIG, "the pig was transformed");
        Helper.succeed();
    }

    @GameTest(template = Arena)
    public static void goldenFragmentOpensOnlyTheScp001File(GameTestHelper Helper) {
        ItemStack Golden = new ItemStack(RiftTechRegistry.ApocalypticFragment.get());
        Helper.assertTrue(!(Golden.getItem() instanceof RiftFragment), "golden fragment must not count as a rift fragment");
        Helper.assertTrue(ArchiveBlockEntity.isFragment(Golden), "the archive must accept the golden fragment");
        Helper.assertTrue(ArchivePool.poolOf(Golden) == ArchivePool.Apocalyptic, "the golden fragment must read from the apocalyptic pool");
        Helper.assertTrue(ApocalypticKeys.isKey(Golden), "golden fragment must carry the apocalyptic tag");
        Helper.assertTrue(!ApocalypticKeys.isKey(new ItemStack(ExtractionRegistry.fragmentFor(FragmentRarity.Abyssal))), "abyssal fragments must not count as apocalyptic keys");
        List<ResourceLocation> Gold = AnomalyDocuments.server().entrySet().stream().filter(Entry -> Entry.getValue().Pool() == ArchivePool.poolOf(Golden)).map(Map.Entry::getKey).toList();
        Map<ResourceLocation, AnomalyDocument> Abyss = DocumentRoller.eligible(Helper.getLevel().getServer(), null, ArchivePool.poolOf(new ItemStack(ExtractionRegistry.fragmentFor(FragmentRarity.Abyssal))));
        Helper.assertTrue(Gold.contains(Scp001Registry.DocumentId) && Gold.stream().allMatch(Id -> Id.getPath().startsWith("scp_001")), "the golden fragment must roll only the SCP-001 files: " + Gold);
        Helper.assertTrue(!Abyss.containsKey(Scp001Registry.DocumentId), "abyssal fragments must never roll the SCP-001 file");
        Helper.succeed();
    }
}
