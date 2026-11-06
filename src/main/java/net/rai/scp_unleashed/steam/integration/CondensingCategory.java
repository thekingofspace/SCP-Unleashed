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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.integration.GasJei;
import net.rai.scp_unleashed.steam.CoolingTowerBlockEntity;
import net.rai.scp_unleashed.steam.SteamRegistry;

public class CondensingCategory implements IRecipeCategory<CondensingCategory.Condensing> {
    public static final RecipeType<Condensing> Type = RecipeType.create(Scp_unleashed.ModId, "condensing", Condensing.class);
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

    public CondensingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(SteamRegistry.CoolingTowerItem.get());
        Arrow = Helper.getRecipeArrow();
        Slot = Helper.getSlotDrawable();
        Tank = SteamJeiLayout.tankInset(Helper);
    }

    public record Condensing(int Steam) {
        public int typical() {
            return Math.round(Steam * (CoolingTowerBlockEntity.MinReturn + CoolingTowerBlockEntity.MaxReturn) / 2.0f);
        }

        public int least() {
            return Math.round(Steam * CoolingTowerBlockEntity.MinReturn);
        }

        public int most() {
            return Math.round(Steam * CoolingTowerBlockEntity.MaxReturn);
        }
    }

    @Override
    public RecipeType<Condensing> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.condensing");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, Condensing Run, IFocusGroup Focuses) {
        Builder.addSlot(RecipeIngredientRole.INPUT, InputX, SteamJeiLayout.TankY)
                .setCustomRenderer(GasJei.Type, new GasJei.ColumnRenderer(Run.Steam(), SteamJeiLayout.TankWidth, SteamJeiLayout.TankHeight))
                .addIngredient(GasJei.Type, new GasStack(GasRegistry.LowPressureSteam.get(), Run.Steam()));
        Builder.addSlot(RecipeIngredientRole.CATALYST, MachineX, MachineY).addItemStack(new ItemStack(SteamRegistry.CoolingTowerItem.get()));
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, SteamJeiLayout.TankY).setFluidRenderer(Run.Steam(), false, SteamJeiLayout.TankWidth, SteamJeiLayout.TankHeight)
                .addFluidStack(Fluids.WATER, Run.typical())
                .addRichTooltipCallback((SlotView, Lines) -> Lines.add(Component.translatable("jei.scp_unleashed.condensing.climate", Run.least(), Run.most())
                        .withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void draw(Condensing Run, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Tank.draw(Graphics, InputX - 1, SteamJeiLayout.TankY - 1);
        Tank.draw(Graphics, OutputX - 1, SteamJeiLayout.TankY - 1);
        Slot.draw(Graphics, MachineX - 1, MachineY - 1);
        Arrow.draw(Graphics, InArrowX, ArrowY);
        Arrow.draw(Graphics, OutArrowX, ArrowY);
        SteamJeiLayout.centred(Graphics, Component.translatable("jei.scp_unleashed.condensing.returns", Math.round(CoolingTowerBlockEntity.MinReturn * 100),
                Math.round(CoolingTowerBlockEntity.MaxReturn * 100)), Width, 50);
        SteamJeiLayout.centred(Graphics, Component.translatable("jei.scp_unleashed.condensing.rate", String.format("%,d", CoolingTowerBlockEntity.CondenseRate * 20)), Width, 60);
    }
}
