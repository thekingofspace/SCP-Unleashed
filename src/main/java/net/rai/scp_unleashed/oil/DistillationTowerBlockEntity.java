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

public class DistillationTowerBlockEntity extends OilMachineBlockEntity {
    public static final int FeedTank = 0;
    public static final int TopTank = 1;
    public static final int BottomTank = 2;

    private final FluidTank Feed = addTank(this::canDistil, PortRole.Input);
    private final FluidTank Top = addTank(Stack -> true, PortRole.Output);
    private final FluidTank Bottom = addTank(Stack -> true, PortRole.Output);
    @Nullable
    private RecipeHolder<DistillingRecipe> Current;
    @Nullable
    private ResourceLocation CurrentId;
    private boolean RecipeDirty = true;
    private float Progress;

    public DistillationTowerBlockEntity(BlockPos Pos, BlockState State) {
        super(OilRegistry.DistillationTowerEntity.get(), Pos, State, OilMachineKind.Tower);
    }

    @Nullable
    public static RecipeHolder<DistillingRecipe> findRecipe(RecipeManager Recipes, FluidStack Stack) {
        if (Stack.isEmpty()) {
            return null;
        }
        for (RecipeHolder<DistillingRecipe> Holder : Recipes.getAllRecipesFor(OilRegistry.Distilling.get())) {
            if (Holder.value().accepts(Stack)) {
                return Holder;
            }
        }
        return null;
    }

    private boolean canDistil(FluidStack Stack) {
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

    @Override
    public float getProgress() {
        DistillingRecipe Recipe = Current == null ? null : Current.value();
        return Recipe == null ? 0.0f : Progress / Recipe.getTime();
    }

    private void refreshRecipe(ServerLevel World) {
        if (!RecipeDirty) {
            return;
        }
        RecipeDirty = false;
        if (Current != null && Current.value().accepts(Feed.getFluid())) {
            return;
        }
        RecipeHolder<DistillingRecipe> Found = findRecipe(World.getRecipeManager(), Feed.getFluid());
        ResourceLocation FoundId = Found == null ? null : Found.id();
        if (!Objects.equals(FoundId, CurrentId)) {
            Progress = 0.0f;
        }
        Current = Found;
        CurrentId = FoundId;
    }

    private static boolean fits(FluidTank Tank, FluidStack Stack) {
        return Stack.isEmpty() || Tank.fill(Stack, IFluidHandler.FluidAction.SIMULATE) >= Stack.getAmount();
    }

    @Override
    protected float work(ServerLevel World, BlockPos Pos, BlockState State, long Now, int Delivered, boolean WasDemanding) {
        refreshRecipe(World);
        DistillingRecipe Recipe = Current == null ? null : Current.value();
        if (Recipe == null || Feed.isEmpty()) {
            Status = OilStatus.Idle;
            Progress = 0.0f;
            return 0.0f;
        }
        if (Feed.getFluidAmount() < Recipe.getInput().amount()) {
            Status = OilStatus.Waiting;
            return 0.0f;
        }
        if (!fits(Top, Recipe.getTop()) || !fits(Bottom, Recipe.getBottom())) {
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
            Feed.drain(Recipe.getInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            Top.fill(Recipe.getTop().copy(), IFluidHandler.FluidAction.EXECUTE);
            Bottom.fill(Recipe.getBottom().copy(), IFluidHandler.FluidAction.EXECUTE);
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
