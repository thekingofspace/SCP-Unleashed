package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp4666.Scp4666Objectives;
import net.rai.scp_unleashed.scp4666.Scp4666State;

import java.util.ArrayList;
import java.util.List;

public class Scp4666ChoreListScreen extends Screen {
    public static final String Prefix = "objective.scp_unleashed.scp4666.";
    public static final String Guide = "guide.scp_unleashed.scp4666.";
    public static final List<String> Chores = List.of("firewood", "hearth", "coal", "windows", "gift", "bread", "kettle", "calendar", "gear", "bed");
    public static final int MaxWidth = 280;
    public static final int Edge = 12;
    public static final int Inset = 10;
    public static final int Row = 10;
    public static final int Indent = 12;
    public static final int SubIndent = 16;
    public static final int Step = 12;
    public static final int Dim = 0x70080A0E;
    public static final int Paper = 0xFFE7DAB9;
    public static final int PaperEdge = 0xFFD4C39C;
    public static final int Rule = 0x2E4E6C8C;
    public static final int Ink = 0xFF3A2A1E;
    public static final int InkSoft = 0xFF6E5A45;
    public static final int InkDone = 0xFF8C7A63;
    public static final int Red = 0xFF8A2A1F;
    public static final int Check = 0xFF3F6E3B;
    public static final int Pending = 0xFF6A5338;
    public static final int Ember = 0xFFB33A1E;
    public static final int Spark = 0xFFE89A42;
    public static final int Pin = 0xFFB4342A;
    public static final int PinShine = 0xFFE8A08A;
    public static final String[] DoneIcon = {".......", "......#", ".....#.", "#...#..", ".#.#...", "..#....", "......."};
    public static final String[] PendingIcon = {"...#...", "..#.#..", ".#...#.", "#.....#", ".#...#.", "..#.#..", "...#..."};
    public static final String[] UrgentIcon = {"...#...", "..#H#..", ".##H##.", "###H###", ".#####.", "..#H#..", "...#..."};
    private int Scroll;
    private int Content;

    public record Mark(int Status) {
    }

    public record Line(FormattedCharSequence Text, int X, int Colour, Mark Icon) {
    }

