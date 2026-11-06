package net.rai.scp_unleashed.scp294;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Scp294Menu extends AbstractContainerMenu {
    private final BlockPos Pos;
    private final ContainerLevelAccess Access;

    public Scp294Menu(int ContainerId, Inventory Pockets, RegistryFriendlyByteBuf Extra) {
        this(ContainerId, Pockets, Extra.readBlockPos());
    }

    public Scp294Menu(int ContainerId, Inventory Pockets, BlockPos Pos) {
        super(Scp294Registry.KeyboardMenu.get(), ContainerId);
        this.Pos = Pos;
        this.Access = ContainerLevelAccess.create(Pockets.player.level(), Pos);
    }

    public BlockPos getPos() {
        return Pos;
    }

    @Nullable
    public Scp294MachineBlockEntity machine(Player User) {
        return User.level().getBlockEntity(Pos) instanceof Scp294MachineBlockEntity Machine ? Machine : null;
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player User) {
        return stillValid(Access, User, Scp294Registry.Machine.get());
    }
}
