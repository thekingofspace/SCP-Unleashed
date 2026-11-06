package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;

public class CreativeTankBlockEntity extends BlockEntity {
    public static final int ShownAmount = 1_000_000;

    private FluidStack Liquid = FluidStack.EMPTY;
    private final IFluidHandler Handler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int Tank) {
            return Liquid.isEmpty() ? FluidStack.EMPTY : Liquid.copyWithAmount(ShownAmount);
        }

        @Override
        public int getTankCapacity(int Tank) {
            return ShownAmount;
        }

        @Override
        public boolean isFluidValid(int Tank, FluidStack Stack) {
            return true;
        }

        @Override
        public int fill(FluidStack Resource, FluidAction Action) {
            return Resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack Resource, FluidAction Action) {
            return Liquid.isEmpty() || Resource.isEmpty() || !FluidStack.isSameFluidSameComponents(Liquid, Resource) ? FluidStack.EMPTY : Resource.copy();
        }

        @Override
        public FluidStack drain(int MaxDrain, FluidAction Action) {
            return Liquid.isEmpty() || MaxDrain <= 0 ? FluidStack.EMPTY : Liquid.copyWithAmount(MaxDrain);
        }
    };

    public CreativeTankBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.CreativeTank.get(), Pos, State);
    }

    public FluidStack getLiquid() {
        return Liquid;
    }

    public IFluidHandler getHandler() {
        return Handler;
    }

    public void setLiquid(FluidStack Stack) {
        Liquid = Stack.isEmpty() ? FluidStack.EMPTY : Stack.copyWithAmount(1);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putBoolean("Set", !Liquid.isEmpty());
        if (!Liquid.isEmpty()) {
            Tag.put("Fluid", Liquid.save(Registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Liquid = FluidStack.parseOptional(Registries, Tag.getCompound("Fluid"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        return saveCustomOnly(Registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
