package net.rai.scp_unleashed.chunkloader.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderArea;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderBlockEntity;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderMenu;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChunkLoaderScreen extends AbstractContainerScreen<ChunkLoaderMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/chunk_loader.png");
    public static final int PanelWidth = 196;
    public static final int PanelHeight = 122;
    public static final int MapX = 8;
    public static final int MapY = 18;
    public static final int MapSize = 72;
    public static final int MaxCell = 16;
    public static final int MinusX = 88;
    public static final int PlusX = 174;
    public static final int ButtonY = 18;
    public static final int ButtonSize = 14;
    public static final int RadiusX = 104;
    public static final int RadiusWidth = 68;
    public static final int ReadoutX = 88;
    public static final int ReadoutWidth = 100;
    public static final int ReadoutHeight = 11;
    public static final int ChunksY = 36;
    public static final int BlocksY = 49;
    public static final int DrawY = 62;
    public static final int FeedY = 75;
    public static final int BarX = 88;
    public static final int BarY = 89;
    public static final int BarWidth = 100;
    public static final int BarHeight = 5;
    public static final int ToggleX = 8;
    public static final int ToggleY = 98;
    public static final int ToggleWidth = 74;
    public static final int ToggleHeight = 16;
    public static final int OutlineX = 88;
    public static final int OutlineY = 98;
    public static final int OutlineSize = 16;
    public static final int LampX = 110;
    public static final int LampY = 103;
    public static final int LampSize = 6;
    public static final int LimitX = 122;
    public static final int LimitY = 98;
    public static final int LimitWidth = 66;
    public static final int LimitHeight = 16;
    public static final int MinusU = 0;
    public static final int PlusU = 28;
    public static final int ButtonV = 128;
    public static final int EyeU = 56;
    public static final int EyeV = 128;
    public static final int LampU = 120;
    public static final int LampV = 128;
    public static final int ToggleU = 0;
    public static final int ToggleV = 144;
    public static final int BarU = 0;
    public static final int BarV = 176;
    public static final int LcdColour = 0xFF5CFF8A;
    public static final int LcdDimColour = 0xFF2A7A44;
    public static final int LcdWarnColour = 0xFFFFB13B;
    public static final int LcdHotColour = 0xFFFF5040;
    public static final int TitleColour = 0xFF404040;
    public static final int CellDim = 0xFF16402A;
    public static final int CellOff = 0xFF1E2A24;
    public static final int CellDead = 0xFF3A1A16;
    public static final int GridColour = 0xFF0A1C12;
    public static final int MarkerColour = 0xFFF0FFF4;
    public static final int MarkerLitColour = 0xFF0A2A16;

    private enum HoverSpot {
        Map(MapX - 1, MapY - 1, MapSize + 2, MapSize + 2),
        Minus(MinusX, ButtonY, ButtonSize, ButtonSize),
        Plus(PlusX, ButtonY, ButtonSize, ButtonSize),
        Radius(RadiusX, ButtonY, RadiusWidth, ButtonSize),
        Area(ReadoutX, ChunksY, ReadoutWidth, BlocksY + ReadoutHeight - ChunksY),
        Draw(ReadoutX, DrawY, ReadoutWidth, ReadoutHeight),
        Feed(ReadoutX, FeedY, ReadoutWidth, BarY + BarHeight + 1 - FeedY),
        Toggle(ToggleX, ToggleY, ToggleWidth, ToggleHeight),
        Outline(OutlineX, OutlineY, OutlineSize, OutlineSize),
        Limit(LampX - 2, LimitY, LimitX + LimitWidth - LampX + 2, LimitHeight);

        private final int X;
        private final int Y;
        private final int Width;
        private final int Height;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.X = X;
            this.Y = Y;
            this.Width = Width;
            this.Height = Height;
        }

        private boolean contains(double GuiX, double GuiY) {
            return GuiX >= X && GuiX < X + Width && GuiY >= Y && GuiY < Y + Height;
        }
    }

    private int Ticks;

    public ChunkLoaderScreen(ChunkLoaderMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
    }

    @Nullable
    private static HoverSpot spotAt(double GuiX, double GuiY) {
        for (HoverSpot Spot : HoverSpot.values()) {
            if (Spot.contains(GuiX, GuiY)) {
                return Spot;
            }
        }
        return null;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        HoverSpot Hovered = spotAt(MouseX - leftPos, MouseY - topPos);
        Graphics.blit(Texture, leftPos + MinusX, topPos + ButtonY, MinusU + (Hovered == HoverSpot.Minus ? ButtonSize : 0), ButtonV, ButtonSize, ButtonSize);
        Graphics.blit(Texture, leftPos + PlusX, topPos + ButtonY, PlusU + (Hovered == HoverSpot.Plus ? ButtonSize : 0), ButtonV, ButtonSize, ButtonSize);
        int EyeFrame = (ChunkLoaderClient.isOutlineShown() ? 0 : 2) + (Hovered == HoverSpot.Outline ? 1 : 0);
        Graphics.blit(Texture, leftPos + OutlineX, topPos + OutlineY, EyeU + EyeFrame * OutlineSize, EyeV, OutlineSize, OutlineSize);
        int ToggleRow = menu.isEnabled() ? 1 : 0;
        Graphics.blit(Texture, leftPos + ToggleX, topPos + ToggleY, ToggleU + (Hovered == HoverSpot.Toggle ? ToggleWidth : 0), ToggleV + ToggleRow * ToggleHeight, ToggleWidth, ToggleHeight);
        int Demand = menu.getDemand();
        float Share = Demand <= 0 ? (menu.isEnabled() ? 1.0f : 0.0f) : Mth.clamp(menu.getReceived() / (float) Demand, 0.0f, 1.0f);
        int Filled = Math.round(Share * BarWidth);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY, BarU, BarV, Filled, BarHeight);
        }
        int Lamp = switch (menu.getStatus()) {
            case Active -> 0;
            case Starting -> Ticks % 10 < 6 ? 3 : -1;
            case LowPower -> Ticks % 10 < 5 ? 1 : -1;
            case Unpowered -> Ticks % 20 < 10 ? 2 : -1;
            default -> -1;
        };
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
        renderMap(Graphics);
    }

    private void renderMap(GuiGraphics Graphics) {
        int Radius = menu.getRadius();
        int Side = ChunkLoaderArea.side(Radius);
        int Cell = Mth.clamp(MapSize / Side, 1, MaxCell);
        int Span = Cell * Side;
        int Left = leftPos + MapX + (MapSize - Span) / 2;
        int Top = topPos + MapY + (MapSize - Span) / 2;
        ChunkLoaderStatus Status = menu.getStatus();
        int Base = switch (Status) {
            case Off -> CellOff;
            case Unpowered -> CellDead;
            default -> CellDim;
        };
        Graphics.fill(Left, Top, Left + Span, Top + Span, Base);
        int Total = Side * Side;
        int Loaded = Status.isHolding() ? Mth.clamp(menu.getLoaded(), 0, Total) : 0;
        int Lit = Status == ChunkLoaderStatus.LowPower ? (Ticks % 10 < 5 ? LcdWarnColour : 0xFF8A5A1A) : LcdColour;
        if (Loaded > 0) {
            int Rings = ((int) Math.sqrt(Loaded) - 1) / 2;
            int Whole = ChunkLoaderArea.chunkCount(Rings);
            int Inner = ChunkLoaderArea.side(Rings) * Cell;
            int Offset = (Radius - Rings) * Cell;
            Graphics.fill(Left + Offset, Top + Offset, Left + Offset + Inner, Top + Offset + Inner, Lit);
            for (int Index = Whole; Index < Loaded; Index++) {
                int CellX = Left + (ChunkLoaderArea.offsetX(Index) + Radius) * Cell;
                int CellY = Top + (ChunkLoaderArea.offsetZ(Index) + Radius) * Cell;
                Graphics.fill(CellX, CellY, CellX + Cell, CellY + Cell, Lit);
            }
        }
        if (Cell >= 4) {
            for (int Line = 1; Line < Side; Line++) {
                Graphics.fill(Left + Line * Cell, Top, Left + Line * Cell + 1, Top + Span, GridColour);
                Graphics.fill(Left, Top + Line * Cell, Left + Span, Top + Line * Cell + 1, GridColour);
            }
            Graphics.fill(Left, Top, Left + Span, Top + 1, GridColour);
            Graphics.fill(Left, Top, Left + 1, Top + Span, GridColour);
        }
        int Middle = Left + Radius * Cell;
        int MiddleTop = Top + Radius * Cell;
        if (Cell >= 6) {
            int Dot = Math.max(2, Cell / 4);
            int Gap = (Cell - Dot + 1) / 2;
            Graphics.fill(Middle + Gap, MiddleTop + Gap, Middle + Gap + Dot, MiddleTop + Gap + Dot, Loaded > 0 ? MarkerLitColour : MarkerColour);
        } else {
            int Mark = Math.max(1, Cell);
            Graphics.fill(Middle - 1, MiddleTop - 1, Middle + Mark + 1, MiddleTop, MarkerColour);
            Graphics.fill(Middle - 1, MiddleTop + Mark, Middle + Mark + 1, MiddleTop + Mark + 1, MarkerColour);
            Graphics.fill(Middle - 1, MiddleTop, Middle, MiddleTop + Mark, MarkerColour);
            Graphics.fill(Middle + Mark, MiddleTop, Middle + Mark + 1, MiddleTop + Mark, MarkerColour);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY, TitleColour, false);
        Component Status = statusLine();
        Graphics.drawString(font, Status, PanelWidth - 8 - font.width(Status), titleLabelY, statusColour(), false);
        int Radius = menu.getRadius();
        int Side = ChunkLoaderArea.side(Radius);
        int Blocks = ChunkLoaderArea.blockSpan(Radius);
        boolean On = menu.isEnabled();
        lcd(Graphics, Component.translatable("gui.scp_unleashed.chunk_loader.lcd.radius", Radius), RadiusX, ButtonY, RadiusWidth, ButtonSize, LcdColour, true);
        lcd(Graphics, Component.translatable("gui.scp_unleashed.chunk_loader.lcd.chunks", Side, Side), ReadoutX, ChunksY, ReadoutWidth, ReadoutHeight, LcdColour, false);
        lcd(Graphics, Component.translatable("gui.scp_unleashed.chunk_loader.lcd.blocks", Blocks, Blocks), ReadoutX, BlocksY, ReadoutWidth, ReadoutHeight, LcdColour, false);
        lcd(Graphics, Component.translatable("gui.scp_unleashed.chunk_loader.lcd.draw", menu.getDemand()), ReadoutX, DrawY, ReadoutWidth, ReadoutHeight, On ? LcdColour : LcdDimColour, false);
        int Received = menu.getReceived();
        int FeedColour = !On ? LcdDimColour : Received >= menu.getDemand() ? LcdColour : Received > 0 ? LcdWarnColour : LcdHotColour;
        lcd(Graphics, Component.translatable("gui.scp_unleashed.chunk_loader.lcd.feed", Received), ReadoutX, FeedY, ReadoutWidth, ReadoutHeight, FeedColour, false);
        lcd(Graphics, Component.translatable("gui.scp_unleashed.chunk_loader.lcd.limit", menu.getMaxRadius()), LimitX, LimitY, LimitWidth, LimitHeight, LcdDimColour, true);
        Component Toggle = Component.translatable(On ? "gui.scp_unleashed.chunk_loader.stop" : "gui.scp_unleashed.chunk_loader.start");
        Graphics.drawString(font, Toggle, ToggleX + (ToggleWidth - font.width(Toggle)) / 2, ToggleY + (ToggleHeight - 8) / 2, 0xFFFFFFFF, true);
    }

    private void lcd(GuiGraphics Graphics, Component Text, int X, int Y, int Width, int Height, int Colour, boolean Centred) {
        int TextWidth = font.width(Text);
        int TextX = Centred ? X + (Width - TextWidth) / 2 : X + Width - 3 - TextWidth;
        Graphics.drawString(font, Text, TextX, Y + (Height - 7) / 2, Colour, false);
    }

    private Component statusLine() {
        ChunkLoaderStatus Status = menu.getStatus();
        return switch (Status) {
            case Starting -> Component.translatable("gui.scp_unleashed.chunk_loader.status.starting_count", menu.getLoaded(), ChunkLoaderArea.chunkCount(menu.getRadius()));
            case LowPower -> Component.translatable("gui.scp_unleashed.chunk_loader.status.low_power_count", seconds(menu.getGraceTicks()));
            default -> Component.translatable(Status.getTranslationKey());
        };
    }

    private int statusColour() {
        ChunkLoaderStatus Status = menu.getStatus();
        if (Status == ChunkLoaderStatus.LowPower) {
            return Ticks % 10 < 6 ? 0xFFC07000 : 0xFF804000;
        }
        return Status.getPanelColour();
    }

    private static int seconds(int Ticks) {
        return (Ticks + 19) / 20;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0 && minecraft != null && minecraft.gameMode != null) {
            HoverSpot Spot = spotAt(MouseX - leftPos, MouseY - topPos);
            if (Spot == HoverSpot.Outline) {
                ChunkLoaderClient.toggleOutline();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4f));
                return true;
            }
            int Id = -1;
            if (Spot == HoverSpot.Minus) {
                Id = hasShiftDown() ? ChunkLoaderBlockEntity.ButtonShrinkMore : ChunkLoaderBlockEntity.ButtonShrink;
            } else if (Spot == HoverSpot.Plus) {
                Id = hasShiftDown() ? ChunkLoaderBlockEntity.ButtonGrowMore : ChunkLoaderBlockEntity.ButtonGrow;
            } else if (Spot == HoverSpot.Toggle) {
                Id = ChunkLoaderBlockEntity.ButtonToggle;
            }
            if (Id >= 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Id);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, Id == ChunkLoaderBlockEntity.ButtonToggle ? 0.8f : 1.2f));
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        double GuiX = MouseX - leftPos;
        double GuiY = MouseY - topPos;
        HoverSpot Spot = spotAt(GuiX, GuiY);
        if (Spot != null) {
            Graphics.renderComponentTooltip(font, tooltipFor(Spot), MouseX, MouseY);
        }
    }

    private List<Component> tooltipFor(HoverSpot Spot) {
        List<Component> Lines = new ArrayList<>();
        int Radius = menu.getRadius();
        int Max = menu.getMaxRadius();
        int Side = ChunkLoaderArea.side(Radius);
        int Total = Side * Side;
        ChunkLoaderStatus Status = menu.getStatus();
        switch (Spot) {
            case Map -> {
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.map", Status.isHolding() ? Math.min(menu.getLoaded(), Total) : 0, Total));
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.map_centre").withStyle(ChatFormatting.GRAY));
                if (Status == ChunkLoaderStatus.Starting) {
                    Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.map_starting").withStyle(ChatFormatting.GRAY));
                }
            }
            case Minus -> {
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.shrink"));
                if (Radius <= 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.smallest").withStyle(ChatFormatting.GRAY));
                } else {
                    Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.next", ChunkLoaderArea.side(Radius - 1), ChunkLoaderArea.side(Radius - 1), menu.demandFor(Radius - 1)).withStyle(ChatFormatting.GRAY));
                }
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.shift_step", ChunkLoaderBlockEntity.StepLarge).withStyle(ChatFormatting.DARK_GRAY));
            }
            case Plus -> {
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.grow"));
                if (Radius >= Max) {
                    Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.largest", Max).withStyle(ChatFormatting.GOLD));
                } else {
                    Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.next", ChunkLoaderArea.side(Radius + 1), ChunkLoaderArea.side(Radius + 1), menu.demandFor(Radius + 1)).withStyle(ChatFormatting.GRAY));
                }
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.shift_step", ChunkLoaderBlockEntity.StepLarge).withStyle(ChatFormatting.DARK_GRAY));
            }
            case Radius -> {
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.radius", Radius));
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.radius_hint").withStyle(ChatFormatting.GRAY));
            }
            case Area -> {
                int Blocks = ChunkLoaderArea.blockSpan(Radius);
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.area", Side, Side, Total, Blocks, Blocks));
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.area_hint").withStyle(ChatFormatting.GRAY));
            }
            case Draw -> {
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.draw", menu.getDemand()));
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.draw_hint", menu.getBasePower(), menu.getPowerPerChunk()).withStyle(ChatFormatting.GRAY));
                if (!menu.isEnabled()) {
                    Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.draw_off").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case Feed -> {
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.power", menu.getReceived(), menu.isEnabled() ? menu.getDemand() : 0));
                if (Status == ChunkLoaderStatus.LowPower) {
                    Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.power_short", seconds(menu.getGraceTicks())).withStyle(ChatFormatting.GOLD));
                } else if (Status == ChunkLoaderStatus.Unpowered) {
                    Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.power_needed", menu.getDemand()).withStyle(ChatFormatting.RED));
                } else {
                    Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.power_hint").withStyle(ChatFormatting.GRAY));
                }
            }
            case Toggle -> Lines.add(Component.translatable(menu.isEnabled() ? "gui.scp_unleashed.chunk_loader.stop_hint" : "gui.scp_unleashed.chunk_loader.start_hint"));
            case Outline -> {
                Lines.add(Component.translatable(ChunkLoaderClient.isOutlineShown() ? "gui.scp_unleashed.chunk_loader.outline_on" : "gui.scp_unleashed.chunk_loader.outline_off"));
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.outline_hint").withStyle(ChatFormatting.GRAY));
            }
            case Limit -> {
                Lines.add(statusLine().copy().withStyle(Status.getColour()));
                Lines.add(Component.translatable("gui.scp_unleashed.chunk_loader.limit", Max, ChunkLoaderArea.side(Max), ChunkLoaderArea.side(Max)).withStyle(ChatFormatting.GRAY));
            }
        }
        return Lines;
    }
}
