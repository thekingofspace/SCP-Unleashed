package net.rai.scp_unleashed.scp093;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

public class Scp093MirrorBlockEntity extends BlockEntity {
    public static final int HumEvery = 86;
    public static final float HumVolume = 0.42f;
    private ItemStack Disc = ItemStack.EMPTY;

    public Scp093MirrorBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp093Registry.MirrorEntity.get(), Pos, State);
    }

    public boolean hasDisc() {
        return !Disc.isEmpty();
    }

    public ItemStack disc() {
        return Disc;
    }

    public void insert(ItemStack Stack) {
        Disc = Stack.copyWithCount(1);
        changed();
    }

    public ItemStack take() {
        ItemStack Taken = Disc;
        Disc = ItemStack.EMPTY;
        changed();
        return Taken;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp093MirrorBlockEntity Holder) {
        if (!Holder.hasDisc() || !State.getValue(Scp093MirrorBlock.Seated)) {
            return;
        }
        if ((World.getGameTime() + Pos.asLong()) % HumEvery == 0) {
            float Pitch = 0.92f + World.random.nextFloat() * 0.1f;
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp093Registry.HumLayers) {
                World.playSound(null, Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, Layer.get(), SoundSource.BLOCKS, HumVolume, Pitch);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!Disc.isEmpty()) {
            Tag.put("Disc", Disc.save(Registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Disc = Tag.contains("Disc") ? ItemStack.parseOptional(Registries, Tag.getCompound("Disc")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = saveCustomOnly(Registries);
        Tag.putBoolean("Seated", !Disc.isEmpty());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
