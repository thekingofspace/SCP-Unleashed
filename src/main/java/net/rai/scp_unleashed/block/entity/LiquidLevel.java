package net.rai.scp_unleashed.block.entity;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class LiquidLevel {
    private FluidStack Shown = FluidStack.EMPTY;
    private float Fill = -1.0f;
    private float Previous;
    private float Goal;
    private float Step;

    public void tick(FluidTank Storage, int SpreadTicks) {
        float Target = fraction(Storage);
        if (!Storage.isEmpty() && !FluidStack.isSameFluidSameComponents(Shown, Storage.getFluid())) {
            Shown = Storage.getFluid().copyWithAmount(1);
        }
        if (Fill < 0.0f) {
            Fill = Target;
            Goal = Target;
        }
        if (Target != Goal) {
            Goal = Target;
            Step = (Goal - Fill) / Math.max(1, SpreadTicks);
        }
        Previous = Fill;
        Fill = Math.abs(Goal - Fill) <= Math.abs(Step) ? Goal : Fill + Step;
        if (Fill <= 0.0f && Storage.isEmpty()) {
            Shown = FluidStack.EMPTY;
        }
    }

    public FluidStack getLiquid(FluidTank Storage) {
        return Shown.isEmpty() ? Storage.getFluid() : Shown;
    }

    public float getFill(FluidTank Storage, float PartialTick) {
        return Fill < 0.0f ? fraction(Storage) : Mth.lerp(PartialTick, Previous, Fill);
    }

    private static float fraction(FluidTank Storage) {
        return (float) Storage.getFluidAmount() / Storage.getCapacity();
    }
}
