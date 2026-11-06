package net.rai.scp_unleashed.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.pressure.PortRole;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ChemicalProcessorBlockEntity extends OilMachineBlockEntity {
    public static final int InputTank = 0;
    public static final int OutputTank = 1;

    private final FluidTank Input = addTank(this::canProcess, PortRole.Input);
    private final FluidTank Output = addTank(Stack -> true, PortRole.Output);
    @Nullable
    private RecipeHolder<ChemicalProcessingRecipe> Current;
    @Nullable
    private ResourceLocation CurrentId;
    private boolean RecipeDirty = true;
    private float Progress;

    public ChemicalProcessorBlockEntity(BlockPos Pos, BlockState State) {
        super(OilRegistry.ChemicalProcessorEntity.get(), Pos, State, OilMachineKind.Processor);
    }

    @Nullable
    public static RecipeHolder<ChemicalProcessingRecipe> findRecipe(RecipeManager Recipes, FluidStack Stack) {
        if (Stack.isEmpty()) {
            return null;
        }
        for (RecipeHolder<ChemicalProcessingRecipe> Holder : Recipes.getAllRecipesFor(OilRegistry.ChemicalProcessing.get())) {
            if (Holder.value().accepts(Stack)) {
                return Holder;
            }
        }
        return null;
    }

    private boolean canProcess(FluidStack Stack) {
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

    @Nullable
    public ChemicalProcessingRecipe getCurrentRecipe() {
        return Current == null ? null : Current.value();
    }

    @Override
    public float getProgress() {
        ChemicalProcessingRecipe Recipe = getCurrentRecipe();
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
        RecipeHolder<ChemicalProcessingRecipe> Found = findRecipe(World.getRecipeManager(), Input.getFluid());
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
        ChemicalProcessingRecipe Recipe = getCurrentRecipe();
        if (Recipe == null || Input.isEmpty()) {
            Status = OilStatus.Idle;
            Progress = 0.0f;
            return 0.0f;
        }
        if (Input.getFluidAmount() < Recipe.getInput().amount()) {
            Status = OilStatus.Waiting;
            return 0.0f;
        }
        if (Output.fill(Recipe.getResult(), IFluidHandler.FluidAction.SIMULATE) < Recipe.getResult().getAmount()) {
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
            Output.fill(Recipe.getResult().copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        return Share;
    }

    @Override
    protected void saveExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putFloat("Progress", Progress);
        if (CurrentId != null) {
            Tag.putString("Recipe", CurrentId.toString());
        }
    }

    @Override
    protected void loadExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        Progress = Math.max(0.0f, Tag.getFloat("Progress"));
        CurrentId = Tag.contains("Recipe") ? ResourceLocation.tryParse(Tag.getString("Recipe")) : null;
        Current = null;
        RecipeDirty = true;
    }
}
