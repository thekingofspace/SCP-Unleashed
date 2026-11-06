package net.rai.scp_unleashed.scpsign.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scpsign.ScpSignKind;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ScpSignLayouts {
    public static final ResourceLocation Sheet = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp_sign.png");
    private static final float SheetWidth = 256.0f;
    private static final float SheetHeight = 128.0f;
    private static final int[] EmblemRect = {0, 16, 40, 40};
    private static final int[] SmallEmblemRect = {40, 16, 24, 24};
    private static final int[] BandRect = {64, 16, 16, 18};
    private static final int[] WhiteRect = {80, 16, 8, 8};
    private static final int[] TileRect = {88, 16, 18, 18};
    private static final int[] StripeRect = {64, 40, 192, 18};
    private static final int[] StaticRect = {0, 64, 68, 48};
    private static final int IconCount = 8;
    private static final int TextMain = 0xF0F2F4;
    private static final int TextTitle = 0xC2C9D0;
    private static final int TextLabel = 0x8E98A3;
    private static final int PaperBorder = 0xFFD9D2C0;
    private static final int BandInkDark = 0xFF15181C;
    private static final int BandInkLight = 0xFFF4F6F8;
    private static final int EmptyBand = 0xFF59616B;
    private static final int EmptyInk = 0xA9B1BA;
    private static final float[][] WideTitle = {{1.0f, 2.0f}, {0.8f, 2.0f}};
    private static final float[][] SmallTitle = {{0.6f, 1.0f}, {0.5f, 1.0f}, {0.6f, 2.0f}, {0.5f, 3.0f}};
    private static final String Ellipsis = "...";
    private static final String GuiKey = "gui.scp_unleashed.scp_sign.";

    private static final Map<ResourceLocation, ScpSignLayout> WideCache = new HashMap<>();
    private static final Map<ResourceLocation, ScpSignLayout> SmallCache = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> PhotoPresence = new HashMap<>();
    private static final Map<ScpSignKind, ScpSignLayout> EmptyCache = new EnumMap<>(ScpSignKind.class);

    private ScpSignLayouts() {
    }

    public static ScpSignLayout get(@Nullable ResourceLocation Id, ScpSignKind Kind, Font Measure) {
        AnomalyDocument Document = Id == null ? null : AnomalyDocuments.lookup(Id);
        if (Document == null) {
            return EmptyCache.computeIfAbsent(Kind, Missing -> empty(Missing, Measure));
        }
        Map<ResourceLocation, ScpSignLayout> Cache = Kind == ScpSignKind.Wide ? WideCache : SmallCache;
        ScpSignLayout Found = Cache.get(Id);
        if (Found == null || Found.Source() != Document) {
            Found = Kind == ScpSignKind.Wide ? wide(Document, Measure) : small(Document, Measure);
            Cache.put(Id, Found);
        }
        return Found;
    }

    public static void clear() {
        WideCache.clear();
        SmallCache.clear();
        PhotoPresence.clear();
        EmptyCache.clear();
    }

    private static ScpSignLayout wide(AnomalyDocument Document, Font Measure) {
        Builder Out = new Builder(Document);
        float Width = ScpSignKind.Wide.getWidth();
        float Height = ScpSignKind.Wide.getHeight();
        Out.photo(Document, 4.0f, 4.0f, 68.0f, 48.0f);
        float Column = 79.0f;
        float Room = Width - Column - 4.0f;
        String Designation = Document.designation().getString();
        float Scale = fit(Measure.width(Designation), Room, 0.5f, 2.0f);
        Designation = truncate(Measure, Designation, Room, Scale);
        TitleFit Title = wrap(Measure, Document.title().getString(), Room, WideTitle);
        int Colour = Document.Classification().getColour();
        String Name = Document.objectClass().getString().toUpperCase(Locale.ROOT);
        Component ClassLine = classLine(Name, Colour);
        float ClassScale = fit(Measure.width(ClassLine), Room, 0.5f, 0.85f);
        while (Measure.width(ClassLine) * ClassScale > Room && Name.length() > 1) {
            Name = Name.substring(0, Name.length() - 1);
            ClassLine = classLine(Name.stripTrailing() + Ellipsis, Colour);
        }
        int Rows = Title.Lines().size();
        float Block = 7.0f * Scale + 5.0f + (Rows * 10.0f - 3.0f) * Title.Scale() + 6.0f + 7.0f * ClassScale;
        float Top = 4.0f + (48.0f - Block) / 2.0f;
        Out.text(Designation, TextMain, Column, Top, Scale);
        float Cursor = Top + 7.0f * Scale + 5.0f;
        for (int Row = 0; Row < Rows; Row++) {
            Out.text(Title.Lines().get(Row), TextTitle, Column, Cursor + Row * 10.0f * Title.Scale(), Title.Scale());
        }
        Cursor += (Rows * 10.0f - 3.0f) * Title.Scale() + 6.0f;
        Out.line(ClassLine, Column, Cursor, ClassScale);
        float BandTop = Height - StripeRect[3];
        Out.icon(Document.Classification(), 0.0f, BandTop);
        Out.sheet(stripeBand(TileRect[2], BandTop, Width, Colour));
        return Out.build();
    }

    private static ScpSignLayout small(AnomalyDocument Document, Font Measure) {
        Builder Out = new Builder(Document);
        float Width = ScpSignKind.Small.getWidth();
        float Height = ScpSignKind.Small.getHeight();
        Out.photo(Document, 2.0f, 2.0f, 34.0f, 24.0f);
        float Column = 40.0f;
        float Room = Width - Column - 2.0f;
        String Designation = Document.designation().getString();
        float Scale = fit(Measure.width(Designation), Room, 0.5f, 1.0f);
        Out.text(truncate(Measure, Designation, Room, Scale), TextMain, Column, 3.0f, Scale);
        TitleFit Title = wrap(Measure, Document.title().getString(), Room, SmallTitle);
        for (int Row = 0; Row < Title.Lines().size(); Row++) {
            Out.text(Title.Lines().get(Row), TextTitle, Column, 4.0f + 7.0f * Scale + 3.5f + Row * 10.0f * Title.Scale(), Title.Scale());
        }
        int Colour = Document.Classification().getColour();
        float BandTop = Height - BandRect[3];
        Out.icon(Document.Classification(), 0.0f, BandTop);
        Out.sheet(plainBand(TileRect[2], BandTop, Width, Colour));
        Out.text(Component.translatable(GuiKey + "object_class").getString(), bandSoft(Colour), 21.0f, BandTop + 2.5f, 0.5f);
        String Name = Document.objectClass().getString().toUpperCase(Locale.ROOT);
        float NameRoom = Width - 23.0f;
        float NameScale = fit(Measure.width(Name), NameRoom, 0.5f, 1.0f);
        Out.text(truncate(Measure, Name, NameRoom, NameScale), bandInk(Colour), 21.0f, BandTop + 7.5f, NameScale);
        return Out.build();
    }

    private static ScpSignLayout empty(ScpSignKind Kind, Font Measure) {
        Builder Out = new Builder(null);
        float Width = Kind.getWidth();
        float Height = Kind.getHeight();
        String Label = Component.translatable(GuiKey + "unassigned").getString();
        if (Kind == ScpSignKind.Wide) {
            Out.sheet(sheetQuad(EmblemRect, 18.0f, 8.0f, 40.0f, 40.0f, 0xFFFFFFFF, 1));
            float Scale = fit(Measure.width(Label), Width - 74.0f, 0.5f, 1.5f);
            Out.text(Label, EmptyInk, 70.0f, 28.0f - 3.5f * Scale, Scale);
            Out.sheet(stripeBand(0.0f, Height - StripeRect[3], Width, EmptyBand));
        } else {
            Out.sheet(sheetQuad(SmallEmblemRect, 5.0f, 3.0f, 24.0f, 24.0f, 0xFFFFFFFF, 1));
            float Scale = fit(Measure.width(Label), Width - 35.0f, 0.4f, 0.75f);
            Out.text(Label, EmptyInk, 33.0f, 15.0f - 3.5f * Scale, Scale);
            Out.sheet(plainBand(0.0f, Height - BandRect[3], Width, EmptyBand));
        }
        return Out.build();
    }

    private static Component classLine(String Name, int Colour) {
        return Component.translatable(GuiKey + "class_line", Component.literal(Name).withColor(classText(Colour) & 0xFFFFFF)).withColor(TextLabel);
    }

    private static float fit(int Width, float Room, float Low, float High) {
        if (Width <= 0) {
            return High;
        }
        return Math.max(Low, Math.min(High, Room / Width));
    }

    private static String truncate(Font Measure, String Text, float Room, float Scale) {
        if (Measure.width(Text) * Scale <= Room) {
            return Text;
        }
        String Cut = Text;
        while (!Cut.isEmpty() && Measure.width(Cut.stripTrailing() + Ellipsis) * Scale > Room) {
            Cut = Cut.substring(0, Cut.length() - 1);
        }
        return Cut.stripTrailing() + Ellipsis;
    }

    private static List<String> split(Font Measure, String Text, float Room, float Scale) {
        List<String> Lines = new ArrayList<>();
        String Current = "";
        for (String Word : Text.split(" ")) {
            String Trial = Current.isEmpty() ? Word : Current + " " + Word;
            if (Measure.width(Trial) * Scale <= Room || Current.isEmpty()) {
                Current = Trial;
            } else {
                Lines.add(Current);
                Current = Word;
            }
        }
        if (!Current.isEmpty()) {
            Lines.add(Current);
        }
        return Lines;
    }

    private static TitleFit wrap(Font Measure, String Title, float Room, float[][] Candidates) {
        List<String> Lines = List.of();
        float Scale = Candidates[Candidates.length - 1][0];
        for (float[] Candidate : Candidates) {
            Scale = Candidate[0];
            Lines = split(Measure, Title, Room, Scale);
            if (Lines.size() <= (int) Candidate[1]) {
                break;
            }
        }
        int Limit = (int) Candidates[Candidates.length - 1][1];
        if (Lines.size() > Limit) {
            List<String> Joined = new ArrayList<>(Lines.subList(0, Limit - 1));
            Joined.add(String.join(" ", Lines.subList(Limit - 1, Lines.size())));
            Lines = Joined;
        }
        List<String> Fitted = new ArrayList<>();
        for (String Line : Lines) {
            Fitted.add(truncate(Measure, Line, Room, Scale));
        }
        return new TitleFit(Fitted, Scale);
    }

    private static int channel(int Colour, int Shift) {
        return (Colour >> Shift) & 255;
    }

    private static float luminance(int Colour) {
        return 0.299f * channel(Colour, 16) + 0.587f * channel(Colour, 8) + 0.114f * channel(Colour, 0);
    }

    private static int blend(int First, int Second, float Share) {
        int Mixed = 0xFF000000;
        for (int Shift = 16; Shift >= 0; Shift -= 8) {
            int Value = Math.round(channel(First, Shift) + (channel(Second, Shift) - channel(First, Shift)) * Share);
            Mixed |= Value << Shift;
        }
        return Mixed;
    }

    private static int bandInk(int Colour) {
        return luminance(Colour) > 135.0f ? BandInkDark : BandInkLight;
    }

    private static int bandSoft(int Colour) {
        return blend(bandInk(Colour), Colour, 0.42f);
    }

    private static int classText(int Colour) {
        float Light = luminance(Colour);
        return Light < 130.0f ? blend(Colour, 0xFFFFFFFF, (130.0f - Light) / (255.0f - Light)) : Colour;
    }

    private static ScpSignLayout.Quad sheetQuad(int[] Rect, float X, float Y, float Width, float Height, int Colour, int Layer) {
        return new ScpSignLayout.Quad(X, Y, X + Width, Y + Height, Rect[0] / SheetWidth, Rect[1] / SheetHeight,
                (Rect[0] + Rect[2]) / SheetWidth, (Rect[1] + Rect[3]) / SheetHeight, Colour, Layer);
    }

    private static ScpSignLayout.Quad fill(float X0, float Y0, float X1, float Y1, int Colour, int Layer) {
        return new ScpSignLayout.Quad(X0, Y0, X1, Y1, (WhiteRect[0] + 1) / SheetWidth, (WhiteRect[1] + 1) / SheetHeight,
                (WhiteRect[0] + WhiteRect[2] - 1) / SheetWidth, (WhiteRect[1] + WhiteRect[3] - 1) / SheetHeight, Colour, Layer);
    }

    private static ScpSignLayout.Quad plainBand(float X0, float Y0, float X1, int Colour) {
        return new ScpSignLayout.Quad(X0, Y0, X1, Y0 + BandRect[3], (BandRect[0] + 0.5f) / SheetWidth, BandRect[1] / SheetHeight,
                (BandRect[0] + BandRect[2] - 0.5f) / SheetWidth, (BandRect[1] + BandRect[3]) / SheetHeight, Colour, 1);
    }

    private static ScpSignLayout.Quad stripeBand(float X0, float Y0, float X1, int Colour) {
        float Span = Math.min(StripeRect[2], X1 - X0);
        return new ScpSignLayout.Quad(X0, Y0, X1, Y0 + StripeRect[3], StripeRect[0] / SheetWidth, StripeRect[1] / SheetHeight,
                (StripeRect[0] + Span) / SheetWidth, (StripeRect[1] + StripeRect[3]) / SheetHeight, Colour, 1);
    }

    private static boolean photoPresent(ResourceLocation Location) {
        return PhotoPresence.computeIfAbsent(Location, Wanted -> Minecraft.getInstance().getResourceManager().getResource(Wanted).isPresent());
    }

    private record TitleFit(List<String> Lines, float Scale) {
    }

    private static final class Builder {
        private final AnomalyDocument Source;
        private final List<ScpSignLayout.Quad> Quads = new ArrayList<>();
        private final List<ScpSignLayout.Line> Lines = new ArrayList<>();
        private ResourceLocation Photo;
        private ScpSignLayout.Quad PhotoQuad;

        private Builder(@Nullable AnomalyDocument Source) {
            this.Source = Source;
        }

        private void sheet(ScpSignLayout.Quad Added) {
            Quads.add(Added);
        }

        private void photo(AnomalyDocument Document, float X, float Y, float Width, float Height) {
            Quads.add(fill(X - 1.0f, Y - 1.0f, X + Width + 1.0f, Y + Height + 1.0f, PaperBorder, 0));
            ResourceLocation Location = Document.photoTexture().filter(ScpSignLayouts::photoPresent).orElse(null);
            if (Location == null) {
                Quads.add(sheetQuad(StaticRect, X, Y, Width, Height, 0xFFFFFFFF, 1));
                return;
            }
            Photo = Location;
            PhotoQuad = new ScpSignLayout.Quad(X, Y, X + Width, Y + Height, 0.0f, 0.0f, 1.0f, 1.0f, 0xFFFFFFFF, 1);
        }

        private void icon(DocumentClass Classification, float X, float Y) {
            int Index = Classification.ordinal() < IconCount ? Classification.ordinal() : DocumentClass.Unknown.ordinal();
            Quads.add(sheetQuad(TileRect, X, Y, TileRect[2], TileRect[3], 0xFFFFFFFF, 1));
            Quads.add(sheetQuad(new int[]{Index * 16, 0, 16, 16}, X + 1.0f, Y + 1.0f, 16.0f, 16.0f, 0xFFFFFFFF, 2));
        }

        private void text(String Text, int Colour, float X, float Y, float Scale) {
            line(Component.literal(Text).withColor(Colour & 0xFFFFFF), X, Y, Scale);
        }

        private void line(FormattedText Text, float X, float Y, float Scale) {
            Component Shown = Text instanceof Component Ready ? Ready : Component.literal(Text.getString());
            Lines.add(new ScpSignLayout.Line(Shown.getVisualOrderText(), X, Y, Scale));
        }

        private ScpSignLayout build() {
            Quads.sort((First, Second) -> Integer.compare(First.Layer(), Second.Layer()));
            return new ScpSignLayout(Source, List.copyOf(Quads), Photo, PhotoQuad, List.copyOf(Lines));
        }
    }
}
