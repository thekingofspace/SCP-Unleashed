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
import net.rai.scp_unleashed.steam.SteamRegistry;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;

public class TurbineCategory implements IRecipeCategory<TurbineCategory.TurbineRun> {
    public static final RecipeType<TurbineRun> Type = RecipeType.create(Scp_unleashed.ModId, "steam_turbine", TurbineRun.class);
    public static final int Width = 128;
    public static final int Height = 70;
    public static final int InputX = 5;
    public static final int OutputX = 107;
    public static final int InArrowX = 25;
    public static final int OutArrowX = 79;
    public static final int ArrowY = 17;
    public static final int MachineX = 56;
    public static final int MachineY = 17;

    private final IDrawable Icon;
    private final IDrawableStatic Arrow;
    private final IDrawableStatic Slot;
    private final IDrawableStatic Tank;

    public TurbineCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(SteamRegistry.SteamTurbineItem.get());
        Arrow = Helper.getRecipeArrow();
        Slot = Helper.getSlotDrawable();
        Tank = SteamJeiLayout.tankInset(Helper);
    }

    public record TurbineRun(int Steam) {
        public long power() {
            return (long) Steam * SteamTurbineBlockEntity.PowerPerSteam;
        }
    }

    @Override
    public RecipeType<TurbineRun> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.steam_turbine");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, TurbineRun Run, IFocusGroup Focuses) {
        Builder.addSlot(RecipeIngredientRole.INPUT, InputX, SteamJeiLayout.TankY)
                .setCustomRenderer(GasJei.Type, new GasJei.ColumnRenderer(Run.Steam(), SteamJeiLayout.TankWidth, SteamJeiLayout.TankHeight))
                .addIngredient(GasJei.Type, new GasStack(GasRegistry.Steam.get(), Run.Steam()));
        Builder.addSlot(RecipeIngredientRole.CATALYST, MachineX, MachineY).addItemStack(new ItemStack(SteamRegistry.SteamTurbineItem.get()));
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, SteamJeiLayout.TankY)
                .setCustomRenderer(GasJei.Type, new GasJei.ColumnRenderer(Run.Steam(), SteamJeiLayout.TankWidth, SteamJeiLayout.TankHeight))
                .addIngredient(GasJei.Type, new GasStack(GasRegistry.LowPressureSteam.get(), Run.Steam()));
    }

    @Override
    public void draw(TurbineRun Run, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Tank.draw(Graphics, InputX - 1, SteamJeiLayout.TankY - 1);
        Tank.draw(Graphics, OutputX - 1, SteamJeiLayout.TankY - 1);
        Slot.draw(Graphics, MachineX - 1, MachineY - 1);
        Arrow.draw(Graphics, InArrowX, ArrowY);
        Arrow.draw(Graphics, OutArrowX, ArrowY);
        SteamJeiLayout.centred(Graphics, Component.translatable("jei.scp_unleashed.steam_turbine.power", String.format("%,d", Run.power())), Width, 50);
        SteamJeiLayout.centred(Graphics, Component.translatable("jei.scp_unleashed.steam_turbine.rate", SteamTurbineBlockEntity.MaxOutput), Width, 60);
    }
}
