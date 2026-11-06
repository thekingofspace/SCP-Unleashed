package net.rai.scp_unleashed.processing.integration;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ProcessingCategory<T extends RecipeHolder<?>> implements IRecipeCategory<T> {
    public static final int Width = 150;
    public static final int Height = 40;
    public static final int BackgroundU = 0;
    public static final int BackgroundV = 168;
    public static final int InputX = 1;
    public static final int InputY = 12;
    public static final int OutputX = 57;
    public static final int OutputY = 12;
    public static final int TextX = 86;
    public static final int FirstLine = 6;
    public static final int LineHeight = 10;
    public static final int TextColour = 0xFF404040;
    public static final int AccentColour = 0xFF3F5F86;
    public static final int ExperienceColour = 0xFF5E7F1F;
    public static final int TicksPerSecond = 20;

    private final RecipeType<T> Type;
    private final String TitleKey;
    private final IGuiHelper Helper;
    private final IDrawable Icon;
    private final IDrawableStatic Background;
    private final IDrawableStatic Progress;
    private final IDrawableAnimated.StartDirection Reveal;
    private final int ProgressX;
    private final int ProgressY;
    private final int ProgressWidth;
    private final int ProgressHeight;
    private final Map<Integer, IDrawableAnimated> Animations = new HashMap<>();

    protected ProcessingCategory(IGuiHelper Helper, RecipeType<T> Type, String TitleKey, ItemLike Machine, ResourceLocation Texture,
                                 int ProgressU, int ProgressV, int ProgressX, int ProgressY, int ProgressWidth, int ProgressHeight, IDrawableAnimated.StartDirection Reveal) {
        this.Helper = Helper;
        this.Type = Type;
        this.TitleKey = TitleKey;
        this.Icon = Helper.createDrawableItemLike(Machine);
        this.Background = Helper.createDrawable(Texture, BackgroundU, BackgroundV, Width, Height);
        this.Progress = Helper.createDrawable(Texture, ProgressU, ProgressV, ProgressWidth, ProgressHeight);
        this.Reveal = Reveal;
        this.ProgressX = ProgressX;
        this.ProgressY = ProgressY;
        this.ProgressWidth = ProgressWidth;
        this.ProgressHeight = ProgressHeight;
    }

    protected abstract int ticks(T Holder);

    protected abstract int draw(T Holder);

    protected abstract void drawExtra(T Holder, GuiGraphics Graphics, Font Text);

    @Override
    public RecipeType<T> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(TitleKey);
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
    public void draw(T Holder, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics, 0, 0);
        int Ticks = ticks(Holder);
        Animations.computeIfAbsent(Ticks, Cycle -> Helper.createAnimatedDrawable(Progress, Cycle, Reveal, false)).draw(Graphics, ProgressX, ProgressY);
        Font Text = Minecraft.getInstance().font;
        String Seconds = String.format("%.1f", Ticks / (float) TicksPerSecond);
        String Energy = String.format("%,d", (long) Ticks * draw(Holder));
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.processing.time", Seconds), TextX, FirstLine, TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.processing.energy", Energy), TextX, FirstLine + LineHeight, TextColour, false);
        drawExtra(Holder, Graphics, Text);
    }

    @Override
    public void getTooltip(ITooltipBuilder Tooltip, T Holder, IRecipeSlotsView Slots, double MouseX, double MouseY) {
        boolean OverProgress = MouseX >= ProgressX && MouseX < ProgressX + ProgressWidth && MouseY >= ProgressY && MouseY < ProgressY + ProgressHeight;
        boolean OverText = MouseX >= TextX && MouseY >= FirstLine - 1 && MouseY < FirstLine + LineHeight * 2;
        if (OverProgress || OverText) {
            Tooltip.addAll(List.of(
                    Component.translatable("jei.scp_unleashed.processing.draw", draw(Holder)),
                    Component.translatable("jei.scp_unleashed.processing.scaling").withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public ResourceLocation getRegistryName(T Holder) {
        return Holder.id();
    }
}
