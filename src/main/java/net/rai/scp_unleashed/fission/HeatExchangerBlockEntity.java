package net.rai.scp_unleashed.fission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.GasTank;

public class HeatExchangerBlockEntity extends BlockEntity {
    public static final int Capacity = 1000;
    public static final int SteamCapacity = 1000;
    public static final int SteamPerWater = 1;
    public static final int CheckTicks = 10;
    public static final int ActiveHold = 30;

    private final FluidTank Water = new FluidTank(Capacity, HeatExchangerBlockEntity::isCoolant) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final GasTank Steam = new GasTank(SteamCapacity, Kind -> Kind == GasRegistry.Steam.get(), this::setChanged);
    private long LastCooled = Long.MIN_VALUE / 2;

    public HeatExchangerBlockEntity(BlockPos Pos, BlockState State) {
        super(FissionRegistry.HeatExchangerEntity.get(), Pos, State);
    }

    public static boolean isCoolant(FluidStack Stack) {
        return Stack.is(FluidTags.WATER);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, HeatExchangerBlockEntity Exchanger) {
        if (World.getGameTime() % CheckTicks != Math.floorMod(Pos.asLong(), CheckTicks)) {
            return;
        }
        boolean Active = World.getGameTime() - Exchanger.LastCooled <= ActiveHold && !Exchanger.Water.isEmpty();
        if (State.getValue(HeatExchangerBlock.Active) != Active) {
            World.setBlock(Pos, State.setValue(HeatExchangerBlock.Active, Active), Block.UPDATE_CLIENTS);
        }
    }

    public FluidTank getTank() {
        return Water;
    }

    public int getWater() {
        return Water.getFluidAmount();
    }

    public GasTank getSteam() {
        return Steam;
    }

    public int drainWater(int Amount) {
        int Drained = Water.drain(Amount, IFluidHandler.FluidAction.EXECUTE).getAmount();
        if (Drained > 0) {
            Steam.fill(new GasStack(GasRegistry.Steam.get(), Drained * SteamPerWater), false);
        }
        return Drained;
    }

    public void markCooling(long Now) {
        LastCooled = Now;
    }

    public boolean isCooling() {
        return level != null && level.getGameTime() - LastCooled <= ActiveHold;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Water.writeToNBT(Registries, Tag);
        Steam.save(Tag, "Steam");
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Water.readFromNBT(Registries, Tag);
        Steam.load(Tag, "Steam");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = saveCustomOnly(Registries);
        Tag.putInt("Amount", Water.getFluidAmount());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
