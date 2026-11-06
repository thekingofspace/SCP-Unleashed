package net.rai.scp_unleashed.gas.integration;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gas.GasStack;

import java.util.List;

public class GasSourceCategory implements IRecipeCategory<GasSourceCategory.Source> {
    public static final RecipeType<Source> ExhaustType = RecipeType.create(Scp_unleashed.ModId, "fission_exhaust", Source.class);
    public static final RecipeType<Source> SteamType = RecipeType.create(Scp_unleashed.ModId, "exchanger_steam", Source.class);

    private final RecipeType<Source> Type;
    private final String TitleKey;
    private final IDrawable Icon;
    private final IDrawableStatic Background;
    private final IDrawableStatic TankInset;
    private final IDrawableStatic SlotInset;

    public GasSourceCategory(IGuiHelper Helper, RecipeType<Source> Type, String TitleKey, ItemLike Machine) {
        this.Type = Type;
        this.TitleKey = TitleKey;
        Icon = Helper.createDrawableItemLike(Machine);
        Background = GasSheet.part(Helper, 0, GasSheet.SourceV, GasSheet.Width, GasSheet.Height);
        TankInset = GasSheet.part(Helper, GasSheet.TankInsetU, GasSheet.PartsV, GasSheet.TankWidth + 2, GasSheet.TankHeight + 2);
        SlotInset = GasSheet.part(Helper, GasSheet.SlotU, GasSheet.PartsV, GasSheet.SlotSize, GasSheet.SlotSize);
    }

    public record Source(ItemStack Machine, List<ItemStack> Items, FluidStack Liquid, GasStack Output, int Capacity, String InfoKey, List<Object> InfoArgs) {
    }

    @Override
    public RecipeType<Source> getRecipeType() {
        return Type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(TitleKey);
    }

    @Override
    public int getWidth() {
        return GasSheet.Width;
    }

    @Override
    public int getHeight() {
        return GasSheet.Height;
    }

    @Override
    public IDrawable getIcon() {
        return Icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder Builder, Source Entry, IFocusGroup Focuses) {
        if (!Entry.Liquid().isEmpty()) {
            Builder.addSlot(RecipeIngredientRole.INPUT, GasSheet.InputX, GasSheet.TankY).setFluidRenderer(Entry.Capacity(), false, GasSheet.TankWidth, GasSheet.TankHeight)
                    .addFluidStack(Entry.Liquid().getFluid(), Entry.Liquid().getAmount());
        } else if (!Entry.Items().isEmpty()) {
            Builder.addSlot(RecipeIngredientRole.INPUT, GasSheet.InputX, GasSheet.SlotY + 1).addItemStacks(Entry.Items());
        }
        Builder.addSlot(RecipeIngredientRole.CATALYST, (GasSheet.Width - 16) / 2, GasSheet.PictureY + (GasSheet.PictureHeight - 16) / 2).addItemStack(Entry.Machine());
        Builder.addSlot(RecipeIngredientRole.OUTPUT, GasSheet.OutputX, GasSheet.TankY)
                .setCustomRenderer(GasJei.Type, new GasJei.ColumnRenderer(Entry.Capacity(), GasSheet.TankWidth, GasSheet.TankHeight))
                .addIngredient(GasJei.Type, Entry.Output());
    }

    @Override
    public void draw(Source Entry, IRecipeSlotsView Slots, GuiGraphics Graphics, double MouseX, double MouseY) {
        Background.draw(Graphics);
        if (!Entry.Liquid().isEmpty()) {
            TankInset.draw(Graphics, GasSheet.InputX - 1, GasSheet.TankY - 1);
        } else if (!Entry.Items().isEmpty()) {
            SlotInset.draw(Graphics, GasSheet.InputX - 1, GasSheet.SlotY);
        }
        Font Text = Minecraft.getInstance().font;
        Component Info = Component.translatable(Entry.InfoKey(), Entry.InfoArgs().toArray());
        Graphics.drawString(Text, Info, (GasSheet.Width - Text.width(Info)) / 2, GasSheet.InfoY, GasSheet.TextColour, false);
    }
}
