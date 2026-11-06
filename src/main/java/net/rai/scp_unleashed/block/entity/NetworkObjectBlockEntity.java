package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.NetworkObjectBlock;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

public abstract class NetworkObjectBlockEntity extends BlockEntity implements WireDevice, PortHolder {
    public static final int PoweredWindow = 2;

    protected int Port;
    private long LastPowered = Long.MIN_VALUE / 2;

    protected NetworkObjectBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
    }

    public abstract String getKindName();

    public int getPort() {
        return Port;
    }

    public int getSlotCount() {
        return 1;
    }

    public int getSlot(int Slot) {
        return Port;
    }

    public void setSlot(int Slot, int Requested) {
        int Clamped = WireChannels.clampPort(Requested);
        if (Slot != 0 || Clamped == Port) {
            return;
        }
        int OldPort = Port;
        Port = Clamped;
        changed();
        if (level != null && !level.isClientSide) {
            portChanged(OldPort);
        }
    }

    protected void portChanged(int OldPort) {
    }

    protected void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean isPowered() {
        return level != null && level.getGameTime() - LastPowered <= PoweredWindow;
    }

    @Override
    public void observeNetwork(long Available) {
        if (Available > 0 && level != null) {
            LastPowered = level.getGameTime();
        }
    }

    protected void send(ServerLevel World, String Channel, CompoundTag Payload) {
        WireNetworkManager.get(World).sendFromDevice(World, worldPosition, Channel, Payload);
    }

    protected void showActive(boolean Powered) {
        BlockState State = getBlockState();
        if (level != null && State.getBlock() instanceof NetworkObjectBlock && State.getValue(NetworkObjectBlock.Active) != Powered) {
            level.setBlock(worldPosition, State.setValue(NetworkObjectBlock.Active, Powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Port", Port);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Port = WireChannels.clampPort(Tag.getInt("Port"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        return saveCustomOnly(Registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
