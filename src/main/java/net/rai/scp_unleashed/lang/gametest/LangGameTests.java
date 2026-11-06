package net.rai.scp_unleashed.lang.gametest;

import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.client.DossierLayout;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DossierContent;
import net.rai.scp_unleashed.lang.LangIndex;
import net.rai.scp_unleashed.lang.LangMerge;
import net.rai.scp_unleashed.lang.LangTree;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class LangGameTests {
    public static final String Arena = "scp420j_arena";
    public static final String English = "en_us";
    public static final String Test = "xx_test";
    public static final String Shorter = "xx_short";
    public static final String Base = "dossier.scp_unleashed.079";

    private LangGameTests() {
    }

    private static JsonObject json(String Text) {
        return LangTree.read(new StringReader(Text));
    }

    private static LangMerge.Part part(int Pack, String Path, boolean Main, String Text) {
        return new LangMerge.Part(Pack, "pack" + Pack, Path, Main, Output -> LangTree.flatten(json(Text), Output));
    }

    private static LangMerge.Part broken(int Pack, String Path) {
        return new LangMerge.Part(Pack, "pack" + Pack, Path, false, Output -> LangTree.flatten(json("{\"a\": [1, 2"), Output));
    }

    private static List<LangMerge.Part> partsFor(String Code) {
        List<LangMerge.Part> Parts = new ArrayList<>();
        if (Code.equals(English)) {
            Parts.add(part(0, "lang/en_us/079.json", false, """
                    {"dossier.scp_unleashed.079": {"designation": "SCP-079", "title": "Old AI",
                      "page": [{"title": "Containment", "text": ["First %rsecret%r line.", "Second line."]},
                               {"title": "Description", "text": "Plain text."},
                               "A page with no heading."]},
                     "stranger.scp_unleashed.scp079": ["one", "two", "three", "four", "five"],
                     "block.scp_unleashed.scp079_computer": "SCP-079"}"""));
            Parts.add(part(0, "lang/en_us.json", true, "{\"gui.scp_unleashed.only_main\": \"Main\"}"));
            Parts.add(part(1, "lang/en_us/079.json", false, "{\"block.scp_unleashed.scp079_computer\": \"Pack Override\"}"));
        }
        if (Code.equals(Test)) {
            Parts.add(part(0, "lang/xx_test/079.json", false, """
                    {"dossier.scp_unleashed.079": {"title": "Vieille IA",
                      "page": [{"title": "Confinement", "text": "Un."}, {"text": "Deux."}, {"text": "Trois."}, {"title": "Ajout", "text": "Quatre."}]},
                     "stranger.scp_unleashed.scp079": ["un", "deux"]}"""));
            Parts.add(broken(0, "lang/xx_test/broken.json"));
        }
        if (Code.equals(Shorter)) {
            Parts.add(part(0, "lang/xx_short/079.json", false, "{\"dossier.scp_unleashed.079.page\": [{\"text\": \"Only one.\"}]}"));
        }
        return Parts;
    }

    private static LangIndex indexOf(LangMerge.Built Built, List<String> Codes) {
        return LangIndex.build(Codes, Built.KeysByCode());
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "lang_flatten")
    public static void flattenScheme(GameTestHelper Helper) {
        Map<String, String> Flat = LangTree.flatten(json("""
                {"plain.key": "Plain", "tree": {"": "Self", "child": "Child", "list": ["a", {"title": "T", "text": ["p0", "p1"]}], "gone": null},
                 "number": 3, "flag": true, "format": "%d and %1$f"}"""));
        Map<String, String> Expected = new HashMap<>();
        Expected.put("plain.key", "Plain");
        Expected.put("tree", "Self");
        Expected.put("tree.child", "Child");
        Expected.put("tree.list.0", "a");
        Expected.put("tree.list.1.title", "T");
        Expected.put("tree.list.1.text.0", "p0");
        Expected.put("tree.list.1.text.1", "p1");
        Expected.put("number", "3");
        Expected.put("flag", "true");
        Expected.put("format", "%s and %1$s");
        Helper.assertTrue(Flat.equals(Expected), "flattened to " + Flat);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "lang_merge")
    public static void mergeOrderDuplicatesAndBrokenFiles(GameTestHelper Helper) {
        List<LangMerge.Part> Parts = new ArrayList<>(List.of(
                part(0, "lang/en_us/b.json", false, "{\"k.two\": \"from b\", \"k.three\": \"b\"}"),
                part(0, "lang/en_us/a.json", false, "{\"k.one\": \"from a\", \"k.two\": \"from a\"}"),
                part(0, "lang/en_us.json", true, "{\"k.three\": \"from main\"}"),
                part(1, "lang/en_us/z.json", false, "{\"k.one\": \"higher pack\"}"),
                broken(1, "lang/en_us/broken.json")));
        Map<String, String> Table = new HashMap<>();
        LangMerge.Report Done = LangMerge.apply(Parts, Table::put, (Entry, Key) -> {
        });
        Helper.assertTrue("higher pack".equals(Table.get("k.one")), "a higher pack did not win: " + Table.get("k.one"));
        Helper.assertTrue("from b".equals(Table.get("k.two")), "path order inside a pack is wrong: " + Table.get("k.two"));
        Helper.assertTrue("from main".equals(Table.get("k.three")), "the classic file did not apply last in its pack: " + Table.get("k.three"));
        Helper.assertTrue(Done.Duplicates().size() == 2, "expected two same-pack duplicates, got " + Done.Duplicates());
        Helper.assertTrue(Done.Broken().size() == 1 && Done.Broken().get(0).contains("broken.json"), "the broken file was not reported: " + Done.Broken());
        Helper.assertTrue(Done.Files() == 4, "expected four loaded files, got " + Done.Files());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "lang_switch")
    public static void languageSwitchRebuildsTable(GameTestHelper Helper) {
        List<String> EnglishOnly = List.of(English);
        List<String> WithTest = List.of(English, Test);
        LangMerge.Built First = LangMerge.build(EnglishOnly, LangGameTests::partsFor);
        LangIndex FirstIndex = indexOf(First, EnglishOnly);
        LangMerge.Built Second = LangMerge.build(WithTest, LangGameTests::partsFor);
        LangIndex SecondIndex = indexOf(Second, WithTest);
        LangMerge.Built Third = LangMerge.build(EnglishOnly, LangGameTests::partsFor);
        LangIndex ThirdIndex = indexOf(Third, EnglishOnly);
        Helper.assertTrue(FirstIndex.indices(Base + ".page").equals(List.of(0, 1, 2)), "English pages " + FirstIndex.indices(Base + ".page"));
        Helper.assertTrue(SecondIndex.indices(Base + ".page").equals(List.of(0, 1, 2, 3)), "test language pages " + SecondIndex.indices(Base + ".page"));
        Helper.assertTrue(ThirdIndex.indices(Base + ".page").equals(List.of(0, 1, 2)), "pages after switching back " + ThirdIndex.indices(Base + ".page"));
        Helper.assertTrue("Vieille IA".equals(Second.Table().get(Base + ".title")), "the test language title did not override English");
        Helper.assertTrue("SCP-079".equals(Second.Table().get(Base + ".designation")), "English did not fill the missing designation");
        Helper.assertTrue("Old AI".equals(Third.Table().get(Base + ".title")), "a stale title survived the switch back: " + Third.Table().get(Base + ".title"));
        Helper.assertTrue(!Third.Table().containsKey(Base + ".page.3.title") && Third.Table().equals(First.Table()), "the rebuilt English table differs from the first one");
        Helper.assertTrue(SecondIndex.indicesOr("stranger.scp_unleashed.scp079", 9).size() == 2, "stranger pages in the test language");
        Helper.assertTrue(ThirdIndex.indicesOr("stranger.scp_unleashed.scp079", 9).size() == 5, "stranger pages in English");
        Helper.assertTrue(ThirdIndex.indicesOr("stranger.scp_unleashed.missing", 3).equals(List.of(0, 1, 2)), "the fixed page count fallback");
        Helper.assertTrue(FirstIndex.generation() != SecondIndex.generation() && SecondIndex.generation() != ThirdIndex.generation(), "the index generation did not change");
        Helper.assertTrue(Second.Done().Broken().size() == 1, "the broken test file was not skipped and reported");
        Helper.assertTrue("Pack Override".equals(First.Table().get("block.scp_unleashed.scp079_computer")), "a resource pack could not override a key");
        LangMerge.Built Brief = LangMerge.build(List.of(English, Shorter), LangGameTests::partsFor);
        Helper.assertTrue(indexOf(Brief, List.of(English, Shorter)).indices(Base + ".page").equals(List.of(0)), "a shorter translation still shows English pages");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "lang_dossier")
    public static void dossierPagesAndRedaction(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_079"));
        Helper.assertTrue(Document != null && Document.Lang().equals(Base), "the SCP-079 dossier does not point at " + Base);
        DossierContent.Fallbacks Defaults = new DossierContent.Fallbacks("Containment:", "Description:", "Euclid");
        LangMerge.Built English0 = LangMerge.build(List.of(English), LangGameTests::partsFor);
        DossierContent EnglishText = DossierContent.resolve(Document, DossierContent.table(English0.Table(), indexOf(English0, List.of(English))), Defaults);
        Helper.assertTrue(EnglishText.Sections().size() == 3, "English sections " + EnglishText.Sections().size());
        Helper.assertTrue(EnglishText.Sections().get(0).Text().equals("First %rsecret%r line.\nSecond line."), "paragraphs did not join: " + EnglishText.Sections().get(0).Text());
        Helper.assertTrue(EnglishText.Sections().get(2).Heading().isEmpty() && EnglishText.Sections().get(2).Text().equals("A page with no heading."), "a bare page string did not work");
        Helper.assertTrue(EnglishText.Item().equals("SCP-079") && EnglishText.ObjectClass().equals("Euclid"), "item and class fallbacks");
        LangMerge.Built Mixed = LangMerge.build(List.of(English, Test), LangGameTests::partsFor);
        DossierContent TestText = DossierContent.resolve(Document, DossierContent.table(Mixed.Table(), indexOf(Mixed, List.of(English, Test))), Defaults);
        Helper.assertTrue(TestText.Sections().size() == 4 && TestText.Title().equals("Vieille IA") && TestText.Designation().equals("SCP-079"), "the test language dossier");
        List<DossierLayout.Glyph> Glyphs = DossierLayout.parse("A %rB%r **C** [[D]] 5%% %rE");
        StringBuilder Kinds = new StringBuilder();
        StringBuilder Letters = new StringBuilder();
        for (DossierLayout.Glyph Entry : Glyphs) {
            Kinds.append(Entry.Kind());
            Letters.append(Entry.Character());
        }
        Helper.assertTrue(Letters.toString().equals("A B C D 5% E"), "parsed letters " + Letters);
        Helper.assertTrue(Kinds.toString().equals("002010200002"), "parsed kinds " + Kinds);
        Helper.assertTrue(DossierLayout.plain("x %ryz%r").equals("x ██"), "plain text of a redaction " + DossierLayout.plain("x %ryz%r"));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "lang_server")
    public static void serverLoadsLanguageFolders(GameTestHelper Helper) {
        Language Words = Language.getInstance();
        Helper.assertTrue("Old AI".equals(Words.getOrDefault(Base + ".title")), "the server did not load lang/en_us/079.json: " + Words.getOrDefault(Base + ".title"));
        Helper.assertTrue(Words.has("stranger.scp_unleashed.scp079.4") && !Words.has("stranger.scp_unleashed.scp079.5"), "the server stranger dialogue");
        Helper.assertTrue(Words.has("gui.scp_unleashed.dossier.title"), "the server lost general text");
        LangIndex Index = LangIndex.current();
        DossierContent.Fallbacks Defaults = new DossierContent.Fallbacks("Containment:", "Description:", "Unknown");
        int Checked = 0;
        for (Map.Entry<ResourceLocation, AnomalyDocument> Listed : AnomalyDocuments.server().entrySet()) {
            DossierContent Text = DossierContent.resolve(Listed.getValue(), DossierContent.language(), Defaults);
            Helper.assertTrue(!Text.Sections().isEmpty(), Listed.getKey() + " has no pages");
            Helper.assertTrue(!Text.Title().isEmpty() && !Text.Designation().equals(Listed.getValue().Lang()), Listed.getKey() + " has no title or designation");
            for (DossierContent.Section Entry : Text.Sections()) {
                int Marks = Entry.Text().replace("%%", "").split("%r", -1).length - 1;
                Helper.assertTrue(Marks % 2 == 0, Listed.getKey() + " has an unbalanced redaction");
            }
            Checked++;
        }
        Helper.assertTrue(Checked >= 78, "only " + Checked + " dossiers were checked");
        Helper.assertTrue(Index.indices(Base + ".page").size() >= 3, "the server index has no SCP-079 pages");
        Helper.succeed();
    }
}
