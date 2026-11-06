package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.block.MiniTankBlock;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;

public class MiniTankBlockEntity extends BlockEntity {
    public static final int SyncTicks = 5;

    private final FluidTank Storage = new FluidTank(MiniTankBlock.Capacity) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    private boolean NeedsSync;
    private long LastSync;
    private final LiquidLevel Display = new LiquidLevel();

    public MiniTankBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.MiniTank.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, MiniTankBlockEntity Tank) {
        Tank.settle(World, Pos);
        if (Tank.NeedsSync && World.getGameTime() - Tank.LastSync >= SyncTicks) {
            Tank.NeedsSync = false;
            Tank.LastSync = World.getGameTime();
            World.sendBlockUpdated(Pos, State, State, Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, MiniTankBlockEntity Tank) {
        Tank.Display.tick(Tank.Storage, SyncTicks);
    }

    private void settle(Level World, BlockPos Pos) {
        if (Storage.isEmpty() || !(World.getBlockEntity(Pos.below()) instanceof MiniTankBlockEntity Below)) {
            return;
        }
        FluidTank Lower = Below.getTank();
        if (!Lower.isEmpty() && !FluidStack.isSameFluidSameComponents(Lower.getFluid(), Storage.getFluid())) {
            return;
        }
        int Room = Lower.getCapacity() - Lower.getFluidAmount();
        if (Room > 0) {
            Lower.fill(Storage.drain(Room, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public FluidTank getTank() {
        return Storage;
    }

    public FluidStack getFluid() {
        return Storage.getFluid();
    }

    public FluidStack getShownLiquid() {
        return Display.getLiquid(Storage);
    }

    public float getShownFill(float PartialTick) {
        return Display.getFill(Storage, PartialTick);
    }

    private void contentsChanged() {
        setChanged();
        NeedsSync = true;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Storage.writeToNBT(Registries, Tag);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Storage.readFromNBT(Registries, Tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = saveCustomOnly(Registries);
        Tag.putInt("Amount", Storage.getFluidAmount());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
