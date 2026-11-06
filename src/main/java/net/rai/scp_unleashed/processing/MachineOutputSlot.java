package net.rai.scp_unleashed.processing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class MachineOutputSlot extends SlotItemHandler {
    private final Player User;
    @Nullable
    private final ProcessingMachineBlockEntity Machine;
    private final boolean Smelting;
    private int Removed;

    public MachineOutputSlot(Player User, @Nullable ProcessingMachineBlockEntity Machine, boolean Smelting, IItemHandler Storage, int Index, int X, int Y) {
        super(Storage, Index, X, Y);
        this.User = User;
        this.Machine = Machine;
        this.Smelting = Smelting;
    }

    @Override
    public boolean mayPlace(ItemStack Stack) {
        return false;
    }

    @Override
    public ItemStack remove(int Amount) {
        if (hasItem()) {
            Removed += Math.min(Amount, getItem().getCount());
        }
        return super.remove(Amount);
    }

    @Override
    public void onTake(Player Taker, ItemStack Stack) {
        checkTakeAchievements(Stack);
        super.onTake(Taker, Stack);
    }

    @Override
    public void onQuickCraft(ItemStack Remaining, ItemStack Original) {
        int Amount = Original.getCount() - Remaining.getCount();
        if (Amount > 0) {
            onQuickCraft(Original, Amount);
        }
    }

    @Override
    protected void onQuickCraft(ItemStack Stack, int Amount) {
        Removed += Amount;
        checkTakeAchievements(Stack);
    }

    @Override
    protected void checkTakeAchievements(ItemStack Stack) {
        Stack.onCraftedBy(User.level(), User, Removed);
        if (User instanceof ServerPlayer Server && Machine != null) {
            Machine.awardExperience(Server);
        }
        Removed = 0;
        if (Smelting) {
            EventHooks.firePlayerSmeltedEvent(User, Stack);
        }
    }
}
