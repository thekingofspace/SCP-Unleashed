package net.rai.scp_unleashed.scpsign;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ScpSignBlockEntity extends BlockEntity {
    private static final String DocumentKey = "Document";

    @Nullable
    private ResourceLocation Document;

    public ScpSignBlockEntity(BlockPos Pos, BlockState State) {
        super(ScpSignRegistry.SignEntity.get(), Pos, State);
    }

    @Nullable
    public ResourceLocation getDocument() {
        return Document;
    }

    public void assign(@Nullable ResourceLocation Id) {
        if (Objects.equals(Document, Id)) {
            return;
        }
        Document = Id;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public AABB getRenderBounds() {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof ScpSignBlock Sign) {
            AABB Box = new AABB(worldPosition);
            for (BlockPos Part : Sign.parts(State, worldPosition)) {
                Box = Box.minmax(new AABB(Part));
            }
            return Box;
        }
        return new AABB(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag Data, HolderLookup.Provider Registries) {
        super.saveAdditional(Data, Registries);
        if (Document != null) {
            Data.putString(DocumentKey, Document.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Data, HolderLookup.Provider Registries) {
        super.loadAdditional(Data, Registries);
        Document = Data.contains(DocumentKey, Tag.TAG_STRING) ? ResourceLocation.tryParse(Data.getString(DocumentKey)) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Data = saveCustomOnly(Registries);
        Data.putBoolean("Assigned", Document != null);
        return Data;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (Document != null) {
            Components.set(ScpSignRegistry.DocumentComponent.get(), Document);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        Document = Input.get(ScpSignRegistry.DocumentComponent.get());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag Data) {
        super.removeComponentsFromTag(Data);
        Data.remove(DocumentKey);
    }
}
