package net.rai.scp_unleashed.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.MiniTankBlock;
import net.rai.scp_unleashed.block.entity.MiniTankBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class MiniTankColumn implements IFluidHandler {
    private final List<MiniTankBlockEntity> Cells;
    private final BlockPos Bottom;

    private MiniTankColumn(List<MiniTankBlockEntity> Cells, BlockPos Bottom) {
        this.Cells = Cells;
        this.Bottom = Bottom;
    }

    public static BlockPos bottomOf(Level World, BlockPos Pos) {
        BlockPos Current = Pos;
        for (int Step = 0; Step < MiniTankBlock.MaxStack && World.getBlockState(Current.below()).getBlock() instanceof MiniTankBlock; Step++) {
            Current = Current.below();
        }
        return Current.immutable();
    }

    public static MiniTankColumn of(Level World, BlockPos Pos) {
        BlockPos Start = bottomOf(World, Pos);
        List<MiniTankBlockEntity> Cells = new ArrayList<>();
        BlockPos Current = Start;
        while (Cells.size() < MiniTankBlock.MaxStack && World.getBlockEntity(Current) instanceof MiniTankBlockEntity Cell) {
            Cells.add(Cell);
            Current = Current.above();
        }
        return new MiniTankColumn(Cells, Start);
    }

    public BlockPos getBottom() {
        return Bottom;
    }

    private FluidStack contents() {
        for (MiniTankBlockEntity Cell : Cells) {
            if (!Cell.getFluid().isEmpty()) {
                return Cell.getFluid();
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int Tank) {
        FluidStack Kind = contents();
        if (Kind.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int Total = 0;
        for (MiniTankBlockEntity Cell : Cells) {
            if (FluidStack.isSameFluidSameComponents(Cell.getFluid(), Kind)) {
                Total += Cell.getFluid().getAmount();
            }
        }
        return Kind.copyWithAmount(Total);
    }

    @Override
    public int getTankCapacity(int Tank) {
        return Cells.size() * MiniTankBlock.Capacity;
    }

    @Override
    public boolean isFluidValid(int Tank, FluidStack Stack) {
        FluidStack Kind = contents();
        return Kind.isEmpty() || FluidStack.isSameFluidSameComponents(Kind, Stack);
    }

    @Override
    public int fill(FluidStack Resource, FluidAction Action) {
        if (Resource.isEmpty() || !isFluidValid(0, Resource)) {
            return 0;
        }
        int Remaining = Resource.getAmount();
        for (MiniTankBlockEntity Cell : Cells) {
            if (Remaining <= 0) {
                break;
            }
            Remaining -= Cell.getTank().fill(Resource.copyWithAmount(Remaining), Action);
        }
        return Resource.getAmount() - Remaining;
    }

    @Override
    public FluidStack drain(FluidStack Resource, FluidAction Action) {
        if (Resource.isEmpty() || !FluidStack.isSameFluidSameComponents(contents(), Resource)) {
            return FluidStack.EMPTY;
        }
        return drain(Resource.getAmount(), Action);
    }

    @Override
    public FluidStack drain(int MaxDrain, FluidAction Action) {
        FluidStack Kind = contents();
        if (Kind.isEmpty() || MaxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int Remaining = MaxDrain;
        for (int Index = Cells.size() - 1; Index >= 0 && Remaining > 0; Index--) {
            MiniTankBlockEntity Cell = Cells.get(Index);
            if (FluidStack.isSameFluidSameComponents(Cell.getFluid(), Kind)) {
                Remaining -= Cell.getTank().drain(Remaining, Action).getAmount();
            }
        }
        int Drained = MaxDrain - Remaining;
        return Drained > 0 ? Kind.copyWithAmount(Drained) : FluidStack.EMPTY;
    }
}
