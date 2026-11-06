package net.rai.scp_unleashed.scp4845.order;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import net.rai.scp_unleashed.scp4845.item.Scp4845MenuItem;

public class Scp4845OrderMenu extends AbstractContainerMenu {
    public static final double Reach = 8.0;
    public static final int Stride = 16;
    private final BlockPos Counter;

    public Scp4845OrderMenu(int ContainerId, Inventory PlayerInventory, RegistryFriendlyByteBuf Buffer) {
        this(ContainerId, PlayerInventory, Buffer.readBlockPos());
    }

    public Scp4845OrderMenu(int ContainerId, Inventory PlayerInventory, BlockPos Counter) {
        super(Scp4845Registry.OrderMenuType.get(), ContainerId);
        this.Counter = Counter.immutable();
    }

    public BlockPos counter() {
        return Counter;
    }

    public static int button(Scp4845MenuItem Entry, int Quantity) {
        return Entry.ordinal() * Stride + Quantity;
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        Scp4845MenuItem Entry = Scp4845MenuItem.byIndex(Id / Stride);
        int Quantity = Id % Stride;
        if (Entry == null || Quantity < 1 || Quantity > Scp4845MenuItem.MaxPerLine || !(User instanceof ServerPlayer Customer)) {
            return false;
        }
        return Scp4845Orders.order(Customer, Counter, Entry, Quantity);
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player User) {
        return User.distanceToSqr(Counter.getX() + 0.5, Counter.getY() + 0.5, Counter.getZ() + 0.5) <= Reach * Reach;
    }
}
