package net.rai.scp_unleashed.guards;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PatrolManagerMenu extends AbstractContainerMenu {
    @Nullable
    private final PatrolManagerBlockEntity Manager;
    private final BlockPos Pos;

    public PatrolManagerMenu(int ContainerId, Inventory Pockets, RegistryFriendlyByteBuf Buffer) {
        super(GuardsRegistry.PatrolManagerMenuType.get(), ContainerId);
        this.Manager = null;
        this.Pos = Buffer.readBlockPos();
    }

    public PatrolManagerMenu(int ContainerId, Inventory Pockets, PatrolManagerBlockEntity Manager) {
        super(GuardsRegistry.PatrolManagerMenuType.get(), ContainerId);
        this.Manager = Manager;
        this.Pos = Manager.getBlockPos();
    }

    public BlockPos getPos() {
        return Pos;
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player User) {
        return Manager == null || Manager.isUsableBy(User);
    }
}
