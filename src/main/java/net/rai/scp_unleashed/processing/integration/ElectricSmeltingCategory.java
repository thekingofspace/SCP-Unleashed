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
import net.rai.scp_unleashed.processing.ElectricFurnaceBlockEntity;
import net.rai.scp_unleashed.processing.ElectricSmeltingRecipe;
import net.rai.scp_unleashed.processing.ProcessingRecipes;
import net.rai.scp_unleashed.processing.ProcessingRegistry;
import net.rai.scp_unleashed.processing.client.ElectricFurnaceScreen;

import java.util.Arrays;

public class ElectricSmeltingCategory extends ProcessingCategory<RecipeHolder<ElectricSmeltingRecipe>> {
    public static final RecipeType<RecipeHolder<ElectricSmeltingRecipe>> Type = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "electric_smelting"));
    public static final int ArrowX = 25;
    public static final int ArrowY = 12;

    public ElectricSmeltingCategory(IGuiHelper Helper) {
        super(Helper, Type, "jei.scp_unleashed.electric_smelting", ProcessingRegistry.ElectricFurnaceItem.get(), ElectricFurnaceScreen.Texture,
                ElectricFurnaceScreen.ArrowU, ElectricFurnaceScreen.ArrowV, ArrowX, ArrowY, ElectricFurnaceScreen.ArrowWidth, ElectricFurnaceScreen.ArrowHeight,
                IDrawableAnimated.StartDirection.LEFT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<ElectricSmeltingRecipe> Holder, IFocusGroup Focuses) {
        ElectricSmeltingRecipe Recipe = Holder.value();
        Builder.addSlot(RecipeIngredientRole.INPUT, InputX, InputY)
                .addItemStacks(Arrays.stream(Recipe.getInput().getItems()).map(Stack -> Stack.copyWithCount(Recipe.getCount())).toList());
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, OutputY).addItemStack(Recipe.getResult());
    }

    @Override
    protected int ticks(RecipeHolder<ElectricSmeltingRecipe> Holder) {
        return ElectricFurnaceBlockEntity.machineTicks(Holder.value());
    }

    @Override
    protected int draw(RecipeHolder<ElectricSmeltingRecipe> Holder) {
        return ProcessingRecipes.drawFor(ElectricFurnaceBlockEntity.WorkDraw, Holder.value().getPowerMultiplier());
    }

    @Override
    protected void drawExtra(RecipeHolder<ElectricSmeltingRecipe> Holder, GuiGraphics Graphics, Font Text) {
        float Experience = Holder.value().getExperience();
        if (Experience > 0.0f) {
            Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.processing.experience", String.format("%.1f", Experience)), TextX, FirstLine + LineHeight * 2, ExperienceColour, false);
        }
    }

    public static ItemStack catalyst() {
        return new ItemStack(ProcessingRegistry.ElectricFurnaceItem.get());
    }
}
