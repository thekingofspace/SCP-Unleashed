package net.rai.scp_unleashed.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.rai.scp_unleashed.block.entity.CoalGeneratorBlockEntity;
import net.rai.scp_unleashed.registry.ModItems;

import java.util.HashMap;
import java.util.Map;

public class CoalGeneratorCategory implements IRecipeCategory<CoalGeneratorCategory.GeneratorFuel> {
    public static final RecipeType<GeneratorFuel> Type = RecipeType.create(Scp_unleashed.ModId, "coal_generator", GeneratorFuel.class);
    private static final int TextColour = 0xFF404040;

    private final IGuiHelper Helper;
    private final IDrawable Icon;
    private final IDrawableStatic SlotBackground;
    private final IDrawableStatic FlameEmpty;
    private final Map<Integer, IDrawableAnimated> Flames = new HashMap<>();

    public CoalGeneratorCategory(IGuiHelper Helper) {
        this.Helper = Helper;
        Icon = Helper.createDrawableItemLike(ModItems.CoalGenerator.get());
        SlotBackground = Helper.getSlotDrawable();
        FlameEmpty = Helper.getRecipeFlameEmpty();
    }

    @Override
    public RecipeType<GeneratorFuel> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scp_unleashed.coal_generator");
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 36;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, GeneratorFuel Fuel, IFocusGroup Focuses) {
        IRecipeSlotBuilder Input = Builder.addSlot(RecipeIngredientRole.INPUT, 1, 10).addItemStack(Fuel.Stack());
        ItemStack Remainder = Fuel.remainder();
        if (!Remainder.isEmpty()) {
            Input.addRichTooltipCallback((SlotView, Lines) -> Lines.add(Component.translatable("jei.scp_unleashed.coal_generator.returns", Remainder.getHoverName()).withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void draw(GeneratorFuel Fuel, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        SlotBackground.draw(Graphics, 0, 9);
        FlameEmpty.draw(Graphics, 21, 11);
        Flames.computeIfAbsent(Fuel.BurnTicks(), Ticks -> Helper.createAnimatedRecipeFlame(Ticks)).draw(Graphics, 21, 11);
        Font Text = Minecraft.getInstance().font;
        int Seconds = Fuel.BurnTicks() / 20;
        long Total = (long) Fuel.BurnTicks() * CoalGeneratorBlockEntity.PowerPerSlot;
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.coal_generator.rate", CoalGeneratorBlockEntity.PowerPerSlot), 40, 4, TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.coal_generator.time", Seconds), 40, 14, TextColour, false);
        Graphics.drawString(Text, Component.translatable("jei.scp_unleashed.coal_generator.total", String.format("%,d", Total)), 40, 24, TextColour, false);
    }

    public record GeneratorFuel(ItemStack Stack, int BurnTicks) {
        public ItemStack remainder() {
            return Stack.getCraftingRemainingItem();
        }
    }
}
