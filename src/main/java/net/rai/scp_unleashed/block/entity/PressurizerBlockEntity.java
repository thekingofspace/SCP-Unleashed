package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.block.PressurizerBlock;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.pressure.PressureNetwork;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.registry.ModParticles;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PressurizerBlockEntity extends BlockEntity implements WireDevice {
    public static final double Hysteresis = 0.97;
    public static final int ActiveHold = 10;
    public static final int BreatheTicks = 3;

    private int Received;
    private boolean Wanting;
    private long LastPumped = Long.MIN_VALUE / 2;
    private double LastPressure;
    private boolean Blocked;

    public PressurizerBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.Pressurizer.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, PressurizerBlockEntity Pump) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof PressurizerBlock Machine) {
            Pump.tickServer(ServerWorld, State, Machine.getKind());
        }
    }

    @Nullable
    private DuctKind kind() {
        return getBlockState().getBlock() instanceof PressurizerBlock Machine ? Machine.getKind() : null;
    }

    private void tickServer(ServerLevel World, BlockState State, DuctKind Kind) {
        PressureNetworkManager Manager = PressureNetworkManager.get(World);
        List<PressureNetwork> Attached = new ArrayList<>();
        for (Direction Side : Direction.values()) {
            BlockPos Next = worldPosition.relative(Side);
            if (!World.isLoaded(Next)) {
                continue;
            }
            BlockState NextState = World.getBlockState(Next);
            if (NextState.getBlock() instanceof DuctBlock Duct && Duct.getKind() == Kind && NextState.getValue(DuctBlock.Connections.get(Side.getOpposite()))) {
                PressureNetwork Network = Manager.networkAt(World, Next, Kind);
                if (!Attached.contains(Network)) {
                    Attached.add(Network);
                }
            }
        }
        double Threshold = Wanting ? Kind.getTargetPressure() : Kind.getTargetPressure() * Hysteresis;
        List<PressureNetwork> Targets = new ArrayList<>();
        double Highest = 0.0;
        boolean AnyOpen = false;
        for (PressureNetwork Network : Attached) {
            if (Network.isOpen()) {
                Network.markPushed();
                AnyOpen = true;
                continue;
            }
            Highest = Math.max(Highest, Network.getPressure());
            if (Network.getPressure() < Threshold) {
                Targets.add(Network);
            }
        }
        Blocked = AnyOpen;
        LastPressure = Highest;
        Wanting = !Targets.isEmpty();
        double Fraction = Math.min(1.0, (double) Received / Kind.getPumpPower());
        Received = 0;
        double Budget = Kind.getPumpRate() * Fraction;
        if (Budget > 0.0 && Wanting) {
            double Share = Budget / Targets.size();
            for (PressureNetwork Network : Targets) {
                double Amount = Math.min(Share, Network.headroom(Kind.getTargetPressure()));
                if (Amount > 0.0) {
                    Network.pump(Amount);
                    LastPumped = World.getGameTime();
                }
            }
        }
        boolean Active = World.getGameTime() - LastPumped <= ActiveHold;
        if (State.getValue(PressurizerBlock.Active) != Active) {
            World.setBlock(worldPosition, State.setValue(PressurizerBlock.Active, Active), Block.UPDATE_CLIENTS);
        }
        if (Active && Kind == DuctKind.Vent && World.getGameTime() % BreatheTicks == 0) {
            breathe(World);
        }
    }

    private void breathe(ServerLevel World) {
        List<Direction> Open = new ArrayList<>();
        for (Direction Side : Direction.values()) {
            if (World.getBlockState(worldPosition.relative(Side)).isAir()) {
                Open.add(Side);
            }
        }
        if (Open.isEmpty()) {
            return;
        }
        RandomSource Random = World.getRandom();
        Direction Side = Open.get(Random.nextInt(Open.size()));
        double X = worldPosition.getX() + 0.5 + Side.getStepX() * 1.1 + (Side.getStepX() == 0 ? (Random.nextDouble() - 0.5) * 0.7 : 0.0);
        double Y = worldPosition.getY() + 0.5 + Side.getStepY() * 1.1 + (Side.getStepY() == 0 ? (Random.nextDouble() - 0.5) * 0.7 : 0.0);
        double Z = worldPosition.getZ() + 0.5 + Side.getStepZ() * 1.1 + (Side.getStepZ() == 0 ? (Random.nextDouble() - 0.5) * 0.7 : 0.0);
        World.sendParticles(ModParticles.VentAir.get(), X, Y, Z, 0, -Side.getStepX(), -Side.getStepY(), -Side.getStepZ(), 0.12);
    }

    public double getLastPressure() {
        return LastPressure;
    }

    public boolean isWanting() {
        return Wanting;
    }

    public boolean isBlocked() {
        return Blocked;
    }

    @Override
    public int getPowerDemand() {
        DuctKind Kind = kind();
        return Wanting && Kind != null ? Kind.getPumpPower() : 0;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }
}
