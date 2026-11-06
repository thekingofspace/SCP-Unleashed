package net.rai.scp_unleashed.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.GasTank;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

public class CombustionGeneratorBlockEntity extends OilMachineBlockEntity {
    public static final int FuelTank = 0;
    public static final int Capacity = 40000;
    public static final int MaxOffer = 800;
    public static final int FuelUnit = 1000;

    private final FluidTank Fuel = addTank(this::canBurn, PortRole.Input);
    private final GasTank Exhaust = new GasTank(OilMachineKind.Generator.getGasCapacity(), Any -> true, this::setChanged);
    @Nullable
    private CombustionFuel Burning;
    private boolean FuelDirty = true;
    private int Stored;
    private int Debt;
    private int Generating;
    private int LastFuelPower;

    public CombustionGeneratorBlockEntity(BlockPos Pos, BlockState State) {
        super(OilRegistry.CombustionGeneratorEntity.get(), Pos, State, OilMachineKind.Generator);
    }

    private boolean canBurn(FluidStack Stack) {
        return level != null && CombustionFuel.find(level.getRecipeManager(), Stack) != null;
    }

    @Override
    protected void tankChanged() {
        FuelDirty = true;
    }

    @Override
    protected void recipesReloaded() {
        FuelDirty = true;
    }

    @Override
    public GasHandler getExhaust() {
        return Exhaust;
    }

    public GasTank getExhaustTank() {
        return Exhaust;
    }

    public FluidTank getFuel() {
        return Fuel;
    }

    public int getGenerating() {
        return Generating;
    }

    public int getFuelPower() {
        return LastFuelPower;
    }

    @Override
    public float getProgress() {
        return (float) Stored / Capacity;
    }

    @Override
    protected int shownPower() {
        return Generating;
    }

    @Override
    protected int shownPowerMax() {
        return LastFuelPower;
    }

    @Override
    protected int shownGas() {
        return Exhaust.getAmount();
    }

    @Override
    protected float work(ServerLevel World, BlockPos Pos, BlockState State, long Now, int Delivered, boolean WasDemanding) {
        if (FuelDirty) {
            FuelDirty = false;
            Burning = CombustionFuel.find(World.getRecipeManager(), Fuel.getFluid());
            if (Burning != null) {
                LastFuelPower = Burning.getPower();
            }
        }
        Generating = 0;
        if (Burning == null || Fuel.isEmpty()) {
            Status = OilStatus.NoFuel;
            return 0.0f;
        }
        GasStack Smoke = Burning.getExhaust();
        if (Exhaust.fill(Smoke, true) < Smoke.getAmount()) {
            Status = OilStatus.ExhaustFull;
            return 0.0f;
        }
        if (Stored + Burning.getPower() > Capacity) {
            Status = OilStatus.BufferFull;
            return 0.0f;
        }
        int Owed = Debt + FuelUnit;
        int Needed = Owed / Burning.getBurnTime();
        if (Needed > Fuel.getFluidAmount()) {
            Status = OilStatus.NoFuel;
            return 0.0f;
        }
        if (Needed > 0) {
            Fuel.drain(Needed, IFluidHandler.FluidAction.EXECUTE);
        }
        Debt = Owed - Needed * Burning.getBurnTime();
        Stored += Burning.getPower();
        Generating = Burning.getPower();
        Exhaust.fill(Smoke, false);
        Status = OilStatus.Working;
        LastWorked = Now;
        setChanged();
        return 1.0f;
    }

    @Override
    public boolean isGenerator() {
        return true;
    }

    @Override
    public int getGenerationRate() {
        return Generating;
    }

    @Override
    public int getPowerOutput() {
        return Math.min(Stored, MaxOffer);
    }

    @Override
    public int getReserveOutput() {
        return WireDevice.bufferedReserve(this);
    }

    @Override
    public int getPowerDemand() {
        return 0;
    }

    @Override
    public void extractPower(int Amount) {
        Stored = Math.max(0, Stored - Amount);
        setChanged();
    }

    @Override
    public int getStoredPower() {
        return Stored;
    }

    @Override
    public int getPowerCapacity() {
        return Capacity;
    }

    @Override
    public void releaseContents(ServerLevel World) {
        if (Exhaust.isEmpty()) {
            return;
        }
        GasStack Held = Exhaust.getGas();
        Vec3 Centre = getCentre();
        Held.getGas().release(World, new Vec3(Centre.x, Centre.y + 0.5, Centre.z), Held.getAmount());
        Exhaust.setGas(GasStack.EMPTY);
    }

    @Override
    protected void saveExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        Exhaust.save(Tag, "Exhaust");
        Tag.putInt("Stored", Stored);
        Tag.putInt("Debt", Debt);
        Tag.putInt("FuelPower", LastFuelPower);
    }

    @Override
    protected void loadExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        Exhaust.load(Tag, "Exhaust");
        Stored = Math.min(Capacity, Math.max(0, Tag.getInt("Stored")));
        Debt = Math.max(0, Tag.getInt("Debt"));
        LastFuelPower = Math.max(0, Tag.getInt("FuelPower"));
        FuelDirty = true;
    }
}
