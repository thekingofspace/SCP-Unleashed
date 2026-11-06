package net.rai.scp_unleashed.steam.integration;

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
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.integration.GasJei;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlockEntity;
import net.rai.scp_unleashed.steam.SteamRegistry;

public class IndustrialTurbineCategory implements IRecipeCategory<IndustrialTurbineCategory.IndustrialRun> {
    public static final RecipeType<IndustrialRun> Type = RecipeType.create(Scp_unleashed.ModId, "industrial_turbine", IndustrialRun.class);

    private final IDrawable Icon;
    private final IDrawableStatic Arrow;
    private final IDrawableStatic Slot;
    private final IDrawableStatic Tank;

    public IndustrialTurbineCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(SteamRegistry.IndustrialTurbineItem.get());
        Arrow = Helper.getRecipeArrow();
        Slot = Helper.getSlotDrawable();
        Tank = SteamJeiLayout.tankInset(Helper);
    }

    public record IndustrialRun(int Steam) {
        public long power() {
            return (long) Steam * IndustrialTurbineBlockEntity.PowerPerSteam;
        }
    }

    @Override
    public RecipeType<IndustrialRun> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.industrial_turbine");
    }

    @Override
    public int getWidth() {
        return TurbineCategory.Width;
    }

    @Override
    public int getHeight() {
        return TurbineCategory.Height;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, IndustrialRun Run, IFocusGroup Focuses) {
        Builder.addSlot(RecipeIngredientRole.INPUT, TurbineCategory.InputX, SteamJeiLayout.TankY)
                .setCustomRenderer(GasJei.Type, new GasJei.ColumnRenderer(Run.Steam(), SteamJeiLayout.TankWidth, SteamJeiLayout.TankHeight))
                .addIngredient(GasJei.Type, new GasStack(GasRegistry.Steam.get(), Run.Steam()));
        Builder.addSlot(RecipeIngredientRole.CATALYST, TurbineCategory.MachineX, TurbineCategory.MachineY).addItemStack(new ItemStack(SteamRegistry.IndustrialTurbineItem.get()));
        Builder.addSlot(RecipeIngredientRole.OUTPUT, TurbineCategory.OutputX, SteamJeiLayout.TankY)
                .setCustomRenderer(GasJei.Type, new GasJei.ColumnRenderer(Run.Steam(), SteamJeiLayout.TankWidth, SteamJeiLayout.TankHeight))
                .addIngredient(GasJei.Type, new GasStack(GasRegistry.LowPressureSteam.get(), Run.Steam()));
    }

    @Override
    public void draw(IndustrialRun Run, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Tank.draw(Graphics, TurbineCategory.InputX - 1, SteamJeiLayout.TankY - 1);
        Tank.draw(Graphics, TurbineCategory.OutputX - 1, SteamJeiLayout.TankY - 1);
        Slot.draw(Graphics, TurbineCategory.MachineX - 1, TurbineCategory.MachineY - 1);
        Arrow.draw(Graphics, TurbineCategory.InArrowX, TurbineCategory.ArrowY);
        Arrow.draw(Graphics, TurbineCategory.OutArrowX, TurbineCategory.ArrowY);
        SteamJeiLayout.centred(Graphics, Component.translatable("jei.scp_unleashed.steam_turbine.power", String.format("%,d", Run.power())), TurbineCategory.Width, 50);
        SteamJeiLayout.centred(Graphics, Component.translatable("jei.scp_unleashed.steam_turbine.rate", String.format("%,d", IndustrialTurbineBlockEntity.MaxOutput)), TurbineCategory.Width, 60);
    }
}
