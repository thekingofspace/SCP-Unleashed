package net.rai.scp_unleashed.scp3008.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;

public class Scp3008CrateBlockEntity extends RandomizableContainerBlockEntity {
    public static final int Slots = 27;
    private NonNullList<ItemStack> Items = NonNullList.withSize(Slots, ItemStack.EMPTY);

    public Scp3008CrateBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp3008Registry.CrateEntity.get(), Pos, State);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.scp_unleashed.scp3008_crate");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return Items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> Stacks) {
        Items = Stacks;
    }

    @Override
    protected AbstractContainerMenu createMenu(int Id, Inventory Pockets) {
        return ChestMenu.threeRows(Id, Pockets, this);
    }

    @Override
    public int getContainerSize() {
        return Slots;
    }

    @Override
    public void startOpen(Player User) {
        if (!remove && !User.isSpectator() && level != null) {
            level.playSound(null, worldPosition, Scp3008Registry.CrateOpen.get(), SoundSource.BLOCKS, 0.6f, 0.95f + level.getRandom().nextFloat() * 0.1f);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.loadAdditional(Saved, Lookup);
        Items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(Saved)) {
            ContainerHelper.loadAllItems(Saved, Items, Lookup);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.saveAdditional(Saved, Lookup);
        if (!trySaveLootTable(Saved)) {
            ContainerHelper.saveAllItems(Saved, Items, Lookup);
        }
    }
}
