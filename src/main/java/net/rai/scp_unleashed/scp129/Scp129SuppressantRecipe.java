package net.rai.scp_unleashed.scp129;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabVials;
import org.jetbrains.annotations.Nullable;

public class Scp129SuppressantRecipe extends CustomRecipe {
    public static final int Yield = 2;
    public static final int Parts = 4;

    public Scp129SuppressantRecipe(CraftingBookCategory Category) {
        super(Category);
    }

    @Nullable
    public static Scp129Strain strainOf(@Nullable ResourceLocation Sample) {
        for (Scp129Strain Strain : Scp129Strain.values()) {
            if (Strain.id().equals(Sample)) {
                return Strain;
            }
        }
        return null;
    }

    public static boolean isCulture(ItemStack Stack) {
        return LabVials.isVial(Stack) && strainOf(LabVials.sampleOf(Stack)) != null;
    }

    @Override
    public boolean matches(CraftingInput Input, Level World) {
        int Cultures = 0;
        int Copper = 0;
        int Coal = 0;
        int Lichen = 0;
        for (ItemStack Stack : Input.items()) {
            if (Stack.isEmpty()) {
                continue;
            }
            if (isCulture(Stack)) {
                Cultures++;
            } else if (Stack.is(Items.COPPER_INGOT)) {
                Copper++;
            } else if (Stack.is(ItemTags.COALS)) {
                Coal++;
            } else if (Stack.is(Items.GLOW_LICHEN)) {
                Lichen++;
            } else {
                return false;
            }
        }
        return Cultures == 1 && Copper == 1 && Coal == 1 && Lichen == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput Input, HolderLookup.Provider Registries) {
        return new ItemStack(Scp129Registry.Suppressant.get(), Yield);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput Input) {
        NonNullList<ItemStack> Left = NonNullList.withSize(Input.size(), ItemStack.EMPTY);
        for (int Index = 0; Index < Input.size(); Index++) {
            ItemStack Stack = Input.getItem(Index);
            if (isCulture(Stack)) {
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
        return Scp129Registry.SuppressantSerializer.get();
    }

    public static ShapelessRecipe display() {
        Ingredient Cultures = Ingredient.of(Scp129Registry.Organisms.stream().map(LabOrganism::sealedVial));
        NonNullList<Ingredient> Inputs = NonNullList.of(Ingredient.EMPTY, Cultures, Ingredient.of(Items.COPPER_INGOT), Ingredient.of(ItemTags.COALS), Ingredient.of(Items.GLOW_LICHEN));
        return new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Scp129Registry.Suppressant.get(), Yield), Inputs);
    }
}
