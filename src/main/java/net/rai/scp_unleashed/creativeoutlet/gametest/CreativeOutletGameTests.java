package net.rai.scp_unleashed.creativeoutlet.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerBlock;
import net.rai.scp_unleashed.cooler.CoolerBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerRegistry;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletBlock;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletBlockEntity;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletRegistry;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.WireTier;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class CreativeOutletGameTests {
    public static final String Arena = "wire_flow_arena";
    public static final int Settle = 40;
    private static final BlockPos SourceSpot = new BlockPos(2, 0, 2);
    private static final BlockPos MachineSpot = new BlockPos(8, 0, 2);

    private CreativeOutletGameTests() {
    }

    private static int floorTop(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(0, Y, 0)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(0, Y + 1, 0)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static void link(OutletBlockEntity First, OutletBlockEntity Second, WireTier Tier) {
        int Slot = WireLink.freeSlot(First.getLinks(), Second.getLinks());
        First.addLink(new WireLink(Second.getBlockPos(), Second.getFacing(), Slot, Tier, false, null));
        Second.addLink(new WireLink(First.getBlockPos(), First.getFacing(), Slot, Tier, false, null));
    }

    private static String describe(GameTestHelper Helper, BlockPos Where) {
        WireNetwork Network = WireNetworkManager.get(Helper.getLevel()).networkAt(Helper.getLevel(), Helper.absolutePos(Where));
        return Network == null ? " (no network)" : " (supply " + Network.getLastSupply() + ", demand " + Network.getLastDemand() + ", devices " + Network.getLastDeviceCount() + ")";
    }

    private static void powers(GameTestHelper Helper, WireTier Tier) {
        int Base = floorTop(Helper);
        BlockPos SourceRel = SourceSpot.above(Base);
        BlockPos MachineRel = MachineSpot.above(Base);
        BlockPos MachineOutletRel = MachineRel.above();
        Helper.setBlock(SourceRel, CreativeOutletRegistry.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.UP));
        Helper.setBlock(MachineRel, CoolerRegistry.Cooler.get().defaultBlockState().setValue(CoolerBlock.Facing, Direction.SOUTH));
        Helper.setBlock(MachineOutletRel, ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.UP));
        CreativeOutletBlockEntity Source = Helper.getBlockEntity(SourceRel);
        OutletBlockEntity MachineOutlet = Helper.getBlockEntity(MachineOutletRel);
        link(Source, MachineOutlet, Tier);
        Helper.runAtTickTime(Settle, () -> {
            Helper.assertBlockPresent(CreativeOutletRegistry.Outlet.get(), SourceRel);
            Helper.assertTrue(Source.isLinkedTo(MachineOutlet.getBlockPos()), "The " + Tier.getSerializedName() + " wire from the creative outlet is gone" + describe(Helper, SourceRel));
            CoolerBlockEntity Machine = Helper.getBlockEntity(MachineRel);
            Helper.assertTrue(Machine.isActive(), "The cooler on the creative outlet's network is not running" + describe(Helper, SourceRel));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "outlet_creative_powers")
    public static void creativeOutletPowersMachine(GameTestHelper Helper) {
        powers(Helper, WireTier.Coated);
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "outlet_creative_tether")
    public static void creativeOutletPowersOverTether(GameTestHelper Helper) {
        powers(Helper, WireTier.Tether);
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "outlet_creative_tiers")
    public static void creativeOutletTakesEveryWire(GameTestHelper Helper) {
        CreativeOutletBlock Outlet = CreativeOutletRegistry.Outlet.get();
        for (WireTier Tier : WireTier.values()) {
            Helper.assertTrue(Outlet.getConnectorTier().accepts(Tier), "The creative outlet refuses " + Tier.getSerializedName() + " wire");
        }
        Helper.assertTrue(CreativeOutletRegistry.OutletItem.get().getDefaultInstance().getRarity() == Rarity.EPIC, "The creative outlet is not marked as a creative item");
        Helper.succeed();
    }
}
