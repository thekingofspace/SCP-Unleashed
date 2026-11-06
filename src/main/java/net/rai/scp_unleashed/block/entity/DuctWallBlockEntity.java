package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;

public class DuctWallBlockEntity extends DuctBlockEntity {
    public static final ModelProperty<BlockState> DisguiseProperty = new ModelProperty<>();

    private BlockState Disguise = Blocks.AIR.defaultBlockState();
    private boolean Consumed;

    public DuctWallBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.DuctWall.get(), Pos, State);
    }

    public BlockState getDisguise() {
        return Disguise;
    }

    public boolean hasDisguise() {
        return !Disguise.isAir();
    }

    public boolean isConsumed() {
        return Consumed;
    }

    public void setDisguise(BlockState Look, boolean Paid) {
        Disguise = Look;
        Consumed = Paid && !Look.isAir();
        disguiseChanged();
    }

    public void clearDisguise() {
        setDisguise(Blocks.AIR.defaultBlockState(), false);
    }

    public void returnDisguise(Player User) {
        if (!Consumed || Disguise.isAir()) {
            return;
        }
        ItemStack Refund = new ItemStack(Disguise.getBlock());
        if (!User.getInventory().add(Refund)) {
            User.drop(Refund, false);
        }
        Consumed = false;
        setChanged();
    }

    private void disguiseChanged() {
        setChanged();
        if (level != null) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ModelData getModelData() {
        return Disguise.isAir() ? ModelData.EMPTY : ModelData.of(DisguiseProperty, Disguise);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!Disguise.isAir()) {
            Tag.put("Disguise", NbtUtils.writeBlockState(Disguise));
            Tag.putBoolean("Consumed", Consumed);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Disguise = Tag.contains("Disguise") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), Tag.getCompound("Disguise")) : Blocks.AIR.defaultBlockState();
        Consumed = Tag.getBoolean("Consumed");
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
