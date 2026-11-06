package net.rai.scp_unleashed.reactor.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.reactor.GridState;
import net.rai.scp_unleashed.reactor.ReactorControl;
import net.rai.scp_unleashed.reactor.ReactorControllerMenu;
import net.rai.scp_unleashed.reactor.TowerHeat;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import net.rai.scp_unleashed.reactor.core.TowerType;
import net.rai.scp_unleashed.registry.ModSounds;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReactorControllerScreen extends AbstractContainerScreen<ReactorControllerMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fission/controller.png");
    public static final ResourceLocation Parts = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/fission/controller_parts.png");
    public static final int PanelWidth = 248;
    public static final int PanelHeight = 206;
    public static final int MapX = 9;
    public static final int MapY = 19;
    public static final int MapSize = 114;
    public static final int MinCell = 5;
    public static final int MaxCell = 22;
    public static final int DetailX = 129;
    public static final int DetailY = 19;
    public static final int DetailWidth = 110;
    public static final int DetailHeight = 76;
    public static final int DetailLine = 9;
    public static final int DetailLines = 8;
    public static final int BarX = 129;
    public static final int BarY = 101;
    public static final int BarWidth = 110;
    public static final int BarHeight = 8;
    public static final int ButtonWidth = 55;
    public static final int ButtonHeight = 14;
    public static final int LeftX = 128;
    public static final int RightX = 185;
    public static final int RowOneY = 114;
    public static final int RowTwoY = 131;
    public static final int ScramX = 128;
    public static final int ScramY = 149;
    public static final int ScramWidth = 112;
    public static final int ScramHeight = 20;
    public static final int SummaryX = 9;
    public static final int SummaryY = 139;
    public static final int SummaryWidth = 114;
    public static final int SummaryLine = 12;
    public static final int LegendX = 129;
    public static final int LegendY = 174;
    public static final int LegendColumn = 56;
    public static final int LegendRow = 9;
    public static final int LampX = 233;
    public static final int LampY = 5;
    public static final int LampSize = 7;
    public static final int OutU = 0;
    public static final int InU = 56;
    public static final int ButtonV = 0;
    public static final int ScramU = 0;
    public static final int ScramV = 44;
    public static final int LampU = 120;
    public static final int LampV = 0;
    public static final int BarU = 0;
    public static final int BarV = 108;
    public static final int MarkerU = 112;
    public static final int MarkerV = 108;
    public static final int MarkerWidth = 3;
    public static final int MarkerHeight = 10;
    public static final int IconV = 120;
    public static final int IconSize = 16;
    public static final int SmallV = 136;
    public static final int SmallSize = 8;
    public static final int ScreenGreen = 0xFF7CF0A8;
    public static final int ScreenDim = 0xFF3E8A62;
    public static final int ScreenAmber = 0xFFFFC452;
    public static final int ScreenRed = 0xFFFF6A5A;
    public static final int ScreenCyan = 0xFF8FE3FF;
    public static final int ScreenTeal = 0xFF5CE0D0;
    public static final int LinkDim = 0xFF2F6A62;
    public static final int ButtonText = 0xFFF2F2F2;
    public static final int ButtonDisabledText = 0xFF8C8C8C;
    public static final int SelectColour = 0xFFF2F2F2;
    public static final int HoverColour = 0xFF9FB4A8;
    public static final int GridLine = 0xFF15301F;
    public static final int Step = 10;
    public static final int FineStep = 1;
    public static final int ButtonOut = 0;
    public static final int ButtonIn = 1;
    public static final int ButtonAllOut = 2;
    public static final int ButtonAllIn = 3;
    public static final int ButtonScram = 4;

    private final List<TowerView> Views = new ArrayList<>();
    private CompoundTag Data = new CompoundTag();
    private boolean Received;
    private int Selected = -1;
    private int SelectedX;
    private int SelectedY;
    private int SelectedZ;
    private boolean HasSelection;
    private int Ticks;
    private int Pressed = -1;

    public ReactorControllerScreen(ReactorControllerMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
    }

    public record TowerView(TowerType Type, int X, int Y, int Z, int Height, CompoundTag Info) {
        public int priority() {
            return switch (Type) {
                case Fuel -> 0;
                case Rods -> 1;
                case Booster -> 2;
                case Channel -> 3;
                case Coolant -> 4;
            };
        }
    }

    public void accept(CompoundTag Snapshot) {
        Data = Snapshot;
        Received = true;
        Views.clear();
        ListTag Towers = Snapshot.getList("Towers", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Towers.size(); Index++) {
            CompoundTag Entry = Towers.getCompound(Index);
            Views.add(new TowerView(TowerType.byIndex(Entry.getByte("T")), Entry.getShort("X"), Entry.getShort("Y"), Entry.getShort("Z"), Entry.getByte("H"), Entry));
        }
        Selected = -1;
        if (HasSelection) {
            for (int Index = 0; Index < Views.size(); Index++) {
                TowerView View = Views.get(Index);
                if (View.X() == SelectedX && View.Y() == SelectedY && View.Z() == SelectedZ) {
                    Selected = Index;
                }
            }
            if (Selected < 0) {
                HasSelection = false;
            }
        }
    }

    private GridState state() {
        return GridState.byIndex(Data.getByte("State"));
    }

    private boolean linked() {
        return Received && state() != GridState.Missing;
    }

    private TowerView selected() {
        return Selected >= 0 && Selected < Views.size() ? Views.get(Selected) : null;
    }

    private boolean selectedRods() {
        TowerView View = selected();
        return View != null && View.Type() == TowerType.Rods;
    }

    private boolean anyRods() {
        for (TowerView View : Views) {
            if (View.Type() == TowerType.Rods) {
                return true;
            }
        }
        return false;
    }

    private float averageTarget() {
        float Sum = 0.0f;
        int Count = 0;
        for (TowerView View : Views) {
            if (View.Type() == TowerType.Rods) {
                Sum += View.Info().getByte("Tgt");
                Count++;
            }
        }
        return Count == 0 ? 100.0f : Sum / Count;
    }

    private float averageInsertion() {
        float Sum = 0.0f;
        int Count = 0;
        for (TowerView View : Views) {
            if (View.Type() == TowerType.Rods) {
                Sum += View.Info().getByte("Ins");
                Count++;
            }
        }
        return Count == 0 ? 100.0f : Sum / Count;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
    }

    private record Layout(int Cell, int OffX, int OffZ, int MinX, int MinZ, int Width, int Depth) {
    }

    private Layout layout() {
        if (Views.isEmpty()) {
            return new Layout(MaxCell, 0, 0, 0, 0, 0, 0);
        }
        int MinX = Integer.MAX_VALUE;
        int MinZ = Integer.MAX_VALUE;
        int MaxX = Integer.MIN_VALUE;
        int MaxZ = Integer.MIN_VALUE;
        for (TowerView View : Views) {
            MinX = Math.min(MinX, View.X());
            MinZ = Math.min(MinZ, View.Z());
            MaxX = Math.max(MaxX, View.X());
            MaxZ = Math.max(MaxZ, View.Z());
        }
        int Width = MaxX - MinX + 1;
        int Depth = MaxZ - MinZ + 1;
        int Cell = Mth.clamp(MapSize / Math.max(Width, Depth), MinCell, MaxCell);
        int OffX = MapX + (MapSize - Width * Cell) / 2;
        int OffZ = MapY + (MapSize - Depth * Cell) / 2;
        return new Layout(Cell, OffX, OffZ, MinX, MinZ, Width, Depth);
    }

    private Map<Long, List<Integer>> columns() {
        Map<Long, List<Integer>> Found = new HashMap<>();
        for (int Index = 0; Index < Views.size(); Index++) {
            TowerView View = Views.get(Index);
            Found.computeIfAbsent(((long) View.X() << 32) ^ (View.Z() & 0xFFFFFFFFL), Key -> new ArrayList<>()).add(Index);
        }
        for (List<Integer> Column : Found.values()) {
            Column.sort((First, Second) -> Integer.compare(Views.get(First).priority(), Views.get(Second).priority()));
        }
        return Found;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        Graphics.blit(Parts, leftPos + LampX, topPos + LampY, LampU + lampIndex() * LampSize, LampV, LampSize, LampSize);
        if (linked()) {
            drawMap(Graphics, MouseX, MouseY);
        }
        drawBar(Graphics);
        button(Graphics, LeftX, RowOneY, OutU, selectedRods(), MouseX, MouseY, ButtonOut);
        button(Graphics, RightX, RowOneY, InU, selectedRods(), MouseX, MouseY, ButtonIn);
        button(Graphics, LeftX, RowTwoY, OutU, anyRods(), MouseX, MouseY, ButtonAllOut);
        button(Graphics, RightX, RowTwoY, InU, anyRods(), MouseX, MouseY, ButtonAllIn);
        boolean ScramReady = linked() && anyRods();
        int ScramRow = !ScramReady ? 2 : Pressed == ButtonScram || isHovering(ScramX, ScramY, ScramWidth, ScramHeight, MouseX, MouseY) ? 1 : 0;
        Graphics.blit(Parts, leftPos + ScramX, topPos + ScramY, ScramU, ScramV + ScramRow * ScramHeight, ScramWidth, ScramHeight);
        for (TowerType Type : TowerType.values()) {
            int Index = Type.ordinal();
            int X = leftPos + LegendX + (Index % 2) * LegendColumn;
            int Y = topPos + LegendY + (Index / 2) * LegendRow;
            Graphics.blit(Parts, X, Y, Index * 2 * SmallSize + SmallSize, SmallV, SmallSize, SmallSize);
        }
    }

    private int lampIndex() {
        if (!linked()) {
            return 0;
        }
        if (state() != GridState.Ready) {
            return Ticks % 30 < 15 ? 2 : 0;
        }
        TowerHeat Band = TowerHeat.of(Data.getFloat("Hottest"), Data.getFloat("Weakest"));
        return switch (Band) {
            case Stable -> 1;
            case Hot -> 2;
            case Critical -> Ticks % 10 < 5 ? 3 : 0;
            case Breach -> Ticks % 4 < 2 ? 3 : 0;
        };
    }

    private void drawMap(GuiGraphics Graphics, int MouseX, int MouseY) {
        Layout Plan = layout();
        int Cell = Plan.Cell();
        for (int Column = 0; Column <= Plan.Width(); Column++) {
            int X = leftPos + Plan.OffX() + Column * Cell;
            Graphics.fill(X, topPos + Plan.OffZ(), X + 1, topPos + Plan.OffZ() + Plan.Depth() * Cell, GridLine);
        }
        for (int Row = 0; Row <= Plan.Depth(); Row++) {
            int Y = topPos + Plan.OffZ() + Row * Cell;
            Graphics.fill(leftPos + Plan.OffX(), Y, leftPos + Plan.OffX() + Plan.Width() * Cell, Y + 1, GridLine);
        }
        int Hovered = hoveredColumn(MouseX, MouseY);
        for (List<Integer> Column : columns().values()) {
            int Primary = Column.getFirst();
            if (Column.contains(Selected)) {
                Primary = Selected;
            }
            TowerView View = Views.get(Primary);
            int X = leftPos + Plan.OffX() + (View.X() - Plan.MinX()) * Cell;
            int Y = topPos + Plan.OffZ() + (View.Z() - Plan.MinZ()) * Cell;
            drawCell(Graphics, View, X, Y, Cell);
            if (Column.contains(Selected)) {
                outline(Graphics, X, Y, Cell, SelectColour);
            } else if (Column.contains(Hovered)) {
                outline(Graphics, X, Y, Cell, HoverColour);
            }
            if (Column.size() > 1 && Cell >= 10) {
                Graphics.fill(X + Cell - 3, Y + 1, X + Cell - 1, Y + 3, ScreenAmber);
            }
        }
        drawLinks(Graphics, Plan);
    }

    private void drawLinks(GuiGraphics Graphics, Layout Plan) {
        int Cell = Plan.Cell();
        for (TowerView View : Views) {
            if (View.Type() != TowerType.Booster) {
                continue;
            }
            int Colour = View.Info().getByte("Gate") > 0 ? ScreenTeal : LinkDim;
            for (int Index : View.Info().getIntArray("Links")) {
                if (Index < 0 || Index >= Views.size()) {
                    continue;
                }
                TowerView Rod = Views.get(Index);
                int Dx = Rod.X() - View.X();
                int Dz = Rod.Z() - View.Z();
                if (Math.abs(Dx) + Math.abs(Dz) != 1) {
                    continue;
                }
                int CentreX = leftPos + Plan.OffX() + (View.X() - Plan.MinX()) * Cell + Cell / 2;
                int CentreZ = topPos + Plan.OffZ() + (View.Z() - Plan.MinZ()) * Cell + Cell / 2;
                int EdgeX = CentreX + Dx * (Cell / 2);
                int EdgeZ = CentreZ + Dz * (Cell / 2);
                int Reach = Math.max(1, Cell / 6);
                if (Dx != 0) {
                    Graphics.fill(EdgeX - Reach, EdgeZ - 1, EdgeX + Reach + 1, EdgeZ + 1, Colour);
                } else {
                    Graphics.fill(EdgeX - 1, EdgeZ - Reach, EdgeX + 1, EdgeZ + Reach + 1, Colour);
                }
            }
        }
        TowerView Chosen = selected();
        if (Chosen == null) {
            return;
        }
        for (int Index : Chosen.Info().getIntArray("Links")) {
            if (Index < 0 || Index >= Views.size() || Index == Selected) {
                continue;
            }
            TowerView Linked = Views.get(Index);
            int X = leftPos + Plan.OffX() + (Linked.X() - Plan.MinX()) * Cell;
            int Y = topPos + Plan.OffZ() + (Linked.Z() - Plan.MinZ()) * Cell;
            if (Ticks % 20 < 14) {
                outline(Graphics, X, Y, Cell, ScreenTeal);
            }
        }
    }

    private static void outline(GuiGraphics Graphics, int X, int Y, int Cell, int Colour) {
        Graphics.fill(X, Y, X + Cell + 1, Y + 1, Colour);
        Graphics.fill(X, Y + Cell, X + Cell + 1, Y + Cell + 1, Colour);
        Graphics.fill(X, Y, X + 1, Y + Cell + 1, Colour);
        Graphics.fill(X + Cell, Y, X + Cell + 1, Y + Cell + 1, Colour);
    }

    private boolean isActive(TowerView View) {
        CompoundTag Info = View.Info();
        return switch (View.Type()) {
            case Fuel -> Info.getFloat("Made") > ReactorTuning.ReactingFloor;
            case Rods -> Info.getByte("Ins") < 100;
            case Booster -> Info.getBoolean("Live");
            default -> Info.getFloat("Made") > 0.05f;
        };
    }

    private void drawCell(GuiGraphics Graphics, TowerView View, int X, int Y, int Cell) {
        int Inner = Cell - 1;
        int Icon = View.Type().ordinal() * 2 + (isActive(View) ? 1 : 0);
        if (Inner >= IconSize + 2) {
            int Pad = (Inner - IconSize) / 2;
            Graphics.blit(Parts, X + 1 + Pad, Y + 1 + Pad, Icon * IconSize, IconV, IconSize, IconSize);
        } else if (Inner >= SmallSize) {
            int Pad = (Inner - SmallSize) / 2;
            Graphics.blit(Parts, X + 1 + Pad, Y + 1 + Pad, Icon * SmallSize, SmallV, SmallSize, SmallSize);
        } else {
            Graphics.fill(X + 1, Y + 1, X + Cell, Y + Cell, flatColour(View));
        }
        if (View.Type() == TowerType.Fuel) {
            TowerHeat Heat = TowerHeat.of(View.Info().getShort("Temp"), View.Info().getByte("Int"));
            if (Heat != TowerHeat.Stable || View.Info().getShort("Temp") > ReactorTuning.Boil) {
                int Fill = Math.max(1, Math.round(ReactorTuning.heatFraction(View.Info().getShort("Temp")) * (Cell - 1)));
                Graphics.fill(X + 1, Y + Cell - 1, X + 1 + Fill, Y + Cell, Heat.getScreenColour());
            }
            if (Heat == TowerHeat.Critical || Heat == TowerHeat.Breach) {
                if (Ticks % 10 < 5) {
                    outline(Graphics, X, Y, Cell, Heat.getScreenColour());
                }
            }
            if (View.Info().getByte("Boost") > 0 && Cell >= 10) {
                Graphics.fill(X + 2, Y + 2, X + 4, Y + 4, ScreenTeal);
            }
        }
        if (Cell >= 14) {
            String Height = Integer.toString(View.Height());
            Graphics.drawString(font, Height, X + Cell - font.width(Height), Y + Cell - 8, ButtonText, true);
        }
    }

    private static int flatColour(TowerView View) {
        return switch (View.Type()) {
            case Fuel -> 0xFF8FD14F;
            case Rods -> 0xFFB8B8A0;
            case Coolant -> 0xFF4C86D8;
            case Channel -> 0xFFD8843C;
            case Booster -> 0xFF3AAAC6;
        };
    }

    private int hoveredColumn(int MouseX, int MouseY) {
        if (!linked() || Views.isEmpty()) {
            return -1;
        }
        Layout Plan = layout();
        int Cell = Plan.Cell();
        int LocalX = MouseX - leftPos - Plan.OffX();
        int LocalZ = MouseY - topPos - Plan.OffZ();
        if (LocalX < 0 || LocalZ < 0 || LocalX >= Plan.Width() * Cell || LocalZ >= Plan.Depth() * Cell) {
            return -1;
        }
        int ColumnX = Plan.MinX() + LocalX / Cell;
        int ColumnZ = Plan.MinZ() + LocalZ / Cell;
        int Best = -1;
        for (int Index = 0; Index < Views.size(); Index++) {
            TowerView View = Views.get(Index);
            if (View.X() == ColumnX && View.Z() == ColumnZ && (Best < 0 || View.priority() < Views.get(Best).priority())) {
                Best = Index;
            }
        }
        return Best;
    }

    private void drawBar(GuiGraphics Graphics) {
        if (!linked() || !anyRods()) {
            return;
        }
        TowerView View = selected();
        boolean Single = View != null && View.Type() == TowerType.Rods;
        float Insertion = Single ? View.Info().getByte("Ins") : averageInsertion();
        float Target = Single ? View.Info().getByte("Tgt") : averageTarget();
        int Fill = Math.round(Mth.clamp(Insertion / 100.0f, 0.0f, 1.0f) * BarWidth);
        if (Fill > 0) {
            Graphics.blit(Parts, leftPos + BarX, topPos + BarY, BarU, BarV, Fill, BarHeight);
        }
        int Marker = Math.round(Mth.clamp(Target / 100.0f, 0.0f, 1.0f) * (BarWidth - 1));
        Graphics.blit(Parts, leftPos + BarX + Marker - 1, topPos + BarY - 1, MarkerU, MarkerV, MarkerWidth, MarkerHeight);
    }

    private void button(GuiGraphics Graphics, int X, int Y, int U, boolean Enabled, int MouseX, int MouseY, int Id) {
        int Row = !Enabled ? 2 : Pressed == Id || isHovering(X, Y, ButtonWidth, ButtonHeight, MouseX, MouseY) ? 1 : 0;
        Graphics.blit(Parts, leftPos + X, topPos + Y, U, ButtonV + Row * ButtonHeight, ButtonWidth, ButtonHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        label(Graphics, Component.translatable("gui.scp_unleashed.reactor_controller.out"), LeftX, RowOneY, ButtonWidth, selectedRods());
        label(Graphics, Component.translatable("gui.scp_unleashed.reactor_controller.in"), RightX, RowOneY, ButtonWidth, selectedRods());
        label(Graphics, Component.translatable("gui.scp_unleashed.reactor_controller.all_out"), LeftX, RowTwoY, ButtonWidth, anyRods());
        label(Graphics, Component.translatable("gui.scp_unleashed.reactor_controller.all_in"), RightX, RowTwoY, ButtonWidth, anyRods());
        Component Scram = Component.translatable("gui.scp_unleashed.reactor_controller.scram");
        boolean ScramReady = linked() && anyRods();
        Graphics.drawString(font, Scram, ScramX + (ScramWidth - font.width(Scram)) / 2, ScramY + 6 + (ScramReady ? 0 : 1), ScramReady ? ButtonText : ButtonDisabledText, ScramReady);
        for (TowerType Type : TowerType.values()) {
            int Index = Type.ordinal();
            Component Name = Component.translatable("gui.scp_unleashed.reactor_controller.legend." + Type.getName());
            Graphics.drawString(font, Name, LegendX + (Index % 2) * LegendColumn + SmallSize + 3, LegendY + (Index / 2) * LegendRow, 0xFF404040, false);
        }
        if (!linked()) {
            Component Waiting = Component.translatable(Received ? "gui.scp_unleashed.reactor_controller.no_link" : "gui.scp_unleashed.reactor_controller.connecting");
            for (int Row = 0; Row < 3; Row++) {
                Component Line = Row == 0 ? Waiting : Component.translatable("gui.scp_unleashed.reactor_controller.no_link." + Row);
                screenText(Graphics, Line, MapX + 3, MapY + 4 + Row * DetailLine, MapSize - 6, Row == 0 ? ScreenAmber : ScreenDim);
            }
        } else {
            screenText(Graphics, Component.translatable("gui.scp_unleashed.reactor_controller.north"), MapX + 2, MapY + 2, 10, ScreenDim);
        }
        List<Line> Detail = detailLines();
        for (int Index = 0; Index < Detail.size() && Index < DetailLines; Index++) {
            screenText(Graphics, Detail.get(Index).Text(), DetailX + 3, DetailY + 3 + Index * DetailLine, DetailWidth - 6, Detail.get(Index).Colour());
        }
        List<Line> Summary = summaryLines();
        for (int Index = 0; Index < Summary.size(); Index++) {
            screenText(Graphics, Summary.get(Index).Text(), SummaryX + 3, SummaryY + 3 + Index * SummaryLine, SummaryWidth - 6, Summary.get(Index).Colour());
        }
    }

    private void screenText(GuiGraphics Graphics, Component Text, int X, int Y, int Width, int Colour) {
        String Plain = Text.getString();
        if (font.width(Plain) > Width) {
            Plain = font.plainSubstrByWidth(Plain, Width - font.width("..")) + "..";
        }
        Graphics.drawString(font, Plain, X, Y, Colour, false);
    }

    private void label(GuiGraphics Graphics, Component Text, int X, int Y, int Width, boolean Enabled) {
        int Sink = Enabled ? 0 : 1;
        Graphics.drawString(font, Text, X + (Width - font.width(Text)) / 2, Y + 3 + Sink, Enabled ? ButtonText : ButtonDisabledText, Enabled);
    }

    private static String amount(float Value) {
        if (Value >= 10000.0f) {
            return String.format(Locale.ROOT, "%.0fk", Value / 1000.0f);
        }
        if (Value >= 1000.0f) {
            return String.format(Locale.ROOT, "%.1fk", Value / 1000.0f);
        }
        return Value >= 100.0f ? String.format(Locale.ROOT, "%.0f", Value) : String.format(Locale.ROOT, "%.1f", Value);
    }

    private List<Line> detailLines() {
        List<Line> Lines = new ArrayList<>();
        if (!linked()) {
            Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.howto.1"), ScreenGreen));
            Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.howto.2"), ScreenDim));
            Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.howto.3"), ScreenDim));
            Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.howto.4"), ScreenDim));
            return Lines;
        }
        TowerView View = selected();
        if (View == null) {
            int[] Counts = new int[TowerType.values().length];
            for (TowerView Entry : Views) {
                Counts[Entry.Type().ordinal()]++;
            }
            Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.select"), ScreenGreen));
            for (TowerType Type : TowerType.values()) {
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.count." + Type.getName(), Counts[Type.ordinal()]), ScreenDim));
            }
            return Lines;
        }
        CompoundTag Info = View.Info();
        Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.name", Component.translatable("gui.scp_unleashed.reactor_controller.short." + View.Type().getName()), View.Height()), ScreenGreen));
        switch (View.Type()) {
            case Fuel -> {
                int Temperature = Info.getShort("Temp");
                int Integrity = Info.getByte("Int");
                TowerHeat Heat = TowerHeat.of(Temperature, Integrity);
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.heat", Temperature, Component.translatable(Heat.getShortKey())), Heat.getScreenColour()));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.integrity", Integrity), Integrity < 100 ? ScreenAmber : ScreenDim));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.rods", Info.getByte("Rods"), View.Height(), Info.getByte("Left")),
                        Info.getByte("Rods") > 0 ? ScreenGreen : ScreenAmber));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.reaction", Info.getByte("Out"), amount(Info.getFloat("Made"))),
                        Info.getByte("Feeds") > 0 ? ScreenGreen : ScreenAmber));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.cooling", amount(Info.getFloat("Cool"))), Info.getByte("Coolers") > 0 ? ScreenCyan : ScreenAmber));
                String Gas = Info.getBoolean("Leak") ? "leaking" : Info.getBoolean("Vent") ? "vented" : "sealed";
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.gas." + Gas, Info.getByte("Gas")),
                        Info.getBoolean("Leak") ? ScreenRed : Info.getBoolean("Vent") ? ScreenDim : ScreenAmber));
                if (Info.getIntArray("Links").length > 0) {
                    int Boost = Info.getByte("Boost");
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.boost", Boost), Boost > 0 ? ScreenTeal : ScreenDim));
                }
            }
            case Rods -> {
                int Insertion = Info.getByte("Ins");
                int Target = Info.getByte("Tgt");
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.inserted", Insertion), ScreenGreen));
                Lines.add(new Line(Component.translatable(Info.getBoolean("Moving") ? "gui.scp_unleashed.reactor_controller.moving" : "gui.scp_unleashed.reactor_controller.target", Target), ScreenDim));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.controls", Info.getByte("Feeds")), Info.getByte("Feeds") > 0 ? ScreenDim : ScreenAmber));
                if (Info.getByte("Boosters") > 0) {
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.gates", Info.getByte("Boosters")), ScreenTeal));
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.passes", Info.getByte("Pass")), Info.getByte("Pass") > 0 ? ScreenTeal : ScreenDim));
                }
                if (Info.getBoolean("Forced")) {
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.forced"), ScreenRed));
                }
                if (Data.getBoolean("Scram")) {
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.scram_held"), ScreenRed));
                }
            }
            case Booster -> {
                int Rods = Info.getByte("Rods");
                int Gate = Info.getByte("Gate");
                int Reach = Info.getByte("Reach");
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.strength", Info.getByte("Str")), ScreenGreen));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.gated", Rods), Rods > 0 ? ScreenDim : ScreenAmber));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.passing", Gate), Gate > 0 ? ScreenTeal : ScreenDim));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.reaches", Reach), Reach > 0 ? ScreenDim : ScreenAmber));
                String Status = Rods == 0 ? "unlinked" : Reach == 0 ? "no_fuel" : Info.getBoolean("Live") ? "live" : Gate > 0 ? "idle" : "blocked";
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.booster." + Status),
                        Status.equals("live") ? ScreenTeal : Status.equals("blocked") || Status.equals("idle") ? ScreenDim : ScreenAmber));
            }
            default -> {
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.water", amount(Info.getInt("Water")), amount(Info.getInt("Cap"))),
                        Info.getBoolean("Dry") ? ScreenAmber : ScreenCyan));
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.steam", amount(Info.getFloat("Made"))), ScreenGreen));
                Lines.add(new Line(Component.translatable(View.Type() == TowerType.Coolant ? "gui.scp_unleashed.reactor_controller.cools" : "gui.scp_unleashed.reactor_controller.feeds",
                        Info.getByte("Feeds")), Info.getByte("Feeds") > 0 ? ScreenDim : ScreenAmber));
                String Outlet = Info.getBoolean("Blow") ? "blowing" : Info.getBoolean("Vent") ? "vented" : "open";
                Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.outlet." + Outlet), Info.getBoolean("Blow") ? ScreenAmber : ScreenDim));
                if (Info.getBoolean("Dry")) {
                    Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.dry"), ScreenRed));
                }
            }
        }
        return Lines;
    }

    private List<Line> summaryLines() {
        List<Line> Lines = new ArrayList<>();
        if (!linked()) {
            return Lines;
        }
        float Hottest = Data.getFloat("Hottest");
        TowerHeat Band = TowerHeat.of(Hottest, Data.getFloat("Weakest"));
        Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.total_steam", amount(Data.getFloat("Steam"))), ScreenGreen));
        Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.total_heat", amount(Data.getFloat("Heat"))), ScreenDim));
        Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.hottest", Math.round(Hottest)), Band.getScreenColour()));
        int Peak = -1;
        for (TowerView View : Views) {
            if (View.Type() == TowerType.Booster) {
                Peak = Math.max(Peak, 0);
            } else if (View.Type() == TowerType.Fuel) {
                Peak = Math.max(Peak, View.Info().getIntArray("Links").length > 0 ? View.Info().getByte("Boost") : -1);
            }
        }
        if (Peak >= 0) {
            Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.total_boost", Peak), Peak > 0 ? ScreenTeal : ScreenDim));
        }
        GridState State = state();
        if (State != GridState.Ready) {
            Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.state." + State.name().toLowerCase(Locale.ROOT)), ScreenAmber));
        } else if (Data.getBoolean("Scram")) {
            Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.scram_held"), ScreenRed));
        } else {
            Lines.add(new Line(Component.translatable("gui.scp_unleashed.reactor_controller.towers", Views.size()), ScreenDim));
        }
        return Lines;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        List<Component> Lines = tooltipLines(MouseX, MouseY);
        if (!Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private List<Component> tooltipLines(int MouseX, int MouseY) {
        List<Component> Lines = new ArrayList<>();
        int Hovered = hoveredColumn(MouseX, MouseY);
        if (Hovered >= 0) {
            TowerView View = Views.get(Hovered);
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.name", Component.translatable("block.scp_unleashed." + View.Type().getName()), View.Height()));
            if (View.Type() == TowerType.Fuel) {
                TowerHeat Heat = TowerHeat.of(View.Info().getShort("Temp"), View.Info().getByte("Int"));
                Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.heat", View.Info().getShort("Temp"), Component.translatable(Heat.getTranslationKey())).withStyle(Heat.getColour()));
                if (View.Info().getByte("Boost") > 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.boost_hint", View.Info().getByte("Boost")).withStyle(ChatFormatting.DARK_AQUA));
                }
            } else if (View.Type() == TowerType.Booster) {
                Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.passing_hint", View.Info().getByte("Gate"), View.Info().getByte("Str"))
                        .withStyle(View.Info().getByte("Gate") > 0 ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY));
            }
            int Stacked = columns().getOrDefault(((long) View.X() << 32) ^ (View.Z() & 0xFFFFFFFFL), List.of()).size();
            if (Stacked > 1) {
                Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.stacked", Stacked).withStyle(ChatFormatting.GRAY));
            }
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.click_select").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(BarX, BarY - 1, BarWidth, BarHeight + 2, MouseX, MouseY) && linked() && anyRods()) {
            boolean Single = selectedRods();
            float Insertion = Single ? selected().Info().getByte("Ins") : averageInsertion();
            float Target = Single ? selected().Info().getByte("Tgt") : averageTarget();
            Lines.add(Component.translatable(Single ? "gui.scp_unleashed.reactor_controller.bar_one" : "gui.scp_unleashed.reactor_controller.bar_all", Math.round(Insertion), Math.round(Target)));
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.bar_hint").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(LeftX, RowOneY, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.out_hint"));
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.fine_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(RightX, RowOneY, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.in_hint"));
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.fine_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(LeftX, RowTwoY, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.all_out_hint"));
        } else if (isHovering(RightX, RowTwoY, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.all_in_hint"));
        } else if (isHovering(ScramX, ScramY, ScramWidth, ScramHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.scram_hint"));
            Lines.add(Component.translatable("gui.scp_unleashed.reactor_controller.redstone_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(LampX, LampY, LampSize, LampSize, MouseX, MouseY) && linked()) {
            Lines.add(Component.translatable(state().getTranslationKey()).withStyle(state().getColour()));
        }
        return Lines;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT && linked()) {
            int Amount = Screen.hasShiftDown() ? FineStep : Step;
            TowerView View = selected();
            if (isHovering(LeftX, RowOneY, ButtonWidth, ButtonHeight, MouseX, MouseY) && selectedRods()) {
                send(ButtonOut, ReactorControl.SetTarget, View, View.Info().getByte("Tgt") - Amount);
                return true;
            }
            if (isHovering(RightX, RowOneY, ButtonWidth, ButtonHeight, MouseX, MouseY) && selectedRods()) {
                send(ButtonIn, ReactorControl.SetTarget, View, View.Info().getByte("Tgt") + Amount);
                return true;
            }
            if (isHovering(LeftX, RowTwoY, ButtonWidth, ButtonHeight, MouseX, MouseY) && anyRods()) {
                send(ButtonAllOut, ReactorControl.SetAll, null, Math.round(averageTarget()) - Amount);
                return true;
            }
            if (isHovering(RightX, RowTwoY, ButtonWidth, ButtonHeight, MouseX, MouseY) && anyRods()) {
                send(ButtonAllIn, ReactorControl.SetAll, null, Math.round(averageTarget()) + Amount);
                return true;
            }
            if (isHovering(ScramX, ScramY, ScramWidth, ScramHeight, MouseX, MouseY) && anyRods()) {
                send(ButtonScram, ReactorControl.Scram, null, 100);
                return true;
            }
            int Hovered = hoveredColumn((int) MouseX, (int) MouseY);
            if (Hovered >= 0) {
                select(Hovered);
                return true;
            }
            if (isHovering(MapX, MapY, MapSize, MapSize, MouseX, MouseY)) {
                Selected = -1;
                HasSelection = false;
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    private void select(int Hovered) {
        TowerView View = Views.get(Hovered);
        List<Integer> Column = columns().getOrDefault(((long) View.X() << 32) ^ (View.Z() & 0xFFFFFFFFL), List.of(Hovered));
        int Next = Hovered;
        if (Column.contains(Selected) && Column.size() > 1) {
            Next = Column.get((Column.indexOf(Selected) + 1) % Column.size());
        }
        Selected = Next;
        TowerView Chosen = Views.get(Next);
        SelectedX = Chosen.X();
        SelectedY = Chosen.Y();
        SelectedZ = Chosen.Z();
        HasSelection = true;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PanelPress.get(), 1.4f, 0.2f));
        }
    }

    private void send(int Id, int Action, TowerView View, int Value) {
        int Clamped = Mth.clamp(Value, 0, 100);
        int Dx = View == null ? 0 : View.X();
        int Dy = View == null ? 0 : View.Y();
        int Dz = View == null ? 0 : View.Z();
        if (!ReactorControl.fits(Dx, Dy, Dz)) {
            return;
        }
        Pressed = Id;
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PanelPress.get(), Id == ButtonScram ? 0.7f : 1.0f, 0.35f));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ReactorControl.encode(Action, Dx, Dy, Dz, Clamped));
        }
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Pressed >= 0 && minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PanelRelease.get(), 1.0f, 0.3f));
        }
        Pressed = -1;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    private record Line(Component Text, int Colour) {
    }
}
