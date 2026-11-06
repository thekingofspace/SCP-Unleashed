package net.rai.scp_unleashed.processing;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ProcessingRecipes {
    public static final int MaxCount = 64;
    public static final float MaxExperience = 1000.0f;
    public static final float MinPower = 0.05f;
    public static final float MaxPower = 100.0f;
    public static final Codec<Float> PowerCodec = Codec.floatRange(MinPower, MaxPower);

    private ProcessingRecipes() {
    }

    public static int drawFor(int BaseDraw, float PowerMultiplier) {
        return Math.max(1, Math.round(BaseDraw * PowerMultiplier));
    }

    public static boolean canSmelt(Level World, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        RecipeManager Recipes = World.getRecipeManager();
        for (RecipeHolder<ElectricSmeltingRecipe> Holder : Recipes.getAllRecipesFor(ProcessingRegistry.ElectricSmeltingType.get())) {
            if (Holder.value().accepts(Stack)) {
                return true;
            }
        }
        return Recipes.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(Stack.copyWithCount(1)), World).isPresent();
    }

    public static boolean canPress(Level World, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        for (RecipeHolder<PressingRecipe> Holder : World.getRecipeManager().getAllRecipesFor(ProcessingRegistry.PressingType.get())) {
            if (Holder.value().accepts(Stack)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static RecipeHolder<PressingRecipe> choosePressing(Level World, ItemStack Input, PressDie Preferred) {
        RecipeHolder<PressingRecipe> Chosen = null;
        for (RecipeHolder<PressingRecipe> Holder : World.getRecipeManager().getRecipesFor(ProcessingRegistry.PressingType.get(), new SingleRecipeInput(Input), World)) {
            if (Holder.value().Die() == Preferred) {
                return Holder;
            }
            if (Chosen == null) {
                Chosen = Holder;
            }
        }
        return Chosen;
    }
}
