package net.rai.scp_unleashed.labware.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.AssayRecord;
import net.rai.scp_unleashed.labware.LabAnalyzerBlockEntity;
import net.rai.scp_unleashed.labware.LabAnalyzerMenu;
import net.rai.scp_unleashed.labware.LabAnalyzerStatus;
import net.rai.scp_unleashed.labware.LabClock;
import net.rai.scp_unleashed.labware.LabOrganisms;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LabAnalyzerScreen extends AbstractContainerScreen<LabAnalyzerMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/labware/analyzer.png");
    public static final int PanelWidth = 224;
    public static final int PanelHeight = 206;
    public static final int TitleX = 10;
    public static final int TitleY = 7;
    public static final int InventoryLabelY = 116;
    public static final int CrtX = 9;
    public static final int CrtY = 25;
    public static final int CrtWidth = 162;
    public static final int CrtHeight = 80;
    public static final int TextX = 13;
    public static final int SubjectY = 28;
    public static final int StampY = 38;
    public static final int HeaderBottom = 49;
    public static final int MarkerX = 13;
    public static final int RowTextX = 21;
    public static final int FirstRowY = 52;
    public static final int RowPitch = 10;
    public static final int VisibleRows = 5;
    public static final int RowRight = 164;
    public static final int TrackX = 167;
    public static final int TrackY = 51;
    public static final int TrackWidth = 4;
    public static final int TrackHeight = 50;
    public static final int ThumbHeight = 12;
    public static final int ThumbU = 56;
    public static final int ThumbHoverU = 61;
    public static final int ThumbV = 208;
    public static final int MarkerU = 48;
    public static final int MarkerV = 208;
    public static final int MarkerSize = 5;
    public static final int TubeX = 190;
    public static final int TubeY = 48;
    public static final int TubeWidth = 4;
    public static final int TubeHeight = 28;
    public static final int TubeU = 224;
    public static final int TubeV = 0;
    public static final int PowerX = 208;
    public static final int PowerY = 27;
    public static final int PowerWidth = 6;
    public static final int PowerHeight = 72;
    public static final int PowerU = 230;
    public static final int PowerV = 0;
    public static final int LampX = 186;
    public static final int PowerLampY = 134;
    public static final int CultureLampY = 152;
    public static final int ResultLampY = 170;
    public static final int LampU = 0;
    public static final int LampV = 208;
    public static final int LampSize = 6;
    public static final int LampStep = 7;
    public static final int LampGreen = 1;
    public static final int LampAmber = 2;
    public static final int LampRed = 3;
    public static final int LampTeal = 4;
    public static final int TitleColour = 0xFFE4E2D6;
    public static final int EngraveColour = 0xFF8E97A2;
    public static final int LcdColour = 0xFF8CFFA0;
    public static final int LcdDim = 0xFF3F7A4C;
    public static final int CleanColour = 0xFF9CFFB4;
    public static final int TrimMark = 6;

    private enum HoverSpot {
        Header(CrtX, CrtY, CrtWidth, HeaderBottom - CrtY),
        Rows(CrtX, HeaderBottom, TrackX - CrtX - 1, CrtY + CrtHeight - HeaderBottom),
        Track(TrackX - 1, TrackY, TrackWidth + 2, TrackHeight),
        Tube(TubeX - 2, TubeY - 2, TubeWidth + 4, TubeHeight + 4),
        Power(PowerX - 2, PowerY - 2, PowerWidth + 4, PowerHeight + 12),
        PowerLamp(LampX - 2, PowerLampY - 2, 24, LampSize + 4),
        CultureLamp(LampX - 2, CultureLampY - 2, 24, LampSize + 4),
        ResultLamp(LampX - 2, ResultLampY - 2, 24, LampSize + 4);

        private final LabHoverArea Area;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.Area = new LabHoverArea(X, Y, Width, Height);
        }
    }

    private int Ticks;
    private int Scroll;
    private int ShownAssays = -1;
    private boolean Dragging;

    public LabAnalyzerScreen(LabAnalyzerMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        titleLabelX = TitleX;
        titleLabelY = TitleY;
        inventoryLabelY = InventoryLabelY;
    }

    @Nullable
    private LabAnalyzerBlockEntity machine() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(menu.getPos()) instanceof LabAnalyzerBlockEntity Machine ? Machine : null;
    }

    @Nullable
    private AssayRecord result() {
        LabAnalyzerBlockEntity Machine = machine();
        return Machine == null ? null : Machine.getLastResult();
    }

    private int rowCount() {
        AssayRecord Result = result();
        return Result == null ? 0 : Math.max(1, Result.Findings().size());
    }

    private int maxScroll() {
        return Math.max(0, rowCount() - VisibleRows);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        LabAnalyzerBlockEntity Machine = machine();
        int Assays = Machine == null ? 0 : Machine.getAssays();
        if (Assays != ShownAssays) {
            ShownAssays = Assays;
            Scroll = 0;
        }
        Scroll = Mth.clamp(Scroll, 0, maxScroll());
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Culture = Math.round(menu.getProgress() * TubeHeight);
        if (Culture > 0) {
            Graphics.blit(Texture, leftPos + TubeX, topPos + TubeY + TubeHeight - Culture, TubeU, TubeV + TubeHeight - Culture, TubeWidth, Culture);
        }
        int Powered = Math.round(menu.getLoad() * PowerHeight);
        if (Powered > 0 && menu.getDemand() > 0) {
            Graphics.blit(Texture, leftPos + PowerX, topPos + PowerY + PowerHeight - Powered, PowerU, PowerV + PowerHeight - Powered, PowerWidth, Powered);
        }
        lamp(Graphics, PowerLampY, powerLamp());
        lamp(Graphics, CultureLampY, cultureLamp());
        lamp(Graphics, ResultLampY, resultLamp());
        AssayRecord Result = result();
        if (Result != null && Result.isPositive()) {
            for (int Row = 0; Row < VisibleRows; Row++) {
                int Index = Scroll + Row;
                if (Index >= Result.Findings().size()) {
                    break;
                }
                int Colour = LabOrganisms.colourOf(Result.Findings().get(Index));
                Graphics.setColor(((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f, 1.0f);
                Graphics.blit(Texture, leftPos + MarkerX, topPos + FirstRowY + Row * RowPitch + 1, MarkerU, MarkerV, MarkerSize, MarkerSize);
                Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        if (maxScroll() > 0) {
            boolean Hover = Dragging || isHovering(TrackX, thumbY(), TrackWidth, ThumbHeight, MouseX, MouseY);
            Graphics.blit(Texture, leftPos + TrackX, topPos + thumbY(), Hover ? ThumbHoverU : ThumbU, ThumbV, TrackWidth, ThumbHeight);
        }
    }

    private void lamp(GuiGraphics Graphics, int Y, int Index) {
        if (Index > 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + Y, LampU + Index * LampStep, LampV, LampSize, LampSize);
        }
    }

    private int powerLamp() {
        LabAnalyzerStatus Status = menu.getStatus();
        if (Status == LabAnalyzerStatus.Unpowered) {
            return Ticks % 20 < 10 ? LampRed : 0;
        }
        if (Status == LabAnalyzerStatus.Underpowered) {
            return LampAmber;
        }
        return menu.getReceived() > 0 ? LampGreen : 0;
    }

    private int cultureLamp() {
        LabAnalyzerStatus Status = menu.getStatus();
        if (Status.isRunning()) {
            return Ticks % 12 < 8 ? LampTeal : 0;
        }
        return Status == LabAnalyzerStatus.Blocked ? LampAmber : 0;
    }

    private int resultLamp() {
        AssayRecord Result = result();
        if (Result == null) {
            return 0;
        }
        return Result.isPositive() ? (Ticks % 16 < 10 ? LampRed : 0) : LampGreen;
    }

    private int thumbY() {
        int Max = maxScroll();
        return Max <= 0 ? TrackY : TrackY + Math.round((TrackHeight - ThumbHeight) * (Scroll / (float) Max));
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY, TitleColour, false);
        Graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, EngraveColour, false);
        Component Status = statusLine();
        Graphics.drawString(font, Status, PanelWidth - 10 - font.width(Status), titleLabelY, statusColour(), false);
        AssayRecord Result = result();
        if (Result == null) {
            centred(Graphics, Component.translatable("gui.scp_unleashed.lab_analyzer.lcd.awaiting"), CrtY + 26, LcdColour);
            centred(Graphics, Component.translatable("gui.scp_unleashed.lab_analyzer.lcd.insert"), CrtY + 40, LcdDim);
            return;
        }
        Component Label = Component.translatable("gui.scp_unleashed.lab_analyzer.lcd.subject");
        Graphics.drawString(font, Label, TextX, SubjectY, LcdDim, false);
        int NameX = TextX + font.width(Label) + 4;
        Graphics.drawString(font, trimmed(Result.subjectName(), RowRight - NameX), NameX, SubjectY, LcdColour, false);
        Graphics.drawString(font, LabClock.stamp(Result.Sampled()), TextX, StampY, LcdDim, false);
        LabAnalyzerBlockEntity Machine = machine();
        Component Number = Component.translatable("gui.scp_unleashed.lab_analyzer.lcd.number", String.format(Locale.ROOT, "%04d", Machine == null ? 0 : Machine.getAssays()));
        Graphics.drawString(font, Number, RowRight - font.width(Number), StampY, LcdDim, false);
        if (!Result.isPositive()) {
            Graphics.drawString(font, trimmed(Component.translatable("gui.scp_unleashed.lab_analyzer.lcd.clean"), RowRight - TextX), TextX, FirstRowY, CleanColour, false);
            Graphics.drawString(font, trimmed(Component.translatable("gui.scp_unleashed.lab_analyzer.lcd.release"), RowRight - TextX), TextX, FirstRowY + RowPitch, LcdDim, false);
            return;
        }
        for (int Row = 0; Row < VisibleRows; Row++) {
            int Index = Scroll + Row;
            if (Index >= Result.Findings().size()) {
                break;
            }
            Component Name = LabOrganisms.nameOf(Result.Findings().get(Index));
            Graphics.drawString(font, trimmed(Name, RowRight - RowTextX), RowTextX, FirstRowY + Row * RowPitch, LcdColour, false);
        }
    }

    private void centred(GuiGraphics Graphics, Component Text, int Y, int Colour) {
        Graphics.drawString(font, Text, CrtX + (CrtWidth - font.width(Text)) / 2, Y, Colour, false);
    }

    private String trimmed(Component Text, int Width) {
        String Plain = Text.getString();
        if (font.width(Plain) <= Width) {
            return Plain;
        }
        return font.plainSubstrByWidth(Plain, Width - TrimMark) + "..";
    }

    private Component statusLine() {
        LabAnalyzerStatus Status = menu.getStatus();
        if (Status.isRunning()) {
            return Component.translatable("gui.scp_unleashed.lab_analyzer.lcd.progress", Component.translatable(Status.getTranslationKey()),
                    Math.round(menu.getProgress() * 100.0f));
        }
        return Component.translatable(Status.getTranslationKey());
    }

    private int statusColour() {
        return switch (menu.getStatus()) {
            case Working -> 0xFF7FE0F0;
            case Underpowered, Blocked -> 0xFFFFB84A;
            case Unpowered -> Ticks % 20 < 12 ? 0xFFFF6A5A : 0xFF8A2A22;
            case Idle -> 0xFFA9B0B8;
        };
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
        if (!menu.getCarried().isEmpty() || Dragging) {
            return;
        }
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            renderTooltip(Graphics, MouseX, MouseY);
            return;
        }
        double GuiX = MouseX - leftPos;
        double GuiY = MouseY - topPos;
        HoverSpot Spot = spotAt(GuiX, GuiY);
        if (Spot == null) {
            return;
        }
        List<Component> Lines = tooltipFor(Spot, GuiY);
        if (!Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private List<Component> tooltipFor(HoverSpot Spot, double GuiY) {
        List<Component> Lines = new ArrayList<>();
        AssayRecord Result = result();
        switch (Spot) {
            case Header -> {
                if (Result == null) {
                    Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.awaiting"));
                    Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.awaiting_hint").withStyle(ChatFormatting.GRAY));
                    break;
                }
                Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.subject", Result.subjectName()));
                Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.sampled", LabClock.stamp(Result.Sampled())).withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.printed", LabClock.stamp(Result.Printed())).withStyle(ChatFormatting.GRAY));
                Lines.add(Result.isPositive() ? Component.translatable("gui.scp_unleashed.lab_analyzer.found", Result.Findings().size()).withStyle(ChatFormatting.RED)
                        : Component.translatable("gui.scp_unleashed.lab_analyzer.clean").withStyle(ChatFormatting.GREEN));
            }
            case Rows -> {
                if (Result == null) {
                    break;
                }
                if (!Result.isPositive()) {
                    Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.clean").withStyle(ChatFormatting.GREEN));
                    Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.clean_hint").withStyle(ChatFormatting.GRAY));
                    break;
                }
                int Row = (int) Math.floor((GuiY - FirstRowY + 1) / RowPitch);
                int Index = Scroll + Row;
                if (Row < 0 || Row >= VisibleRows || Index >= Result.Findings().size()) {
                    break;
                }
                Lines.add(LabOrganisms.styledNameOf(Result.Findings().get(Index)));
                Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.finding", Result.subjectName()).withStyle(ChatFormatting.GRAY));
                if (maxScroll() > 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.scroll", Index + 1, Result.Findings().size()).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case Track -> {
                if (maxScroll() > 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.more", rowCount() - VisibleRows));
                    Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.more_hint").withStyle(ChatFormatting.GRAY));
                }
            }
            case Tube -> {
                LabAnalyzerStatus Status = menu.getStatus();
                Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
                Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.progress", Math.round(menu.getProgress() * 100.0f)).withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable(Status.getHintKey()).withStyle(ChatFormatting.DARK_GRAY));
            }
            case Power, PowerLamp -> {
                Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.power", menu.getReceived(), menu.getDemand()));
                Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.power_hint", LabAnalyzerBlockEntity.WorkDraw, LabAnalyzerBlockEntity.WorkTicks / 20)
                        .withStyle(ChatFormatting.GRAY));
            }
            case CultureLamp -> {
                LabAnalyzerStatus Status = menu.getStatus();
                Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
                Lines.add(Component.translatable(Status.getHintKey()).withStyle(ChatFormatting.GRAY));
            }
            case ResultLamp -> {
                if (Result == null) {
                    Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.awaiting"));
                } else {
                    Lines.add(Result.isPositive() ? Component.translatable("gui.scp_unleashed.lab_analyzer.found", Result.Findings().size()).withStyle(ChatFormatting.RED)
                            : Component.translatable("gui.scp_unleashed.lab_analyzer.clean").withStyle(ChatFormatting.GREEN));
                }
                Lines.add(Component.translatable("gui.scp_unleashed.lab_analyzer.comparator").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return Lines;
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        if (ScrollY != 0.0 && maxScroll() > 0 && isHovering(CrtX, CrtY, CrtWidth, CrtHeight, MouseX, MouseY)) {
            Scroll = Mth.clamp(Scroll - (int) Math.signum(ScrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT && maxScroll() > 0 && isHovering(TrackX - 1, TrackY, TrackWidth + 2, TrackHeight, MouseX, MouseY)) {
            double GuiY = MouseY - topPos;
            int Thumb = thumbY();
            if (GuiY < Thumb) {
                Scroll = Math.max(0, Scroll - VisibleRows);
            } else if (GuiY >= Thumb + ThumbHeight) {
                Scroll = Math.min(maxScroll(), Scroll + VisibleRows);
            } else {
                Dragging = true;
            }
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseDragged(double MouseX, double MouseY, int Button, double DragX, double DragY) {
        if (Dragging) {
            double Along = (MouseY - topPos - TrackY - ThumbHeight / 2.0) / (TrackHeight - ThumbHeight);
            Scroll = Mth.clamp((int) Math.round(Along * maxScroll()), 0, maxScroll());
            return true;
        }
        return super.mouseDragged(MouseX, MouseY, Button, DragX, DragY);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Dragging && Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Dragging = false;
            return true;
        }
        return super.mouseReleased(MouseX, MouseY, Button);
    }
}
