package net.rai.scp_unleashed.archive.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalyDocumentItem;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.DossierClearance;
import net.rai.scp_unleashed.archive.document.DossierContent;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.lang.LangIndex;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DossierScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/archive/dossier.png");
    private static final int TextureSize = 512;
    private static final int SpreadWidth = 432;
    private static final int SpreadHeight = 248;
    private static final int Spine = 216;
    private static final int[] Cover = {0, 256, 216, 248};
    private static final int[] TapeTile = {216, 256, 16, 14};
    private static final int[] WaxSeal = {232, 256, 26, 26};
    private static final int[] WaxLeft = {258, 256, 13, 26};
    private static final int[] WaxRight = {271, 256, 13, 26};
    private static final int[] Emblem = {284, 256, 26, 26};
    private static final int[] StampBig = {216, 284, 112, 26};
    private static final int[] StampBigDistress = {328, 284, 112, 26};
    private static final int[] ArrowLeft = {216, 332, 14, 12};
    private static final int[] ArrowLeftHover = {230, 332, 14, 12};
    private static final int[] ArrowRight = {244, 332, 14, 12};
    private static final int[] ArrowRightHover = {258, 332, 14, 12};
    private static final int[] Paperclip = {272, 332, 9, 22};
    private static final int[] PhotoGrain = {216, 360, 160, 120};
    private static final int[] Pip = {310, 256, 7, 7};
    private static final int[] PipEmpty = {317, 256, 7, 7};
    private static final int RightX0 = 224;
    private static final int RightX1 = 422;
    private static final int SheetY0 = 8;
    private static final int SheetY1 = 240;
    private static final int PhotoX = 124;
    private static final int PhotoY = 54;
    private static final int PhotoWidth = 76;
    private static final int PhotoHeight = 64;
    private static final int GrainWidth = 68;
    private static final int GrainHeight = 48;
    private static final int ClipX = PhotoX + 26;
    private static final int ClipY = PhotoY - 6;
    private static final int LeftColumn = PhotoX - 6 - 18;
    private static final int TitleRows = 2;
    private static final String Ellipsis = "...";
    private static final int NotesX = 14;
    private static final int NotesY = 172;
    private static final int NotesWidth = 192;
    private static final int NotesRows = 5;
    private static final int BodyX = 230;
    private static final int BodyY = 33;
    private static final int BodyWidth = 184;
    private static final int BodyBottom = 219;
    private static final int MaxTop = BodyBottom - 8 - BodyY;
    private static final Style BoldStyle = Style.EMPTY.withBold(true);
    private static final int ArrowY = 226;
    private static final int ArrowLeftX = 228;
    private static final int ArrowRightX = 404;
    private static final int InkMain = 0xFF261F1A;
    private static final int InkSoft = 0xFF4E4236;
    private static final int RedactionColour = 0xFF14110F;
    private static final int DeclassifiedInk = 0xFF7A2820;
    private static final int DeclassifiedSoft = 0xFF8E4436;
    private static final int DeclassifiedWash = 0x287A2820;
    private static final int DeclassifiedBracket = 0xC07A2820;
    private static final int BracketArm = 3;
    private static final int RiseTicks = 8;
    private static final int SealAt = 12;
    private static final int TearTicks = 6;
    private static final int OpenStart = 20;
    private static final int OpenEnd = 30;
    private static final int StampAt = 32;
    private static final int StampSlam = 3;
    private static final int TypeStart = 34;
    private static final int TypeRate = 14;
    private static final int TurnTicks = 5;
    private static final int GiveUpTicks = 60;
    private static final int Finished = 100000;
    private static final int Never = -100000;
    private static final int PictureX = 232;
    private static final int PictureY = 34;
    private static final int PictureSize = 180;
    private static final int LegendWidth = 184;
    private static final int LegendHeight = 48;

    private final InteractionHand Hand;
    private final boolean Reveal;
    @Nullable
    private final ItemStack Virtual;
    @Nullable
    private final Screen Parent;
    private final RandomSource Noise = RandomSource.create();
    private final List<int[]> Redactions = new ArrayList<>();
    private int Ticks;
    private int Clock;
    private int Waiting;
    private int Left;
    private int Top;
    private int Page;
    private int TurnedAt = Never;
    private int Typed;
    private int HeldClearance;
    private boolean Declassified;
    private ResourceLocation LoadedId;
    private int LoadedGeneration = -1;
    @Nullable
    private DossierContent Content;
    @Nullable
    private ResourceLocation PhotoTexture;
    private List<List<DossierLayout.Line>> Pages = List.of(List.of());
    private List<ResourceLocation> Pictures = List.of();
    @Nullable
    private ResourceLocation Legend;
    private boolean Pictorial;
    @Nullable
    private ResourceLocation CoverId;
    private boolean CoverPictorial;

    public DossierScreen(InteractionHand Hand, boolean Reveal) {
        this(Hand, Reveal, null, null);
    }

    public DossierScreen(ItemStack Virtual, @Nullable Screen Parent) {
        this(InteractionHand.MAIN_HAND, false, Virtual, Parent);
    }

    private DossierScreen(InteractionHand Hand, boolean Reveal, @Nullable ItemStack Virtual, @Nullable Screen Parent) {
        super(Component.translatable("gui.scp_unleashed.dossier.title"));
        this.Hand = Hand;
        this.Reveal = Reveal;
        this.Virtual = Virtual;
        this.Parent = Parent;
        if (!Reveal) {
            Clock = Finished;
            Typed = Integer.MAX_VALUE;
        }
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - SpreadWidth) / 2;
        Top = (height - SpreadHeight) / 2;
        if (Ticks == 0) {
            checkClearance();
        }
        if (!Reveal && Ticks == 0) {
            play(ArchiveRegistry.Page, 1.1f, 0.6f);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void play(DeferredHolder<SoundEvent, SoundEvent> Sound, float Pitch, float Volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound.get(), Pitch, Volume));
        }
    }

    private void checkClearance() {
        HeldClearance = DossierClearance.highest(minecraft == null ? null : minecraft.player);
    }

    private ItemStack held() {
        if (Virtual != null) {
            return Virtual;
        }
        return minecraft == null || minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(Hand);
    }

    private boolean available() {
        ItemStack Stack = held();
        return Stack.getItem() instanceof AnomalyDocumentItem && Stack.get(ArchiveRegistry.StampComponent.get()) != null;
    }

    private String coverDesignation() {
        AnomalyDocument Known = AnomalyDocumentItem.lookup(held());
        return Known == null ? Component.translatable("gui.scp_unleashed.dossier.unknown").getString() : Known.designation().getString();
    }

    private DocumentClass sealedClass() {
        DocumentStamp Stamp = held().get(ArchiveRegistry.StampComponent.get());
        return Stamp == null ? DocumentClass.Euclid : Stamp.Classification();
    }

    @Override
    public void tick() {
        super.tick();
        Ticks++;
        boolean Ready = available();
        if (!Reveal && !Ready) {
            onClose();
            return;
        }
        if (Reveal && Clock < Finished) {
            if (Clock < SealAt - 1 || Ready) {
                Clock++;
                Waiting = 0;
                revealSounds();
            } else if (++Waiting > GiveUpTicks) {
                onClose();
                return;
            }
            if (Clock >= TypeStart + 400) {
                Clock = Finished;
            }
        }
        if (Clock >= TypeStart && Typed < Integer.MAX_VALUE) {
            int Before = Typed;
            Typed += TypeRate;
            if (Typed / 18 != Before / 18 && Typed < pageLength(0)) {
                play(ArchiveRegistry.Keystroke, 0.9f + Noise.nextFloat() * 0.3f, 0.18f);
            }
            if (Typed >= pageLength(0)) {
                Typed = Integer.MAX_VALUE;
            }
        }
    }

    private void revealSounds() {
        if (Clock == SealAt) {
            play(ArchiveRegistry.Seal, 1.0f, 0.8f);
        }
        if (Clock == OpenStart) {
            play(ArchiveRegistry.Page, 0.9f, 0.7f);
        }
        if (Clock == StampAt) {
            play(ArchiveRegistry.StampSound, 0.9f, 0.85f);
        }
    }

    private int pageLength(int Index) {
        int Count = 0;
        if (Index < Pages.size()) {
            for (DossierLayout.Line Line : Pages.get(Index)) {
                Count += Line.Glyphs().size();
            }
        }
        return Count;
    }

    private int glyphWidth(DossierLayout.Glyph Entry) {
        return font.width(FormattedCharSequence.forward(String.valueOf(Entry.Character()), Entry.Kind() == DossierLayout.Bold ? BoldStyle : Style.EMPTY));
    }

    private void refresh(AnomalyDocument Document, ResourceLocation Id) {
        int Generation = LangIndex.current().generation();
        if (Id.equals(LoadedId) && Generation == LoadedGeneration) {
            return;
        }
        LoadedId = Id;
        LoadedGeneration = Generation;
        Content = Document == null ? null : DossierContent.client(Document);
        List<DossierLayout.Block> Blocks = Content == null
                ? DossierLayout.paragraphs(Component.translatable("gui.scp_unleashed.dossier.corrupted").getString())
                : DossierLayout.blocks(Content);
        Pictorial = Content != null && Content.pictorial();
        Pictures = Pictorial ? Content.Sections().stream().map(Entry -> picture(Entry.Picture().orElse(""))).toList() : List.of();
        Legend = Pictorial ? Content.Legend().map(DossierScreen::picture).orElse(null) : null;
        Pages = Pictorial ? Pictures.stream().map(Entry -> List.<DossierLayout.Line>of()).toList() : DossierLayout.paginate(Blocks, BodyWidth, MaxTop, this::glyphWidth);
        Page = Mth.clamp(Page, 0, Pages.size() - 1);
        PhotoTexture = Document == null || minecraft == null ? null
                : Document.photoTexture().filter(Location -> minecraft.getResourceManager().getResource(Location).isPresent()).orElse(null);
    }

    public static ResourceLocation picture(String Location) {
        ResourceLocation Parsed = ResourceLocation.tryParse(Location);
        ResourceLocation Base = Parsed == null ? ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "missing") : Parsed;
        return Base.withPath(Path -> "textures/" + Path + ".png");
    }

    @Nullable
    public ResourceLocation shownDocument() {
        DocumentStamp Stamp = held().get(ArchiveRegistry.StampComponent.get());
        return Stamp == null ? null : Stamp.Document();
    }

    public boolean pictorial() {
        return Pictorial;
    }

    private boolean coverPictorial() {
        ResourceLocation Id = shownDocument();
        if (Id == null) {
            return false;
        }
        if (!Id.equals(CoverId)) {
            CoverId = Id;
            AnomalyDocument Found = AnomalyDocuments.lookup(Id);
            CoverPictorial = Found != null && DossierContent.client(Found).pictorial();
        }
        return CoverPictorial;
    }

    private List<String> titleLines(String Title) {
        List<String> Lines = new ArrayList<>();
        font.getSplitter().splitLines(Title, LeftColumn, Style.EMPTY).forEach(Line -> Lines.add(Line.getString()));
        if (Lines.size() <= TitleRows) {
            return Lines;
        }
        String Last = String.join(" ", Lines.subList(TitleRows - 1, Lines.size()));
        Last = font.plainSubstrByWidth(Last, LeftColumn - font.width(Ellipsis)).stripTrailing() + Ellipsis;
        List<String> Kept = new ArrayList<>(Lines.subList(0, TitleRows - 1));
        Kept.add(Last);
        return Kept;
    }

    private boolean done() {
        return Clock >= OpenEnd;
    }

    private void turn(int Delta) {
        int Next = Mth.clamp(Page + Delta, 0, Pages.size() - 1);
        if (Next != Page) {
            Page = Next;
            TurnedAt = Ticks;
            Typed = Integer.MAX_VALUE;
            play(ArchiveRegistry.Page, 0.95f + Noise.nextFloat() * 0.15f, 0.6f);
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (!done() || Typed < Integer.MAX_VALUE) {
            if (Clock < Finished && Clock >= SealAt - 1) {
                Clock = Math.max(Clock, OpenEnd);
            }
            Typed = Integer.MAX_VALUE;
            return true;
        }
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (overArrow(MouseX, MouseY, ArrowLeftX) && Page > 0) {
                turn(-1);
                return true;
            }
            if (overArrow(MouseX, MouseY, ArrowRightX) && Page < Pages.size() - 1) {
                turn(1);
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    private boolean overArrow(double MouseX, double MouseY, int X) {
        return MouseX >= Left + X - 2 && MouseX < Left + X + 16 && MouseY >= Top + ArrowY - 2 && MouseY < Top + ArrowY + 14;
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        if (done() && ScrollY != 0.0) {
            turn(ScrollY > 0.0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        if (done()) {
            if (Key == GLFW.GLFW_KEY_LEFT || Key == GLFW.GLFW_KEY_A || Key == GLFW.GLFW_KEY_PAGE_UP) {
                turn(-1);
                return true;
            }
            if (Key == GLFW.GLFW_KEY_RIGHT || Key == GLFW.GLFW_KEY_D || Key == GLFW.GLFW_KEY_PAGE_DOWN) {
                turn(1);
                return true;
            }
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(Key, ScanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    @Override
    public void onClose() {
        if (done()) {
            play(ArchiveRegistry.Page, 0.8f, 0.4f);
        }
        if (Parent != null && minecraft != null) {
            minecraft.setScreen(Parent);
            return;
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        ItemStack Stack = held();
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        boolean Ready = available();
        AnomalyDocument Document = Ready ? AnomalyDocumentItem.lookup(Stack) : null;
        Declassified = Document != null && DossierClearance.clears(HeldClearance, DossierClearance.required(Document));
        if (Ready && Stamp != null) {
            refresh(Document, Stamp.Document());
        }
        FieldNotes Notes = Ready ? Stack.get(ArchiveRegistry.NotesComponent.get()) : null;
        DocumentClass Classification = Stamp != null ? Stamp.Classification() : sealedClass();
        float Time = Math.min(Clock, OpenEnd + 40) + (Clock < Finished && (Clock < SealAt - 1 || Ready) ? PartialTick : 0.0f);
        Redactions.clear();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (!Reveal || Clock >= Finished || Time >= OpenEnd) {
            float StampScale = Reveal && Clock < Finished ? stampScale(Time) : 1.0f;
            drawLeftSheet(Graphics, Left, Top, Document, Notes, Classification, StampScale);
            drawRightSheet(Graphics, Left, Top, Document, MouseX, MouseY, Classification);
            drawPageSweep(Graphics, PartialTick);
            RenderSystem.disableBlend();
            renderHover(Graphics, Document, Notes, Classification, MouseX, MouseY);
            return;
        }
        drawReveal(Graphics, Time, Document, Notes, Classification);
        RenderSystem.disableBlend();
    }

    private float stampScale(float Time) {
        if (Time < StampAt) {
            return 0.0f;
        }
        return 1.0f + 1.2f * (1.0f - Mth.clamp((Time - StampAt) / StampSlam, 0.0f, 1.0f));
    }

    private void drawReveal(GuiGraphics Graphics, float Time, AnomalyDocument Document, FieldNotes Notes, DocumentClass Classification) {
        float Rise = Mth.clamp(Time / RiseTicks, 0.0f, 1.0f);
        float Eased = 1.0f - (1.0f - Rise) * (1.0f - Rise) * (1.0f - Rise);
        int Lift = Math.round((1.0f - Eased) * 40.0f);
        float Torn = Mth.clamp((Time - SealAt) / TearTicks, 0.0f, 1.0f);
        float Open = Mth.clamp((Time - OpenStart) / (OpenEnd - OpenStart), 0.0f, 1.0f);
        if (Open <= 0.0f) {
            drawCover(Graphics, Left + Spine, Top + Lift, Classification, Torn, Eased);
            return;
        }
        drawRightSheet(Graphics, Left, Top, Document, -1, -1, Classification);
        Graphics.pose().pushPose();
        if (Open < 0.5f) {
            float Squeeze = Math.max(0.01f, (float) Math.cos(Open * Math.PI));
            Graphics.pose().translate(Left + Spine, Top, 0.0f);
            Graphics.pose().scale(Squeeze, 1.0f, 1.0f);
            drawCover(Graphics, 0, 0, Classification, 1.0f, 1.0f);
        } else {
            float Squeeze = Math.max(0.01f, (float) -Math.cos(Open * Math.PI));
            Graphics.pose().translate(Left + Spine - Spine * Squeeze, Top, 0.0f);
            Graphics.pose().scale(Squeeze, 1.0f, 1.0f);
            drawLeftSheet(Graphics, 0, 0, Document, Notes, Classification, 0.0f);
        }
        Graphics.pose().popPose();
    }

    private void blit(GuiGraphics Graphics, int[] Region, int X, int Y) {
        Graphics.blit(Texture, X, Y, Region[0], Region[1], Region[2], Region[3], TextureSize, TextureSize);
    }

    private void tinted(GuiGraphics Graphics, int[] Region, int X, int Y, int Colour, float Alpha) {
        Graphics.setColor(((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f, Alpha);
        blit(Graphics, Region, X, Y);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void centred(GuiGraphics Graphics, String Line, int CentreX, int Y, int Colour, boolean Bold) {
        int X = CentreX - font.width(Line) / 2;
        Graphics.drawString(font, Line, X, Y, Colour, false);
        if (Bold) {
            Graphics.drawString(font, Line, X + 1, Y, Colour, false);
        }
    }

    private void drawCover(GuiGraphics Graphics, int X, int Y, DocumentClass Classification, float Torn, float Alpha) {
        Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
        blit(Graphics, Cover, X, Y);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (Alpha < 0.05f) {
            return;
        }
        int TextAlpha = Math.max(4, (int) (Alpha * 255)) << 24;
        boolean Wordless = coverPictorial();
        if (!Wordless) {
            centred(Graphics, Component.translatable("gui.scp_unleashed.dossier.restricted").getString(), X + 108, Y + 16, 0xFAE6DC | TextAlpha, false);
            centred(Graphics, Component.translatable("gui.scp_unleashed.dossier.sealed").getString(), X + 108, Y + 172, (InkMain & 0xFFFFFF) | TextAlpha, true);
        }
        centred(Graphics, coverDesignation(), X + 108, Y + 184, (InkSoft & 0xFFFFFF) | TextAlpha, false);
        centred(Graphics, Classification.getName().getString().toUpperCase(Locale.ROOT), X + 108, Y + 198, (Classification.getInk() & 0xFFFFFF) | TextAlpha, true);
        if (Torn >= 1.0f) {
            return;
        }
        int Shift = Math.round(Torn * 40.0f);
        float Fade = Alpha * (1.0f - Torn);
        for (int TileX = 0; TileX < Cover[2]; TileX += TapeTile[2]) {
            int Dx = TileX + TapeTile[2] / 2 < Cover[2] / 2 ? -Shift : Shift;
            int Width = Math.min(TapeTile[2], Cover[2] - TileX);
            Graphics.setColor(((Classification.getColour() >> 16) & 255) / 255.0f, ((Classification.getColour() >> 8) & 255) / 255.0f, (Classification.getColour() & 255) / 255.0f, Fade);
            Graphics.blit(Texture, X + TileX + Dx, Y + 112, TapeTile[0], TapeTile[1], Width, TapeTile[3], TextureSize, TextureSize);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        String Sealed = Component.translatable("gui.scp_unleashed.dossier.tape").getString();
        if (!Wordless) {
            Graphics.drawString(font, Sealed, X + 78 - font.width(Sealed) / 2 - Shift, Y + 115, 0xFFFFFF | (Math.max(4, (int) (Fade * 255)) << 24), false);
        }
        if (Torn <= 0.0f) {
            tinted(Graphics, WaxSeal, X + 95, Y + 106, Classification.getColour(), Alpha);
        } else {
            tinted(Graphics, WaxLeft, X + 95 - Shift / 2, Y + 106 + Shift / 3, Classification.getColour(), Fade);
            tinted(Graphics, WaxRight, X + 108 + Shift / 2, Y + 106 + Shift / 2, Classification.getColour(), Fade);
        }
    }

    private void drawLeftSheet(GuiGraphics Graphics, int X, int Y, AnomalyDocument Document, FieldNotes Notes, DocumentClass Classification, float StampScale) {
        Graphics.blit(Texture, X, Y, 0, 0, Spine, SpreadHeight, TextureSize, TextureSize);
        blit(Graphics, Emblem, X + 18, Y + 14);
        if (Pictorial && Document != null && Content != null) {
            drawWordlessLeft(Graphics, X, Y, Document, Classification, StampScale);
            return;
        }
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.dossier.division"), X + 50, Y + 18, InkMain, false);
        int Clearance = DossierClearance.required(Document);
        boolean IsCopy = held().is(ArchiveRegistry.DatabaseCopy.get());
        Graphics.drawString(font, Component.translatable(IsCopy ? "gui.scp_unleashed.dossier.database_copy" : "gui.scp_unleashed.dossier.copy", Clearance), X + 50, Y + 29, InkSoft, false);
        String Designation = Document == null || Content == null ? Component.translatable("gui.scp_unleashed.dossier.unknown").getString() : Content.Designation();
        float DesignationScale = Math.min(2.0f, (float) LeftColumn / Math.max(1, font.width(Designation)));
        Graphics.pose().pushPose();
        Graphics.pose().translate(X + 18, Y + 50, 0.0f);
        Graphics.pose().scale(DesignationScale, DesignationScale, 1.0f);
        Graphics.drawString(font, Designation, 0, 0, InkMain, false);
        Graphics.pose().popPose();
        String Title = Document == null || Content == null ? Component.translatable("archive.scp_unleashed.notes.corrupted").getString() : Content.Title();
        List<String> TitleLines = titleLines(Title);
        for (int Row = 0; Row < TitleLines.size(); Row++) {
            Graphics.drawString(font, TitleLines.get(Row), X + 18, Y + 70 + Row * 10, InkSoft, false);
        }
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.dossier.field.item"), X + 18, Y + 96, InkSoft, false);
        Graphics.drawString(font, Document == null || Content == null ? "-" : DossierLayout.plain(Content.Item(), Declassified), X + 18, Y + 106, InkMain, false);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.dossier.field.class"), X + 18, Y + 120, InkSoft, false);
        String ClassText = (Document == null || Content == null ? Classification.getName().getString() : Content.ObjectClass()).toUpperCase(Locale.ROOT);
        int ClassColour = 0xFF000000 | Classification.getInk();
        Graphics.drawString(font, ClassText, X + 18, Y + 130, ClassColour, false);
        Graphics.drawString(font, ClassText, X + 19, Y + 130, ClassColour, false);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.dossier.field.tier"), X + 18, Y + 144, InkSoft, false);
        int Danger = Document == null ? 0 : Document.Danger();
        for (int Index = 0; Index < 5; Index++) {
            blit(Graphics, Index < Danger ? Pip : PipEmpty, X + 18 + Index * 9, Y + 154);
        }
        drawPhoto(Graphics, X, Y, Document);
        centred(Graphics, Component.translatable("gui.scp_unleashed.dossier.figure").getString(), X + PhotoX + PhotoWidth / 2, Y + PhotoY + PhotoHeight - 10, InkSoft, false);
        blit(Graphics, Paperclip, X + ClipX, Y + ClipY);
        if (StampScale > 0.0f) {
            bigStamp(Graphics, X + 92, Y + 140, Classification.getName().getString().toUpperCase(Locale.ROOT), Classification.getInk(), StampScale);
        }
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.dossier.notes"), X + NotesX + 6, Y + NotesY + 4, InkMain, false);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.dossier.notes"), X + NotesX + 7, Y + NotesY + 4, InkMain, false);
        int Row = 0;
        if (Notes != null) {
            for (Component Line : Notes.readableLines()) {
                for (FormattedCharSequence Wrapped : font.split(Line, NotesWidth - 12)) {
                    if (Row < NotesRows) {
                        Graphics.drawString(font, Wrapped, X + NotesX + 6, Y + NotesY + 17 + Row * 9, InkMain, false);
                        Row++;
                    }
                }
            }
        }
        if (Row == 0) {
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.dossier.no_notes"), X + NotesX + 6, Y + NotesY + 17, InkSoft, false);
        }
    }

    private void drawPhoto(GuiGraphics Graphics, int X, int Y, AnomalyDocument Document) {
        if (PhotoTexture != null && Document != null) {
            Graphics.blit(PhotoTexture, X + PhotoX + 4, Y + PhotoY + 4, 0, 0, GrainWidth, GrainHeight, GrainWidth, GrainHeight);
        } else {
            int Hash = LoadedId == null ? 0 : LoadedId.hashCode();
            int GrainU = PhotoGrain[0] + Math.floorMod(Hash, PhotoGrain[2] - GrainWidth);
            int GrainV = PhotoGrain[1] + Math.floorMod(Hash / 97, PhotoGrain[3] - GrainHeight);
            Graphics.blit(Texture, X + PhotoX + 4, Y + PhotoY + 4, GrainU, GrainV, GrainWidth, GrainHeight, TextureSize, TextureSize);
        }
    }

    private void drawWordlessLeft(GuiGraphics Graphics, int X, int Y, AnomalyDocument Document, DocumentClass Classification, float StampScale) {
        String Designation = Content.Designation();
        float DesignationScale = Math.min(2.0f, (float) LeftColumn / Math.max(1, font.width(Designation)));
        Graphics.pose().pushPose();
        Graphics.pose().translate(X + 18, Y + 50, 0.0f);
        Graphics.pose().scale(DesignationScale, DesignationScale, 1.0f);
        Graphics.drawString(font, Designation, 0, 0, InkMain, false);
        Graphics.pose().popPose();
        String ClassText = Content.ObjectClass().toUpperCase(Locale.ROOT);
        int ClassColour = 0xFF000000 | Classification.getInk();
        Graphics.drawString(font, ClassText, X + 18, Y + 130, ClassColour, false);
        Graphics.drawString(font, ClassText, X + 19, Y + 130, ClassColour, false);
        for (int Index = 0; Index < 5; Index++) {
            blit(Graphics, Index < Document.Danger() ? Pip : PipEmpty, X + 18 + Index * 9, Y + 154);
        }
        drawPhoto(Graphics, X, Y, Document);
        blit(Graphics, Paperclip, X + ClipX, Y + ClipY);
        if (StampScale > 0.0f) {
            bigStamp(Graphics, X + 92, Y + 140, Classification.getName().getString().toUpperCase(Locale.ROOT), Classification.getInk(), StampScale);
        }
        if (Legend != null) {
            Graphics.blit(Legend, X + NotesX + 4, Y + NotesY + 6, 0, 0, LegendWidth, LegendHeight, LegendWidth, LegendHeight);
        }
    }

    private void bigStamp(GuiGraphics Graphics, int X, int Y, String Word, int Ink, float Scale) {
        float Alpha = Mth.clamp(2.2f - Scale, 0.0f, 1.0f);
        if (Alpha <= 0.05f) {
            return;
        }
        float CentreX = X + StampBig[2] / 2.0f;
        float CentreY = Y + StampBig[3] / 2.0f;
        Graphics.pose().pushPose();
        Graphics.pose().translate(CentreX, CentreY, 0.0f);
        Graphics.pose().scale(Scale, Scale, 1.0f);
        Graphics.pose().translate(-CentreX, -CentreY, 0.0f);
        tinted(Graphics, StampBig, X, Y, Ink, Alpha);
        int Width = font.width(Word);
        float Fit = Math.min(2.0f, (StampBig[2] - 10) / (float) Math.max(1, Width));
        Graphics.pose().pushPose();
        Graphics.pose().translate(CentreX, CentreY, 0.0f);
        Graphics.pose().scale(Fit, Fit, 1.0f);
        Graphics.drawString(font, Word, -Width / 2 + 1, -4, (Ink & 0xFFFFFF) | ((int) (Alpha * 255) << 24), false);
        Graphics.pose().popPose();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
        blit(Graphics, StampBigDistress, X, Y);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        Graphics.pose().popPose();
    }

    private static long scramble(long Seed) {
        return (Seed * 2654435761L) & 0xFFFFFFFFL;
    }

    private void drawRightSheet(GuiGraphics Graphics, int X, int Y, AnomalyDocument Document, int MouseX, int MouseY, DocumentClass Classification) {
        Graphics.blit(Texture, X + Spine, Y, Spine, 0, SpreadWidth - Spine, SpreadHeight, TextureSize, TextureSize);
        String Designation = Document == null || Content == null ? Component.translatable("gui.scp_unleashed.dossier.unknown").getString() : Content.Designation();
        Graphics.drawString(font, Designation, X + BodyX, Y + 16, InkMain, false);
        Graphics.drawString(font, Designation, X + BodyX + 1, Y + 16, InkMain, false);
        if (!Pictorial) {
            String PageLabel = Component.translatable("gui.scp_unleashed.dossier.page", Page + 1, Pages.size()).getString();
            Graphics.drawString(font, PageLabel, X + RightX1 - 10 - font.width(PageLabel), Y + 16, InkSoft, false);
        } else if (!Pictures.isEmpty()) {
            Graphics.blit(Pictures.get(Mth.clamp(Page, 0, Pictures.size() - 1)), X + PictureX, Y + PictureY, 0, 0, PictureSize, PictureSize, PictureSize, PictureSize);
        }
        List<DossierLayout.Line> Lines = Pages.get(Mth.clamp(Page, 0, Pages.size() - 1));
        int Limit = Page == 0 && Clock >= TypeStart ? Typed : Page == 0 && Reveal && Clock < Finished ? 0 : Integer.MAX_VALUE;
        Graphics.drawManaged(() -> {
            int Count = 0;
            for (int Row = 0; Row < Lines.size(); Row++) {
                DossierLayout.Line Line = Lines.get(Row);
                Count = typeLine(Graphics, Line.Glyphs(), X + BodyX, Y + BodyY + Line.Y(), Page * 977 + Row, Count, Limit);
            }
        });
        boolean Done = done() && MouseX >= 0;
        if (Page > 0) {
            blit(Graphics, Done && overArrow(MouseX, MouseY, ArrowLeftX) ? ArrowLeftHover : ArrowLeft, X + ArrowLeftX, Y + ArrowY);
        }
        if (Page < Pages.size() - 1) {
            blit(Graphics, Done && overArrow(MouseX, MouseY, ArrowRightX) ? ArrowRightHover : ArrowRight, X + ArrowRightX, Y + ArrowY);
        }
        centred(Graphics, "- " + (Page + 1) + " -", X + (RightX0 + RightX1) / 2, Y + ArrowY + 2, InkSoft, false);
    }

    private int typeLine(GuiGraphics Graphics, List<DossierLayout.Glyph> Line, int X, int Y, int Seed, int Count, int Limit) {
        int Cursor = X;
        int RunStart = -1;
        boolean OpenLeft = false;
        for (int Column = 0; Column < Line.size(); Column++) {
            if (Count >= Limit) {
                closeRun(Graphics, RunStart, Cursor, Y, OpenLeft, true);
                return Count;
            }
            Count++;
            DossierLayout.Glyph Entry = Line.get(Column);
            long Hash = scramble((long) Seed * 131 + Column);
            int Width = glyphWidth(Entry);
            if (Entry.Kind() == DossierLayout.Redacted) {
                if (RunStart < 0) {
                    RunStart = Cursor;
                    OpenLeft = Column == 0 || Line.get(Column - 1).Character() == ' ';
                }
                if (Declassified) {
                    declassifiedGlyph(Graphics, Entry, Cursor, Y, Width, RunStart == Cursor ? (OpenLeft ? 2 : 1) : 0, Hash);
                } else {
                    int Upper = Y - 1 + (Hash % 5 == 0 ? 1 : 0);
                    int Lower = Y + 8 - (Hash % 7 == 0 ? 1 : 0);
                    Graphics.fill(Cursor - (RunStart == Cursor ? 1 : 0), Upper, Cursor + Width, Lower, RedactionColour);
                }
                Cursor += Width;
                continue;
            }
            closeRun(Graphics, RunStart, Cursor, Y, OpenLeft, Entry.Character() == ' ');
            RunStart = -1;
            if (Entry.Character() != ' ') {
                int Shade = (int) (Hash % 9);
                int Colour = (Shade > 1 ? InkMain : InkSoft) & 0xFFFFFF;
                int Alpha = Shade == 3 ? 0xC8 : 0xFF;
                FormattedCharSequence Glyph = FormattedCharSequence.forward(String.valueOf(Entry.Character()), Entry.Kind() == DossierLayout.Bold ? BoldStyle : Style.EMPTY);
                Graphics.drawString(font, Glyph, Cursor, Y, Colour | (Alpha << 24), false);
            }
            Cursor += Width;
        }
        closeRun(Graphics, RunStart, Cursor, Y, OpenLeft, true);
        return Count;
    }

    private void declassifiedGlyph(GuiGraphics Graphics, DossierLayout.Glyph Entry, int X, int Y, int Width, int Lead, long Hash) {
        Graphics.fill(X - Lead, Y - 1, X + Width, Y + 9, DeclassifiedWash);
        if (Entry.Character() != ' ') {
            int Colour = Hash % 9 > 1 ? DeclassifiedInk : DeclassifiedSoft;
            Graphics.drawString(font, FormattedCharSequence.forward(String.valueOf(Entry.Character()), Style.EMPTY), X, Y, Colour, false);
        }
    }

    private void closeRun(GuiGraphics Graphics, int RunStart, int RunEnd, int Y, boolean OpenLeft, boolean OpenRight) {
        if (RunStart >= 0 && RunEnd > RunStart) {
            Redactions.add(new int[]{RunStart - 1, Y - 1, RunEnd, Y + 8});
            if (Declassified) {
                if (OpenRight) {
                    Graphics.fill(RunEnd, Y - 1, RunEnd + 1, Y + 9, DeclassifiedWash);
                }
                declassifiedFrame(Graphics, RunStart - (OpenLeft ? 2 : 1), Y - 1, RunEnd + (OpenRight ? 1 : 0), Y + 9);
            }
        }
    }

    private void declassifiedFrame(GuiGraphics Graphics, int X0, int Y0, int X1, int Y1) {
        int Arm = Math.max(1, Math.min(BracketArm, (X1 - X0) / 2));
        Graphics.fill(X0, Y0, X0 + Arm, Y0 + 1, DeclassifiedBracket);
        Graphics.fill(X0, Y0 + 1, X0 + 1, Y0 + BracketArm, DeclassifiedBracket);
        Graphics.fill(X1 - Arm, Y0, X1, Y0 + 1, DeclassifiedBracket);
        Graphics.fill(X1 - 1, Y0 + 1, X1, Y0 + BracketArm, DeclassifiedBracket);
        Graphics.fill(X0, Y1 - 1, X0 + Arm, Y1, DeclassifiedBracket);
        Graphics.fill(X0, Y1 - BracketArm, X0 + 1, Y1 - 1, DeclassifiedBracket);
        Graphics.fill(X1 - Arm, Y1 - 1, X1, Y1, DeclassifiedBracket);
        Graphics.fill(X1 - 1, Y1 - BracketArm, X1, Y1 - 1, DeclassifiedBracket);
    }

    private void drawPageSweep(GuiGraphics Graphics, float PartialTick) {
        float Since = Ticks - TurnedAt + PartialTick;
        if (Since >= TurnTicks) {
            return;
        }
        float Fade = 1.0f - Since / TurnTicks;
        int Alpha = (int) (Fade * 220);
        Graphics.fill(Left + RightX0 + 2, Top + 30, Left + RightX1 - 4, Top + 222, (Alpha << 24) | 0xE2D6B6);
        int Edge = Left + RightX1 - 6 - (int) ((RightX1 - RightX0 - 10) * (1.0f - Fade));
        Graphics.fillGradient(Edge - 6, Top + SheetY0 + 2, Edge, Top + SheetY1 - 2, 0x00000000, ((int) (Fade * 90) << 24));
    }

    private void renderHover(GuiGraphics Graphics, AnomalyDocument Document, FieldNotes Notes, DocumentClass Classification, int MouseX, int MouseY) {
        List<Component> Lines = new ArrayList<>();
        for (int[] Bar : Redactions) {
            if (MouseX >= Bar[0] && MouseX < Bar[2] && MouseY >= Bar[1] && MouseY < Bar[3]) {
                int Clearance = DossierClearance.required(Document);
                if (Declassified) {
                    Lines.add(Component.translatable("gui.scp_unleashed.dossier.declassified").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                    Lines.add(Component.translatable("gui.scp_unleashed.dossier.cleared", Clearance).withStyle(ChatFormatting.GRAY));
                } else {
                    Lines.add(Component.translatable("gui.scp_unleashed.dossier.redacted").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                    Lines.add(Component.translatable("gui.scp_unleashed.dossier.clearance", Clearance).withStyle(ChatFormatting.GRAY));
                }
                break;
            }
        }
        if (Lines.isEmpty() && !Pictorial && MouseX >= Left + PhotoX && MouseX < Left + PhotoX + PhotoWidth && MouseY >= Top + PhotoY && MouseY < Top + PhotoY + PhotoHeight) {
            Lines.add(Component.translatable("gui.scp_unleashed.dossier.figure_title").withStyle(ChatFormatting.WHITE));
            String Caption = Document == null || Content == null ? null : Content.Caption().orElse(null);
            if (Caption != null) {
                for (FormattedCharSequence Wrapped : font.split(Component.literal(DossierLayout.plain(Caption, Declassified)), 200)) {
                    StringBuilder Builder = new StringBuilder();
                    Wrapped.accept((Index, Look, Code) -> {
                        Builder.appendCodePoint(Code);
                        return true;
                    });
                    Lines.add(Component.literal(Builder.toString()).withStyle(ChatFormatting.GRAY));
                }
            } else {
                Lines.add(Component.translatable("gui.scp_unleashed.dossier.no_image").withStyle(ChatFormatting.GRAY));
            }
        }
        if (Lines.isEmpty() && MouseX >= Left + 92 && MouseX < Left + 92 + StampBig[2] && MouseY >= Top + 140 && MouseY < Top + 140 + StampBig[3]) {
            Lines.add(Classification.getName().copy().withStyle(Look -> Look.withColor(Classification.getColour() & 0xFFFFFF).withBold(true)));
            if (!Pictorial) {
                Lines.add(Classification.getSummary().copy().withStyle(ChatFormatting.GRAY));
            }
        }
        if (Lines.isEmpty() && !Pictorial && Document != null && MouseX >= Left + 16 && MouseX < Left + 64 && MouseY >= Top + 152 && MouseY < Top + 162) {
            Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.tier", Document.dangerNumeral()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.anomaly_document.spawn", Document.Spawn().getName()).withStyle(ChatFormatting.GRAY));
        }
        if (!Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }
}
