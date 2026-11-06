package net.rai.scp_unleashed.advmaterials.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsRecipe;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsRegistry;
import net.rai.scp_unleashed.advmaterials.client.AdvancedMaterialsScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedMaterialsCategory implements IRecipeCategory<RecipeHolder<AdvancedMaterialsRecipe>> {
    public static final RecipeType<RecipeHolder<AdvancedMaterialsRecipe>> Type = RecipeType.createRecipeHolderType(AdvancedMaterialsRegistry.RecipeId);
    public static final int Width = 150;
    public static final int Height = 64;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 168;
    public static final int InputX = 5;
    public static final int InputY = 5;
    public static final int ArrowX = 60;
    public static final int ArrowY = 16;
    public static final int OutputX = 93;
    public static final int OutputY = 14;
    public static final int TimeX = 5;
    public static final int DrawX = 62;
    public static final int TextY = 45;
    public static final int EnergyY = 55;
    private static final int TextColour = 0xFF3A3F45;
    private static final int BoostColour = 0xFFB0400C;

    private final IGuiHelper Helper;
    private final IDrawable Icon;
    private final IDrawableStatic Background;
    private final IDrawableStatic ArrowFill;
    private final Map<Integer, IDrawableAnimated> Arrows = new HashMap<>();

    public AdvancedMaterialsCategory(IGuiHelper Helper) {
        this.Helper = Helper;
        Icon = Helper.createDrawableItemLike(AdvancedMaterialsRegistry.BenchItem.get());
        Background = Helper.createDrawable(AdvancedMaterialsScreen.Texture, BackgroundU, BackgroundV, Width, Height);
        ArrowFill = Helper.createDrawable(AdvancedMaterialsScreen.Texture, AdvancedMaterialsScreen.ArrowU, AdvancedMaterialsScreen.ArrowV,
                AdvancedMaterialsScreen.ArrowWidth, AdvancedMaterialsScreen.ArrowHeight);
    }

    @Override
    public RecipeType<RecipeHolder<AdvancedMaterialsRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.advanced_materials");
    }

    @Override
    public int getWidth() {
        return Width;
    }

    @Override
    public int getHeight() {
        return Height;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<AdvancedMaterialsRecipe> Holder, IFocusGroup Focuses) {
        List<SizedIngredient> Inputs = Holder.value().getInputs();
        for (int Index = 0; Index < Inputs.size(); Index++) {
            Builder.addSlot(RecipeIngredientRole.INPUT, InputX + Index % 2 * 18, InputY + Index / 2 * 18).addItemStacks(List.of(Inputs.get(Index).getItems()));
        }
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, OutputY).addItemStack(Holder.value().getResult());
    }

    @Override
    public void draw(RecipeHolder<AdvancedMaterialsRecipe> Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        AdvancedMaterialsRecipe Formula = Holder.value();
        Background.draw(Graphics);
        Arrows.computeIfAbsent(Formula.getTime(), Ticks -> Helper.createAnimatedDrawable(ArrowFill, Ticks, IDrawableAnimated.StartDirection.LEFT, false))
                .draw(Graphics, ArrowX, ArrowY);
        Font Text = Minecraft.getInstance().font;
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.advanced_materials.time", String.format("%.1f", Formula.getTime() / 20.0f)), TimeX, TextY, TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.advanced_materials.draw", Formula.getDraw()), DrawX, TextY,
                Formula.getPower() > AdvancedMaterialsRecipe.DefaultPower ? BoostColour : TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.advanced_materials.energy", String.format("%,d", Formula.getEnergy())), TimeX, EnergyY, TextColour, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder Tooltip, RecipeHolder<AdvancedMaterialsRecipe> Holder, IRecipeSlotsView Slots, double MouseX, double MouseY) {
        if (MouseX >= ArrowX && MouseX < ArrowX + AdvancedMaterialsScreen.ArrowWidth && MouseY >= ArrowY && MouseY < ArrowY + AdvancedMaterialsScreen.ArrowHeight) {
            AdvancedMaterialsRecipe Formula = Holder.value();
            Tooltip.add(Component.translatable("jei.scp_unleashed.advanced_materials.full_power", String.format("%.1f", Formula.getTime() / 20.0f), Formula.getDraw()));
            Tooltip.add(Component.translatable("jei.scp_unleashed.advanced_materials.slower").withStyle(ChatFormatting.GRAY));
        }
    }
}
