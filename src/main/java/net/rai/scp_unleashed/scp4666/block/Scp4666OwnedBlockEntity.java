package net.rai.scp_unleashed.scp4666.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp4666OwnedBlockEntity extends BlockEntity {
    @Nullable
    private UUID Owner;
    private String Name = "";

    public Scp4666OwnedBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp4666Registry.OwnedEntity.get(), Pos, State);
    }

    public void setOwner(UUID Id, String Label) {
        Owner = Id;
        Name = Label;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    public UUID owner() {
        return Owner;
    }

    public String name() {
        return Name;
    }

    public boolean owns(UUID Id) {
        return Owner == null || Owner.equals(Id);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
        }
        Tag.putString("Name", Name);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Owner = Tag.hasUUID("Owner") ? Tag.getUUID("Owner") : null;
        Name = Tag.getString("Name");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putString("Name", Name);
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
        }
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
