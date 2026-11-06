package net.rai.scp_unleashed.discord.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.discord.Discord;
import net.rai.scp_unleashed.discord.DiscordApocalypse;
import net.rai.scp_unleashed.discord.DiscordApocalypseWatch;
import net.rai.scp_unleashed.discord.DiscordCategory;
import net.rai.scp_unleashed.discord.DiscordEmbed;
import net.rai.scp_unleashed.discord.DiscordFilters;
import net.rai.scp_unleashed.discord.DiscordScarletWatch;
import net.rai.scp_unleashed.discord.DiscordSettings;
import net.rai.scp_unleashed.discord.DiscordText;
import net.rai.scp_unleashed.discord.DiscordToggle;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.xk.Scp5001Xk;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkConfig;
import net.rai.scp_unleashed.worldend.WorldEndConfig;
import net.rai.scp_unleashed.worldend.WorldEndRules;
import net.rai.scp_unleashed.worldend.WorldEndState;
import net.rai.scp_unleashed.worldend.WorldEnding;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class DiscordApocalypseGameTests {
    public static final String Arena = DiscordGameTests.Arena;
    public static final String P = DiscordApocalypse.Prefix;
    public static final int Patience = 100000;
    public static final long DeadlineMillis = 20000L;
    public static final long SettleMillis = 1500L;

    private DiscordApocalypseGameTests() {
    }

    private static void check(boolean Condition, String Message) {
        if (!Condition) {
            throw new GameTestAssertException(Message);
        }
    }

    private static void within(GameTestHelper Helper, long MinimumMillis, Runnable Check) {
        long Start = System.currentTimeMillis();
        boolean[] Failing = {false};
        Helper.succeedWhen(() -> {
            long Spent = System.currentTimeMillis() - Start;
            if (Spent < MinimumMillis) {
                throw new GameTestAssertException("still settling");
            }
            try {
                Check.run();
            } catch (GameTestAssertException Problem) {
                if (Spent > DeadlineMillis && !Failing[0]) {
                    Failing[0] = true;
                    Helper.runAfterDelay(1, () -> Helper.fail(Problem.getMessage()));
                }
                throw Problem;
            }
        });
    }

    private static void settled(GameTestHelper Helper, Runnable Check) {
        within(Helper, SettleMillis, Check);
    }

    private static void whenDone(GameTestHelper Helper, Runnable Cleanup) {
        Helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo Info) {
            }

            @Override
            public void testPassed(GameTestInfo Info, GameTestRunner Runner) {
                Cleanup.run();
            }

            @Override
            public void testFailed(GameTestInfo Info, GameTestRunner Runner) {
                Cleanup.run();
            }

            @Override
            public void testAddedForRerun(GameTestInfo Old, GameTestInfo Fresh, GameTestRunner Runner) {
            }
        });
    }

    private static FakeDiscord fake(GameTestHelper Helper) {
        Discord.resetForTests();
        DiscordApocalypse.resetForTests();
        FakeDiscord Fake = FakeDiscord.start();
        whenDone(Helper, () -> {
            Discord.resetForTests();
            DiscordApocalypse.resetForTests();
            Fake.close();
        });
        return Fake;
    }

    private static DiscordSettings only(DiscordSettings Base) {
        DiscordSettings Result = Base;
        for (DiscordCategory Category : DiscordCategory.values()) {
            Result = Result.withCategoryEnabled(Category, Category == DiscordCategory.Apocalypse);
        }
        return Result;
    }

    private static MinecraftServer server(GameTestHelper Helper) {
        return Helper.getLevel().getServer();
    }

    private static FakePlayer tester(GameTestHelper Helper) {
        String Name = "Discord_Tester";
        return FakePlayerFactory.get(Helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(Name.getBytes(StandardCharsets.UTF_8)), Name));
    }

    private static String shown(String Key, Object... Args) {
        return DiscordText.escape(DiscordText.translate(P + Key, Args));
    }

    private static String title(FakeDiscord.Hit Entry) {
        JsonObject Embed = Entry.embed();
        return Embed.has("title") ? Embed.get("title").getAsString() : "";
    }

    private static String description(FakeDiscord.Hit Entry) {
        JsonObject Embed = Entry.embed();
        return Embed.has("description") ? Embed.get("description").getAsString() : "";
    }

    private static int colour(FakeDiscord.Hit Entry) {
        return Entry.embed().get("color").getAsInt();
    }

    private static Optional<String> field(FakeDiscord.Hit Entry, String Name) {
        JsonObject Embed = Entry.embed();
        if (!Embed.has("fields")) {
            return Optional.empty();
        }
        for (JsonElement Row : Embed.getAsJsonArray("fields")) {
            JsonObject Cell = Row.getAsJsonObject();
            if (Cell.get("name").getAsString().equals(Name)) {
                return Optional.of(Cell.get("value").getAsString());
            }
        }
        return Optional.empty();
    }

    private static String fieldName(String Key) {
        return DiscordText.translate(P + "field." + Key);
    }

    private static List<String> titles(FakeDiscord Fake, String Hook) {
        List<String> Found = new ArrayList<>();
        for (FakeDiscord.Hit Entry : Fake.hits(Hook)) {
            Found.add(title(Entry));
        }
        return Found;
    }

    private static FakeDiscord.Hit titled(FakeDiscord Fake, String Hook, String Wanted) {
        for (FakeDiscord.Hit Entry : Fake.hits(Hook)) {
            if (title(Entry).equals(Wanted)) {
                return Entry;
            }
        }
        throw new GameTestAssertException("no embed titled " + Wanted + " among " + titles(Fake, Hook));
    }

    private static void common(FakeDiscord.Hit Entry, String Scenario) {
        JsonObject Embed = Entry.embed();
        check(Embed.getAsJsonObject("author").get("name").getAsString().equals(DiscordText.translate(P + "author")), "the O5 author line is missing");
        check(Embed.getAsJsonObject("footer").get("text").getAsString().endsWith(DiscordText.translate(DiscordCategory.Apocalypse.langKey())),
                "the footer does not name the XK category");
        check(field(Entry, fieldName("scenario")).orElse("").equals(DiscordText.escape(Scenario)), "the scenario field is wrong: " + field(Entry, fieldName("scenario")));
        check(field(Entry, fieldName("stage")).isPresent(), "the stage field is missing on " + title(Entry));
        check(Entry.json().getAsJsonObject("allowed_mentions").getAsJsonArray("parse").isEmpty(), "mentions are allowed");
    }

    private static void walk(DiscordApocalypseWatch.Watcher Watch, MinecraftServer Server, BlockPos Site, Object... Steps) {
        int Index = 0;
        while (Index < Steps.length) {
            Scp5001State.Phase Phase = (Scp5001State.Phase) Steps[Index];
            int Days = Steps[Index + 1] instanceof Integer Count ? Count : -1;
            Watch.observe(Server, Phase, Days, 3, Site);
            Index += 2;
        }
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discordxk_chain")
    public static void everyScp5001StagePostsExactlyOnce(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("xk_chain"))));
        MinecraftServer Server = server(Helper);
        BlockPos Site = Helper.absolutePos(new BlockPos(1, 1, 1));
        DiscordApocalypseWatch.Watcher Watch = new DiscordApocalypseWatch.Watcher();
        walk(Watch, Server, Site,
                Scp5001State.Phase.Dormant, -1,
                Scp5001State.Phase.Unlocked, -1,
                Scp5001State.Phase.Rising, -1,
                Scp5001State.Phase.Overdrive, -1,
                Scp5001State.Phase.Purging, -1,
                Scp5001State.Phase.Resetting, -1,
                Scp5001State.Phase.Dormant, -1,
                Scp5001State.Phase.Xk1, -1,
                Scp5001State.Phase.Xk2, 3,
                Scp5001State.Phase.Xk2, 3,
                Scp5001State.Phase.Xk2, 2,
                Scp5001State.Phase.Xk2, 2,
                Scp5001State.Phase.Xk2, 1,
                Scp5001State.Phase.Summoning, 1,
                Scp5001State.Phase.Duel, 1,
                Scp5001State.Phase.Summoning, 1,
                Scp5001State.Phase.Duel, 1,
                Scp5001State.Phase.Saved, -1,
                Scp5001State.Phase.Dormant, -1);
        List<String> Expected = List.of(shown("scp5001.breach.title"), shown("scp5001.overdrive.title"), shown("scp5001.purge.title"),
                shown("scp5001.recontained.title"), shown("scp5001.xk1.title"), shown("scp5001.xk2.title"), shown("countdown.many", 2), shown("countdown.one"),
                shown("scp5001.summoning.title"), shown("scp5001.duel.title"), shown("scp5001.saved.title"));
        settled(Helper, () -> {
            List<String> Seen = titles(Fake, "xk_chain");
            check(Seen.equals(Expected), "expected the stages " + Expected + " but got " + Seen);
            for (FakeDiscord.Hit Entry : Fake.hits("xk_chain")) {
                common(Entry, "SCP-5001");
                check(field(Entry, "Position").orElse("").equals(DiscordEmbed.code(Site)), "the site position is missing on " + title(Entry));
            }
            FakeDiscord.Hit Breach = titled(Fake, "xk_chain", shown("scp5001.breach.title"));
            check(colour(Breach) == DiscordCategory.Apocalypse.getDefaultColour(), "alerts should use the category colour");
            check(field(Breach, fieldName("stage")).orElse("").equals(shown("stage.breach")), "the breach stage label is wrong");
            check(colour(titled(Fake, "xk_chain", shown("scp5001.recontained.title"))) == DiscordApocalypse.Relief, "recontainment should use the relief colour");
            FakeDiscord.Hit Second = titled(Fake, "xk_chain", shown("scp5001.xk2.title"));
            check(field(Second, fieldName("remaining")).orElse("").equals(shown("days.many", 3)), "XK-2 should say 3 days remain: " + field(Second, fieldName("remaining")));
            FakeDiscord.Hit Two = titled(Fake, "xk_chain", shown("countdown.many", 2));
            check(field(Two, fieldName("remaining")).orElse("").equals(shown("days.many", 2)), "the countdown field is wrong");
            check(description(Two).contains("2") && description(Two).contains("3"), "the countdown does not say which day it is: " + description(Two));
            FakeDiscord.Hit One = titled(Fake, "xk_chain", shown("countdown.one"));
            check(field(One, fieldName("remaining")).orElse("").equals(shown("days.one")), "the last day field is wrong");
            for (FakeDiscord.Hit Entry : Fake.hits("xk_chain")) {
                String Body = description(Entry).toLowerCase();
                check(!Body.contains("console") && !Body.contains("mekhane") && !Body.contains("999") && !Body.contains("overdrive") && !Body.contains("protocol"),
                        "an alert gives away how to stop the scenario: " + description(Entry));
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discordxk_cancel")
    public static void operatorCancelIsNotedAndTheEndingStaysSilent(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("xk_cancel"))));
        MinecraftServer Server = server(Helper);
        BlockPos Site = Helper.absolutePos(new BlockPos(1, 1, 1));
        DiscordApocalypseWatch.Watcher Watch = new DiscordApocalypseWatch.Watcher();
        walk(Watch, Server, Site,
                Scp5001State.Phase.Xk2, 1,
                Scp5001State.Phase.Xk2, 1,
                Scp5001State.Phase.Ended, -1,
                Scp5001State.Phase.Dormant, -1);
        settled(Helper, () -> {
            List<FakeDiscord.Hit> Posts = Fake.hits("xk_cancel");
            check(Posts.size() == 1, "the watcher should only post the cancel, got " + titles(Fake, "xk_cancel"));
            FakeDiscord.Hit Entry = Posts.get(0);
            common(Entry, "SCP-5001");
            check(title(Entry).equals(shown("cancelled.title")), "the cancel title is wrong: " + title(Entry));
            check(field(Entry, fieldName("cancelled_at")).orElse("").equals(shown("stage.ending")), "the cancel does not say when it happened");
            check(description(Entry).contains("operator"), "the cancel note does not mention the operator");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discordxk_repeat")
    public static void repeatedStagesPostOnce(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("xk_repeat"))));
        MinecraftServer Server = server(Helper);
        BlockPos Site = Helper.absolutePos(new BlockPos(1, 1, 1));
        check(DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Overdrive, Site), "the first overdrive was not posted");
        check(!DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Overdrive, Site), "a repeat overdrive was posted");
        DiscordApocalypseWatch.Watcher Watch = new DiscordApocalypseWatch.Watcher();
        walk(Watch, Server, Site,
                Scp5001State.Phase.Rising, -1,
                Scp5001State.Phase.Overdrive, -1,
                Scp5001State.Phase.Rising, -1,
                Scp5001State.Phase.Overdrive, -1);
        Component Detail = Component.literal("probe");
        check(DiscordApocalypse.scenario(Server, "discordxk_probe", "same", Component.literal("PROBE ONE"), Detail, null, -1), "the first probe was not posted");
        check(!DiscordApocalypse.scenario(Server, "discordxk_probe", "same", Component.literal("PROBE ONE AGAIN"), Detail, null, -1), "the repeat probe was posted");
        check(DiscordApocalypse.scenario(Server, "discordxk_probe", "other", Component.literal("PROBE TWO"), Detail, null, 0x123456), "another stage was held back");
        settled(Helper, () -> {
            List<String> Seen = titles(Fake, "xk_repeat");
            check(Seen.equals(List.of(shown("scp5001.overdrive.title"), "PROBE ONE", "PROBE TWO")), "expected one post per stage, got " + Seen);
            check(colour(titled(Fake, "xk_repeat", "PROBE TWO")) == 0x123456, "the caller's colour was ignored");
            check(field(titled(Fake, "xk_repeat", "PROBE ONE"), "Position").isEmpty(), "a probe without a place has a position");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discordxk_disabled")
    public static void nothingPostsWhileTheCategoryIsOff(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        MinecraftServer Server = server(Helper);
        BlockPos Site = Helper.absolutePos(new BlockPos(1, 1, 1));
        FakePlayer User = tester(Helper);
        List<DiscordSettings> Silent = List.of(DiscordSettings.testing(Fake.url("xk_off")).withCategoryEnabled(DiscordCategory.Apocalypse, false),
                only(DiscordSettings.testing(Fake.url("xk_off"))).withEnabled(false));
        for (DiscordSettings Settings : Silent) {
            Discord.useForTests(Settings);
            DiscordApocalypseWatch.Watcher Watch = new DiscordApocalypseWatch.Watcher();
            walk(Watch, Server, Site,
                    Scp5001State.Phase.Dormant, -1,
                    Scp5001State.Phase.Unlocked, -1,
                    Scp5001State.Phase.Overdrive, -1,
                    Scp5001State.Phase.Purging, -1,
                    Scp5001State.Phase.Resetting, -1,
                    Scp5001State.Phase.Dormant, -1,
                    Scp5001State.Phase.Xk1, -1,
                    Scp5001State.Phase.Xk2, 3,
                    Scp5001State.Phase.Xk2, 2,
                    Scp5001State.Phase.Summoning, 2,
                    Scp5001State.Phase.Duel, 2,
                    Scp5001State.Phase.Saved, -1,
                    Scp5001State.Phase.Dormant, -1);
            check(!DiscordApocalypse.scenario(Server, "discordxk_probe", "off", Component.literal("OFF"), Component.literal("off"), Site, -1), "a scenario posted while off");
            check(!DiscordApocalypse.scarletManifestation(Server, 0.2f, 0.9f, Site), "manifestation posted while off");
            check(!DiscordApocalypse.scarletChain(Server, 2, Site), "a chain posted while off");
            check(!DiscordApocalypse.scarletRitual(Server, Site, Level.OVERWORLD), "a ritual posted while off");
            check(!DiscordApocalypse.scarletXk2(Server, 3, Site), "the Scarlet XK-2 posted while off");
            check(!DiscordApocalypse.scarletRite(Server, User, Site), "the rite posted while off");
            check(!DiscordApocalypse.scarletSlain(Server, User, Site), "the slaying posted while off");
            check(!DiscordApocalypse.scarletEnding(Server, Site), "the Scarlet ending posted while off");
            DiscordApocalypse.worldEnding(Server, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "discordxk_probe"), Component.literal("off"), null);
            check(!DiscordApocalypse.lastWords(Server, "scp_unleashed:scp5001", "Off World"), "the final notice claims delivery while off");
        }
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("xk_off")))
                .withFilters(DiscordFilters.standard().withToggle(DiscordToggle.ScarletRituals, false)));
        check(!DiscordApocalypse.scarletRitual(Server, Site, Level.OVERWORLD), "a muted ritual was posted");
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("xk_off"))));
        check(DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Breach, Site), "a stage seen while off blocked the first real post");
        settled(Helper, () -> {
            List<String> Seen = titles(Fake, "xk_off");
            check(Seen.equals(List.of(shown("scp5001.breach.title"))), "only the post made after enabling should arrive, got " + Seen);
            check(Discord.current() != null, "the relay never started");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discordxk_scarlet")
    public static void scarletKingCallsProduceEmbeds(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("xk_scarlet"))));
        MinecraftServer Server = server(Helper);
        BlockPos Throne = Helper.absolutePos(new BlockPos(2, 1, 2));
        FakePlayer User = tester(Helper);
        check(DiscordApocalypse.scarletManifestation(Server, 0.20f, 0.30f, Throne), "crossing 25% was not posted");
        check(!DiscordApocalypse.scarletManifestation(Server, 0.30f, 0.31f, Throne), "a step without a threshold was posted");
        check(DiscordApocalypse.scarletManifestation(Server, 0.31f, 0.80f, Throne), "the jump past 75% was not posted");
        check(!DiscordApocalypse.scarletManifestation(Server, 0.80f, 0.60f, Throne), "a falling meter was posted");
        check(DiscordApocalypse.scarletChain(Server, 3, Throne), "the third chain was not posted");
        check(!DiscordApocalypse.scarletChain(Server, 3, Throne), "the third chain was posted twice");
        check(DiscordApocalypse.scarletChain(Server, 1, Throne), "the first chain was not posted");
        check(DiscordApocalypse.scarletRitual(Server, Throne, Level.OVERWORLD), "the ritual was not posted");
        check(!DiscordApocalypse.scarletRitual(Server, Throne, Level.OVERWORLD), "a second ritual right away was posted");
        check(DiscordApocalypse.scarletXk2(Server, 3, Throne), "the Scarlet XK-2 was not posted");
        check(DiscordApocalypse.scarletCountdown(Server, 2, 3, Throne), "the Scarlet countdown was not posted");
        check(DiscordApocalypse.scarletRite(Server, User, Throne), "the rite was not posted");
        check(DiscordApocalypse.scarletSlain(Server, User, Throne), "the slaying was not posted");
        check(DiscordApocalypse.scarletEscaped(Server, Throne), "the escape was not posted");
        DiscordApocalypse.worldEnding(Server, DiscordApocalypse.ScarletSource, Component.literal("The King has come."), Vec3.atCenterOf(Throne));
        check(!DiscordApocalypse.scarletEnding(Server, Throne), "the Scarlet ending was posted by both paths");
        String Scenario = DiscordText.translate(P + "scenario." + DiscordApocalypse.Scarlet);
        List<String> Expected = List.of(shown("scp001_scarlet.manifestation.title", 25), shown("scp001_scarlet.manifestation.title", 75),
                shown("scp001_scarlet.chain.title", DiscordApocalypse.ordinal(3).toUpperCase(Locale.ROOT)),
                shown("scp001_scarlet.chain.title", DiscordApocalypse.ordinal(1).toUpperCase(Locale.ROOT)),
                shown("scp001_scarlet.ritual.title"), shown("scp001_scarlet.xk2.title"), shown("countdown.many", 2), shown("scp001_scarlet.rite.title"),
                shown("scp001_scarlet.slain.title"), shown("scp001_scarlet.escaped.title"), shown("ending.title"));
        settled(Helper, () -> {
            List<String> Seen = titles(Fake, "xk_scarlet");
            check(Seen.equals(Expected), "expected " + Expected + " but got " + Seen);
            for (FakeDiscord.Hit Entry : Fake.hits("xk_scarlet")) {
                common(Entry, Scenario);
            }
            check(DiscordApocalypse.ordinal(3).equals("3rd") && DiscordApocalypse.ordinal(12).equals("12th") && DiscordApocalypse.ordinal(21).equals("21st"),
                    "ordinals are wrong");
            check(title(Fake.hits("xk_scarlet").get(2)).contains("3RD"), "the chain title does not say the 3rd chain");
            check(field(Fake.hits("xk_scarlet").get(2), fieldName("chains")).orElse("").equals(shown("chains.count", 3, 7)), "the chain count is wrong");
            check(field(Fake.hits("xk_scarlet").get(1), fieldName("manifestation")).orElse("").equals("80%"), "the manifestation reading is wrong");
            FakeDiscord.Hit Rite = titled(Fake, "xk_scarlet", shown("scp001_scarlet.rite.title"));
            check(field(Rite, fieldName("leader")).orElse("").equals("Discord\\_Tester"), "the rite leader is missing");
            FakeDiscord.Hit Slain = titled(Fake, "xk_scarlet", shown("scp001_scarlet.slain.title"));
            check(field(Slain, "Slain by").orElse("").equals("Discord\\_Tester"), "the slayer is missing");
            check(colour(Slain) == DiscordApocalypse.Relief, "the slaying should use the relief colour");
            FakeDiscord.Hit End = titled(Fake, "xk_scarlet", shown("ending.title"));
            check(field(End, fieldName("source")).orElse("").equals(DiscordText.escape(DiscordApocalypse.ScarletSource.toString())), "the ending source is wrong");
            check(description(End).startsWith("The King has come."), "the ending reason is missing: " + description(End));
            check(description(End).contains(DiscordText.escape(DiscordText.translate(P + "scp001_scarlet.ending.detail"))), "the Scarlet ending text is missing");
            check(colour(End) == DiscordApocalypse.Doom, "the ending should use the doom colour");
            for (FakeDiscord.Hit Entry : Fake.hits("xk_scarlet")) {
                String Body = description(Entry).toLowerCase();
                check(!Body.contains("999") && !Body.contains("seven thrones") && !Body.contains("chicken"), "a Scarlet alert gives a secret away: " + description(Entry));
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discordxk_scarletwatch")
    public static void scarletStateChangesAreWatched(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("xk_scarletwatch"))));
        MinecraftServer Server = server(Helper);
        BlockPos Throne = Helper.absolutePos(new BlockPos(2, 1, 2));
        check(DiscordScarletWatch.listening(), "the chain listener was never registered");
        DiscordScarletWatch.Watcher Watch = new DiscordScarletWatch.Watcher();
        Watch.observe(Server, ScarletState.Phase.Stirring, 0.20f, -1, 3, null);
        Watch.observe(Server, ScarletState.Phase.Stirring, 0.26f, -1, 3, null);
        Watch.observe(Server, ScarletState.Phase.Stirring, 0.24f, -1, 3, null);
        Watch.observe(Server, ScarletState.Phase.Stirring, 0.27f, -1, 3, null);
        Watch.observe(Server, ScarletState.Phase.Xk2, 1.0f, 3, 3, Throne);
        Watch.observe(Server, ScarletState.Phase.Xk2, 1.0f, 3, 3, Throne);
        Watch.observe(Server, ScarletState.Phase.Xk2, 1.0f, 2, 3, Throne);
        Watch.observe(Server, ScarletState.Phase.Rite, 1.0f, 2, 3, Throne);
        Watch.observe(Server, ScarletState.Phase.Fight, 1.0f, 2, 3, Throne);
        Watch.observe(Server, ScarletState.Phase.Freed, 0.0f, -1, 3, Throne);
        Watch.observe(Server, ScarletState.Phase.Asleep, 0.0f, -1, 3, null);
        DiscordScarletWatch.snapped(Server, 4);
        DiscordScarletWatch.Watcher Cancel = new DiscordScarletWatch.Watcher();
        Cancel.observe(Server, ScarletState.Phase.Xk2, 1.0f, 2, 3, Throne);
        Cancel.observe(Server, ScarletState.Phase.Rite, 1.0f, 2, 3, Throne);
        Cancel.observe(Server, ScarletState.Phase.Fight, 1.0f, 2, 3, Throne);
        Cancel.observe(Server, ScarletState.Phase.Xk2, 1.0f, 2, 3, Throne);
        Cancel.observe(Server, ScarletState.Phase.Asleep, 0.0f, -1, 3, null);
        List<String> Expected = List.of(shown("scp001_scarlet.manifestation.title", 25), shown("scp001_scarlet.xk2.title"), shown("countdown.many", 2),
                shown("scp001_scarlet.rite.title"), shown("scp001_scarlet.slain.title"), shown("scp001_scarlet.chain.title", "4TH"), shown("scp001_scarlet.escaped.title"),
                shown("cancelled.title"));
        settled(Helper, () -> {
            List<String> Seen = titles(Fake, "xk_scarletwatch");
            check(Seen.equals(Expected), "expected " + Expected + " but got " + Seen);
            String Scenario = DiscordText.translate(P + "scenario." + DiscordApocalypse.Scarlet);
            for (FakeDiscord.Hit Entry : Fake.hits("xk_scarletwatch")) {
                common(Entry, Scenario);
            }
            FakeDiscord.Hit Second = titled(Fake, "xk_scarletwatch", shown("scp001_scarlet.xk2.title"));
            check(field(Second, fieldName("remaining")).orElse("").equals(shown("days.many", 3)), "the Scarlet XK-2 should say 3 days remain");
            check(field(Second, "Position").orElse("").equals(DiscordEmbed.code(Throne)), "the throne position is missing");
            check(field(titled(Fake, "xk_scarletwatch", shown("cancelled.title")), fieldName("cancelled_at")).orElse("").equals(shown("stage.xk2")),
                    "the Scarlet cancel does not say when it happened");
            check(field(titled(Fake, "xk_scarletwatch", shown("scp001_scarlet.manifestation.title", 25)), "Position").isEmpty(),
                    "manifestation readings must not give away a place");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discordxk_ending")
    public static void anyWorldEndingIsAnnouncedOnce(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("xk_ending"))));
        MinecraftServer Server = server(Helper);
        ResourceLocation Source = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "discordxk_probe");
        Vec3 Focus = Vec3.atCenterOf(Helper.absolutePos(new BlockPos(3, 2, 3)));
        DiscordApocalypse.worldEnding(Server, Source, Component.literal("The probe has ended the world."), Focus);
        DiscordApocalypse.worldEnding(Server, Source, Component.literal("The probe has ended the world."), Focus);
        settled(Helper, () -> {
            List<FakeDiscord.Hit> Posts = Fake.hits("xk_ending");
            check(Posts.size() == 1, "one world ending should post once, got " + titles(Fake, "xk_ending"));
            FakeDiscord.Hit End = Posts.get(0);
            common(End, Source.toString());
            check(title(End).equals(shown("ending.title")), "the ending title is wrong: " + title(End));
            check(field(End, fieldName("source")).orElse("").equals(DiscordText.escape(Source.toString())), "the source id is missing");
            check(field(End, fieldName("deletion")).isPresent(), "the deletion state is missing");
            check(description(End).startsWith("The probe has ended the world.") && description(End).contains(DiscordText.escape(DiscordText.translate(P + "ending.detail"))),
                    "the generic ending text is wrong: " + description(End));
            check(field(End, "Position").orElse("").equals(DiscordEmbed.code(BlockPos.containing(Focus))), "the focus position is missing");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "discordxk_final")
    public static void finalNoticeIsFlushedBeforeTheDeletion(GameTestHelper Helper) {
        MinecraftServer Server = server(Helper);
        FakeDiscord Fake = fake(Helper);
        Scp5001Xk.cancel(Server);
        DiscordApocalypseWatch.live().forget();
        Scp5001XkConfig.overrideForTests(0.0, 1000000, 100000L, 0);
        WorldEnding.clearDryRun();
        WorldEndRules.setEndings(Server, true);
        WorldEndConfig.overrideForTests(1);
        whenDone(Helper, () -> {
            Discord.useForTests(DiscordSettings.disabled());
            Scp5001Xk.cancel(Server);
            DiscordApocalypseWatch.live().forget();
            Discord.useForTests(null);
            Scp5001XkConfig.clearOverrides();
            WorldEndConfig.overrideForTests(-1);
            WorldEnding.clearDryRun();
        });
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("xk_final"))));
        check(WorldEnding.dryRun(Server), "the GameTest server must be a dry run");
        Scp5001State.get(Server).phase(Scp5001State.Phase.Ended, Server.overworld().getGameTime());
        Helper.runAfterDelay(10, () -> check(WorldEnding.stage(Server) == WorldEndState.Stage.Freezing, "the ending did not start, stage " + WorldEnding.stage(Server)));
        Helper.runAfterDelay(WorldEnding.FreezeTicks + 40, () -> {
            List<String> Records = WorldEnding.dryRunRecords();
            int Flushed = Records.indexOf("discord final notice flushed");
            int Deleted = -1;
            for (int Index = 0; Index < Records.size(); Index++) {
                if (Records.get(Index).startsWith("delete ")) {
                    Deleted = Index;
                    break;
                }
            }
            check(Deleted >= 0, "the dry run did not record the deletion: " + Records);
            check(Flushed >= 0 && Flushed < Deleted, "the final notice was not flushed before the deletion: " + Records);
            List<String> Seen = titles(Fake, "xk_final");
            check(Seen.equals(List.of(shown("ending.title"), shown("final.title"))), "expected the ending and the final notice, got " + Seen);
            FakeDiscord.Hit End = Fake.hits("xk_final").get(0);
            common(End, "SCP-5001");
            check(field(End, fieldName("source")).orElse("").equals(DiscordText.escape(DiscordApocalypse.Scp5001Source.toString())), "the SCP-5001 source is missing");
            check(field(End, fieldName("deletion")).orElse("").equals(shown("deletion.dry_run")), "the deletion state is wrong: " + field(End, fieldName("deletion")));
            check(description(End).contains(DiscordText.escape(DiscordText.translate("message.scp_unleashed.scp5001_xk.ending"))), "the ending reason is missing");
            FakeDiscord.Hit Last = Fake.hits("xk_final").get(1);
            common(Last, "SCP-5001");
            check(field(Last, fieldName("world")).isPresent(), "the final notice does not name the world");
            check(Last.AtMillis() >= End.AtMillis(), "the final notice arrived before the ending");
            Helper.succeed();
        });
    }
}
