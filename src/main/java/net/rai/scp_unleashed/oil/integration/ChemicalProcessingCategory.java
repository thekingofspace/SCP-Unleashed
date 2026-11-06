package net.rai.scp_unleashed.oil.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.oil.ChemicalProcessingRecipe;
import net.rai.scp_unleashed.oil.OilRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChemicalProcessingCategory implements IRecipeCategory<RecipeHolder<ChemicalProcessingRecipe>> {
    public static final RecipeType<RecipeHolder<ChemicalProcessingRecipe>> Type = RecipeType.createRecipeHolderType(OilRegistry.ChemicalProcessingId);

    private final IDrawable Icon;
    private final IDrawableStatic Background;

    public ChemicalProcessingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(OilRegistry.ChemicalProcessorItem.get());
        Background = OilJeiSheet.part(Helper, OilJeiSheet.ProcessingV);
    }

    @Override
    public RecipeType<RecipeHolder<ChemicalProcessingRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.chemical_processing");
    }

    @Override
    public int getWidth() {
        return OilJeiSheet.Width;
    }

    @Override
    public int getHeight() {
        return OilJeiSheet.Height;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<ChemicalProcessingRecipe> Holder, IFocusGroup Focuses) {
        ChemicalProcessingRecipe Recipe = Holder.value();
        int Amount = Recipe.getInput().amount();
        List<FluidStack> Inputs = new ArrayList<>();
        for (FluidStack Stack : Recipe.getInput().getFluids()) {
            Inputs.add(Stack.copyWithAmount(Amount));
        }
        int Capacity = Math.max(Amount, Recipe.getResult().getAmount());
        Builder.addSlot(RecipeIngredientRole.INPUT, OilJeiSheet.ProcessInputX, OilJeiSheet.TankY)
                .setFluidRenderer(Capacity, false, OilJeiSheet.TankWidth, OilJeiSheet.TankHeight)
                .addIngredients(NeoForgeTypes.FLUID_STACK, Inputs);
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OilJeiSheet.ProcessOutputX, OilJeiSheet.TankY)
                .setFluidRenderer(Capacity, false, OilJeiSheet.TankWidth, OilJeiSheet.TankHeight)
                .addIngredient(NeoForgeTypes.FLUID_STACK, Recipe.getResult());
    }

    @Override
    public void draw(RecipeHolder<ChemicalProcessingRecipe> Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
        ChemicalProcessingRecipe Recipe = Holder.value();
        String Seconds = String.format(Locale.ROOT, "%.1f", Recipe.getTime() / 20.0f);
        OilJeiSheet.centred(Graphics, Component.translatable("jei.scp_unleashed.oil.info", Seconds, Recipe.getPower()), OilJeiSheet.InfoY);
    }
}
