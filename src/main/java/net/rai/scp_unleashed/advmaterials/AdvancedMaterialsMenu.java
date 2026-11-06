package net.rai.scp_unleashed.advmaterials;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdvancedMaterialsMenu extends AbstractContainerMenu {
    public static final int[] InputX = {26, 44, 26, 44};
    public static final int[] InputY = {26, 26, 44, 44};
    public static final int OutputX = 116;
    public static final int OutputY = 35;
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int MachineSlots = AdvancedMaterialsBenchBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineSlots + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final AdvancedMaterialsBenchBlockEntity Machine;
    private final ContainerData Data;
    private final IItemHandler Handler;

    public AdvancedMaterialsMenu(int ContainerId, Inventory PlayerInventory, @Nullable RegistryFriendlyByteBuf Snapshot) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(AdvancedMaterialsBenchBlockEntity.SlotCount), readState(Snapshot));
    }

    public AdvancedMaterialsMenu(int ContainerId, Inventory PlayerInventory, AdvancedMaterialsBenchBlockEntity Machine, ContainerData Data) {
        this(ContainerId, PlayerInventory, Machine, Machine.getContents(), Data);
    }

    private AdvancedMaterialsMenu(int ContainerId, Inventory PlayerInventory, @Nullable AdvancedMaterialsBenchBlockEntity Machine, IItemHandler Handler, ContainerData Data) {
        super(AdvancedMaterialsRegistry.BenchMenu.get(), ContainerId);
        this.Machine = Machine;
        this.Data = Data;
        this.Handler = Handler;
        Player Viewer = PlayerInventory.player;
        for (int Index = 0; Index < AdvancedMaterialsBenchBlockEntity.InputCount; Index++) {
            addSlot(new SlotItemHandler(Handler, Index, InputX[Index], InputY[Index]) {
                @Override
                public boolean mayPlace(ItemStack Stack) {
                    return AdvancedMaterialsRecipes.isIngredient(Viewer.level(), Stack) && AdvancedMaterialsRecipes.canJoin(Viewer.level(), currentInputs(), Stack);
                }
            });
        }
        addSlot(new SlotItemHandler(Handler, AdvancedMaterialsBenchBlockEntity.OutputSlot, OutputX, OutputY) {
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
        addDataSlots(Data);
    }

    public static void writeState(RegistryFriendlyByteBuf Buffer, ContainerData Source) {
        for (int Index = 0; Index < AdvancedMaterialsBenchBlockEntity.DataCount; Index++) {
            Buffer.writeVarInt(Source.get(Index));
        }
    }

    private static ContainerData readState(@Nullable RegistryFriendlyByteBuf Snapshot) {
        SimpleContainerData State = new SimpleContainerData(AdvancedMaterialsBenchBlockEntity.DataCount);
        if (Snapshot != null) {
            for (int Index = 0; Index < AdvancedMaterialsBenchBlockEntity.DataCount; Index++) {
                State.set(Index, Snapshot.readVarInt());
            }
        }
        return State;
    }

    private List<ItemStack> currentInputs() {
        List<ItemStack> Items = new ArrayList<>(AdvancedMaterialsBenchBlockEntity.InputCount);
        for (int Slot = 0; Slot < AdvancedMaterialsBenchBlockEntity.InputCount; Slot++) {
            Items.add(Handler.getStackInSlot(Slot));
        }
        return Items;
    }

    public float getProgress() {
        return Math.min(1.0f, Data.get(AdvancedMaterialsBenchBlockEntity.DataProgress) / (float) AdvancedMaterialsBenchBlockEntity.ProgressScale);
    }

    public int getReceived() {
        return Data.get(AdvancedMaterialsBenchBlockEntity.DataReceived);
    }

    public int getDemand() {
        return Data.get(AdvancedMaterialsBenchBlockEntity.DataDemand);
    }

    public int getDraw() {
        return Data.get(AdvancedMaterialsBenchBlockEntity.DataDraw);
    }

    public int getTime() {
        return Data.get(AdvancedMaterialsBenchBlockEntity.DataTime);
    }

    public BenchStatus getStatus() {
        return BenchStatus.byIndex(Data.get(AdvancedMaterialsBenchBlockEntity.DataStatus));
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < MachineSlots) {
            if (!moveItemStackTo(Stack, MachineSlots, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (AdvancedMaterialsRecipes.isIngredient(User.level(), Stack) && AdvancedMaterialsRecipes.canJoin(User.level(), currentInputs(), Stack)) {
            if (!moveItemStackTo(Stack, 0, AdvancedMaterialsBenchBlockEntity.InputCount, false)) {
                return ItemStack.EMPTY;
            }
        } else if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, MachineSlots, InventoryEnd, false)) {
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
        Source.onTake(User, Stack);
        return Original;
    }

    @Override
    public boolean stillValid(Player User) {
        return Machine == null || Machine.isUsableBy(User);
    }
}
