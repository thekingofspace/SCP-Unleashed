package net.rai.scp_unleashed.archive;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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

public class ArchiveMenu extends AbstractContainerMenu {
    public static final int FragmentX = 184;
    public static final int FragmentY = 166;
    public static final int PaperX = 206;
    public static final int PaperY = 166;
    public static final int OutputX = 226;
    public static final int OutputY = 218;
    public static final int InventoryX = 8;
    public static final int InventoryY = 164;
    public static final int HotbarY = 222;
    public static final int RollButton = 0;
    public static final int PickButton = 1;

    @Nullable
    private final ArchiveBlockEntity Terminal;
    private final ContainerData Data;
    private final BlockPos Pos;

    public ArchiveMenu(int ContainerId, Inventory PlayerInventory, @Nullable RegistryFriendlyByteBuf Buffer) {
        this(ContainerId, PlayerInventory, Opening.read(Buffer));
    }

    private ArchiveMenu(int ContainerId, Inventory PlayerInventory, Opening Opened) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(ArchiveBlockEntity.SlotCount), prefilled(Opened.Values()), Opened.Pos());
    }

    public ArchiveMenu(int ContainerId, Inventory PlayerInventory, ArchiveBlockEntity Terminal, ContainerData Data) {
        this(ContainerId, PlayerInventory, Terminal, Terminal.getStorage(), Data, Terminal.getBlockPos());
    }

    private ArchiveMenu(int ContainerId, Inventory PlayerInventory, @Nullable ArchiveBlockEntity Terminal, IItemHandler Storage, ContainerData Data, BlockPos Pos) {
        super(ArchiveRegistry.ArchiveMenuType.get(), ContainerId);
        this.Terminal = Terminal;
        this.Data = Data;
        this.Pos = Pos;
        addSlot(new SlotItemHandler(Storage, ArchiveBlockEntity.FragmentSlot, FragmentX, FragmentY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return ArchiveBlockEntity.isFragment(Stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                markTerminal();
            }
        });
        addSlot(new SlotItemHandler(Storage, ArchiveBlockEntity.PaperSlot, PaperX, PaperY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return ArchiveBlockEntity.isPaper(Stack);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                markTerminal();
            }
        });
        addSlot(new SlotItemHandler(Storage, ArchiveBlockEntity.OutputSlot, OutputX, OutputY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return false;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                markTerminal();
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

    private void markTerminal() {
        if (Terminal != null) {
            Terminal.setChanged();
        }
    }

    private static ContainerData prefilled(int[] Values) {
        SimpleContainerData Filled = new SimpleContainerData(ArchiveBlockEntity.DataCount);
        for (int Index = 0; Index < Values.length; Index++) {
            Filled.set(Index, Values[Index]);
        }
        return Filled;
    }

    public BlockPos getPos() {
        return Pos;
    }

    public int cardCode(int Index) {
        return Index >= 0 && Index < ArchiveBlockEntity.CardCount ? Data.get(ArchiveBlockEntity.CardData + Index) : 0;
    }

    public int cardCount() {
        return Data.get(ArchiveBlockEntity.CountData);
    }

    public int serial() {
        return Data.get(ArchiveBlockEntity.SerialData);
    }

    public int printRemaining() {
        return Data.get(ArchiveBlockEntity.RemainingData);
    }

    public int printingCard() {
        return Data.get(ArchiveBlockEntity.PrintingData);
    }

    public int tierMask() {
        return Data.get(ArchiveBlockEntity.MaskData);
    }

    public int rollTier() {
        return Data.get(ArchiveBlockEntity.TierData);
    }

    public ItemStack getFragment() {
        return slots.get(ArchiveBlockEntity.FragmentSlot).getItem();
    }

    public ItemStack getPaper() {
        return slots.get(ArchiveBlockEntity.PaperSlot).getItem();
    }

    public ItemStack getOutput() {
        return slots.get(ArchiveBlockEntity.OutputSlot).getItem();
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Terminal == null || !(User instanceof ServerPlayer Reader)) {
            return false;
        }
        if (Id == RollButton) {
            return Terminal.roll(Reader);
        }
        if (Id >= PickButton && Id < PickButton + ArchiveBlockEntity.CardCount) {
            return Terminal.pick(Reader, Id - PickButton);
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        int MachineEnd = ArchiveBlockEntity.SlotCount;
        int InventoryEnd = MachineEnd + 27;
        int HotbarEnd = InventoryEnd + 9;
        if (Index < MachineEnd) {
            if (!moveItemStackTo(Stack, MachineEnd, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (ArchiveBlockEntity.isFragment(Stack)) {
            if (!moveItemStackTo(Stack, ArchiveBlockEntity.FragmentSlot, ArchiveBlockEntity.FragmentSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ArchiveBlockEntity.isPaper(Stack)) {
            if (!moveItemStackTo(Stack, ArchiveBlockEntity.PaperSlot, ArchiveBlockEntity.PaperSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, MachineEnd, InventoryEnd, false)) {
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
        return Terminal == null || Terminal.isUsableBy(User);
    }

    private record Opening(BlockPos Pos, int[] Values) {
        private static Opening read(@Nullable RegistryFriendlyByteBuf Buffer) {
            if (Buffer == null || Buffer.readableBytes() < Long.BYTES) {
                return new Opening(BlockPos.ZERO, new int[ArchiveBlockEntity.DataCount]);
            }
            BlockPos Pos = Buffer.readBlockPos();
            return new Opening(Pos, ArchiveBlockEntity.readState(Buffer));
        }
    }
}
