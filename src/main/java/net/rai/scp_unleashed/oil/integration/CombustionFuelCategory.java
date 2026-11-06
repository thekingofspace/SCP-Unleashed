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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.gas.integration.GasJei;
import net.rai.scp_unleashed.oil.CombustionFuel;
import net.rai.scp_unleashed.oil.OilRegistry;

import java.util.ArrayList;
import java.util.List;

public class CombustionFuelCategory implements IRecipeCategory<RecipeHolder<CombustionFuel>> {
    public static final RecipeType<RecipeHolder<CombustionFuel>> Type = RecipeType.createRecipeHolderType(OilRegistry.CombustionFuelId);
    public static final int LineGap = 10;

    private final IDrawable Icon;
    private final IDrawableStatic Background;

    public CombustionFuelCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(OilRegistry.CombustionGeneratorItem.get());
        Background = OilJeiSheet.part(Helper, OilJeiSheet.FuelV);
    }

    @Override
    public RecipeType<RecipeHolder<CombustionFuel>> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.combustion_fuel");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, RecipeHolder<CombustionFuel> Holder, IFocusGroup Focuses) {
        CombustionFuel Fuel = Holder.value();
        List<FluidStack> Inputs = new ArrayList<>();
        for (FluidStack Stack : Fuel.getFuel().getStacks()) {
            Inputs.add(Stack.copyWithAmount(FluidType.BUCKET_VOLUME));
        }
        Builder.addSlot(RecipeIngredientRole.INPUT, OilJeiSheet.FuelInputX, OilJeiSheet.TankY)
                .setFluidRenderer(FluidType.BUCKET_VOLUME, false, OilJeiSheet.TankWidth, OilJeiSheet.TankHeight)
                .addIngredients(NeoForgeTypes.FLUID_STACK, Inputs);
        int Exhaust = (int) Math.min(Integer.MAX_VALUE, Fuel.exhaustPerBucket());
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OilJeiSheet.FuelGasX, OilJeiSheet.TankY)
                .setCustomRenderer(GasJei.Type, new GasJei.ColumnRenderer(Math.max(Exhaust, 1), OilJeiSheet.TankWidth, OilJeiSheet.TankHeight))
                .addIngredient(GasJei.Type, Fuel.getExhaust().copyWithAmount(Math.max(1, Exhaust)));
    }

    @Override
    public void draw(RecipeHolder<CombustionFuel> Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
        CombustionFuel Fuel = Holder.value();
        Font Writer = Minecraft.getInstance().font;
        int X = OilJeiSheet.FuelTextX;
        int Y = OilJeiSheet.TankY + 2;
        Graphics.drawString(Writer, Component.translatable("jei.scp_unleashed.combustion_fuel.rate", Fuel.getPower()), X, Y, OilJeiSheet.TextColour, false);
        Graphics.drawString(Writer, Component.translatable("jei.scp_unleashed.combustion_fuel.time", Fuel.bucketSeconds()), X, Y + LineGap, OilJeiSheet.TextColour, false);
        Graphics.drawString(Writer, Component.translatable("jei.scp_unleashed.combustion_fuel.smoke", Fuel.getExhaust().getAmount() * 20), X, Y + LineGap * 2,
                OilJeiSheet.TextColour, false);
        OilJeiSheet.centred(Graphics, Component.translatable("jei.scp_unleashed.combustion_fuel.total", Fuel.energyPerBucket()), OilJeiSheet.InfoY);
    }
}
