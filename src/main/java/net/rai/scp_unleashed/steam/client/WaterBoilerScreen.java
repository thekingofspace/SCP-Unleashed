package net.rai.scp_unleashed.steam.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.fission.client.FissionGuiFluids;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.client.GasGui;
import net.rai.scp_unleashed.steam.BoilerStatus;
import net.rai.scp_unleashed.steam.WaterBoilerBlockEntity;
import net.rai.scp_unleashed.steam.WaterBoilerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WaterBoilerScreen extends AbstractContainerScreen<WaterBoilerMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/steam/water_boiler.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 166;
    public static final int BayX = 22;
    public static final int BayY = 14;
    public static final int BayWidth = 132;
    public static final int BayHeight = 56;
    public static final int WaterX = 26;
    public static final int SteamX = 134;
    public static final int TankY = 19;
    public static final int TankWidth = 16;
    public static final int TankHeight = 46;
    public static final int DrumX = 56;
    public static final int DrumY = 18;
    public static final int DrumWidth = 64;
    public static final int DrumHeight = 20;
    public static final int HeatX = 56;
    public static final int HeatY = 41;
    public static final int HeatWidth = 64;
    public static final int HeatHeight = 3;
    public static final int HeatU = 176;
    public static final int HeatV = 40;
    public static final int FireboxX = 46;
    public static final int FireboxY = 47;
    public static final int FireboxWidth = 82;
    public static final int FireboxHeight = 20;
    public static final int FlameX = 49;
    public static final int FlameY = 50;
    public static final int FlameSize = 14;
    public static final int FlameU = 176;
    public static final int FlameV = 0;
    public static final int ArrowY = 24;
    public static final int InArrowX = 45;
    public static final int OutArrowX = 122;
    public static final int ArrowWidth = 10;
    public static final int ArrowHeight = 7;
    public static final int ArrowU = 204;
    public static final int ArrowV = 0;
    public static final int LampX = 121;
    public static final int LampY = 54;
    public static final int LampSize = 6;
    public static final int LampU = 176;
    public static final int LampV = 16;
    public static final int WispU = 176;
    public static final int WispV = 24;
    public static final int WispWidth = 6;
    public static final int WispHeight = 7;
    public static final int Wisps = 6;
    public static final int BubbleU = 184;
    public static final int BubbleV = 24;
    public static final int BubbleSize = 3;
    public static final int Bubbles = 7;
    public static final int MinPool = 3;
    public static final HoverArea RecipeArea = new HoverArea(DrumX - 1, DrumY - 1, DrumWidth + 2, DrumHeight + 2);

    private enum HoverSpot {
        WaterTank(WaterX - 1, TankY - 1, TankWidth + 2, TankHeight + 2),
        SteamTank(SteamX - 1, TankY - 1, TankWidth + 2, TankHeight + 2),
        Drum(DrumX - 1, DrumY - 1, DrumWidth + 2, DrumHeight + 2),
        Flame(FlameX - 1, FlameY - 1, FlameSize + 2, FlameSize + 2),
        Heat(HeatX - 1, HeatY - 1, HeatWidth + 2, HeatHeight + 2),
        Lamp(LampX - 1, LampY - 1, LampSize + 2, LampSize + 2);

        private final HoverArea Area;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.Area = new HoverArea(X, Y, Width, Height);
        }
    }

    private float Boil;
    private float PreviousBoil;
    private int Ticks;

    public WaterBoilerScreen(WaterBoilerMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = 72;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        PreviousBoil = Boil;
        float Target = Mth.clamp(menu.getBoiled() / (float) WaterBoilerBlockEntity.BoilRate, 0.0f, 1.0f);
        Boil += (Target - Boil) * (Target > Boil ? 0.08f : 0.04f);
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int WaterFill = Math.round((float) menu.getWater() / WaterBoilerBlockEntity.WaterCapacity * TankHeight);
        if (WaterFill > 0) {
            FissionGuiFluids.drawFluid(Graphics, Fluids.WATER, leftPos + WaterX, topPos + TankY + TankHeight - WaterFill, TankWidth, WaterFill);
        }
        int SteamFill = Math.round((float) menu.getSteam() / WaterBoilerBlockEntity.SteamCapacity * TankHeight);
        if (SteamFill > 0) {
            GasGui.drawGas(Graphics, GasRegistry.Steam.get(), leftPos + SteamX, topPos + TankY + TankHeight - SteamFill, TankWidth, SteamFill);
        }
        float Shown = Mth.lerp(PartialTick, PreviousBoil, Boil);
        renderDrum(Graphics, Shown, Ticks + PartialTick);
        float Burn = menu.burnFraction();
        if (Burn > 0.0f) {
            int Height = Math.max(1, Math.round(Burn * FlameSize));
            Graphics.blit(Texture, leftPos + FlameX, topPos + FlameY + FlameSize - Height, FlameU, FlameV + FlameSize - Height, FlameSize, Height);
        }
        int HeatFill = Math.round((float) menu.getHeat() / WaterBoilerBlockEntity.MaxHeat * HeatWidth);
        if (HeatFill > 0) {
            Graphics.blit(Texture, leftPos + HeatX, topPos + HeatY, HeatU, HeatV, HeatFill, HeatHeight);
        }
        if (Shown > 0.05f) {
            int Lit = Math.max(1, Math.round(Shown * ArrowWidth));
            Graphics.blit(Texture, leftPos + InArrowX, topPos + ArrowY, ArrowU, ArrowV, Lit, ArrowHeight);
            Graphics.blit(Texture, leftPos + OutArrowX, topPos + ArrowY, ArrowU, ArrowV, Lit, ArrowHeight);
        }
        int Lamp = switch (menu.getStatus()) {
            case Boiling -> 0;
            case Heating, Cooling -> 1;
            case Blocked -> Ticks % 20 < 12 ? 1 : -1;
            case NoWater -> Ticks % 20 < 10 ? 2 : -1;
            default -> -1;
        };
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
    }

    private void renderDrum(GuiGraphics Graphics, float Shown, float Time) {
        int Left = leftPos + DrumX;
        int Top = topPos + DrumY;
        int Bottom = Top + DrumHeight;
        int Water = menu.getWater();
        int Pool = Water <= 0 ? 0 : Math.max(MinPool, Math.round((float) Water / WaterBoilerBlockEntity.WaterCapacity * (DrumHeight - 8)));
        int Surface = Bottom - Pool;
        if (Pool > 0) {
            FissionGuiFluids.drawFluid(Graphics, Fluids.WATER, Left, Surface, DrumWidth, Pool);
        }
        if (Shown <= 0.02f) {
            return;
        }
        Graphics.enableScissor(Left, Top, Left + DrumWidth, Bottom);
        RenderSystem.enableBlend();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Shown);
        if (Pool > BubbleSize + 1) {
            for (int Index = 0; Index < Bubbles; Index++) {
                float Speed = 0.5f + (Index * 37 % 7) * 0.07f;
                float Travel = (Time * Speed + Index * 4.7f) % Math.max(1, Pool - BubbleSize);
                int X = Left + 4 + (Index * 17 + 5) % (DrumWidth - 8);
                int Y = Bottom - BubbleSize - Math.round(Travel);
                Graphics.blit(Texture, X, Y, BubbleU, BubbleV, BubbleSize, BubbleSize);
            }
        }
        int Rise = Math.max(1, Surface - WispHeight - Top);
        for (int Index = 0; Index < Wisps; Index++) {
            float Life = (Time * 0.035f + Index / (float) Wisps) % 1.0f;
            int X = Left + 4 + Index * 9 + Math.round(Mth.sin(Time * 0.11f + Index * 1.9f) * 2.0f);
            int Y = Surface - WispHeight - Math.round(Life * Rise);
            Graphics.setColor(1.0f, 1.0f, 1.0f, Shown * (1.0f - Life * Life));
            Graphics.blit(Texture, X, Y, WispU, WispV, WispWidth, WispHeight);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        Graphics.disableScissor();
    }

    @Nullable
    private static HoverSpot spotAt(double GuiX, double GuiY) {
        for (HoverSpot Spot : HoverSpot.values()) {
            if (Spot.Area.contains(GuiX, GuiY)) {
                return Spot;
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            renderTooltip(Graphics, MouseX, MouseY);
            return;
        }
        double GuiX = MouseX - leftPos;
        double GuiY = MouseY - topPos;
        HoverSpot Spot = spotAt(GuiX, GuiY);
        if (Spot != null) {
            Graphics.renderComponentTooltip(font, tooltipFor(Spot, GuiX, GuiY), MouseX, MouseY);
        }
    }

    private List<Component> tooltipFor(HoverSpot Spot, double GuiX, double GuiY) {
        List<Component> Lines = new ArrayList<>();
        switch (Spot) {
            case WaterTank -> {
                Lines.add(Component.translatable("gui.scp_unleashed.water_boiler.water", menu.getWater(), WaterBoilerBlockEntity.WaterCapacity));
                Lines.add(Component.translatable("gui.scp_unleashed.water_boiler.water_hint").withStyle(ChatFormatting.GRAY));
            }
            case SteamTank -> {
                Lines.add(Component.translatable("gui.scp_unleashed.water_boiler.steam", menu.getSteam(), WaterBoilerBlockEntity.SteamCapacity));
                Lines.add(Component.translatable("gui.scp_unleashed.water_boiler.steam_hint").withStyle(ChatFormatting.GRAY));
            }
            case Flame -> {
                Lines.add(Component.translatable(menu.burnFraction() > 0.0f ? "gui.scp_unleashed.water_boiler.burning" : "gui.scp_unleashed.water_boiler.unlit",
                        Math.round(menu.burnFraction() * 100.0f)));
                Lines.add(Component.translatable("gui.scp_unleashed.water_boiler.fuel_hint").withStyle(ChatFormatting.GRAY));
            }
            case Heat -> {
                Lines.add(Component.translatable("gui.scp_unleashed.water_boiler.heat", Math.round(menu.getHeat() * 100.0f / WaterBoilerBlockEntity.MaxHeat)));
                Lines.add(Component.translatable("gui.scp_unleashed.water_boiler.heat_hint").withStyle(ChatFormatting.GRAY));
            }
            case Drum, Lamp -> {
                BoilerStatus Status = menu.getStatus();
                Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
                if (menu.getBoiled() > 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.water_boiler.rate", menu.getBoiled() * 20).withStyle(ChatFormatting.GRAY));
                }
                return RecipeHint.with(Lines, RecipeArea.contains(GuiX, GuiY));
            }
        }
        return Lines;
    }
}
