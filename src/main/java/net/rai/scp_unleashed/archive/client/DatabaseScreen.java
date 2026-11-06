package net.rai.scp_unleashed.archive.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.database.DatabaseActions;
import net.rai.scp_unleashed.archive.database.DatabaseCopyItem;
import net.rai.scp_unleashed.archive.database.DatabaseRecord;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.network.DatabaseCopyPayload;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class DatabaseScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/archive/database.png");
    public static final int TextureSize = 512;
    public static final int PanelWidth = 280;
    public static final int PanelHeight = 200;
    public static final int ListX = 8;
    public static final int ListY = 26;
    public static final int ListWidth = 132;
    public static final int RowHeight = 12;
    public static final int VisibleRows = 12;
    public static final int GlassTop = 24;
    public static final int GlassBottom = 172;
    public static final int TrackX = 142;
    public static final int TrackY = 24;
    public static final int TrackHeight = 148;
    public static final int CardX = 154;
    public static final int CardY = 20;
    public static final int CardTextX = 160;
    public static final int CardTextWidth = 110;
    public static final int ButtonY = 155;
    public static final int PrintX = 154;
    public static final int ReadX = 217;
    public static final int ButtonWidth = 59;
    public static final int ButtonHeight = 18;
    private static final int[] RowSelected = {0, 208, 132, 12};
    private static final int[] RowHover = {0, 220, 132, 12};
    private static final int[] Scanline = {0, 232, 132, 4};
    private static final int[] Thumb = {136, 208, 4, 18};
    private static final int[] ThumbHover = {140, 208, 4, 18};
    private static final int[] Chip = {144, 208, 4, 8};
    private static final int[] LampGreenOff = {148, 208, 5, 5};
    private static final int[] LampGreenOn = {153, 208, 5, 5};
    private static final int[] LampAmberOff = {158, 208, 5, 5};
    private static final int[] LampAmberOn = {163, 208, 5, 5};
    private static final int[] LampRedOff = {168, 208, 5, 5};
    private static final int[] LampRedOn = {173, 208, 5, 5};
    private static final int[] EmptyFolder = {180, 208, 22, 18};
    private static final int[] Button = {0, 240, 59, 18};
    private static final int[] ButtonHover = {59, 240, 59, 18};
    private static final int[] ButtonPressed = {118, 240, 59, 18};
    private static final int[] ButtonDisabled = {177, 240, 59, 18};
    private static final int[] CardBand = {0, 260, 110, 5};
    private static final int Phosphor = 0xFF9CF5B0;
    private static final int PhosphorDim = 0xFF3E9A5C;
    private static final int PhosphorTitle = 0xFF6FD08A;
    private static final int PhosphorHot = 0xFFE8FFE8;
    private static final int PhosphorDark = 0xFF0B2614;
    private static final int AmberText = 0xFFFFC857;
    private static final int AmberDim = 0xFF8A6420;
    private static final int InkMain = 0xFF2A221B;
    private static final int InkSoft = 0xFF5A4C3C;
    private static final int ButtonText = 0xFFEDE6D2;
    private static final int ButtonTextOff = 0xFF6E6A62;
    private static final int FieldsY = 68;
    private static final int NotesLimit = 112;
    private static final int FooterY = 184;
    private static final int FlashTicks = 40;
    private static final int PressTicks = 6;
    private static final int DoubleClickTicks = 8;

    private final BlockPos Pos;
    private final boolean Main;
    private int Left;
    private int Top;
    private int Scroll;
    private int Ticks;
    private int FlashUntil = -1;
    private int PrintPressedUntil = -1;
    private int LastClickTick = -100;
    private boolean Dragging;
    @Nullable
    private ResourceLocation Selected;
    @Nullable
    private String FlashDesignation;

    public DatabaseScreen(BlockPos Pos, boolean Main) {
        super(Component.translatable(Main ? "gui.scp_unleashed.database.title" : "gui.scp_unleashed.database.terminal_title"));
        this.Pos = Pos;
        this.Main = Main;
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
        DatabaseRecords.resort();
        List<DatabaseRecord> Records = DatabaseRecords.records();
        if (Selected == null && !Records.isEmpty()) {
            Selected = Records.getFirst().Document();
        }
        clampScroll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Ticks++;
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        double Limit = DatabaseActions.Reach + 1.0;
        if (minecraft.player.distanceToSqr(Vec3.atCenterOf(Pos)) > Limit * Limit || !DatabaseActions.isUnit(minecraft.level.getBlockState(Pos))) {
            onClose();
            return;
        }
        if (Selected != null && DatabaseRecords.find(Selected).isEmpty()) {
            List<DatabaseRecord> Records = DatabaseRecords.records();
            Selected = Records.isEmpty() ? null : Records.getFirst().Document();
        }
        clampScroll();
    }

    private void play(SoundEvent Sound, float Pitch, float Volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, Volume));
        }
    }

    private int maxScroll() {
        return Math.max(0, DatabaseRecords.count() - VisibleRows);
    }

    private void clampScroll() {
        Scroll = Mth.clamp(Scroll, 0, maxScroll());
    }

    private int selectedIndex() {
        List<DatabaseRecord> Records = DatabaseRecords.records();
        for (int Index = 0; Index < Records.size(); Index++) {
            if (Records.get(Index).Document().equals(Selected)) {
                return Index;
            }
        }
        return -1;
    }

    @Nullable
    private DatabaseRecord selected() {
        return Selected == null ? null : DatabaseRecords.find(Selected).orElse(null);
    }

    private void select(int Index) {
        List<DatabaseRecord> Records = DatabaseRecords.records();
        if (Records.isEmpty()) {
            return;
        }
        int Clamped = Mth.clamp(Index, 0, Records.size() - 1);
        ResourceLocation Next = Records.get(Clamped).Document();
        if (!Next.equals(Selected)) {
            Selected = Next;
            play(ArchiveRegistry.Keystroke.get(), 1.2f + (Clamped % 3) * 0.05f, 0.25f);
        }
        if (Clamped < Scroll) {
            Scroll = Clamped;
        } else if (Clamped >= Scroll + VisibleRows) {
            Scroll = Clamped - VisibleRows + 1;
        }
        clampScroll();
    }

    private void printCopy() {
        DatabaseRecord Entry = selected();
        if (Entry == null) {
            play(ArchiveRegistry.Deny.get(), 1.0f, 0.4f);
            return;
        }
        PacketDistributor.sendToServer(new DatabaseCopyPayload(Pos, Entry.Document()));
        PrintPressedUntil = Ticks + PressTicks;
        FlashUntil = Ticks + FlashTicks;
        FlashDesignation = DatabaseRecords.designation(Entry);
        play(ArchiveRegistry.Keystroke.get(), 0.8f, 0.5f);
    }

    private void readSelected() {
        DatabaseRecord Entry = selected();
        if (Entry == null || minecraft == null) {
            play(ArchiveRegistry.Deny.get(), 1.0f, 0.4f);
            return;
        }
        minecraft.setScreen(new DossierScreen(DatabaseCopyItem.create(Entry), this));
    }

    private boolean inside(double MouseX, double MouseY, int X, int Y, int Width, int Height) {
        return MouseX >= Left + X && MouseX < Left + X + Width && MouseY >= Top + Y && MouseY < Top + Y + Height;
    }

    private int rowAt(double MouseX, double MouseY) {
        if (!inside(MouseX, MouseY, ListX, ListY, ListWidth, VisibleRows * RowHeight)) {
            return -1;
        }
        int Index = Scroll + (int) ((MouseY - Top - ListY) / RowHeight);
        return Index < DatabaseRecords.count() ? Index : -1;
    }

    private int thumbHeight() {
        return Thumb[3];
    }

    private int thumbY() {
        int Range = TrackHeight - thumbHeight();
        return TrackY + (maxScroll() == 0 ? 0 : Range * Scroll / maxScroll());
    }

    private void dragTo(double MouseY) {
        int Range = TrackHeight - thumbHeight();
        if (Range <= 0) {
            return;
        }
        double Fraction = (MouseY - Top - TrackY - thumbHeight() / 2.0) / Range;
        Scroll = Mth.clamp((int) Math.round(Fraction * maxScroll()), 0, maxScroll());
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0) {
            int Row = rowAt(MouseX, MouseY);
            if (Row >= 0) {
                boolean Again = DatabaseRecords.records().get(Row).Document().equals(Selected) && Ticks - LastClickTick <= DoubleClickTicks;
                select(Row);
                LastClickTick = Ticks;
                if (Again) {
                    readSelected();
                }
                return true;
            }
            if (inside(MouseX, MouseY, TrackX, TrackY, Thumb[2], TrackHeight) && maxScroll() > 0) {
                Dragging = true;
                dragTo(MouseY);
                return true;
            }
            if (inside(MouseX, MouseY, PrintX, ButtonY, ButtonWidth, ButtonHeight)) {
                printCopy();
                return true;
            }
            if (inside(MouseX, MouseY, ReadX, ButtonY, ButtonWidth, ButtonHeight)) {
                readSelected();
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseDragged(double MouseX, double MouseY, int Button, double DragX, double DragY) {
        if (Dragging) {
            dragTo(MouseY);
            return true;
        }
        return super.mouseDragged(MouseX, MouseY, Button, DragX, DragY);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        Dragging = false;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        if (ScrollY != 0 && maxScroll() > 0) {
            Scroll = Mth.clamp(Scroll - (int) Math.signum(ScrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        int Index = selectedIndex();
        if (Key == GLFW.GLFW_KEY_DOWN || Key == GLFW.GLFW_KEY_S) {
            select(Index + 1);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_UP || Key == GLFW.GLFW_KEY_W) {
            select(Math.max(0, Index - 1));
            return true;
        }
        if (Key == GLFW.GLFW_KEY_PAGE_DOWN) {
            select(Index + VisibleRows);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_PAGE_UP) {
            select(Math.max(0, Index - VisibleRows));
            return true;
        }
        if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
            readSelected();
            return true;
        }
        if (Key == GLFW.GLFW_KEY_P) {
            printCopy();
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(Key, ScanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    private void blit(GuiGraphics Graphics, int[] Sprite, int X, int Y) {
        Graphics.blit(Texture, X, Y, Sprite[0], Sprite[1], Sprite[2], Sprite[3], TextureSize, TextureSize);
    }

    private void tinted(GuiGraphics Graphics, int[] Sprite, int X, int Y, int Colour) {
        RenderSystem.setShaderColor(((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f, 1.0f);
        blit(Graphics, Sprite, X, Y);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private String fitted(String Text, int Width) {
        if (font.width(Text) <= Width) {
            return Text;
        }
        String Ellipsis = "...";
        return font.plainSubstrByWidth(Text, Math.max(0, Width - font.width(Ellipsis))).stripTrailing() + Ellipsis;
    }

    private void centred(GuiGraphics Graphics, Component Text, int CentreX, int Y, int Colour) {
        Graphics.drawString(font, Text, CentreX - font.width(Text) / 2, Y, Colour, false);
    }

    private String title(DatabaseRecord Entry) {
        AnomalyDocument Document = AnomalyDocuments.client().get(Entry.Document());
        return Document == null ? Component.translatable("gui.scp_unleashed.database.unknown_title").getString() : Document.title().getString();
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, TextureSize, TextureSize);
        drawHeader(Graphics);
        drawList(Graphics, MouseX, MouseY);
        drawCard(Graphics);
        drawButtons(Graphics, MouseX, MouseY);
        drawFooter(Graphics);
        RenderSystem.disableBlend();
    }

    private void drawHeader(GuiGraphics Graphics) {
        Graphics.drawString(font, title, Left + 10, Top + 7, AmberText, false);
        String Count = Component.translatable("gui.scp_unleashed.database.count", String.format("%04d", DatabaseRecords.count())).getString();
        Graphics.drawString(font, Count, Left + 244 - font.width(Count), Top + 7, AmberDim, false);
        boolean Blink = (Ticks / 10) % 2 == 0;
        blit(Graphics, Blink ? LampGreenOn : LampGreenOff, Left + 250, Top + 9);
        blit(Graphics, FlashUntil > Ticks && Blink ? LampAmberOn : LampAmberOff, Left + 258, Top + 9);
        blit(Graphics, Main ? LampRedOff : LampRedOn, Left + 266, Top + 9);
    }

    private void drawList(GuiGraphics Graphics, int MouseX, int MouseY) {
        List<DatabaseRecord> Records = DatabaseRecords.records();
        int X = Left + ListX;
        for (int Y = GlassTop; Y < GlassBottom; Y += Scanline[3]) {
            blit(Graphics, Scanline, X, Top + Y);
        }
        if (Records.isEmpty()) {
            int Centre = X + ListWidth / 2;
            blit(Graphics, EmptyFolder, Centre - EmptyFolder[2] / 2, Top + 64);
            centred(Graphics, Component.translatable("gui.scp_unleashed.database.empty"), Centre, Top + 90, Phosphor);
            List<FormattedCharSequence> Lines = font.split(Component.translatable(Main ? "gui.scp_unleashed.database.empty_hint" : "gui.scp_unleashed.database.empty_hint_terminal"), ListWidth - 12);
            for (int Line = 0; Line < Lines.size() && Line < 5; Line++) {
                Graphics.drawString(font, Lines.get(Line), Centre - font.width(Lines.get(Line)) / 2, Top + 104 + Line * 10, PhosphorDim, false);
            }
        } else {
            int Hovered = rowAt(MouseX, MouseY);
            for (int Row = 0; Row < VisibleRows; Row++) {
                int Index = Scroll + Row;
                if (Index >= Records.size()) {
                    break;
                }
                DatabaseRecord Entry = Records.get(Index);
                int Y = Top + ListY + Row * RowHeight;
                boolean IsSelected = Entry.Document().equals(Selected);
                if (IsSelected) {
                    blit(Graphics, RowSelected, X, Y);
                } else if (Index == Hovered) {
                    blit(Graphics, RowHover, X, Y);
                }
                tinted(Graphics, Chip, X + 3, Y + 2, Entry.Classification().getColour());
                String Designation = DatabaseRecords.designation(Entry);
                int TextX = X + 10;
                Graphics.drawString(font, Designation, TextX, Y + 2, IsSelected ? PhosphorDark : Phosphor, false);
                int TitleX = TextX + font.width(Designation) + 5;
                int Room = X + ListWidth - 4 - TitleX;
                if (Room > 12) {
                    Graphics.drawString(font, fitted(title(Entry), Room), TitleX, Y + 2, IsSelected ? PhosphorDark : PhosphorTitle, false);
                }
            }
        }
        if (maxScroll() > 0) {
            boolean Hot = Dragging || inside(MouseX, MouseY, TrackX, TrackY, Thumb[2], TrackHeight);
            int[] Sprite = Hot ? ThumbHover : Thumb;
            blit(Graphics, Sprite, Left + TrackX, Top + thumbY());
        }
    }

    private void drawCard(GuiGraphics Graphics) {
        DatabaseRecord Entry = selected();
        int X = Left + CardTextX;
        if (Entry == null) {
            centred(Graphics, Component.translatable("gui.scp_unleashed.database.no_selection"), Left + CardX + 61, Top + 70, InkSoft);
            return;
        }
        tinted(Graphics, CardBand, Left + CardX + 4, Top + CardY + 4, Entry.Classification().getColour());
        String Designation = DatabaseRecords.designation(Entry);
        float Scale = Math.min(2.0f, (float) CardTextWidth / Math.max(1, font.width(Designation)));
        Scale = Scale >= 2.0f ? 2.0f : 1.0f;
        Graphics.pose().pushPose();
        Graphics.pose().translate(X, Top + CardY + 14, 0.0f);
        Graphics.pose().scale(Scale, Scale, 1.0f);
        Graphics.drawString(font, Designation, 0, 0, InkMain, false);
        Graphics.pose().popPose();
        int Y = Top + CardY + 14 + (int) (9 * Scale) + 3;
        List<FormattedText> TitleLines = font.getSplitter().splitLines(title(Entry), CardTextWidth, Style.EMPTY);
        for (int Line = 0; Line < TitleLines.size() && Line < 3; Line++) {
            String Text = TitleLines.get(Line).getString();
            if (Line == 2 && TitleLines.size() > 3) {
                Text = fitted(Text + " " + TitleLines.get(3).getString(), CardTextWidth);
            }
            Graphics.drawString(font, Text, X, Y, InkSoft, false);
            Y += 10;
        }
        Y = Math.max(Y + 3, Top + CardY + FieldsY);
        Component ClassName = Entry.Classification().getName().copy().withStyle(Look -> Look.withColor(Entry.Classification().getColour() & 0xFFFFFF).withBold(true));
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.database.class", ClassName), X, Y, InkMain, false);
        Y += 11;
        AnomalyDocument Document = AnomalyDocuments.client().get(Entry.Document());
        if (Document != null) {
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.database.threat", Document.dangerNumeral()), X, Y, InkMain, false);
            Y += 11;
        }
        String Archivist = Entry.Archivist().isEmpty() ? Component.translatable("gui.scp_unleashed.database.unknown_archivist").getString() : Entry.Archivist();
        Graphics.drawString(font, fitted(Component.translatable("gui.scp_unleashed.database.filed_by", Archivist).getString(), CardTextWidth), X, Y, InkSoft, false);
        Y += 11;
        if (Entry.Day() > 0) {
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.database.filed_day", Entry.Day()), X, Y, InkSoft, false);
            Y += 11;
        }
        int NoteCount = Entry.Notes().Lines().size();
        Component Notes = Entry.Notes().Resolved() ? Component.translatable("gui.scp_unleashed.database.notes_located")
                : NoteCount == 0 ? Component.translatable("gui.scp_unleashed.database.notes_none")
                : Component.translatable("gui.scp_unleashed.database.notes", NoteCount);
        Graphics.drawString(font, Notes, X, Math.min(Y, Top + CardY + NotesLimit), InkSoft, false);
    }

    private void drawButtons(GuiGraphics Graphics, int MouseX, int MouseY) {
        boolean Ready = selected() != null;
        drawButton(Graphics, PrintX, Component.translatable("gui.scp_unleashed.database.print"), Ready, PrintPressedUntil > Ticks, MouseX, MouseY);
        drawButton(Graphics, ReadX, Component.translatable("gui.scp_unleashed.database.read"), Ready, false, MouseX, MouseY);
    }

    private void drawButton(GuiGraphics Graphics, int X, Component Label, boolean Ready, boolean Pressed, int MouseX, int MouseY) {
        boolean Hover = Ready && inside(MouseX, MouseY, X, ButtonY, ButtonWidth, ButtonHeight);
        int[] Sprite = !Ready ? ButtonDisabled : Pressed ? ButtonPressed : Hover ? ButtonHover : Button;
        blit(Graphics, Sprite, Left + X, Top + ButtonY);
        int Drop = Pressed ? 1 : 0;
        Graphics.drawString(font, Label, Left + X + (ButtonWidth - font.width(Label)) / 2, Top + ButtonY + 5 + Drop, Ready ? ButtonText : ButtonTextOff, Ready);
    }

    private void drawFooter(GuiGraphics Graphics) {
        Component Hint;
        int Colour;
        if (FlashUntil > Ticks && FlashDesignation != null) {
            Hint = Component.translatable("gui.scp_unleashed.database.printed", FlashDesignation);
            Colour = PhosphorHot;
        } else {
            Hint = Component.translatable(Main ? "gui.scp_unleashed.database.hint" : "gui.scp_unleashed.database.hint_terminal");
            Colour = PhosphorDim;
        }
        Graphics.drawString(font, fitted(Hint.getString(), PanelWidth - 20), Left + 10, Top + FooterY, Colour, false);
    }
}
