package net.rai.scp_unleashed.oil;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class OilLiquid {
    private final OilGrade Grade;
    private DeferredHolder<FluidType, FluidType> Type;
    private DeferredHolder<Fluid, BaseFlowingFluid.Source> Source;
    private DeferredHolder<Fluid, BaseFlowingFluid.Flowing> Flowing;
    private DeferredBlock<OilLiquidBlock> Pool;
    private DeferredItem<BucketItem> Bucket;

    OilLiquid(OilGrade Grade) {
        this.Grade = Grade;
    }

    void bind(DeferredHolder<FluidType, FluidType> Type, DeferredHolder<Fluid, BaseFlowingFluid.Source> Source, DeferredHolder<Fluid, BaseFlowingFluid.Flowing> Flowing,
              DeferredBlock<OilLiquidBlock> Pool, DeferredItem<BucketItem> Bucket) {
        this.Type = Type;
        this.Source = Source;
        this.Flowing = Flowing;
        this.Pool = Pool;
        this.Bucket = Bucket;
    }

    BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(Type, Source, Flowing).bucket(Bucket).block(Pool)
                .slopeFindDistance(Grade.getSpread() + 1).levelDecreasePerBlock(Grade.getSpread()).tickRate(Grade.getTickRate()).explosionResistance(100.0f);
    }

    public OilGrade getGrade() {
        return Grade;
    }

    public DeferredHolder<FluidType, FluidType> getType() {
        return Type;
    }

    public BaseFlowingFluid.Source getSource() {
        return Source.get();
    }

    public BaseFlowingFluid.Flowing getFlowing() {
        return Flowing.get();
    }

    public DeferredBlock<OilLiquidBlock> getBlock() {
        return Pool;
    }

    public DeferredItem<BucketItem> getBucket() {
        return Bucket;
    }

    public FluidStack stack(int Amount) {
        return new FluidStack(Source.get(), Amount);
    }

    public boolean is(FluidStack Stack) {
        return !Stack.isEmpty() && Stack.getFluid().isSame(Source.get());
    }
}
