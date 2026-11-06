package net.rai.scp_unleashed.fission.integration;

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
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.SonicVibratorBlockEntity;

public class SonicVibratingCategory implements IRecipeCategory<SonicVibratingCategory.Cleaning> {
    public static final RecipeType<Cleaning> Type = RecipeType.create(Scp_unleashed.ModId, "sonic_vibrating", Cleaning.class);
    public static final int Width = 128;
    public static final int Height = 60;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 56;
    public static final int InputX = 5;
    public static final int OutputX = 107;
    public static final int TankY = 5;
    public static final int TankWidth = 16;
    public static final int TankHeight = 40;
    public static final int PictureX = 36;
    public static final int PictureY = 6;
    public static final int PictureWidth = 56;
    public static final int PictureHeight = 36;
    public static final int InArrowX = 24;
    public static final int OutArrowX = 94;
    public static final int FlowArrowY = 20;
    public static final int InfoY = 50;
    public static final int CleanBatch = 1000;

    private final IDrawable Icon;
    private final IDrawableStatic Background;

    public SonicVibratingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(FissionRegistry.SonicVibratorItem.get());
        Background = IrradiatingCategory.sheetPart(Helper, BackgroundU, BackgroundV, Width, Height);
    }

    public record Cleaning(int Amount) {
    }

    @Override
    public RecipeType<Cleaning> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.sonic_vibrating");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, Cleaning Entry, IFocusGroup Focuses) {
        Builder.addSlot(RecipeIngredientRole.INPUT, InputX, TankY).setFluidRenderer(Entry.Amount(), false, TankWidth, TankHeight)
                .addFluidStack(FissionRegistry.WasteWater.get(), Entry.Amount());
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, TankY).setFluidRenderer(Entry.Amount(), false, TankWidth, TankHeight)
                .addFluidStack(Fluids.WATER, Entry.Amount());
    }

    @Override
    public void draw(Cleaning Entry, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
        Font Text = Minecraft.getInstance().font;
        Component Info = Component.translatable("jei.scp_unleashed.sonic_vibrating.info", SonicVibratorBlockEntity.CleanRate * 20, SonicVibratorBlockEntity.WorkDraw);
        Graphics.drawString(Text, Info, (Width - Text.width(Info)) / 2, InfoY, IrradiatingCategory.TextColour, false);
    }
}
