package net.rai.scp_unleashed.guns;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class GunBenchMenu extends AbstractContainerMenu {
    public static final int OutputX = 199;
    public static final int OutputY = 29;
    public static final int InventoryX = 108;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int OutputSlot = 0;
    public static final int InventoryStart = 1;
    public static final int InventoryEnd = InventoryStart + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    private final ContainerLevelAccess Access;
    private final Player User;
    private final SimpleContainer Output = new SimpleContainer(1);

    public GunBenchMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, ContainerLevelAccess.NULL);
    }

    public GunBenchMenu(int ContainerId, Inventory PlayerInventory, ContainerLevelAccess Access) {
        super(GunsRegistry.GunBenchMenuType.get(), ContainerId);
        this.Access = Access;
        this.User = PlayerInventory.player;
        addSlot(new Slot(Output, 0, OutputX, OutputY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return false;
            }
        });
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 9; Column++) {
                addSlot(new Slot(PlayerInventory, Column + Row * 9 + 9, InventoryX + Column * 18, InventoryY + Row * 18));
            }
        }
        for (int Column = 0; Column < 9; Column++) {
            addSlot(new Slot(PlayerInventory, Column, InventoryX + Column * 18, HotbarY));
        }
    }

    public ItemStack getOutput() {
        return Output.getItem(0);
    }

    public Player getUser() {
        return User;
    }

    public boolean canAccept(ItemStack Result) {
        ItemStack Waiting = Output.getItem(0);
        if (Waiting.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(Waiting, Result) && Waiting.getCount() + Result.getCount() <= Waiting.getMaxStackSize();
    }

    @Override
    public boolean clickMenuButton(Player Clicker, int Id) {
        List<RecipeHolder<GunCraftingRecipe>> Recipes = GunCraftingRecipes.all(Clicker.level());
        if (Id < 0 || Id >= Recipes.size()) {
            return false;
        }
        GunCraftingRecipe Formula = Recipes.get(Id).value();
        ItemStack Result = Formula.getResult().copy();
        if (Result.isEmpty() || !canAccept(Result)) {
            return false;
        }
        List<ItemStack> Supplies = GunCraftingRecipes.supplies(Clicker);
        if (!Clicker.getAbilities().instabuild) {
            int[] Taken = Formula.plan(Supplies);
            if (Taken == null) {
                return false;
            }
            for (int Slot = 0; Slot < Taken.length; Slot++) {
                if (Taken[Slot] > 0) {
                    Supplies.get(Slot).shrink(Taken[Slot]);
                }
            }
            Clicker.getInventory().setChanged();
        }
        ItemStack Waiting = Output.getItem(0);
        if (Waiting.isEmpty()) {
            Output.setItem(0, Result);
        } else {
            Waiting.grow(Result.getCount());
            Output.setChanged();
        }
        Result.onCraftedBy(Clicker.level(), Clicker, Result.getCount());
        Access.execute((World, Pos) -> GunSounds.play(World, null, Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, GunSounds.benchAssemble(), 1.0F));
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player Clicker, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index == OutputSlot) {
            if (!moveItemStackTo(Stack, InventoryStart, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
            Source.onQuickCraft(Stack, Original);
        } else if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, InventoryStart, InventoryEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (Stack.isEmpty()) {
            Source.setByPlayer(ItemStack.EMPTY);
        } else {
            Source.setChanged();
        }
        if (Stack.getCount() == Original.getCount()) {
            return ItemStack.EMPTY;
        }
        Source.onTake(Clicker, Stack);
        return Original;
    }

    @Override
    public void removed(Player Clicker) {
        super.removed(Clicker);
        Access.execute((World, Pos) -> clearContainer(Clicker, Output));
    }

    @Override
    public boolean stillValid(Player Clicker) {
        return stillValid(Access, Clicker, GunsRegistry.GunBench.get());
    }
}
