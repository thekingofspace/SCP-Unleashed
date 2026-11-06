package net.rai.scp_unleashed.space.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.space.AirCanisterItem;
import net.rai.scp_unleashed.space.AirInfuserBlockEntity;
import net.rai.scp_unleashed.space.SpaceAir;
import net.rai.scp_unleashed.space.SpaceRegistry;
import net.rai.scp_unleashed.space.client.AirInfuserScreen;

import java.util.List;

public class AirInfusingCategory implements IRecipeCategory<AirInfusingCategory.Infusing> {
    public static final RecipeType<Infusing> Type = RecipeType.create(Scp_unleashed.ModId, "air_infusing", Infusing.class);
    public static final int Width = 132;
    public static final int Height = 48;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 168;
    public static final int InputX = 5;
    public static final int OutputX = 111;
    public static final int SlotY = 12;
    public static final int GaugeX = 64;
    public static final int GaugeY = 4;
    public static final int InfoY = 38;
    public static final int TextColour = 0xFF404040;
    public static final int TicksPerSecond = 20;

    private final IDrawable Icon;
    private final IDrawableStatic Background;
    private final IDrawableAnimated Gauge;

    public AirInfusingCategory(IGuiHelper Helper) {
        Icon = Helper.createDrawableItemLike(SpaceRegistry.AirInfuserItem.get());
        Background = Helper.createDrawable(AirInfuserScreen.Texture, BackgroundU, BackgroundV, Width, Height);
        IDrawableStatic Fill = Helper.createDrawable(AirInfuserScreen.Texture, AirInfuserScreen.GaugeU, AirInfuserScreen.GaugeV + 18, AirInfuserScreen.GaugeWidth, 30);
        Gauge = Helper.createAnimatedDrawable(Fill, AirInfuserBlockEntity.fillTicks(0), IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    public record Infusing(ItemStack Input, ItemStack Output) {
        public static List<Infusing> all() {
            return List.of(new Infusing(SpaceAir.canister(0), SpaceAir.canister(AirCanisterItem.Capacity)));
        }
    }

    @Override
    public RecipeType<Infusing> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.air_infusing");
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
    public void setRecipe(IRecipeLayoutBuilder Builder, Infusing Entry, IFocusGroup Focuses) {
        Builder.addSlot(RecipeIngredientRole.INPUT, InputX, SlotY).addItemStack(Entry.Input());
        Builder.addSlot(RecipeIngredientRole.OUTPUT, OutputX, SlotY).addItemStack(Entry.Output());
    }

    @Override
    public void draw(Infusing Entry, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics, 0, 0);
        Gauge.draw(Graphics, GaugeX, GaugeY);
        Font Text = Minecraft.getInstance().font;
        Component Line = Component.translatable("jei.scp_unleashed.air_infusing.info", AirInfuserBlockEntity.fillTicks(0) / TicksPerSecond, AirInfuserBlockEntity.WorkDraw);
        Graphics.drawString(Text, Line, (Width - Text.width(Line)) / 2, InfoY, TextColour, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder Tooltip, Infusing Entry, IRecipeSlotsView Slots, double MouseX, double MouseY) {
        if (MouseX >= 30 && MouseX < 102 && MouseY < InfoY) {
            Tooltip.add(Component.translatable("jei.scp_unleashed.air_infusing.tooltip").withStyle(ChatFormatting.GRAY));
        }
    }
}
