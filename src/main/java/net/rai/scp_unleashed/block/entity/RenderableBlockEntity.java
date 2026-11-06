package net.rai.scp_unleashed.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.rai.scp_unleashed.block.FaceLayer;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RenderableBlockEntity extends BlockEntity {
    public static final ModelProperty<Map<Direction, List<FaceLayer>>> LayersProperty = new ModelProperty<>();
    private static final Codec<Map<Direction, List<FaceLayer>>> LayersCodec = Codec.unboundedMap(Direction.CODEC, FaceLayer.LayerCodec.listOf());

    private final Map<Direction, List<FaceLayer>> Layers = new EnumMap<>(Direction.class);

    public RenderableBlockEntity(BlockPos Pos, BlockState State) {
        this(ModBlockEntityTypes.RenderableBlock.get(), Pos, State);
    }

    protected RenderableBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
    }

    public boolean hasLayers() {
        return !Layers.isEmpty();
    }

    public List<FaceLayer> getLayers(Direction Face) {
        return Collections.unmodifiableList(Layers.getOrDefault(Face, List.of()));
    }

    public void addLayer(Direction Face, FaceLayer Layer) {
        Layers.computeIfAbsent(Face, Key -> new ArrayList<>()).add(Layer);
        layersChanged();
    }

    public boolean removeLayer(Direction Face, FaceLayer Layer) {
        List<FaceLayer> FaceLayers = Layers.get(Face);
        if (FaceLayers == null || !FaceLayers.remove(Layer)) {
            return false;
        }
        if (FaceLayers.isEmpty()) {
            Layers.remove(Face);
        }
        layersChanged();
        return true;
    }

    public void clearLayers(Direction Face) {
        if (Layers.remove(Face) != null) {
            layersChanged();
        }
    }

    public void clearLayers() {
        if (!Layers.isEmpty()) {
            Layers.clear();
            layersChanged();
        }
    }

    private void layersChanged() {
        setChanged();
        if (level != null) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ModelData getModelData() {
        Map<Direction, List<FaceLayer>> Snapshot = new EnumMap<>(Direction.class);
        Layers.forEach((Face, FaceLayers) -> Snapshot.put(Face, List.copyOf(FaceLayers)));
        return ModelData.of(LayersProperty, Map.copyOf(Snapshot));
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        LayersCodec.encodeStart(NbtOps.INSTANCE, Layers).ifSuccess(Encoded -> Tag.put("Layers", Encoded));
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Layers.clear();
        if (Tag.contains("Layers")) {
            LayersCodec.parse(NbtOps.INSTANCE, Tag.get("Layers")).ifSuccess(Decoded -> Decoded.forEach((Face, FaceLayers) -> {
                if (!FaceLayers.isEmpty()) {
                    Layers.put(Face, new ArrayList<>(FaceLayers));
                }
            }));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = saveCustomOnly(Registries);
        Tag.putBoolean("Synced", true);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection Net, ClientboundBlockEntityDataPacket DataPacket, HolderLookup.Provider Registries) {
        super.onDataPacket(Net, DataPacket, Registries);
        if (level != null) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.handleUpdateTag(Tag, Registries);
        refreshLook();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshLook();
    }

    private void refreshLook() {
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }
}
