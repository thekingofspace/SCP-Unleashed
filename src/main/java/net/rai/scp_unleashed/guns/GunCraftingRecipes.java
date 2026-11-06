package net.rai.scp_unleashed.guns;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GunCraftingRecipes {
    private GunCraftingRecipes() {
    }

    public static List<RecipeHolder<GunCraftingRecipe>> all(@Nullable Level World) {
        if (World == null) {
            return List.of();
        }
        List<RecipeHolder<GunCraftingRecipe>> Sorted = new ArrayList<>(World.getRecipeManager().getAllRecipesFor(GunsRegistry.GunCraftingType.get()));
        Sorted.sort(Comparator.comparingInt((RecipeHolder<GunCraftingRecipe> Holder) -> rank(Holder.value().getResult())).thenComparing(Holder -> Holder.id().toString()));
        return Sorted;
    }

    public static List<ItemStack> supplies(Player User) {
        return User.getInventory().items;
    }

    public static boolean canAfford(GunCraftingRecipe Formula, Player User) {
        return User.getAbilities().instabuild || Formula.plan(supplies(User)) != null;
    }

    private static int rank(ItemStack Result) {
        List<DeferredItem<? extends Item>> Order = GunsRegistry.creativeOrder();
        for (int Index = 0; Index < Order.size(); Index++) {
            if (Result.is(Order.get(Index).get())) {
                return Index;
            }
        }
        return Order.size();
    }
}
