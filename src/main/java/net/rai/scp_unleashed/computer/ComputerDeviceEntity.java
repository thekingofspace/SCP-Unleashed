package net.rai.scp_unleashed.computer;

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
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

public abstract class ComputerDeviceEntity extends BlockEntity implements WireDevice {
    public static final int BootDelay = 10;

    protected final PowerFeed Feed = new PowerFeed();

    protected ComputerDeviceEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
    }

    public abstract int getDraw();

    public boolean isRunning() {
        return Feed.isRunning();
    }

    public void serverTick(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        if (Feed.tick(Now, getDraw(), BootDelay)) {
            setChanged();
            powerChanged(World, Feed.isRunning());
        }
        if (State.hasProperty(ComputerDeviceBlock.Powered) && State.getValue(ComputerDeviceBlock.Powered) != Feed.isRunning()) {
            World.setBlock(worldPosition, State.setValue(ComputerDeviceBlock.Powered, Feed.isRunning()), Block.UPDATE_CLIENTS);
        }
        tickDevice(World, Now);
    }

    protected void powerChanged(ServerLevel World, boolean Running) {
    }

    protected void tickDevice(ServerLevel World, long Now) {
    }

    @Override
    public int getPowerDemand() {
        return getDraw();
    }

    @Override
    public void insertPower(int Amount) {
        Feed.insert(Amount);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putBoolean("Running", Feed.isRunning());
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Running")) {
            Feed.restore(Tag.getBoolean("Running"));
        }
    }

    protected void writeClient(CompoundTag Tag, HolderLookup.Provider Registries) {
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Running", Feed.isRunning());
        writeClient(Tag, Registries);
        return Tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    protected void syncClients() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
