package net.rai.scp_unleashed.assembler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AssemblingRecipes {
    private AssemblingRecipes() {
    }

    public static List<RecipeHolder<AssemblingRecipe>> all(@Nullable Level World) {
        return World == null ? List.of() : World.getRecipeManager().getAllRecipesFor(AssemblerRegistry.AssemblingType.get());
    }

    @Nullable
    public static RecipeHolder<AssemblingRecipe> byId(@Nullable Level World, ResourceLocation Id) {
        for (RecipeHolder<AssemblingRecipe> Holder : all(World)) {
            if (Holder.id().equals(Id)) {
                return Holder;
            }
        }
        return null;
    }

    @Nullable
    public static RecipeHolder<AssemblingRecipe> find(@Nullable Level World, List<ItemStack> Items) {
        RecipeHolder<AssemblingRecipe> Best = null;
        for (RecipeHolder<AssemblingRecipe> Holder : all(World)) {
            if (Holder.value().plan(Items) == null) {
                continue;
            }
            if (Best == null || better(Holder, Best)) {
                Best = Holder;
            }
        }
        return Best;
    }

    private static boolean better(RecipeHolder<AssemblingRecipe> Candidate, RecipeHolder<AssemblingRecipe> Best) {
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
        for (RecipeHolder<AssemblingRecipe> Holder : all(World)) {
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
        for (RecipeHolder<AssemblingRecipe> Holder : all(World)) {
            AssemblingRecipe Formula = Holder.value();
            if (Kinds.stream().allMatch(Formula::uses)) {
                return true;
            }
        }
        return false;
    }
}
