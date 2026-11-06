package net.rai.scp_unleashed.fusion.integration;

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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fusion.FusionReactorBlock;
import net.rai.scp_unleashed.fusion.FusionReactorBlockEntity;
import net.rai.scp_unleashed.fusion.FusionRegistry;

public class FusionCategory implements IRecipeCategory<FusionCategory.FusionFuel> {
    public static final RecipeType<FusionFuel> Type = RecipeType.create(Scp_unleashed.ModId, "fusion_reactor", FusionFuel.class);
    private static final int TextColour = 0xFF404040;
    private static final int OutputColour = 0xFF7A2F9E;

    private final IDrawable Icon;
    private final IDrawableStatic SlotBackground;
    private final IDrawableStatic Plus;
    private final IDrawableStatic Arrow;

    public FusionCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(FusionRegistry.FusionReactorItem.get());
        SlotBackground = Helper.getSlotDrawable();
        Plus = Helper.getRecipePlusSign();
        Arrow = Helper.getRecipeArrow();
    }

    @Override
    public RecipeType<FusionFuel> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.fusion_reactor");
    }

    @Override
    public int getWidth() {
        return 170;
    }

    @Override
    public int getHeight() {
        return 64;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, FusionFuel Fuel, IFocusGroup Focuses) {
        Builder.addSlot(RecipeIngredientRole.INPUT, 1, 5).addItemStack(Fuel.Stack());
        Builder.addSlot(RecipeIngredientRole.INPUT, 37, 5).addFluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME).setFluidRenderer(FluidType.BUCKET_VOLUME, false, 16, 16);
    }

    @Override
    public void draw(FusionFuel Fuel, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        SlotBackground.draw(Graphics, 0, 4);
        SlotBackground.draw(Graphics, 36, 4);
        Plus.draw(Graphics, 22, 8);
        Arrow.draw(Graphics, 58, 5);
        Font Text = Minecraft.getInstance().font;
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.fusion_reactor.output", FusionReactorBlock.grouped(FusionReactorBlockEntity.MaxOutput)), 86, 9, OutputColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.fusion_reactor.fuel", FusionReactorBlockEntity.FuelTicks / 1200), 0, 28, TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.fusion_reactor.water", FusionReactorBlockEntity.WaterPerTick), 0, 39, TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.fusion_reactor.startup", FusionReactorBlock.grouped(FusionReactorBlockEntity.StartupEnergy)), 0, 50, TextColour, false);
    }

    public record FusionFuel(ItemStack Stack) {
    }
}
