package net.rai.scp_unleashed.processing.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.processing.ItemPressBlockEntity;
import net.rai.scp_unleashed.processing.PressingRecipe;
import net.rai.scp_unleashed.processing.ProcessingRecipes;
import net.rai.scp_unleashed.processing.ProcessingRegistry;
import net.rai.scp_unleashed.processing.client.ItemPressScreen;

public class PressingCategory extends ProcessingCategory<RecipeHolder<PressingRecipe>> {
    public static final RecipeType<RecipeHolder<PressingRecipe>> Type = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "pressing"));
    public static final int StampX = 26;
    public static final int StampY = 6;

    public PressingCategory(IGuiHelper Helper) {
        super(Helper, Type, "jei.scp_unleashed.pressing", ProcessingRegistry.ItemPressItem.get(), ItemPressScreen.Texture,
                ItemPressScreen.StampFullU, ItemPressScreen.StampV, StampX, StampY, ItemPressScreen.StampWidth, ItemPressScreen.StampHeight,
                IDrawableAnimated.StartDirection.TOP);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<PressingRecipe> Holder, IFocusGroup Focuses) {
        PressingRecipe Recipe = Holder.value();
        Builder.addSlot(RecipeIngredientRole.INPUT, InputX, InputY).addItemStacks(Recipe.inputStacks());
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, OutputY).addItemStack(Recipe.Result());
    }

    @Override
    protected int ticks(RecipeHolder<PressingRecipe> Holder) {
        return Holder.value().Time();
    }

    @Override
    protected int draw(RecipeHolder<PressingRecipe> Holder) {
        return ProcessingRecipes.drawFor(ItemPressBlockEntity.WorkDraw, Holder.value().PowerMultiplier());
    }

    @Override
    protected void drawExtra(RecipeHolder<PressingRecipe> Holder, GuiGraphics Graphics, Font Text) {
        Component Die = Component.translatable(Holder.value().Die().getTranslationKey());
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.pressing.die", Die), TextX, FirstLine + LineHeight * 2, AccentColour, false);
    }

    public static ItemStack catalyst() {
        return new ItemStack(ProcessingRegistry.ItemPressItem.get());
    }
}
