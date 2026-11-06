package net.rai.scp_unleashed.reactor.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.integration.IrradiatingCategory;
import net.rai.scp_unleashed.reactor.ReactorRegistry;

import java.util.Locale;

public class TowerFuelCategory implements IRecipeCategory<TowerFuelCategory.Rod> {
    public static final RecipeType<Rod> Type = RecipeType.create(Scp_unleashed.ModId, "fission_tower_fuel", Rod.class);
    public static final int Width = 180;
    public static final int Height = 40;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 120;
    public static final int SlotX = 5;
    public static final int SlotY = 12;
    public static final int TextX = 28;
    public static final int[] LineY = {4, 15, 26};

    private final IDrawable Icon;
    private final IDrawableStatic Background;

    public TowerFuelCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(ReactorRegistry.FuelTowerItem.get());
        Background = IrradiatingCategory.sheetPart(Helper, BackgroundU, BackgroundV, Width, Height);
    }

    public record Rod(ItemStack Stack, ItemStack Spent, float Heat, int Minutes, float Gas) {
    }

    @Override
    public RecipeType<Rod> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.fission_tower_fuel");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, Rod Entry, IFocusGroup Focuses) {
        Builder.addSlot(RecipeIngredientRole.INPUT, SlotX, SlotY).addItemStack(Entry.Stack());
        if (!Entry.Spent().isEmpty()) {
            Builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(Entry.Spent());
        }
    }

    @Override
    public void draw(Rod Entry, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
        Font Text = Minecraft.getInstance().font;
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.fission_tower_fuel.heat", String.format(Locale.ROOT, "%.0f", Entry.Heat())), TextX, LineY[0], IrradiatingCategory.TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.fission_tower_fuel.time", Entry.Minutes()), TextX, LineY[1], IrradiatingCategory.TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.fission_tower_fuel.gas", String.format(Locale.ROOT, "%.1f", Entry.Gas())), TextX, LineY[2], IrradiatingCategory.TextColour, false);
    }
}
