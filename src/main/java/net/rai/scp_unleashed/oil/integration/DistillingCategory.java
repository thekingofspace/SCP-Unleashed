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
import net.rai.scp_unleashed.oil.DistillingRecipe;
import net.rai.scp_unleashed.oil.OilRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DistillingCategory implements IRecipeCategory<RecipeHolder<DistillingRecipe>> {
    public static final RecipeType<RecipeHolder<DistillingRecipe>> Type = RecipeType.createRecipeHolderType(OilRegistry.DistillingId);

    private final IDrawable Icon;
    private final IDrawableStatic Background;

    public DistillingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(OilRegistry.DistillationTowerItem.get());
        Background = OilJeiSheet.part(Helper, OilJeiSheet.DistillingV);
    }

    @Override
    public RecipeType<RecipeHolder<DistillingRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.distilling");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<DistillingRecipe> Holder, IFocusGroup Focuses) {
        DistillingRecipe Recipe = Holder.value();
        int Amount = Recipe.getInput().amount();
        List<FluidStack> Inputs = new ArrayList<>();
        for (FluidStack Stack : Recipe.getInput().getFluids()) {
            Inputs.add(Stack.copyWithAmount(Amount));
        }
        int Capacity = Math.max(Amount, Math.max(Recipe.getTop().getAmount(), Recipe.getBottom().getAmount()));
        Builder.addSlot(RecipeIngredientRole.INPUT, OilJeiSheet.DistilInputX, OilJeiSheet.TankY)
                .setFluidRenderer(Capacity, false, OilJeiSheet.TankWidth, OilJeiSheet.TankHeight)
                .addIngredients(NeoForgeTypes.FLUID_STACK, Inputs);
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OilJeiSheet.DistilOutputX, OilJeiSheet.TankY)
                .setFluidRenderer(Capacity, false, OilJeiSheet.TankWidth, OilJeiSheet.HalfHeight)
                .addIngredient(NeoForgeTypes.FLUID_STACK, Recipe.getTop());
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OilJeiSheet.DistilOutputX, OilJeiSheet.LowerY)
                .setFluidRenderer(Capacity, false, OilJeiSheet.TankWidth, OilJeiSheet.HalfHeight)
                .addIngredient(NeoForgeTypes.FLUID_STACK, Recipe.getBottom());
    }

    @Override
    public void draw(RecipeHolder<DistillingRecipe> Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
        DistillingRecipe Recipe = Holder.value();
        String Seconds = String.format(Locale.ROOT, "%.1f", Recipe.getTime() / 20.0f);
        OilJeiSheet.centred(Graphics, Component.translatable("jei.scp_unleashed.oil.info", Seconds, Recipe.getPower()), OilJeiSheet.InfoY);
    }
}
