package net.rai.scp_unleashed.steam.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.GasTankBlockEntity;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerBlock;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerRegistry;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlock;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlockEntity;
import net.rai.scp_unleashed.steam.MachineLayout;
import net.rai.scp_unleashed.steam.MachineLight;
import net.rai.scp_unleashed.steam.SteamMachineBlock;
import net.rai.scp_unleashed.steam.SteamRegistry;
import net.rai.scp_unleashed.steam.SteamTurbineBlock;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;
import net.rai.scp_unleashed.steam.TurbineStatus;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.WireTier;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class IndustrialTurbineGameTests {
    public static final String Arena = "industrial_turbine_arena";
    public static final int Settle = 460;
    public static final int Window = 40;
    public static final double Tolerance = 0.01;

    private IndustrialTurbineGameTests() {
    }

    private static int lift(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(0, Y, 0)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(0, Y + 1, 0)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static BlockPos origin(GameTestHelper Helper) {
        return new BlockPos(6, lift(Helper), 2);
    }

    private static BlockPos build(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MachineLayout Layout = IndustrialTurbineBlock.Layout;
        BlockPos Base = Helper.absolutePos(origin(Helper));
        BlockState First = SteamRegistry.IndustrialTurbine.get().defaultBlockState().setValue(SteamMachineBlock.Facing, Direction.NORTH);
        for (int Index = 0; Index < Layout.count(); Index++) {
            World.setBlock(Layout.cellPos(Base, Direction.NORTH, Index), First.setValue(IndustrialTurbineBlock.Part, Index), Block.UPDATE_ALL);
        }
        return Layout.cellPos(Base, Direction.NORTH, Layout.Master());
    }

    private static IndustrialTurbineBlockEntity machine(GameTestHelper Helper, BlockPos Master) {
        if (Helper.getLevel().getBlockEntity(Master) instanceof IndustrialTurbineBlockEntity Machine) {
            return Machine;
        }
        Helper.fail("the industrial turbine has no block entity at its master cell");
        return null;
    }

    private static BlockPos cell(GameTestHelper Helper, int Part) {
        return IndustrialTurbineBlock.Layout.cellPos(Helper.absolutePos(origin(Helper)), Direction.NORTH, Part);
    }

    private static void feed(GameTestHelper Helper, BlockPos Master, int[] Rates, boolean[] Running) {
        Helper.onEachTick(() -> {
            if (!(Helper.getLevel().getBlockEntity(Master) instanceof IndustrialTurbineBlockEntity Machine)) {
                return;
            }
            if (Running[0]) {
                for (int Index = 0; Index < Rates.length; Index++) {
                    if (Rates[Index] > 0) {
                        Machine.getInletTank(Index).fill(new GasStack(GasRegistry.Steam.get(), Rates[Index]), false);
                    }
                }
            }
            Machine.getExhaustTank().drain(Integer.MAX_VALUE, false);
            Machine.extractPower(Machine.getStoredPower());
        });
    }

    private static void expectOutput(GameTestHelper Helper, int[] Rates, int Expected) {
        BlockPos Master = build(Helper);
        long[] Total = new long[1];
        feed(Helper, Master, Rates, new boolean[]{true});
        Helper.onEachTick(() -> {
            if (Helper.getTick() > Settle - Window && Helper.getTick() <= Settle && Helper.getLevel().getBlockEntity(Master) instanceof IndustrialTurbineBlockEntity Machine) {
                Total[0] += Machine.getLastGenerated();
            }
        });
        Helper.runAtTickTime(Settle + 1, () -> {
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            double Average = (double) Total[0] / Window;
            Helper.assertTrue(Math.abs(Average - Expected) <= Math.max(1.0, Expected * Tolerance),
                    "feeding " + List.of(Rates[0], Rates[1], Rates[2]) + " steam a tick gave " + Average + " SP/t, expected " + Expected);
            Helper.assertTrue(Machine.getLastGenerated() <= IndustrialTurbineBlockEntity.MaxOutput, "the turbine went over its maximum output");
            Helper.assertTrue(Machine.getStatus() == TurbineStatus.Spinning, "a fed turbine with a load reports " + Machine.getStatus());
            Helper.succeed();
        });
    }

    private static void expectFill(GameTestHelper Helper, IndustrialTurbineBlockEntity Machine, int[] Amounts) {
        float Step = 1.0f / IndustrialTurbineBlockEntity.FillSteps;
        CompoundTag Synced = Machine.getUpdateTag(Helper.getLevel().registryAccess());
        IndustrialTurbineBlockEntity Copy = new IndustrialTurbineBlockEntity(Machine.getBlockPos(), Machine.getBlockState());
        Copy.handleUpdateTag(Synced, Helper.getLevel().registryAccess());
        for (int Inlet = 0; Inlet < Amounts.length; Inlet++) {
            float Expected = (float) Amounts[Inlet] / IndustrialTurbineBlockEntity.InletCapacity;
            Helper.assertTrue(Machine.getInletTank(Inlet).getAmount() == Amounts[Inlet], "inlet " + Inlet + " holds " + Machine.getInletTank(Inlet).getAmount() + " instead of " + Amounts[Inlet]);
            Helper.assertTrue(Math.abs(Machine.getFillTarget(Inlet) - Expected) <= Step / 2.0f + 1.0e-4f,
                    "inlet " + Inlet + " is " + Expected + " full but the turbine syncs " + Machine.getFillTarget(Inlet));
            Helper.assertTrue(Copy.getFillTarget(Inlet) == Machine.getFillTarget(Inlet), "the client copy of inlet " + Inlet + " reads " + Copy.getFillTarget(Inlet)
                    + " but the server sent " + Machine.getFillTarget(Inlet));
        }
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "industrial_turbine_fill")
    public static void syncedSteamFillTracksTheInlets(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Master = build(Helper);
        IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
        Machine.getExhaustTank().fill(new GasStack(GasRegistry.LowPressureSteam.get(), IndustrialTurbineBlockEntity.ExhaustCapacity), false);
        int[] First = {0, 2000, IndustrialTurbineBlockEntity.InletCapacity};
        int[] Second = {6000, 2000, 1000};
        for (int Inlet = 0; Inlet < First.length; Inlet++) {
            Machine.getInletTank(Inlet).fill(new GasStack(GasRegistry.Steam.get(), First[Inlet]), false);
        }
        IndustrialTurbineBlockEntity Viewer = new IndustrialTurbineBlockEntity(Master, World.getBlockState(Master));
        Helper.runAtTickTime(25, () -> {
            expectFill(Helper, Machine, First);
            Viewer.handleUpdateTag(Machine.getUpdateTag(World.registryAccess()), World.registryAccess());
            IndustrialTurbineBlockEntity.clientTick(World, Master, World.getBlockState(Master), Viewer);
            for (int Inlet = 0; Inlet < First.length; Inlet++) {
                Helper.assertTrue(Viewer.getRenderFill(Inlet, 1.0f) == Viewer.getFillTarget(Inlet), "a freshly loaded turbine faded its steam in instead of showing it");
            }
            Machine.getInletTank(0).fill(new GasStack(GasRegistry.Steam.get(), Second[0]), false);
            Machine.getInletTank(2).drain(First[2] - Second[2], false);
        });
        Helper.runAtTickTime(50, () -> {
            expectFill(Helper, Machine, Second);
            float Before = Viewer.getRenderFill(0, 1.0f);
            Viewer.handleUpdateTag(Machine.getUpdateTag(World.registryAccess()), World.registryAccess());
            IndustrialTurbineBlockEntity.clientTick(World, Master, World.getBlockState(Master), Viewer);
            float After = Viewer.getRenderFill(0, 1.0f);
            Helper.assertTrue(After > Before && After - Before < 0.1f, "the steam behind window 0 jumped from " + Before + " to " + After + " in one tick");
            for (int Tick = 0; Tick < 400; Tick++) {
                IndustrialTurbineBlockEntity.clientTick(World, Master, World.getBlockState(Master), Viewer);
            }
            for (int Inlet = 0; Inlet < Second.length; Inlet++) {
                Helper.assertTrue(Viewer.getRenderFill(Inlet, 1.0f) == Viewer.getFillTarget(Inlet), "the steam behind window " + Inlet + " never settled on the synced fill");
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "industrial_turbine_place")
    public static void itemPlacesTheWholeMachine(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "turbine_tester"));
        Vec3 Feet = Vec3.atBottomCenterOf(Helper.absolutePos(origin(Helper).north()));
        User.moveTo(Feet.x, Feet.y, Feet.z, 0.0f, 30.0f);
        ItemStack Held = new ItemStack(SteamRegistry.IndustrialTurbineItem.get());
        User.setItemInHand(InteractionHand.MAIN_HAND, Held);
        BlockPos Floor = Helper.absolutePos(origin(Helper).below());
        InteractionResult Result = Held.useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false)));
        Helper.runAfterDelay(2, () -> {
            Helper.assertTrue(Result.consumesAction(), "placing the turbine from its item gave " + Result + " facing " + User.getDirection() + ", origin holds " + World.getBlockState(Helper.absolutePos(origin(Helper))));
            MachineLayout Layout = IndustrialTurbineBlock.Layout;
            for (int Part = 0; Part < Layout.count(); Part++) {
                BlockState State = World.getBlockState(cell(Helper, Part));
                Helper.assertTrue(State.is(SteamRegistry.IndustrialTurbine.get()), "cell " + Part + " of the industrial turbine was not placed");
                Helper.assertTrue(State.getValue(IndustrialTurbineBlock.Part) == Part, "cell " + Part + " holds part " + State.getValue(IndustrialTurbineBlock.Part));
                Helper.assertTrue(State.getValue(SteamMachineBlock.Facing) == Direction.NORTH, "cell " + Part + " faces " + State.getValue(SteamMachineBlock.Facing));
            }
            Helper.assertTrue(Layout.Width() == 3 && Layout.Height() == 2 && Layout.Depth() == 2 * SteamTurbineBlock.Layout.Depth(), "the industrial turbine is not two turbines long");
            Helper.assertTrue(World.getBlockEntity(cell(Helper, Layout.Master())) instanceof IndustrialTurbineBlockEntity, "the master cell has no block entity");
            Helper.assertTrue(World.getBlockState(cell(Helper, Layout.count() - 1).south()).isAir(), "the machine spilled past its footprint");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "industrial_turbine_place")
    public static void breakingAnyPartDropsTheItem(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Master = build(Helper);
        machine(Helper, Master).getInletTank(1).fill(new GasStack(GasRegistry.Steam.get(), 500), false);
        BlockPos Far = cell(Helper, IndustrialTurbineBlock.Layout.index(2, 1, 4));
        World.destroyBlock(Far, true);
        Helper.runAfterDelay(2, () -> {
            for (int Part = 0; Part < IndustrialTurbineBlock.Layout.count(); Part++) {
                Helper.assertTrue(World.getBlockState(cell(Helper, Part)).isAir(), "cell " + Part + " was left behind after breaking the machine");
            }
            AABB Area = new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(12.0, 8.0, 11.0);
            List<ItemEntity> Drops = World.getEntitiesOfClass(ItemEntity.class, Area);
            int Count = 0;
            for (ItemEntity Drop : Drops) {
                Helper.assertTrue(Drop.getItem().is(SteamRegistry.IndustrialTurbineItem.get()), "breaking the turbine dropped " + Drop.getItem());
                Count += Drop.getItem().getCount();
            }
            Helper.assertTrue(Count == 1, "breaking the turbine dropped " + Count + " turbines");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "industrial_turbine_ports")
    public static void eachInletTakesSteam(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Master = build(Helper);
        IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
        IndustrialTurbineBlock Turbine = SteamRegistry.IndustrialTurbine.get();
        for (int Inlet = 0; Inlet < IndustrialTurbineBlock.InletPorts.size(); Inlet++) {
            MachineLayout.Port Port = IndustrialTurbineBlock.InletPorts.get(Inlet);
            BlockPos Pos = cell(Helper, Port.Part());
            BlockState State = World.getBlockState(Pos);
            Helper.assertTrue(Turbine.gasRole(State, Direction.UP) == PortRole.Input, "inlet " + Inlet + " does not take steam from above");
            GasHandler Handler = Turbine.gasHandler(World, Pos, State);
            Helper.assertTrue(Handler != null, "inlet " + Inlet + " has no gas handler");
            Helper.assertTrue(Handler.fill(new GasStack(GasRegistry.LowPressureSteam.get(), 100), true) == 0, "inlet " + Inlet + " takes low pressure steam");
            int Before = Machine.getInletTank(Inlet).getAmount();
            Helper.assertTrue(Handler.fill(new GasStack(GasRegistry.Steam.get(), 1000), false) == 1000, "inlet " + Inlet + " refused steam");
            Helper.assertTrue(Machine.getInletTank(Inlet).getAmount() == Before + 1000, "steam through inlet " + Inlet + " missed its own steam chest");
            Helper.assertTrue(Handler.drain(100, true).isEmpty(), "inlet " + Inlet + " lets steam back out");
        }
        for (MachineLayout.Port Port : IndustrialTurbineBlock.ExhaustPorts) {
            BlockPos Pos = cell(Helper, Port.Part());
            BlockState State = World.getBlockState(Pos);
            Helper.assertTrue(Turbine.gasRole(State, Direction.SOUTH) == PortRole.Output, "exhaust cell " + Port.Part() + " is not an output on the back");
            Helper.assertTrue(Turbine.gasRole(State, Direction.UP) == null, "exhaust cell " + Port.Part() + " also opens upwards");
        }
        Machine.getExhaustTank().fill(new GasStack(GasRegistry.LowPressureSteam.get(), 300), false);
        for (MachineLayout.Port Port : IndustrialTurbineBlock.ExhaustPorts) {
            BlockPos Pos = cell(Helper, Port.Part());
            GasHandler Handler = Turbine.gasHandler(World, Pos, World.getBlockState(Pos));
            Helper.assertTrue(Handler != null && Handler.drain(100, false).getAmount() == 100, "exhaust cell " + Port.Part() + " gave no low pressure steam");
            Helper.assertTrue(Handler.fill(new GasStack(GasRegistry.Steam.get(), 100), true) == 0, "exhaust cell " + Port.Part() + " takes steam in");
        }
        Helper.assertTrue(Turbine.gasRole(World.getBlockState(cell(Helper, IndustrialTurbineBlock.Layout.index(1, 1, 2))), Direction.UP) == null,
                "a plain casing cell acts as a port");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "industrial_turbine_ducts")
    public static void eachInletDrawsFromItsOwnVentLine(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Master = build(Helper);
        int Filled = 4000;
        GasTankBlockEntity[] Sources = new GasTankBlockEntity[IndustrialTurbineBlock.InletPorts.size()];
        for (int Inlet = 0; Inlet < Sources.length; Inlet++) {
            BlockPos Top = cell(Helper, IndustrialTurbineBlock.InletPorts.get(Inlet).Part());
            World.setBlock(Top.above(3), GasRegistry.GasTank.get().defaultBlockState(), Block.UPDATE_ALL);
            World.setBlock(Top.above(2).east(), ModBlocks.CreativePressurizer.get().defaultBlockState(), Block.UPDATE_ALL);
            for (int Step = 1; Step <= 2; Step++) {
                BlockPos Duct = Top.above(Step);
                World.setBlock(Duct, Block.updateFromNeighbourShapes(ModBlocks.Vent.get().defaultBlockState(), World, Duct), Block.UPDATE_ALL);
            }
            if (World.getBlockEntity(Top.above(3)) instanceof GasTankBlockEntity Tank) {
                Tank.getTank().fill(new GasStack(GasRegistry.Steam.get(), Filled), false);
                Sources[Inlet] = Tank;
            } else {
                Helper.fail("the steam tank for inlet " + Inlet + " has no block entity");
            }
        }
        Helper.runAtTickTime(140, () -> {
            for (int Inlet = 0; Inlet < Sources.length; Inlet++) {
                Helper.assertTrue(Sources[Inlet].getTank().getAmount() < Filled, "no steam left the tank feeding inlet " + Inlet);
            }
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            Helper.assertTrue(Machine.getStoredPower() > 0, "steam reached the inlets but the turbine made no power");
            Helper.assertTrue(Machine.getExhaustTank().getAmount() > 0, "spent steam never reached the exhaust");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "industrial_turbine_flow")
    public static void oneInletRunsAThird(GameTestHelper Helper) {
        expectOutput(Helper, new int[]{0, 150, 0}, IndustrialTurbineBlockEntity.InletFlow * IndustrialTurbineBlockEntity.PowerPerSteam);
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "industrial_turbine_flow")
    public static void partFlowScales(GameTestHelper Helper) {
        expectOutput(Helper, new int[]{50, 50, 50}, 150 * IndustrialTurbineBlockEntity.PowerPerSteam);
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "industrial_turbine_flow")
    public static void twoInletsRunTwoThirds(GameTestHelper Helper) {
        expectOutput(Helper, new int[]{100, 0, 100}, 2 * IndustrialTurbineBlockEntity.InletFlow * IndustrialTurbineBlockEntity.PowerPerSteam);
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "industrial_turbine_flow")
    public static void threeInletsReachFullOutput(GameTestHelper Helper) {
        expectOutput(Helper, new int[]{100, 100, 100}, IndustrialTurbineBlockEntity.MaxOutput);
    }

    @GameTest(template = Arena, timeoutTicks = Settle + 20, batch = "industrial_turbine_flow")
    public static void overfeedingStopsAtTheMaximum(GameTestHelper Helper) {
        expectOutput(Helper, new int[]{250, 250, 250}, IndustrialTurbineBlockEntity.MaxOutput);
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "industrial_turbine_idle")
    public static void noSteamMakesNoPower(GameTestHelper Helper) {
        BlockPos Master = build(Helper);
        long[] Made = new long[1];
        Helper.onEachTick(() -> {
            if (Helper.getLevel().getBlockEntity(Master) instanceof IndustrialTurbineBlockEntity Machine) {
                Made[0] += Machine.getLastGenerated();
            }
        });
        Helper.runAtTickTime(120, () -> {
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            Helper.assertTrue(Made[0] == 0, "a dry turbine made " + Made[0] + " SP");
            Helper.assertTrue(Machine.getStoredPower() == 0, "a dry turbine stored " + Machine.getStoredPower() + " SP");
            Helper.assertTrue(Machine.getMomentum() == 0.0f, "a dry rotor turns at " + Machine.getMomentum());
            Helper.assertTrue(Machine.getStatus() == TurbineStatus.Idle, "a dry turbine reports " + Machine.getStatus());
            BlockState MasterState = Helper.getLevel().getBlockState(Master);
            Helper.assertTrue(SteamRegistry.IndustrialTurbine.get().shownLight(Helper.getLevel(), Master, MasterState) == MachineLight.Off, "a dry turbine lights its status lamp");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "industrial_turbine_idle")
    public static void blockedExhaustStopsTheTurbine(GameTestHelper Helper) {
        BlockPos Master = build(Helper);
        IndustrialTurbineBlockEntity Start = machine(Helper, Master);
        Start.getExhaustTank().fill(new GasStack(GasRegistry.LowPressureSteam.get(), IndustrialTurbineBlockEntity.ExhaustCapacity), false);
        for (int Inlet = 0; Inlet < IndustrialTurbineBlockEntity.InletCount; Inlet++) {
            Start.getInletTank(Inlet).fill(new GasStack(GasRegistry.Steam.get(), IndustrialTurbineBlockEntity.InletCapacity), false);
        }
        Helper.runAtTickTime(100, () -> {
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            Helper.assertTrue(Machine.getLastGenerated() == 0, "a turbine with a full exhaust still makes " + Machine.getLastGenerated() + " SP/t");
            Helper.assertTrue(Machine.getStatus() == TurbineStatus.Backed, "a blocked turbine reports " + Machine.getStatus());
            Helper.assertTrue(Machine.getInletTank(0).getAmount() == IndustrialTurbineBlockEntity.InletCapacity, "a blocked turbine wasted steam");
            BlockState MasterState = Helper.getLevel().getBlockState(Master);
            Helper.assertTrue(SteamRegistry.IndustrialTurbine.get().shownLight(Helper.getLevel(), Master, MasterState) == MachineLight.Alert, "a blocked turbine does not show its alert lamp");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 880, batch = "industrial_turbine_spin")
    public static void heavyRotorSpinsUpAndCoastsDown(GameTestHelper Helper) {
        BlockPos Master = build(Helper);
        boolean[] Running = {true};
        feed(Helper, Master, new int[]{100, 100, 100}, Running);
        float[] Early = new float[2];
        int[] EarlyOutput = new int[1];
        Helper.runAtTickTime(40, () -> {
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            Early[0] = Machine.getMomentum();
            EarlyOutput[0] = Machine.getLastGenerated();
            Helper.assertTrue(EarlyOutput[0] > 0, "a fed turbine made nothing while starting");
            Helper.assertTrue(EarlyOutput[0] < IndustrialTurbineBlockEntity.MaxOutput / 2, "the heavy rotor reached " + EarlyOutput[0] + " SP/t within two seconds");
        });
        Helper.runAtTickTime(120, () -> {
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            Early[1] = Machine.getMomentum();
            Helper.assertTrue(Early[1] > Early[0], "the rotor did not keep speeding up: " + Early[0] + " then " + Early[1]);
            Helper.assertTrue(Machine.getLastGenerated() > EarlyOutput[0], "output did not grow with the rotor");
            Helper.assertTrue(Early[1] < 1.0f, "the rotor was already at full speed after six seconds");
        });
        Helper.runAtTickTime(400, () -> {
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            Helper.assertTrue(Machine.getMomentum() >= 0.999f, "the rotor only reached " + Machine.getMomentum() + " after twenty seconds");
            Helper.assertTrue(Machine.getLastGenerated() == IndustrialTurbineBlockEntity.MaxOutput, "a rotor at full speed made " + Machine.getLastGenerated() + " SP/t");
            Running[0] = false;
            for (int Inlet = 0; Inlet < IndustrialTurbineBlockEntity.InletCount; Inlet++) {
                Machine.getInletTank(Inlet).setGas(GasStack.EMPTY);
            }
        });
        Helper.runAtTickTime(410, () -> {
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            Helper.assertTrue(Machine.getLastGenerated() == 0, "the turbine kept making power with no steam");
            Helper.assertTrue(Machine.getMomentum() > 0.9f, "the heavy rotor stopped almost at once: " + Machine.getMomentum());
        });
        Helper.runAtTickTime(520, () -> {
            float Momentum = machine(Helper, Master).getMomentum();
            Helper.assertTrue(Momentum > 0.5f && Momentum < 0.85f, "after five seconds of coasting the rotor turns at " + Momentum);
        });
        Helper.runAtTickTime(860, () -> {
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            Helper.assertTrue(Machine.getMomentum() == 0.0f, "the rotor never came to rest: " + Machine.getMomentum());
            Helper.assertTrue(Machine.getStatus() == TurbineStatus.Idle, "a stopped turbine reports " + Machine.getStatus());
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "industrial_turbine_wire")
    public static void powerReachesAWiredLoad(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Master = build(Helper);
        BlockPos Terminal = cell(Helper, IndustrialTurbineBlock.Layout.index(0, 0, 0));
        BlockPos Socket = Terminal.west();
        BlockState Outlet = ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.WEST);
        Helper.assertTrue(Outlet.canSurvive(World, Socket), "an outlet cannot sit on the turbine's terminal box");
        World.setBlock(Socket, Outlet, Block.UPDATE_ALL);
        BlockPos Load = Helper.absolutePos(new BlockPos(1, lift(Helper), 2));
        World.setBlock(Load, SemanticBlockerRegistry.Blocker.get().defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(Load.above(), ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Direction.UP), Block.UPDATE_ALL);
        if (!(World.getBlockEntity(Socket) instanceof OutletBlockEntity Near) || !(World.getBlockEntity(Load.above()) instanceof OutletBlockEntity Far)) {
            Helper.fail("the outlets have no block entities");
            return;
        }
        int Slot = WireLink.freeSlot(Near.getLinks(), Far.getLinks());
        Near.addLink(new WireLink(Far.getBlockPos(), Far.getFacing(), Slot, WireTier.Advanced, false, null));
        Far.addLink(new WireLink(Near.getBlockPos(), Near.getFacing(), Slot, WireTier.Advanced, false, null));
        Helper.onEachTick(() -> {
            if (World.getBlockEntity(Master) instanceof IndustrialTurbineBlockEntity Machine) {
                Machine.getInletTank(0).fill(new GasStack(GasRegistry.Steam.get(), 20), false);
                Machine.getExhaustTank().drain(Integer.MAX_VALUE, false);
            }
        });
        Helper.runAtTickTime(240, () -> {
            WireNetwork Network = WireNetworkManager.get(World).networkAt(World, Socket);
            Helper.assertTrue(Network != null, "the turbine's outlet is not on a wire network");
            IndustrialTurbineBlockEntity Machine = machine(Helper, Master);
            Helper.assertTrue(Network.getLastOutput(Master) > 0, "the network took no power from the turbine: supply " + Network.getLastSupply() + ", demand " + Network.getLastDemand()
                    + ", devices " + Network.getLastDeviceCount() + ", hosts " + Network.findDevices(World).keySet() + ", master " + Master + ", stored " + Machine.getStoredPower()
                    + ", made " + Machine.getLastGenerated() + ", links " + Near.getLinks().size() + "/" + Far.getLinks().size() + ", load powered " + World.getBlockState(Load).getValue(SemanticBlockerBlock.Powered));
            Helper.assertTrue(World.getBlockState(Load).getValue(SemanticBlockerBlock.Powered), "the wired load is not powered by the turbine");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 280, batch = "industrial_turbine_normal")
    public static void normalTurbineIsUnchanged(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(SteamTurbineBlockEntity.MaxFlow == 100 && SteamTurbineBlockEntity.PowerPerSteam == 5 && SteamTurbineBlockEntity.MaxOutput == 500,
                "the steam turbine's output changed");
        Helper.assertTrue(SteamTurbineBlockEntity.Capacity == 50000 && SteamTurbineBlockEntity.InletCapacity == 8000 && SteamTurbineBlockEntity.ExhaustCapacity == 8000,
                "the steam turbine's tanks or buffer changed");
        MachineLayout Layout = SteamTurbineBlock.Layout;
        Helper.assertTrue(Layout.Width() == 3 && Layout.Height() == 2 && Layout.Depth() == 3 && Layout.Master() == Layout.index(1, 0, 0), "the steam turbine's shape changed");
        Helper.assertTrue(SteamTurbineBlock.InletPorts.equals(List.of(new MachineLayout.Port(Layout.index(1, 1, 0), Direction.UP), new MachineLayout.Port(Layout.index(1, 1, 1), Direction.UP))),
                "the steam turbine's inlets moved");
        Helper.assertTrue(SteamTurbineBlock.ExhaustPorts.equals(List.of(new MachineLayout.Port(Layout.index(1, 0, 2), Direction.SOUTH), new MachineLayout.Port(Layout.index(1, 1, 2), Direction.SOUTH))),
                "the steam turbine's exhausts moved");
        BlockPos Base = Helper.absolutePos(origin(Helper));
        BlockState First = SteamRegistry.SteamTurbine.get().defaultBlockState().setValue(SteamMachineBlock.Facing, Direction.NORTH);
        for (int Part = 0; Part < Layout.count(); Part++) {
            World.setBlock(Layout.cellPos(Base, Direction.NORTH, Part), First.setValue(SteamTurbineBlock.Part, Part), Block.UPDATE_ALL);
        }
        BlockPos Master = Layout.cellPos(Base, Direction.NORTH, Layout.Master());
        long[] Total = new long[1];
        int[] Early = new int[1];
        Helper.onEachTick(() -> {
            if (World.getBlockEntity(Master) instanceof SteamTurbineBlockEntity Machine) {
                Machine.getInletTank().fill(new GasStack(GasRegistry.Steam.get(), 100), false);
                Machine.getExhaustTank().drain(Integer.MAX_VALUE, false);
                Machine.extractPower(Machine.getStoredPower());
                if (Helper.getTick() == 3) {
                    Early[0] = Machine.getLastGenerated();
                }
                if (Helper.getTick() > 220 && Helper.getTick() <= 260) {
                    Total[0] += Machine.getLastGenerated();
                }
            }
        });
        Helper.runAtTickTime(262, () -> {
            double Average = Total[0] / 40.0;
            Helper.assertTrue(Math.abs(Average - SteamTurbineBlockEntity.MaxOutput) <= 5.0, "the steam turbine made " + Average + " SP/t on 100 steam a tick");
            Helper.assertTrue(Early[0] > 0, "the steam turbine no longer starts making power straight away");
            Helper.assertTrue(World.getBlockEntity(Master).getType() == SteamRegistry.SteamTurbineEntity.get(), "the steam turbine lost its own block entity");
            Helper.succeed();
        });
    }
}
