package net.rai.scp_unleashed.props.integration;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.props.DecorationRecipe;
import net.rai.scp_unleashed.props.PropsRegistry;
import net.rai.scp_unleashed.props.client.DecorationWorkbenchScreen;

import java.util.List;

public class DecorationRecipeCategory implements IRecipeCategory<RecipeHolder<DecorationRecipe>> {
    public static final RecipeType<RecipeHolder<DecorationRecipe>> Type = RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "decoration"));
    public static final int Width = 92;
    public static final int Height = 26;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 214;
    public static final int MaterialX = 1;
    public static final int MaterialY = 5;
    public static final int ExtraX = 20;
    public static final int ExtraY = 5;
    public static final int ExtraFrameU = 96;
    public static final int ExtraFrameV = 218;
    public static final int OutputX = 71;
    public static final int OutputY = 5;

    private final IDrawable Icon;
    private final IDrawableStatic Background;
    private final IDrawableStatic ExtraFrame;

    public DecorationRecipeCategory(IGuiHelper Helper) {
        this.Icon = Helper.createDrawableItemLike(PropsRegistry.DecorationWorkbench.get());
        this.Background = Helper.createDrawable(DecorationWorkbenchScreen.Texture, BackgroundU, BackgroundV, Width, Height);
        this.ExtraFrame = Helper.createDrawable(DecorationWorkbenchScreen.Texture, ExtraFrameU, ExtraFrameV, 18, 18);
    }

    public static ItemStack catalyst() {
        return new ItemStack(PropsRegistry.DecorationWorkbench.get());
    }

    @Override
    public RecipeType<RecipeHolder<DecorationRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.decoration");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<DecorationRecipe> Holder, IFocusGroup Focuses) {
        DecorationRecipe Plan = Holder.value();
        Builder.addSlot(RecipeIngredientRole.INPUT, MaterialX, MaterialY).addItemStacks(List.of(Plan.Material().getItems()));
        Plan.Extra().ifPresent(Needed -> Builder.addSlot(RecipeIngredientRole.INPUT, ExtraX, ExtraY).addItemStacks(List.of(Needed.getItems())));
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, OutputY).addItemStack(Plan.Result());
    }

    @Override
    public void draw(RecipeHolder<DecorationRecipe> Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics, 0, 0);
        if (Holder.value().Extra().isPresent()) {
            ExtraFrame.draw(Graphics, ExtraX - 1, ExtraY - 1);
        }
    }

    @Override
    public ResourceLocation getRegistryName(RecipeHolder<DecorationRecipe> Holder) {
        return Holder.id();
    }
}
