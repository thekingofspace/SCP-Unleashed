package net.rai.scp_unleashed.reactor;

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
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.IrradiatingRecipe;
import net.rai.scp_unleashed.reactor.core.TowerType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class IrradiationChamberBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SlotCount = 2;
    public static final int InputSlot = 0;
    public static final int OutputSlot = 1;
    public static final int FluxTicks = 20;
    public static final int ActiveHold = 30;
    public static final float MaxFlux = 4.0f;
    public static final int DataProgress = 0;
    public static final int DataFlux = 1;
    public static final int DataTime = 2;
    public static final int DataSources = 3;
    public static final int DataCount = 4;

    private final ItemStackHandler Contents = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Slot == InputSlot && isIrradiatable(level, Stack);
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
            if (Slot == InputSlot) {
                RecipeDirty = true;
            }
        }
    };
    private final IItemHandler Automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SlotCount;
        }

        @Override
        public ItemStack getStackInSlot(int Slot) {
            return Contents.getStackInSlot(Slot);
        }

        @Override
        public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
            return Slot == InputSlot ? Contents.insertItem(Slot, Stack, Simulate) : Stack;
        }

        @Override
        public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
            return Slot == OutputSlot ? Contents.extractItem(Slot, Amount, Simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int Slot) {
            return Contents.getSlotLimit(Slot);
        }

        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Slot == InputSlot && Contents.isItemValid(Slot, Stack);
        }
    };

    private float Progress;
    private float Flux;
    private int Sources;
    private long LastWorked = Long.MIN_VALUE / 2;
    @Nullable
    private RecipeManager KnownRecipes;
    @Nullable
    private RecipeHolder<IrradiatingRecipe> Current;
    @Nullable
    private ResourceLocation CurrentId;
    private boolean RecipeDirty = true;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataProgress -> progressPermille();
                case DataFlux -> Math.round(Flux * 100.0f);
                case DataTime -> Current == null ? 0 : Current.value().getTime();
                case DataSources -> Sources;
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

    public IrradiationChamberBlockEntity(BlockPos Pos, BlockState State) {
        super(ReactorRegistry.ChamberEntity.get(), Pos, State);
    }

    public static boolean isIrradiatable(@Nullable Level World, ItemStack Stack) {
        if (World == null || Stack.isEmpty()) {
            return false;
        }
        return World.getRecipeManager().getRecipeFor(FissionRegistry.Irradiating.get(), new SingleRecipeInput(Stack), World).isPresent();
    }

    public ItemStackHandler getContents() {
        return Contents;
    }

    public IItemHandler getAutomation() {
        return Automation;
    }

    public float getFlux() {
        return Flux;
    }

    public int getSources() {
        return Sources;
    }

    public int progressPermille() {
        if (Current == null) {
            return 0;
        }
        return Math.round(Mth.clamp(Progress / Current.value().getTime(), 0.0f, 1.0f) * 1000.0f);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, IrradiationChamberBlockEntity Chamber) {
        if (World instanceof ServerLevel ServerWorld) {
            Chamber.tickServer(ServerWorld, Pos, State);
        }
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        if (Math.floorMod(Now + Pos.hashCode(), FluxTicks) == 0) {
            measure(World, Pos);
        }
        refreshRecipe(World);
        if (Current != null && Flux > 0.0f) {
            ItemStack Result = Current.value().getResult();
            if (fits(Result)) {
                Progress += Flux;
                LastWorked = Now;
                if (Progress >= Current.value().getTime()) {
                    Progress = 0.0f;
                    ItemStack Held = Contents.getStackInSlot(OutputSlot);
                    Contents.setStackInSlot(OutputSlot, Held.isEmpty() ? Result.copy() : Held.copyWithCount(Held.getCount() + Result.getCount()));
                    Contents.extractItem(InputSlot, 1, false);
                }
                setChanged();
            }
        }
        boolean Active = Now - LastWorked <= ActiveHold;
        if (State.getValue(IrradiationChamberBlock.Active) != Active) {
            World.setBlock(Pos, State.setValue(IrradiationChamberBlock.Active, Active), Block.UPDATE_CLIENTS);
        }
    }

    private boolean fits(ItemStack Result) {
        ItemStack Held = Contents.getStackInSlot(OutputSlot);
        if (Held.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(Held, Result) && Held.getCount() + Result.getCount() <= Math.min(Held.getMaxStackSize(), Contents.getSlotLimit(OutputSlot));
    }

    private void measure(ServerLevel World, BlockPos Pos) {
        ReactorGridManager Manager = ReactorGridManager.get(World);
        float Total = 0.0f;
        int Count = 0;
        for (Direction Side : Direction.values()) {
            BlockPos Next = Pos.relative(Side);
            if (!World.isLoaded(Next) || !(World.getBlockState(Next).getBlock() instanceof FuelTowerBlock)) {
                continue;
            }
            ReactorGrid Grid = Manager.gridAt(Next);
            GridTower Tower = Grid == null ? null : Grid.towerAt(Next);
            if (Tower == null || Tower.getType() != TowerType.Fuel) {
                continue;
            }
            int Tier = Tower.levelOf(Next);
            if (Tier >= 0 && Tier < Tower.Core.LevelHeat.length && Tower.Core.LevelHeat[Tier] > 0.0f) {
                Total += Tower.Core.LevelWithdrawal[Tier];
                Count++;
            }
        }
        Flux = Math.min(MaxFlux, Total);
        Sources = Count;
    }

    private void refreshRecipe(ServerLevel World) {
        RecipeManager Recipes = World.getRecipeManager();
        if (Recipes != KnownRecipes) {
            KnownRecipes = Recipes;
            Current = null;
            RecipeDirty = true;
        }
        if (!RecipeDirty) {
            return;
        }
        RecipeDirty = false;
        ItemStack Loaded = Contents.getStackInSlot(InputSlot);
        RecipeHolder<IrradiatingRecipe> Found = Loaded.isEmpty() ? null
                : Recipes.getRecipeFor(FissionRegistry.Irradiating.get(), new SingleRecipeInput(Loaded), World).orElse(null);
        ResourceLocation FoundId = Found == null ? null : Found.id();
        if (!Objects.equals(FoundId, CurrentId)) {
            Progress = 0.0f;
        }
        Current = Found;
        CurrentId = FoundId;
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Contents.getStackInSlot(Slot).copy());
            Contents.setStackInSlot(Slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Contents", Contents.serializeNBT(Registries));
        Tag.putFloat("Progress", Progress);
        if (CurrentId != null) {
            Tag.putString("Recipe", CurrentId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            return;
        }
        if (Tag.contains("Contents")) {
            Contents.deserializeNBT(Registries, Tag.getCompound("Contents"));
        }
        Progress = Math.max(0.0f, Tag.getFloat("Progress"));
        CurrentId = Tag.contains("Recipe") ? ResourceLocation.tryParse(Tag.getString("Recipe")) : null;
        Current = null;
        KnownRecipes = null;
        RecipeDirty = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.irradiation_chamber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new IrradiationChamberMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
