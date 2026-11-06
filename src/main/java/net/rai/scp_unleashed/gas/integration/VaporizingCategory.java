package net.rai.scp_unleashed.gas.integration;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.VaporizingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VaporizingCategory implements IRecipeCategory<RecipeHolder<VaporizingRecipe>> {
    public static final RecipeType<RecipeHolder<VaporizingRecipe>> Type = RecipeType.createRecipeHolderType(GasRegistry.VaporizingId);

    private final IDrawable Icon;
    private final IDrawableStatic Background;

    public VaporizingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(GasRegistry.VaporizerItem.get());
        Background = GasSheet.part(Helper, 0, GasSheet.VaporizingV, GasSheet.Width, GasSheet.Height);
    }

    @Override
    public RecipeType<RecipeHolder<VaporizingRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.vaporizing");
    }

    @Override
    public int getWidth() {
        return GasSheet.Width;
    }

    @Override
    public int getHeight() {
        return GasSheet.Height;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<VaporizingRecipe> Holder, IFocusGroup Focuses) {
        VaporizingRecipe Recipe = Holder.value();
        int Amount = Recipe.getInput().amount();
        List<FluidStack> Inputs = new ArrayList<>();
        for (FluidStack Stack : Recipe.getInput().getFluids()) {
            Inputs.add(Stack.copyWithAmount(Amount));
        }
        int Capacity = Math.max(Amount, Recipe.getResult().getAmount());
        Builder.addSlot(RecipeIngredientRole.INPUT, GasSheet.InputX, GasSheet.TankY).setFluidRenderer(Capacity, false, GasSheet.TankWidth, GasSheet.TankHeight)
                .addIngredients(NeoForgeTypes.FLUID_STACK, Inputs);
        Builder.addSlot(RecipeIngredientRole.OUTPUT, GasSheet.OutputX, GasSheet.TankY)
                .setCustomRenderer(GasJei.Type, new GasJei.ColumnRenderer(Capacity, GasSheet.TankWidth, GasSheet.TankHeight))
                .addIngredient(GasJei.Type, Recipe.getResult());
    }

    @Override
    public void draw(RecipeHolder<VaporizingRecipe> Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
        VaporizingRecipe Recipe = Holder.value();
        Font Text = Minecraft.getInstance().font;
        String Seconds = String.format(Locale.ROOT, "%.1f", Recipe.getTime() / 20.0f);
        Component Info = Component.translatable("jei.scp_unleashed.vaporizing.info", Seconds, Recipe.getPower());
        Graphics.drawString(Text, Info, (GasSheet.Width - Text.width(Info)) / 2, GasSheet.InfoY, GasSheet.TextColour, false);
    }
}
