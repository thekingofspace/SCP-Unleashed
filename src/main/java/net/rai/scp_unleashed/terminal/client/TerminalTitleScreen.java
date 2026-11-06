package net.rai.scp_unleashed.terminal.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModList;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.loading.ClientModLoader;
import net.neoforged.neoforge.internal.BrandingControl;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.terminal.TerminalArchive;
import net.rai.scp_unleashed.terminal.TerminalAux;
import net.rai.scp_unleashed.terminal.TerminalBoot;
import net.rai.scp_unleashed.terminal.TerminalHighlight;
import net.rai.scp_unleashed.terminal.TerminalMotd;
import net.rai.scp_unleashed.terminal.TerminalSettings;
import net.rai.scp_unleashed.terminal.TerminalTags;
import net.rai.scp_unleashed.terminal.TerminalText;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TerminalTitleScreen extends TitleScreen {
    public static final String Prefix = "terminal.scp_unleashed.";
    public static final String ArchiveKey = Prefix + "menu.archive";
    public static final int AuxGap = 6;
    public static final int IconGap = 4;
    public static final int IconMax = 24;
    public static final int ColumnGap = 8;
    public static final int IdleLimit = 40;
    public static final int SpinMs = 90;

    public enum Role {
        Demo("menu.playdemo", "demo"),
        ResetDemo("menu.resetdemo", "reset_demo"),
        Singleplayer("menu.singleplayer", "singleplayer"),
        Multiplayer("menu.multiplayer", "multiplayer"),
        Realms("menu.online", "realms"),
        Mods("fml.menu.mods", "mods"),
        Options("menu.options", "options"),
        Quit("menu.quit", "quit"),
        ModConfig(Prefix + "menu.mod_config", TerminalAux.ModConfig),
        ScpConfig(Prefix + "menu.scp_config", TerminalAux.ScpConfig),
        Archive(ArchiveKey, TerminalAux.Archive),
        Language("options.language", "language"),
        Accessibility("options.accessibility", "accessibility"),
        Credits("title.credits", "credits");

        public final String MessageKey;
        public final String Id;

        Role(String NewMessageKey, String NewId) {
            MessageKey = NewMessageKey;
            Id = NewId;
        }

        public Component label() {
            return Component.translatable(Prefix + "menu." + Id);
        }

        public static Role of(String Key) {
            for (Role Each : values()) {
                if (Each.MessageKey.equals(Key)) {
                    return Each;
                }
            }
            return null;
        }
    }

    public static final int SlotRow = 0;
    public static final int SlotBracket = 1;
    public static final int SlotCredits = 2;
    public static final List<Role> AuxRoles = List.of(Role.ModConfig, Role.ScpConfig, Role.Archive);
    public static final List<Role> MenuOrder = List.of(Role.Demo, Role.ResetDemo, Role.Singleplayer, Role.Multiplayer, Role.Realms, Role.Mods, Role.Options, Role.Quit);

    private record PanelLine(String Tag, Component Line, boolean Packed) {
    }

    private static boolean BootShown;

    private final long Seed;
    private final int MotdIndex;
    private final List<PanelLine> Idle = new ArrayList<>();
    private TerminalBoot Boot;
    private List<String> BootLines = List.of();
    private List<PanelLine> Summary = List.of();
    private int LastTyped;
    private boolean BeepPlayed;
    private long IdleNext = -1L;
    private int IdleStep;
    private Boolean UpdateMark;
    private boolean LastEnabled = true;

    public TerminalTitleScreen() {
        super(false);
        Seed = Util.getMillis() ^ System.nanoTime();
        MotdIndex = TerminalMotd.pick(Seed);
    }

    @Override
    protected void init() {
        super.init();
        LastEnabled = TerminalSettings.menu();
        if (LastEnabled) {
            addRenderableWidget(Button.builder(Component.translatable(Prefix + "menu.mod_config"), Press -> minecraft.setScreen(new ModListScreen(this)))
                    .bounds(0, 0, TerminalPaint.MinMenuW, TerminalPaint.RowH - 1).build());
            addRenderableWidget(Button.builder(Component.translatable(Prefix + "menu.scp_config"), Press -> openConfig())
                    .bounds(0, 0, TerminalPaint.MinMenuW, TerminalPaint.RowH - 1).build());
            addRenderableWidget(Button.builder(Component.translatable(ArchiveKey), Press -> minecraft.setScreen(new TerminalArchiveScreen(this)))
                    .bounds(0, 0, TerminalPaint.MinMenuW, TerminalPaint.RowH - 1).build());
        }
        if (Boot == null) {
            prepareBoot();
        }
    }

    private void openConfig() {
        ModList.get().getModContainerById(Scp_unleashed.ModId).ifPresent(Container -> minecraft.setScreen(IConfigScreenFactory.getForMod(Container.getModInfo())
                .map(Factory -> Factory.createScreen(Container, this)).orElseGet(() -> new ConfigurationScreen(Container, this))));
    }

    private void prepareBoot() {
        Optional<TerminalArchive.Census> Count = TerminalArchive.census();
        List<TerminalBoot.Step> Steps = TerminalBoot.script(Count.isPresent());
        List<String> Lines = new ArrayList<>();
        List<PanelLine> Tail = new ArrayList<>();
        for (TerminalBoot.Step Each : Steps) {
            String Line = bootText(Each.Key(), Count);
            Lines.add(Line);
            if (Each.Tagged()) {
                Tail.add(new PanelLine(TerminalTags.Ok, Component.literal(Line), false));
            } else if (TerminalBoot.Breakdown.equals(Each.Key())) {
                Tail.add(new PanelLine(null, Component.literal(Line), true));
            }
        }
        BootLines = Lines;
        Summary = Tail;
        Boot = new TerminalBoot(Steps, Lines.stream().mapToInt(String::length).toArray());
        if (BootShown || TerminalClient.calm()) {
            Boot.skip();
        }
    }

    private static String bootText(String Key, Optional<TerminalArchive.Census> Count) {
        if (Key.isEmpty()) {
            return "";
        }
        if (TerminalBoot.Breakdown.equals(Key)) {
            return breakdown(Count.orElse(null));
        }
        if (TerminalBoot.Indexed.equals(Key)) {
            return I18n.get(Key, Count.map(TerminalArchive.Census::Total).orElse(0));
        }
        if (TerminalBoot.Modules.equals(Key)) {
            return I18n.get(Key, ModList.get().size());
        }
        return I18n.get(Key);
    }

    public static String breakdown(TerminalArchive.Census Count) {
        if (Count == null) {
            return "";
        }
        List<String> Parts = new ArrayList<>();
        for (DocumentClass Each : DocumentClass.values()) {
            int Found = Count.of(Each);
            if (Found > 0) {
                Parts.add(Found + " " + TerminalClient.upper(Each.getName()));
            }
        }
        return String.join(TerminalText.Separator, Parts);
    }

    private boolean intro(long Now) {
        return TerminalSettings.menu() && Boot != null && !Boot.revealed(Boot.elapsed(Now));
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (intro(Util.getMillis())) {
            Boot.skip();
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        if (intro(Util.getMillis())) {
            Boot.skip();
            return true;
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        boolean Enabled = TerminalSettings.menu();
        if (Enabled != LastEnabled) {
            LastEnabled = Enabled;
            rebuildWidgets();
        }
        if (!Enabled || Boot == null) {
            super.render(Graphics, MouseX, MouseY, PartialTick);
            return;
        }
        long Now = Util.getMillis();
        if (minecraft.getOverlay() == null && !Boot.started()) {
            Boot.begin(Now);
            BootShown = true;
        }
        if (Boot.started() && !Boot.skipped() && !BeepPlayed) {
            BeepPlayed = true;
            TerminalSound.beep();
        }
        TerminalSound.frame(Now);
        TerminalPaint Paint = new TerminalPaint(Graphics, font, width, height, Now);
        long Elapsed = Boot.elapsed(Now);
        if (!Boot.done(Elapsed)) {
            Paint.background();
            drawBoot(Paint, Elapsed);
        } else {
            drawMenu(Paint, Elapsed, MouseX, MouseY, PartialTick);
        }
        Paint.crt();
    }

    private void drawBoot(TerminalPaint Paint, long Elapsed) {
        int Y = Paint.Pad + 4;
        int Current = Boot.current(Elapsed);
        int TypedNow = 0;
        int Columns = Paint.columns(Paint.ContentW);
        int TextColumns = Math.max(8, Columns - TerminalTags.Width - 1);
        for (int Index = 0; Index <= Current && Index < Boot.lines(); Index++) {
            TerminalBoot.Step Step = Boot.steps().get(Index);
            String Full = BootLines.get(Index);
            String Key = Step.Key();
            int Chars = Boot.chars(Index, Elapsed);
            TypedNow += Chars;
            int X = Paint.ContentX;
            boolean Indented = Step.Tagged() || TerminalBoot.Breakdown.equals(Key);
            if (Step.Tagged()) {
                boolean Done = Boot.resolved(Index, Elapsed);
                if (Done) {
                    Paint.tag(TerminalTags.Ok, X, Y, Paint.Colours.Bright());
                } else {
                    Paint.spinner((int) (Elapsed / SpinMs), X, Y, Paint.Colours.Dim());
                }
            }
            if (Indented) {
                X += TerminalTags.textOffset(TerminalPaint.CharW);
            }
            int Room = Indented ? TextColumns : Columns;
            List<String> Rows = TerminalBoot.Breakdown.equals(Key) ? TerminalText.pack(TerminalText.parts(Full), Room) : TerminalText.wrap(Full, Room);
            int Colour = TerminalBoot.Title.equals(Key) || TerminalBoot.Motto.equals(Key) ? Paint.Colours.Bright()
                    : TerminalBoot.Breakdown.equals(Key) ? Paint.Colours.Dim() : Paint.Colours.Main();
            int Left = Chars;
            int End = X;
            for (int Row = 0; Row < Rows.size(); Row++) {
                String Segment = Rows.get(Row);
                String Shown = Segment.substring(0, Math.max(0, Math.min(Segment.length(), Left)));
                if (Row > 0 && Shown.isEmpty()) {
                    break;
                }
                if (Row > 0) {
                    Y += TerminalPaint.LineH;
                }
                End = Paint.glow(Shown, X, Y, Colour);
                if (Shown.isEmpty()) {
                    End = X;
                }
                Left -= Segment.length() + (TerminalBoot.Breakdown.equals(Key) ? TerminalText.Separator.length() : 1);
            }
            if (Index == Current) {
                Paint.cursor(End == X ? X : End + 1, Y, Paint.Colours.Main());
            }
            Y += TerminalPaint.LineH;
        }
        TerminalSound.typed(TypedNow - LastTyped, Paint.Now);
        LastTyped = TypedNow;
    }

    private record Slot(AbstractWidget Widget, int Kind, Component Label, Component Suffix, int SuffixColour) {
    }

    private void drawMenu(TerminalPaint Paint, long Elapsed, int MouseX, int MouseY, float PartialTick) {
        Map<Role, AbstractWidget> Known = new EnumMap<>(Role.class);
        List<AbstractWidget> Plain = new ArrayList<>();
        List<AbstractWidget> Native = new ArrayList<>();
        List<Renderable> Others = new ArrayList<>();
        for (Renderable Each : renderables) {
            if (Each instanceof AbstractWidget Widget) {
                if (!Widget.visible) {
                    continue;
                }
                Role Which = Role.of(TerminalClient.key(Widget.getMessage()));
                if (Which != null && !Known.containsKey(Which)) {
                    Known.put(Which, Widget);
                } else if (plain(Widget)) {
                    Plain.add(Widget);
                } else {
                    Native.add(Widget);
                }
            } else {
                Others.add(Each);
            }
        }
        int CharW = TerminalPaint.CharW;
        int RowH = TerminalPaint.RowH;
        int Scale = TerminalPaint.logoScale(Paint.ContentW, height);
        int LogoY = Paint.Pad + TerminalPaint.HeaderGap;
        int SubY = LogoY + 8 * Scale + 4;
        int MenuTop = SubY + TerminalPaint.LineH + 10;
        int FooterY = height - Paint.Pad - 9;
        int BrandY = FooterY - 12;
        boolean Wide = footerFits(Paint, Known);
        boolean Status = ClientHooks.forgeStatusLine != null;
        int Raise = !Wide && Status ? 11 : 0;
        List<Role> Menu = MenuOrder.stream().filter(Known::containsKey).toList();
        int MenuW = TerminalPaint.MinMenuW;
        int Locked = Paint.width(Component.translatable(Prefix + "menu.locked"));
        for (Role Each : Menu) {
            MenuW = Math.max(MenuW, Paint.width(Each.label()) + 4 * CharW + 6 + (Known.get(Each).active ? 2 * CharW : Locked));
        }
        for (Role Each : AuxRoles) {
            if (Known.containsKey(Each)) {
                MenuW = Math.max(MenuW, Paint.width(Each.label()) + 4 * CharW + 6);
            }
        }
        for (AbstractWidget Each : Plain) {
            MenuW = Math.max(MenuW, Math.min(Paint.ContentW / 2, Paint.width(TerminalClient.upper(Each.getMessage())) + 4 * CharW + 6));
        }
        List<Slot> Slots = new ArrayList<>();
        for (int Index = 0; Index < Menu.size(); Index++) {
            Role Which = Menu.get(Index);
            AbstractWidget Widget = Known.get(Which);
            place(Widget, Paint.ContentX, MenuTop + Index * RowH, MenuW, RowH - 1);
            if (!Boot.rowShown(Index, Elapsed)) {
                continue;
            }
            Component Suffix = null;
            int SuffixColour = Paint.Colours.Warn();
            if (!Widget.active) {
                Suffix = Component.translatable(Prefix + "menu.locked");
                SuffixColour = Paint.Colours.Dim();
            } else if (Which == Role.Mods && updateMark()) {
                Suffix = Component.literal(" *");
            }
            Slots.add(new Slot(Widget, SlotRow, Which.label(), Suffix, SuffixColour));
        }
        boolean Revealed = Boot.rowShown(Menu.size(), Elapsed);
        int AuxTop = MenuTop + Menu.size() * RowH + AuxGap;
        int AuxLimit = BrandY - 4 - Raise;
        int RowStart = AuxTop + TerminalPaint.LineH + 1;
        Map<String, AbstractWidget> ById = new LinkedHashMap<>();
        Map<String, Component> Labels = new HashMap<>();
        List<TerminalAux.Item> Items = new ArrayList<>();
        List<String> Extra = new ArrayList<>();
        for (int Index = 0; Index < Plain.size(); Index++) {
            Extra.add("plain:" + Index);
            ById.put("plain:" + Index, Plain.get(Index));
        }
        for (int Index = 0; Index < Native.size(); Index++) {
            Extra.add("native:" + Index);
            ById.put("native:" + Index, Native.get(Index));
        }
        for (Role Each : AuxRoles) {
            if (Known.containsKey(Each)) {
                ById.put(Each.Id, Known.get(Each));
                Labels.put(Each.Id, Each.label());
            }
        }
        for (String Id : TerminalAux.order(Extra)) {
            AbstractWidget Widget = ById.get(Id);
            if (Widget == null) {
                continue;
            }
            boolean Row = !Id.startsWith("native:");
            Items.add(new TerminalAux.Item(Id, Row ? MenuW : Widget.getWidth(), Row ? RowH - 1 : Widget.getHeight(), Row));
            if (Row && !Labels.containsKey(Id)) {
                Labels.put(Id, Component.literal(TerminalPaint.fit(TerminalClient.upper(Widget.getMessage()), (MenuW - 30) / CharW)));
            }
        }
        List<TerminalAux.Spot> Spots = TerminalAux.place(Items, Paint.ContentX, RowStart, AuxLimit, MenuW, RowH, ColumnGap, IconMax, IconGap);
        int Right = Paint.ContentX + MenuW;
        List<AbstractWidget> Natives = new ArrayList<>();
        for (TerminalAux.Spot Spot : Spots) {
            AbstractWidget Widget = ById.get(Spot.Id());
            Right = Math.max(Right, Spot.X() + Spot.Width());
            if (Spot.Id().startsWith("native:")) {
                Widget.setX(Spot.X());
                Widget.setY(Spot.Y());
                Natives.add(Widget);
            } else {
                place(Widget, Spot.X(), Spot.Y(), Spot.Width(), Spot.Height());
                if (Revealed) {
                    Slots.add(new Slot(Widget, SlotRow, Labels.get(Spot.Id()), null, 0));
                }
            }
        }
        int FooterX = Paint.ContentX + 1;
        for (Role Which : List.of(Role.Language, Role.Accessibility)) {
            AbstractWidget Widget = Known.get(Which);
            if (Widget == null) {
                continue;
            }
            int Span = Paint.width(Component.literal("[  ]").append(Which.label()));
            place(Widget, FooterX - 1, FooterY - 2, Span + 2, 11);
            if (Revealed) {
                Slots.add(new Slot(Widget, SlotBracket, Which.label(), null, 0));
            }
            FooterX += Span + 8;
        }
        AbstractWidget Credits = Known.get(Role.Credits);
        int CreditsY = Wide ? FooterY : BrandY;
        int CreditsX = Paint.right();
        if (Credits != null) {
            int Span = Math.min(Paint.width(Credits.getMessage()), Paint.ContentW);
            CreditsX = Paint.right() - Span;
            place(Credits, CreditsX - 1, CreditsY - 2, Span + 2, 11);
            if (Revealed) {
                Slots.add(new Slot(Credits, SlotCredits, Credits.getMessage(), null, 0));
            }
        }
        for (Slot Each : Slots) {
            TerminalClient.ghost(Paint.Graphics, Each.Widget(), MouseX, MouseY, PartialTick);
        }
        boolean MouseMode = TerminalClient.observe(MouseX, MouseY);
        boolean AnyHovered = false;
        boolean AnyFocused = false;
        for (Slot Each : Slots) {
            AnyHovered |= hovered(Each.Widget(), MouseX, MouseY);
            AnyFocused |= Each.Widget().isFocused();
        }
        Paint.background();
        Paint.header(Component.translatable(Prefix + "header.title"),
                Component.translatable(Prefix + "header.uptime", TerminalClient.clock(Paint.Now - TerminalClient.Started)));
        Paint.logo(Paint.ContentX, LogoY, Scale, Boot.logoChars(Elapsed, TerminalPaint.Logo.length()), true);
        Paint.glow(Component.translatable(TerminalBoot.Motto), Paint.ContentX, SubY, Paint.Colours.Dim());
        if (Revealed && Items.size() > 0) {
            Paint.text(Component.translatable(Prefix + "menu.aux"), Paint.ContentX + 3, AuxTop, Paint.Colours.Dim());
        }
        for (Slot Each : Slots) {
            AbstractWidget Widget = Each.Widget();
            boolean Lit = TerminalHighlight.lit(hovered(Widget, MouseX, MouseY), Widget.isFocused(), AnyHovered, AnyFocused, MouseMode);
            switch (Each.Kind()) {
                case SlotBracket -> Paint.bracket(Each.Label(), Widget.getX() + 1, FooterY, Lit);
                case SlotCredits -> {
                    String Line = Each.Label().getString();
                    int Columns = Paint.columns(Widget.getWidth() - 2) + 1;
                    if (Lit) {
                        Paint.fill(Widget.getX(), Widget.getY(), Widget.getX() + Widget.getWidth(), Widget.getY() + Widget.getHeight(), Paint.Colours.Main());
                    }
                    Paint.clipped(Line, Widget.getX() + 1, CreditsY, Lit ? Paint.Colours.Bg() : Paint.Colours.Dim(), Columns);
                }
                default -> Paint.row(Widget.getX(), Widget.getY(), Widget.getWidth(), Each.Label(), Lit, !Widget.active, Each.Suffix(), Each.SuffixColour());
            }
        }
        if (Revealed) {
            for (AbstractWidget Widget : Natives) {
                Widget.render(Paint.Graphics, MouseX, MouseY, PartialTick);
            }
            List<String> Brands = new ArrayList<>();
            BrandingControl.forEachLine(true, false, (Index, Line) -> Brands.add(Line));
            int BrandColumns = Wide ? Paint.columns(Paint.ContentW * 2 / 3) : Paint.columns(CreditsX - 8 - Paint.ContentX);
            Paint.clipped(String.join(" / ", Brands), Paint.ContentX, BrandY, Paint.Colours.Dim(), BrandColumns);
            if (Status) {
                String Shown = TerminalPaint.fit(ClientHooks.forgeStatusLine, Paint.columns(Paint.ContentW / 3));
                Paint.text(Shown, Paint.right() - Paint.width(Shown), Wide ? BrandY : BrandY - 11, Paint.Colours.Warn());
            }
        }
        int PanelX = Right + TerminalPaint.PanelGap;
        if (Revealed && Paint.right() - PanelX >= TerminalPaint.MinPanelW) {
            drawPanel(Paint, PanelX, MenuTop + 4, Paint.right(), BrandY - 6 - Raise);
        }
        for (Renderable Each : Others) {
            Each.render(Paint.Graphics, MouseX, MouseY, PartialTick);
        }
    }

    private static boolean hovered(AbstractWidget Widget, int MouseX, int MouseY) {
        return Widget.visible && TerminalHighlight.within(MouseX, MouseY, Widget.getX(), Widget.getY(), Widget.getWidth(), Widget.getHeight());
    }

    private static int footerButtons(TerminalPaint Paint, Map<Role, AbstractWidget> Known) {
        int Span = 0;
        for (Role Which : List.of(Role.Language, Role.Accessibility)) {
            if (Known.containsKey(Which)) {
                Span += Paint.width(Component.literal("[  ]").append(Which.label())) + 8;
            }
        }
        return Span;
    }

    private static boolean footerFits(TerminalPaint Paint, Map<Role, AbstractWidget> Known) {
        AbstractWidget Credits = Known.get(Role.Credits);
        return Credits == null || footerButtons(Paint, Known) + 8 + Paint.width(Credits.getMessage()) <= Paint.ContentW;
    }

    private void drawPanel(TerminalPaint Paint, int X0, int Y0, int X1, int Y1) {
        if (Y1 - Y0 < 40) {
            return;
        }
        if (IdleNext < 0L) {
            IdleNext = Paint.Now + TerminalMotd.idleGap(Seed, IdleStep);
        } else if (Paint.Now >= IdleNext) {
            int Index = TerminalMotd.idleIndex(Seed, IdleStep);
            Idle.add(new PanelLine(TerminalMotd.idleTag(Index), Component.translatable(TerminalMotd.idleKey(Index)), false));
            while (Idle.size() > IdleLimit) {
                Idle.remove(0);
            }
            IdleStep++;
            IdleNext = Paint.Now + TerminalMotd.idleGap(Seed, IdleStep);
            TerminalSound.typed(TerminalSound.CharsPerKey, Paint.Now);
        }
        Paint.panel(X0, Y0, X1, Y1, Component.translatable(Prefix + "panel.log"));
        int Inner = X1 - X0 - 14;
        int Columns = Paint.columns(Inner);
        List<FormattedCharSequence> Motd = List.of();
        if (!minecraft.options.hideSplashTexts().get()) {
            Motd = Paint.wrap(Component.translatable(Prefix + "motd", Component.translatable(TerminalMotd.motdKey(MotdIndex))), Inner);
        }
        int MotdTop = Y1 - 7 - Motd.size() * TerminalPaint.LineH;
        List<PanelLine> Lines = new ArrayList<>(Summary);
        Lines.addAll(Idle);
        int TextX = X0 + 7 + TerminalTags.textOffset(TerminalPaint.CharW);
        int TextColumns = Math.max(8, Columns - TerminalTags.Width - 1);
        List<PanelLine> Rows = new ArrayList<>();
        for (PanelLine Line : Lines) {
            String Body = TerminalText.upper(Line.Line().getString());
            List<String> Wrapped = Line.Packed() ? TerminalText.pack(TerminalText.parts(Body), TextColumns) : TerminalText.wrap(Body, TextColumns);
            for (int Row = 0; Row < Wrapped.size(); Row++) {
                Rows.add(new PanelLine(Row == 0 ? Line.Tag() : null, Component.literal(Wrapped.get(Row)), false));
            }
        }
        int Room = Math.max(0, (MotdTop - 6 - (Y0 + 8)) / TerminalPaint.LineH);
        int First = Math.max(0, Rows.size() - Room);
        int Y = Y0 + 8;
        for (int Index = First; Index < Rows.size(); Index++) {
            PanelLine Line = Rows.get(Index);
            if (Line.Tag() != null) {
                Paint.tag(Line.Tag(), X0 + 7, Y, Paint.Colours.Main());
            }
            Paint.text(Line.Line().getString(), TextX, Y, Paint.Colours.Dim());
            Y += TerminalPaint.LineH;
        }
        int MotdY = MotdTop;
        for (FormattedCharSequence Line : Motd) {
            Paint.Graphics.drawString(font, Line, X0 + 7, MotdY, Paint.Colours.Bright(), false);
            MotdY += TerminalPaint.LineH;
        }
    }

    private boolean updateMark() {
        if (UpdateMark == null) {
            boolean Mark;
            try {
                Mark = ClientModLoader.checkForUpdates().shouldDraw();
            } catch (RuntimeException Failure) {
                Mark = false;
            }
            UpdateMark = Mark;
        }
        return UpdateMark;
    }

    private static boolean plain(AbstractWidget Widget) {
        return Widget.getClass() == Button.class;
    }

    private static void place(AbstractWidget Widget, int X, int Y, int Width, int Height) {
        Widget.setX(X);
        Widget.setY(Y);
        Widget.setWidth(Width);
        Widget.setHeight(Height);
    }
}
