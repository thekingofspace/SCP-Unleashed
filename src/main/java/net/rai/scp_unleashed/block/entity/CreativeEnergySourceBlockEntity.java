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
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.WireDevice;

public class CreativeEnergySourceBlockEntity extends BlockEntity implements WireDevice {
    public static final int Supply = 1_000_000;

    private boolean Sink;

    public CreativeEnergySourceBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.CreativeEnergySource.get(), Pos, State);
    }

    public boolean isSinkMode() {
        return Sink;
    }

    public void toggleMode() {
        Sink = !Sink;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getPowerOutput() {
        return Sink ? 0 : Supply;
    }

    @Override
    public boolean isGenerator() {
        return !Sink;
    }

    @Override
    public boolean isSink() {
        return Sink;
    }

    @Override
    public int getGenerationRate() {
        return Sink ? 0 : Supply;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putBoolean("Sink", Sink);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Sink = Tag.getBoolean("Sink");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sink", Sink);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
