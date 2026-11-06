package net.rai.scp_unleashed.terminal.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.rai.scp_unleashed.arrival.ArrivalCards;
import net.rai.scp_unleashed.scp2521.client.Scp2521ClientScene;
import net.rai.scp_unleashed.scp902.client.Scp902ClientScene;
import net.rai.scp_unleashed.terminal.TerminalArchive;
import net.rai.scp_unleashed.terminal.TerminalChunks;
import net.rai.scp_unleashed.terminal.TerminalDimensions;
import net.rai.scp_unleashed.terminal.TerminalEta;
import net.rai.scp_unleashed.terminal.TerminalGate;
import net.rai.scp_unleashed.terminal.TerminalHighlight;
import net.rai.scp_unleashed.terminal.TerminalLog;
import net.rai.scp_unleashed.terminal.TerminalPhase;
import net.rai.scp_unleashed.terminal.TerminalProgress;
import net.rai.scp_unleashed.terminal.TerminalTags;
import net.rai.scp_unleashed.terminal.TerminalText;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class TerminalLoading {
    public static final String Prefix = "terminal.scp_unleashed.";
    public static final long StaleMs = 2500L;
    public static final int TypeMs = 3;
    public static final int StampColumns = 9;
    public static final int MinTextColumns = 52;
    public static final int MinMapCell = 2;
    public static final int MaxMapCell = 5;
    public static final int MinLogW = 200;
    public static final int MaxReasonLines = 8;
    public static final int BarGap = 12;

    private static final class Session {
        final long Start;
        final boolean Dimension;
        final TerminalLog Log;
        final TerminalEta Eta = new TerminalEta();
        final Map<TerminalLog.Entry, String> Cache = new IdentityHashMap<>();
        long LastFrame;
        TerminalPhase LastPhase;
        TerminalLog.Entry TypedEntry;
        int TypedChars;
        boolean Warned;
        boolean Instant;

        Session(long Now, boolean NewDimension) {
            Start = Now;
            Dimension = NewDimension;
            Log = new TerminalLog(Now ^ System.nanoTime());
            LastFrame = Now;
        }
    }

    public record State(TerminalPhase Phase, float Fraction, int Count, String Subject, Component Info, String Raw, StoringChunkProgressListener Grid) {
    }

    private static Session Current;
    private static ResourceKey<Level> LastDimension;

    private TerminalLoading() {
    }

    public static void tick() {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null) {
            LastDimension = null;
            return;
        }
        if (Game.screen == null || TerminalGate.kindOf(Game.screen.getClass().getName()) == TerminalGate.Kind.None) {
            LastDimension = Game.level.dimension();
        }
    }

    private static boolean alive(long Now) {
        return Current != null && Now - Current.LastFrame < StaleMs;
    }

    public static boolean draw(Screen Shown, TerminalGate.Kind Which, GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        Minecraft Game = Minecraft.getInstance();
        long Now = Util.getMillis();
        boolean Alive = alive(Now);
        if (Which == TerminalGate.Kind.Disconnected) {
            Component Reason = TerminalAccess.disconnectReason((DisconnectedScreen) Shown);
            if (Reason == null) {
                TerminalClient.fallback(Shown, "disconnect reason unreadable");
                return false;
            }
            if (Current != null) {
                Current.LastFrame = Now;
            }
            drawDisconnected(Shown, Reason, Graphics, MouseX, MouseY, PartialTick, Now);
            return true;
        }
        boolean DimensionMode = false;
        if (Which == TerminalGate.Kind.Receiving) {
            ReceivingLevelScreen Receiving = (ReceivingLevelScreen) Shown;
            if (TerminalAccess.reason(Receiving) == null) {
                TerminalClient.fallback(Shown, "loading reason unreadable");
                return false;
            }
            ResourceKey<Level> To = Game.level == null ? null : Game.level.dimension();
            boolean InitialJoin = Alive && Current != null && !Current.Dimension || LastDimension == null;
            boolean Scene = Scp902ClientScene.cinematic() || Scp2521ClientScene.cinematic();
            if (TerminalGate.skipReceiving(InitialJoin, ArrivalCards.cutsceneDimension(LastDimension), ArrivalCards.cutsceneDimension(To), Scene)) {
                TerminalClient.fallback(Shown, "cutscene transition into " + (To == null ? "?" : To.location()));
                return false;
            }
            DimensionMode = !InitialJoin;
        }
        if (!Alive) {
            Current = new Session(Now, DimensionMode);
            if (!DimensionMode) {
                TerminalArchive.census().ifPresent(Count -> Current.Log.note(Now, TerminalTags.Ok, Prefix + "log.indexed", List.of(String.valueOf(Count.Total()))));
            }
        }
        State Data = state(Shown, Which, Game, Current.Dimension);
        if (Data == null) {
            TerminalClient.fallback(Shown, "screen state unreadable");
            return false;
        }
        boolean Blocking = Which == TerminalGate.Kind.Message || Which == TerminalGate.Kind.Progress;
        Current.LastFrame = Now;
        int Percent = Data.Fraction() >= 0.0f ? TerminalProgress.percent(Data.Fraction()) : -1;
        Current.Log.update(Now, Data.Phase(), Percent, Data.Count(), Data.Subject());
        if (Data.Raw() != null) {
            Current.Log.raw(Now, Data.Phase(), Data.Raw());
        }
        if (Blocking && !Current.Warned) {
            Current.Warned = true;
            Current.Log.note(Now, TerminalTags.Sys, Prefix + "log.wait", List.of());
        }
        if (Data.Phase() != Current.LastPhase) {
            Current.LastPhase = Data.Phase();
            Current.Eta.reset();
        }
        if (Data.Fraction() >= 0.0f) {
            Current.Eta.sample(Now, Data.Fraction());
        }
        Current.Instant = Blocking;
        paint(Shown, Data, Percent, Graphics, MouseX, MouseY, PartialTick, Now);
        TerminalSound.frame(Now);
        return true;
    }

    private static State state(Screen Shown, TerminalGate.Kind Which, Minecraft Game, boolean Dimension) {
        return switch (Which) {
            case Message -> new State(TerminalPhase.fromMessageKey(TerminalClient.key(Shown.getTitle())), -1.0f, -1, "", Shown.getTitle(), Shown.getTitle().getString(), null);
            case Progress -> progress((ProgressScreen) Shown, Game.level != null);
            case Connect -> connect((ConnectScreen) Shown);
            case LevelLoading -> levelLoading((LevelLoadingScreen) Shown);
            case Receiving -> receiving((ReceivingLevelScreen) Shown, Game, Dimension);
            default -> null;
        };
    }

    private static State progress(ProgressScreen Shown, boolean Leaving) {
        if (!TerminalAccess.progressReadable()) {
            return null;
        }
        Component Header = TerminalAccess.progressHeader(Shown);
        Component Stage = TerminalAccess.progressStage(Shown);
        int Value = TerminalAccess.progressValue(Shown);
        TerminalPhase Phase = Leaving || (Current != null && Current.Log.phase() == TerminalPhase.Save) ? TerminalPhase.Save : TerminalPhase.Prepare;
        Component Info = Stage != null ? Stage : Header;
        String Raw = Stage != null ? Stage.getString() : (Header != null ? Header.getString() : null);
        return new State(Phase, Value > 0 ? Value / 100.0f : -1.0f, -1, "", Info, Raw, null);
    }

    private static State connect(ConnectScreen Shown) {
        Component Status = TerminalAccess.connectStatus(Shown);
        if (Status == null) {
            return null;
        }
        String Key = TerminalClient.key(Status);
        TerminalPhase Phase = TerminalPhase.fromConnectStatus(Key);
        String Raw = Key.startsWith("connect.") ? null : Status.getString();
        return new State(Phase, Phase.Staged >= 0 ? Phase.Staged / 100.0f : -1.0f, -1, "", Status, Raw, null);
    }

    private static State levelLoading(LevelLoadingScreen Shown) {
        StoringChunkProgressListener Grid = TerminalAccess.listener(Shown);
        if (Grid == null) {
            return null;
        }
        int Percent = TerminalProgress.clamp(Grid.getProgress());
        int Diameter = Grid.getDiameter();
        int Full = 0;
        for (int Z = 0; Z < Diameter; Z++) {
            for (int X = 0; X < Diameter; X++) {
                if (Grid.getStatus(X, Z) == ChunkStatus.FULL) {
                    Full++;
                }
            }
        }
        int Total = TerminalProgress.readyTotal(Grid.getFullDiameter());
        Component Info = Component.translatable(Prefix + "info.sectors", TerminalProgress.ready(Full, Grid.getFullDiameter()), Total);
        return new State(TerminalPhase.Generate, Percent / 100.0f, Full, "", Info, null, Grid);
    }

    private static State receiving(ReceivingLevelScreen Shown, Minecraft Game, boolean Dimension) {
        boolean Ready = TerminalAccess.ready(Shown);
        float Fraction = 0.0f;
        int Count = -1;
        if (Game.level != null) {
            Count = Game.level.getChunkSource().getLoadedChunksCount();
            if (Game.player != null) {
                ChunkPos Centre = Game.player.chunkPosition();
                int Loaded = TerminalChunks.count(Centre.x, Centre.z, TerminalChunks.Radius, (X, Z) -> Game.level.getChunkSource().hasChunk(X, Z));
                boolean Own = Game.level.getChunkSource().hasChunk(Centre.x, Centre.z);
                Fraction = TerminalChunks.fraction(true, Loaded, TerminalChunks.total(TerminalChunks.Radius), Own);
            }
        }
        if (Ready) {
            Fraction = 1.0f;
        }
        String Who = Game.getUser().getName();
        if (Dimension) {
            String Name = Game.level == null ? "?" : TerminalDimensions.name(Game.level.dimension().location(), I18n::exists, I18n::get);
            TerminalPhase Phase = Ready ? TerminalPhase.Ready : TerminalPhase.Dimension;
            return new State(Phase, Fraction, Count, Ready ? Who : Name, Component.literal(Name), null, null);
        }
        TerminalPhase Phase = Ready ? TerminalPhase.Ready : TerminalPhase.Terrain;
        return new State(Phase, Fraction, Count, Who, Component.translatable("multiplayer.downloadingTerrain"), null, null);
    }

    private static String line(TerminalLog.Entry Entry) {
        if (Entry.Raw() != null) {
            return TerminalText.upper(Entry.Raw());
        }
        return Current.Cache.computeIfAbsent(Entry, Each -> TerminalText.upper(I18n.get(Each.Key(), Each.Args().toArray())));
    }

    private record LogRow(TerminalLog.Entry Entry, String Text, boolean First, int Start, boolean Last, boolean Recent) {
    }

    private static void paint(Screen Shown, State Data, int Percent, GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick, long Now) {
        Minecraft Game = Minecraft.getInstance();
        TerminalPaint Paint = new TerminalPaint(Graphics, Game.font, Shown.width, Shown.height, Now);
        int CharW = TerminalPaint.CharW;
        int BodyTop = Paint.Pad + TerminalPaint.HeaderGap;
        int BarY = Shown.height - Paint.Pad - 9;
        int PhaseY = BarY - 13;
        Layout Buttons = placeBar(Paint, Shown, PhaseY);
        ghost(Paint, Buttons, MouseX, MouseY, PartialTick);
        Paint.background();
        Paint.header(Component.translatable(Prefix + "header.title"), Component.translatable(Prefix + "header.session", TerminalClient.clock(Now - Current.Start)));
        int LogRight = Paint.right();
        if (Data.Grid() != null) {
            LogRight = drawMap(Paint, Data.Grid(), BodyTop, PhaseY, LogRight);
        }
        drawLog(Paint, BodyTop, PhaseY - 6, LogRight);
        Paint.glow(Component.translatable(Prefix + "phase.label", Component.translatable(Data.Phase().headerKey())), Paint.ContentX, PhaseY, Paint.Colours.Bright());
        drawPlaced(Paint, Buttons, MouseX, MouseY, PartialTick);
        int ButtonsLeft = Buttons.Left();
        if (Data.Info() != null) {
            int PhaseEnd = Paint.ContentX + Paint.width(Component.translatable(Prefix + "phase.label", Component.translatable(Data.Phase().headerKey()))) + 2 * CharW;
            int Room = Paint.columns(ButtonsLeft - 8 - PhaseEnd);
            String Info = TerminalPaint.fit(Data.Info().getString(), Room);
            if (!Info.isEmpty()) {
                Paint.text(Info, ButtonsLeft - 8 - Paint.width(Info), PhaseY, Paint.Colours.Dim());
            }
        }
        String Eta = I18n.get(Prefix + "eta", Data.Fraction() >= 0.0f ? Current.Eta.text() : "--");
        int EtaW = Paint.width(Eta);
        int Cells = Math.max(10, (Paint.ContentW - EtaW - BarGap) / CharW - 7);
        int X = Paint.text("[", Paint.ContentX, BarY, Paint.Colours.Main());
        String Inside = Percent >= 0 ? TerminalProgress.cells(Percent, Cells)
                : (Paint.Calm ? String.valueOf(TerminalProgress.Empty).repeat(Cells) : TerminalProgress.scanner(Now, Cells).substring(1, Cells + 1));
        int Split = Inside.indexOf(TerminalProgress.Empty);
        if (Percent < 0) {
            int Start = Paint.Calm ? Cells : TerminalProgress.scanStart(Now, Cells);
            String Before = Inside.substring(0, Math.min(Cells, Start));
            String Lit = Inside.substring(Math.min(Cells, Start), Math.min(Cells, Start + TerminalProgress.ScanWidth));
            X = Paint.text(Before, X, BarY, Paint.Colours.Dim());
            X = Paint.glow(Lit, X, BarY, Paint.Colours.Bright());
            X = Paint.text(Inside.substring(Math.min(Cells, Start + TerminalProgress.ScanWidth)), X, BarY, Paint.Colours.Dim());
        } else {
            int Filled = Split < 0 ? Cells : Split;
            X = Paint.glow(Inside.substring(0, Filled), X, BarY, Paint.Colours.Bright());
            X = Paint.text(Inside.substring(Filled), X, BarY, Paint.Colours.Dim());
        }
        X = Paint.text("] ", X, BarY, Paint.Colours.Main());
        Paint.glow(Percent >= 0 ? Percent + "%" : "--%", X, BarY, Paint.Colours.Bright());
        Paint.text(Eta, Paint.right() - EtaW, BarY, Data.Fraction() >= 0.0f && Current.Eta.known() ? Paint.Colours.Main() : Paint.Colours.Dim());
        Paint.crt();
    }

    private static int drawMap(TerminalPaint Paint, StoringChunkProgressListener Grid, int BodyTop, int PhaseY, int Right) {
        int Diameter = Math.max(1, Grid.getDiameter());
        int Available = PhaseY - 10 - BodyTop - 30;
        int Cell = Math.max(MinMapCell, Math.min(MaxMapCell, Available / Diameter - 1));
        int Size = Diameter * (Cell + 1) - 1;
        int Left = Right - Size - 14;
        if (Left - Paint.ContentX < MinLogW) {
            return Right;
        }
        Paint.panel(Left, BodyTop + 4, Right, BodyTop + 4 + Size + 26, Component.translatable(Prefix + "panel.map"));
        int GridX = Left + 7;
        int GridY = BodyTop + 14;
        int Full = 0;
        int Ready = ChunkStatus.FULL.getIndex();
        for (int Z = 0; Z < Diameter; Z++) {
            for (int X = 0; X < Diameter; X++) {
                ChunkStatus Status = Grid.getStatus(X, Z);
                int Colour;
                if (Status == null) {
                    Colour = Paint.Colours.Faint();
                } else if (Status == ChunkStatus.FULL) {
                    Colour = Paint.Colours.Bright();
                    Full++;
                } else {
                    Colour = Status.getIndex() * 2 >= Ready ? Paint.Colours.Main() : Paint.Colours.Dim();
                }
                int CellX = GridX + X * (Cell + 1);
                int CellY = GridY + Z * (Cell + 1);
                Paint.fill(CellX, CellY, CellX + Cell, CellY + Cell, Colour);
            }
        }
        Paint.clipped(I18n.get(Prefix + "info.online", TerminalProgress.ready(Full, Grid.getFullDiameter()), TerminalProgress.readyTotal(Grid.getFullDiameter())),
                GridX - 1, GridY + Size + 4, Paint.Colours.Dim(), Paint.columns(Size + 10));
        return Left - 12;
    }

    private static void drawLog(TerminalPaint Paint, int Top, int Bottom, int Right) {
        List<TerminalLog.Entry> Entries = Current.Log.entries();
        int Rows = Math.max(1, (Bottom - Top) / TerminalPaint.LineH);
        boolean Stamps = Paint.columns(Right - Paint.ContentX) - StampColumns - TerminalTags.Width - 1 >= MinTextColumns;
        int TextX = Paint.ContentX + (Stamps ? StampColumns * TerminalPaint.CharW : 0) + TerminalTags.textOffset(TerminalPaint.CharW);
        int TextColumns = Math.max(8, Paint.columns(Right - TextX));
        List<LogRow> Shown = new ArrayList<>();
        for (int Index = Entries.size() - 1; Index >= 0 && Shown.size() < Rows; Index--) {
            TerminalLog.Entry Entry = Entries.get(Index);
            List<String> Wrapped = TerminalText.wrap(line(Entry), TextColumns);
            boolean Last = Index == Entries.size() - 1;
            boolean Recent = Index >= Entries.size() - 3;
            List<LogRow> Block = new ArrayList<>();
            int Start = 0;
            for (int Row = 0; Row < Wrapped.size(); Row++) {
                Block.add(new LogRow(Entry, Wrapped.get(Row), Row == 0, Start, Last, Recent));
                Start += Wrapped.get(Row).length() + 1;
            }
            for (int Row = Block.size() - 1; Row >= 0 && Shown.size() < Rows; Row--) {
                Shown.add(0, Block.get(Row));
            }
        }
        int Typed = Integer.MAX_VALUE;
        if (!Entries.isEmpty() && !Paint.Calm && !Current.Instant) {
            TerminalLog.Entry Newest = Entries.get(Entries.size() - 1);
            Typed = (int) Math.min(line(Newest).length(), Math.max(0L, Paint.Now - Newest.At()) / TypeMs);
            if (Newest != Current.TypedEntry) {
                Current.TypedEntry = Newest;
                Current.TypedChars = 0;
            }
            TerminalSound.typed(Typed - Current.TypedChars, Paint.Now);
            Current.TypedChars = Typed;
        }
        int Y = Top + 2;
        int CursorX = -1;
        int CursorY = Y;
        for (LogRow Row : Shown) {
            if (Row.First()) {
                int X = Paint.ContentX;
                if (Stamps) {
                    Paint.text(TerminalClient.stamp(Row.Entry().At() - Current.Start), X, Y, Paint.Colours.Dim());
                    X += StampColumns * TerminalPaint.CharW;
                }
                Paint.tag(Row.Entry().label(), X, Y, Row.Recent() ? Paint.Colours.Bright() : Paint.Colours.Main());
            }
            String Text = Row.Text();
            if (Row.Last()) {
                int Visible = Math.max(0, Math.min(Text.length(), Typed - Row.Start()));
                if (Visible > 0 || Row.First()) {
                    int End = Paint.glow(Text.substring(0, Visible), TextX, Y, Paint.Colours.Main());
                    CursorX = Visible > 0 ? End + 1 : TextX;
                    CursorY = Y;
                }
            } else {
                Paint.text(Text, TextX, Y, Row.Recent() ? Paint.Colours.Main() : Paint.Colours.Dim());
            }
            Y += TerminalPaint.LineH;
        }
        if (CursorX >= 0) {
            Paint.cursor(CursorX, CursorY, Paint.Colours.Main());
        }
    }

    private record Placed(AbstractWidget Widget, Component Label, boolean Row) {
    }

    private record Layout(List<Placed> Restyled, List<Renderable> Natives, int Left) {
    }

    private static Layout placeBar(TerminalPaint Paint, Screen Shown, int Y) {
        int Left = Paint.right();
        List<Placed> Restyled = new ArrayList<>();
        List<Renderable> Natives = new ArrayList<>();
        for (Renderable Each : Shown.renderables) {
            if (!(Each instanceof AbstractWidget Widget)) {
                Natives.add(Each);
                continue;
            }
            if (!Widget.visible || Widget instanceof AbstractStringWidget) {
                continue;
            }
            if (Widget.getClass() != Button.class) {
                Natives.add(Widget);
                continue;
            }
            Component Label = Component.literal(TerminalClient.upper(Widget.getMessage()));
            int Span = Paint.width(Component.literal("[  ]").append(Label));
            Left -= Span;
            Widget.setX(Left - 1);
            Widget.setY(Y - 2);
            Widget.setWidth(Span + 2);
            Widget.setHeight(11);
            Restyled.add(new Placed(Widget, Label, false));
            Left -= 8;
        }
        return new Layout(Restyled, Natives, Left);
    }

    private static void ghost(TerminalPaint Paint, Layout Placement, int MouseX, int MouseY, float PartialTick) {
        for (Placed Each : Placement.Restyled()) {
            TerminalClient.ghost(Paint.Graphics, Each.Widget(), MouseX, MouseY, PartialTick);
        }
    }

    private static void drawPlaced(TerminalPaint Paint, Layout Placement, int MouseX, int MouseY, float PartialTick) {
        boolean MouseMode = TerminalClient.observe(MouseX, MouseY);
        boolean AnyHovered = false;
        boolean AnyFocused = false;
        for (Placed Each : Placement.Restyled()) {
            AbstractWidget Widget = Each.Widget();
            AnyHovered |= TerminalHighlight.within(MouseX, MouseY, Widget.getX(), Widget.getY(), Widget.getWidth(), Widget.getHeight());
            AnyFocused |= Widget.isFocused();
        }
        for (Placed Each : Placement.Restyled()) {
            AbstractWidget Widget = Each.Widget();
            boolean Hovered = TerminalHighlight.within(MouseX, MouseY, Widget.getX(), Widget.getY(), Widget.getWidth(), Widget.getHeight());
            boolean Lit = TerminalHighlight.lit(Hovered, Widget.isFocused(), AnyHovered, AnyFocused, MouseMode);
            if (Each.Row()) {
                Paint.row(Widget.getX(), Widget.getY(), Widget.getWidth(), Each.Label(), Lit, !Widget.active, null, 0);
            } else {
                Paint.bracket(Each.Label(), Widget.getX() + 1, Widget.getY() + 2, Lit);
            }
        }
        for (Renderable Each : Placement.Natives()) {
            Each.render(Paint.Graphics, MouseX, MouseY, PartialTick);
        }
    }

    private static void drawDisconnected(Screen Shown, Component Reason, GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick, long Now) {
        Minecraft Game = Minecraft.getInstance();
        TerminalPaint Paint = new TerminalPaint(Graphics, Game.font, Shown.width, Shown.height, Now);
        int Top = Paint.Pad + TerminalPaint.HeaderGap + 4;
        List<FormattedCharSequence> Lines = Paint.wrap(Reason, Paint.ContentW - 2 * TerminalPaint.CharW);
        int Count = Math.min(MaxReasonLines, Lines.size());
        int ButtonY = Top + TerminalPaint.LineH + 6 + Count * TerminalPaint.LineH + 10;
        int RowW = TerminalPaint.MinMenuW;
        for (Renderable Each : Shown.renderables) {
            if (Each instanceof AbstractWidget Widget && Widget.getClass() == Button.class && Widget.visible) {
                RowW = Math.max(RowW, Paint.width(TerminalClient.upper(Widget.getMessage())) + 4 * TerminalPaint.CharW + 6);
            }
        }
        RowW = Math.min(RowW, Paint.ContentW);
        List<Placed> Restyled = new ArrayList<>();
        List<Renderable> Natives = new ArrayList<>();
        int Y = ButtonY;
        for (Renderable Each : Shown.renderables) {
            if (!(Each instanceof AbstractWidget Widget)) {
                Natives.add(Each);
                continue;
            }
            if (!Widget.visible || Widget instanceof AbstractStringWidget) {
                continue;
            }
            if (Widget.getClass() != Button.class) {
                Natives.add(Widget);
                continue;
            }
            Widget.setX(Paint.ContentX);
            Widget.setY(Y);
            Widget.setWidth(RowW);
            Widget.setHeight(TerminalPaint.RowH - 1);
            Restyled.add(new Placed(Widget, Component.literal(TerminalClient.upper(Widget.getMessage())), true));
            Y += TerminalPaint.RowH;
        }
        Layout Placement = new Layout(Restyled, Natives, Paint.right());
        ghost(Paint, Placement, MouseX, MouseY, PartialTick);
        Paint.background();
        Paint.header(Component.translatable(Prefix + "header.title"), Component.translatable(Prefix + "header.link_lost"));
        int X = Paint.tag(TerminalTags.Fail, Paint.ContentX, Top, Paint.Colours.Warn());
        Paint.glow(TerminalClient.upper(Shown.getTitle()), X, Top, Paint.Colours.Warn());
        int LineY = Top + TerminalPaint.LineH + 6;
        for (int Index = 0; Index < Count; Index++) {
            Graphics.drawString(Game.font, Lines.get(Index), Paint.ContentX + 2 * TerminalPaint.CharW, LineY, Paint.Colours.Main(), false);
            LineY += TerminalPaint.LineH;
        }
        drawPlaced(Paint, Placement, MouseX, MouseY, PartialTick);
        Paint.crt();
        TerminalSound.frame(Now);
    }
}
