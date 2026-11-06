package net.rai.scp_unleashed.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
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
import net.rai.scp_unleashed.block.entity.ChargeDisperserBlockEntity;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.wire.ChargingRedstone;

public class ChargingCategory implements IRecipeCategory<ChargingCategory.ChargingRecipe> {
    public static final RecipeType<ChargingRecipe> Type = RecipeType.create(Scp_unleashed.ModId, "charging", ChargingRecipe.class);
    private static final int TextColour = 0xFF404040;

    private final IDrawable Icon;
    private final IDrawableStatic SlotBackground;
    private final IDrawableAnimated Arrow;

    public ChargingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(ModItems.ChargedRedstone.get());
        SlotBackground = Helper.getSlotDrawable();
        Arrow = Helper.createAnimatedRecipeArrow(ChargingRedstone.ChargeCost / ChargingRedstone.ChargeRate);
    }

    @Override
    public RecipeType<ChargingRecipe> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.charging");
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 46;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, ChargingRecipe Recipe, IFocusGroup Focuses) {
        Builder.addSlot(RecipeIngredientRole.INPUT, 1, 3).addItemStack(Recipe.Input());
        Builder.addSlot(RecipeIngredientRole.OUTPUT, 53, 3).addItemStack(Recipe.Output());
    }

    @Override
    public void draw(ChargingRecipe Recipe, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        SlotBackground.draw(Graphics, 0, 2);
        SlotBackground.draw(Graphics, 52, 2);
        Arrow.draw(Graphics, 24, 3);
        Font Text = Minecraft.getInstance().font;
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.charging.cost", Recipe.Cost()), 78, 7, TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.charging.outlet", ChargingRedstone.ChargeRate), 0, 25, TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.charging.disperser", ChargeDisperserBlockEntity.PowerDraw), 0, 36, TextColour, false);
    }

    public record ChargingRecipe(ItemStack Input, ItemStack Output, int Cost) {
    }
}
