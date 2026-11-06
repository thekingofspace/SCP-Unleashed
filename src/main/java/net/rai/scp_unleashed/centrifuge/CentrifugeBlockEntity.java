package net.rai.scp_unleashed.centrifuge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.upgrade.MachineUpgrades;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class CentrifugeBlockEntity extends BlockEntity implements WireDevice, MenuProvider, Upgradeable {
    public static final int SlotCount = 5;
    public static final int InputSlot = 0;
    public static final int FirstOutput = 1;
    public static final int OutputCount = 4;
    public static final int WorkDraw = 40;
    public static final int ActiveHold = 10;
    public static final int SyncTicks = 5;
    public static final int SpinSteps = 16;
    public static final float MaxSpinDegrees = 45.0f;
    public static final float SpinUpRate = 0.055f;
    public static final float SpinUpFloor = 0.004f;
    public static final float SpinDownRate = 0.03f;
    public static final float SpinDownFloor = 0.0035f;
    public static final int ProgressScale = 1000;
    public static final int DataCount = 4;
    public static final int DataProgress = 0;
    public static final int DataReceived = 1;
    public static final int DataDemand = 2;
    public static final int DataStatus = 3;

    private static Consumer<CentrifugeBlockEntity> ClientEffects = Machine -> {
    };

    private final ItemStackHandler Contents = new ItemStackHandler(SlotCount) {
        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
            if (Slot == InputSlot) {
                InputDirty = true;
            }
            OutputDirty = true;
        }
    };
    private final CentrifugeItemHandler Feed = new CentrifugeItemHandler(this, CentrifugeItemHandler.Access.Feed);
    private final CentrifugeItemHandler Drain = new CentrifugeItemHandler(this, CentrifugeItemHandler.Access.Drain);
    private final CentrifugeItemHandler Whole = new CentrifugeItemHandler(this, CentrifugeItemHandler.Access.Both);
    private final MachineUpgrades Upgrades = new MachineUpgrades(this::setChanged, UpgradeType.Speed);

    @Nullable
    private RecipeManager KnownRecipes;
    @Nullable
    private RecipeHolder<CentrifugeRecipe> Current;
    @Nullable
    private ResourceLocation CurrentId;
    private boolean InputDirty = true;
    private boolean OutputDirty = true;
    private boolean Fits;
    private int Received;
    private int LastReceived;
    private int Demand;
    private int Progress;
    private float Carry;
    private CentrifugeStatus Status = CentrifugeStatus.Idle;
    private long LastWorked = Long.MIN_VALUE / 2;
    private int Rate;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private boolean Primed;
    private float Spin;
    private float PreviousSpin;
    private float Angle;
    private float PreviousAngle;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataProgress -> Math.round(getProgressFraction() * ProgressScale);
                case DataReceived -> LastReceived;
                case DataDemand -> Demand;
                case DataStatus -> Status.ordinal();
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

    public CentrifugeBlockEntity(BlockPos Pos, BlockState State) {
        super(CentrifugeRegistry.CentrifugeEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<CentrifugeBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static boolean isProcessable(@Nullable Level World, ItemStack Stack) {
        if (World == null || Stack.isEmpty()) {
            return false;
        }
        for (RecipeHolder<CentrifugeRecipe> Known : World.getRecipeManager().getAllRecipesFor(CentrifugeRegistry.Centrifuging.get())) {
            if (Known.value().accepts(Stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isProcessable(ItemStack Stack) {
        return isProcessable(level, Stack);
    }

    public ItemStackHandler getContents() {
        return Contents;
    }

    public IItemHandler getItemHandler(@Nullable Direction Side) {
        if (Side == null) {
            return Whole;
        }
        return Side == Direction.DOWN ? Drain : Feed;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, CentrifugeBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof CentrifugeBlock) {
            Machine.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, CentrifugeBlockEntity Machine) {
        Machine.tickClient();
        ClientEffects.accept(Machine);
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        boolean WasDemanding = Demand > 0;
        RecipeManager Recipes = World.getRecipeManager();
        if (Recipes != KnownRecipes) {
            KnownRecipes = Recipes;
            Current = null;
            InputDirty = true;
        }
        if (InputDirty) {
            InputDirty = false;
            refreshRecipe(World);
            OutputDirty = true;
        }
        if (OutputDirty) {
            OutputDirty = false;
            Fits = Current != null && placeAll(Current.value().getResults().stream().map(CentrifugeOutput::Stack).toList(), true);
        }
        int Draw = Upgrades.draw(WorkDraw);
        int Used = 0;
        if (Current == null) {
            Status = CentrifugeStatus.Idle;
        } else if (!Fits) {
            Status = CentrifugeStatus.Blocked;
        } else if (WasDemanding) {
            Used = Math.min(Delivered, Draw);
            Status = Used >= Draw ? CentrifugeStatus.Spinning : Used > 0 ? CentrifugeStatus.LowPower : CentrifugeStatus.Unpowered;
            if (Used > 0) {
                float Gain = Used * Upgrades.speedFactor() / Upgrades.powerFactor() + Carry;
                int Earned = (int) Gain;
                Carry = Gain - Earned;
                Progress += Earned;
                LastWorked = Now;
                setChanged();
                if (Progress >= Current.value().getEnergy()) {
                    finish(World, Current.value());
                }
            }
        }
        Demand = Current != null && Fits ? Draw : 0;
        boolean Active = Now - LastWorked <= ActiveHold;
        if (State.getValue(CentrifugeBlock.Active) != Active) {
            World.setBlock(Pos, State.setValue(CentrifugeBlock.Active, Active), Block.UPDATE_CLIENTS);
        }
        updateRate(World, Now, Used, Draw);
    }

    private void refreshRecipe(Level World) {
        ItemStack Loaded = Contents.getStackInSlot(InputSlot);
        RecipeHolder<CentrifugeRecipe> Found = Loaded.isEmpty() ? null
                : World.getRecipeManager().getRecipeFor(CentrifugeRegistry.Centrifuging.get(), new SingleRecipeInput(Loaded), World, Current).orElse(null);
        ResourceLocation FoundId = Found == null ? null : Found.id();
        if (!Objects.equals(FoundId, CurrentId)) {
            Progress = 0;
        }
        Current = Found;
        CurrentId = FoundId;
    }

    private void finish(ServerLevel World, CentrifugeRecipe Separation) {
        List<ItemStack> Rolled = new ArrayList<>();
        for (CentrifugeOutput Output : Separation.getResults()) {
            ItemStack Stack = Output.roll(World.getRandom());
            if (!Stack.isEmpty()) {
                Rolled.add(Stack);
            }
        }
        if (!placeAll(Rolled, false)) {
            Progress = Separation.getEnergy();
            Fits = false;
            return;
        }
        Contents.extractItem(InputSlot, Separation.getInput().count(), false);
        Progress = 0;
    }

    private boolean placeAll(List<ItemStack> Stacks, boolean Simulate) {
        ItemStack[] Slots = new ItemStack[OutputCount];
        for (int Index = 0; Index < OutputCount; Index++) {
            Slots[Index] = Contents.getStackInSlot(FirstOutput + Index).copy();
        }
        for (ItemStack Stack : Stacks) {
            ItemStack Remaining = Stack.copy();
            for (int Pass = 0; Pass < 2 && !Remaining.isEmpty(); Pass++) {
                for (int Index = 0; Index < OutputCount && !Remaining.isEmpty(); Index++) {
                    ItemStack Held = Slots[Index];
                    boolean Candidate = Pass == 0 ? !Held.isEmpty() && ItemStack.isSameItemSameComponents(Held, Remaining) : Held.isEmpty();
                    if (!Candidate) {
                        continue;
                    }
                    int Room = Math.min(Contents.getSlotLimit(FirstOutput + Index), Remaining.getMaxStackSize()) - Held.getCount();
                    int Moved = Math.min(Room, Remaining.getCount());
                    if (Moved <= 0) {
                        continue;
                    }
                    if (Held.isEmpty()) {
                        Slots[Index] = Remaining.copyWithCount(Moved);
                    } else {
                        Held.grow(Moved);
                    }
                    Remaining.shrink(Moved);
                }
            }
            if (!Remaining.isEmpty()) {
                return false;
            }
        }
        if (!Simulate) {
            for (int Index = 0; Index < OutputCount; Index++) {
                if (!ItemStack.matches(Slots[Index], Contents.getStackInSlot(FirstOutput + Index))) {
                    Contents.setStackInSlot(FirstOutput + Index, Slots[Index]);
                }
            }
        }
        return true;
    }

    private void updateRate(ServerLevel World, long Now, int Used, int Draw) {
        int Wanted = Mth.clamp(Math.round((float) Used * SpinSteps / Draw), 0, SpinSteps);
        if (Wanted != Rate) {
            UrgentSync |= Wanted == 0 || Rate == 0;
            Rate = Wanted;
            NeedsSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void tickClient() {
        float Target = (float) Rate / SpinSteps;
        if (!Primed) {
            Primed = true;
            Spin = Target;
        }
        PreviousSpin = Spin;
        if (Spin < Target) {
            Spin = Math.min(Target, Spin + (Target - Spin) * SpinUpRate + SpinUpFloor);
        } else if (Spin > Target) {
            Spin = Math.max(Target, Spin - (Spin - Target) * SpinDownRate - SpinDownFloor);
        }
        PreviousAngle = Angle;
        Angle += Spin * MaxSpinDegrees;
        if (Angle >= 360.0f) {
            Angle -= 360.0f;
            PreviousAngle -= 360.0f;
        }
    }

    public float getSpin() {
        return Spin;
    }

    public float getRenderSpin(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousSpin, Spin);
    }

    public float getRenderAngle(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousAngle, Angle);
    }

    public CentrifugeStatus getStatus() {
        return Status;
    }

    public float getProgressFraction() {
        if (Current == null) {
            return 0.0f;
        }
        return Mth.clamp((float) Progress / Current.value().getEnergy(), 0.0f, 1.0f);
    }

    public int getDemand() {
        return Demand;
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Contents.getStackInSlot(Slot).copy());
            Contents.setStackInSlot(Slot, ItemStack.EMPTY);
        }
        Upgrades.drop(World, Pos);
    }

    @Override
    public boolean acceptsUpgrade(UpgradeType Type) {
        return Upgrades.accepts(Type);
    }

    @Override
    public boolean hasUpgrade(UpgradeType Type) {
        return Upgrades.count(Type) > 0;
    }

    @Override
    public int countUpgrade(UpgradeType Type) {
        return Upgrades.count(Type);
    }

    @Override
    public void installUpgrade(UpgradeType Type) {
        Upgrades.install(Type);
    }

    @Override
    public Collection<UpgradeType> getUpgrades() {
        return Upgrades.list();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Contents", Contents.serializeNBT(Registries));
        Upgrades.save(Tag);
        Tag.putInt("Progress", Progress);
        if (CurrentId != null) {
            Tag.putString("Recipe", CurrentId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Contents")) {
            Contents.deserializeNBT(Registries, Tag.getCompound("Contents"));
        }
        Upgrades.load(Tag);
        if (Tag.contains("Progress")) {
            Progress = Math.max(0, Tag.getInt("Progress"));
        }
        if (Tag.contains("Recipe")) {
            CurrentId = ResourceLocation.tryParse(Tag.getString("Recipe"));
        }
        if (Tag.contains("Rate")) {
            Rate = Mth.clamp(Tag.getByte("Rate"), 0, SpinSteps);
        }
        InputDirty = true;
        OutputDirty = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Rate", (byte) Rate);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.centrifuge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new CentrifugeMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
