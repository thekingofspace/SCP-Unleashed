package net.rai.scp_unleashed.refinery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.FissionTags;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
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

public class OrePurifierBlockEntity extends BlockEntity implements WireDevice, MenuProvider, Upgradeable {
    public static final int SlotCount = 7;
    public static final int InputSlot = 0;
    public static final int FirstOutput = 1;
    public static final int OutputCount = 6;
    public static final int TankCapacity = 8000;
    public static final int WorkDraw = 80;
    public static final int IdleDraw = 0;
    public static final int ActiveHold = 10;
    public static final int AlertDelay = 20;
    public static final int PowerGrace = 10;
    public static final int SyncTicks = 5;
    public static final int RateSteps = 16;
    public static final int ProgressScale = 1000;
    public static final double ReachSquared = 100.0;
    public static final int DataProgress = 0;
    public static final int DataReceived = 1;
    public static final int DataDemand = 2;
    public static final int DataStatus = 3;
    public static final int DataWater = 4;
    public static final int DataWaste = 5;
    public static final int DataCount = 6;

    private static Consumer<OrePurifierBlockEntity> ClientEffects = Machine -> {
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
    private final FluidTank Water = new FluidTank(TankCapacity, Stack -> Stack.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank Waste = new FluidTank(TankCapacity, Stack -> Stack.is(FissionTags.WasteWater)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler WaterInlet = new RestrictedFluidHandler(Water, PortRole.Input);
    private final IFluidHandler WasteOutlet = new RestrictedFluidHandler(Waste, PortRole.Output);
    private final IFluidHandler Combined = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int Tank) {
            return Tank == 0 ? Water.getFluid() : Waste.getFluid();
        }

        @Override
        public int getTankCapacity(int Tank) {
            return TankCapacity;
        }

        @Override
        public boolean isFluidValid(int Tank, FluidStack Stack) {
            return Tank == 0 && Water.isFluidValid(Stack);
        }

        @Override
        public int fill(FluidStack Resource, FluidAction Action) {
            return Water.fill(Resource, Action);
        }

        @Override
        public FluidStack drain(FluidStack Resource, FluidAction Action) {
            return Waste.drain(Resource, Action);
        }

        @Override
        public FluidStack drain(int MaxDrain, FluidAction Action) {
            return Waste.drain(MaxDrain, Action);
        }
    };
    private final PurifierItemHandler Feed = new PurifierItemHandler(this, PurifierItemHandler.Access.Feed);
    private final PurifierItemHandler Drain = new PurifierItemHandler(this, PurifierItemHandler.Access.Drain);
    private final PurifierItemHandler Whole = new PurifierItemHandler(this, PurifierItemHandler.Access.Both);
    private final MachineUpgrades Upgrades = new MachineUpgrades(this::setChanged, UpgradeType.Speed);

    @Nullable
    private RecipeManager KnownRecipes;
    @Nullable
    private RecipeHolder<PurifyingRecipe> Current;
    @Nullable
    private ResourceLocation CurrentId;
    private boolean InputDirty = true;
    private boolean OutputDirty = true;
    private boolean Fits;
    private int Received;
    private int LastReceived;
    private int Demand = IdleDraw;
    private int Progress;
    private float Carry;
    private PurifierStatus Status = PurifierStatus.Idle;
    private long LastWorked = Long.MIN_VALUE / 2;
    private long LastPowered = Long.MIN_VALUE / 2;
    private long AlertSince = Long.MAX_VALUE;
    private int Rate;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private boolean Primed;
    private float Churn;
    private float PreviousChurn;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataProgress -> Math.round(getProgressFraction() * ProgressScale);
                case DataReceived -> LastReceived;
                case DataDemand -> Demand;
                case DataStatus -> Status.ordinal();
                case DataWater -> Water.getFluidAmount();
                case DataWaste -> Waste.getFluidAmount();
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

    public OrePurifierBlockEntity(BlockPos Pos, BlockState State) {
        super(RefineryRegistry.OrePurifierEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<OrePurifierBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static boolean isProcessable(@Nullable Level World, ItemStack Stack) {
        if (World == null || Stack.isEmpty()) {
            return false;
        }
        for (RecipeHolder<PurifyingRecipe> Known : World.getRecipeManager().getAllRecipesFor(RefineryRegistry.Purifying.get())) {
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

    public FluidTank getWaterTank() {
        return Water;
    }

    public FluidTank getWasteTank() {
        return Waste;
    }

    public IFluidHandler getWaterInlet() {
        return WaterInlet;
    }

    public IFluidHandler getWasteOutlet() {
        return WasteOutlet;
    }

    public IFluidHandler getCombined() {
        return Combined;
    }

    public IItemHandler getItemHandler(@Nullable Direction Side) {
        if (Side == null) {
            return Whole;
        }
        return Side == Direction.DOWN ? Drain : Feed;
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof OrePurifierBlock ? State.getValue(OrePurifierBlock.Facing) : Direction.NORTH;
    }

    public Vec3 getCentre() {
        return PurifierLayout.centre(worldPosition, getFront());
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, OrePurifierBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof OrePurifierBlock) {
            Machine.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, OrePurifierBlockEntity Machine) {
        Machine.tickClient();
        ClientEffects.accept(Machine);
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        boolean WasDemanding = Demand > IdleDraw;
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
            Fits = Current != null && placeAll(Current.value().getResults().stream().map(PurifierOutput::Stack).toList(), true);
        }
        if (Delivered > 0) {
            LastPowered = Now;
        }
        boolean Powered = Now - LastPowered <= PowerGrace;
        int Draw = Upgrades.draw(WorkDraw);
        int Used = 0;
        boolean Ready = false;
        if (Current == null) {
            Status = PurifierStatus.Idle;
        } else if (!Fits) {
            Status = PurifierStatus.Blocked;
        } else if (Water.getFluidAmount() < Current.value().getWater()) {
            Status = PurifierStatus.NoWater;
        } else if (Waste.getSpace() < Current.value().getWaste()) {
            Status = PurifierStatus.WasteFull;
        } else {
            Ready = true;
            Used = WasDemanding ? Math.min(Delivered, Draw) : 0;
            if (Used > 0) {
                Status = Used >= Draw ? PurifierStatus.Working : PurifierStatus.LowPower;
                float Gain = Used * Upgrades.speedFactor() / Upgrades.powerFactor() + Carry;
                int Earned = (int) Gain;
                Carry = Gain - Earned;
                Progress += Earned;
                LastWorked = Now;
                setChanged();
                if (Progress >= Current.value().getEnergy()) {
                    finish(World, Current.value());
                }
            } else if (WasDemanding || !Powered) {
                Status = PurifierStatus.Unpowered;
            } else if (!Status.isWorking()) {
                Status = PurifierStatus.Idle;
            }
        }
        Demand = Ready ? Draw : IdleDraw;
        if (!Status.isAlert()) {
            AlertSince = Long.MAX_VALUE;
        } else if (AlertSince == Long.MAX_VALUE) {
            AlertSince = Now;
        }
        PurifierLight Wanted;
        if (Now - LastWorked <= ActiveHold) {
            Wanted = PurifierLight.Working;
        } else if (!Powered) {
            Wanted = PurifierLight.Off;
        } else if (Status.isAlert() && Now - AlertSince >= AlertDelay) {
            Wanted = PurifierLight.Alert;
        } else {
            Wanted = PurifierLight.Idle;
        }
        if (State.getValue(OrePurifierBlock.Light) != Wanted) {
            OrePurifierBlock.applyLight(World, Pos, State, Wanted);
        }
        updateSync(World, Now, Used, Draw);
    }

    private void refreshRecipe(Level World) {
        ItemStack Loaded = Contents.getStackInSlot(InputSlot);
        RecipeHolder<PurifyingRecipe> Found = Loaded.isEmpty() ? null
                : World.getRecipeManager().getRecipeFor(RefineryRegistry.Purifying.get(), new SingleRecipeInput(Loaded), World, Current).orElse(null);
        ResourceLocation FoundId = Found == null ? null : Found.id();
        if (!Objects.equals(FoundId, CurrentId)) {
            Progress = 0;
        }
        Current = Found;
        CurrentId = FoundId;
    }

    private void finish(ServerLevel World, PurifyingRecipe Wash) {
        List<ItemStack> Rolled = new ArrayList<>();
        for (PurifierOutput Output : Wash.getResults()) {
            ItemStack Stack = Output.roll(World.getRandom());
            if (!Stack.isEmpty()) {
                Rolled.add(Stack);
            }
        }
        if (!placeAll(Rolled, false)) {
            Progress = Wash.getEnergy();
            Fits = false;
            return;
        }
        Contents.extractItem(InputSlot, Wash.getInput().count(), false);
        if (Wash.getWater() > 0) {
            Water.drain(Wash.getWater(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (Wash.getWaste() > 0) {
            Waste.fill(new FluidStack(FissionRegistry.WasteWater.get(), Wash.getWaste()), IFluidHandler.FluidAction.EXECUTE);
        }
        Progress = 0;
        Vec3 Centre = getCentre();
        World.playSound(null, Centre.x, Centre.y, Centre.z, RefineryRegistry.PurifierDrain.get(), SoundSource.BLOCKS, 0.55f, 0.9f + World.getRandom().nextFloat() * 0.2f);
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

    private void updateSync(ServerLevel World, long Now, int Used, int Draw) {
        int Wanted = Mth.clamp(Math.round((float) Used * RateSteps / Draw), 0, RateSteps);
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
        float Target = (float) Rate / RateSteps;
        if (!Primed) {
            Primed = true;
            Churn = Target;
        }
        PreviousChurn = Churn;
        if (Churn < Target) {
            Churn = Math.min(Target, Churn + (Target - Churn) * 0.08f + 0.006f);
        } else if (Churn > Target) {
            Churn = Math.max(Target, Churn - (Churn - Target) * 0.05f - 0.004f);
        }
    }

    public float getChurn() {
        return Churn;
    }

    public float getRenderChurn(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousChurn, Churn);
    }

    public PurifierStatus getStatus() {
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

    public int getLastReceived() {
        return LastReceived;
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    public void observeNetwork(long Available) {
        if (Available > 0 && level != null) {
            LastPowered = level.getGameTime();
        }
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Contents.getStackInSlot(Slot).copy());
            Contents.setStackInSlot(Slot, ItemStack.EMPTY);
        }
        Upgrades.drop(World, Pos);
    }

    public int comparatorLevel() {
        int Filled = 0;
        float Fraction = 0.0f;
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            ItemStack Stack = Contents.getStackInSlot(Slot);
            if (!Stack.isEmpty()) {
                Fraction += Stack.getCount() / (float) Math.min(Contents.getSlotLimit(Slot), Stack.getMaxStackSize());
                Filled++;
            }
        }
        return Filled == 0 ? 0 : Mth.floor(Fraction / SlotCount * 14.0f) + 1;
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
        Tag.put("Water", Water.writeToNBT(Registries, new CompoundTag()));
        Tag.put("Waste", Waste.writeToNBT(Registries, new CompoundTag()));
        Upgrades.save(Tag);
        Tag.putInt("Progress", Progress);
        if (CurrentId != null) {
            Tag.putString("Recipe", CurrentId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            Rate = Mth.clamp(Tag.getByte("Rate"), 0, RateSteps);
            return;
        }
        if (Tag.contains("Contents")) {
            Contents.deserializeNBT(Registries, Tag.getCompound("Contents"));
        }
        if (Tag.contains("Water")) {
            Water.readFromNBT(Registries, Tag.getCompound("Water"));
        }
        if (Tag.contains("Waste")) {
            Waste.readFromNBT(Registries, Tag.getCompound("Waste"));
        }
        Upgrades.load(Tag);
        if (Tag.contains("Progress")) {
            Progress = Math.max(0, Tag.getInt("Progress"));
        }
        if (Tag.contains("Recipe")) {
            CurrentId = ResourceLocation.tryParse(Tag.getString("Recipe"));
        }
        InputDirty = true;
        OutputDirty = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        Tag.putByte("Rate", (byte) Rate);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.ore_purifier");
    }

    public void writeMenuState(RegistryFriendlyByteBuf Buffer) {
        OrePurifierMenu.writeState(Buffer, Data);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new OrePurifierMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        Vec3 Centre = getCentre();
        return User.distanceToSqr(Centre.x, Centre.y, Centre.z) <= ReachSquared;
    }
}
