package net.rai.scp_unleashed.advmaterials;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AdvancedMaterialsRecipes {
    private AdvancedMaterialsRecipes() {
    }

    public static List<RecipeHolder<AdvancedMaterialsRecipe>> all(@Nullable Level World) {
        return World == null ? List.of() : World.getRecipeManager().getAllRecipesFor(AdvancedMaterialsRegistry.RecipeKind.get());
    }

    @Nullable
    public static RecipeHolder<AdvancedMaterialsRecipe> find(@Nullable Level World, List<ItemStack> Items) {
        RecipeHolder<AdvancedMaterialsRecipe> Best = null;
        for (RecipeHolder<AdvancedMaterialsRecipe> Holder : all(World)) {
            if (Holder.value().plan(Items) == null) {
                continue;
            }
            if (Best == null || better(Holder, Best)) {
                Best = Holder;
            }
        }
        return Best;
    }

    private static boolean better(RecipeHolder<AdvancedMaterialsRecipe> Candidate, RecipeHolder<AdvancedMaterialsRecipe> Best) {
        int CandidateCount = Candidate.value().getTotalCount();
        int BestCount = Best.value().getTotalCount();
        if (CandidateCount != BestCount) {
            return CandidateCount > BestCount;
        }
        return Candidate.id().compareTo(Best.id()) < 0;
    }

    public static boolean isIngredient(@Nullable Level World, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        if (World == null) {
            return true;
        }
        for (RecipeHolder<AdvancedMaterialsRecipe> Holder : all(World)) {
            if (Holder.value().uses(Stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canJoin(@Nullable Level World, List<ItemStack> Items, ItemStack Candidate) {
        if (World == null) {
            return true;
        }
        List<ItemStack> Kinds = new ArrayList<>();
        Kinds.add(Candidate);
        for (ItemStack Stack : Items) {
            if (!Stack.isEmpty() && Kinds.stream().noneMatch(Known -> ItemStack.isSameItemSameComponents(Known, Stack))) {
                Kinds.add(Stack);
            }
        }
        for (RecipeHolder<AdvancedMaterialsRecipe> Holder : all(World)) {
            AdvancedMaterialsRecipe Formula = Holder.value();
            if (Kinds.stream().allMatch(Formula::uses)) {
                return true;
            }
        }
        return false;
    }
}