    public Scp4666ChoreListScreen() {
        super(Component.translatable("block.scp_unleashed.scp4666_chore_list"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        Graphics.fill(0, 0, width, height, Dim);
    }

    public static String key(Scp4666Objectives.Line Line) {
        boolean Done = Line.Status() == Scp4666Objectives.Done;
        return switch (Line.Kind()) {
            case Freezing -> "freezing";
            case Gear -> "gear";
            case Hearth -> Line.Value() <= 0 ? "hearth.out" : Line.Value() == 1 ? "hearth.embers" : Line.Value() == 2 ? "hearth.low" : "hearth.burning";
            case Firewood -> "firewood";
            case Boiler -> Done ? "boiler.hot" : "boiler";
            case Windows -> "windows";
            case Sleep -> Line.Value() > 0 ? "sleep.last" : "sleep";
            case Bread -> Done ? "bread.done" : "bread";
            case Gift -> Done ? "gift.done" : "gift";
            case Calendar -> Done ? "calendar.done" : "calendar";
        };
    }

    public static String sub(Scp4666Objectives.Line Line) {
        if (Line.Status() == Scp4666Objectives.Done) {
            return null;
        }
        return switch (Line.Kind()) {
            case Boiler -> Line.Step() == 1 ? "boiler.carry" : Line.Value() <= 0 ? "boiler.cold" : "boiler.cooling";
            case Windows -> Line.Step() == 1 ? "windows.nail" : "windows.saw";
            case Bread -> switch (Line.Step()) {
                case 1 -> "bread.oats";
                case 2 -> "bread.baking";
                case 3 -> "bread.ready";
                default -> "bread.where";
            };
            case Gift -> Line.Step() == 1 ? "gift.carry" : "gift.where";
            default -> null;
        };
    }

    public static List<Line> lines(Font Text, int Width, boolean Winter, int Night, List<Scp4666Objectives.Line> Tasks) {
        List<Line> Found = new ArrayList<>();
        if (Winter) {
            add(Found, Text, Component.translatable(Prefix + "tonight"), Width, 0, Red, null);
            add(Found, Text, Component.translatable(Prefix + "night", Night, Scp4666State.FinalNight), Width, 0, Ink, null);
            add(Found, Text, Component.translatable(Prefix + "goal"), Width, 0, InkSoft, null);
            Found.add(new Line(FormattedCharSequence.EMPTY, 0, Ink, null));
            for (Scp4666Objectives.Line Task : Tasks) {
                Component Main = Task.Kind() == Scp4666Objectives.Task.Windows ? Component.translatable(Prefix + key(Task), Task.Value()) : Component.translatable(Prefix + key(Task));
                int Colour = Task.Status() == Scp4666Objectives.Done ? InkDone : Task.Status() == Scp4666Objectives.Urgent ? Red : Ink;
                add(Found, Text, Main, Width - Indent, Indent, Colour, new Mark(Task.Status()));
                String Sub = sub(Task);
                if (Sub != null) {
                    add(Found, Text, Component.translatable(Prefix + Sub), Width - SubIndent, SubIndent, InkSoft, null);
                }
            }
            Found.add(new Line(FormattedCharSequence.EMPTY, 0, Ink, null));
        }
        add(Found, Text, Component.translatable(Guide + "heading"), Width, 0, Red, null);
        add(Found, Text, Component.translatable(Guide + "intro"), Width, 0, InkSoft, null);
        for (String Chore : Chores) {
            Found.add(new Line(FormattedCharSequence.EMPTY, 0, Ink, null));
            add(Found, Text, Component.translatable(Guide + Chore + ".title"), Width, 0, Ink, null);
            add(Found, Text, Component.translatable(Guide + Chore + ".text"), Width - 4, 4, InkSoft, null);
        }
        Found.add(new Line(FormattedCharSequence.EMPTY, 0, Ink, null));
        add(Found, Text, Component.translatable(Prefix + "footer"), Width, 0, Red, null);
        return Found;
    }

    private static void add(List<Line> Found, Font Text, Component Words, int Width, int X, int Colour, Mark Icon) {
        List<FormattedCharSequence> Parts = Text.split(Words, Math.max(40, Width));
        for (int Index = 0; Index < Parts.size(); Index++) {
            Found.add(new Line(Parts.get(Index), X, Colour, Index == 0 ? Icon : null));
        }
    }

    public static int pageWidth(int ScreenWidth) {
        return Math.min(MaxWidth, ScreenWidth - 32);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        Minecraft Client = Minecraft.getInstance();
        boolean Winter = Scp4666ClientState.inside(Client) && Scp4666ClientState.started();
        List<Scp4666Objectives.Line> Tasks = new ArrayList<>();
        for (int Packed : Scp4666ClientState.tasks()) {
            Tasks.add(Scp4666Objectives.Line.unpack(Packed));
        }
        Scroll = draw(Graphics, font, width, height, Winter, Scp4666ClientState.night(), Tasks, Scroll, Scp4666ClientState.ticks() + Partial);
    }

    public int draw(GuiGraphics Graphics, Font Text, int ScreenWidth, int ScreenHeight, boolean Winter, int Night, List<Scp4666Objectives.Line> Tasks, int Offset, float Time) {
        int Width = pageWidth(ScreenWidth);
        int Height = ScreenHeight - Edge * 2;
        int X = (ScreenWidth - Width) / 2;
        int Y = Edge;
        page(Graphics, X, Y, Width, Height);
        List<Line> Rows = lines(Text, Width - Inset * 2, Winter, Night, Tasks);
        int View = Height - Inset * 2 - 6;
        Content = Rows.size() * Row;
        int Limit = Math.max(0, Content - View);
        int Shift = Mth.clamp(Offset, 0, Limit);
        int Top = Y + Inset + 4;
        Graphics.enableScissor(X + 2, Top, X + Width - 2, Top + View);
        for (int Index = 0; Index < Rows.size(); Index++) {
            int Line0 = Top + Index * Row - Shift;
            if (Line0 < Top - Row || Line0 > Top + View) {
                continue;
            }
            Graphics.fill(X + Inset, Line0 + Row - 1, X + Width - Inset, Line0 + Row, Rule);
            Line Each = Rows.get(Index);
            if (Each.Icon() != null) {
                int Status = Each.Icon().Status();
                String[] Pattern = Status == Scp4666Objectives.Done ? DoneIcon : Status == Scp4666Objectives.Urgent ? UrgentIcon : PendingIcon;
                int Colour = Status == Scp4666Objectives.Done ? Check : Status == Scp4666Objectives.Urgent ? Ember : Pending;
                icon(Graphics, Pattern, X + Inset, Line0 + 1, Colour, Spark, Status == Scp4666Objectives.Urgent ? pulse(Time) : 1.0f);
            }
            Graphics.drawString(Text, Each.Text(), X + Inset + Each.X(), Line0 + 1, Each.Colour(), false);
        }
        Graphics.disableScissor();
        if (Limit > 0) {
            int Mid = X + Width / 2;
            if (Shift < Limit) {
                chevron(Graphics, Mid, Y + Height - Inset + 1, true);
            }
            if (Shift > 0) {
                chevron(Graphics, Mid, Y + Inset - 3, false);
            }
        }
        return Shift;
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        Scroll = Math.max(0, Scroll - (int) Math.round(ScrollY * Step));
        return true;
    }

    @Override
    public boolean keyPressed(int Key, int Scan, int Modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(Key, Scan)) {
            onClose();
            return true;
        }
        if (Key == 264 || Key == 267) {
            Scroll += Key == 267 ? Step * 8 : Step;
            return true;
        }
        if (Key == 265 || Key == 266) {
            Scroll = Math.max(0, Scroll - (Key == 266 ? Step * 8 : Step));
            return true;
        }
        return super.keyPressed(Key, Scan, Modifiers);
    }

    public static float pulse(float Time) {
        return 0.55f + 0.45f * (0.5f + 0.5f * Mth.sin(Time * 0.3f));
    }

    private static void page(GuiGraphics Graphics, int X, int Y, int Width, int Height) {
        Graphics.fill(X + 2, Y, X + Width - 2, Y + 1, PaperEdge);
        Graphics.fill(X + 1, Y + 1, X + Width - 1, Y + 2, PaperEdge);
        Graphics.fill(X, Y + 2, X + Width, Y + Height - 2, PaperEdge);
        Graphics.fill(X + 1, Y + Height - 2, X + Width - 1, Y + Height - 1, PaperEdge);
        Graphics.fill(X + 2, Y + Height - 1, X + Width - 2, Y + Height, PaperEdge);
        Graphics.fill(X + 2, Y + 2, X + Width - 2, Y + Height - 2, Paper);
        for (int Fleck = 0; Fleck < Width * Height / 220; Fleck++) {
            int Fx = X + 3 + (Fleck * 37 + Fleck * Fleck * 13) % Math.max(1, Width - 6);
            int Fy = Y + 3 + (Fleck * 53 + Fleck * Fleck * 7) % Math.max(1, Height - 6);
            Graphics.fill(Fx, Fy, Fx + 1, Fy + 1, PaperEdge);
        }
        int Mid = X + Width / 2;
        Graphics.fill(Mid - 2, Y + 2, Mid + 2, Y + 6, Pin);
        Graphics.fill(Mid - 1, Y + 3, Mid, Y + 4, PinShine);
    }

    private static void chevron(GuiGraphics Graphics, int X, int Y, boolean Down) {
        for (int Step0 = 0; Step0 < 3; Step0++) {
            int Row0 = Down ? Y + Step0 : Y + 2 - Step0;
            Graphics.fill(X - 3 + Step0, Row0, X - 2 + Step0, Row0 + 1, InkSoft);
            Graphics.fill(X + 2 - Step0, Row0, X + 3 - Step0, Row0 + 1, InkSoft);
        }
    }

    private static void icon(GuiGraphics Graphics, String[] Pattern, int X, int Y, int Colour, int Highlight, float Alpha) {
        int Shade = Mth.clamp((int) (Alpha * 255.0f), 0, 255) << 24;
        for (int Row0 = 0; Row0 < Pattern.length; Row0++) {
            for (int Column = 0; Column < Pattern[Row0].length(); Column++) {
                char Mark0 = Pattern[Row0].charAt(Column);
                if (Mark0 == '.') {
                    continue;
                }
                int Base = Mark0 == 'H' ? Highlight : Colour;
                Graphics.fill(X + Column, Y + Row0, X + Column + 1, Y + Row0 + 1, Shade | Base & 0xFFFFFF);
            }
        }
    }
}
