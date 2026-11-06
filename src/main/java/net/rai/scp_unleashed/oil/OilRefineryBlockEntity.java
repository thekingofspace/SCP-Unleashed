package net.rai.scp_unleashed.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.pressure.PortRole;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class OilRefineryBlockEntity extends OilMachineBlockEntity {
    public static final int InputTank = 0;
    public static final int WaterTank = 1;
    public static final int OutputSlot = 0;

    private final FluidTank Input = addTank(this::canRefine, PortRole.Input);
    private final FluidTank Water = addTank(Stack -> true, PortRole.Output);
    private final ItemStackHandler Output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
        }
    };
    private final IItemHandler Automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int Slot) {
            return Output.getStackInSlot(Slot);
        }

        @Override
        public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
            return Stack;
        }

        @Override
        public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
            return Output.extractItem(Slot, Amount, Simulate);
        }

        @Override
        public int getSlotLimit(int Slot) {
            return Output.getSlotLimit(Slot);
        }

        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return false;
        }
    };
    @Nullable
    private RecipeHolder<OilRefiningRecipe> Current;
    @Nullable
    private ResourceLocation CurrentId;
    private boolean RecipeDirty = true;
    private float Progress;

    public OilRefineryBlockEntity(BlockPos Pos, BlockState State) {
        super(OilRegistry.OilRefineryEntity.get(), Pos, State, OilMachineKind.Refinery);
    }

    @Nullable
    public static RecipeHolder<OilRefiningRecipe> findRecipe(RecipeManager Recipes, FluidStack Stack) {
        if (Stack.isEmpty()) {
            return null;
        }
        for (RecipeHolder<OilRefiningRecipe> Holder : Recipes.getAllRecipesFor(OilRegistry.OilRefining.get())) {
            if (Holder.value().accepts(Stack)) {
                return Holder;
            }
        }
        return null;
    }

    private boolean canRefine(FluidStack Stack) {
        return level != null && findRecipe(level.getRecipeManager(), Stack) != null;
    }

    @Override
    protected void tankChanged() {
        RecipeDirty = true;
    }

    @Override
    protected void recipesReloaded() {
        Current = null;
        RecipeDirty = true;
    }

    public ItemStackHandler getOutput() {
        return Output;
    }

    @Override
    public IItemHandler getItemHandler(@Nullable Direction Side) {
        return Side == null ? Output : Automation;
    }

    @Override
    public float getProgress() {
        OilRefiningRecipe Recipe = Current == null ? null : Current.value();
        return Recipe == null ? 0.0f : Progress / Recipe.getTime();
    }

    private void refreshRecipe(ServerLevel World) {
        if (!RecipeDirty) {
            return;
        }
        RecipeDirty = false;
        if (Current != null && Current.value().accepts(Input.getFluid())) {
            return;
        }
        RecipeHolder<OilRefiningRecipe> Found = findRecipe(World.getRecipeManager(), Input.getFluid());
        ResourceLocation FoundId = Found == null ? null : Found.id();
        if (!Objects.equals(FoundId, CurrentId)) {
            Progress = 0.0f;
        }
        Current = Found;
        CurrentId = FoundId;
    }

    @Override
    protected float work(ServerLevel World, BlockPos Pos, BlockState State, long Now, int Delivered, boolean WasDemanding) {
        refreshRecipe(World);
        OilRefiningRecipe Recipe = Current == null ? null : Current.value();
        if (Recipe == null || Input.isEmpty()) {
            Status = OilStatus.Idle;
            Progress = 0.0f;
            return 0.0f;
        }
        if (Input.getFluidAmount() < Recipe.getInput().amount()) {
            Status = OilStatus.Waiting;
            return 0.0f;
        }
        ItemStack Result = Recipe.getResult();
        boolean ItemFits = Output.insertItem(OutputSlot, Result.copy(), true).isEmpty();
        boolean WaterFits = Recipe.getByproduct().isEmpty() || Water.fill(Recipe.getByproduct(), IFluidHandler.FluidAction.SIMULATE) >= Recipe.getByproduct().getAmount();
        if (!ItemFits || !WaterFits) {
            Status = OilStatus.OutputFull;
            return 0.0f;
        }
        float Share = consume(Now, Recipe.getPower(), Delivered, WasDemanding);
        if (Share <= 0.0f) {
            return 0.0f;
        }
        Progress += Share;
        if (Progress >= Recipe.getTime()) {
            Progress -= Recipe.getTime();
            Input.drain(Recipe.getInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            Output.insertItem(OutputSlot, Result.copy(), false);
            if (!Recipe.getByproduct().isEmpty()) {
                Water.fill(Recipe.getByproduct().copy(), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        return Share;
    }

    @Override
    public void dropContents(Level World, BlockPos Pos) {
        Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Output.getStackInSlot(OutputSlot).copy());
        Output.setStackInSlot(OutputSlot, ItemStack.EMPTY);
    }

    @Override
    protected void saveExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.put("Output", Output.serializeNBT(Registries));
        Tag.putFloat("Progress", Progress);
        if (CurrentId != null) {
            Tag.putString("Recipe", CurrentId.toString());
        }
    }

    @Override
    protected void loadExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (Tag.contains("Output")) {
            Output.deserializeNBT(Registries, Tag.getCompound("Output"));
        }
        Progress = Math.max(0.0f, Tag.getFloat("Progress"));
        CurrentId = Tag.contains("Recipe") ? ResourceLocation.tryParse(Tag.getString("Recipe")) : null;
        Current = null;
        RecipeDirty = true;
    }
}
