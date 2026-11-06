package net.rai.scp_unleashed.powerbridge.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerBlock;
import net.rai.scp_unleashed.cooler.CoolerBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerRegistry;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletBlockEntity;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletRegistry;
import net.rai.scp_unleashed.powerbridge.PowerBridgeBlock;
import net.rai.scp_unleashed.powerbridge.PowerBridgeBlockEntity;
import net.rai.scp_unleashed.powerbridge.PowerBridgeConversion;
import net.rai.scp_unleashed.powerbridge.PowerBridgeKind;
import net.rai.scp_unleashed.powerbridge.PowerBridgeRegistry;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.WireTier;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class PowerBridgeGameTests {
    public static final String Arena = "wire_flow_arena";
    public static final int Settle = 60;
    public static final int Push = 100;
    public static final int Seed = 10_000;
    private static final BlockPos Near = new BlockPos(2, 0, 2);
    private static final BlockPos Far = new BlockPos(9, 0, 2);
    private static final BlockPos Second = new BlockPos(3, 0, 2);
    private static final BlockPos Third = new BlockPos(13, 0, 2);

    private PowerBridgeGameTests() {
    }

    private static final class MockSink implements IEnergyStorage {
        private final int Limit;
        private final boolean Open;
        private int Received;

        private MockSink(int Limit, boolean Open) {
            this.Limit = Limit;
            this.Open = Open;
        }

        @Override
        public int receiveEnergy(int ToReceive, boolean Simulate) {
            int Taken = Open ? Math.min(ToReceive, Limit) : 0;
            if (!Simulate) {
                Received += Taken;
            }
            return Taken;
        }

        @Override
        public int extractEnergy(int ToExtract, boolean Simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return Received;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return Open;
        }
    }

    private static int floorTop(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(0, Y, 0)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(0, Y + 1, 0)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static void link(OutletBlockEntity First, OutletBlockEntity Other, WireTier Tier) {
        int Slot = WireLink.freeSlot(First.getLinks(), Other.getLinks());
        First.addLink(new WireLink(Other.getBlockPos(), Other.getFacing(), Slot, Tier, false, null));
        Other.addLink(new WireLink(First.getBlockPos(), First.getFacing(), Slot, Tier, false, null));
    }

    private static PowerBridgeBlockEntity bridge(GameTestHelper Helper, BlockPos Rel, PowerBridgeKind Kind, Direction Front) {
        PowerBridgeBlock Bridge = Kind == PowerBridgeRegistry.ReceiverKind ? PowerBridgeRegistry.Receiver.get() : PowerBridgeRegistry.Emitter.get();
        Helper.setBlock(Rel, Bridge.defaultBlockState().setValue(PowerBridgeBlock.Facing, Front));
        return Helper.getBlockEntity(Rel);
    }

    private static PowerBridgeBlockEntity receiver(GameTestHelper Helper, BlockPos Rel, Direction Front) {
        return bridge(Helper, Rel, PowerBridgeKind.Inbound, Front);
    }

    private static PowerBridgeBlockEntity emitter(GameTestHelper Helper, BlockPos Rel, Direction Front) {
        return bridge(Helper, Rel, PowerBridgeKind.Outbound, Front);
    }

    private static OutletBlockEntity topOutlet(GameTestHelper Helper, BlockPos HostRel) {
        BlockPos Rel = HostRel.above();
        Helper.setBlock(Rel, ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.UP));
        return Helper.getBlockEntity(Rel);
    }

    private static CoolerBlockEntity cooler(GameTestHelper Helper, BlockPos Rel) {
        Helper.setBlock(Rel, CoolerRegistry.Cooler.get().defaultBlockState().setValue(CoolerBlock.Facing, Direction.SOUTH));
        return Helper.getBlockEntity(Rel);
    }

    private static CreativeOutletBlockEntity creative(GameTestHelper Helper, BlockPos Rel) {
        Helper.setBlock(Rel, CreativeOutletRegistry.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.UP));
        return Helper.getBlockEntity(Rel);
    }

    private static IEnergyStorage capability(GameTestHelper Helper, BlockPos Rel, Direction Side) {
        return Helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, Helper.absolutePos(Rel), Side);
    }

    private static WireNetwork network(GameTestHelper Helper, OutletBlockEntity Outlet) {
        return WireNetworkManager.get(Helper.getLevel()).networkAt(Helper.getLevel(), Outlet.getBlockPos());
    }

    private static int bankStored(GameTestHelper Helper, BlockPos Rel) {
        return WireNetworkManager.get(Helper.getLevel()).bankAt(Helper.getLevel(), Helper.absolutePos(Rel)).getStoredPower();
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "powerbridge_receiver")
    public static void receiverFeedsNetwork(GameTestHelper Helper) {
        int Base = floorTop(Helper);
        BlockPos BridgeRel = Near.above(Base);
        BlockPos MachineRel = Far.above(Base);
        PowerBridgeBlockEntity Bridge = receiver(Helper, BridgeRel, Direction.WEST);
        CoolerBlockEntity Machine = cooler(Helper, MachineRel);
        OutletBlockEntity BridgeOutlet = topOutlet(Helper, BridgeRel);
        link(BridgeOutlet, topOutlet(Helper, MachineRel), WireTier.Coated);
        long[] Pushed = new long[1];
        Helper.onEachTick(() -> {
            IEnergyStorage Port = capability(Helper, BridgeRel, Direction.WEST);
            if (Port != null) {
                Pushed[0] += Port.receiveEnergy(Push, false);
            }
        });
        Helper.runAtTickTime(Settle, () -> {
            Helper.assertTrue(Pushed[0] > 0, "the Receiver never accepted FE through its front");
            Helper.assertTrue(Machine.isActive(), "the cooler on the Receiver's network is not running");
            WireNetwork Network = network(Helper, BridgeOutlet);
            Helper.assertTrue(Network != null && Network.getLastSupply() > 0, "the Receiver offers no power to its network");
            Helper.assertTrue(Bridge.getShownUnits() >= CoolerBlockEntity.PowerDraw, "the Receiver reports " + Bridge.getShownUnits() + " SP/t, under the cooler's draw");
            Helper.assertTrue(Bridge.isActive(), "the Receiver is not lit while power flows");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 40, batch = "powerbridge_emitter")
    public static void emitterDeliversFe(GameTestHelper Helper) {
        int Base = floorTop(Helper);
        BlockPos SourceRel = Near.above(Base);
        BlockPos EmitterRel = new BlockPos(6, Base, 2);
        BlockPos ReceiverRel = new BlockPos(7, Base, 2);
        BlockPos MachineRel = Third.above(Base);
        CreativeOutletBlockEntity Source = creative(Helper, SourceRel);
        PowerBridgeBlockEntity Out = emitter(Helper, EmitterRel, Direction.EAST);
        PowerBridgeBlockEntity In = receiver(Helper, ReceiverRel, Direction.WEST);
        CoolerBlockEntity Machine = cooler(Helper, MachineRel);
        link(Source, topOutlet(Helper, EmitterRel), WireTier.Coated);
        link(topOutlet(Helper, ReceiverRel), topOutlet(Helper, MachineRel), WireTier.Coated);
        Helper.runAtTickTime(Settle + 20, () -> {
            Helper.assertTrue(Out.hasNeighbour(), "the Emitter does not see the Receiver on its front");
            Helper.assertTrue(Out.getShownUnits() > 0, "the Emitter draws nothing from its network");
            Helper.assertTrue(Out.getShownFe() > 0, "the Emitter sends no FE out of its front");
            Helper.assertTrue(In.getShownFe() > 0, "the Receiver in front of the Emitter takes in no FE");
            Helper.assertTrue(Machine.isActive(), "the cooler behind the Emitter and Receiver is not running");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "powerbridge_mock")
    public static void emitterRespectsConsumerLimit(GameTestHelper Helper) {
        int Base = floorTop(Helper);
        PowerBridgeBlockEntity Out = emitter(Helper, Near.above(Base), Direction.NORTH);
        Out.setBufferFe(1_000);
        MockSink Slow = new MockSink(70, true);
        Helper.assertTrue(Out.pushTo(Slow) == 70, "the Emitter pushed more than the consumer would take");
        Helper.assertTrue(Slow.getEnergyStored() == 70 && Out.getBufferFe() == 930, "the Emitter's buffer and the consumer disagree: " + Out.getBufferFe() + " / " + Slow.getEnergyStored());
        MockSink Closed = new MockSink(1_000, false);
        Helper.assertTrue(Out.pushTo(Closed) == 0 && Out.getBufferFe() == 930, "the Emitter pushed into a consumer that refuses FE");
        IEnergyStorage Port = capability(Helper, Near.above(Base), Direction.NORTH);
        Helper.assertTrue(Port != null && Port.extractEnergy(50, false) == 50 && Out.getBufferFe() == 880, "a pipe could not pull FE out of the Emitter's front");
        Helper.assertTrue(Port.receiveEnergy(500, false) == 0, "the Emitter's front took FE in");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "powerbridge_ratio")
    public static void conversionRatioAndLoss(GameTestHelper Helper) {
        Helper.assertTrue(PowerBridgeConversion.unitsFromFe(100) == 95, "100 FE should feed 95 SP, got " + PowerBridgeConversion.unitsFromFe(100));
        Helper.assertTrue(PowerBridgeConversion.feFromUnits(100) == 95, "100 SP should give 95 FE, got " + PowerBridgeConversion.feFromUnits(100));
        Helper.assertTrue(PowerBridgeConversion.LossPercent >= 5 && PowerBridgeConversion.LossPercent <= 10, "the loss each way is outside 5 to 10 percent");
        Helper.assertTrue(PowerBridgeConversion.costMilli(1) > PowerBridgeConversion.milli(1), "one SP costs no more than one FE");
        int Base = floorTop(Helper);
        PowerBridgeBlockEntity Out = emitter(Helper, Near.above(Base), Direction.NORTH);
        PowerBridgeBlockEntity In = receiver(Helper, Second.above(Base), Direction.NORTH);
        Out.insertPower(100);
        Helper.assertTrue(Out.getBufferMilli() == PowerBridgeConversion.milli(95), "100 SP into the Emitter stored " + Out.getBufferMilli() + " mFE, not 95 FE");
        In.setBufferFe(100);
        In.extractPower(95);
        Helper.assertTrue(In.getBufferMilli() == 0, "95 SP out of the Receiver left " + In.getBufferMilli() + " mFE of 100 FE");
        In.setBufferFe(100);
        In.extractPower(1);
        Helper.assertTrue(In.getBufferMilli() == PowerBridgeConversion.milli(100) - PowerBridgeConversion.costMilli(1), "one SP did not cost its rounded up price");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "powerbridge_loop_math")
    public static void roundTripAlwaysLoses(GameTestHelper Helper) {
        for (long Amount = 1; Amount <= 50_000; Amount++) {
            long Back = PowerBridgeConversion.feFromUnits(PowerBridgeConversion.unitsFromFe(Amount));
            long Again = PowerBridgeConversion.unitsFromFe(PowerBridgeConversion.feFromUnits(Amount));
            if (Back >= Amount || Again >= Amount) {
                Helper.fail("a loop gained or broke even at " + Amount + ": FE round trip " + Back + ", SP round trip " + Again);
                return;
            }
            if (PowerBridgeConversion.gainMilli(Amount) >= PowerBridgeConversion.costMilli(Amount)) {
                Helper.fail("SP bought back for less FE than it cost at " + Amount);
                return;
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "powerbridge_loop_live")
    public static void liveLoopDrains(GameTestHelper Helper) {
        int Base = floorTop(Helper);
        BlockPos InRel = Near.above(Base);
        BlockPos OutRel = Second.above(Base);
        PowerBridgeBlockEntity In = receiver(Helper, InRel, Direction.EAST);
        PowerBridgeBlockEntity Out = emitter(Helper, OutRel, Direction.WEST);
        link(topOutlet(Helper, InRel), topOutlet(Helper, OutRel), WireTier.Coated);
        In.setBufferFe(Seed);
        long[] Last = {PowerBridgeConversion.milli(Seed)};
        long[] Moved = new long[1];
        Helper.onEachTick(() -> {
            long Total = In.getBufferMilli() + Out.getBufferMilli();
            if (Total > Last[0]) {
                Helper.fail("the Receiver and Emitter loop gained energy: " + Last[0] + " mFE became " + Total);
            }
            Moved[0] += Out.getShownFe() + In.getShownUnits();
            Last[0] = Total;
        });
        Helper.runAtTickTime(120, () -> {
            long Total = In.getBufferMilli() + Out.getBufferMilli();
            Helper.assertTrue(Moved[0] > 0, "nothing circulated in the loop");
            Helper.assertTrue(Total < PowerBridgeConversion.milli(Seed), "the loop kept all of its energy");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "powerbridge_save")
    public static void buffersSaveAndLoad(GameTestHelper Helper) {
        int Base = floorTop(Helper);
        PowerBridgeBlockEntity In = receiver(Helper, Near.above(Base), Direction.NORTH);
        PowerBridgeBlockEntity Out = emitter(Helper, Second.above(Base), Direction.NORTH);
        In.setBufferFe(12_345);
        Out.insertPower(7);
        for (PowerBridgeBlockEntity Bridge : new PowerBridgeBlockEntity[]{In, Out}) {
            CompoundTag Saved = Bridge.saveWithFullMetadata(Helper.getLevel().registryAccess());
            BlockEntity Loaded = BlockEntity.loadStatic(Bridge.getBlockPos(), Bridge.getBlockState(), Saved, Helper.getLevel().registryAccess());
            Helper.assertTrue(Loaded instanceof PowerBridgeBlockEntity Copy && Copy.getBufferMilli() == Bridge.getBufferMilli() && Copy.getKind() == Bridge.getKind(),
                    "the " + Bridge.getKind().getSerializedName() + " bridge lost its buffer of " + Bridge.getBufferMilli() + " mFE through a save");
        }
        Helper.succeed();
    }

    private static void throughput(GameTestHelper Helper, WireTier Tier, int Cells) {
        int Base = floorTop(Helper);
        BlockPos BridgeRel = Near.above(Base);
        PowerBridgeBlockEntity In = receiver(Helper, BridgeRel, Direction.WEST);
        In.setBufferFe(PowerBridgeBlockEntity.BufferCapacity);
        for (int Cell = 0; Cell < Cells; Cell++) {
            Helper.setBlock(Far.above(Base).east(Cell % 4).south(Cell / 4), ModBlocks.Battery.get().defaultBlockState());
        }
        OutletBlockEntity BridgeOutlet = topOutlet(Helper, BridgeRel);
        OutletBlockEntity BankOutlet = topOutlet(Helper, Far.above(Base));
        link(BridgeOutlet, BankOutlet, Tier);
        int Limit = Math.min(Tier.getMaxDemand(), ModBlocks.Outlet.get().getConnectorTier().getMaxDemand());
        int[] Peak = new int[1];
        Helper.onEachTick(() -> {
            WireNetwork Network = network(Helper, BridgeOutlet);
            if (Network == null) {
                return;
            }
            Peak[0] = (int) Math.max(Peak[0], Network.getLastSupply());
            if (Network.getLastSupply() > Limit || In.getPowerOutput() > Limit) {
                Helper.fail("the Receiver offered " + Math.max(Network.getLastSupply(), In.getPowerOutput()) + " SP/t over a " + Tier.getSerializedName() + " wire rated " + Limit);
            }
        });
        Helper.runAtTickTime(40, () -> {
            Helper.assertTrue(In.getLineCapacity() == Limit, "the Receiver read its line as " + In.getLineCapacity() + " SP/t, not " + Limit);
            Helper.assertTrue(BridgeOutlet.isLinkedTo(BankOutlet.getBlockPos()), "the " + Tier.getSerializedName() + " wire burned out");
            Helper.assertTrue(Peak[0] == Limit, "the Receiver peaked at " + Peak[0] + " SP/t instead of filling its " + Limit + " SP/t line");
            Helper.assertTrue(bankStored(Helper, Far.above(Base)) > 0, "the battery bank got nothing");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "powerbridge_line_coated")
    public static void throughputFollowsCoatedWire(GameTestHelper Helper) {
        throughput(Helper, WireTier.Coated, 8);
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "powerbridge_line_advanced")
    public static void throughputFollowsAdvancedWire(GameTestHelper Helper) {
        throughput(Helper, WireTier.Advanced, 8);
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "powerbridge_line_emitter")
    public static void emitterDemandFollowsWire(GameTestHelper Helper) {
        int Base = floorTop(Helper);
        BlockPos EmitterRel = new BlockPos(6, Base, 2);
        PowerBridgeBlockEntity Out = emitter(Helper, EmitterRel, Direction.EAST);
        receiver(Helper, new BlockPos(7, Base, 2), Direction.WEST);
        link(creative(Helper, Near.above(Base)), topOutlet(Helper, EmitterRel), WireTier.Coated);
        int[] Peak = new int[1];
        Helper.onEachTick(() -> {
            int Demand = Out.getPowerDemand();
            Peak[0] = Math.max(Peak[0], Demand);
            if (Demand > WireTier.Coated.getMaxDemand()) {
                Helper.fail("the Emitter asked for " + Demand + " SP/t over a coated wire");
            }
        });
        Helper.runAtTickTime(Settle, () -> {
            Helper.assertTrue(Peak[0] == WireTier.Coated.getMaxDemand(), "the Emitter never filled its coated line, peak " + Peak[0]);
            Helper.assertTrue(Out.getBufferFe() > 0 && Out.getBufferFe() <= PowerBridgeBlockEntity.MinTarget, "the Emitter filled past its small working buffer: " + Out.getBufferFe());
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "powerbridge_idle")
    public static void noTransferWithoutPeers(GameTestHelper Helper) {
        int Base = floorTop(Helper);
        BlockPos LoneRel = Near.above(Base);
        BlockPos EmitterRel = new BlockPos(6, Base, 2);
        BlockPos ReceiverRel = new BlockPos(6, Base, 5);
        PowerBridgeBlockEntity Lone = receiver(Helper, LoneRel, Direction.WEST);
        PowerBridgeBlockEntity Out = emitter(Helper, EmitterRel, Direction.EAST);
        PowerBridgeBlockEntity In = receiver(Helper, ReceiverRel, Direction.EAST);
        link(creative(Helper, new BlockPos(10, Base, 2)), topOutlet(Helper, EmitterRel), WireTier.Coated);
        link(topOutlet(Helper, ReceiverRel), creative(Helper, new BlockPos(10, Base, 5)), WireTier.Coated);
        Helper.runAtTickTime(Settle, () -> {
            IEnergyStorage Port = capability(Helper, LoneRel, Direction.WEST);
            Helper.assertTrue(Port != null && !Port.canReceive() && Port.receiveEnergy(1_000, false) == 0, "a Receiver with no network still took FE");
            Helper.assertTrue(Lone.getBufferFe() == 0 && !Lone.isLinked(), "the unwired Receiver thinks it is wired");
            Helper.assertTrue(!Out.hasNeighbour() && Out.getPowerDemand() == 0 && Out.getBufferFe() == 0, "an Emitter with nothing on its front drew power: " + Out.getBufferFe() + " FE");
            Helper.assertTrue(!In.hasNeighbour() && In.getPowerOutput() == 0 && In.getBufferFe() == 0, "a Receiver with nothing on its front made power");
            Helper.assertTrue(!Out.isActive() && !In.isActive() && !Lone.isActive(), "an idle bridge is lit");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "powerbridge_faces")
    public static void capabilityOnFrontOnly(GameTestHelper Helper) {
        int Base = floorTop(Helper);
        BlockPos InRel = Near.above(Base);
        BlockPos OutRel = new BlockPos(6, Base, 2);
        receiver(Helper, InRel, Direction.EAST);
        emitter(Helper, OutRel, Direction.NORTH);
        for (Direction Side : Direction.values()) {
            IEnergyStorage InPort = capability(Helper, InRel, Side);
            IEnergyStorage OutPort = capability(Helper, OutRel, Side);
            Helper.assertTrue((InPort != null) == (Side == Direction.EAST), "the Receiver's FE port is wrong on its " + Side.getSerializedName() + " face");
            Helper.assertTrue((OutPort != null) == (Side == Direction.NORTH), "the Emitter's FE port is wrong on its " + Side.getSerializedName() + " face");
        }
        Helper.assertTrue(capability(Helper, InRel, null) == null && capability(Helper, OutRel, null) == null, "a bridge exposes FE without a side");
        IEnergyStorage InPort = capability(Helper, InRel, Direction.EAST);
        IEnergyStorage OutPort = capability(Helper, OutRel, Direction.NORTH);
        Helper.assertTrue(!InPort.canExtract() && OutPort.canExtract() && !OutPort.canReceive(), "the ports run the wrong way");
        BlockState FrontOutlet = ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.EAST);
        BlockState BackOutlet = ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.WEST);
        Helper.assertTrue(!FrontOutlet.canSurvive(Helper.getLevel(), Helper.absolutePos(InRel.east())), "an outlet fits on the Receiver's FE port");
        Helper.assertTrue(BackOutlet.canSurvive(Helper.getLevel(), Helper.absolutePos(InRel.west())), "an outlet does not fit on the Receiver's back");
        Helper.succeed();
    }
}
