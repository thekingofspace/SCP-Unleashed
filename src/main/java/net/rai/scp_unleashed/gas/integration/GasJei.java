package net.rai.scp_unleashed.gas.integration;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.client.GasGui;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GasJei {
    public static final IIngredientType<GasStack> Type = () -> GasStack.class;
    public static final int ListAmount = 1000;

    private GasJei() {
    }

    public static List<Component> describe(GasStack Stack) {
        List<Component> Lines = new ArrayList<>();
        if (Stack.isEmpty()) {
            return Lines;
        }
        Lines.add(Stack.getGas().getName());
        Lines.add(Component.translatable("jei.scp_unleashed.gas.amount", Stack.getAmount()).withStyle(ChatFormatting.GRAY));
        if (Stack.getGas().isToxic()) {
            Lines.add(Component.translatable("jei.scp_unleashed.gas.toxic").withStyle(ChatFormatting.RED));
        }
        return Lines;
    }

    public enum Helper implements IIngredientHelper<GasStack> {
        Instance;

        @Override
        public IIngredientType<GasStack> getIngredientType() {
            return Type;
        }

        @Override
        public String getDisplayName(GasStack Stack) {
            return Stack.isEmpty() ? "" : Stack.getGas().getName().getString();
        }

        @Override
        @SuppressWarnings("removal")
        public String getUniqueId(GasStack Stack, UidContext Context) {
            return Stack.isEmpty() ? "gas:empty" : "gas:" + Stack.getGas().getId();
        }

        @Override
        public Object getUid(GasStack Stack, UidContext Context) {
            return Stack.isEmpty() ? "gas:empty" : Stack.getGas().getId();
        }

        @Override
        public Object getGroupingUid(GasStack Stack) {
            return getUid(Stack, UidContext.Ingredient);
        }

        @Override
        public long getAmount(GasStack Stack) {
            return Stack.getAmount();
        }

        @Override
        public GasStack copyWithAmount(GasStack Stack, long Amount) {
            return Stack.copyWithAmount((int) Math.max(1, Math.min(Integer.MAX_VALUE, Amount)));
        }

        @Override
        public Iterable<Integer> getColors(GasStack Stack) {
            return Stack.isEmpty() ? List.of() : List.of(Stack.getGas().getColour() & 0xFFFFFF);
        }

        @Override
        public ResourceLocation getResourceLocation(GasStack Stack) {
            return Stack.getGas().getId();
        }

        @Override
        public GasStack copyIngredient(GasStack Stack) {
            return Stack;
        }

        @Override
        public GasStack normalizeIngredient(GasStack Stack) {
            return Stack.copyWithAmount(ListAmount);
        }

        @Override
        public boolean isValidIngredient(GasStack Stack) {
            return !Stack.isEmpty();
        }

        @Override
        public String getErrorInfo(@Nullable GasStack Stack) {
            return Stack == null ? "null gas" : Stack.toString();
        }
    }

    public enum IconRenderer implements IIngredientRenderer<GasStack> {
        Instance;

        @Override
        public void render(GuiGraphics Graphics, GasStack Stack) {
            if (!Stack.isEmpty()) {
                GasGui.drawIcon(Graphics, Stack.getGas(), 0, 0);
            }
        }

        @Override
        @SuppressWarnings("removal")
        public List<Component> getTooltip(GasStack Stack, TooltipFlag Flag) {
            return describe(Stack);
        }
    }

    public record ColumnRenderer(int Capacity, int Width, int Height) implements IIngredientRenderer<GasStack> {
        @Override
        public void render(GuiGraphics Graphics, GasStack Stack) {
            if (Stack.isEmpty()) {
                return;
            }
            int Fill = Math.max(1, Math.min(Height, Math.round(Stack.getAmount() * (float) Height / Capacity)));
            GasGui.drawGas(Graphics, Stack.getGas(), 0, Height - Fill, Width, Fill);
        }

        @Override
        @SuppressWarnings("removal")
        public List<Component> getTooltip(GasStack Stack, TooltipFlag Flag) {
            return describe(Stack);
        }

        @Override
        public int getWidth() {
            return Width;
        }

        @Override
        public int getHeight() {
            return Height;
        }
    }
}
