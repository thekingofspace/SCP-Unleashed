package net.rai.scp_unleashed.assembler;

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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.upgrade.MachineUpgrades;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class AdvancedAssemblerBlockEntity extends BlockEntity implements WireDevice, MenuProvider, Upgradeable {
    public static final int InputCount = 9;
    public static final int OutputSlot = 9;
    public static final int SlotCount = 10;
    public static final int WorkingDraw = 60;
    public static final int PowerGrace = 10;
    public static final int WorkHold = 8;
    public static final int SyncTicks = 5;
    public static final int ProgressScale = 1000;
    public static final int FinishEvent = 1;
    public static final float CycleTicks = 72.0f;
    public static final int ProgressData = 0;
    public static final int TimeData = 1;
    public static final int ReceivedData = 2;
    public static final int DemandData = 3;
    public static final int DrawData = 4;
    public static final int StatusData = 5;
    public static final int DataCount = 6;

    private static Consumer<AdvancedAssemblerBlockEntity> ClientEffects = Assembler -> {
    };

    private final AssemblerInventory Contents = new AssemblerInventory(this::getLevel, this::contentsChanged);
    private final AssemblerAccess InputAccess = new AssemblerAccess(Contents, true, false);
    private final AssemblerAccess OutputAccess = new AssemblerAccess(Contents, false, true);
    private final AssemblerAccess FullAccess = new AssemblerAccess(Contents, true, true);
    private final MachineUpgrades Upgrades = new MachineUpgrades(this::setChanged, UpgradeType.Speed);
    @Nullable
    private RecipeHolder<AssemblingRecipe> Current;
    @Nullable
    private ResourceLocation SavedRecipe;
    @Nullable
    private RecipeManager KnownRecipes;
    private float Progress;
    private int Demand;
    private int Received;
    private int LastReceived;
    private long LastPowered = Long.MIN_VALUE / 2;
    private long LastWorked = Long.MIN_VALUE / 2;
    private boolean Searching = true;
    private boolean Blocked;
    private AssemblerStatus Status = AssemblerStatus.Off;
    private ItemStack Display = ItemStack.EMPTY;
    private float Speed;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private float Clock;
    private float PreviousClock;
    private float Engage;
    private float PreviousEngage;
    private float ShownSpeed;
    private int Cycles;
    private int PreviousCycles;
    private boolean FinishPending;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case ProgressData -> progressScaled();
                case TimeData -> Current == null ? 0 : Current.value().getTime();
                case ReceivedData -> LastReceived;
                case DemandData -> Demand;
                case DrawData -> Upgrades.draw(Current == null ? WorkingDraw : Current.value().getDraw());
                case StatusData -> Status.ordinal();
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

    public AdvancedAssemblerBlockEntity(BlockPos Pos, BlockState State) {
        super(AssemblerRegistry.AdvancedAssemblerEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<AdvancedAssemblerBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, AdvancedAssemblerBlockEntity Assembler) {
        if (World instanceof ServerLevel ServerWorld) {
            Assembler.tickServer(ServerWorld, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, AdvancedAssemblerBlockEntity Assembler) {
        Assembler.tickClient();
        ClientEffects.accept(Assembler);
    }

    private void contentsChanged() {
        Searching = true;
        setChanged();
    }

    private void tickServer(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        int Got = Received;
        Received = 0;
        LastReceived = Got;
        if (Got > 0) {
            LastPowered = Now;
        }
        if (World.getRecipeManager() != KnownRecipes) {
            KnownRecipes = World.getRecipeManager();
            Searching = true;
        }
        if (Searching) {
            search(World);
        }
        float Fraction = 0.0f;
        Blocked = false;
        if (Current == null) {
            Demand = 0;
            Progress = 0.0f;
        } else {
            AssemblingRecipe Formula = Current.value();
            if (!Contents.outputFits(Formula.getResult())) {
                Blocked = true;
                Demand = 0;
            } else {
                int Draw = Upgrades.draw(Formula.getDraw());
                Fraction = Math.min(1.0f, (float) Got / Draw);
                Demand = Draw;
                if (Fraction > 0.0f) {
                    Progress += Fraction * Upgrades.speedFactor();
                    LastWorked = Now;
                    setChanged();
                }
                if (Progress >= Formula.getTime()) {
                    finish(World, Formula);
                }
            }
        }
        updateStatus(World, State, Now);
        updateSpeed(Fraction * Upgrades.speedFactor());
        updateDisplay();
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void search(Level World) {
        Searching = false;
        List<ItemStack> Items = Contents.inputs();
        ResourceLocation Previous = Current != null ? Current.id() : SavedRecipe;
        SavedRecipe = null;
        RecipeHolder<AssemblingRecipe> Kept = Previous == null ? null : AssemblingRecipes.byId(World, Previous);
        if (Kept != null && Kept.value().plan(Items) != null) {
            Current = Kept;
            return;
        }
        RecipeHolder<AssemblingRecipe> Found = AssemblingRecipes.find(World, Items);
        if (Found == null || !Found.id().equals(Previous)) {
            Progress = 0.0f;
        }
        Current = Found;
    }

    private void finish(ServerLevel World, AssemblingRecipe Formula) {
        int[] Plan = Formula.plan(Contents.inputs());
        if (Plan == null) {
            Searching = true;
            return;
        }
        for (int Slot = 0; Slot < InputCount; Slot++) {
            if (Plan[Slot] > 0) {
                Contents.extractItem(Slot, Plan[Slot], false);
            }
        }
        Contents.addOutput(Formula.getResult());
        Progress = 0.0f;
        World.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.3, worldPosition.getZ() + 0.5, AssemblerRegistry.Finish.get(), SoundSource.BLOCKS,
                0.55f, 0.94f + World.getRandom().nextFloat() * 0.12f);
        World.blockEvent(worldPosition, getBlockState().getBlock(), FinishEvent, 0);
        setChanged();
    }

    private void updateStatus(ServerLevel World, BlockState State, long Now) {
        AssemblerStatus Next;
        if (Now - LastPowered > PowerGrace) {
            Next = AssemblerStatus.Off;
        } else if (Blocked) {
            Next = AssemblerStatus.Blocked;
        } else if (Current != null && Now - LastWorked <= WorkHold) {
            Next = AssemblerStatus.Working;
        } else {
            Next = AssemblerStatus.Idle;
        }
        if (Next != Status) {
            Status = Next;
            NeedsSync = true;
            UrgentSync = true;
        }
        if (State.getBlock() instanceof AdvancedAssemblerBlock && State.getValue(AdvancedAssemblerBlock.Status) != Status) {
            AdvancedAssemblerBlock.setStatus(World, worldPosition, State, Status);
        }
    }

    private void updateSpeed(float Fraction) {
        float Wanted = Status == AssemblerStatus.Working ? Fraction : 0.0f;
        if (Math.abs(Wanted - Speed) > 0.04f || (Wanted <= 0.0f) != (Speed <= 0.0f)) {
            Speed = Wanted;
            NeedsSync = true;
        }
    }

    private void updateDisplay() {
        ItemStack Wanted = Current == null ? ItemStack.EMPTY : Current.value().getResult();
        if (!ItemStack.isSameItemSameComponents(Wanted, Display)) {
            Display = Wanted.isEmpty() ? ItemStack.EMPTY : Wanted.copyWithCount(1);
            NeedsSync = true;
            UrgentSync = true;
        }
    }

    private int progressScaled() {
        if (Current == null) {
            return 0;
        }
        return Math.min(ProgressScale, Math.round(Progress * ProgressScale / Current.value().getTime()));
    }

    private void tickClient() {
        PreviousClock = Clock;
        PreviousEngage = Engage;
        PreviousCycles = Cycles;
        boolean Running = Status == AssemblerStatus.Working;
        float Target = Running ? 1.0f : 0.0f;
        Engage += (Target - Engage) * (Running ? 0.1f : 0.07f);
        if (Math.abs(Target - Engage) < 0.002f) {
            Engage = Target;
        }
        ShownSpeed += ((Running ? Speed : 0.0f) - ShownSpeed) * 0.2f;
        if (Running) {
            Clock += Math.max(0.08f, ShownSpeed);
            if (Clock >= CycleTicks) {
                Clock -= CycleTicks;
                Cycles = (Cycles + 1) & 1;
            }
        }
    }

    public float getClock(float PartialTick) {
        float Delta = Clock - PreviousClock;
        if (Delta < 0.0f) {
            Delta += CycleTicks;
        }
        float Value = PreviousClock + Delta * PartialTick;
        return Value >= CycleTicks ? Value - CycleTicks : Value;
    }

    public float getPreviousClock() {
        return PreviousClock;
    }

    public float getCurrentClock() {
        return Clock;
    }

    public float getEngage(float PartialTick) {
        return PreviousEngage + (Engage - PreviousEngage) * PartialTick;
    }

    public int getCycles(float PartialTick) {
        return Clock < PreviousClock && PartialTick < 1.0f && PreviousClock + (Clock + CycleTicks - PreviousClock) * PartialTick < CycleTicks ? PreviousCycles : Cycles;
    }

    public float getShownSpeed() {
        return ShownSpeed;
    }

    public boolean takeFinish() {
        boolean Pending = FinishPending;
        FinishPending = false;
        return Pending;
    }

    public AssemblerStatus getStatus() {
        return Status;
    }

    public ItemStack getDisplay() {
        return Display;
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.hasProperty(AdvancedAssemblerBlock.Facing) ? State.getValue(AdvancedAssemblerBlock.Facing) : Direction.NORTH;
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).expandTowards(0.0, 1.0, 0.0);
    }

    public AssemblerInventory getContents() {
        return Contents;
    }

    public IItemHandler accessFor(@Nullable Direction Side) {
        if (Side == null) {
            return FullAccess;
        }
        return Side == Direction.DOWN ? OutputAccess : InputAccess;
    }

    public float getProgressFraction() {
        return Current == null ? 0.0f : Math.min(1.0f, Progress / Current.value().getTime());
    }

    public ItemStack getCurrentResult() {
        return Current == null ? ItemStack.EMPTY : Current.value().getResult();
    }

    public int getLastReceived() {
        return LastReceived;
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

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5) <= 64.0;
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

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id == FinishEvent) {
            if (level != null && level.isClientSide) {
                FinishPending = true;
            }
            return true;
        }
        return super.triggerEvent(Id, Param);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Inventory", Contents.serializeNBT(Registries));
        Upgrades.save(Tag);
        Tag.putFloat("Progress", Progress);
        ResourceLocation RecipeId = Current != null ? Current.id() : SavedRecipe;
        if (RecipeId != null) {
            Tag.putString("Recipe", RecipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Upgrades.load(Tag);
        if (Tag.contains("Inventory")) {
            Contents.deserializeNBT(Registries, Tag.getCompound("Inventory"));
            Progress = Tag.getFloat("Progress");
            SavedRecipe = Tag.contains("Recipe") ? ResourceLocation.tryParse(Tag.getString("Recipe")) : null;
            Current = null;
            Searching = true;
        }
        if (Tag.contains("Status")) {
            Status = AssemblerStatus.byIndex(Tag.getByte("Status"));
            Speed = Tag.getFloat("Speed");
            Display = Tag.contains("Display") ? ItemStack.parseOptional(Registries, Tag.getCompound("Display")) : ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Status", (byte) Status.ordinal());
        Tag.putFloat("Speed", Speed);
        if (!Display.isEmpty()) {
            Tag.put("Display", Display.save(Registries));
        }
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.advanced_assembler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new AssemblerMenu(ContainerId, PlayerInventory, this, Data);
    }
}
