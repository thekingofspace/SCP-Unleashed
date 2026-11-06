package net.rai.scp_unleashed.disperser;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabVials;

public class OrganicGrenadeRecipe extends CustomRecipe {
    public static final int Parts = 2;

    public OrganicGrenadeRecipe(CraftingBookCategory Category) {
        super(Category);
    }

    @Override
    public boolean matches(CraftingInput Input, Level World) {
        int Grenades = 0;
        int Cultures = 0;
        for (ItemStack Stack : Input.items()) {
            if (Stack.isEmpty()) {
                continue;
            }
            if (OrganicGrenadeItem.isEmptyGrenade(Stack)) {
                Grenades++;
            } else if (LabVials.carriesCulture(Stack)) {
                Cultures++;
            } else {
                return false;
            }
        }
        return Grenades == 1 && Cultures == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput Input, HolderLookup.Provider Registries) {
        for (ItemStack Stack : Input.items()) {
            if (LabVials.carriesCulture(Stack)) {
                return OrganicGrenadeItem.loaded(LabVials.sampleOf(Stack));
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput Input) {
        NonNullList<ItemStack> Left = NonNullList.withSize(Input.size(), ItemStack.EMPTY);
        for (int Index = 0; Index < Input.size(); Index++) {
            ItemStack Stack = Input.getItem(Index);
            if (LabVials.carriesCulture(Stack)) {
                Left.set(Index, LabVials.openVial(null));
            } else if (Stack.hasCraftingRemainingItem()) {
                Left.set(Index, Stack.getCraftingRemainingItem());
            }
        }
        return Left;
    }

    @Override
    public boolean canCraftInDimensions(int Width, int Height) {
        return Width * Height >= Parts;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DisperserRegistry.LoadingSerializer.get();
    }

    public static ShapelessRecipe display(LabOrganism Organism) {
        NonNullList<Ingredient> Inputs = NonNullList.of(Ingredient.EMPTY, Ingredient.of(new ItemStack(DisperserRegistry.OrganicGrenade.get())),
                Ingredient.of(Organism.sealedVial(), Organism.openVial()));
        return new ShapelessRecipe("", CraftingBookCategory.MISC, OrganicGrenadeItem.loaded(Organism.getId()), Inputs);
    }
}
