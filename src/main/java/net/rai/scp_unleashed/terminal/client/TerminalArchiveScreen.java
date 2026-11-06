package net.rai.scp_unleashed.terminal.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.archive.AnomalyDocumentItem;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.terminal.TerminalArchiveIndex;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TerminalArchiveScreen extends Screen {
    public static final String Prefix = "terminal.scp_unleashed.archive.";
    public static final int ListRowH = 11;
    public static final int DesignationColumns = 12;
    public static final int ClassColumns = 10;
    public static final int NewColumns = 5;
    public static final int ScrollW = 3;

    private record Hit(int X0, int Y0, int X1, int Y1, Runnable Action) {
        boolean contains(double X, double Y) {
            return X >= X0 && X < X1 && Y >= Y0 && Y < Y1;
        }
    }

    private final Screen Parent;
    private final List<Hit> Hits = new ArrayList<>();
    private final Map<DocumentClass, Integer> Counts = new EnumMap<>(DocumentClass.class);
    private List<TerminalArchiveIndex.Row> All = List.of();
    private List<TerminalArchiveIndex.Row> Shown = List.of();
    private DocumentClass Filter;
    private boolean NewOnly;
    private String Query = "";
    private int Scroll;
    private int Selected;
    private int Visible = 1;
    private int Fresh;

    public TerminalArchiveScreen(Screen NewParent) {
        super(Component.translatable(Prefix + "title"));
        Parent = NewParent;
    }

    @Override
    protected void init() {
        super.init();
        TitleArchive.install();
        if (All.isEmpty()) {
            List<TerminalArchiveIndex.Row> Rows = new ArrayList<>();
            for (Map.Entry<ResourceLocation, AnomalyDocument> Each : TitleArchive.documents().entrySet()) {
                AnomalyDocument Document = Each.getValue();
                Rows.add(new TerminalArchiveIndex.Row(Each.getKey(), Document.designation().getString(), Document.title().getString(), Document.Classification(),
                        TerminalClient.upper(Document.Classification().getName()), Document.isNew()));
                Counts.merge(Document.Classification(), 1, Integer::sum);
            }
            All = TerminalArchiveIndex.sorted(Rows);
            Fresh = TerminalArchiveIndex.fresh(All);
        }
        refilter();
    }

    private void refilter() {
        Shown = TerminalArchiveIndex.filter(All, Filter, NewOnly, Query);
        Selected = Math.max(0, Math.min(Selected, Shown.size() - 1));
        clampScroll();
    }

    private void clampScroll() {
        if (Selected < Scroll) {
            Scroll = Selected;
        }
        if (Selected >= Scroll + Visible) {
            Scroll = Selected - Visible + 1;
        }
        Scroll = Math.max(0, Math.min(Scroll, Math.max(0, Shown.size() - Visible)));
    }

    private void open(TerminalArchiveIndex.Row Each) {
        TerminalSound.typed(TerminalSound.CharsPerKey, Util.getMillis());
        minecraft.setScreen(new TitleDossierScreen(AnomalyDocumentItem.create(Each.Id(), Each.Classification(), new FieldNotes(List.of(), false, Optional.empty())), this));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        TitleArchive.release();
        minecraft.setScreen(Parent);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        long Now = Util.getMillis();
        TerminalPaint Paint = new TerminalPaint(Graphics, font, width, height, Now);
        TerminalSound.frame(Now);
        Hits.clear();
        Paint.background();
        Paint.header(Component.translatable(Prefix + "header"), Component.translatable(Prefix + "count", All.size(), Fresh));
        int CharW = TerminalPaint.CharW;
        int Y = Paint.Pad + TerminalPaint.HeaderGap;
        int X = Paint.ContentX;
        X += Paint.text(Component.translatable(Prefix + "class"), X, Y, Paint.Colours.Dim()) - X + CharW;
        List<DocumentClass> Options = new ArrayList<>();
        Options.add(null);
        for (DocumentClass Each : DocumentClass.values()) {
            if (Counts.getOrDefault(Each, 0) > 0) {
                Options.add(Each);
            }
        }
        Component NewLabel = Component.translatable(Prefix + "new_only");
        int NewW = Paint.width(Component.literal("[  ]").append(NewLabel));
        int FilterRight = Paint.right() - NewW - 2 * CharW;
        for (DocumentClass Each : Options) {
            Component Label = Each == null ? Component.translatable(Prefix + "all") : Component.literal(TerminalClient.upper(Each.getName()));
            int Span = Paint.width(Component.literal("[  ]").append(Label));
            if (X + Span > FilterRight) {
                X = Paint.ContentX;
                Y += TerminalPaint.LineH + 2;
            }
            boolean Active = Filter == Each;
            boolean Hover = MouseX >= X - 1 && MouseX < X + Span + 1 && MouseY >= Y - 2 && MouseY < Y + 9;
            DocumentClass Target = Each;
            if (Active) {
                Paint.fill(X - 1, Y - 2, X + Span + 1, Y + 9, Each == null ? Paint.Colours.Main() : Each.getColour());
                Paint.text(Component.literal("[ ").append(Label).append(" ]"), X, Y, Paint.Colours.Bg());
            } else {
                Paint.bracket(Label, X, Y, Hover);
            }
            Hits.add(new Hit(X - 1, Y - 2, X + Span + 1, Y + 9, () -> {
                Filter = Target;
                Selected = 0;
                Scroll = 0;
                refilter();
            }));
            X += Span + CharW;
        }
        int NewX = Paint.right() - NewW;
        int NewY = Paint.Pad + TerminalPaint.HeaderGap;
        boolean NewHover = MouseX >= NewX - 1 && MouseX < NewX + NewW + 1 && MouseY >= NewY - 2 && MouseY < NewY + 9;
        if (NewOnly) {
            Paint.fill(NewX - 1, NewY - 2, NewX + NewW + 1, NewY + 9, Paint.Colours.Bright());
            Paint.text(Component.literal("[ ").append(NewLabel).append(" ]"), NewX, NewY, Paint.Colours.Bg());
        } else {
            Paint.bracket(NewLabel, NewX, NewY, NewHover);
        }
        Hits.add(new Hit(NewX - 1, NewY - 2, NewX + NewW + 1, NewY + 9, () -> {
            NewOnly = !NewOnly;
            Selected = 0;
            Scroll = 0;
            refilter();
        }));
        Y += TerminalPaint.LineH + 6;
        int End = Paint.glow(Component.translatable(Prefix + "search"), Paint.ContentX, Y, Paint.Colours.Bright());
        End = Paint.text(Query, End + CharW, Y, Paint.Colours.Main());
        Paint.cursor(End + 1, Y, Paint.Colours.Main());
        Component Matches = Component.translatable(Prefix + "showing", Shown.size(), All.size());
        Paint.text(Matches, Paint.right() - Paint.width(Matches), Y, Paint.Colours.Dim());
        Y += TerminalPaint.LineH + 4;
        int ListRight = Paint.right() - ScrollW - 4;
        int TitleX = Paint.ContentX + 3 + DesignationColumns * CharW;
        int ClassX = ListRight - 3 - (ClassColumns + NewColumns) * CharW;
        int NewTagX = ListRight - 3 - (NewColumns - 1) * CharW;
        Paint.text(Component.translatable(Prefix + "column.designation"), Paint.ContentX + 3, Y, Paint.Colours.Dim());
        Paint.text(Component.translatable(Prefix + "column.title"), TitleX, Y, Paint.Colours.Dim());
        Paint.text(Component.translatable(Prefix + "column.class"), ClassX, Y, Paint.Colours.Dim());
        Paint.dashes(Paint.ContentX, Y + 10, ListRight - Paint.ContentX, Paint.Colours.Faint());
        int ListTop = Y + 13;
        int FooterY = height - Paint.Pad - 9;
        int ListBottom = FooterY - 6;
        Visible = Math.max(1, (ListBottom - ListTop) / ListRowH);
        clampScroll();
        int TitleColumns = Math.max(4, (ClassX - TitleX) / CharW - 1);
        boolean MouseMode = TerminalClient.observe(MouseX, MouseY);
        if (MouseMode && MouseX >= Paint.ContentX && MouseX < ListRight && MouseY >= ListTop) {
            int Hovered = Scroll + (MouseY - ListTop) / ListRowH;
            if (Hovered < Math.min(Shown.size(), Scroll + Visible)) {
                Selected = Hovered;
            }
        }
        for (int Index = Scroll; Index < Math.min(Shown.size(), Scroll + Visible); Index++) {
            TerminalArchiveIndex.Row Each = Shown.get(Index);
            int RowY = ListTop + (Index - Scroll) * ListRowH;
            boolean Lit = Index == Selected;
            int Ink = Paint.Colours.Main();
            if (Lit) {
                Paint.fill(Paint.ContentX, RowY, ListRight, RowY + ListRowH - 1, Paint.Colours.Main());
                Ink = Paint.Colours.Bg();
            }
            Paint.text(TerminalPaint.fit(Each.Designation(), DesignationColumns - 1), Paint.ContentX + 3, RowY + 2, Ink);
            Paint.text(TerminalPaint.fit(Each.Title(), TitleColumns), TitleX, RowY + 2, Lit ? Ink : Paint.Colours.Dim());
            Paint.text(TerminalPaint.fit(Each.ClassName(), ClassColumns - 1), ClassX, RowY + 2, Lit ? Ink : Each.Classification().getColour());
            if (Each.Fresh()) {
                Paint.inverse(Component.translatable(Prefix + "new").getString(), NewTagX, RowY + 2, Lit ? Paint.Colours.Bg() : Paint.Colours.Bright(),
                        Lit ? Paint.Colours.Bright() : Paint.Colours.Bg());
            }
            int Pick = Index;
            Hits.add(new Hit(Paint.ContentX, RowY, ListRight, RowY + ListRowH, () -> {
                Selected = Pick;
                open(Each);
            }));
        }
        if (Shown.isEmpty()) {
            Paint.text(Component.translatable(Prefix + "empty"), Paint.ContentX + 3, ListTop + 2, Paint.Colours.Dim());
        }
        int TrackX = Paint.right() - ScrollW;
        Paint.fill(TrackX, ListTop, TrackX + ScrollW, ListBottom, Paint.Colours.Faint());
        if (Shown.size() > Visible) {
            int Track = ListBottom - ListTop;
            int Thumb = Math.max(6, Track * Visible / Shown.size());
            int ThumbY = ListTop + (Track - Thumb) * Scroll / Math.max(1, Shown.size() - Visible);
            Paint.fill(TrackX, ThumbY, TrackX + ScrollW, ThumbY + Thumb, Paint.Colours.Main());
        }
        Component Back = Component.translatable(Prefix + "back");
        int BackW = Paint.width(Component.literal("[  ]").append(Back));
        boolean BackHover = MouseX >= Paint.ContentX - 1 && MouseX < Paint.ContentX + BackW + 1 && MouseY >= FooterY - 2 && MouseY < FooterY + 9;
        Paint.bracket(Back, Paint.ContentX, FooterY, BackHover);
        Hits.add(new Hit(Paint.ContentX - 1, FooterY - 2, Paint.ContentX + BackW + 1, FooterY + 9, this::onClose));
        Component Hint = Component.translatable(Prefix + "hint");
        Paint.clipped(Hint.getString(), Paint.right() - Math.min(Paint.width(Hint), Paint.ContentW - BackW - 3 * CharW), FooterY, Paint.Colours.Dim(),
                Paint.columns(Paint.ContentW - BackW - 3 * CharW));
        Paint.crt();
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0) {
            for (Hit Each : List.copyOf(Hits)) {
                if (Each.contains(MouseX, MouseY)) {
                    Each.Action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        Scroll = Math.max(0, Math.min(Math.max(0, Shown.size() - Visible), Scroll - (int) Math.signum(ScrollY) * 3));
        Selected = Math.max(Scroll, Math.min(Selected, Scroll + Visible - 1));
        return true;
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        switch (Key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (!Query.isEmpty()) {
                    Query = "";
                    refilter();
                } else {
                    onClose();
                }
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!Query.isEmpty()) {
                    Query = hasControlDown() ? "" : Query.substring(0, Query.length() - 1);
                    Selected = 0;
                    refilter();
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP -> move(-1);
            case GLFW.GLFW_KEY_DOWN -> move(1);
            case GLFW.GLFW_KEY_PAGE_UP -> move(-Visible);
            case GLFW.GLFW_KEY_PAGE_DOWN -> move(Visible);
            case GLFW.GLFW_KEY_HOME -> move(-Shown.size());
            case GLFW.GLFW_KEY_END -> move(Shown.size());
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (!Shown.isEmpty()) {
                    open(Shown.get(Selected));
                }
                return true;
            }
            default -> {
                return super.keyPressed(Key, ScanCode, Modifiers);
            }
        }
        return true;
    }

    private void move(int Delta) {
        if (Shown.isEmpty()) {
            return;
        }
        Selected = Math.max(0, Math.min(Shown.size() - 1, Selected + Delta));
        clampScroll();
    }

    @Override
    public boolean charTyped(char Typed, int Modifiers) {
        if (TerminalArchiveIndex.typable(Typed, Query)) {
            Query += Typed;
            Selected = 0;
            Scroll = 0;
            refilter();
            TerminalSound.typed(TerminalSound.CharsPerKey, Util.getMillis());
            return true;
        }
        return super.charTyped(Typed, Modifiers);
    }
}
