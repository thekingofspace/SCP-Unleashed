package net.rai.scp_unleashed.oil.integration;

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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.oil.OilRefiningRecipe;
import net.rai.scp_unleashed.oil.OilRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OilRefiningCategory implements IRecipeCategory<RecipeHolder<OilRefiningRecipe>> {
    public static final RecipeType<RecipeHolder<OilRefiningRecipe>> Type = RecipeType.createRecipeHolderType(OilRegistry.OilRefiningId);

    private final IDrawable Icon;
    private final IDrawableStatic Background;

    public OilRefiningCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(OilRegistry.OilRefineryItem.get());
        Background = OilJeiSheet.part(Helper, OilJeiSheet.RefiningV);
    }

    @Override
    public RecipeType<RecipeHolder<OilRefiningRecipe>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.oil_refining");
    }

    @Override
    public int getWidth() {
        return OilJeiSheet.Width;
    }

    @Override
    public int getHeight() {
        return OilJeiSheet.Height;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<OilRefiningRecipe> Holder, IFocusGroup Focuses) {
        OilRefiningRecipe Recipe = Holder.value();
        int Amount = Recipe.getInput().amount();
        List<FluidStack> Inputs = new ArrayList<>();
        for (FluidStack Stack : Recipe.getInput().getFluids()) {
            Inputs.add(Stack.copyWithAmount(Amount));
        }
        int Capacity = Math.max(Amount, Recipe.getByproduct().getAmount());
        Builder.addSlot(RecipeIngredientRole.INPUT, OilJeiSheet.RefineInputX, OilJeiSheet.TankY)
                .setFluidRenderer(Capacity, false, OilJeiSheet.TankWidth, OilJeiSheet.TankHeight)
                .addIngredients(NeoForgeTypes.FLUID_STACK, Inputs);
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OilJeiSheet.RefineSlotX + 1, OilJeiSheet.RefineSlotY + 1)
                .addItemStack(Recipe.getResult());
        if (!Recipe.getByproduct().isEmpty()) {
            Builder.addSlot(RecipeIngredientRole.OUTPUT, OilJeiSheet.RefineWaterX, OilJeiSheet.TankY)
                    .setFluidRenderer(Capacity, false, OilJeiSheet.TankWidth, OilJeiSheet.TankHeight)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, Recipe.getByproduct());
        }
    }

    @Override
    public void draw(RecipeHolder<OilRefiningRecipe> Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
        OilRefiningRecipe Recipe = Holder.value();
        String Seconds = String.format(Locale.ROOT, "%.1f", Recipe.getTime() / 20.0f);
        OilJeiSheet.centred(Graphics, Component.translatable("jei.scp_unleashed.oil.info", Seconds, Recipe.getPower()), OilJeiSheet.InfoY);
    }
}
