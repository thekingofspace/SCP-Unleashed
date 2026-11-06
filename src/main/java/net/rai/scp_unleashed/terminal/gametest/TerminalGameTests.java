package net.rai.scp_unleashed.terminal.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DossierContent;
import net.rai.scp_unleashed.arrival.ArrivalCards;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.terminal.TerminalArchive;
import net.rai.scp_unleashed.terminal.TerminalAux;
import net.rai.scp_unleashed.terminal.TerminalHighlight;
import net.rai.scp_unleashed.terminal.TerminalArchiveIndex;
import net.rai.scp_unleashed.terminal.TerminalBoot;
import net.rai.scp_unleashed.terminal.TerminalChunks;
import net.rai.scp_unleashed.terminal.TerminalColour;
import net.rai.scp_unleashed.terminal.TerminalDimensions;
import net.rai.scp_unleashed.terminal.TerminalEta;
import net.rai.scp_unleashed.terminal.TerminalGate;
import net.rai.scp_unleashed.terminal.TerminalLog;
import net.rai.scp_unleashed.terminal.TerminalMotd;
import net.rai.scp_unleashed.terminal.TerminalPalette;
import net.rai.scp_unleashed.terminal.TerminalPhase;
import net.rai.scp_unleashed.terminal.TerminalProgress;
import net.rai.scp_unleashed.terminal.TerminalTags;
import net.rai.scp_unleashed.terminal.TerminalWorldTags;
import net.rai.scp_unleashed.terminal.TerminalText;
import net.neoforged.fml.ModList;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class TerminalGameTests {
    public static final String Arena = "scp420j_arena";

    private TerminalGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_tags")
    public static void tagsShareOneWidthAndCentreTheLabel(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            int Longest = TerminalTags.Labels.stream().mapToInt(String::length).max().orElse(0);
            Helper.assertTrue(TerminalTags.Inner == Longest + 2, "the tag width is not the longest label plus one space each side: " + TerminalTags.Inner);
            for (String Label : TerminalTags.Labels) {
                String Tag = TerminalTags.tag(Label);
                Helper.assertTrue(Tag.length() == TerminalTags.Width, Label + " tag is " + Tag.length() + " wide: '" + Tag + "'");
                Helper.assertTrue(Tag.startsWith("[") && Tag.endsWith("]"), Label + " tag lost its brackets: " + Tag);
                String Inside = Tag.substring(1, Tag.length() - 1);
                int Left = Inside.indexOf(Label);
                int Right = Inside.length() - Left - Label.length();
                int Spare = TerminalTags.Inner - Label.length();
                Helper.assertTrue(Left >= 0 && Inside.strip().equals(Label), Label + " is not inside its tag: " + Tag);
                Helper.assertTrue(Left == Spare / 2 && Right == Spare - Spare / 2, Label + " is not centred with the odd space on the right: " + Tag);
                Helper.assertTrue(Left >= 1 && Right - Left <= 1, Label + " has too much space: " + Tag);
                if (Label.length() == Longest) {
                    Helper.assertTrue(Left == 1 && Right == 1, Label + " should read with one space each side: " + Tag);
                }
            }
            Helper.assertTrue("[ GEN ]".equals(TerminalTags.tag(TerminalTags.Gen)) && "[ OK  ]".equals(TerminalTags.tag(TerminalTags.Ok)), "tag shapes drifted");
            BufferedImage Sheet = fontSheet();
            Helper.assertTrue(Sheet != null, "the terminal font sheet was not found");
            int Cell = TerminalTags.CellPixels;
            int OpenStart = ink(Sheet, '[', true);
            int CloseEnd = TerminalTags.closeOffset(Cell) + ink(Sheet, ']', false) + 1;
            double Middle = (OpenStart + CloseEnd) / 2.0;
            int OpenEnd = ink(Sheet, '[', false) + 1;
            int CloseStart = TerminalTags.closeOffset(Cell) + ink(Sheet, ']', true);
            for (String Label : TerminalTags.Labels) {
                int LabelX = TerminalTags.labelOffset(Label.length() * Cell, Cell);
                int Start = LabelX + ink(Sheet, Label.charAt(0), true);
                int End = LabelX + (Label.length() - 1) * Cell + ink(Sheet, Label.charAt(Label.length() - 1), false) + 1;
                double Centre = (Start + End) / 2.0;
                Helper.assertTrue(Math.abs(Centre - Middle) <= 1.0, Label + " is " + (Centre - Middle) + " px off the bracket centre");
                Helper.assertTrue(Start > OpenEnd && End < CloseStart, Label + " touches its brackets");
            }
            Helper.assertTrue(TerminalTags.textOffset(Cell) > CloseEnd, "the message column overlaps the closing bracket");
            for (TerminalPhase Phase : TerminalPhase.values()) {
                Helper.assertTrue(Phase.tag().length() == TerminalTags.Width, Phase + " prints a tag of another width");
            }
            for (int Index = 0; Index < TerminalMotd.IdleCount; Index++) {
                Helper.assertTrue(TerminalTags.tag(TerminalMotd.idleTag(Index)).length() == TerminalTags.Width, "idle tag " + Index + " has another width");
            }
            Helper.assertTrue(TerminalTags.tag("TOOLONGLABEL").length() == TerminalTags.Width, "a long label stretched the tag");
            for (int Step = 0; Step < 20; Step++) {
                Helper.assertTrue(TerminalTags.spinner(Step).length() == TerminalTags.Width, "the spinner changes width at step " + Step);
            }
            Helper.succeed();
        });
    }

    private static BufferedImage fontSheet() {
        try {
            Path Sheet = ModList.get().getModFileById(Scp_unleashed.ModId).getFile().findResource("assets", Scp_unleashed.ModId, "textures", "font", "terminal.png");
            try (InputStream Input = Files.newInputStream(Sheet)) {
                return ImageIO.read(Input);
            }
        } catch (Exception Failure) {
            return null;
        }
    }

    private static int ink(BufferedImage Sheet, char Glyph, boolean Leftmost) {
        int Code = Glyph - 0x20;
        int CellX = (Code % 16) * 8;
        int CellY = (Code / 16) * 8;
        int Found = -1;
        for (int X = 0; X < 8; X++) {
            for (int Y = 0; Y < 8; Y++) {
                if ((Sheet.getRGB(CellX + X, CellY + Y) >>> 24) >= 128) {
                    if (Leftmost) {
                        return X;
                    }
                    Found = X;
                }
            }
        }
        return Found;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_text")
    public static void longLinesWrapWithoutCutting(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            String Line = "ARCHIVE: 112 ANOMALIES INDEXED AND CROSS REFERENCED WITH SITE RECORDS";
            for (int Columns : new int[]{10, 18, 30, 80}) {
                List<String> Rows = TerminalText.wrap(Line, Columns);
                Helper.assertTrue(Rows.stream().allMatch(Each -> Each.length() <= Columns), "a wrapped row is wider than " + Columns);
                Helper.assertTrue(String.join(" ", Rows).equals(Line), "wrapping lost text at " + Columns + ": " + Rows);
                Helper.assertTrue(Rows.stream().noneMatch(Each -> Each.endsWith("...")), "wrapping cut a row with an ellipsis");
            }
            Helper.assertTrue(TerminalText.wrap("ABCDEFGHIJKL", 5).equals(List.of("ABCDE", "FGHIJ", "KL")), "a word longer than the row was not split");
            List<String> Parts = List.of("30 SAFE", "41 EUCLID", "33 KETER", "1 THAUMIEL", "4 Z-CLASS", "1 UNKNOWN");
            for (int Columns : new int[]{12, 24, 40, 200}) {
                List<String> Rows = TerminalText.pack(Parts, Columns);
                Helper.assertTrue(Rows.stream().allMatch(Each -> Each.length() <= Columns), "a packed row is wider than " + Columns + ": " + Rows);
                Helper.assertTrue(TerminalText.parts(String.join(TerminalText.Separator, Rows)).equals(Parts), "packing split or lost a class at " + Columns + ": " + Rows);
            }
            Helper.assertTrue(TerminalText.pack(Parts, 200).size() == 1, "the breakdown wrapped although it fits");
            Helper.assertTrue(TerminalText.upper("heartbeat nominal").equals("HEARTBEAT NOMINAL"), "log text is not uppercased");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_progress")
    public static void progressBarFormatting(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Helper.assertTrue("[----------] 0%".equals(TerminalProgress.bar(0, 10)), "0%: " + TerminalProgress.bar(0, 10));
            Helper.assertTrue("[#####-----] 52%".equals(TerminalProgress.bar(52, 10)), "52%: " + TerminalProgress.bar(52, 10));
            Helper.assertTrue("[##########] 100%".equals(TerminalProgress.bar(100, 10)), "100%: " + TerminalProgress.bar(100, 10));
            Helper.assertTrue("[----------] 0%".equals(TerminalProgress.bar(-7, 10)), "negative percent was not clamped");
            Helper.assertTrue("[##########] 100%".equals(TerminalProgress.bar(140, 10)), "overflowing percent was not clamped");
            int FullDiameter = 7;
            int GridDiameter = 11;
            int Total = TerminalProgress.readyTotal(FullDiameter);
            Helper.assertTrue(Total == 49, "the ready total should cover the full spawn square, not the border ring");
            for (int Percent = 0; Percent <= 100; Percent += 10) {
                int Full = Math.round(Percent * Total / 100.0f);
                int Border = GridDiameter * GridDiameter - Total;
                int Ready = TerminalProgress.ready(Full, FullDiameter);
                Helper.assertTrue(Ready == Full, "ready cells " + Ready + " do not match " + Full + " full chunks");
                Helper.assertTrue(TerminalProgress.agrees(Percent, Ready, Total), Percent + "% disagrees with " + Ready + "/" + Total + " cells ready");
                Helper.assertTrue(TerminalProgress.ready(Full + Border, FullDiameter) <= Total, "border chunks pushed the count past the total");
            }
            Helper.assertTrue(!TerminalProgress.agrees(30, 0, Total) && !TerminalProgress.agrees(30, 40, Total), "contradicting counts were accepted");
            Helper.assertTrue(TerminalProgress.percent(0.999f) == 99 && TerminalProgress.percent(1.0f) == 100 && TerminalProgress.percent(Float.NaN) == 0, "fraction rounding");
            for (int Cells : new int[]{10, 20, 54}) {
                for (int Percent = 0; Percent <= 100; Percent += 7) {
                    String Inside = TerminalProgress.cells(Percent, Cells);
                    Helper.assertTrue(Inside.length() == Cells, "the bar changes length at " + Percent + "%");
                }
                for (long Now = 0; Now < 5000; Now += 37) {
                    String Scan = TerminalProgress.scanner(Now, Cells);
                    Helper.assertTrue(Scan.length() == Cells + 2, "the scanner changes length");
                    Helper.assertTrue(Scan.chars().filter(Each -> Each == TerminalProgress.Fill).count() == TerminalProgress.ScanWidth, "the scanner lost its block: " + Scan);
                }
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_phases")
    public static void phasesMapToLines(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Helper.assertTrue(TerminalPhase.fromConnectStatus("connect.connecting") == TerminalPhase.Connect, "connecting");
            Helper.assertTrue(TerminalPhase.fromConnectStatus("connect.encrypting") == TerminalPhase.Connect, "encrypting");
            Helper.assertTrue(TerminalPhase.fromConnectStatus("connect.authorizing") == TerminalPhase.Auth, "authorizing");
            Helper.assertTrue(TerminalPhase.fromConnectStatus("connect.joining") == TerminalPhase.Join, "joining");
            Helper.assertTrue(TerminalPhase.fromConnectStatus("connect.reconfiguring") == TerminalPhase.Sync, "reconfiguring");
            Helper.assertTrue(TerminalPhase.fromConnectStatus(null) == TerminalPhase.Connect, "missing status");
            Helper.assertTrue(TerminalPhase.fromMessageKey("menu.savingLevel") == TerminalPhase.Save, "saving");
            Helper.assertTrue(TerminalPhase.fromMessageKey("selectWorld.data_read") == TerminalPhase.Prepare, "reading data");
            int Previous = -1;
            for (TerminalPhase Each : List.of(TerminalPhase.Connect, TerminalPhase.Auth, TerminalPhase.Sync, TerminalPhase.Join)) {
                Helper.assertTrue(Each.Staged > Previous && Each.Staged < 100, Each + " is not a rising stage");
                Previous = Each.Staged;
            }
            TerminalLog Log = new TerminalLog(42L);
            Log.update(0L, TerminalPhase.Generate, 0, -1, "");
            Log.update(50L, TerminalPhase.Generate, 34, -1, "");
            Log.update(60L, TerminalPhase.Generate, 36, -1, "");
            List<TerminalLog.Entry> Entries = Log.entries();
            Helper.assertTrue(Entries.get(0).Key().equals(TerminalPhase.Generate.openKey()), "the phase did not open with its header line");
            long Progress = Entries.stream().filter(Each -> TerminalPhase.Generate.progressKey().equals(Each.Key())).count();
            Helper.assertTrue(Progress == 2, "expected one progress line per decile, got " + Progress);
            Helper.assertTrue(Entries.stream().anyMatch(Each -> TerminalPhase.Generate.progressKey().equals(Each.Key()) && Each.Args().get(0).equals("34")),
                    "the 34% line does not carry the real percent");
            for (long Now = 100L; Now < 20000L; Now += 50L) {
                Log.update(Now, TerminalPhase.Generate, 40, -1, "");
            }
            long Flavours = Log.entries().stream().filter(Each -> Each.Key() != null && Each.Key().contains(".flavour.")).count();
            Helper.assertTrue(Flavours == TerminalPhase.Generate.Flavours, "flavour lines did not stop after one pass: " + Flavours);
            Log.update(20000L, TerminalPhase.Terrain, -1, 10, "");
            Log.update(20010L, TerminalPhase.Terrain, -1, 20, "");
            Log.update(20020L, TerminalPhase.Terrain, -1, 40, "");
            long Counts = Log.entries().stream().filter(Each -> TerminalPhase.Terrain.countKey().equals(Each.Key())).count();
            Helper.assertTrue(Counts == 2, "count lines should appear at the first count and every step, got " + Counts);
            Helper.assertTrue(Log.entries().size() <= TerminalLog.Limit, "the log grew past its limit");
            for (TerminalLog.Entry Each : Log.entries()) {
                Helper.assertTrue(Each.tag().length() == TerminalTags.Width, "a log line has a misaligned tag");
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_gate")
    public static void stylingAppliesOnlyToTitleAndLoadingScreens(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Set<String> Covered = Set.of("net.minecraft.client.gui.screens.TitleScreen", "net.minecraft.client.gui.screens.LevelLoadingScreen",
                    "net.minecraft.client.gui.screens.ReceivingLevelScreen", "net.minecraft.client.gui.screens.ConnectScreen",
                    "net.minecraft.client.gui.screens.GenericMessageScreen", "net.minecraft.client.gui.screens.ProgressScreen",
                    "net.minecraft.client.gui.screens.DisconnectedScreen",
                    "net.minecraft.client.gui.screens.worldselection.SelectWorldScreen",
                    "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen",
                    "net.minecraft.client.gui.screens.worldselection.EditWorldScreen");
            Helper.assertTrue(TerminalGate.Covered.keySet().equals(Covered), "the covered screen list drifted: " + TerminalGate.Covered.keySet());
            List<String> Untouched = List.of(
                    "net.minecraft.client.gui.screens.options.OptionsScreen",
                    "net.minecraft.client.gui.screens.options.VideoSettingsScreen",
                    "net.minecraft.client.gui.screens.options.SoundOptionsScreen",
                    "net.minecraft.client.gui.screens.options.controls.ControlsScreen",
                    "net.minecraft.client.gui.screens.options.controls.KeyBindsScreen",
                    "net.minecraft.client.gui.screens.options.LanguageSelectScreen",
                    "net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen",
                    "net.minecraft.client.gui.screens.options.ChatOptionsScreen",
                    "net.minecraft.client.gui.screens.options.SkinCustomizationScreen",
                    "net.minecraft.client.gui.screens.options.OnlineOptionsScreen",
                    "net.minecraft.client.gui.screens.options.MouseSettingsScreen",
                    "net.minecraft.client.gui.screens.PauseScreen",
                    "net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen",
                    "net.minecraft.client.gui.screens.worldselection.ExperimentsScreen",
                    "net.minecraft.client.gui.screens.packs.PackSelectionScreen",
                    "net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen",
                    "net.minecraft.client.gui.screens.ManageServerScreen",
                    "net.minecraft.client.gui.screens.DirectJoinServerScreen",
                    "net.neoforged.neoforge.client.gui.ModListScreen",
                    "net.neoforged.neoforge.client.gui.ConfigurationScreen",
                    "net.neoforged.neoforge.client.gui.ConfigurationScreen$ConfigurationSectionScreen",
                    "com.example.othermod.FancyTitleScreen",
                    "net.minecraft.client.gui.screens.TitleScreen$Sub",
                    "");
            for (String Name : Untouched) {
                Helper.assertTrue(TerminalGate.kindOf(Name) == TerminalGate.Kind.None, Name + " would be restyled");
                for (boolean Menu : new boolean[]{true, false}) {
                    for (boolean Loading : new boolean[]{true, false}) {
                        for (boolean Worlds : new boolean[]{true, false}) {
                            Helper.assertTrue(!TerminalGate.applies(Name, true, Menu, Loading, Worlds), Name + " passes the gate");
                        }
                    }
                }
            }
            Helper.assertTrue(TerminalGate.kindOf(null) == TerminalGate.Kind.None, "a missing class name was covered");
            Helper.assertTrue(TerminalGate.applies("net.minecraft.client.gui.screens.TitleScreen", true, true, false), "title gate with the menu on");
            Helper.assertTrue(!TerminalGate.applies("net.minecraft.client.gui.screens.TitleScreen", true, false, true), "title gate with the menu off");
            Helper.assertTrue(TerminalGate.applies("net.minecraft.client.gui.screens.LevelLoadingScreen", true, false, true), "loading gate with loading on");
            Helper.assertTrue(!TerminalGate.applies("net.minecraft.client.gui.screens.LevelLoadingScreen", true, true, false), "loading gate with loading off");
            Helper.assertTrue(!TerminalGate.dimensionScreen(true, true, false, false), "a cutscene departure got the terminal");
            Helper.assertTrue(!TerminalGate.dimensionScreen(true, false, true, false), "a cutscene arrival got the terminal");
            Helper.assertTrue(!TerminalGate.dimensionScreen(true, false, false, true), "a running cinematic got the terminal");
            Helper.assertTrue(TerminalGate.dimensionScreen(true, false, false, false), "a plain dimension change lost the terminal");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_worlds_gate")
    public static void worldScreensFollowTheirOption(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            List<String> Worlds = List.of("net.minecraft.client.gui.screens.worldselection.SelectWorldScreen",
                    "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen", "net.minecraft.client.gui.screens.worldselection.EditWorldScreen");
            for (String Name : Worlds) {
                Helper.assertTrue(TerminalGate.kindOf(Name) == TerminalGate.Kind.Worlds, Name + " is not a world screen");
                Helper.assertTrue(TerminalGate.applies(Name, true, false, false, true), Name + " is not restyled with the world option on");
                Helper.assertTrue(!TerminalGate.applies(Name, true, true, true, false), Name + " is restyled with the world option off");
                Helper.assertTrue(!TerminalGate.applies(Name, false, true, true, true), Name + " is restyled with the master switch off");
                Helper.assertTrue(!TerminalGate.applies(Name, true, true, true), Name + " is restyled by the old gate");
            }
            Helper.assertTrue(TerminalGate.kindOf("net.minecraft.client.gui.screens.options.OptionsScreen") == TerminalGate.Kind.None, "Options became covered");
            Helper.assertTrue(ScpClientConfig.TerminalWorlds.getDefault(), "terminalWorlds should default on");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_opening")
    public static void decorationOnlyPaintsAndNeverBlocks(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            List<String> Names = new ArrayList<>(TerminalGate.Covered.keySet());
            Names.addAll(List.of("net.minecraft.client.gui.screens.options.OptionsScreen", "net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen",
                    "net.minecraft.client.gui.screens.ManageServerScreen", "net.minecraft.client.gui.screens.DirectJoinServerScreen",
                    "net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen", "com.example.othermod.WorldScreen"));
            for (String Name : Names) {
                TerminalGate.Kind Which = TerminalGate.kindOf(Name);
                for (boolean Menu : new boolean[]{true, false}) {
                    TerminalGate.Decision Choice = TerminalGate.opening(Name, false, Menu);
                    if (Which == TerminalGate.Kind.Title) {
                        Helper.assertTrue(Choice == (Menu ? TerminalGate.Decision.Terminal : TerminalGate.Decision.Keep), "the title swap is wrong for " + Name);
                    } else {
                        Helper.assertTrue(Choice == TerminalGate.Decision.Keep, Name + " would be replaced on opening");
                        Helper.assertTrue(TerminalGate.paintsOnly(Which), Name + " is not paint only");
                    }
                }
            }
            Helper.assertTrue(TerminalGate.opening("net.rai.scp_unleashed.terminal.client.TerminalTitleScreen", true, true) == TerminalGate.Decision.Keep, "the terminal title was swapped again");
            Helper.assertTrue(TerminalGate.opening("net.rai.scp_unleashed.terminal.client.TerminalTitleScreen", true, false) == TerminalGate.Decision.Vanilla, "the terminal title stayed with the menu off");
            Helper.assertTrue(!TerminalGate.paintsOnly(TerminalGate.Kind.Title), "the title is not a swap");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_skip")
    public static void loadingTerrainSkipsOnlyCutscenes(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Helper.assertTrue(!TerminalGate.skipReceiving(true, false, false, false), "a fresh world join fell back to vanilla");
            Helper.assertTrue(!TerminalGate.skipReceiving(true, false, false, true), "a fresh world join with the intro starting fell back to vanilla");
            Helper.assertTrue(!TerminalGate.skipReceiving(true, true, false, true), "a join after leaving a cutscene world earlier fell back to vanilla");
            Helper.assertTrue(!TerminalGate.skipReceiving(false, false, false, false), "a plain dimension change fell back to vanilla");
            Helper.assertTrue(TerminalGate.skipReceiving(true, false, true, false), "joining straight into a cutscene dimension got the terminal");
            Helper.assertTrue(TerminalGate.skipReceiving(false, false, true, false), "travelling into the intro got the terminal");
            Helper.assertTrue(TerminalGate.skipReceiving(false, true, false, false), "returning from the 902 wastes got the terminal");
            Helper.assertTrue(TerminalGate.skipReceiving(false, false, false, true), "a change during a 2521 scene got the terminal");
            Helper.assertTrue(ArrivalCards.cutsceneDimension(ArrivalCards.Intro) && ArrivalCards.cutsceneDimension(ArrivalCards.Wastes902)
                    && ArrivalCards.cutsceneDimension(ArrivalCards.Void2521) && !ArrivalCards.cutsceneDimension(ArrivalCards.Pocket), "the cutscene dimension list drifted");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_world_rows")
    public static void worldRowsUseTagsAndLeaders(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            for (String Label : TerminalWorldTags.Labels) {
                Helper.assertTrue(Label.length() <= TerminalTags.Inner - 2, Label + " is too long for a centred tag");
                Helper.assertTrue(TerminalTags.tag(Label).length() == TerminalTags.Width, Label + " changes the tag width");
            }
            Helper.assertTrue(TerminalWorldTags.tags("survival", false, false, 3955, 3955, false, false, true, false).equals(List.of(TerminalWorldTags.Survival)), "plain survival world");
            Helper.assertTrue(TerminalWorldTags.tags("creative", false, true, 3955, 3955, false, false, true, false).equals(List.of(TerminalWorldTags.Creative, TerminalWorldTags.Cheats)), "creative with cheats");
            Helper.assertTrue(TerminalWorldTags.tags("survival", true, false, 3700, 3955, false, false, true, false).equals(List.of(TerminalWorldTags.Hardcore, TerminalWorldTags.Older)), "old hardcore world");
            Helper.assertTrue(TerminalWorldTags.tags("adventure", false, false, 4000, 3955, true, true, false, false)
                    .equals(List.of(TerminalWorldTags.Adventure, TerminalWorldTags.Newer, TerminalWorldTags.Experimental, TerminalWorldTags.Locked, TerminalWorldTags.Broken)), "problem world");
            for (int Columns : new int[]{20, 25, 35, 60}) {
                String Line = TerminalText.leader("GAME MODE", "SURVIVAL", Columns);
                Helper.assertTrue(Line.length() == Columns, "leader is " + Line.length() + " wide, wanted " + Columns + ": " + Line);
                Helper.assertTrue(Line.endsWith("[ SURVIVAL ]") && Line.startsWith(Columns >= 25 ? "GAME MODE" : "GAM"), "leader lost its parts: " + Line);
            }
            Helper.assertTrue(TerminalText.leader("GAME MODE", "SURVIVAL", 35).startsWith("GAME MODE ...."), "the dotted leader is missing");
            Helper.assertTrue(TerminalText.leader("A VERY LONG MODDED PRESET LABEL", "AMPLIFIED", 25).endsWith("[ AMPLIFIED ]"), "the value was cut instead of the label");
            Helper.assertTrue(TerminalText.leader("X", "WIDE VALUE TEXT", 10).equals("[ WIDE VALUE TEXT ]"), "an oversized value was cut");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_aux")
    public static void auxAlwaysOffersConfigAndArchive(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            List<String> Expected = List.of(TerminalAux.ModConfig, TerminalAux.ScpConfig, TerminalAux.Archive);
            Helper.assertTrue(TerminalAux.order(List.of()).equals(Expected), "AUX without other mods: " + TerminalAux.order(List.of()));
            List<String> WithMods = TerminalAux.order(List.of("plain:0", "native:0", TerminalAux.Archive));
            Helper.assertTrue(WithMods.subList(0, 3).equals(Expected) && WithMods.size() == 5, "AUX with other mods: " + WithMods);
            List<TerminalAux.Item> Items = new ArrayList<>();
            for (String Id : Expected) {
                Items.add(new TerminalAux.Item(Id, 132, 11, true));
            }
            Items.add(new TerminalAux.Item("plain:0", 132, 11, true));
            Items.add(new TerminalAux.Item("native:0", 100, 20, false));
            Items.add(new TerminalAux.Item("native:1", 20, 20, false));
            Items.add(new TerminalAux.Item("native:2", 20, 20, false));
            int Left = 10;
            int Top = 170;
            int Limit = 240;
            List<TerminalAux.Spot> Spots = TerminalAux.place(Items, Left, Top, Limit, 132, 12, 8, 24, 4);
            Helper.assertTrue(Spots.size() == Items.size(), "the layout dropped a widget: " + Spots);
            for (TerminalAux.Spot Spot : Spots) {
                Helper.assertTrue(Spot.Width() > 0 && Spot.Height() > 0 && Spot.X() >= Left && Spot.Y() >= Top, Spot.Id() + " has no clickable area: " + Spot);
                Helper.assertTrue(Spot.Y() + Spot.Height() <= Limit, Spot.Id() + " runs into the footer: " + Spot);
                for (TerminalAux.Spot Other : Spots) {
                    Helper.assertTrue(Spot == Other || !Spot.overlaps(Other), Spot.Id() + " overlaps " + Other.Id());
                }
            }
            Helper.assertTrue(Spots.get(0).Id().equals(TerminalAux.ModConfig) && Spots.get(0).Y() == Top, "MOD CONFIG is not the first AUX row");
            TerminalAux.Spot Fake = Spots.stream().filter(Each -> Each.Id().equals("native:0")).findFirst().orElseThrow();
            Helper.assertTrue(Fake.Width() == 100 && Fake.Height() == 20, "a mod widget was resized: " + Fake);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_highlight")
    public static void highlightComesFromHoverOrFocus(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Helper.assertTrue(TerminalHighlight.lit(true, false, true, false, true), "a hovered row is not lit");
            Helper.assertTrue(TerminalHighlight.lit(false, true, false, true, false), "a focused row is not lit with the keyboard");
            Helper.assertTrue(!TerminalHighlight.lit(false, false, true, true, true) && !TerminalHighlight.lit(false, false, false, false, false), "an idle row is lit");
            Helper.assertTrue(!TerminalHighlight.lit(false, true, true, true, true), "focus stayed lit while the mouse hovers another row");
            Helper.assertTrue(!TerminalHighlight.lit(true, false, true, true, false), "hover stayed lit while the keyboard moves focus");
            Helper.assertTrue(TerminalHighlight.lit(false, true, false, true, true), "focus vanished when the mouse is away from every row");
            for (int Rows = 1; Rows <= 5; Rows++) {
                for (int Hover = -1; Hover < Rows; Hover++) {
                    for (int Focus = -1; Focus < Rows; Focus++) {
                        for (boolean MouseMode : new boolean[]{true, false}) {
                            int Lit = 0;
                            for (int Row = 0; Row < Rows; Row++) {
                                if (TerminalHighlight.lit(Row == Hover, Row == Focus, Hover >= 0, Focus >= 0, MouseMode)) {
                                    Lit++;
                                }
                            }
                            int Expected = Hover >= 0 || Focus >= 0 ? 1 : 0;
                            Helper.assertTrue(Lit == Expected, Lit + " rows lit with hover " + Hover + " focus " + Focus + " mouse " + MouseMode);
                        }
                    }
                }
            }
            Helper.assertTrue(!TerminalHighlight.mouseMode(true, false, true) && TerminalHighlight.mouseMode(false, true, false) && !TerminalHighlight.mouseMode(false, false, false),
                    "mouse and keyboard mode switching");
            Helper.assertTrue(TerminalHighlight.within(5, 5, 0, 0, 10, 10) && !TerminalHighlight.within(10, 5, 0, 0, 10, 10), "hover bounds");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_master")
    public static void masterSwitchDisablesEveryHook(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            for (TerminalGate.Kind Which : TerminalGate.Kind.values()) {
                for (boolean Menu : new boolean[]{true, false}) {
                    for (boolean Loading : new boolean[]{true, false}) {
                        Helper.assertTrue(!TerminalGate.applies(Which, false, Menu, Loading), Which + " still applies with the master switch off");
                    }
                }
            }
            for (String Name : TerminalGate.Covered.keySet()) {
                Helper.assertTrue(!TerminalGate.applies(Name, false, true, true), Name + " still applies with the master switch off");
            }
            Helper.assertTrue(!TerminalGate.sounds(false, true), "sounds play with the master switch off");
            Helper.assertTrue(!TerminalGate.effects(false, false), "screen effects run with the master switch off");
            Helper.assertTrue(TerminalGate.sounds(true, true) && !TerminalGate.sounds(true, false), "the sound toggle is ignored");
            Helper.assertTrue(ScpClientConfig.TerminalScreens.getDefault(), "terminalScreens should default on");
            Helper.assertTrue(ScpClientConfig.TerminalMenu.getDefault(), "terminalMenu should default on");
            Helper.assertTrue(ScpClientConfig.TerminalLoading.getDefault(), "terminalLoading should default on");
            Helper.assertTrue(ScpClientConfig.TerminalSounds.getDefault(), "terminalSounds should default on");
            Helper.assertTrue(!ScpClientConfig.TerminalReducedMotion.getDefault(), "reduced motion should default off");
            Helper.assertTrue(ScpClientConfig.TerminalTheme.getDefault() == TerminalColour.Green, "the theme should default to green");
            for (TerminalPalette Each : List.of(TerminalPalette.Green, TerminalPalette.Amber)) {
                Helper.assertTrue(TerminalPalette.contrast(Each.Main(), Each.Bg()) >= 7.0, "main text contrast too low");
                Helper.assertTrue(TerminalPalette.contrast(Each.Dim(), Each.Bg()) >= 3.0, "dim text contrast too low");
                Helper.assertTrue(TerminalPalette.contrast(Each.Bg(), Each.Main()) >= 7.0, "selected row contrast too low");
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_boot")
    public static void bootSequenceTimingAndSkip(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            for (boolean Census : new boolean[]{true, false}) {
                List<TerminalBoot.Step> Steps = TerminalBoot.script(Census);
                int[] English = new int[Steps.size()];
                for (int Index = 0; Index < Steps.size(); Index++) {
                    English[Index] = Steps.get(Index).Key().isEmpty() ? 0 : 30;
                }
                TerminalBoot Boot = new TerminalBoot(Steps, English);
                Helper.assertTrue(Boot.total() >= 2000 && Boot.total() <= TerminalBoot.MaxMs, "boot takes " + Boot.total() + " ms");
                Helper.assertTrue(Boot.elapsed(5000L) == 0L && !Boot.started(), "the boot clock ran before it began");
                Boot.begin(1000L);
                Helper.assertTrue(Boot.elapsed(1000L) == 0L && Boot.chars(0, 0L) == 0, "the first line was typed at once");
                Helper.assertTrue(!Boot.done(Boot.elapsed(1000L + Boot.total() - 1)), "the boot ended early");
                Helper.assertTrue(Boot.done(Boot.elapsed(1000L + Boot.total())), "the boot did not end on time");
                int Last = Boot.lines() - 1;
                Helper.assertTrue(Boot.chars(Last, Boot.total()) == English[Last], "the motto was not fully typed at the end");
                long Mid = Boot.total() / 2;
                Helper.assertTrue(!Boot.revealed(Mid), "the menu showed mid boot");
                Boot.skip();
                long Skipped = Boot.elapsed(1000L + Mid);
                Helper.assertTrue(Boot.done(Skipped) && Boot.revealed(Skipped), "skipping did not finish the boot");
                for (int Row = 0; Row < 12; Row++) {
                    Helper.assertTrue(Boot.rowShown(Row, Skipped), "row " + Row + " stayed hidden after a skip");
                }
                Helper.assertTrue(Boot.logoChars(Skipped, 13) == 13, "the logo did not finish after a skip");
                int[] Long = new int[Steps.size()];
                Arrays.fill(Long, 400);
                Helper.assertTrue(new TerminalBoot(Steps, Long).total() <= TerminalBoot.MaxMs + 1, "long translations stretched the boot");
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_eta")
    public static void etaIsSmoothAndHonest(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            TerminalEta Steady = new TerminalEta();
            double Previous = -1.0;
            long PreviousAt = 0L;
            for (long Now = 0L; Now <= 10000L; Now += 50L) {
                float Value = Math.min(1.0f, Now / 20000.0f);
                Steady.sample(Now, (float) Math.floor(Value * 100.0f) / 100.0f);
                double Shown = Steady.seconds();
                if (Now < TerminalEta.MinSpanMs) {
                    Helper.assertTrue(!Steady.known(), "an ETA showed before enough data at " + Now);
                }
                Helper.assertTrue(Shown == TerminalEta.Unknown || Shown >= 0.0, "negative ETA " + Shown);
                if (Now >= 3000L) {
                    double Expected = (20000L - Now) / 1000.0;
                    Helper.assertTrue(Steady.known() && Math.abs(Shown - Expected) <= 2.0, "steady ETA at " + Now + " is " + Shown + ", expected about " + Expected);
                    if (Previous >= 0.0) {
                        double Rise = Shown - Previous;
                        Helper.assertTrue(Rise <= TerminalEta.MaxRisePerSecond * (Now - PreviousAt) / 1000.0 + 0.11, "the ETA jumped up by " + Rise);
                    }
                }
                Previous = Shown;
                PreviousAt = Now;
            }
            for (long Now = 10050L; Now <= 10000L + TerminalEta.StallMs + 500L; Now += 50L) {
                Steady.sample(Now, 0.5f);
            }
            Helper.assertTrue(!Steady.known() && "--".equals(Steady.text()), "a stall still shows an ETA: " + Steady.text());
            TerminalEta Back = new TerminalEta();
            for (long Now = 0L; Now <= 3000L; Now += 100L) {
                Back.sample(Now, Now / 6000.0f);
            }
            Back.sample(3100L, 0.1f);
            Helper.assertTrue(!Back.known(), "progress going backwards kept the old ETA");
            TerminalEta Jitter = new TerminalEta();
            float Value = 0.0f;
            for (long Now = 0L; Now <= 12000L; Now += 50L) {
                Value = Math.min(0.99f, Value + ((Now / 50L) % 7L == 0L ? 0.02f : 0.0005f));
                Jitter.sample(Now, Value);
                Helper.assertTrue(Jitter.seconds() == TerminalEta.Unknown || Jitter.seconds() >= 0.0, "jittery progress gave a negative ETA");
            }
            Helper.assertTrue("--".equals(TerminalEta.format(-1.0)) && "~6s".equals(TerminalEta.format(5.2)) && "~1m 20s".equals(TerminalEta.format(80.0))
                    && "~60m+".equals(TerminalEta.format(99999.0)), "ETA formatting");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_chunks")
    public static void chunkFractionEstimator(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            int Total = TerminalChunks.total(TerminalChunks.Radius);
            Helper.assertTrue(Total == 25, "radius 2 should cover 25 chunks, got " + Total);
            Helper.assertTrue(TerminalChunks.fraction(false, Total, Total, true) == 0.0f, "progress before the player has a position");
            Helper.assertTrue(Math.abs(TerminalChunks.fraction(true, 0, Total, false) - TerminalChunks.PositionShare) < 1.0e-6f, "position only");
            Helper.assertTrue(TerminalChunks.fraction(true, Total, Total, true) == 1.0f, "everything loaded should be full");
            Helper.assertTrue(TerminalChunks.fraction(true, Total, Total, false) < TerminalChunks.fraction(true, Total, Total, true), "the own chunk does not count");
            float Last = -1.0f;
            for (int Loaded = 0; Loaded <= Total; Loaded++) {
                float Value = TerminalChunks.fraction(true, Loaded, Total, false);
                Helper.assertTrue(Value >= Last && Value >= 0.0f && Value <= 1.0f, "the fraction is not monotonic at " + Loaded);
                Last = Value;
            }
            Helper.assertTrue(TerminalChunks.fraction(true, Total * 3, Total, true) <= 1.0f && TerminalChunks.fraction(true, -5, Total, false) >= 0.0f, "out of range counts");
            Set<Long> Present = Set.of(pack(10, 10), pack(11, 10), pack(9, 9), pack(12, 12), pack(13, 13));
            int Found = TerminalChunks.count(10, 10, 2, (X, Z) -> Present.contains(pack(X, Z)));
            Helper.assertTrue(Found == 4, "the probe counted " + Found + " chunks inside radius 2, expected 4");
            Helper.succeed();
        });
    }

    private static long pack(int X, int Z) {
        return ((long) X << 32) ^ (Z & 0xFFFFFFFFL);
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_dimensions")
    public static void dimensionNamesUseArrivalCards(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Map<String, String> Words = Map.of(ArrivalCards.titleKey(ArrivalCards.Pocket), "THE POCKET DIMENSION", "terminal.scp_unleashed.dimension.minecraft.overworld", "THE OVERWORLD");
            ResourceLocation Pocket = ArrivalCards.Pocket.location();
            Helper.assertTrue(TerminalDimensions.candidates(Pocket).get(0).equals("arrival.scp_unleashed.scp106_pocket.title"), "the pocket dimension does not try its arrival card first");
            Helper.assertTrue("THE POCKET DIMENSION".equals(TerminalDimensions.name(Pocket, Words::containsKey, Words::get)), "arrival card name");
            Helper.assertTrue("THE OVERWORLD".equals(TerminalDimensions.name(ResourceLocation.withDefaultNamespace("overworld"), Words::containsKey, Words::get)), "vanilla name");
            ResourceLocation Unknown = ResourceLocation.fromNamespaceAndPath("othermod", "deep_lab");
            Helper.assertTrue("othermod:deep_lab".equals(TerminalDimensions.name(Unknown, Words::containsKey, Words::get)), "unknown dimensions fall back to the id");
            ResourceLocation Unlisted = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fresh_dimension");
            Helper.assertTrue("scp_unleashed:fresh_dimension".equals(TerminalDimensions.name(Unlisted, Words::containsKey, Words::get)), "mod dimension without a card");
            Language Words2 = Language.getInstance();
            for (var Each : ArrivalCards.Listed) {
                Helper.assertTrue(Words2.has(ArrivalCards.titleKey(Each)), "the arrival card title for " + Each.location() + " is missing");
                Helper.assertTrue(!TerminalDimensions.name(Each.location(), Words2::has, Words2::getOrDefault).contains(":"), Each.location() + " has no readable name");
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "terminal_archive_census")
    public static void archiveCensusMatchesTheDocuments(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Path Folder = TerminalArchive.folder();
            Helper.assertTrue(Folder != null && Files.isDirectory(Folder), "the anomaly document folder was not found in the mod file");
            List<String> Classes = new ArrayList<>();
            try (Stream<Path> Listing = Files.list(Folder)) {
                for (Path Each : Listing.filter(Name -> Name.getFileName().toString().endsWith(".json")).toList()) {
                    try (Reader Input = Files.newBufferedReader(Each)) {
                        JsonObject Body = JsonParser.parseReader(Input).getAsJsonObject();
                        Classes.add(Body.get("classification").getAsString());
                    }
                }
            } catch (Exception Failure) {
                throw new IllegalStateException(Failure);
            }
            TerminalArchive.Census Count = TerminalArchive.census().orElse(null);
            Helper.assertTrue(Count != null, "the census failed");
            Helper.assertTrue(Count.Total() == Classes.size(), "census total " + Count.Total() + " but " + Classes.size() + " files");
            Helper.assertTrue(Count.sum() == Count.Total(), "class sums " + Count.sum() + " do not add up to " + Count.Total());
            for (DocumentClass Each : DocumentClass.values()) {
                long Expected = Classes.stream().filter(Name -> TerminalArchive.classOf(Name) == Each).count();
                Helper.assertTrue(Count.of(Each) == Expected, Each + ": census " + Count.of(Each) + " files " + Expected);
            }
            Helper.assertTrue(Count.Total() >= 100, "suspiciously few anomalies: " + Count.Total());
            Helper.assertTrue(TerminalArchive.documents().size() == Count.Total(), "the viewer loaded " + TerminalArchive.documents().size() + " of " + Count.Total() + " documents");
            Helper.assertTrue(TerminalArchive.read(null) == null && TerminalArchive.read(Folder.resolve("missing_folder")) == null, "a missing folder produced a census");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_archive_codec")
    public static void addedVersionDefaultsToTheCurrentVersion(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            String Current = AnomalyDocument.currentVersion();
            Helper.assertTrue(!Current.equals(AnomalyDocument.FallbackVersion), "the running mod version was not found");
            JsonObject Without = JsonParser.parseString("{\"classification\": \"safe\", \"danger\": 1}").getAsJsonObject();
            AnomalyDocument Missing = AnomalyDocument.DocumentCodec.parse(JsonOps.INSTANCE, Without).getOrThrow();
            Helper.assertTrue(Missing.Added().isEmpty() && Missing.addedVersion().equals(Current) && Missing.isNew(), "a missing added field is not the current version");
            JsonObject Old = JsonParser.parseString("{\"classification\": \"keter\", \"danger\": 5, \"added\": \"0.0.1\"}").getAsJsonObject();
            AnomalyDocument Older = AnomalyDocument.DocumentCodec.parse(JsonOps.INSTANCE, Old).getOrThrow();
            Helper.assertTrue(Older.addedVersion().equals("0.0.1") && !Older.isNew(), "an old document counts as new");
            JsonObject Encoded = AnomalyDocument.DocumentCodec.encodeStart(JsonOps.INSTANCE, Older).getOrThrow().getAsJsonObject();
            Helper.assertTrue("0.0.1".equals(Encoded.get(AnomalyDocument.AddedField).getAsString()), "the added field does not round trip");
            Helper.assertTrue(!AnomalyDocument.DocumentCodec.encodeStart(JsonOps.INSTANCE, Missing).getOrThrow().getAsJsonObject().has(AnomalyDocument.AddedField),
                    "a missing added field was written out");
            Helper.assertTrue(Older.withLang("x").Added().equals(Older.Added()), "withLang dropped the added field");
            for (Map.Entry<ResourceLocation, AnomalyDocument> Each : TerminalArchive.documents().entrySet()) {
                Helper.assertTrue(!Each.getValue().addedVersion().isBlank(), Each.getKey() + " has no version");
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_archive_sort")
    public static void archiveSortsNumericallyThenZClass(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            List<String> Paths = List.of("z_0002", "scp_3008", "scp_173", "scp_420_j", "z_0001", "scp_049", "scp_002", "scp_1762", "scp_420");
            List<TerminalArchiveIndex.Row> Rows = new ArrayList<>();
            for (String Each : Paths) {
                Rows.add(row(Each, Each.toUpperCase(), "Title " + Each, DocumentClass.Safe, false));
            }
            List<String> Sorted = TerminalArchiveIndex.sorted(Rows).stream().map(Each -> Each.Id().getPath()).toList();
            List<String> Expected = List.of("scp_002", "scp_049", "scp_173", "scp_420", "scp_420_j", "scp_1762", "scp_3008", "z_0001", "z_0002");
            Helper.assertTrue(Sorted.equals(Expected), "archive order " + Sorted);
            List<TerminalArchiveIndex.Row> Real = new ArrayList<>();
            for (Map.Entry<ResourceLocation, AnomalyDocument> Each : TerminalArchive.documents().entrySet()) {
                Real.add(row(Each.getKey().getPath(), "", "", Each.getValue().Classification(), false));
            }
            List<TerminalArchiveIndex.Row> RealSorted = TerminalArchiveIndex.sorted(Real);
            boolean SeenZ = false;
            long LastNumber = -1L;
            for (TerminalArchiveIndex.Row Each : RealSorted) {
                TerminalArchiveIndex.SortKey Key = TerminalArchiveIndex.key(Each.Id().getPath());
                if (Key.Group() == 1) {
                    SeenZ = true;
                } else {
                    Helper.assertTrue(!SeenZ, Each.Id() + " sorted after the Z-class files");
                    Helper.assertTrue(Key.Number() >= LastNumber, Each.Id() + " is out of numeric order");
                    LastNumber = Key.Number();
                }
            }
            Helper.succeed();
        });
    }

    private static TerminalArchiveIndex.Row row(String Path, String Designation, String Title, DocumentClass Classification, boolean Fresh) {
        return new TerminalArchiveIndex.Row(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path), Designation, Title, Classification,
                Classification.getSerializedName().toUpperCase(), Fresh);
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_archive_filter")
    public static void archiveFilterAndSearch(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            List<TerminalArchiveIndex.Row> Rows = List.of(
                    row("scp_173", "SCP-173", "The Sculpture", DocumentClass.Euclid, false),
                    row("scp_049", "SCP-049", "Plague Doctor", DocumentClass.Euclid, true),
                    row("scp_682", "SCP-682", "Hard-to-Destroy Reptile", DocumentClass.Keter, true),
                    row("scp_999", "SCP-999", "The Tickle Monster", DocumentClass.Safe, false),
                    row("z_0001", "Z-0001", "Example Entry", DocumentClass.ZClass, false));
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, null, false, "").size() == 5, "no filter should keep everything");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, DocumentClass.Euclid, false, "").size() == 2, "class filter");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, null, true, "").size() == 2, "new only");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, DocumentClass.Keter, true, "").size() == 1, "class and new only together");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, null, false, "173").size() == 1, "number search");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, null, false, "scp-049").size() == 1, "designation search");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, null, false, "SCULPT").size() == 1, "case insensitive title search");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, null, false, "the monster").size() == 1, "every word must match");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, null, false, "keter").size() == 1, "class name search");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, null, false, "z0001").size() == 1, "search ignores dashes and underscores");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, DocumentClass.Safe, false, "173").isEmpty(), "search and class filter together");
            Helper.assertTrue(TerminalArchiveIndex.filter(Rows, null, false, "nothing like this").isEmpty(), "a miss still matched");
            Helper.assertTrue(TerminalArchiveIndex.fresh(Rows) == 2, "new count");
            Helper.assertTrue(TerminalArchiveIndex.typable('a', "") && !TerminalArchiveIndex.typable('\n', "") && !TerminalArchiveIndex.typable('a', "x".repeat(TerminalArchiveIndex.MaxQuery)),
                    "search input rules");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "terminal_archive_titles")
    public static void everyDocumentResolvesItsTitle(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Language Words = Language.getInstance();
            List<String> Missing = new ArrayList<>();
            for (Map.Entry<ResourceLocation, AnomalyDocument> Each : TerminalArchive.documents().entrySet()) {
                AnomalyDocument Document = Each.getValue();
                boolean Titled = Words.has(Document.key(DossierContent.TitleField)) || Document.Legacy().Title().isPresent();
                boolean Designated = Words.has(Document.key(DossierContent.DesignationField)) || Document.Legacy().Designation().isPresent();
                if (!Titled || !Designated) {
                    Missing.add(Each.getKey().getPath());
                }
            }
            Helper.assertTrue(Missing.isEmpty(), "documents without a title or designation: " + Missing);
            Helper.succeed();
        });
    }
}
