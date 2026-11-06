package net.rai.scp_unleashed.guidebook.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guidebook.GuidebookRegistry;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.rai.scp_unleashed.guidebook.client.TabletLayout.*;

public class FacilityTabletScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/guidebook/tablet.png");
    private static final String Prefix = "gui.scp_unleashed.facility_tablet.";
    private static final int FullBoot = 16;
    private static final int QuickBoot = 6;
    private static final int ChimeGap = 3;
    private static final int PressTicks = 4;
    private static final int BootSegments = 12;
    private static final double ScrollStep = 12.0;
    private static final Style AccentStyle = Style.EMPTY.withColor(TextColor.fromRgb(Accent & 0xFFFFFF));
    private static final Style BoldStyle = Style.EMPTY.withBold(true);
    @Nullable
    private static ResourceLocation LastPage;
    private static double LastScroll;
    private static boolean Booted;

    private final int BootLength;
    private final List<Laid> Layout = new ArrayList<>();
    private final List<SideRow> SideRows = new ArrayList<>();
    private int Left;
    private int Top;
    private int Ticks;
    private int PageIndex = -1;
    private int ContentHeight;
    private double Scroll;
    private int SideContentHeight;
    private double SideScroll;
    private int HomePressedUntil = -1;
    private boolean DraggingThumb;
    @Nullable
    private ItemStack HoverStack;
    @Nullable
    private Component HoverText;
    @Nullable
    private ResourceLocation HoverLink;
    private final List<LinkSpot> LinkSpots = new ArrayList<>();

    public FacilityTabletScreen() {
        super(Component.translatable(Prefix + "title"));
        BootLength = Booted ? QuickBoot : FullBoot;
        Booted = true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = Math.max(0, (height - PanelHeight) / 2);
        List<GuidePage> Pages = GuideLibrary.pages();
        double Keep = Scroll;
        if (PageIndex < 0) {
            int Found = GuideLibrary.indexOf(LastPage);
            PageIndex = Math.max(0, Found);
            Keep = Found >= 0 ? LastScroll : 0.0;
        }
        PageIndex = Pages.isEmpty() ? 0 : Mth.clamp(PageIndex, 0, Pages.size() - 1);
        buildSidebar();
        buildPage();
        Scroll = Mth.clamp(Keep, 0.0, maxScroll());
    }

    @Override
    public void tick() {
        super.tick();
        Ticks++;
        if (BootLength == FullBoot && Ticks == BootLength) {
            play(GuidebookRegistry.TabletChime, 1.0f, 0.35f);
        }
        if (BootLength == FullBoot && Ticks == BootLength + ChimeGap) {
            play(GuidebookRegistry.TabletChime, 1.5f, 0.3f);
        }
    }

    @Override
    public void removed() {
        super.removed();
        GuidePage Page = currentPage();
        if (Page != null) {
            LastPage = Page.Id();
            LastScroll = Scroll;
        }
    }

    private boolean booting() {
        return Ticks < BootLength;
    }

    @Nullable
    private GuidePage currentPage() {
        List<GuidePage> Pages = GuideLibrary.pages();
        return PageIndex >= 0 && PageIndex < Pages.size() ? Pages.get(PageIndex) : null;
    }

    private void play(DeferredHolder<SoundEvent, SoundEvent> Sound, float Pitch, float Volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound.get(), Pitch, Volume));
        }
    }

    private void tap() {
        float Wobble = minecraft == null ? 0.0f : (minecraft.level == null ? 0.0f : minecraft.level.random.nextFloat() * 0.08f);
        play(GuidebookRegistry.TabletTap, 0.96f + Wobble, 0.6f);
        play(GuidebookRegistry.TabletTapClick, 1.0f + Wobble, 0.45f);
    }

    private void openPage(int Index) {
        List<GuidePage> Pages = GuideLibrary.pages();
        if (Pages.isEmpty()) {
            return;
        }
        int Target = Mth.clamp(Index, 0, Pages.size() - 1);
        if (Target != PageIndex) {
            PageIndex = Target;
            Scroll = 0.0;
            buildPage();
        }
        tap();
    }

    private static ItemStack stackOf(@Nullable ResourceLocation Id) {
        if (Id == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(Id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private void buildSidebar() {
        SideRows.clear();
        int Cursor = 0;
        for (GuideChapter Chapter : GuideLibrary.chapters()) {
            List<FormattedCharSequence> Heading = font.split(Component.literal(Chapter.Title().flat()).withStyle(BoldStyle), SidebarWidth - ChapterIndent - 3);
            int HeadingHeight = Heading.size() * LineHeight + 3;
            SideRows.add(new SideRow(null, Chapter.Pages().get(0), Cursor, HeadingHeight, Heading, ChapterIndent));
            Cursor += HeadingHeight;
            for (GuidePage Page : Chapter.Pages()) {
                List<FormattedCharSequence> Lines = font.split(Component.literal(Page.Title().flat()), SidebarWidth - PageIndent - 3);
                int RowHeight = Lines.size() * LineHeight + 1;
                SideRows.add(new SideRow(Page, Page, Cursor, RowHeight, Lines, PageIndent));
                Cursor += RowHeight;
            }
            Cursor += 4;
        }
        SideContentHeight = Cursor;
        SideScroll = Mth.clamp(SideScroll, 0.0, maxSideScroll());
    }

    private void buildPage() {
        Layout.clear();
        GuidePage Page = currentPage();
        if (Page == null) {
            List<FormattedCharSequence> Lines = font.split(Component.translatable(Prefix + "empty"), ViewWidth);
            Layout.add(new Laid(Lines.size() * LineHeight, (Graphics, X, Y, MouseX, MouseY, Live) -> drawLines(Graphics, Lines, X, Y, InkMuted)));
        } else {
            Layout.add(title(Page));
            int StepNumber = 0;
            for (GuideBlock Entry : Page.Blocks()) {
                if (Entry instanceof GuideBlock.Heading) {
                    StepNumber = 0;
                } else if (Entry instanceof GuideBlock.Step Listed && !Listed.Bullet()) {
                    StepNumber++;
                }
                Layout.add(lay(Entry, StepNumber));
            }
        }
        int Total = 0;
        for (Laid Piece : Layout) {
            Total += Piece.Height() + BlockGap;
        }
        ContentHeight = Math.max(0, Total - BlockGap + 2);
        Scroll = Mth.clamp(Scroll, 0.0, maxScroll());
    }

    private Laid title(GuidePage Page) {
        ItemStack Icon = stackOf(Page.Icon());
        int TextX = Icon.isEmpty() ? 0 : 20;
        List<FormattedCharSequence> Lines = font.split(Component.literal(Page.Title().flat()).withStyle(BoldStyle), ViewWidth - TextX);
        int TextHeight = Lines.size() * LineHeight - 1;
        int Band = Math.max(Icon.isEmpty() ? 0 : 16, TextHeight);
        int Height = Band + 4;
        return new Laid(Height, (Graphics, X, Y, MouseX, MouseY, Live) -> {
            if (!Icon.isEmpty()) {
                int IconY = Y + (Band - 16) / 2;
                Graphics.renderItem(Icon, X, IconY);
                hoverStack(Live, MouseX, MouseY, X, IconY, 16, 16, Icon);
            }
            drawLines(Graphics, Lines, X + TextX, Y + (Band - TextHeight) / 2, Accent);
            Graphics.fill(X, Y + Height - 1, X + ViewWidth, Y + Height, Rule);
        });
    }

    private Laid lay(GuideBlock Entry, int StepNumber) {
        return switch (Entry) {
            case GuideBlock.Paragraph Text -> {
                List<FormattedCharSequence> Lines = font.split(Text.Text().styled(AccentStyle), ViewWidth);
                yield new Laid(Lines.size() * LineHeight - 1, (Graphics, X, Y, MouseX, MouseY, Live) -> drawLines(Graphics, Lines, X, Y, Ink));
            }
            case GuideBlock.Heading Text -> {
                List<FormattedCharSequence> Lines = font.split(Component.literal(Text.Text().flat()).withStyle(BoldStyle), ViewWidth);
                yield new Laid(Lines.size() * LineHeight + 2, (Graphics, X, Y, MouseX, MouseY, Live) -> drawLines(Graphics, Lines, X, Y + 3, Accent));
            }
            case GuideBlock.Note Text -> {
                List<FormattedCharSequence> Lines = font.split(Text.Text().styled(AccentStyle), ViewWidth - 16);
                int Height = Lines.size() * LineHeight + 7;
                yield new Laid(Height, (Graphics, X, Y, MouseX, MouseY, Live) -> {
                    Graphics.fill(X, Y, X + ViewWidth, Y + Height, NoteFill);
                    Graphics.fill(X, Y, X + 2, Y + Height, NoteStripe);
                    Graphics.blit(Texture, X + 5, Y + 4, NoteU, NoteV, NoteSize, NoteSize, TextureWidth, TextureHeight);
                    drawLines(Graphics, Lines, X + 15, Y + 4, NoteInk);
                });
            }
            case GuideBlock.Step Text -> step(Text, StepNumber);
            case GuideBlock.Icons Row -> icons(Row);
            case GuideBlock.Picture Image -> picture(Image);
        };
    }

    private Laid step(GuideBlock.Step Text, int Number) {
        ItemStack Icon = stackOf(Text.Icon());
        String Label = Text.Bullet() ? "" : Integer.toString(Number);
        int BadgeWidth = Math.max(9, font.width(Label) + 3);
        int TextX = BadgeWidth + 3 + (Icon.isEmpty() ? 0 : 19);
        List<FormattedCharSequence> Lines = font.split(Text.Text().styled(AccentStyle), ViewWidth - TextX);
        int TextHeight = Lines.size() * LineHeight - 1;
        int Band = Math.max(Icon.isEmpty() ? 9 : 16, TextHeight);
        return new Laid(Band, (Graphics, X, Y, MouseX, MouseY, Live) -> {
            int Middle = Icon.isEmpty() ? 0 : (Math.min(Band, 16) - 9) / 2;
            if (Text.Bullet()) {
                Graphics.fill(X + 2, Y + Middle + 2, X + BadgeWidth - 2, Y + Middle + 7, Accent);
            } else {
                Graphics.fill(X, Y + Middle, X + BadgeWidth, Y + Middle + 9, Accent);
                Graphics.drawString(font, Label, X + (BadgeWidth - font.width(Label) + 1) / 2, Y + Middle + 1, AccentLight, false);
            }
            if (!Icon.isEmpty()) {
                Graphics.renderItem(Icon, X + BadgeWidth + 3, Y);
                hoverStack(Live, MouseX, MouseY, X + BadgeWidth + 3, Y, 16, 16, Icon);
            }
            int TextY = Icon.isEmpty() || TextHeight >= 16 ? Y : Y + (16 - TextHeight) / 2;
            drawLines(Graphics, Lines, X + TextX, TextY, Ink);
        });
    }

    private Laid icons(GuideBlock.Icons Row) {
        List<ItemStack> Stacks = new ArrayList<>();
        List<ResourceLocation> Targets = new ArrayList<>();
        for (int Index = 0; Index < Row.Items().size(); Index++) {
            ItemStack Stack = stackOf(Row.Items().get(Index));
            if (!Stack.isEmpty()) {
                Stacks.add(Stack);
                ResourceLocation Target = Row.linkAt(Index);
                Targets.add(Target != null && GuideLibrary.indexOf(Target) >= 0 ? Target : null);
            }
        }
        int Gap = Row.Flow() ? FlowGap : SlotGap;
        int PerRow = Math.max(1, (ViewWidth + Gap) / (SlotSize + Gap));
        int Rows = Math.max(1, (Stacks.size() + PerRow - 1) / PerRow);
        List<FormattedCharSequence> Caption = Row.Caption() == null ? List.of() : font.split(Row.Caption().styled(AccentStyle), ViewWidth);
        int SlotsHeight = Stacks.isEmpty() ? 0 : Rows * SlotSize + (Rows - 1) * SlotGap;
        int Height = SlotsHeight + (Caption.isEmpty() ? 0 : Caption.size() * LineHeight + 2);
        return new Laid(Height, (Graphics, X, Y, MouseX, MouseY, Live) -> {
            for (int Line = 0; Line < Rows; Line++) {
                int From = Line * PerRow;
                int Count = Math.min(PerRow, Stacks.size() - From);
                int Span = Count * SlotSize + (Count - 1) * Gap;
                int RowX = X + (ViewWidth - Span) / 2;
                int RowY = Y + Line * (SlotSize + SlotGap);
                for (int Index = 0; Index < Count; Index++) {
                    int SlotX = RowX + Index * (SlotSize + Gap);
                    ResourceLocation Target = Targets.get(From + Index);
                    boolean Over = Live && inside(MouseX, MouseY, SlotX, RowY, SlotSize, SlotSize);
                    if (Target == null) {
                        drawSlot(Graphics, SlotX, RowY);
                    } else {
                        drawLinkSlot(Graphics, SlotX, RowY, Over);
                        LinkSpots.add(new LinkSpot(SlotX, RowY, Target));
                        if (Over) {
                            HoverLink = Target;
                        }
                    }
                    ItemStack Stack = Stacks.get(From + Index);
                    Graphics.renderItem(Stack, SlotX + 1, RowY + 1);
                    hoverStack(Live, MouseX, MouseY, SlotX, RowY, SlotSize, SlotSize, Stack);
                    if (Row.Flow() && From + Index < Stacks.size() - 1) {
                        drawArrow(Graphics, SlotX + SlotSize + (Gap - 3) / 2, RowY + (SlotSize - 5) / 2);
                    }
                }
            }
            int CaptionY = Y + SlotsHeight + 2;
            for (FormattedCharSequence Text : Caption) {
                Graphics.drawString(font, Text, X + (ViewWidth - font.width(Text)) / 2, CaptionY, InkMuted, false);
                CaptionY += LineHeight;
            }
        });
    }

    private Laid picture(GuideBlock.Picture Image) {
        int DrawWidth = Math.min(Image.Width(), ViewWidth);
        int DrawHeight = Image.Width() <= ViewWidth ? Image.Height() : Image.Height() * ViewWidth / Image.Width();
        List<FormattedCharSequence> Caption = Image.Caption() == null ? List.of() : font.split(Image.Caption().styled(AccentStyle), ViewWidth);
        int Height = DrawHeight + (Caption.isEmpty() ? 0 : Caption.size() * LineHeight + 2);
        return new Laid(Height, (Graphics, X, Y, MouseX, MouseY, Live) -> {
            Graphics.blit(Image.Texture(), X + (ViewWidth - DrawWidth) / 2, Y, DrawWidth, DrawHeight, Image.U(), Image.V(), Image.Width(), Image.Height(),
                    Image.TextureWidth(), Image.TextureHeight());
            int CaptionY = Y + DrawHeight + 2;
            for (FormattedCharSequence Text : Caption) {
                Graphics.drawString(font, Text, X + (ViewWidth - font.width(Text)) / 2, CaptionY, InkMuted, false);
                CaptionY += LineHeight;
            }
        });
    }

    private void drawLines(GuiGraphics Graphics, List<FormattedCharSequence> Lines, int X, int Y, int Colour) {
        for (int Index = 0; Index < Lines.size(); Index++) {
            Graphics.drawString(font, Lines.get(Index), X, Y + Index * LineHeight, Colour, false);
        }
    }

    private void drawSlot(GuiGraphics Graphics, int X, int Y) {
        Graphics.fill(X, Y, X + SlotSize, Y + SlotSize, SlotFill);
        Graphics.fill(X, Y, X + SlotSize - 1, Y + 1, SlotDark);
        Graphics.fill(X, Y, X + 1, Y + SlotSize - 1, SlotDark);
        Graphics.fill(X + 1, Y + SlotSize - 1, X + SlotSize, Y + SlotSize, SlotLight);
        Graphics.fill(X + SlotSize - 1, Y + 1, X + SlotSize, Y + SlotSize, SlotLight);
    }

    private void drawLinkSlot(GuiGraphics Graphics, int X, int Y, boolean Over) {
        Graphics.fill(X, Y, X + SlotSize, Y + SlotSize, Over ? SideHover : SlotFill);
        Graphics.fill(X, Y, X + SlotSize - 1, Y + 1, Accent);
        Graphics.fill(X, Y, X + 1, Y + SlotSize - 1, Accent);
        Graphics.fill(X + 1, Y + SlotSize - 1, X + SlotSize, Y + SlotSize, SideSelected);
        Graphics.fill(X + SlotSize - 1, Y + 1, X + SlotSize, Y + SlotSize, SideSelected);
    }

    private void drawArrow(GuiGraphics Graphics, int X, int Y) {
        for (int Column = 0; Column < 3; Column++) {
            Graphics.fill(X + Column, Y + Column, X + Column + 1, Y + 5 - Column, Accent);
        }
    }

    private void hoverStack(boolean Live, int MouseX, int MouseY, int X, int Y, int Width, int Height, ItemStack Stack) {
        if (Live && inside(MouseX, MouseY, X, Y, Width, Height)) {
            HoverStack = Stack;
        }
    }

    private static boolean inside(double MouseX, double MouseY, int X, int Y, int Width, int Height) {
        return MouseX >= X && MouseX < X + Width && MouseY >= Y && MouseY < Y + Height;
    }

    private int maxScroll() {
        return Math.max(0, ContentHeight - ViewHeight);
    }

    private int maxSideScroll() {
        return Math.max(0, SideContentHeight - (ScreenHeight - StatusHeight - SideListTop - 2));
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        HoverStack = null;
        HoverText = null;
        HoverLink = null;
        LinkSpots.clear();
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, TextureWidth, TextureHeight);
        drawLed(Graphics, MouseX, MouseY);
        drawHome(Graphics, MouseX, MouseY);
        if (booting()) {
            drawBoot(Graphics);
        } else {
            drawStatus(Graphics);
            drawSidebar(Graphics, MouseX, MouseY);
            drawContent(Graphics, MouseX, MouseY);
            drawNav(Graphics, MouseX, MouseY);
        }
        if (HoverStack != null && HoverLink != null && minecraft != null) {
            List<Component> Lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, HoverStack));
            Lines.add(Component.translatable(Prefix + "open_page").withStyle(ChatFormatting.AQUA));
            Graphics.renderTooltip(font, Lines, HoverStack.getTooltipImage(), HoverStack, MouseX, MouseY);
        } else if (HoverStack != null) {
            Graphics.renderTooltip(font, HoverStack, MouseX, MouseY);
        } else if (HoverText != null) {
            Graphics.renderTooltip(font, HoverText, MouseX, MouseY);
        }
    }

    private void drawLed(GuiGraphics Graphics, int MouseX, int MouseY) {
        int Frame = booting() ? ((Ticks / 3) % 2 == 0 ? 1 : 2) : 0;
        Graphics.blit(Texture, Left + LedX, Top + LedY, LedU + Frame * (LedSize + 1), LedV, LedSize, LedSize, TextureWidth, TextureHeight);
        if (inside(MouseX, MouseY, Left + LedX - 2, Top + LedY - 2, LedSize + 4, LedSize + 4)) {
            HoverText = Component.translatable(Prefix + (booting() ? "led.booting" : "led.power"));
        }
    }

    private void drawHome(GuiGraphics Graphics, int MouseX, int MouseY) {
        boolean Over = inside(MouseX, MouseY, Left + HomeX, Top + HomeY, HomeSize, HomeSize);
        if (Ticks < HomePressedUntil) {
            Graphics.blit(Texture, Left + HomeX, Top + HomeY, HomeU + HomeSize + 2, HomeV, HomeSize, HomeSize, TextureWidth, TextureHeight);
        } else if (Over) {
            Graphics.blit(Texture, Left + HomeX, Top + HomeY, HomeU, HomeV, HomeSize, HomeSize, TextureWidth, TextureHeight);
        }
        if (Over) {
            HoverText = Component.translatable(Prefix + "home");
        }
    }

    private void drawBoot(GuiGraphics Graphics) {
        int X = Left + ScreenX;
        int Y = Top + ScreenY;
        Graphics.fill(X, Y, X + ScreenWidth, Y + ScreenHeight, BootDark);
        if (Ticks < 2) {
            return;
        }
        int Middle = X + ScreenWidth / 2;
        int EmblemY = Y + ScreenHeight / 2 - 32;
        Graphics.blit(Texture, Middle - EmblemSize / 2, EmblemY, EmblemU, EmblemV, EmblemSize, EmblemSize, TextureWidth, TextureHeight);
        Component Name = Component.translatable(Prefix + "title");
        Graphics.drawString(font, Name, Middle - font.width(Name) / 2, EmblemY + EmblemSize + 6, BootInk, false);
        int Lit = Mth.clamp((Ticks - 1) * BootSegments / Math.max(1, BootLength - 2), 0, BootSegments);
        int BarWidth = BootSegments * 6 - 1;
        int BarX = Middle - BarWidth / 2;
        int BarY = EmblemY + EmblemSize + 20;
        for (int Segment = 0; Segment < BootSegments; Segment++) {
            Graphics.fill(BarX + Segment * 6, BarY, BarX + Segment * 6 + 5, BarY + 3, Segment < Lit ? BootInk : BootDim);
        }
    }

    private void drawStatus(GuiGraphics Graphics) {
        int X = Left + ScreenX;
        int Y = Top + ScreenY;
        Graphics.drawString(font, title, X + 4, Y + 2, StatusInk, false);
        int BatteryX = X + ScreenWidth - 4 - BatteryWidth;
        Graphics.blit(Texture, BatteryX, Y + 3, BatteryU, BatteryV, BatteryWidth, BatteryHeight, TextureWidth, TextureHeight);
        String Clock = clock();
        Graphics.drawString(font, Clock, BatteryX - 4 - font.width(Clock), Y + 2, StatusDim, false);
    }

    private String clock() {
        if (minecraft == null || minecraft.level == null) {
            return "";
        }
        long Time = Math.floorMod(minecraft.level.getDayTime() + 6000L, 24000L);
        return String.format(Locale.ROOT, "%02d:%02d", Time / 1000L, (Time % 1000L) * 60L / 1000L);
    }

    private void drawSidebar(GuiGraphics Graphics, int MouseX, int MouseY) {
        int X = Left + ScreenX;
        int Y = Top + ScreenY + StatusHeight;
        Graphics.drawString(font, Component.translatable(Prefix + "contents"), X + ChapterIndent, Y + 4, InkMuted, false);
        int ListTop = Y + SideListTop;
        int ListBottom = Top + ScreenY + ScreenHeight - 2;
        boolean OverList = inside(MouseX, MouseY, X, ListTop, SidebarWidth, ListBottom - ListTop);
        GuidePage Current = currentPage();
        Graphics.enableScissor(X, ListTop, X + SidebarWidth, ListBottom);
        for (SideRow Row : SideRows) {
            int RowY = ListTop + Row.Y() - (int) SideScroll;
            if (RowY + Row.Height() < ListTop || RowY > ListBottom) {
                continue;
            }
            boolean Chapter = Row.Page() == null;
            boolean Selected = !Chapter && Row.Page() == Current;
            boolean Over = OverList && inside(MouseX, MouseY, X, RowY, SidebarWidth, Row.Height());
            int TextY = RowY + (Chapter ? 3 : 1);
            if (Selected) {
                Graphics.fill(X + 3, RowY, X + SidebarWidth - 3, RowY + Row.Height(), SideSelected);
            } else if (Over) {
                Graphics.fill(X + 3, Chapter ? RowY + 2 : RowY, X + SidebarWidth - 3, RowY + Row.Height(), SideHover);
            }
            drawLines(Graphics, Row.Lines(), X + Row.Indent(), TextY, Selected ? AccentLight : (Chapter ? Accent : Ink));
        }
        Graphics.disableScissor();
        if (maxSideScroll() > 0) {
            int Track = ListBottom - ListTop;
            int Thumb = Math.max(10, Track * Track / Math.max(1, SideContentHeight));
            int ThumbY = ListTop + (int) ((Track - Thumb) * SideScroll / maxSideScroll());
            Graphics.fill(X + SidebarWidth - 3, ThumbY, X + SidebarWidth - 1, ThumbY + Thumb, ScrollThumb);
        }
    }

    private void drawContent(GuiGraphics Graphics, int MouseX, int MouseY) {
        int X = Left + ViewX;
        int Y = Top + ViewY;
        boolean Live = inside(MouseX, MouseY, X, Y, ViewWidth, ViewHeight);
        Graphics.enableScissor(X - 2, Y, X + ViewWidth + 2, Y + ViewHeight);
        int Cursor = Y - (int) Scroll;
        for (Laid Piece : Layout) {
            if (Cursor + Piece.Height() >= Y && Cursor <= Y + ViewHeight) {
                Piece.Draw().draw(Graphics, X, Cursor, MouseX, MouseY, Live);
            }
            Cursor += Piece.Height() + BlockGap;
        }
        Graphics.disableScissor();
        if (maxScroll() > 0) {
            int TrackX = Left + ScrollX;
            Graphics.fill(TrackX, Y, TrackX + ScrollWidth, Y + ViewHeight, ScrollTrack);
            int Thumb = thumbHeight();
            int ThumbY = Y + (int) ((ViewHeight - Thumb) * Scroll / maxScroll());
            Graphics.fill(TrackX, ThumbY, TrackX + ScrollWidth, ThumbY + Thumb, ScrollThumb);
        }
    }

    private int thumbHeight() {
        return Math.max(12, ViewHeight * ViewHeight / Math.max(1, ContentHeight));
    }

    private void drawNav(GuiGraphics Graphics, int MouseX, int MouseY) {
        int Count = GuideLibrary.pages().size();
        boolean CanBack = PageIndex > 0;
        boolean CanForward = PageIndex < Count - 1;
        drawArrowButton(Graphics, MouseX, MouseY, PrevX, 0, CanBack, "previous");
        drawArrowButton(Graphics, MouseX, MouseY, NextX, 1, CanForward, "next");
        if (Count > 0) {
            Component Label = Component.translatable(Prefix + "page", PageIndex + 1, Count);
            int Middle = Left + ContentX + ContentWidth / 2;
            Graphics.drawString(font, Label, Middle - font.width(Label) / 2, Top + NavY + 4, InkMuted, false);
        }
    }

    private void drawArrowButton(GuiGraphics Graphics, int MouseX, int MouseY, int ButtonX, int Row, boolean Enabled, String Hint) {
        boolean Over = inside(MouseX, MouseY, Left + ButtonX, Top + NavButtonY, ArrowSize, ArrowSize);
        int State = !Enabled ? 2 : (Over ? 1 : 0);
        Graphics.blit(Texture, Left + ButtonX, Top + NavButtonY, ArrowU + State * (ArrowSize + 1), ArrowV + Row * (ArrowSize + 1), ArrowSize, ArrowSize,
                TextureWidth, TextureHeight);
        if (Over && Enabled) {
            HoverText = Component.translatable(Prefix + Hint);
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (booting()) {
            Ticks = Math.max(Ticks, BootLength - 1);
            return true;
        }
        if (Button == 0) {
            if (inside(MouseX, MouseY, Left + HomeX, Top + HomeY, HomeSize, HomeSize)) {
                HomePressedUntil = Ticks + PressTicks;
                openPage(0);
                return true;
            }
            int Count = GuideLibrary.pages().size();
            if (inside(MouseX, MouseY, Left + PrevX, Top + NavButtonY, ArrowSize, ArrowSize) && PageIndex > 0) {
                openPage(PageIndex - 1);
                return true;
            }
            if (inside(MouseX, MouseY, Left + NextX, Top + NavButtonY, ArrowSize, ArrowSize) && PageIndex < Count - 1) {
                openPage(PageIndex + 1);
                return true;
            }
            if (clickSidebar(MouseX, MouseY)) {
                return true;
            }
            if (clickLink(MouseX, MouseY)) {
                return true;
            }
            if (maxScroll() > 0 && inside(MouseX, MouseY, Left + ScrollX - 1, Top + ViewY, ScrollWidth + 2, ViewHeight)) {
                DraggingThumb = true;
                dragTo(MouseY);
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    private boolean clickSidebar(double MouseX, double MouseY) {
        int X = Left + ScreenX;
        int ListTop = Top + ScreenY + StatusHeight + SideListTop;
        int ListBottom = Top + ScreenY + ScreenHeight - 2;
        if (!inside(MouseX, MouseY, X, ListTop, SidebarWidth, ListBottom - ListTop)) {
            return false;
        }
        for (SideRow Row : SideRows) {
            int RowY = ListTop + Row.Y() - (int) SideScroll;
            if (inside(MouseX, MouseY, X, RowY, SidebarWidth, Row.Height())) {
                openPage(GuideLibrary.indexOf(Row.Target().Id()));
                return true;
            }
        }
        return false;
    }

    private boolean clickLink(double MouseX, double MouseY) {
        if (!inside(MouseX, MouseY, Left + ViewX, Top + ViewY, ViewWidth, ViewHeight)) {
            return false;
        }
        for (LinkSpot Spot : LinkSpots) {
            int Target = GuideLibrary.indexOf(Spot.Target());
            if (Target >= 0 && inside(MouseX, MouseY, Spot.X(), Spot.Y(), SlotSize, SlotSize)) {
                openPage(Target);
                return true;
            }
        }
        return false;
    }

    private void dragTo(double MouseY) {
        int Track = ViewHeight - thumbHeight();
        if (Track > 0) {
            double Fraction = (MouseY - (Top + ViewY) - thumbHeight() / 2.0) / Track;
            Scroll = Mth.clamp(Fraction * maxScroll(), 0.0, maxScroll());
        }
    }

    @Override
    public boolean mouseDragged(double MouseX, double MouseY, int Button, double DragX, double DragY) {
        if (DraggingThumb) {
            dragTo(MouseY);
            return true;
        }
        return super.mouseDragged(MouseX, MouseY, Button, DragX, DragY);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        DraggingThumb = false;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double DeltaX, double DeltaY) {
        if (booting()) {
            return true;
        }
        if (inside(MouseX, MouseY, Left + ScreenX, Top + ScreenY + StatusHeight, SidebarWidth, ScreenHeight - StatusHeight)) {
            SideScroll = Mth.clamp(SideScroll - DeltaY * ScrollStep, 0.0, maxSideScroll());
        } else {
            Scroll = Mth.clamp(Scroll - DeltaY * ScrollStep, 0.0, maxScroll());
        }
        return true;
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(Key, ScanCode)) {
            onClose();
            return true;
        }
        if (!booting()) {
            int Count = GuideLibrary.pages().size();
            switch (Key) {
                case GLFW.GLFW_KEY_LEFT -> {
                    if (PageIndex > 0) {
                        openPage(PageIndex - 1);
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (PageIndex < Count - 1) {
                        openPage(PageIndex + 1);
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    Scroll = Mth.clamp(Scroll - ScrollStep, 0.0, maxScroll());
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    Scroll = Mth.clamp(Scroll + ScrollStep, 0.0, maxScroll());
                    return true;
                }
                case GLFW.GLFW_KEY_PAGE_UP -> {
                    Scroll = Mth.clamp(Scroll - ViewHeight + LineHeight, 0.0, maxScroll());
                    return true;
                }
                case GLFW.GLFW_KEY_PAGE_DOWN -> {
                    Scroll = Mth.clamp(Scroll + ViewHeight - LineHeight, 0.0, maxScroll());
                    return true;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    HomePressedUntil = Ticks + PressTicks;
                    openPage(0);
                    return true;
                }
                default -> {
                }
            }
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    @FunctionalInterface
    private interface Drawer {
        void draw(GuiGraphics Graphics, int X, int Y, int MouseX, int MouseY, boolean Live);
    }

    private record Laid(int Height, Drawer Draw) {
    }

    private record LinkSpot(int X, int Y, ResourceLocation Target) {
    }

    private record SideRow(@Nullable GuidePage Page, GuidePage Target, int Y, int Height, List<FormattedCharSequence> Lines, int Indent) {
    }
}
