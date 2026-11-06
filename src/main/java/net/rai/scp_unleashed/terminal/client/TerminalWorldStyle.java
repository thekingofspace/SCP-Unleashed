package net.rai.scp_unleashed.terminal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.storage.LevelSummary;
import net.rai.scp_unleashed.terminal.TerminalHighlight;
import net.rai.scp_unleashed.terminal.TerminalTags;
import net.rai.scp_unleashed.terminal.TerminalText;
import net.rai.scp_unleashed.terminal.TerminalWorldTags;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TerminalWorldStyle {
    public static final String Prefix = "terminal.scp_unleashed.worlds.";
    public static final int IconSize = 32;
    public static final int CardInset = 38;
    public static final int ScrollW = 4;
    public static final int FieldH = 14;
    public static final int TabH = 14;
    public static final int PanelPad = 12;
    public static final int MinPanelW = 330;
    public static final int FooterRowGap = 13;
    public static final int Select = 0;
    public static final int Create = 1;
    public static final int Other = 2;
    public static final int SkinButton = 0;
    public static final int SkinSelector = 1;
    public static final int SkinSlider = 2;
    public static final int SkinField = 3;
    public static final int SkinCheck = 4;
    public static final int SkinLabel = 5;
    public static final int SkinFooter = 6;
    public static final int SkinTab = 7;
    public static final Map<String, String> SelectActions = Map.of(
            "selectWorld.select", "play", "selectWorld.create", "create", "selectWorld.edit", "edit",
            "selectWorld.delete", "delete", "selectWorld.recreate", "recreate", "gui.back", "back");
    public static final Map<String, String> CreateActions = Map.of("selectWorld.create", "confirm", "gui.cancel", "cancel");

    private static final Field EntrySummary = field(WorldSelectionList.WorldListEntry.class, "summary");
    private static final Field EntryIcon = field(WorldSelectionList.WorldListEntry.class, "icon");
    private static final Field EditHint = field(EditBox.class, "hint");
    private static final Field LabelAlign = field(StringWidget.class, "alignX");
    private static final Field ItemHeight = field(AbstractSelectionList.class, "itemHeight");
    private static final Field SliderValue = field(AbstractSliderButton.class, "value");
    private static final Field Tabs = field(CreateWorldScreen.class, "tabManager");
    private static final Method RowTop = method(AbstractSelectionList.class, "getRowTop", int.class);

    private record Skin(AbstractWidget Widget, int Type, Component Label) {
    }

    private record Box(int X0, int Y0, int X1, int Y1, Component Title) {
    }

    private TerminalWorldStyle() {
    }

    private static Field field(Class<?> Owner, String Name) {
        try {
            Field Found = Owner.getDeclaredField(Name);
            Found.setAccessible(true);
            return Found;
        } catch (ReflectiveOperationException | RuntimeException Failure) {
            return null;
        }
    }

    private static Method method(Class<?> Owner, String Name, Class<?>... Types) {
        try {
            Method Found = Owner.getDeclaredMethod(Name, Types);
            Found.setAccessible(true);
            return Found;
        } catch (ReflectiveOperationException | RuntimeException Failure) {
            return null;
        }
    }

    private static Object read(Field Source, Object Target) {
        try {
            return Source == null ? null : Source.get(Target);
        } catch (ReflectiveOperationException | RuntimeException Failure) {
            return null;
        }
    }

    public static int kind(Screen Shown) {
        if (Shown.getClass() == SelectWorldScreen.class) {
            return Select;
        }
        if (Shown.getClass() == CreateWorldScreen.class) {
            return Create;
        }
        return Other;
    }

    public static boolean skinned(AbstractWidget Widget) {
        if (Widget instanceof EditBox || Widget instanceof AbstractStringWidget || Widget instanceof Checkbox || Widget instanceof AbstractSliderButton) {
            return true;
        }
        return Widget instanceof AbstractButton && !(Widget instanceof SpriteIconButton) && !(Widget instanceof ImageButton) && !Widget.getMessage().getString().isBlank();
    }

    private static int skinOf(AbstractWidget Widget) {
        if (Widget instanceof EditBox) {
            return SkinField;
        }
        if (Widget instanceof AbstractStringWidget) {
            return SkinLabel;
        }
        if (Widget instanceof Checkbox) {
            return SkinCheck;
        }
        if (Widget instanceof AbstractSliderButton) {
            return SkinSlider;
        }
        if (Widget instanceof CycleButton<?>) {
            return SkinSelector;
        }
        return SkinButton;
    }

    private static Component[] split(Component Message) {
        if (Message.getContents() instanceof TranslatableContents Translated && "options.generic_value".equals(Translated.getKey()) && Translated.getArgs().length >= 2) {
            Object Name = Translated.getArgs()[0];
            Object Value = Translated.getArgs()[1];
            return new Component[]{Name instanceof Component Named ? Named : Component.literal(String.valueOf(Name)),
                    Value instanceof Component Valued ? Valued : Component.literal(String.valueOf(Value))};
        }
        return null;
    }

    public static boolean draw(Screen Shown, GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        Minecraft Game = Minecraft.getInstance();
        long Now = Util.getMillis();
        TerminalPaint Paint = new TerminalPaint(Graphics, Game.font, Shown.width, Shown.height, Now);
        int Which = kind(Shown);
        List<Renderable> Items = new ArrayList<>(Shown.renderables);
        for (GuiEventListener Child : Shown.children()) {
            if (Child instanceof EditBox Input && !Items.contains(Input)) {
                Items.add(Input);
            }
        }
        int HeaderBottom = Paint.Pad + 14;
        int FooterY = Shown.height - Paint.Pad - 9;
        WorldSelectionList Picker = null;
        TabNavigationBar Bar = null;
        for (Renderable Each : Items) {
            if (Each instanceof WorldSelectionList Found) {
                Picker = Found;
            } else if (Each instanceof TabNavigationBar Found) {
                Bar = Found;
            }
        }
        Map<AbstractWidget, Component> FooterLabels = new HashMap<>();
        List<AbstractWidget> Footer = new ArrayList<>();
        Map<String, String> Actions = Which == Select ? SelectActions : Which == Create ? CreateActions : Map.of();
        for (Renderable Each : Items) {
            if (Each instanceof AbstractWidget Widget && Widget.visible && Widget instanceof AbstractButton && !(Widget instanceof CycleButton<?>)) {
                String Action = Actions.get(TerminalClient.key(Widget.getMessage()));
                if (Action != null || (Which == Select && skinned(Widget) && !(Widget instanceof Checkbox))) {
                    Footer.add(Widget);
                    FooterLabels.put(Widget, Action != null ? Component.translatable(Prefix + "action." + Action) : Component.literal(TerminalClient.upper(Widget.getMessage())));
                }
            }
        }
        int FooterTop = layoutFooter(Paint, Footer, FooterLabels, FooterY);
        Box Panel = null;
        AbstractWidget Search = null;
        if (Which == Select && Picker != null) {
            int RowW = Picker.getRowWidth();
            int PanelX0 = Shown.width / 2 - RowW / 2 - PanelPad;
            int PanelX1 = Shown.width / 2 + RowW / 2 + PanelPad;
            int SearchY = HeaderBottom + 8;
            for (GuiEventListener Child : Shown.children()) {
                if (Child instanceof EditBox Input) {
                    Search = Input;
                    int FieldX = PanelX0 + 8 * TerminalPaint.CharW;
                    Input.setX(FieldX);
                    Input.setY(SearchY - 3);
                    Input.setWidth(PanelX1 - FieldX);
                    Input.setHeight(FieldH);
                    break;
                }
            }
            int PanelTop = SearchY + 20;
            int ListTop = PanelTop + 8;
            int ListBottom = FooterTop - 12;
            if (Picker.getY() != ListTop || Picker.getHeight() != ListBottom - ListTop || Picker.getWidth() != Shown.width) {
                Picker.updateSizeAndPosition(Shown.width, Math.max(36, ListBottom - ListTop), ListTop);
            }
            Panel = new Box(PanelX0, PanelTop, PanelX1, ListBottom + 6, Component.translatable(Prefix + "panel"));
        }
        if (Which == Create && Bar != null) {
            List<TabButton> TabButtons = new ArrayList<>();
            for (GuiEventListener Child : Bar.children()) {
                if (Child instanceof TabButton Tab) {
                    TabButtons.add(Tab);
                }
            }
            int Total = 0;
            for (TabButton Tab : TabButtons) {
                Total += Paint.width("[  ]" + TerminalClient.upper(Tab.getMessage())) + 2 + 2 * TerminalPaint.CharW;
            }
            int X = (Shown.width - Total) / 2;
            int TabY = HeaderBottom + 6;
            for (TabButton Tab : TabButtons) {
                int Span = Paint.width("[  ]" + TerminalClient.upper(Tab.getMessage())) + 2;
                Tab.setX(X);
                Tab.setY(TabY);
                Tab.setWidth(Span);
                Tab.setHeight(TabH);
                X += Span + 2 * TerminalPaint.CharW;
            }
            int AreaTop = TabY + TabH + 18;
            int AreaBottom = FooterTop - 12;
            if (read(Tabs, Shown) instanceof TabManager Manager && AreaBottom > AreaTop) {
                Manager.setTabArea(new ScreenRectangle(0, AreaTop, Shown.width, AreaBottom - AreaTop));
            }
        }
        List<Skin> Skins = new ArrayList<>();
        List<Renderable> Natives = new ArrayList<>();
        int MinX = Integer.MAX_VALUE;
        int MinY = Integer.MAX_VALUE;
        int MaxX = Integer.MIN_VALUE;
        int MaxY = Integer.MIN_VALUE;
        for (Renderable Each : Items) {
            if (Each == Picker || Each == Bar) {
                continue;
            }
            if (!(Each instanceof AbstractWidget Widget)) {
                Natives.add(Each);
                continue;
            }
            if (!Widget.visible) {
                continue;
            }
            if (FooterLabels.containsKey(Widget)) {
                Skins.add(new Skin(Widget, SkinFooter, FooterLabels.get(Widget)));
                continue;
            }
            if (skinned(Widget)) {
                Skins.add(new Skin(Widget, skinOf(Widget), Widget.getMessage()));
            } else {
                Natives.add(Widget);
            }
            if (Widget != Search) {
                MinX = Math.min(MinX, Widget.getX());
                MinY = Math.min(MinY, Widget.getY());
                MaxX = Math.max(MaxX, Widget.getX() + Widget.getWidth());
                MaxY = Math.max(MaxY, Widget.getY() + Widget.getHeight());
            }
        }
        if (Bar != null) {
            for (GuiEventListener Child : Bar.children()) {
                if (Child instanceof TabButton Tab) {
                    Skins.add(new Skin(Tab, SkinTab, Tab.getMessage()));
                }
            }
        }
        if (Which == Create && MinX != Integer.MAX_VALUE) {
            int Width = Math.max(MinPanelW, MaxX - MinX + 2 * PanelPad);
            int Centre = (MinX + MaxX) / 2;
            Component Section = Component.translatable(Prefix + "section", Component.literal(currentTab(Bar)));
            Panel = new Box(Centre - Width / 2, MinY - PanelPad, Centre + Width / 2, MaxY + 8, Section);
        }
        for (Skin Each : Skins) {
            TerminalClient.ghost(Graphics, Each.Widget(), MouseX, MouseY, PartialTick);
        }
        if (Picker != null) {
            TerminalClient.ghost(Graphics, Picker, MouseX, MouseY, PartialTick);
        }
        boolean MouseMode = TerminalClient.observe(MouseX, MouseY);
        boolean AnyHovered = false;
        boolean AnyFocused = false;
        for (Skin Each : Skins) {
            if (Each.Type() != SkinLabel) {
                AnyHovered |= hovered(Each.Widget(), MouseX, MouseY);
                AnyFocused |= Each.Widget().isFocused();
            }
        }
        Paint.background();
        Paint.header(Component.translatable(Prefix + "header." + (Which == Select ? "select" : Which == Create ? "create" : "other")),
                Component.translatable("terminal.scp_unleashed.header.uptime", TerminalClient.clock(Now - TerminalClient.Started)));
        if (Which == Other) {
            String Title = TerminalClient.upper(Shown.getTitle());
            int TitleY = Math.max(HeaderBottom + 4, 15);
            if (MinY == Integer.MAX_VALUE || TitleY + 10 <= MinY) {
                Paint.glow(Title, (Shown.width - Paint.width(Title)) / 2, TitleY, Paint.Colours.Bright());
            }
        }
        if (Panel != null) {
            Paint.panel(Panel.X0(), Panel.Y0(), Panel.X1(), Panel.Y1(), Panel.Title());
        }
        if (Search != null) {
            Paint.glow(Component.translatable(Prefix + "search"), Panel == null ? Paint.ContentX : Panel.X0(), Search.getY() + 3, Paint.Colours.Bright());
        }
        if (Bar != null) {
            Paint.dashes(Paint.ContentX, HeaderBottom + 6 + TabH + 2, Paint.ContentW, Paint.Colours.Faint());
        }
        AbstractWidget Tipped = null;
        for (Skin Each : Skins) {
            AbstractWidget Widget = Each.Widget();
            boolean Lit = Each.Type() != SkinLabel && TerminalHighlight.lit(hovered(Widget, MouseX, MouseY), Widget.isFocused(), AnyHovered, AnyFocused, MouseMode);
            switch (Each.Type()) {
                case SkinFooter -> Paint.bracket(Each.Label(), Widget.getX() + 1, Widget.getY() + 2, Lit && Widget.active);
                case SkinTab -> drawTab(Paint, (TabButton) Widget, Lit);
                case SkinField -> drawField(Paint, (EditBox) Widget, Lit);
                case SkinLabel -> drawLabel(Paint, (AbstractStringWidget) Widget, Skins);
                case SkinCheck -> drawCheck(Paint, (Checkbox) Widget, Lit);
                case SkinSlider -> drawSlider(Paint, (AbstractSliderButton) Widget, Lit);
                case SkinSelector -> drawSelector(Paint, Widget, Lit);
                default -> drawButton(Paint, Widget, Lit);
            }
            if (hovered(Widget, MouseX, MouseY) && Widget.getTooltip() != null) {
                Tipped = Widget;
            }
        }
        if (Picker != null && !drawWorlds(Paint, Picker, MouseX, MouseY, MouseMode)) {
            Picker.render(Graphics, MouseX, MouseY, PartialTick);
            TerminalClient.fallback(Shown, "world list fields unreadable");
        }
        for (Renderable Each : Natives) {
            Each.render(Graphics, MouseX, MouseY, PartialTick);
        }
        Paint.crt();
        if (Tipped != null) {
            Tooltip Tip = Tipped.getTooltip();
            Graphics.renderTooltip(Game.font, Tip.toCharSequence(Game), MouseX, MouseY);
        }
        TerminalSound.frame(Now);
        return true;
    }

    private static String currentTab(TabNavigationBar Bar) {
        if (Bar != null) {
            for (GuiEventListener Child : Bar.children()) {
                if (Child instanceof TabButton Tab && Tab.isSelected()) {
                    return TerminalClient.upper(Tab.getMessage());
                }
            }
        }
        return "";
    }

    private static int layoutFooter(TerminalPaint Paint, List<AbstractWidget> Footer, Map<AbstractWidget, Component> Labels, int FooterY) {
        if (Footer.isEmpty()) {
            return FooterY;
        }
        int Total = 0;
        for (AbstractWidget Widget : Footer) {
            Total += Paint.width(Component.literal("[  ]").append(Labels.get(Widget))) + 8;
        }
        boolean TwoRows = Total - 8 > Paint.ContentW;
        int X = Paint.ContentX + 1;
        int Y = TwoRows ? FooterY - FooterRowGap : FooterY;
        for (AbstractWidget Widget : Footer) {
            int Span = Paint.width(Component.literal("[  ]").append(Labels.get(Widget)));
            if (X + Span > Paint.right() && X > Paint.ContentX + 1) {
                X = Paint.ContentX + 1;
                Y += FooterRowGap;
            }
            Widget.setX(X - 1);
            Widget.setY(Y - 2);
            Widget.setWidth(Span + 2);
            Widget.setHeight(11);
            X += Span + 8;
        }
        return TwoRows ? FooterY - FooterRowGap : FooterY;
    }

    private static boolean hovered(AbstractWidget Widget, int MouseX, int MouseY) {
        return Widget.visible && TerminalHighlight.within(MouseX, MouseY, Widget.getX(), Widget.getY(), Widget.getWidth(), Widget.getHeight());
    }

    private static void frame(TerminalPaint Paint, int X0, int Y0, int X1, int Y1, int Colour) {
        Paint.fill(X0, Y0, X1, Y0 + 1, Colour);
        Paint.fill(X0, Y1 - 1, X1, Y1, Colour);
        Paint.fill(X0, Y0, X0 + 1, Y1, Colour);
        Paint.fill(X1 - 1, Y0, X1, Y1, Colour);
    }

    private static int textY(AbstractWidget Widget) {
        return Widget.getY() + (Widget.getHeight() - 8) / 2 + 1;
    }

    private static void row(TerminalPaint Paint, AbstractWidget Widget, String Line, boolean Lit, int Ink) {
        if (Lit && Widget.active) {
            Paint.fill(Widget.getX(), Widget.getY(), Widget.getX() + Widget.getWidth(), Widget.getY() + Widget.getHeight(), Paint.Colours.Main());
            Paint.text(Line, Widget.getX() + 3, textY(Widget), Paint.Colours.Bg());
        } else {
            Paint.text(Line, Widget.getX() + 3, textY(Widget), Widget.active ? Ink : Paint.Colours.Dim());
        }
    }

    private static void drawButton(TerminalPaint Paint, AbstractWidget Widget, boolean Lit) {
        int Columns = Paint.columns(Widget.getWidth() - 6);
        String Label = TerminalClient.upper(Widget.getMessage());
        String Line = TerminalPaint.fit(Label, Math.max(1, Columns - 2));
        Line = Line + " ".repeat(Math.max(1, Columns - Line.length() - 1)) + ">";
        frame(Paint, Widget.getX(), Widget.getY(), Widget.getX() + Widget.getWidth(), Widget.getY() + Widget.getHeight(),
                Widget.active ? Paint.Colours.Faint() : Paint.Colours.Bg());
        row(Paint, Widget, Line, Lit, Paint.Colours.Main());
    }

    private static void drawSelector(TerminalPaint Paint, AbstractWidget Widget, boolean Lit) {
        int Columns = Paint.columns(Widget.getWidth() - 6);
        Component[] Parts = split(Widget.getMessage());
        String Line;
        if (Parts == null) {
            String Bracket = "[ " + TerminalClient.upper(Widget.getMessage()) + " ]";
            Line = " ".repeat(Math.max(0, Columns - Bracket.length())) + Bracket;
        } else {
            Line = TerminalText.leader(TerminalClient.upper(Parts[0]), TerminalClient.upper(Parts[1]), Columns);
        }
        if (Lit && Widget.active) {
            row(Paint, Widget, Line, true, Paint.Colours.Main());
            return;
        }
        int Open = Line.lastIndexOf("[ ");
        int Ink = Widget.active ? Paint.Colours.Main() : Paint.Colours.Dim();
        int X = Widget.getX() + 3;
        int Y = textY(Widget);
        if (Open <= 0) {
            Paint.glow(Line, X, Y, Widget.active ? Paint.Colours.Bright() : Paint.Colours.Dim());
            return;
        }
        String Head = Line.substring(0, Open);
        int Dots = Head.indexOf(" .");
        String Name = Dots < 0 ? Head : Head.substring(0, Dots + 1);
        X = Paint.text(Name, X, Y, Ink);
        X = Paint.text(Head.substring(Name.length()), X, Y, Paint.Colours.Faint());
        Paint.glow(Line.substring(Open), X, Y, Widget.active ? Paint.Colours.Bright() : Paint.Colours.Dim());
    }

    private static void drawSlider(TerminalPaint Paint, AbstractSliderButton Slider, boolean Lit) {
        int Columns = Paint.columns(Slider.getWidth() - 6);
        Component[] Parts = split(Slider.getMessage());
        String Name = Parts == null ? TerminalClient.upper(Slider.getMessage()) : TerminalClient.upper(Parts[0]);
        String Value = Parts == null ? "" : TerminalClient.upper(Parts[1]);
        double Share = read(SliderValue, Slider) instanceof Double Found ? Found : 0.0;
        int Cells = Math.max(4, Math.min(12, Columns - Name.length() - Value.length() - 5));
        int Filled = (int) Math.round(Math.max(0.0, Math.min(1.0, Share)) * Cells);
        String Track = "[" + "#".repeat(Filled) + "-".repeat(Cells - Filled) + "]";
        String Head = TerminalPaint.fit(Name, Math.max(1, Columns - Track.length() - Value.length() - 2));
        String Line = Head + " ".repeat(Math.max(1, Columns - Head.length() - Track.length() - Value.length() - 1)) + Track + " " + Value;
        row(Paint, Slider, Line, Lit, Paint.Colours.Main());
    }

    private static void drawCheck(TerminalPaint Paint, Checkbox Tick, boolean Lit) {
        String Line = (Tick.selected() ? "[X] " : "[ ] ") + TerminalClient.upper(Tick.getMessage());
        row(Paint, Tick, TerminalPaint.fit(Line, Paint.columns(Tick.getWidth())), Lit, Paint.Colours.Main());
    }

    private static void drawField(TerminalPaint Paint, EditBox Input, boolean Lit) {
        int X = Input.getX();
        int Y = Input.getY();
        int Width = Input.getWidth();
        int Height = Input.getHeight();
        boolean Active = Input.isFocused() || Lit;
        Paint.fill(X, Y, X + Width, Y + Height, Paint.Colours.Panel());
        frame(Paint, X, Y, X + Width, Y + Height, Active ? Paint.Colours.Main() : Paint.Colours.Dim());
        int Columns = Math.max(1, Paint.columns(Width - 10));
        String Value = Input.getValue();
        int TextY = Y + (Height - 8) / 2 + 1;
        if (Value.isEmpty() && !Input.isFocused()) {
            if (read(EditHint, Input) instanceof Component Hint) {
                Paint.clipped(TerminalClient.upper(Hint), X + 4, TextY, Paint.Colours.Dim(), Columns);
            }
            return;
        }
        int Cursor = Math.max(0, Math.min(Value.length(), Input.getCursorPosition()));
        int Start = Math.max(0, Math.min(Cursor, Value.length() - Columns + 1));
        if (Cursor - Start >= Columns) {
            Start = Cursor - Columns + 1;
        }
        String Shown = Value.substring(Start, Math.min(Value.length(), Start + Columns));
        Paint.text(Shown, X + 4, TextY, Paint.Colours.Main());
        if (Input.isFocused() && Paint.blink()) {
            int CursorX = X + 4 + (Cursor - Start) * TerminalPaint.CharW;
            Paint.text(TerminalPaint.Block, CursorX, TextY, Paint.Colours.Bright());
            if (Cursor < Value.length()) {
                Paint.text(String.valueOf(Value.charAt(Cursor)), CursorX, TextY, Paint.Colours.Bg());
            }
        }
    }

    private static void drawLabel(TerminalPaint Paint, AbstractStringWidget Label, List<Skin> Skins) {
        Component Message = Label.getMessage();
        if (Label instanceof StringWidget) {
            AbstractWidget Partner = null;
            for (Skin Each : Skins) {
                AbstractWidget Other = Each.Widget();
                if (Each.Type() == SkinSelector && split(Other.getMessage()) == null && Math.abs(Other.getY() + Other.getHeight() / 2 - (Label.getY() + Label.getHeight() / 2)) <= 6
                        && Other.getX() > Label.getX()) {
                    Partner = Other;
                }
            }
            String Text = TerminalClient.upper(Message);
            if (Partner != null) {
                int Columns = Paint.columns(Partner.getX() - Label.getX());
                String Name = TerminalPaint.fit(Text, Math.max(1, Columns - 3));
                int End = Paint.text(Name + " ", Label.getX(), textY(Label), Paint.Colours.Main());
                Paint.text(".".repeat(Math.max(0, Columns - Name.length() - 2)), End, textY(Label), Paint.Colours.Faint());
                return;
            }
            float Align = read(LabelAlign, Label) instanceof Float Value ? Value : 0.5f;
            String Shown = TerminalPaint.fit(Text, Paint.columns(Label.getWidth()) + 2);
            int X = Label.getX() + Math.round((Label.getWidth() - Paint.width(Shown)) * Align);
            Paint.text(Shown, X, textY(Label), Paint.Colours.Main());
            return;
        }
        int Y = Label.getY();
        for (FormattedCharSequence Line : Paint.wrap(Message, Math.max(TerminalPaint.CharW, Label.getWidth()))) {
            Paint.Graphics.drawString(Paint.Text, Line, Label.getX(), Y, Paint.Colours.Dim(), false);
            Y += TerminalPaint.LineH;
        }
    }

    private static void drawTab(TerminalPaint Paint, TabButton Tab, boolean Lit) {
        Component Label = Component.literal(TerminalClient.upper(Tab.getMessage()));
        if (Lit) {
            Paint.bracket(Label, Tab.getX() + 1, Tab.getY() + 3, true);
            return;
        }
        Component Shown = Component.literal("[ ").append(Label).append(" ]");
        if (Tab.isSelected()) {
            Paint.fill(Tab.getX(), Tab.getY(), Tab.getX() + Tab.getWidth(), Tab.getY() + Tab.getHeight(), Paint.Colours.Faint());
            Paint.glow(Shown, Tab.getX() + 1, Tab.getY() + 3, Paint.Colours.Bright());
            Paint.fill(Tab.getX(), Tab.getY() + Tab.getHeight() - 1, Tab.getX() + Tab.getWidth(), Tab.getY() + Tab.getHeight(), Paint.Colours.Bright());
        } else {
            Paint.text(Shown, Tab.getX() + 1, Tab.getY() + 3, Paint.Colours.Dim());
        }
    }

    private static int rowTop(AbstractSelectionList<?> Picker, int Index) {
        try {
            return (int) RowTop.invoke(Picker, Index);
        } catch (ReflectiveOperationException | RuntimeException Failure) {
            return Integer.MIN_VALUE;
        }
    }

    public static List<String> tagsOf(LevelSummary Summary) {
        return TerminalWorldTags.tags(Summary.getGameMode().getName(), Summary.isHardcore(), Summary.hasCommands(),
                Summary.levelVersion().minecraftVersion().getVersion(), SharedConstants.getCurrentVersion().getDataVersion().getVersion(),
                Summary.isExperimental(), Summary.isLocked(), Summary.isCompatible(), Summary.requiresManualConversion());
    }

    private static boolean drawWorlds(TerminalPaint Paint, WorldSelectionList Picker, int MouseX, int MouseY, boolean MouseMode) {
        if (RowTop == null || EntrySummary == null || !(read(ItemHeight, Picker) instanceof Integer ItemH)) {
            return false;
        }
        int Left = Picker.getRowLeft() - 2;
        int Width = Picker.getRowWidth();
        int Top = Picker.getY();
        int Bottom = Picker.getBottom();
        List<WorldSelectionList.Entry> Entries = Picker.children();
        WorldSelectionList.Entry Selected = Picker.getSelected();
        WorldSelectionList.Entry Hovered = null;
        boolean Inside = MouseY >= Top && MouseY < Bottom;
        boolean Worlds = false;
        for (int Index = 0; Index < Entries.size(); Index++) {
            int RowY = rowTop(Picker, Index);
            Worlds |= Entries.get(Index) instanceof WorldSelectionList.WorldListEntry;
            if (Inside && RowY != Integer.MIN_VALUE && TerminalHighlight.within(MouseX, MouseY, Left, RowY, Width, ItemH)) {
                Hovered = Entries.get(Index);
            }
        }
        if (!MouseMode) {
            Hovered = null;
        }
        Paint.Graphics.enableScissor(Left - 4, Top, Left + Width + 4, Bottom);
        for (int Index = 0; Index < Entries.size(); Index++) {
            int RowY = rowTop(Picker, Index);
            if (RowY == Integer.MIN_VALUE || RowY + ItemH < Top || RowY > Bottom) {
                continue;
            }
            WorldSelectionList.Entry Entry = Entries.get(Index);
            if (!(Entry instanceof WorldSelectionList.WorldListEntry World) || !(read(EntrySummary, World) instanceof LevelSummary Summary)) {
                Component Loading = Component.translatable(Prefix + "loading");
                Paint.text(Loading, Left + (Width - Paint.width(Loading)) / 2, RowY + ItemH / 2 - 4, Paint.Colours.Dim());
                continue;
            }
            int CardBottom = RowY + ItemH - 2;
            boolean IsSelected = Entry == Selected;
            boolean IsHovered = Entry == Hovered && !IsSelected;
            if (IsSelected) {
                Paint.fill(Left, RowY, Left + Width, CardBottom, Paint.Colours.Main());
            } else {
                Paint.fill(Left, RowY, Left + Width, CardBottom, IsHovered ? Paint.Colours.Faint() : Paint.Colours.Panel());
                frame(Paint, Left, RowY, Left + Width, CardBottom, IsHovered ? Paint.Colours.Bright() : Paint.Colours.Faint());
            }
            int IconX = Left + 2;
            int IconY = RowY + 1;
            Paint.fill(IconX - 1, IconY - 1, IconX + IconSize + 1, IconY + IconSize + 1, IsSelected ? Paint.Colours.Bg() : Paint.Colours.Dim());
            if (read(EntryIcon, World) instanceof FaviconTexture Icon) {
                RenderSystem.enableBlend();
                Paint.Graphics.blit(Icon.textureLocation(), IconX, IconY, 0.0f, 0.0f, IconSize, IconSize, IconSize, IconSize);
                RenderSystem.disableBlend();
            }
            int TextX = Left + CardInset;
            int Columns = Paint.columns(Left + Width - 3 - TextX);
            String Name = Summary.getLevelName();
            if (Name == null || Name.isBlank()) {
                Name = I18n.get("selectWorld.world") + " " + (Index + 1);
            }
            List<String> Tags = tagsOf(Summary);
            int TagSpan = TerminalTags.Width + 1;
            int TagRoom = Math.max(1, Math.min(Tags.size(), (Columns - 8) / TagSpan));
            int Ink = IsSelected ? Paint.Colours.Bg() : Paint.Colours.Main();
            int Soft = IsSelected ? Paint.Colours.Bg() : Paint.Colours.Dim();
            int Head = IsSelected ? Paint.Colours.Bg() : Paint.Colours.Bright();
            Paint.clipped(TerminalText.upper(Name), TextX, RowY + 3, Head, Math.max(4, Columns - TagRoom * TagSpan));
            int TagX = Left + Width - 3 - TagRoom * TagSpan * TerminalPaint.CharW + TerminalPaint.CharW;
            for (int Tag = 0; Tag < TagRoom; Tag++) {
                String Label = Tags.get(Tag);
                boolean Alert = Label.equals(TerminalWorldTags.Broken) || Label.equals(TerminalWorldTags.Locked) || Label.equals(TerminalWorldTags.Older)
                        || Label.equals(TerminalWorldTags.Hardcore);
                Paint.tag(Label, TagX + Tag * TagSpan * TerminalPaint.CharW, RowY + 3, IsSelected ? Paint.Colours.Bg() : Alert ? Paint.Colours.Warn() : Ink);
            }
            String Played = Summary.getLastPlayed() > 0L ? WorldSelectionList.DATE_FORMAT.format(Instant.ofEpochMilli(Summary.getLastPlayed())) : "--";
            Paint.clipped(TerminalText.upper(I18n.get(Prefix + "played", Played)), TextX, RowY + 14, Soft, Columns);
            String Extra = Tags.size() > TagRoom ? "  " + String.join(" ", Tags.subList(TagRoom, Tags.size())) : "";
            Paint.clipped(TerminalText.upper(I18n.get(Prefix + "version", Summary.levelVersion().minecraftVersionName())) + Extra, TextX, RowY + 24, Soft, Columns);
        }
        if (!Worlds && Entries.stream().allMatch(Each -> Each instanceof WorldSelectionList.WorldListEntry)) {
            Component Empty = Component.translatable(Prefix + "empty");
            Paint.text(Empty, Left + (Width - Paint.width(Empty)) / 2, Top + 12, Paint.Colours.Dim());
        }
        Paint.Graphics.disableScissor();
        int MaxScroll = Picker.getMaxScroll();
        int TrackX = Left + Width + 4;
        Paint.fill(TrackX, Top, TrackX + ScrollW, Bottom, Paint.Colours.Faint());
        if (MaxScroll > 0) {
            int Track = Bottom - Top;
            int Thumb = Math.max(8, Track * Track / Math.max(1, Track + MaxScroll));
            int ThumbY = Top + (int) ((Track - Thumb) * (Picker.getScrollAmount() / MaxScroll));
            Paint.fill(TrackX, ThumbY, TrackX + ScrollW, ThumbY + Thumb, Paint.Colours.Main());
        } else {
            Paint.fill(TrackX, Top, TrackX + ScrollW, Bottom, Paint.Colours.Dim());
        }
        return true;
    }
}
