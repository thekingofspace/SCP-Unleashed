package net.rai.scp_unleashed.reactor.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.reactor.IrradiationChamberBlockEntity;
import net.rai.scp_unleashed.reactor.IrradiationChamberMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IrradiationChamberScreen extends AbstractContainerScreen<IrradiationChamberMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fission/irradiation_chamber.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 166;
    public static final int FluxX = 14;
    public static final int FluxY = 18;
    public static final int FluxWidth = 10;
    public static final int FluxHeight = 50;
    public static final int FluxU = 176;
    public static final int FluxV = 0;
    public static final int WindowX = 65;
    public static final int WindowY = 19;
    public static final int WindowWidth = 45;
    public static final int WindowHeight = 48;
    public static final int GlowU = 188;
    public static final int GlowV = 0;
    public static final int ArrowX = 76;
    public static final int ArrowY = 44;
    public static final int ArrowWidth = 24;
    public static final int ArrowHeight = 16;
    public static final int ArrowU = 176;
    public static final int ArrowV = 52;
    public static final int LampX = 152;
    public static final int[] LampY = {20, 32, 44, 56};
    public static final int LampSize = 7;
    public static final int LampU = 176;
    public static final int LampV = 70;
    public static final HoverArea RecipeArea = new HoverArea(ArrowX, ArrowY, ArrowWidth, ArrowHeight);

    public IrradiationChamberScreen(IrradiationChamberMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        float Flux = menu.getFlux();
        int Filled = Math.round(Mth.clamp(Flux / IrradiationChamberBlockEntity.MaxFlux, 0.0f, 1.0f) * FluxHeight);
        if (Flux > 0.0f && Filled == 0) {
            Filled = 1;
        }
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + FluxX, topPos + FluxY + FluxHeight - Filled, FluxU, FluxV + FluxHeight - Filled, FluxWidth, Filled);
        }
        if (Flux > 0.0f) {
            Graphics.blit(Texture, leftPos + WindowX, topPos + WindowY, GlowU, GlowV, WindowWidth, ArrowY - WindowY - 2);
        }
        int Progress = Math.round(Mth.clamp(menu.getProgress(), 0.0f, 1.0f) * ArrowWidth);
        if (Progress > 0) {
            Graphics.blit(Texture, leftPos + ArrowX, topPos + ArrowY, ArrowU, ArrowV, Progress, ArrowHeight);
        }
        for (int Index = 0; Index < LampY.length; Index++) {
            boolean Lit = Index < menu.getSources();
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY[Index], LampU + (Lit ? LampSize : 0), LampV, LampSize, LampSize);
        }
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        List<Component> Lines = new ArrayList<>();
        if (isHovering(FluxX - 1, FluxY - 1, FluxWidth + 2, FluxHeight + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.irradiation_chamber.flux", String.format(Locale.ROOT, "%.2f", menu.getFlux())));
            Lines.add(Component.translatable("gui.scp_unleashed.irradiation_chamber.flux_hint").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(ArrowX, ArrowY, ArrowWidth, ArrowHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.irradiation_chamber.progress", Math.round(menu.getProgress() * 100.0f)));
            if (menu.getRecipeTime() > 0 && menu.getFlux() > 0.0f) {
                float Seconds = (1.0f - menu.getProgress()) * menu.getRecipeTime() / menu.getFlux() / 20.0f;
                Lines.add(Component.translatable("gui.scp_unleashed.irradiation_chamber.eta", Math.round(Seconds)).withStyle(ChatFormatting.GRAY));
            } else if (menu.getFlux() <= 0.0f) {
                Lines.add(Component.translatable("gui.scp_unleashed.irradiation_chamber.idle").withStyle(ChatFormatting.GOLD));
            }
            Lines = RecipeHint.with(Lines, true);
        } else if (isHovering(LampX - 1, LampY[0] - 1, LampSize + 2, LampY[LampY.length - 1] + LampSize - LampY[0] + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.irradiation_chamber.sources", menu.getSources()));
            Lines.add(Component.translatable("gui.scp_unleashed.irradiation_chamber.sources_hint").withStyle(ChatFormatting.GRAY));
        }
        if (Lines.isEmpty()) {
            renderTooltip(Graphics, MouseX, MouseY);
        } else {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }
}
