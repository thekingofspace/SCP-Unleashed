package net.rai.scp_unleashed.advmaterials;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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

public class AdvancedMaterialsBenchBlockEntity extends BlockEntity implements WireDevice, MenuProvider, Upgradeable {
    public static final int InputCount = 4;
    public static final int OutputSlot = 4;
    public static final int SlotCount = 5;
    public static final int WorkDraw = 80;
    public static final int IdleDraw = 0;
    public static final int ActiveHold = 10;
    public static final int AlertDelay = 20;
    public static final int PowerGrace = 10;
    public static final int SyncTicks = 5;
    public static final int RateSteps = 16;
    public static final int ProgressScale = 1000;
    public static final int WorkSoundEvery = 36;
    public static final double ReachSquared = 100.0;
    public static final int DataProgress = 0;
    public static final int DataReceived = 1;
    public static final int DataDemand = 2;
    public static final int DataStatus = 3;
    public static final int DataDraw = 4;
    public static final int DataTime = 5;
    public static final int DataCount = 6;

    private static Consumer<AdvancedMaterialsBenchBlockEntity> ClientEffects = Machine -> {
    };

    private final ItemStackHandler Contents = new ItemStackHandler(SlotCount) {
        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
            if (Slot < InputCount) {
                InputDirty = true;
            }
            OutputDirty = true;
        }
    };
    private final BenchItemHandler Feed = new BenchItemHandler(this, BenchItemHandler.Access.Feed);
    private final BenchItemHandler Drain = new BenchItemHandler(this, BenchItemHandler.Access.Drain);
    private final BenchItemHandler Whole = new BenchItemHandler(this, BenchItemHandler.Access.Both);
    private final MachineUpgrades Upgrades = new MachineUpgrades(this::setChanged, UpgradeType.Speed);

    @Nullable
    private RecipeManager KnownRecipes;
    @Nullable
    private RecipeHolder<AdvancedMaterialsRecipe> Current;
    @Nullable
    private ResourceLocation CurrentId;
    private boolean InputDirty = true;
    private boolean OutputDirty = true;
    private boolean Fits;
    private int Received;
    private int LastReceived;
    private int Demand = IdleDraw;
    private long Progress;
    private float Carry;
    private BenchStatus Status = BenchStatus.Idle;
    private long LastWorked = Long.MIN_VALUE / 2;
    private long LastPowered = Long.MIN_VALUE / 2;
    private long AlertSince = Long.MAX_VALUE;
    private long LastWorkSound = Long.MIN_VALUE / 2;
    private int Rate;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private boolean Primed;
    private float Charge;
    private float PreviousCharge;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataProgress -> Math.round(getProgressFraction() * ProgressScale);
                case DataReceived -> LastReceived;
                case DataDemand -> Demand;
                case DataStatus -> Status.ordinal();
                case DataDraw -> Current == null ? 0 : Upgrades.draw(Current.value().getDraw());
                case DataTime -> Current == null ? 0 : Math.round(Current.value().getTime() / Upgrades.speedFactor());
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

    public AdvancedMaterialsBenchBlockEntity(BlockPos Pos, BlockState State) {
        super(AdvancedMaterialsRegistry.BenchEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<AdvancedMaterialsBenchBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public ItemStackHandler getContents() {
        return Contents;
    }

    public List<ItemStack> inputs() {
        List<ItemStack> Items = new ArrayList<>(InputCount);
        for (int Slot = 0; Slot < InputCount; Slot++) {
            Items.add(Contents.getStackInSlot(Slot));
        }
        return Items;
    }

    public boolean accepts(ItemStack Stack) {
        return AdvancedMaterialsRecipes.isIngredient(level, Stack) && AdvancedMaterialsRecipes.canJoin(level, inputs(), Stack);
    }

    public IItemHandler getItemHandler(@Nullable Direction Side) {
        if (Side == null) {
            return Whole;
        }
        return Side == Direction.DOWN ? Drain : Feed;
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof AdvancedMaterialsBenchBlock ? State.getValue(AdvancedMaterialsBenchBlock.Facing) : Direction.NORTH;
    }

    public Vec3 getCentre() {
        return BenchLayout.centre(worldPosition, getFront());
    }

    public Vec3 getHeadPoint() {
        return BenchLayout.headPoint(worldPosition, getFront());
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, AdvancedMaterialsBenchBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof AdvancedMaterialsBenchBlock) {
            Machine.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, AdvancedMaterialsBenchBlockEntity Machine) {
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
            Fits = Current != null && fits(Current.value().getResult());
        }
        if (Delivered > 0) {
            LastPowered = Now;
        }
        boolean Powered = Now - LastPowered <= PowerGrace;
        int Draw = Current == null ? WorkDraw : Upgrades.draw(Current.value().getDraw());
        int Used = 0;
        boolean Ready = false;
        if (Current == null) {
            Status = BenchStatus.Idle;
        } else if (!Fits) {
            Status = BenchStatus.Blocked;
        } else {
            Ready = true;
            Used = WasDemanding ? Math.min(Delivered, Draw) : 0;
            if (Used > 0) {
                Status = Used >= Draw ? BenchStatus.Working : BenchStatus.LowPower;
                float Gain = Used * Upgrades.speedFactor() / Upgrades.powerFactor() + Carry;
                int Earned = (int) Gain;
                Carry = Gain - Earned;
                Progress += Earned;
                LastWorked = Now;
                setChanged();
                if (Now - LastWorkSound >= WorkSoundEvery) {
                    LastWorkSound = Now;
                    Vec3 Head = getHeadPoint();
                    World.playSound(null, Head.x, Head.y, Head.z, AdvancedMaterialsRegistry.BenchWork.get(), SoundSource.BLOCKS, 0.5f, 0.92f + World.getRandom().nextFloat() * 0.12f);
                }
                if (Progress >= Current.value().getEnergy()) {
                    finish(World, Current.value());
                }
            } else if (WasDemanding || !Powered) {
                Status = BenchStatus.Unpowered;
            } else if (!Status.isWorking()) {
                Status = BenchStatus.Idle;
            }
        }
        Demand = Ready ? Draw : IdleDraw;
        if (!Status.isAlert()) {
            AlertSince = Long.MAX_VALUE;
        } else if (AlertSince == Long.MAX_VALUE) {
            AlertSince = Now;
        }
        BenchLight Wanted;
        if (Now - LastWorked <= ActiveHold) {
            Wanted = BenchLight.Working;
        } else if (!Powered) {
            Wanted = BenchLight.Off;
        } else if (Status.isAlert() && Now - AlertSince >= AlertDelay) {
            Wanted = BenchLight.Alert;
        } else {
            Wanted = BenchLight.Idle;
        }
        if (State.getValue(AdvancedMaterialsBenchBlock.Light) != Wanted) {
            AdvancedMaterialsBenchBlock.applyLight(World, Pos, State, Wanted);
        }
        updateSync(World, Now, Used, Draw);
    }

    private void refreshRecipe(Level World) {
        RecipeHolder<AdvancedMaterialsRecipe> Found = AdvancedMaterialsRecipes.find(World, inputs());
        ResourceLocation FoundId = Found == null ? null : Found.id();
        if (!Objects.equals(FoundId, CurrentId)) {
            Progress = 0;
            Carry = 0.0f;
        }
        Current = Found;
        CurrentId = FoundId;
    }

    private boolean fits(ItemStack Result) {
        ItemStack Held = Contents.getStackInSlot(OutputSlot);
        if (Held.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(Held, Result)) {
            return false;
        }
        return Held.getCount() + Result.getCount() <= Math.min(Contents.getSlotLimit(OutputSlot), Held.getMaxStackSize());
    }

    private void finish(ServerLevel World, AdvancedMaterialsRecipe Formula) {
        int[] Taken = Formula.plan(inputs());
        ItemStack Result = Formula.getResult().copy();
        if (Taken == null || !fits(Result)) {
            Progress = Formula.getEnergy();
            Fits = false;
            InputDirty = true;
            return;
        }
        for (int Slot = 0; Slot < InputCount; Slot++) {
            if (Taken[Slot] > 0) {
                Contents.extractItem(Slot, Taken[Slot], false);
            }
        }
        ItemStack Held = Contents.getStackInSlot(OutputSlot);
        if (Held.isEmpty()) {
            Contents.setStackInSlot(OutputSlot, Result);
        } else {
            Contents.setStackInSlot(OutputSlot, Held.copyWithCount(Held.getCount() + Result.getCount()));
        }
        Progress = 0;
        Carry = 0.0f;
        Vec3 Head = getHeadPoint();
        World.playSound(null, Head.x, Head.y, Head.z, AdvancedMaterialsRegistry.BenchFinish.get(), SoundSource.BLOCKS, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        World.playSound(null, Head.x, Head.y, Head.z, AdvancedMaterialsRegistry.BenchCharge.get(), SoundSource.BLOCKS, 0.45f, 1.1f);
    }

    private void updateSync(ServerLevel World, long Now, int Used, int Draw) {
        int Wanted = Mth.clamp(Math.round((float) Used * RateSteps / Math.max(1, Draw)), 0, RateSteps);
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
            Charge = Target;
        }
        PreviousCharge = Charge;
        if (Charge < Target) {
            Charge = Math.min(Target, Charge + (Target - Charge) * 0.1f + 0.008f);
        } else if (Charge > Target) {
            Charge = Math.max(Target, Charge - (Charge - Target) * 0.06f - 0.005f);
        }
    }

    public float getCharge() {
        return Charge;
    }

    public float getRenderCharge(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousCharge, Charge);
    }

    public BenchStatus getStatus() {
        return Status;
    }

    public ItemStack getCurrentResult() {
        return Current == null ? ItemStack.EMPTY : Current.value().getResult();
    }

    public float getProgressFraction() {
        if (Current == null) {
            return 0.0f;
        }
        return Mth.clamp((float) Progress / Math.max(1L, Current.value().getEnergy()), 0.0f, 1.0f);
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
        Upgrades.save(Tag);
        Tag.putLong("Progress", Progress);
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
        Upgrades.load(Tag);
        if (Tag.contains("Progress")) {
            Progress = Math.max(0L, Tag.getLong("Progress"));
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
        return Component.translatable("container.scp_unleashed.advanced_materials_bench");
    }

    public void writeMenuState(RegistryFriendlyByteBuf Buffer) {
        AdvancedMaterialsMenu.writeState(Buffer, Data);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new AdvancedMaterialsMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        Vec3 Centre = getCentre();
        return User.distanceToSqr(Centre.x, Centre.y, Centre.z) <= ReachSquared;
    }
}
