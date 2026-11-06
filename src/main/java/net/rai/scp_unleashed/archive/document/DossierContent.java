package net.rai.scp_unleashed.archive.document;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.lang.LangIndex;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DossierContent(String Designation, String Title, String Item, String ObjectClass, Optional<String> Caption, List<Section> Sections, Optional<String> Legend) {
    public static final String DesignationField = "designation";
    public static final String TitleField = "title";
    public static final String CaptionField = "caption";
    public static final String ItemField = "item";
    public static final String ClassField = "class";
    public static final String PageField = "page";
    public static final String TextField = "text";
    public static final String PictureField = "picture";
    public static final String LegendField = "legend";
    public static final String ContainmentHeading = "gui.scp_unleashed.dossier.containment";
    public static final String DescriptionHeading = "gui.scp_unleashed.dossier.description";

    public DossierContent(String Designation, String Title, String Item, String ObjectClass, Optional<String> Caption, List<Section> Sections) {
        this(Designation, Title, Item, ObjectClass, Caption, Sections, Optional.empty());
    }

    public record Section(Optional<String> Heading, String Text, Optional<String> Picture) {
        public Section(Optional<String> Heading, String Text) {
            this(Heading, Text, Optional.empty());
        }
    }

    public boolean pictorial() {
        return !Sections.isEmpty() && Sections.stream().allMatch(Entry -> Entry.Picture().isPresent() && Entry.Heading().isEmpty() && Entry.Text().isBlank());
    }

    public interface Source {
        @Nullable
        String raw(String Key);

        List<Integer> indices(String Parent);
    }

    public record Fallbacks(String Containment, String Description, String ClassName) {
    }

    public static Source language() {
        Language Words = Language.getInstance();
        LangIndex Index = LangIndex.current();
        return new Source() {
            @Override
            @Nullable
            public String raw(String Key) {
                return Words.has(Key) ? Words.getOrDefault(Key, "") : null;
            }

            @Override
            public List<Integer> indices(String Parent) {
                return Index.indices(Parent);
            }
        };
    }

    public static Source table(Map<String, String> Values, LangIndex Index) {
        return new Source() {
            @Override
            @Nullable
            public String raw(String Key) {
                return Values.get(Key);
            }

            @Override
            public List<Integer> indices(String Parent) {
                return Index.indices(Parent);
            }
        };
    }

    public static Fallbacks fallbacks(DocumentClass Classification) {
        return new Fallbacks(Component.translatable(ContainmentHeading).getString(), Component.translatable(DescriptionHeading).getString(), Classification.getName().getString());
    }

    public static DossierContent client(AnomalyDocument Document) {
        return resolve(Document, language(), fallbacks(Document.Classification()));
    }

    private static String first(@Nullable String Found, Optional<String> Legacy, String Otherwise) {
        return Found != null ? Found : Legacy.orElse(Otherwise);
    }

    public static DossierContent resolve(AnomalyDocument Document, Source Text, Fallbacks Defaults) {
        LegacyText Legacy = Document.Legacy();
        Optional<DossierText> Old = Legacy.Dossier();
        String Designation = first(Text.raw(Document.key(DesignationField)), Legacy.Designation(), Document.Lang());
        String Title = first(Text.raw(Document.key(TitleField)), Legacy.Title(), "");
        String CaptionText = Text.raw(Document.key(CaptionField));
        Optional<String> Caption = CaptionText != null ? Optional.of(CaptionText) : Legacy.Caption();
        String Item = first(Text.raw(Document.key(ItemField)), Old.flatMap(DossierText::ItemNumber), Designation);
        String ObjectClass = first(Text.raw(Document.key(ClassField)), Old.flatMap(DossierText::ObjectClass), Defaults.ClassName());
        List<Section> Sections = sections(Document.key(PageField), Text);
        if (Sections.isEmpty() && Old.isPresent()) {
            Sections = legacySections(Old.get(), Defaults);
        }
        return new DossierContent(Designation, Title, Item, ObjectClass, Caption, Sections, Optional.ofNullable(Text.raw(Document.key(LegendField))));
    }

    public static List<Section> sections(String PageBase, Source Text) {
        List<Section> Found = new ArrayList<>();
        for (int Index : Text.indices(PageBase)) {
            String Prefix = PageBase + "." + Index;
            Found.add(new Section(Optional.ofNullable(Text.raw(Prefix + "." + TitleField)), body(Prefix, Text), Optional.ofNullable(Text.raw(Prefix + "." + PictureField))));
        }
        return Found;
    }

    public static String body(String Prefix, Source Text) {
        String Whole = Text.raw(Prefix + "." + TextField);
        if (Whole != null) {
            return Whole;
        }
        List<Integer> Paragraphs = Text.indices(Prefix + "." + TextField);
        if (!Paragraphs.isEmpty()) {
            StringBuilder Joined = new StringBuilder();
            for (int Index = 0; Index < Paragraphs.size(); Index++) {
                if (Index > 0) {
                    Joined.append('\n');
                }
                String Paragraph = Text.raw(Prefix + "." + TextField + "." + Paragraphs.get(Index));
                Joined.append(Paragraph == null ? "" : Paragraph);
            }
            return Joined.toString();
        }
        String Bare = Text.raw(Prefix);
        return Bare == null ? "" : Bare;
    }

    private static List<Section> legacySections(DossierText Old, Fallbacks Defaults) {
        List<Section> Found = new ArrayList<>();
        Found.add(new Section(Optional.of(Defaults.Containment()), Old.Containment()));
        Found.add(new Section(Optional.of(Defaults.Description()), Old.Description()));
        for (DossierText.Addendum Entry : Old.Addenda()) {
            Found.add(new Section(Optional.of(Entry.Title()), Entry.Text()));
        }
        return Found;
    }
}
