package net.rai.scp_unleashed.sulfur.integration;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlockEntity;

import java.util.List;
import java.util.Locale;

public class AcidMixingCategory implements IRecipeCategory<AcidMixingCategory.Mixing> {
    public static final RecipeType<Mixing> Type = RecipeType.create(Scp_unleashed.ModId, "acid_mixing", Mixing.class);
    public static final ResourceLocation Sheet = SulfurRegistry.location("textures/gui/sulfur/jei.png");
    public static final int SheetSize = 256;
    public static final int Width = 140;
    public static final int Height = 60;
    public static final int SlotX = 5;
    public static final int SlotY = 18;
    public static final int WaterX = 28;
    public static final int AcidX = 118;
    public static final int TankY = 5;
    public static final int TankWidth = 16;
    public static final int TankHeight = 40;
    public static final int InfoY = 50;
    public static final int TextColour = 0xFF404040;

    public record Mixing(ItemStack Sulfur, FluidStack Water, FluidStack Acid, int Ticks, int Power) {
        public static Mixing standard() {
            return new Mixing(new ItemStack(SulfurRegistry.Sulfur.get(), IndustrialTankBlockEntity.BatchSulfur),
                    new FluidStack(Fluids.WATER, IndustrialTankBlockEntity.BatchWater), new FluidStack(SulfurRegistry.Acid.get(), IndustrialTankBlockEntity.BatchAcid),
                    IndustrialTankBlockEntity.BatchTicks, IndustrialTankBlockEntity.WorkDraw);
        }
    }

    private final IDrawable Icon;
    private final IDrawableStatic Background;

    public AcidMixingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(SulfurRegistry.TankItem.get());
        Background = Helper.drawableBuilder(Sheet, 0, 0, Width, Height).setTextureSize(SheetSize, SheetSize).build();
    }

    @Override
    public RecipeType<Mixing> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.acid_mixing");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, Mixing Recipe, IFocusGroup Focuses) {
        int Capacity = Math.max(Recipe.Water().getAmount(), Recipe.Acid().getAmount());
        Builder.addSlot(RecipeIngredientRole.INPUT, SlotX + 1, SlotY + 1).addItemStack(Recipe.Sulfur());
        Builder.addSlot(RecipeIngredientRole.INPUT, WaterX, TankY).setFluidRenderer(Capacity, false, TankWidth, TankHeight)
                .addIngredients(NeoForgeTypes.FLUID_STACK, List.of(Recipe.Water()));
        Builder.addSlot(RecipeIngredientRole.OUTPUT, AcidX, TankY).setFluidRenderer(Capacity, false, TankWidth, TankHeight)
                .addIngredients(NeoForgeTypes.FLUID_STACK, List.of(Recipe.Acid()));
    }

    @Override
    public void draw(Mixing Recipe, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
        Font Text = Minecraft.getInstance().font;
        String Seconds = String.format(Locale.ROOT, "%.1f", Recipe.Ticks() / 20.0f);
        Component Info = Component.translatable("jei.scp_unleashed.acid_mixing.info", Seconds, Recipe.Power());
        Graphics.drawString(Text, Info, (Width - Text.width(Info)) / 2, InfoY, TextColour, false);
    }
}
