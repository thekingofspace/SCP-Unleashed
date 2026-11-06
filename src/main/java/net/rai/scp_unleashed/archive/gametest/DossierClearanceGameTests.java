package net.rai.scp_unleashed.archive.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.client.DossierLayout;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DossierClearance;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.door.KeycardItem;

import java.util.List;
import java.util.Map;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class DossierClearanceGameTests {
    public static final String Arena = "gear_arena";

    private DossierClearanceGameTests() {
    }

    private static ItemStack card(int Clearance) {
        return new ItemStack(switch (Clearance) {
            case 1 -> DoorRegistry.KeycardLevel1.get();
            case 2 -> DoorRegistry.KeycardLevel2.get();
            case 3 -> DoorRegistry.KeycardLevel3.get();
            case 4 -> DoorRegistry.KeycardLevel4.get();
            case 5 -> DoorRegistry.KeycardLevel5.get();
            default -> DoorRegistry.KeycardOmni.get();
        });
    }

    private static Player reader(GameTestHelper Helper) {
        Player Reader = Helper.makeMockPlayer(GameType.SURVIVAL);
        Reader.getInventory().clearContent();
        return Reader;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "dossier_clearance_map")
    public static void clearanceFollowsObjectClass(GameTestHelper Helper) {
        Helper.assertTrue(DossierClearance.required(DocumentClass.Safe) == 2, "Safe files should need level 2");
        Helper.assertTrue(DossierClearance.required(DocumentClass.Euclid) == 3, "Euclid files should need level 3");
        Helper.assertTrue(DossierClearance.required(DocumentClass.Keter) == 4, "Keter files should need level 4");
        Helper.assertTrue(DossierClearance.required(DocumentClass.Thaumiel) == 5, "Thaumiel files should need level 5");
        Helper.assertTrue(DossierClearance.required(DocumentClass.ZClass) == 5, "Z-Class files should need level 5");
        Helper.assertTrue(DossierClearance.required(DocumentClass.Unknown) == 5, "Unknown files should need level 5");
        Helper.assertTrue(DossierClearance.required((AnomalyDocument) null) == DossierClearance.Highest, "a missing file should need the highest level");
        for (DocumentClass Grade : DocumentClass.values()) {
            int Needed = DossierClearance.required(Grade);
            Helper.assertTrue(Needed >= 1 && Needed <= DossierClearance.Highest, Grade.getSerializedName() + " needs level " + Needed);
            Helper.assertTrue(Needed < DossierClearance.Omni, Grade.getSerializedName() + " is out of reach of the omni card");
        }
        int Checked = 0;
        for (Map.Entry<ResourceLocation, AnomalyDocument> Entry : AnomalyDocuments.server().entrySet()) {
            AnomalyDocument Document = Entry.getValue();
            Helper.assertTrue(DossierClearance.required(Document) == DossierClearance.required(Document.Classification()), Entry.getKey() + " does not follow its class");
            Helper.assertTrue(DossierClearance.required(Document) >= 2, Entry.getKey() + " can be read with a level 1 card");
            Checked++;
        }
        Helper.assertTrue(Checked > 0, "no archive files are loaded");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "dossier_clearance_check")
    public static void cardsRevealAtOrAboveTheirLevel(GameTestHelper Helper) {
        for (int Rank = 1; Rank <= 5; Rank++) {
            Helper.assertTrue(DossierClearance.clearanceOf(card(Rank)) == Rank, "a level " + Rank + " card reads as " + DossierClearance.clearanceOf(card(Rank)));
        }
        Helper.assertTrue(DossierClearance.clearanceOf(card(KeycardItem.OmniClearance)) == DossierClearance.Omni, "the omni card has the wrong clearance");
        Helper.assertTrue(DossierClearance.clearanceOf(ItemStack.EMPTY) == DossierClearance.None, "an empty slot counts as a card");
        Helper.assertTrue(DossierClearance.clearanceOf(new ItemStack(Items.PAPER)) == DossierClearance.None, "paper counts as a card");
        for (DocumentClass Grade : DocumentClass.values()) {
            int Needed = DossierClearance.required(Grade);
            Helper.assertTrue(DossierClearance.clears(Needed, Needed), "an equal card does not reveal " + Grade.getSerializedName());
            Helper.assertTrue(DossierClearance.clears(DossierClearance.Highest, Needed), "a level 5 card does not reveal " + Grade.getSerializedName());
            Helper.assertTrue(!DossierClearance.clears(Needed - 1, Needed), "a lower card reveals " + Grade.getSerializedName());
            Helper.assertTrue(!DossierClearance.clears(DossierClearance.None, Needed), "no card reveals " + Grade.getSerializedName());
            Helper.assertTrue(DossierClearance.clears(DossierClearance.Omni, Needed), "the omni card does not reveal " + Grade.getSerializedName());
        }
        Player Reader = reader(Helper);
        Helper.assertTrue(!DossierClearance.canRead(Reader, DocumentClass.Safe), "a reader with no card can read a Safe file");
        Reader.getInventory().setItem(0, card(3));
        Helper.assertTrue(DossierClearance.canRead(Reader, DocumentClass.Safe), "a level 3 card does not reveal a Safe file");
        Helper.assertTrue(DossierClearance.canRead(Reader, DocumentClass.Euclid), "a level 3 card does not reveal a Euclid file");
        Helper.assertTrue(!DossierClearance.canRead(Reader, DocumentClass.Keter), "a level 3 card reveals a Keter file");
        Helper.assertTrue(!DossierClearance.canRead(Reader, DocumentClass.ZClass), "a level 3 card reveals a Z-Class file");
        Reader.getInventory().setItem(0, card(KeycardItem.OmniClearance));
        for (DocumentClass Grade : DocumentClass.values()) {
            Helper.assertTrue(DossierClearance.canRead(Reader, Grade), "the omni card does not reveal " + Grade.getSerializedName());
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "dossier_clearance_inventory")
    public static void cardsCountAnywhereTheyAreCarried(GameTestHelper Helper) {
        Player Reader = reader(Helper);
        Inventory Carried = Reader.getInventory();
        Helper.assertTrue(DossierClearance.highest(Reader) == DossierClearance.None, "an empty inventory holds a card");
        Helper.assertTrue(DossierClearance.highest(null) == DossierClearance.None, "nobody holds a card");
        int Hotbar = Inventory.getSelectionSize() - 1;
        Carried.setItem(Hotbar, card(2));
        Helper.assertTrue(DossierClearance.highest(Reader) == 2, "a card on the hotbar was missed");
        Carried.setItem(Hotbar, ItemStack.EMPTY);
        int Main = Inventory.INVENTORY_SIZE - 1;
        Carried.setItem(Main, card(4));
        Helper.assertTrue(DossierClearance.highest(Reader) == 4, "a card in the main inventory was missed");
        Carried.setItem(Main, ItemStack.EMPTY);
        Carried.offhand.set(0, card(5));
        Helper.assertTrue(DossierClearance.highest(Reader) == 5, "a card in the offhand was missed");
        Carried.offhand.set(0, ItemStack.EMPTY);
        Helper.assertTrue(DossierClearance.highest(Reader) == DossierClearance.None, "a dropped card still counts");
        Carried.setItem(3, card(1));
        Carried.setItem(Main, card(3));
        Carried.offhand.set(0, card(2));
        Helper.assertTrue(DossierClearance.highest(Reader) == 3, "the best card does not win: " + DossierClearance.highest(Reader));
        Carried.setItem(12, card(KeycardItem.OmniClearance));
        Helper.assertTrue(DossierClearance.highest(Reader) == DossierClearance.Omni, "the omni card does not win");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "dossier_clearance_parse")
    public static void redactionsSplitAndReveal(GameTestHelper Helper) {
        String Text = "a %rb c%r d %re%%f%r. **g** %rh%r";
        List<DossierLayout.Glyph> Glyphs = DossierLayout.parse(Text);
        StringBuilder Kinds = new StringBuilder();
        StringBuilder Letters = new StringBuilder();
        for (DossierLayout.Glyph Entry : Glyphs) {
            Kinds.append(Entry.Kind());
            Letters.append(Entry.Character());
        }
        Helper.assertTrue(Letters.toString().equals("a b c d e%f. g h"), "parsed letters " + Letters);
        Helper.assertTrue(Kinds.toString().equals("0022200022200102"), "parsed kinds " + Kinds);
        Helper.assertTrue(DossierLayout.plain(Text).equals("a ███ d ███. g █"), "redacted text " + DossierLayout.plain(Text));
        Helper.assertTrue(DossierLayout.plain(Text, false).equals(DossierLayout.plain(Text)), "the redacted default changed");
        Helper.assertTrue(DossierLayout.plain(Text, true).equals("a b c d e%f. g h"), "revealed text " + DossierLayout.plain(Text, true));
        Helper.assertTrue(DossierLayout.plain("%rREDACTED%r", true).equals("REDACTED"), "a lone redaction did not reveal");
        Helper.assertTrue(DossierLayout.plain("no marks", true).equals("no marks"), "plain text changed when revealed");
        Helper.succeed();
    }

    private static String joined(List<DossierLayout.Glyph> Line) {
        StringBuilder Builder = new StringBuilder();
        for (DossierLayout.Glyph Entry : Line) {
            Builder.append(Entry.Character());
        }
        return Builder.toString();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "dossier_clearance_wrap")
    public static void longRedactionsWrapBetweenWords(GameTestHelper Helper) {
        DossierLayout.Measure Fixed = Entry -> Entry.Character() == ' ' ? 4 : 6;
        List<List<DossierLayout.Glyph>> Lines = DossierLayout.lines(DossierLayout.parse("Lead in. %rone two three four five six seven%r. Tail"), 60, Fixed);
        List<String> Texts = Lines.stream().map(DossierClearanceGameTests::joined).toList();
        Helper.assertTrue(Texts.equals(List.of("Lead in.", "one two", "three four", "five six", "seven.", "Tail")), "a long redaction wrapped as " + Texts);
        for (List<DossierLayout.Glyph> Line : Lines) {
            Helper.assertTrue(Line.get(0).Character() != ' ' && Line.get(Line.size() - 1).Character() != ' ', "a wrapped line starts or ends on a space: " + joined(Line));
        }
        List<String> Short = DossierLayout.lines(DossierLayout.parse("ab %rcd ef%r gh"), 60, Fixed).stream().map(DossierClearanceGameTests::joined).toList();
        Helper.assertTrue(Short.equals(List.of("ab cd ef gh")), "a short redaction moved: " + Short);
        List<String> Whole = DossierLayout.lines(DossierLayout.parse("ab %rcdefghijklmn%r"), 60, Fixed).stream().map(DossierClearanceGameTests::joined).toList();
        Helper.assertTrue(Whole.equals(List.of("ab", "cdefghijkl", "mn")), "a redaction with no gap did not fall back to splitting: " + Whole);
        Helper.succeed();
    }
}
