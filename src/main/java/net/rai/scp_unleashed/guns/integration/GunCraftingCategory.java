package net.rai.scp_unleashed.guns.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunCraftingRecipe;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.client.GunBenchScreen;

import java.util.List;

public class GunCraftingCategory implements IRecipeCategory<RecipeHolder<GunCraftingRecipe>> {
    public static final RecipeType<RecipeHolder<GunCraftingRecipe>> Type = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gun_crafting"));
    public static final int Width = 124;
    public static final int Height = 40;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 200;
    public static final int SlotLeft = 1;
    public static final int SlotTop = 3;
    public static final int OutputX = 98;
    public static final int OutputY = 12;

    private final IDrawable Icon;
    private final IDrawableStatic Background;

    public GunCraftingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(GunsRegistry.GunBenchItem.get());
        Background = Helper.drawableBuilder(GunBenchScreen.Texture, BackgroundU, BackgroundV, Width, Height)
                .setTextureSize(GunBenchScreen.TextureWidth, GunBenchScreen.TextureHeight).build();
    }

    @Override
    public RecipeType<RecipeHolder<GunCraftingRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.gun_crafting");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<GunCraftingRecipe> Holder, IFocusGroup Focuses) {
        List<SizedIngredient> Inputs = Holder.value().getInputs();
        for (int Index = 0; Index < Inputs.size(); Index++) {
            Builder.addSlot(RecipeIngredientRole.INPUT, SlotLeft + Index % 3 * 18, SlotTop + Index / 3 * 18).addItemStacks(List.of(Inputs.get(Index).getItems()));
        }
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, OutputY).addItemStack(Holder.value().getResult());
    }

    @Override
    public void draw(RecipeHolder<GunCraftingRecipe> Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
    }
}
