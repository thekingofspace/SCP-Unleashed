package net.rai.scp_unleashed.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.GasTank;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class WaterBoilerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FuelSlots = 3;
    public static final int WaterCapacity = 16000;
    public static final int SteamCapacity = 16000;
    public static final int BoilRate = 20;
    public static final int BurnSpeed = 3;
    public static final int MaxHeat = 1000;
    public static final int HeatUp = 5;
    public static final int CoolDown = 5;
    public static final int LitHold = 20;
    public static final int LightCheckTicks = 40;
    public static final int HeatSteps = 10;
    public static final int SyncTicks = 10;
    public static final int BurnSteps = 1000;
    public static final int DataBurn = 0;
    public static final int DataHeat = 1;
    public static final int DataWater = 2;
    public static final int DataSteam = 3;
    public static final int DataStatus = 4;
    public static final int DataBoiled = 5;
    public static final int DataCount = 6;

    private static Consumer<WaterBoilerBlockEntity> ClientEffects = Machine -> {
    };

    private final ItemStackHandler Fuel = new ItemStackHandler(FuelSlots) {
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
            return FuelSlots;
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
    private final FluidTank Water = new FluidTank(WaterCapacity, Stack -> Stack.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final GasTank Steam = new GasTank(SteamCapacity, Kind -> Kind == GasRegistry.Steam.get(), this::setChanged);

    private int BurnTime;
    private int BurnTotal;
    private int Heat;
    private float Carry;
    private int LastBoiled;
    private int LitTimer;
    private MachineLight LastLight = MachineLight.Off;
    private BoilerStatus Status = BoilerStatus.Idle;
    private int ShownHeat;
    private boolean ShownBlocked;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataBurn -> burnShown();
                case DataHeat -> Heat;
                case DataWater -> Water.getFluidAmount();
                case DataSteam -> Steam.getAmount();
                case DataStatus -> Status.ordinal();
                case DataBoiled -> LastBoiled;
                default -> 0;
            };
        }

        @Override
        public void set(int Index, int Value) {
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    };

    public WaterBoilerBlockEntity(BlockPos Pos, BlockState State) {
        super(SteamRegistry.WaterBoilerEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<WaterBoilerBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static boolean isFuel(ItemStack Stack) {
        return !Stack.isEmpty() && burnTicks(Stack) > 0;
    }

    public static int burnTicks(ItemStack Stack) {
        return Stack.getBurnTime(RecipeType.SMELTING);
    }

    public static int steamFor(int BurnTicks) {
        return BurnTicks / BurnSpeed * BoilRate;
    }

    public ItemStackHandler getFuel() {
        return Fuel;
    }

    public IItemHandler getAutomation() {
        return Automation;
    }

    public FluidTank getWaterTank() {
        return Water;
    }

    public GasTank getSteamTank() {
        return Steam;
    }

    public BoilerStatus getStatus() {
        return Status;
    }

    public int getHeat() {
        return Heat;
    }

    public int getLastBoiled() {
        return LastBoiled;
    }

    public int getBurnTime() {
        return BurnTime;
    }

    public int getBurnTotal() {
        return BurnTotal;
    }

    public int burnShown() {
        return BurnTotal <= 0 || BurnTime <= 0 ? 0 : (int) Mth.clamp(Math.round((double) BurnTime * BurnSteps / BurnTotal), 1L, (long) BurnSteps);
    }

    public int getShownHeat() {
        return ShownHeat;
    }

    public boolean isShownBlocked() {
        return ShownBlocked;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, WaterBoilerBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof WaterBoilerBlock) {
            Machine.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, WaterBoilerBlockEntity Machine) {
        ClientEffects.accept(Machine);
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        boolean HasWater = Water.getFluidAmount() > 0;
        boolean HasRoom = Steam.getSpace() > 0;
        if (BurnTime <= 0 && HasWater && HasRoom) {
            ignite(World, Pos);
        }
        boolean Burning = BurnTime > 0 && HasWater && HasRoom;
        if (Burning) {
            BurnTime = Math.max(0, BurnTime - BurnSpeed);
            Heat = Math.min(MaxHeat, Heat + HeatUp);
            setChanged();
        } else if (Heat > 0) {
            Heat = Math.max(0, Heat - CoolDown);
            setChanged();
        }
        float Wanted = (float) BoilRate * Heat / MaxHeat + Carry;
        int Whole = (int) Wanted;
        int Boiled = Math.min(Whole, Math.min(Water.getFluidAmount(), Steam.getSpace()));
        Carry = Boiled == Whole ? Wanted - Whole : 0.0f;
        if (Boiled > 0) {
            Water.drain(Boiled, IFluidHandler.FluidAction.EXECUTE);
            Steam.fill(new GasStack(GasRegistry.Steam.get(), Boiled), false);
        }
        LastBoiled = Boiled;
        if (!Water.isEmpty() || Boiled > 0) {
            HasWater = true;
        }
        if (!HasWater) {
            Status = BoilerStatus.NoWater;
        } else if (Steam.getSpace() <= 0) {
            Status = BoilerStatus.Blocked;
        } else if (Burning) {
            Status = Heat < MaxHeat ? BoilerStatus.Heating : BoilerStatus.Boiling;
        } else if (Heat > 0) {
            Status = BoilerStatus.Cooling;
        } else {
            Status = BoilerStatus.Idle;
        }
        if (Burning) {
            LitTimer = LitHold;
        } else if (LitTimer > 0) {
            LitTimer--;
        }
        MachineLight Shown = Status == BoilerStatus.NoWater || Status == BoilerStatus.Blocked ? MachineLight.Alert
                : LitTimer > 0 ? MachineLight.Working : Heat > 0 ? MachineLight.Waiting : MachineLight.Off;
        if (Shown != LastLight || World.getGameTime() % LightCheckTicks == 0) {
            LastLight = Shown;
            if (State.getBlock() instanceof WaterBoilerBlock Boiler) {
                Boiler.showLight(World, Pos, State, Shown);
            }
        }
        updateShown(World, Boiled > 0 ? Mth.clamp(Math.round((float) Heat * HeatSteps / MaxHeat), 1, HeatSteps) : 0, Status == BoilerStatus.Blocked);
    }

    private void ignite(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < FuelSlots; Slot++) {
            ItemStack Stack = Fuel.getStackInSlot(Slot);
            if (!isFuel(Stack)) {
                continue;
            }
            BurnTime = burnTicks(Stack);
            BurnTotal = BurnTime;
            ItemStack Remainder = Stack.getCraftingRemainingItem();
            Fuel.extractItem(Slot, 1, false);
            if (Remainder.isEmpty()) {
                return;
            }
            if (Fuel.getStackInSlot(Slot).isEmpty()) {
                Fuel.setStackInSlot(Slot, Remainder);
            } else {
                Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 1.1, Pos.getZ() + 0.5, Remainder);
            }
            return;
        }
    }

    private void updateShown(ServerLevel World, int Step, boolean Blocked) {
        if (Step != ShownHeat || Blocked != ShownBlocked) {
            UrgentSync |= (Step == 0) != (ShownHeat == 0) || Blocked != ShownBlocked;
            ShownHeat = Step;
            ShownBlocked = Blocked;
            NeedsSync = true;
        }
        long Now = World.getGameTime();
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < FuelSlots; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Fuel.getStackInSlot(Slot).copy());
            Fuel.setStackInSlot(Slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Fuel", Fuel.serializeNBT(Registries));
        Tag.put("Water", Water.writeToNBT(Registries, new CompoundTag()));
        Steam.save(Tag, "Steam");
        Tag.putInt("BurnTime", BurnTime);
        Tag.putInt("BurnTotal", BurnTotal);
        Tag.putInt("Heat", Heat);
        Tag.putFloat("Carry", Carry);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Shown")) {
            ShownHeat = Mth.clamp(Tag.getByte("Shown"), 0, HeatSteps);
            ShownBlocked = Tag.getBoolean("Venting");
            return;
        }
        if (Tag.contains("Fuel")) {
            Fuel.deserializeNBT(Registries, Tag.getCompound("Fuel"));
        }
        if (Tag.contains("Water")) {
            Water.readFromNBT(Registries, Tag.getCompound("Water"));
        }
        Steam.load(Tag, "Steam");
        BurnTime = Math.max(0, Tag.getInt("BurnTime"));
        BurnTotal = Math.max(0, Tag.getInt("BurnTotal"));
        Heat = Mth.clamp(Tag.getInt("Heat"), 0, MaxHeat);
        Carry = Mth.clamp(Tag.getFloat("Carry"), 0.0f, 1.0f);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Shown", (byte) ShownHeat);
        Tag.putBoolean("Venting", ShownBlocked);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.water_boiler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new WaterBoilerMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5) <= 100.0;
    }
}
