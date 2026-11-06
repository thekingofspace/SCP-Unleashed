package net.rai.scp_unleashed.assembler.integration;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.assembler.AssemblerRegistry;
import net.rai.scp_unleashed.assembler.AssemblingRecipe;
import net.rai.scp_unleashed.assembler.client.AdvancedAssemblerScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssemblingCategory implements IRecipeCategory<RecipeHolder<AssemblingRecipe>> {
    public static final RecipeType<RecipeHolder<AssemblingRecipe>> Type = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "assembling"));
    public static final int Width = 160;
    public static final int Height = 64;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 180;
    public static final int OutputX = 101;
    public static final int OutputY = 19;
    public static final int ArrowX = 62;
    public static final int ArrowY = 19;
    public static final int TimeX = 69;
    public static final int DrawX = 116;
    public static final int TextY = 47;
    public static final int EnergyY = 56;
    private static final int TextColour = 0xFF404040;
    private static final int BoostColour = 0xFFB0400C;

    private final IGuiHelper Helper;
    private final IDrawable Icon;
    private final IDrawableStatic Background;
    private final IDrawableStatic ArrowFill;
    private final Map<Integer, IDrawableAnimated> Arrows = new HashMap<>();

    public AssemblingCategory(IGuiHelper Helper) {
        this.Helper = Helper;
        Icon = Helper.createDrawableItemLike(AssemblerRegistry.AdvancedAssemblerItem.get());
        Background = Helper.createDrawable(AdvancedAssemblerScreen.Texture, BackgroundU, BackgroundV, Width, Height);
        ArrowFill = Helper.createDrawable(AdvancedAssemblerScreen.Texture, AdvancedAssemblerScreen.ArrowU, AdvancedAssemblerScreen.ArrowV,
                AdvancedAssemblerScreen.ArrowWidth, AdvancedAssemblerScreen.ArrowHeight);
    }

    @Override
    public RecipeType<RecipeHolder<AssemblingRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.assembling");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<AssemblingRecipe> Holder, IFocusGroup Focuses) {
        List<SizedIngredient> Inputs = Holder.value().getInputs();
        for (int Index = 0; Index < Inputs.size(); Index++) {
            Builder.addSlot(RecipeIngredientRole.INPUT, Index % 3 * 18 + 1, Index / 3 * 18 + 1).addItemStacks(List.of(Inputs.get(Index).getItems()));
        }
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, OutputY).addItemStack(Holder.value().getResult());
    }

    @Override
    public void draw(RecipeHolder<AssemblingRecipe> Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        AssemblingRecipe Formula = Holder.value();
        Background.draw(Graphics);
        Arrows.computeIfAbsent(Formula.getTime(), Ticks -> Helper.createAnimatedDrawable(ArrowFill, Ticks, IDrawableAnimated.StartDirection.LEFT, false))
                .draw(Graphics, ArrowX, ArrowY);
        Font Text = Minecraft.getInstance().font;
        long Energy = (long) Formula.getTime() * Formula.getDraw();
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.assembling.time", String.format("%.1f", Formula.getTime() / 20.0f)), TimeX, TextY, TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.assembling.draw", Formula.getDraw()), DrawX, TextY,
                Formula.getPower() > AssemblingRecipe.DefaultPower ? BoostColour : TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.assembling.energy", String.format("%,d", Energy)), TimeX, EnergyY, TextColour, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder Tooltip, RecipeHolder<AssemblingRecipe> Holder, IRecipeSlotsView Slots, double MouseX, double MouseY) {
        if (MouseX >= ArrowX && MouseX < ArrowX + AdvancedAssemblerScreen.ArrowWidth && MouseY >= ArrowY && MouseY < ArrowY + AdvancedAssemblerScreen.ArrowHeight) {
            AssemblingRecipe Formula = Holder.value();
            Tooltip.add(Component.translatable("jei.scp_unleashed.assembling.full_power", String.format("%.1f", Formula.getTime() / 20.0f), Formula.getDraw()));
            Tooltip.add(Component.translatable("jei.scp_unleashed.assembling.slower").withStyle(ChatFormatting.GRAY));
        }
    }
}
