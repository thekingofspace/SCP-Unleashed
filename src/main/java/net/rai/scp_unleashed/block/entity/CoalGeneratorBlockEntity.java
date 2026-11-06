package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.block.CoalGeneratorBlock;
import net.rai.scp_unleashed.menu.CoalGeneratorMenu;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

public class CoalGeneratorBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int SlotCount = 3;
    public static final int PowerPerSlot = 10;
    public static final int Capacity = 10000;
    public static final int MaxOutput = SlotCount * PowerPerSlot;
    public static final int DataCount = SlotCount * 2 + 2;
    public static final int LitHold = 20;

    private final ItemStackHandler Fuel = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return isFuel(Stack);
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
        }
    };
    private final IItemHandler Automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SlotCount;
        }

        @Override
        public ItemStack getStackInSlot(int Slot) {
            return Fuel.getStackInSlot(Slot);
        }

        @Override
        public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
            return Fuel.insertItem(Slot, Stack, Simulate);
        }

        @Override
        public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
            return isFuel(Fuel.getStackInSlot(Slot)) ? ItemStack.EMPTY : Fuel.extractItem(Slot, Amount, Simulate);
        }

        @Override
        public int getSlotLimit(int Slot) {
            return Fuel.getSlotLimit(Slot);
        }

        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Fuel.isItemValid(Slot, Stack);
        }
    };
    private final int[] BurnTime = new int[SlotCount];
    private final int[] BurnTotal = new int[SlotCount];
    private int Stored;
    private int BurningSlots;
    private int LitTimer;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            if (Index < SlotCount) {
                return BurnTime[Index];
            }
            if (Index < SlotCount * 2) {
                return BurnTotal[Index - SlotCount];
            }
            return Index == SlotCount * 2 ? Stored : BurningSlots;
        }

        @Override
        public void set(int Index, int Value) {
            if (Index < SlotCount) {
                BurnTime[Index] = Value;
            } else if (Index < SlotCount * 2) {
                BurnTotal[Index - SlotCount] = Value;
            } else if (Index == SlotCount * 2) {
                Stored = Value;
            } else {
                BurningSlots = Value;
            }
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    };

    public CoalGeneratorBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.CoalGenerator.get(), Pos, State);
    }

    public static boolean isFuel(ItemStack Stack) {
        return !Stack.isEmpty() && burnTicks(Stack) > 0;
    }

    public static int burnTicks(ItemStack Stack) {
        return Stack.getBurnTime(RecipeType.SMELTING);
    }

    public ItemStackHandler getFuel() {
        return Fuel;
    }

    public IItemHandler getAutomation() {
        return Automation;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, CoalGeneratorBlockEntity Generator) {
        Generator.tickServer(World, Pos, State);
    }

    private void tickServer(Level World, BlockPos Pos, BlockState State) {
        int Burning = 0;
        boolean Changed = false;
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            if (Stored + PowerPerSlot > Capacity) {
                break;
            }
            if (BurnTime[Slot] <= 0) {
                ItemStack Stack = Fuel.getStackInSlot(Slot);
                if (!isFuel(Stack)) {
                    continue;
                }
                BurnTime[Slot] = burnTicks(Stack);
                BurnTotal[Slot] = BurnTime[Slot];
                consumeFuel(World, Pos, Slot, Stack);
            }
            BurnTime[Slot]--;
            Stored += PowerPerSlot;
            Burning++;
            Changed = true;
        }
        BurningSlots = Burning;
        if (Changed) {
            setChanged();
        }
        if (Burning > 0) {
            LitTimer = LitHold;
        } else if (LitTimer > 0) {
            LitTimer--;
        }
        boolean Lit = LitTimer > 0;
        if (State.getValue(CoalGeneratorBlock.Lit) != Lit) {
            CoalGeneratorBlock.setLit(World, Pos, State, Lit);
        }
    }

    private void consumeFuel(Level World, BlockPos Pos, int Slot, ItemStack Stack) {
        ItemStack Remainder = Stack.getCraftingRemainingItem();
        Fuel.extractItem(Slot, 1, false);
        if (Remainder.isEmpty()) {
            return;
        }
        if (Fuel.getStackInSlot(Slot).isEmpty()) {
            Fuel.setStackInSlot(Slot, Remainder);
        } else {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 2.1, Pos.getZ() + 0.5, Remainder);
        }
    }

    @Override
    public int getPowerOutput() {
        return Math.min(Stored, MaxOutput);
    }

    @Override
    public int getReserveOutput() {
        return WireDevice.bufferedReserve(this);
    }

    @Override
    public boolean isGenerator() {
        return true;
    }

    @Override
    public int getGenerationRate() {
        return BurningSlots * PowerPerSlot;
    }

    @Override
    public void extractPower(int Amount) {
        Stored = Math.max(0, Stored - Amount);
        setChanged();
    }

    @Override
    public int getStoredPower() {
        return Stored;
    }

    @Override
    public int getPowerCapacity() {
        return Capacity;
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Fuel.getStackInSlot(Slot).copy());
            Fuel.setStackInSlot(Slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Fuel", Fuel.serializeNBT(Registries));
        Tag.putIntArray("BurnTime", BurnTime);
        Tag.putIntArray("BurnTotal", BurnTotal);
        Tag.putInt("Stored", Stored);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Fuel")) {
            Fuel.deserializeNBT(Registries, Tag.getCompound("Fuel"));
        }
        int[] SavedTime = Tag.getIntArray("BurnTime");
        int[] SavedTotal = Tag.getIntArray("BurnTotal");
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            BurnTime[Slot] = Slot < SavedTime.length ? SavedTime[Slot] : 0;
            BurnTotal[Slot] = Slot < SavedTotal.length ? SavedTotal[Slot] : 0;
        }
        Stored = Math.min(Capacity, Tag.getInt("Stored"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.coal_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new CoalGeneratorMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
