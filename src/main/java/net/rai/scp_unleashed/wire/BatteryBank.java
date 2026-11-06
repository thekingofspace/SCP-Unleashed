package net.rai.scp_unleashed.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.block.BatteryBlock;
import net.rai.scp_unleashed.block.entity.BatteryBlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BatteryBank implements WireDevice {
    public static final int CapacityPerCell = 300;
    public static final int TransferPerCell = 100;
    public static final int MaxCells = 256;

    private final List<BatteryBlockEntity> Cells;
    private final BlockPos Canonical;

    private BatteryBank(List<BatteryBlockEntity> Cells, BlockPos Canonical) {
        this.Cells = Cells;
        this.Canonical = Canonical;
    }

    public static BatteryBank scan(ServerLevel World, BlockPos Start) {
        List<BatteryBlockEntity> Found = new ArrayList<>();
        Set<BlockPos> Visited = new HashSet<>();
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        Queue.add(Start.immutable());
        Visited.add(Start.immutable());
        BlockPos Lowest = Start.immutable();
        while (!Queue.isEmpty() && Found.size() < MaxCells) {
            BlockPos Current = Queue.poll();
            if (!(World.getBlockEntity(Current) instanceof BatteryBlockEntity Cell)) {
                continue;
            }
            Found.add(Cell);
            if (Current.asLong() < Lowest.asLong()) {
                Lowest = Current;
            }
            for (Direction Side : Direction.values()) {
                BlockPos Next = Current.relative(Side);
                if (Visited.add(Next) && World.isLoaded(Next) && World.getBlockState(Next).getBlock() instanceof BatteryBlock) {
                    Queue.add(Next);
                }
            }
        }
        return new BatteryBank(Found, Lowest);
    }

    public List<BlockPos> getMembers() {
        List<BlockPos> Members = new ArrayList<>(Cells.size());
        for (BatteryBlockEntity Cell : Cells) {
            Members.add(Cell.getBlockPos());
        }
        return Members;
    }

    public BlockPos getCanonical() {
        return Canonical;
    }

    public int getCellCount() {
        return Cells.size();
    }

    public int getMaxTransfer() {
        return Cells.size() * TransferPerCell;
    }

    @Override
    public boolean isStorage() {
        return true;
    }

    @Override
    public int getStoredPower() {
        long Total = 0;
        for (BatteryBlockEntity Cell : Cells) {
            Total += Cell.getStored();
        }
        return (int) Math.min(Integer.MAX_VALUE, Total);
    }

    @Override
    public int getPowerCapacity() {
        return (int) Math.min(Integer.MAX_VALUE, (long) Cells.size() * CapacityPerCell);
    }

    @Override
    public int getPowerOutput() {
        return getStoredPower();
    }

    @Override
    public int getPowerDemand() {
        return Math.max(0, Math.min(getPowerCapacity() - getStoredPower(), getMaxTransfer()));
    }

    @Override
    public void insertPower(int Amount) {
        int Remaining = Amount;
        for (BatteryBlockEntity Cell : Cells) {
            if (Remaining <= 0) {
                break;
            }
            Remaining -= Cell.addPower(Remaining);
        }
    }

    @Override
    public void extractPower(int Amount) {
        int Remaining = Amount;
        for (BatteryBlockEntity Cell : Cells) {
            if (Remaining <= 0) {
                break;
            }
            Remaining -= Cell.takePower(Remaining);
        }
    }
}
