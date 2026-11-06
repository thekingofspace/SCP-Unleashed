package net.rai.scp_unleashed.wire.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerBlockEntity;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerRegistry;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.WirePair;
import net.rai.scp_unleashed.wire.WireTier;
import net.rai.scp_unleashed.wire.Wires;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class WireFlowGameTests {
    public static final String Arena = "wire_flow_arena";
    public static final int Settle = 60;
    public static final double Tolerance = 0.5;
    private static final BlockPos Source = new BlockPos(2, 0, 2);
    private static final BlockPos Junction = new BlockPos(10, 0, 2);
    private static final int LoadRow = 6;
    private static final int LoadSpacing = 2;

    private WireFlowGameTests() {
    }

    private record Rig(OutletBlockEntity LightNear, OutletBlockEntity LightFar, OutletBlockEntity HeavyNear, OutletBlockEntity HeavyFar, double Load) {
        boolean lightLinked() {
            return LightNear.isLinkedTo(LightFar.getBlockPos());
        }

        boolean heavyLinked() {
            return HeavyNear != null && HeavyNear.isLinkedTo(HeavyFar.getBlockPos());
        }
    }

    private static OutletBlockEntity outlet(GameTestHelper Helper, BlockPos HostRel, Direction Facing) {
        BlockPos Rel = HostRel.relative(Facing);
        Helper.setBlock(Rel, ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Facing));
        return Helper.getBlockEntity(Rel);
    }

    private static void link(OutletBlockEntity First, OutletBlockEntity Second, WireTier Tier) {
        int Slot = WireLink.freeSlot(First.getLinks(), Second.getLinks());
        First.addLink(new WireLink(Second.getBlockPos(), Second.getFacing(), Slot, Tier, false, null));
        Second.addLink(new WireLink(First.getBlockPos(), First.getFacing(), Slot, Tier, false, null));
    }

    private static int floorTop(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(0, Y, 0)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(0, Y + 1, 0)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static Rig build(GameTestHelper Helper, int Loads, WireTier Heavy) {
        int Base = floorTop(Helper);
        BlockPos Source = WireFlowGameTests.Source.above(Base);
        BlockPos Junction = WireFlowGameTests.Junction.above(Base);
        Helper.setBlock(Source, ModBlocks.CreativeEnergySource.get());
        Helper.setBlock(Junction, Blocks.IRON_BLOCK);
        List<BlockPos> Blockers = new ArrayList<>();
        for (int Index = 0; Index < Loads; Index++) {
            BlockPos Rel = new BlockPos(2 + Index * LoadSpacing, Base, LoadRow);
            Helper.setBlock(Rel, SemanticBlockerRegistry.Blocker.get());
            Blockers.add(Rel);
        }
        OutletBlockEntity LightNear = outlet(Helper, Source, Direction.EAST);
        OutletBlockEntity LightFar = outlet(Helper, Junction, Direction.WEST);
        OutletBlockEntity HeavyNear = Heavy == null ? null : outlet(Helper, Source, Direction.NORTH);
        OutletBlockEntity HeavyFar = Heavy == null ? null : outlet(Helper, Junction, Direction.NORTH);
        OutletBlockEntity[] Feeders = {outlet(Helper, Junction, Direction.UP), outlet(Helper, Junction, Direction.SOUTH)};
        List<OutletBlockEntity> Ends = new ArrayList<>();
        for (BlockPos Rel : Blockers) {
            Ends.add(outlet(Helper, Rel, Direction.UP));
        }
        link(LightNear, LightFar, WireTier.Coated);
        if (Heavy != null) {
            link(HeavyNear, HeavyFar, Heavy);
        }
        for (int Index = 0; Index < Ends.size(); Index++) {
            link(Feeders[Index / Wires.MaxLinksPerOutlet], Ends.get(Index), WireTier.Coated);
        }
        return new Rig(LightNear, LightFar, HeavyNear, HeavyFar, (double) Loads * SemanticBlockerBlockEntity.PowerDraw);
    }

    private static String describe(GameTestHelper Helper, Rig Setup) {
        WireNetwork Network = WireNetworkManager.get(Helper.getLevel()).networkAt(Helper.getLevel(), Setup.LightNear().getBlockPos());
        String Flow = Network == null ? "no network" : "supply " + Network.getLastSupply() + ", demand " + Network.getLastDemand() + ", loads " + Network.getWireLoads().values();
        return " (near outlet " + Helper.getLevel().getBlockState(Setup.LightNear().getBlockPos()).getBlock() + " with " + Setup.LightNear().getLinks().size() + " wires, " + Flow + ")";
    }

    private static double loadOn(GameTestHelper Helper, OutletBlockEntity First, OutletBlockEntity Second) {
        WireNetwork Network = WireNetworkManager.get(Helper.getLevel()).networkAt(Helper.getLevel(), First.getBlockPos());
        if (Network == null) {
            Helper.fail("the outlets are not on a network");
            return 0.0;
        }
        return Network.getWireLoads().getOrDefault(WirePair.of(First.getBlockPos(), Second.getBlockPos()), 0.0);
    }

    private static int loadsFor(double Power) {
        return (int) Math.ceil(Power / SemanticBlockerBlockEntity.PowerDraw);
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "wire_flow_shared")
    public static void mixedTiersShareByCapacity(GameTestHelper Helper) {
        double Light = WireTier.Coated.getMaxDemand();
        double Heavy = WireTier.Advanced.getMaxDemand();
        int Loads = loadsFor(Light * 2.0 + 1.0);
        Helper.assertTrue(Loads * SemanticBlockerBlockEntity.PowerDraw < (Light + Heavy) * 0.95, "the test load must fit the pair, retune the rig");
        Rig Setup = build(Helper, Loads, WireTier.Advanced);
        Helper.runAtTickTime(Settle, () -> {
            Helper.assertTrue(Setup.lightLinked(), "the coated wire is gone although the pair can carry " + Setup.Load() + " SP/t" + describe(Helper, Setup));
            Helper.assertTrue(Setup.heavyLinked(), "the advanced wire burnt");
            double Expected = Setup.Load() * Light / (Light + Heavy);
            double Measured = loadOn(Helper, Setup.LightNear(), Setup.LightFar());
            Helper.assertTrue(Math.abs(Measured - Expected) < Tolerance, "the coated wire carried " + Measured + " SP/t, expected " + Expected);
            Helper.assertTrue(Measured <= Light, "the coated wire is over its limit");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 60, batch = "wire_flow_overload")
    public static void mixedTierOverloadBurnsWeakest(GameTestHelper Helper) {
        double Light = WireTier.Coated.getMaxDemand();
        double Heavy = WireTier.Advanced.getMaxDemand();
        int Loads = loadsFor(Light + Heavy + 1.0);
        Helper.assertTrue(Loads <= Wires.MaxLinksPerOutlet * 2, "the overload needs more loads than the rig can feed");
        Rig Setup = build(Helper, Loads, WireTier.Advanced);
        Helper.succeedWhen(() -> Helper.assertFalse(Setup.lightLinked(), "the coated wire survived an overload of " + Setup.Load() + " SP/t"));
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "wire_flow_single")
    public static void singleRouteCarriesFullLoad(GameTestHelper Helper) {
        double Light = WireTier.Coated.getMaxDemand();
        int Loads = (int) Math.floor(Light / SemanticBlockerBlockEntity.PowerDraw);
        Helper.assertTrue(Loads >= 1, "one load already overloads a coated wire, retune the rig");
        Rig Setup = build(Helper, Loads, null);
        Helper.runAtTickTime(Settle, () -> {
            Helper.assertTrue(Setup.lightLinked(), "a single coated wire is gone under its limit" + describe(Helper, Setup));
            double Measured = loadOn(Helper, Setup.LightNear(), Setup.LightFar());
            Helper.assertTrue(Math.abs(Measured - Setup.Load()) < 0.01, "a single route carried " + Measured + " SP/t, expected " + Setup.Load());
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 60, batch = "wire_flow_single_overload")
    public static void singleRouteOverloadBurns(GameTestHelper Helper) {
        int Loads = loadsFor(WireTier.Coated.getMaxDemand() + 1.0);
        Rig Setup = build(Helper, Loads, null);
        Helper.succeedWhen(() -> Helper.assertFalse(Setup.lightLinked(), "a single coated wire survived " + Setup.Load() + " SP/t"));
    }
}
