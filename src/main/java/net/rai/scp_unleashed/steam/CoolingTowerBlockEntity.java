package net.rai.scp_unleashed.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasTank;

import java.util.function.Consumer;

public class CoolingTowerBlockEntity extends BlockEntity {
    public static final int SteamCapacity = 12000;
    public static final int WaterCapacity = 24000;
    public static final int CondenseRate = 300;
    public static final float MinReturn = 0.7f;
    public static final float MaxReturn = 0.9f;
    public static final float ReturnPerDegree = 0.1f;
    public static final int ClimateTicks = 100;
    public static final int PlumeSteps = 8;
    public static final int SyncTicks = 10;
    public static final int LightCheckTicks = 40;

    private static Consumer<CoolingTowerBlockEntity> ClientEffects = Machine -> {
    };

    private final GasTank Steam = new GasTank(SteamCapacity, Kind -> Kind == GasRegistry.LowPressureSteam.get(), this::setChanged);
    private final FluidTank Water = new FluidTank(WaterCapacity, Stack -> Stack.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private float Return = (MinReturn + MaxReturn) / 2.0f;
    private long ClimateCheckedAt = Long.MIN_VALUE / 2;
    private float Carry;
    private int LastCondensed;
    private int LastReturned;
    private TowerStatus Status = TowerStatus.Idle;
    private MachineLight LastLight = MachineLight.Off;
    private int Plume;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync;

    public CoolingTowerBlockEntity(BlockPos Pos, BlockState State) {
        super(SteamRegistry.CoolingTowerEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<CoolingTowerBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static float returnFor(float Temperature) {
        return Mth.clamp(MaxReturn - ReturnPerDegree * Mth.clamp(Temperature, 0.0f, 2.0f), MinReturn, MaxReturn);
    }

    public GasTank getSteamTank() {
        return Steam;
    }

    public FluidTank getWaterTank() {
        return Water;
    }

    public TowerStatus getStatus() {
        return Status;
    }

    public float getReturn() {
        return Return;
    }

    public int getReturnPercent() {
        return Math.round(Return * 100.0f);
    }

    public int getLastCondensed() {
        return LastCondensed;
    }

    public int getLastReturned() {
        return LastReturned;
    }

    public int getPlume() {
        return Plume;
    }

    public BlockPos getMouth() {
        return worldPosition.above(CoolingTowerBlock.Layout.Height() - 1);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, CoolingTowerBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof CoolingTowerBlock) {
            Machine.tickServer(ServerWorld);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, CoolingTowerBlockEntity Machine) {
        ClientEffects.accept(Machine);
    }

    private void tickServer(ServerLevel World) {
        long Now = World.getGameTime();
        if (Now - ClimateCheckedAt >= ClimateTicks) {
            ClimateCheckedAt = Now;
            Return = returnFor(World.getBiome(getMouth()).value().getBaseTemperature());
        }
        int Take = Math.min(Steam.getAmount(), CondenseRate);
        int Fits = (int) Math.floor((Water.getSpace() - Carry) / Return);
        Take = Math.max(0, Math.min(Take, Fits));
        int Made = 0;
        if (Take > 0) {
            float Total = Take * Return + Carry;
            Made = (int) Total;
            Carry = Total - Made;
            Steam.drain(Take, false);
            if (Made > 0) {
                Water.fill(new FluidStack(Fluids.WATER, Made), IFluidHandler.FluidAction.EXECUTE);
            }
            setChanged();
        }
        LastCondensed = Take;
        LastReturned = Made;
        if (Take > 0) {
            Status = TowerStatus.Condensing;
        } else {
            Status = Steam.isEmpty() ? TowerStatus.Idle : TowerStatus.Full;
        }
        MachineLight Shown = Status == TowerStatus.Condensing ? MachineLight.Working : Status == TowerStatus.Full ? MachineLight.Alert : MachineLight.Off;
        if (Shown != LastLight || Now % LightCheckTicks == 0) {
            LastLight = Shown;
            if (getBlockState().getBlock() instanceof CoolingTowerBlock Tower) {
                Tower.showLight(World, worldPosition, getBlockState(), Shown);
            }
        }
        updatePlume(World, Take <= 0 ? 0 : Mth.clamp(Math.round((float) Take * PlumeSteps / CondenseRate), 1, PlumeSteps));
    }

    private void updatePlume(ServerLevel World, int Wanted) {
        if (Wanted != Plume) {
            UrgentSync |= Wanted == 0 || Plume == 0;
            Plume = Wanted;
            NeedsSync = true;
        }
        long Now = World.getGameTime();
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public Component describe() {
        return Component.translatable("message.scp_unleashed.cooling_tower.status", Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()),
                Water.getFluidAmount(), WaterCapacity, getReturnPercent());
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Steam.save(Tag, "Steam");
        Tag.put("Water", Water.writeToNBT(Registries, new CompoundTag()));
        Tag.putFloat("Carry", Carry);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Plume")) {
            Plume = Mth.clamp(Tag.getByte("Plume"), 0, PlumeSteps);
            return;
        }
        Steam.load(Tag, "Steam");
        if (Tag.contains("Water")) {
            Water.readFromNBT(Registries, Tag.getCompound("Water"));
        }
        Carry = Mth.clamp(Tag.getFloat("Carry"), 0.0f, 1.0f);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Plume", (byte) Plume);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
