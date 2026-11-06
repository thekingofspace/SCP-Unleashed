package net.rai.scp_unleashed.fission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.block.PressurizerBlock;
import net.rai.scp_unleashed.gas.Gas;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetwork;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;

import java.util.ArrayList;
import java.util.List;

public final class ReactorExhaust {
    public static final double MouthReach = 1.0;

    private ReactorExhaust() {
    }

    public record Exhaust(int Connected, double Pressure, List<PressureNetwork> Networks, List<GasHandler> Direct, boolean Captures, List<Vec3> Mouths) {
        public static final Exhaust None = new Exhaust(0, 0.0, List.of(), List.of(), false, List.of());

        public boolean isVenting() {
            return !Mouths.isEmpty() || Captures;
        }

        public boolean hasSinks() {
            return Captures;
        }

        public int deliver(ServerLevel World, GasStack Offer) {
            if (Offer.isEmpty()) {
                return 0;
            }
            int Taken = 0;
            if (!Direct.isEmpty()) {
                List<PressureNetwork.Acceptor> Takers = new ArrayList<>();
                for (GasHandler Sink : Direct) {
                    Takers.add((Amount, Execute) -> Sink.fill(Offer.copyWithAmount(Amount), !Execute));
                }
                Taken += PressureNetwork.fairShare(Offer.getAmount(), Takers, true);
            }
            int Left = Networks.size();
            for (PressureNetwork Network : Networks) {
                int Remaining = Offer.getAmount() - Taken;
                if (Remaining <= 0) {
                    break;
                }
                int Share = Math.max(1, Remaining / Left);
                Left--;
                Taken += Network.offerGas(World, Offer.copyWithAmount(Left == 0 ? Remaining : Share), null);
            }
            return Math.min(Offer.getAmount(), Taken);
        }
    }

    public static Exhaust find(ServerLevel World, List<BlockPos> Ports) {
        PressureNetworkManager Manager = PressureNetworkManager.get(World);
        List<PressureNetwork> Networks = new ArrayList<>();
        List<GasHandler> Direct = new ArrayList<>();
        List<BlockPos> DirectAt = new ArrayList<>();
        for (BlockPos Port : Ports) {
            BlockPos Above = Port.above();
            if (!World.isLoaded(Above)) {
                continue;
            }
            BlockState State = World.getBlockState(Above);
            if (isVent(State)) {
                State = refresh(World, Above, State);
                if (linksDown(State)) {
                    add(Networks, Manager.networkAt(World, Above, DuctKind.Vent));
                }
            } else if (isVentPump(State)) {
                for (Direction Side : Direction.values()) {
                    if (Side == Direction.DOWN) {
                        continue;
                    }
                    BlockPos Next = Above.relative(Side);
                    if (!World.isLoaded(Next)) {
                        continue;
                    }
                    BlockState NextState = World.getBlockState(Next);
                    if (isVent(NextState) && NextState.getValue(DuctBlock.Connections.get(Side.getOpposite()))) {
                        add(Networks, Manager.networkAt(World, Next, DuctKind.Vent));
                    }
                }
            } else if (isDirectSink(State) && !DirectAt.contains(Above) && State.getBlock() instanceof GasPort Sink) {
                GasHandler Handler = Sink.gasHandler(World, Above, State);
                if (Handler != null) {
                    Direct.add(Handler);
                    DirectAt.add(Above.immutable());
                }
            }
        }
        if (Networks.isEmpty() && Direct.isEmpty()) {
            return Exhaust.None;
        }
        Gas Toxic = GasRegistry.ToxicGas.get();
        boolean Captures = false;
        for (GasHandler Sink : Direct) {
            if (Sink.fill(new GasStack(Toxic, 1), true) > 0) {
                Captures = true;
                break;
            }
        }
        double Pressure = 0.0;
        List<Vec3> Mouths = new ArrayList<>();
        for (PressureNetwork Network : Networks) {
            Pressure = Math.max(Pressure, Network.getPressure());
            if (Network.getPressure() < PressureNetwork.GasPressureMin) {
                continue;
            }
            if (!Captures && Network.canTakeGas(World, Toxic, null)) {
                Captures = true;
            }
            for (PressureNetwork.OpenEnd End : Network.getOpenEnds()) {
                Mouths.add(End.point(MouthReach));
            }
        }
        return new Exhaust(Networks.size() + Direct.size(), Pressure, Networks, Direct, Captures, Mouths);
    }

    private static boolean isDirectSink(BlockState State) {
        if (!(State.getBlock() instanceof GasPort Sink)) {
            return false;
        }
        PortRole Role = Sink.gasRole(State, Direction.DOWN);
        return Role != null && Role.canFill();
    }

    private static void add(List<PressureNetwork> Networks, PressureNetwork Network) {
        if (!Networks.contains(Network)) {
            Networks.add(Network);
        }
    }

    private static boolean isVent(BlockState State) {
        return State.getBlock() instanceof DuctBlock Duct && Duct.getKind() == DuctKind.Vent;
    }

    private static boolean isVentPump(BlockState State) {
        return State.getBlock() instanceof PressurizerBlock Pump && Pump.getKind() == DuctKind.Vent;
    }

    public static boolean linksDown(BlockState State) {
        return State.getValue(DuctBlock.Connections.get(Direction.DOWN));
    }

    private static BlockState refresh(ServerLevel World, BlockPos Pos, BlockState State) {
        BlockState Fresh = Block.updateFromNeighbourShapes(State, World, Pos);
        if (Fresh != State) {
            World.setBlock(Pos, Fresh, Block.UPDATE_ALL);
        }
        return Fresh;
    }

    public static boolean hooksUp(BlockGetter World, BlockPos Port) {
        BlockState State = World.getBlockState(Port.above());
        return isVent(State) ? linksDown(State) : isVentPump(State) || isDirectSink(State);
    }
}
