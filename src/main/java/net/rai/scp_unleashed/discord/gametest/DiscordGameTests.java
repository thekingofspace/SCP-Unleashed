package net.rai.scp_unleashed.discord.gametest;

import net.rai.scp_unleashed.config.ScpConfigs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.discord.Discord;
import net.rai.scp_unleashed.discord.DiscordCategory;
import net.rai.scp_unleashed.discord.DiscordCommands;
import net.rai.scp_unleashed.discord.DiscordConfig;
import net.rai.scp_unleashed.discord.DiscordContainment;
import net.rai.scp_unleashed.discord.DiscordCoordinates;
import net.rai.scp_unleashed.discord.DiscordDeaths;
import net.rai.scp_unleashed.discord.DiscordEmbed;
import net.rai.scp_unleashed.discord.DiscordFilters;
import net.rai.scp_unleashed.discord.DiscordNotices;
import net.rai.scp_unleashed.discord.DiscordRaids;
import net.rai.scp_unleashed.discord.DiscordReadLog;
import net.rai.scp_unleashed.discord.DiscordReads;
import net.rai.scp_unleashed.discord.DiscordRelay;
import net.rai.scp_unleashed.discord.DiscordSettings;
import net.rai.scp_unleashed.discord.DiscordText;
import net.rai.scp_unleashed.discord.DiscordToggle;
import net.rai.scp_unleashed.discord.DiscordUrls;
import net.rai.scp_unleashed.discord.DiscordWarhead;
import net.rai.scp_unleashed.discord.DiscordWebhooks;
import net.rai.scp_unleashed.mtf.MtfTier;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.base.RaidBases;
import net.rai.scp_unleashed.raid.chaos.ChaosFaction;
import net.rai.scp_unleashed.scp106.Scp106Registry;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerVariants;
import net.rai.scp_unleashed.warhead.WarheadSequence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class DiscordGameTests {
    public static final String Arena = "scp420j_arena";
    public static final String TesterName = "Discord_Tester";
    public static final String Secret = "TopSecretToken_abc123XYZ";
    public static final ResourceLocation Scp173 = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_173");
    public static final int Patience = 100000;
    public static final long DeadlineMillis = 20000L;
    public static final long SettleMillis = 1500L;

    private DiscordGameTests() {
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

    private static void eventually(GameTestHelper Helper, Runnable Check) {
        within(Helper, 0L, Check);
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
        FakeDiscord Fake = FakeDiscord.start();
        whenDone(Helper, () -> {
            Discord.resetForTests();
            Fake.close();
        });
        return Fake;
    }

    private static DiscordRelay relay(GameTestHelper Helper, int Limit, long Backoff) {
        DiscordRelay Made = new DiscordRelay(Limit, 600, Backoff);
        whenDone(Helper, () -> Made.stop(Duration.ZERO));
        return Made;
    }

    private static DiscordSettings only(DiscordSettings Base, DiscordCategory... Kept) {
        DiscordSettings Result = Base;
        for (DiscordCategory Category : DiscordCategory.values()) {
            boolean Wanted = List.of(Kept).contains(Category);
            Result = Result.withCategoryEnabled(Category, Wanted);
        }
        return Result;
    }

    private static FakePlayer tester(GameTestHelper Helper) {
        return reader(Helper, TesterName);
    }

    private static FakePlayer reader(GameTestHelper Helper, String Name) {
        return FakePlayerFactory.get(Helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(Name.getBytes(StandardCharsets.UTF_8)), Name));
    }

    private static ItemStack opened(ResourceLocation Id) {
        ItemStack Copy = new ItemStack(ArchiveRegistry.OpenedDocument.get());
        Copy.set(ArchiveRegistry.StampComponent.get(), new DocumentStamp(Id, DocumentClass.Euclid));
        return Copy;
    }

    private static DiscordSettings filtered(FakeDiscord Fake, String Hook, DiscordCategory Category, DiscordFilters Rules) {
        return only(DiscordSettings.testing(Fake.url(Hook)), Category).withFilters(Rules);
    }

    private static void forgetReads(MinecraftServer Server, List<FakePlayer> Readers) {
        DiscordReadLog Seen = DiscordReadLog.get(Server);
        for (FakePlayer Reader : Readers) {
            Seen.forget(Reader.getUUID());
        }
    }

    private static long reads(FakeDiscord Fake, String Hook, String Reader, String Designation) {
        return Fake.titled(Hook, "is reading a file").stream()
                .filter(Entry -> field(Entry, "Reader").orElse("").equals(Reader) && field(Entry, "Document").orElse("").equals(Designation)).count();
    }

    private static String title(FakeDiscord.Hit Entry) {
        JsonObject Embed = Entry.embed();
        return Embed.has("title") ? Embed.get("title").getAsString() : "";
    }

    private static String description(FakeDiscord.Hit Entry) {
        JsonObject Embed = Entry.embed();
        return Embed.has("description") ? Embed.get("description").getAsString() : "";
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

    private static void checkPayload(FakeDiscord.Hit Entry) {
        JsonObject Root = Entry.json();
        check(Root.has("allowed_mentions"), "payload has no allowed_mentions");
        JsonArray Parse = Root.getAsJsonObject("allowed_mentions").getAsJsonArray("parse");
        check(Parse != null && Parse.isEmpty(), "allowed_mentions.parse is not empty: " + Parse);
        check(!Root.has("content"), "payload carries plain content");
        check(Root.getAsJsonArray("embeds").size() == 1, "payload should carry exactly one embed");
        JsonObject Embed = Entry.embed();
        check(Embed.has("timestamp") && Embed.has("color"), "embed misses its timestamp or colour");
        check(Embed.getAsJsonObject("footer").get("text").getAsString().startsWith("Test Site"), "footer misses the server name");
        check("application/json".equals(Entry.ContentType()), "wrong content type " + Entry.ContentType());
        check(Entry.UserAgent().startsWith("SCP-Unleashed-Discord"), "wrong user agent " + Entry.UserAgent());
    }

    private static int total(JsonObject Embed) {
        int Sum = Embed.has("title") ? Embed.get("title").getAsString().length() : 0;
        Sum += Embed.has("description") ? Embed.get("description").getAsString().length() : 0;
        Sum += Embed.has("footer") ? Embed.getAsJsonObject("footer").get("text").getAsString().length() : 0;
        Sum += Embed.has("author") ? Embed.getAsJsonObject("author").get("name").getAsString().length() : 0;
        if (Embed.has("fields")) {
            for (JsonElement Row : Embed.getAsJsonArray("fields")) {
                Sum += Row.getAsJsonObject().get("name").getAsString().length() + Row.getAsJsonObject().get("value").getAsString().length();
            }
        }
        return Sum;
    }

    private static String faceOf(FakePlayer User) {
        return "https://mc-heads.net/avatar/" + User.getUUID() + "/64";
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_players")
    public static void joinAndLeaveEmbedsShowThePlayersFace(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("players_hook")), DiscordCategory.Players));
        FakePlayer User = tester(Helper);
        DiscordNotices.playerJoined(User, true);
        DiscordNotices.playerLeft(User);
        eventually(Helper, () -> {
            List<FakeDiscord.Hit> Joined = Fake.titled("players_hook", "joined for the first time");
            List<FakeDiscord.Hit> Left = Fake.titled("players_hook", "left the server");
            check(Joined.size() == 1 && Left.size() == 1, "expected one join and one leave embed, got " + Fake.hits().size() + " posts");
            for (FakeDiscord.Hit Entry : List.of(Joined.get(0), Left.get(0))) {
                checkPayload(Entry);
                check(title(Entry).contains("Discord\\_Tester"), "player name is not escaped in the title: " + title(Entry));
                check(Entry.embed().getAsJsonObject("thumbnail").get("url").getAsString().equals(faceOf(User)), "thumbnail is not the player's face");
                check(field(Entry, "Player").orElse("").equals("Discord\\_Tester"), "player field is missing");
            }
            check(field(Joined.get(0), "First join").orElse("").equals("Yes"), "first join flag is missing");
            check(field(Joined.get(0), "Online").isPresent(), "online count is missing");
            check(field(Joined.get(0), "Position").isPresent(), "players follow the global coordinates setting");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_deaths")
    public static void anomalyDeathsNameTheAnomaly(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("deaths_hook")), DiscordCategory.Players).withDeaths(DiscordDeaths.Anomalies));
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        check(DiscordNotices.anomalyToken("scp173_snap").equals(Optional.of(Scp173)), "scp173_snap does not map to SCP-173");
        check(DiscordNotices.anomalyToken("scp420j_smoke").map(ResourceLocation::getPath).equals(Optional.of("scp_420_j")), "420-J mapping is wrong");
        check(DiscordNotices.anomalyToken("z0002_slash").map(ResourceLocation::getPath).equals(Optional.of("z_0002")), "Z-0002 mapping is wrong");
        check(DiscordNotices.anomalyToken("gunshot").isEmpty(), "gunshots are not anomalies");
        DiscordNotices.playerDied(User, World.damageSources().generic());
        DamageSource Snap = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp173Registry.Snap));
        DiscordNotices.playerDied(User, Snap);
        settled(Helper, () -> {
            List<FakeDiscord.Hit> Posts = Fake.hits("deaths_hook");
            check(Posts.size() == 1, "only the anomaly death should be posted, got " + Posts.size());
            checkPayload(Posts.get(0));
            check(title(Posts.get(0)).contains("died"), "death title is wrong: " + title(Posts.get(0)));
            check(field(Posts.get(0), "Anomaly").orElse("").contains("SCP-173"), "anomaly field does not name SCP-173");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_documents")
    public static void documentReadsNameTheFileAndTheReader(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("documents_hook")), DiscordCategory.Documents));
        FakePlayer User = tester(Helper);
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp173);
        check(Document != null, "SCP-173 has no document");
        DiscordNotices.documentOpened(User, Scp173, Document, true);
        ItemStack Copy = new ItemStack(ArchiveRegistry.OpenedDocument.get());
        Copy.set(ArchiveRegistry.StampComponent.get(), new DocumentStamp(Scp173, DocumentClass.Euclid));
        DiscordNotices.documentRead(User, Copy);
        DiscordNotices.documentRead(User, Copy);
        settled(Helper, () -> {
            List<FakeDiscord.Hit> Opened = Fake.titled("documents_hook", "opened a sealed file");
            List<FakeDiscord.Hit> Reading = Fake.titled("documents_hook", "is reading a file");
            check(Opened.size() == 1, "expected one sealed file embed, got " + Opened.size());
            check(Reading.size() == 1, "re-reading the same file should only post once, got " + Reading.size());
            for (FakeDiscord.Hit Entry : List.of(Opened.get(0), Reading.get(0))) {
                checkPayload(Entry);
                check(field(Entry, "Document").orElse("").equals("SCP-173"), "document field is wrong: " + field(Entry, "Document"));
                check(field(Entry, "Reader").orElse("").equals("Discord\\_Tester"), "reader field is wrong");
                check(description(Entry).contains("SCP-173: "), "description does not name the file");
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_anomalies")
    public static void anomalyArrivalsAndBreachesAreReported(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("anomalies_hook")), DiscordCategory.Anomalies));
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Spot = Helper.absolutePos(new BlockPos(1, 1, 1));
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp173);
        DiscordNotices.anomalyArrived(new AnomalySpawners.Discovery(World, Spot, User, Scp173, Document, true));
        DiscordNotices.containment(World, Spot, Scp106Registry.DocumentId, true);
        DiscordNotices.rift(World, Spot, true);
        eventually(Helper, () -> {
            List<FakeDiscord.Hit> Arrived = Fake.titled("anomalies_hook", "has manifested");
            List<FakeDiscord.Hit> Breached = Fake.titled("anomalies_hook", "breached containment");
            List<FakeDiscord.Hit> Rift = Fake.titled("anomalies_hook", "Apocalyptic Rift");
            check(Arrived.size() == 1 && Breached.size() == 1 && Rift.size() == 1, "missing anomaly embeds, got " + Fake.hits().size());
            checkPayload(Arrived.get(0));
            check(title(Arrived.get(0)).contains("SCP-173"), "arrival does not name the anomaly");
            check(field(Arrived.get(0), "Investigator").orElse("").equals("Discord\\_Tester"), "investigator is missing");
            check(field(Arrived.get(0), "Position").orElse("").equals(DiscordEmbed.code(Spot)), "arrival position is wrong");
            check(field(Arrived.get(0), "Dimension").isPresent(), "arrival dimension is missing");
            check(title(Breached.get(0)).contains("SCP-106"), "breach does not name SCP-106");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_raid")
    public static void raidStartGoesThroughTheRaidNotice(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("raid_hook")), DiscordCategory.Security));
        ServerLevel World = Helper.getLevel();
        BlockPos Target = Helper.absolutePos(new BlockPos(2, 1, 2));
        long Now = World.getGameTime();
        Raid Current = new Raid(9001, ChaosFaction.Id, 0, World.dimension(), RaidBases.TeamPrefix + "Blue", Target, 0.0, 8.0, 1.0, 0.0, true, Now, Target);
        Current.setStage(RaidStage.Active, Now);
        RaidContext Context = RaidDirector.context(World.getServer(), Current, Now);
        check(Context != null, "no raid context for the Chaos Insurgency");
        Context.notice("start");
        eventually(Helper, () -> {
            List<FakeDiscord.Hit> Started = Fake.titled("raid_hook", "Raid started");
            check(Started.size() == 1, "expected one raid start embed, got " + Fake.hits().size());
            FakeDiscord.Hit Entry = Started.get(0);
            checkPayload(Entry);
            check(title(Entry).contains("Chaos Insurgency"), "raid title misses the faction: " + title(Entry));
            check(field(Entry, "Raid").orElse("").equals("\\#9001"), "raid id is wrong: " + field(Entry, "Raid"));
            check(field(Entry, "Team").orElse("").equals("Blue"), "raid team is wrong");
            check(field(Entry, "Started by command").isPresent(), "forced raids are not flagged");
            check(!description(Entry).isEmpty(), "raid notice text is missing");
            check(field(Entry, "Position").orElse("").equals(DiscordEmbed.code(Target)), "raid position is wrong");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_security")
    public static void securityEventsAllArrive(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("security_hook")), DiscordCategory.Security));
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Panel = Helper.absolutePos(new BlockPos(1, 1, 1));
        long Now = World.getGameTime();
        DiscordNotices.mtfCalled(User, MtfTier.Volatile, Panel);
        DiscordNotices.warheadLever(World, Panel, User, true, 2);
        DiscordNotices.warheadEngaged(World, Panel, User, 2, Now + 1800);
        DiscordNotices.warheadCancelled(World, Panel, User);
        DiscordNotices.warheadEnded(World, new WarheadSequence(Panel, Now, Now + 1800, List.of(Panel)), true);
        DiscordNotices.meltdown(World, Panel, false);
        eventually(Helper, () -> {
            check(Fake.hits("security_hook").size() == 6, "expected six security embeds, got " + Fake.hits().size());
            for (String Fragment : List.of("called in", "Warhead armed", "countdown started", "countdown cancelled", "Warhead detonated", "meltdown")) {
                check(Fake.titled("security_hook", Fragment).size() == 1, "missing security embed: " + Fragment);
            }
            FakeDiscord.Hit Engaged = Fake.titled("security_hook", "countdown started").get(0);
            check(field(Engaged, "Countdown").orElse("").startsWith("90"), "countdown is wrong: " + field(Engaged, "Countdown"));
            check(Engaged.embed().getAsJsonObject("author").get("name").getAsString().equals(TesterName), "operator is not the author");
            Fake.hits("security_hook").forEach(DiscordGameTests::checkPayload);
        });
    }

    private static void chatTest(GameTestHelper Helper, String Hook, boolean Coordinates) {
        FakeDiscord Fake = fake(Helper);
        DiscordSettings Settings = only(DiscordSettings.testing(Fake.url(Hook)), DiscordCategory.Computers);
        if (Coordinates) {
            Settings = Settings.withCategoryCoordinates(DiscordCategory.Computers, DiscordCoordinates.On);
        }
        Discord.useForTests(Settings);
        check(Settings.coordinatesFor(DiscordCategory.Computers) == Coordinates, "computer chat coordinates should default to off");
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Computer = Helper.absolutePos(new BlockPos(1, 1, 1));
        ChatMessage Draft = new ChatMessage(0L, ChatMessage.Sender.Player, TesterName, "hello @everyone **bold** <@123> [x](y)", "", Optional.empty(), 0L, false);
        DiscordNotices.computerChat(User, World, Computer, ChatChannels.Network, Draft);
        ComputerGrid.Reach Linked = ComputerGrid.reach(World, Computer);
        DiscordNotices.computerPosted(World, Linked, ChatChannels.Network, Draft);
        DiscordNotices.computerPosted(World, Linked, ChatChannels.Network,
                new ChatMessage(0L, ChatMessage.Sender.System, "LUNCH", "Lunch is served", "", Optional.empty(), 0L, false));
        settled(Helper, () -> {
            List<FakeDiscord.Hit> Posts = Fake.hits(Hook);
            check(Posts.size() == 2, "player chat must be mirrored once plus one scheduled message, got " + Posts.size());
            FakeDiscord.Hit Said = Posts.stream().filter(Entry -> field(Entry, "Player").isPresent()).findFirst().orElseThrow(() -> new GameTestAssertException("no player chat"));
            checkPayload(Said);
            String Body = description(Said);
            check(Body.startsWith(">>> "), "chat text is not quoted");
            check(Body.contains("@\u200Beveryone"), "mentions are not broken up: " + Body);
            check(Body.contains("\\*\\*bold\\*\\*") && Body.contains("\\<@\u200B123>") && Body.contains("\\[x\\]"), "markdown is not escaped: " + Body);
            check(Said.embed().getAsJsonObject("author").get("name").getAsString().equals(TesterName), "sender is not the author");
            check(Said.embed().getAsJsonObject("author").get("icon_url").getAsString().equals(faceOf(User)), "sender face is missing");
            check(field(Said, "Computer").orElse("").startsWith("PC-"), "computer node is missing: " + field(Said, "Computer"));
            check(field(Said, "Channel").orElse("").equals("NETWORK"), "channel is missing");
            check(field(Said, "Position").isPresent() == Coordinates, "position shown " + field(Said, "Position").isPresent() + " but expected " + Coordinates);
            check(field(Said, "Dimension").isPresent() == Coordinates, "dimension shown when it should not be");
            if (Coordinates) {
                check(field(Said, "Position").orElse("").equals(DiscordEmbed.code(Computer)), "computer position is wrong");
            }
            FakeDiscord.Hit Scheduled = Posts.stream().filter(Entry -> field(Entry, "Sender").isPresent()).findFirst().orElseThrow(() -> new GameTestAssertException("no scheduled message"));
            check(description(Scheduled).contains("Lunch is served"), "scheduled text is missing");
            check(field(Scheduled, "Sender").orElse("").contains("LUNCH"), "scheduled sender is missing");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_chat_plain")
    public static void computerChatHidesCoordinatesByDefault(GameTestHelper Helper) {
        chatTest(Helper, "chat_plain", false);
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_chat_coords")
    public static void computerChatShowsCoordinatesWhenAsked(GameTestHelper Helper) {
        chatTest(Helper, "chat_coords", true);
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_strangers")
    public static void strangerVisitsAreReported(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(only(DiscordSettings.testing(Fake.url("strangers_hook")), DiscordCategory.Strangers));
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Spot = Helper.absolutePos(new BlockPos(1, 1, 1));
        DiscordNotices.strangerAppeared(User, StrangerVariants.Foundation, Spot, Scp173);
        DiscordNotices.strangerDelivered(User, User, StrangerVariants.Foundation, List.of(new StrangerGift(ResourceLocation.withDefaultNamespace("apple"), 3)), Scp173);
        DiscordNotices.strangerLeft(World.getServer(), true, User.getUUID(), StrangerVariants.Foundation, Level.OVERWORLD.location().toString(), Spot, Scp173);
        DiscordNotices.strangerLeft(World.getServer(), false, User.getUUID(), StrangerVariants.Foundation, Level.OVERWORLD.location().toString(), Spot, Scp173);
        settled(Helper, () -> {
            List<FakeDiscord.Hit> Posts = Fake.hits("strangers_hook");
            check(Posts.size() == 3, "expected appear, deliver and leave embeds, got " + Posts.size());
            List<FakeDiscord.Hit> Appeared = Fake.titled("strangers_hook", "appeared to");
            List<FakeDiscord.Hit> Delivered = Fake.titled("strangers_hook", "handed");
            List<FakeDiscord.Hit> Left = Fake.titled("strangers_hook", "left");
            check(Appeared.size() == 1 && Delivered.size() == 1 && Left.size() == 1, "stranger embeds are missing");
            check(title(Appeared.get(0)).contains("Mysterious Stranger") && title(Appeared.get(0)).contains("Discord\\_Tester"), "appear title is wrong");
            check(field(Delivered.get(0), "Delivered").orElse("").equals("3 \u00D7 Apple"), "delivery is wrong: " + field(Delivered.get(0), "Delivered"));
            check(field(Delivered.get(0), "Case").orElse("").equals("SCP-173"), "case is wrong");
            check(Appeared.get(0).embed().getAsJsonObject("thumbnail").get("url").getAsString().equals(faceOf(User)), "stranger embed misses the face");
            Posts.forEach(DiscordGameTests::checkPayload);
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_routing")
    public static void categoriesRouteToTheirOwnWebhooks(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        DiscordSettings Settings = only(DiscordSettings.testing(Fake.url("route_default")), DiscordCategory.Players, DiscordCategory.Computers, DiscordCategory.Strangers)
                .withWebhook(DiscordCategory.Computers, Fake.url("route_moderators"))
                .withWebhook(DiscordCategory.Strangers, Fake.url("route_strangers"));
        Discord.useForTests(Settings);
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Spot = Helper.absolutePos(new BlockPos(1, 1, 1));
        DiscordNotices.playerJoined(User, false);
        DiscordNotices.computerChat(User, World, Spot, ChatChannels.Network,
                new ChatMessage(0L, ChatMessage.Sender.Player, TesterName, "routed", "", Optional.empty(), 0L, false));
        DiscordNotices.strangerAppeared(User, StrangerVariants.Foundation, Spot, null);
        settled(Helper, () -> {
            check(Fake.hits("route_default").size() == 1 && title(Fake.hits("route_default").get(0)).contains("joined the server"),
                    "the default webhook should only get the join");
            check(Fake.hits("route_moderators").size() == 1 && description(Fake.hits("route_moderators").get(0)).contains("routed"),
                    "the moderator webhook should only get the chat");
            check(Fake.hits("route_strangers").size() == 1 && title(Fake.hits("route_strangers").get(0)).contains("appeared"),
                    "the stranger webhook should only get the stranger");
            check(Fake.hits().size() == 3, "unexpected extra posts: " + Fake.hits().size());
            Fake.hits().forEach(DiscordGameTests::checkPayload);
        });
    }

    private static void collect(UnmodifiableConfig Values, String Path, List<String> Found) {
        for (Map.Entry<String, Object> Entry : Values.valueMap().entrySet()) {
            String Here = Path.isEmpty() ? Entry.getKey() : Path + "." + Entry.getKey();
            if (Entry.getValue() instanceof UnmodifiableConfig Inner) {
                collect(Inner, Here, Found);
            } else {
                Found.add(Here);
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "discord_unit")
    public static void webhookUrlsStayOutOfTheSyncedServerConfig(GameTestHelper Helper) {
        List<String> Synced = new ArrayList<>();
        collect(ScpConfigs.Server.getValues(), "", Synced);
        Helper.assertTrue(Synced.contains("discord.general.enabled") && Synced.contains("discord.computers.coordinates"), "the server config is missing values: " + Synced);
        for (String Path : Synced) {
            Helper.assertTrue(!Path.toLowerCase(Locale.ROOT).contains("webhook"), "the synced server config holds " + Path);
        }
        List<String> Secret = new ArrayList<>();
        collect(ScpConfigs.Common.getValues(), "", Secret);
        Helper.assertTrue(Secret.contains("discord.webhooks.defaultWebhook") && Secret.contains("discord.webhooks.computers"), "the webhook file is missing values: " + Secret);
        ModConfig ServerFile = ModConfigs.getFileMap().get(DiscordConfig.FileName);
        ModConfig SecretFile = ModConfigs.getFileMap().get(DiscordWebhooks.FileName);
        Helper.assertTrue(ServerFile != null && ServerFile.getType() == ModConfig.Type.SERVER, "the Discord settings are not a server config");
        Helper.assertTrue(SecretFile != null && SecretFile.getType() == ModConfig.Type.COMMON, "the webhook file is not a common config, so it would be synced");
        for (ModConfig Shared : ModConfigs.getConfigSet(ModConfig.Type.SERVER)) {
            Helper.assertTrue(Shared.getSpec() != ScpConfigs.Common, "the webhook file is in the synced set");
        }
        try {
            String Sent = Files.readString(ServerFile.getFullPath(), StandardCharsets.UTF_8);
            Helper.assertTrue(!Sent.contains("/api/webhooks/") && !Sent.contains("defaultWebhook"), "the synced server file mentions webhook URLs");
        } catch (IOException Problem) {
            throw new GameTestAssertException("could not read the server config file");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "discord_unit")
    public static void mentionsAreAlwaysDisabled(GameTestHelper Helper) {
        DiscordSettings Settings = DiscordSettings.testing("https://discord.com/api/webhooks/123456/abcdefghijklmnopqrstuvwx");
        for (DiscordCategory Category : DiscordCategory.values()) {
            DiscordEmbed Embed = new DiscordEmbed().title("@everyone").description("@here <@&42> @everyone");
            JsonObject Payload = DiscordEmbed.payload(Settings, Category, Embed, Instant.now());
            JsonArray Parse = Payload.getAsJsonObject("allowed_mentions").getAsJsonArray("parse");
            Helper.assertTrue(Parse.isEmpty(), Category.getKey() + " allows mentions");
            Helper.assertTrue(!Payload.has("content"), Category.getKey() + " sends plain content");
            String Shown = Payload.getAsJsonArray("embeds").get(0).getAsJsonObject().get("description").getAsString();
            Helper.assertTrue(!Shown.contains("@everyone") && !Shown.contains("@here"), "mentions survive in " + Shown);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "discord_unit")
    public static void markdownIsEscapedAndTextIsTruncated(GameTestHelper Helper) {
        String Inline = "**b** _i_ ~~s~~ `c` > q # h - l [a](b) <@1> | \\ SCP-173";
        String Escaped = "\\*\\*b\\*\\* \\_i\\_ \\~\\~s\\~\\~ \\`c\\` > q # h - l \\[a\\](b) \\<@\u200B1> \\| \\\\ SCP-173";
        Helper.assertTrue(DiscordText.escape(Inline).equals(Escaped), "inline escape is wrong: " + DiscordText.escape(Inline));
        String Lines = "# head\n- item\n> quote\n12. twelve\n  -# sub\n+ plus";
        String Guarded = "\\# head\n\\- item\n\\> quote\n12\\. twelve\n  \\-# sub\n\\+ plus";
        Helper.assertTrue(DiscordText.escape(Lines).equals(Guarded), "line start escape is wrong: " + DiscordText.escape(Lines));
        Helper.assertTrue(DiscordText.clip("abcdef", 4).equals("abc\u2026"), "plain clip is wrong");
        Helper.assertTrue(DiscordText.clip("ab\\*cd", 4).equals("ab\u2026"), "clip split an escape: " + DiscordText.clip("ab\\*cd", 4));
        Helper.assertTrue(DiscordText.clip("ab\uD83D\uDE00cd", 4).equals("ab\u2026"), "clip split a surrogate pair");
        DiscordEmbed Embed = new DiscordEmbed().title("x".repeat(300)).description("*".repeat(5000)).author("a".repeat(300), "");
        for (int Index = 0; Index < 30; Index++) {
            Embed.field("f" + Index, "v".repeat(2000), false);
        }
        JsonObject Shaped = Embed.toJson(0x123456, "footer", Instant.now());
        Helper.assertTrue(Shaped.get("title").getAsString().length() <= DiscordText.TitleLimit, "title is too long");
        Helper.assertTrue(!Shaped.has("description") || Shaped.get("description").getAsString().length() <= DiscordText.DescriptionLimit, "description is too long");
        Helper.assertTrue(!Shaped.has("fields") || Shaped.getAsJsonArray("fields").size() <= DiscordText.FieldCount, "too many fields");
        if (Shaped.has("fields")) {
            for (JsonElement Row : Shaped.getAsJsonArray("fields")) {
                Helper.assertTrue(Row.getAsJsonObject().get("value").getAsString().length() <= DiscordText.FieldValueLimit, "field value is too long");
            }
        }
        Helper.assertTrue(total(Shaped) <= DiscordText.TotalLimit, "embed is over the 6000 limit: " + total(Shaped));
        Helper.assertTrue(Shaped.getAsJsonObject("author").get("name").getAsString().length() <= DiscordText.AuthorLimit, "author is too long");
        DiscordEmbed Many = new DiscordEmbed().title("t");
        for (int Index = 0; Index < 40; Index++) {
            Many.field("n" + Index, "v", true);
        }
        Helper.assertTrue(Many.toJson(0, "", Instant.now()).getAsJsonArray("fields").size() == DiscordText.FieldCount, "field cap is not 25");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_rate")
    public static void rateLimitsAreHonoured(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Fake.script("limited_hook", new FakeDiscord.Scripted(429, "{\"message\": \"You are being rate limited.\", \"retry_after\": 0.6, \"global\": false}", Map.of()));
        Fake.script("bucket_hook", new FakeDiscord.Scripted(204, "", Map.of("X-RateLimit-Remaining", "0", "X-RateLimit-Reset-After", "0.5")));
        DiscordRelay Relay = relay(Helper, 10, 50L);
        CompletableFuture<DiscordRelay.Result> Limited = Relay.offer(URI.create(Fake.url("limited_hook")), "{\"embeds\":[]}", "test");
        CompletableFuture<DiscordRelay.Result> First = Relay.offer(URI.create(Fake.url("bucket_hook")), "{\"embeds\":[]}", "test");
        CompletableFuture<DiscordRelay.Result> Second = Relay.offer(URI.create(Fake.url("bucket_hook")), "{\"embeds\":[]}", "test");
        eventually(Helper, () -> {
            check(Limited.isDone() && First.isDone() && Second.isDone(), "messages still in flight");
            check(Limited.join().Success() && First.join().Success() && Second.join().Success(), "a message failed");
            List<FakeDiscord.Hit> Tries = Fake.hits("limited_hook");
            check(Tries.size() == 2, "a 429 should be retried exactly once, got " + Tries.size());
            long Gap = Tries.get(1).AtMillis() - Tries.get(0).AtMillis();
            check(Gap >= 550L, "retry_after was not honoured, retried after " + Gap + " ms");
            List<FakeDiscord.Hit> Paced = Fake.hits("bucket_hook");
            check(Paced.size() == 2, "bucket messages missing");
            long Wait = Paced.get(1).AtMillis() - Paced.get(0).AtMillis();
            check(Wait >= 450L, "X-RateLimit-Reset-After was not honoured, sent after " + Wait + " ms");
            check(Relay.status().Limited() == 1L, "the 429 was not counted");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_retry")
    public static void serverErrorsRetryAndClientErrorsDoNot(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Fake.script("flaky_hook", new FakeDiscord.Scripted(502, "", Map.of()), new FakeDiscord.Scripted(500, "", Map.of()));
        Fake.script("gone_hook", new FakeDiscord.Scripted(404, "{\"message\": \"Unknown Webhook\", \"code\": 10015}", Map.of()));
        Fake.script("bad_hook", new FakeDiscord.Scripted(400, "{\"message\": \"Invalid Form Body\"}", Map.of()));
        DiscordRelay Relay = relay(Helper, 10, 50L);
        CompletableFuture<DiscordRelay.Result> Flaky = Relay.offer(URI.create(Fake.url("flaky_hook")), "{}", "test");
        CompletableFuture<DiscordRelay.Result> Gone = Relay.offer(URI.create(Fake.url("gone_hook")), "{}", "test");
        CompletableFuture<DiscordRelay.Result> Bad = Relay.offer(URI.create(Fake.url("bad_hook")), "{}", "test");
        eventually(Helper, () -> {
            check(Flaky.isDone() && Gone.isDone() && Bad.isDone(), "messages still in flight");
            check(Flaky.join().Success() && Fake.hits("flaky_hook").size() == 3, "5xx should be retried until it works");
            check(!Gone.join().Success() && Gone.join().Status() == 404 && Fake.hits("gone_hook").size() == 1, "404 must not be retried");
            check(!Bad.join().Success() && Fake.hits("bad_hook").size() == 1, "400 must not be retried");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_queue")
    public static void fullQueueDropsNewMessages(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        DiscordRelay Relay = relay(Helper, 3, 50L);
        Relay.hold(true);
        List<CompletableFuture<DiscordRelay.Result>> Sent = new ArrayList<>();
        for (int Index = 0; Index < 5; Index++) {
            Sent.add(Relay.offer(URI.create(Fake.url("queue_hook")), "{\"n\":" + Index + "}", "test"));
        }
        Helper.assertTrue(Relay.queued() == 3, "queue should hold 3, holds " + Relay.queued());
        Helper.assertTrue(Relay.dropped() == 2, "two messages should be dropped, dropped " + Relay.dropped());
        Helper.assertTrue(Sent.get(3).isDone() && !Sent.get(3).join().Success() && Sent.get(4).isDone(), "dropped messages should fail at once");
        Relay.hold(false);
        eventually(Helper, () -> {
            check(Fake.hits("queue_hook").size() == 3, "expected the three queued messages, got " + Fake.hits("queue_hook").size());
            check(Sent.get(0).join().Success() && Sent.get(1).join().Success() && Sent.get(2).join().Success(), "queued messages failed");
            check(Fake.hits("queue_hook").get(0).Body().equals("{\"n\":0}"), "queue order changed");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_disabled")
    public static void nothingIsSentWhileDisabled(GameTestHelper Helper) {
        DiscordSettings Stock = DiscordConfig.snapshot();
        Helper.assertTrue(!Stock.Enabled(), "the shipped config must start disabled");
        Helper.assertTrue(!Stock.AllowLoopback(), "the real config must never allow loopback webhooks");
        Helper.assertTrue(Stock.FaceTemplate().equals(DiscordSettings.DefaultFace), "player face template default changed");
        Helper.assertTrue(!Stock.coordinatesFor(DiscordCategory.Computers) && Stock.coordinatesFor(DiscordCategory.Security), "coordinate defaults are wrong");
        Helper.assertTrue(ScpConfigs.Server.isLoaded(), "the Discord server config is not loaded");
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(DiscordSettings.testing(Fake.url("off_hook")).withEnabled(false));
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Spot = Helper.absolutePos(new BlockPos(1, 1, 1));
        DiscordNotices.playerJoined(User, true);
        DiscordNotices.playerLeft(User);
        DiscordNotices.documentOpened(User, Scp173, AnomalyDocuments.server().get(Scp173), true);
        DiscordNotices.anomalyArrived(new AnomalySpawners.Discovery(World, Spot, User, Scp173, AnomalyDocuments.server().get(Scp173), false));
        DiscordNotices.containment(World, Spot, Scp106Registry.DocumentId, true);
        DiscordNotices.mtfCalled(User, MtfTier.Faint, Spot);
        DiscordNotices.meltdown(World, Spot, true);
        DiscordNotices.computerChat(User, World, Spot, ChatChannels.Network, new ChatMessage(0L, ChatMessage.Sender.Player, TesterName, "x", "", Optional.empty(), 0L, false));
        DiscordNotices.strangerAppeared(User, StrangerVariants.Foundation, Spot, Scp173);
        for (DiscordCategory Category : DiscordCategory.values()) {
            Helper.assertTrue(Discord.post(Category, DiscordNotices.sample(Category, null)).isEmpty(), Category.getKey() + " posted while disabled");
        }
        Helper.assertTrue(DiscordCommands.test(World.getServer().createCommandSourceStack().withSuppressedOutput(), DiscordCategory.Players) == 0, "the test command ran while disabled");
        settled(Helper, () -> {
            Helper.assertTrue(Fake.hits().isEmpty(), "something was sent while disabled: " + Fake.hits().size());
            Helper.assertTrue(Discord.current() == null, "the relay started while disabled");
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_invalid")
    public static void invalidWebhooksAreRefused(GameTestHelper Helper) {
        for (String Good : List.of("https://discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyzABCDEF_-12",
                "https://discordapp.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz",
                "https://ptb.discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz",
                "https://canary.discord.com/api/v10/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz",
                "https://discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz?thread_id=123456789012")) {
            Helper.assertTrue(DiscordUrls.parse(Good, false).isPresent(), "a real webhook was refused: " + DiscordUrls.label(Good));
        }
        for (String Bad : List.of("", "not a url", "http://discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz",
                "https://discord.com.evil.example/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz",
                "https://evil.example/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz",
                "https://discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz/../../users/@me",
                "https://user@discord.com/api/webhooks/123456789012345678/abcdefghijklmnopqrstuvwxyz",
                "https://discord.com/api/channels/123456789012345678/messages",
                "http://127.0.0.1:8080/api/webhooks/123/abc")) {
            Helper.assertTrue(DiscordUrls.parse(Bad, false).isEmpty(), "a bad webhook was accepted: " + DiscordUrls.label(Bad));
        }
        Helper.assertTrue(DiscordUrls.parse("http://127.0.0.1:8080/api/webhooks/123/abc", true).isPresent(), "test loopback is refused under the test flag");
        FakeDiscord Fake = fake(Helper);
        DiscordSettings Settings = only(DiscordSettings.testing(Fake.url("loop_hook")).withLoopback(false), DiscordCategory.Players)
                .withWebhook(DiscordCategory.Players, "");
        Discord.useForTests(Settings);
        Helper.assertTrue(Discord.route(Settings, DiscordCategory.Players).State() == Discord.Readiness.BadWebhook, "loopback was allowed without the test flag");
        DiscordSettings Foreign = Settings.withDefaultWebhook("https://evil.example/api/webhooks/123456/" + Secret);
        Helper.assertTrue(Discord.route(Foreign, DiscordCategory.Players).State() == Discord.Readiness.BadWebhook, "a foreign host was accepted");
        Helper.assertTrue(Discord.route(Settings.withDefaultWebhook(""), DiscordCategory.Players).State() == Discord.Readiness.NoWebhook, "an empty webhook was not reported");
        DiscordNotices.playerJoined(tester(Helper), true);
        Helper.assertTrue(Discord.post(DiscordCategory.Players, DiscordNotices.sample(DiscordCategory.Players, null)).isEmpty(), "a refused webhook queued a message");
        settled(Helper, () -> {
            Helper.assertTrue(Fake.hits().isEmpty(), "a refused webhook still received " + Fake.hits().size() + " posts");
        });
    }

    private static final class Capture extends AbstractAppender {
        private final List<String> Lines = new CopyOnWriteArrayList<>();

        private Capture() {
            super("DiscordLogCapture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent Event) {
            StringBuilder Line = new StringBuilder(Event.getMessage().getFormattedMessage());
            if (Event.getThrown() != null) {
                StringWriter Trace = new StringWriter();
                Event.getThrown().printStackTrace(new PrintWriter(Trace));
                Line.append('\n').append(Trace);
            }
            Lines.add(Line.toString());
        }
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_logs")
    public static void webhookUrlsNeverReachTheLog(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Capture Sink = new Capture();
        Sink.start();
        List<Logger> Watched = new ArrayList<>();
        for (String Name : List.of(LogManager.ROOT_LOGGER_NAME, DiscordRelay.class.getName(), Discord.class.getName(), DiscordNotices.class.getName())) {
            if (LogManager.getLogger(Name) instanceof Logger Core) {
                Core.addAppender(Sink);
                Watched.add(Core);
            }
        }
        whenDone(Helper, () -> {
            for (Logger Core : Watched) {
                Core.removeAppender(Sink);
            }
            Sink.stop();
        });
        Helper.assertTrue(!Watched.isEmpty(), "could not attach to the log");
        int Closed;
        try (ServerSocket Probe = new ServerSocket(0)) {
            Closed = Probe.getLocalPort();
        } catch (IOException Problem) {
            throw new GameTestAssertException("no free port");
        }
        String Gone = Fake.url(Secret);
        String Dead = "http://127.0.0.1:" + Closed + "/api/webhooks/123456/" + Secret;
        String Foreign = "https://evil.example/api/webhooks/123456/" + Secret;
        Fake.script(Secret, new FakeDiscord.Scripted(404, "{\"message\": \"Unknown Webhook\"}", Map.of()));
        Discord.route(DiscordSettings.testing(Foreign), DiscordCategory.Security);
        DiscordRelay Relay = relay(Helper, 10, 20L);
        CompletableFuture<DiscordRelay.Result> Missing = Relay.offer(URI.create(Gone), "{}", "security");
        CompletableFuture<DiscordRelay.Result> Refused = Relay.offer(URI.create(Dead), "{}", "computers");
        Helper.assertTrue(!DiscordUrls.scrub("failed at " + Gone + " today", Gone).contains(Secret), "scrub left the token");
        Helper.assertTrue(!DiscordUrls.label(Gone).contains(Secret), "label shows the token");
        eventually(Helper, () -> {
            check(Missing.isDone() && Refused.isDone(), "messages still in flight");
            check(!Missing.join().Success() && !Refused.join().Success(), "failures were reported as success");
            check(!Sink.Lines.isEmpty(), "nothing was logged, the capture is not working");
            for (String Line : Sink.Lines) {
                check(!Line.contains(Secret), "the webhook token reached the log: " + Line.replace(Secret, "<token>"));
                check(!Line.contains("/api/webhooks/"), "a webhook URL reached the log: " + DiscordUrls.scrub(Line, Gone));
            }
            DiscordRelay.Snapshot Now = Relay.status();
            check(!Now.LastError().contains(Secret) && !Now.LastError().contains("/api/webhooks/"), "the status shows the webhook");
            check(Sink.Lines.stream().anyMatch(Line -> Line.contains("webhook#")), "warnings do not name the webhook by its label");
            for (DiscordRelay.BucketView View : Now.Buckets()) {
                check(View.Label().startsWith("webhook#"), "bucket label leaks the URL");
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_command")
    public static void testCommandReachesTheCategoryWebhook(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(DiscordSettings.testing(Fake.url("command_default")).withWebhook(DiscordCategory.Strangers, Fake.url("command_strangers"))
                .withCategoryEnabled(DiscordCategory.Security, false));
        CommandSourceStack Source = Helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
        Helper.assertTrue(DiscordCommands.test(Source, DiscordCategory.Strangers) == 1, "the strangers test was not sent");
        Helper.assertTrue(DiscordCommands.test(Source, null) == 2, "a full test should post once per distinct webhook");
        Helper.assertTrue(DiscordCommands.status(Source) == 1, "status failed");
        eventually(Helper, () -> {
            List<FakeDiscord.Hit> Strangers = Fake.titled("command_strangers", "Test message");
            List<FakeDiscord.Hit> Shared = Fake.titled("command_default", "Test message");
            check(Strangers.size() == 2, "the stranger webhook should get two tests, got " + Strangers.size());
            check(Shared.size() == 1, "the default webhook should get one shared test, got " + Shared.size());
            check(title(Strangers.get(0)).contains("Strangers"), "the test does not name its category");
            check(field(Shared.get(0), "Categories").orElse("").contains("computers"), "the shared test does not list its categories");
            check(field(Shared.get(0), "Categories").orElse("").contains("security"), "disabled categories still share the webhook and are tested");
            Fake.hits().forEach(DiscordGameTests::checkPayload);
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_first_read")
    public static void firstReadPostsOncePerPlayerAndFileAndSurvivesReloading(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(filtered(Fake, "first_hook", DiscordCategory.Documents, DiscordFilters.standard().withReads(DiscordReads.FirstRead)));
        MinecraftServer Server = Helper.getLevel().getServer();
        ResourceLocation Scp106 = Scp106Registry.DocumentId;
        check(AnomalyDocuments.server().get(Scp173) != null && AnomalyDocuments.server().get(Scp106) != null, "SCP-173 or SCP-106 has no document");
        FakePlayer First = reader(Helper, "FirstReader");
        FakePlayer Second = reader(Helper, "SecondReader");
        FakePlayer Unsealer = reader(Helper, "SealReader");
        List<FakePlayer> Readers = List.of(First, Second, Unsealer);
        forgetReads(Server, Readers);
        whenDone(Helper, () -> forgetReads(Server, Readers));
        ItemStack Copy173 = opened(Scp173);
        ItemStack Copy106 = opened(Scp106);
        DiscordNotices.documentRead(First, Copy173);
        DiscordNotices.documentRead(First, Copy173);
        DiscordNotices.documentRead(First, Copy106);
        DiscordNotices.documentRead(Second, Copy173);
        DiscordNotices.documentOpened(Unsealer, Scp173, AnomalyDocuments.server().get(Scp173), true);
        DiscordNotices.documentRead(Unsealer, Copy173);
        DiscordReadLog Seen = DiscordReadLog.get(Server);
        check(Seen.has(First.getUUID(), Scp173) && Seen.has(First.getUUID(), Scp106) && Seen.has(Second.getUUID(), Scp173), "first reads were not remembered");
        check(!Seen.has(Second.getUUID(), Scp106), "a file the player never opened is remembered");
        check(Seen.has(Unsealer.getUUID(), Scp173), "breaking the seal was not remembered as the first read");
        CompoundTag Saved = Seen.save(new CompoundTag(), Server.registryAccess());
        DiscordReadLog Loaded = DiscordReadLog.load(Saved, Server.registryAccess());
        for (FakePlayer Reader : Readers) {
            check(Loaded.openedBy(Reader.getUUID()).equals(Seen.openedBy(Reader.getUUID())), "the read log changed after saving and loading");
        }
        check(Loaded.total() == Seen.total(), "the reloaded read log has a different size");
        Server.overworld().getDataStorage().set(DiscordReadLog.DataName, Loaded);
        check(DiscordReadLog.get(Server) == Loaded, "the reloaded read log is not in use");
        DiscordNotices.documentRead(First, Copy173);
        DiscordNotices.documentRead(Second, Copy173);
        DiscordNotices.documentRead(Second, Copy106);
        settled(Helper, () -> {
            check(reads(Fake, "first_hook", "FirstReader", "SCP-173") == 1, "SCP-173 should post once for the first reader, got " + reads(Fake, "first_hook", "FirstReader", "SCP-173"));
            check(reads(Fake, "first_hook", "FirstReader", "SCP-106") == 1, "a second file should post once for the same reader");
            check(reads(Fake, "first_hook", "SecondReader", "SCP-173") == 1, "a reload brought back a repeat post or lost the first one");
            check(reads(Fake, "first_hook", "SecondReader", "SCP-106") == 1, "a new file after the reload should still post");
            check(reads(Fake, "first_hook", "SealReader", "SCP-173") == 0, "reading after breaking the seal should not post again");
            check(Fake.titled("first_hook", "opened a sealed file").size() == 1, "the sealed file opening is missing");
            check(Fake.hits("first_hook").size() == 5, "expected five document posts, got " + Fake.hits("first_hook").size());
            Fake.hits("first_hook").forEach(DiscordGameTests::checkPayload);
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_reads_off")
    public static void readsSetToOffPostNothing(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        DiscordFilters Quiet = DiscordFilters.standard().withReads(DiscordReads.Off);
        Discord.useForTests(filtered(Fake, "off_reads_hook", DiscordCategory.Documents, Quiet));
        MinecraftServer Server = Helper.getLevel().getServer();
        FakePlayer User = reader(Helper, "QuietReader");
        forgetReads(Server, List.of(User));
        whenDone(Helper, () -> forgetReads(Server, List.of(User)));
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp173);
        ItemStack Copy = opened(Scp173);
        DiscordNotices.documentRead(User, Copy);
        DiscordNotices.documentRead(User, Copy);
        DiscordNotices.documentOpened(User, Scp173, Document, false);
        DiscordNotices.documentOpened(User, Scp173, Document, true);
        Discord.useForTests(filtered(Fake, "off_reads_hook", DiscordCategory.Documents, Quiet.withToggle(DiscordToggle.SealedFiles, false)));
        DiscordNotices.documentOpened(User, Scp173, Document, true);
        DiscordNotices.documentRead(User, Copy);
        check(!DiscordReadLog.get(Server).has(User.getUUID(), Scp173), "reads were remembered while reads are off");
        settled(Helper, () -> {
            check(Fake.titled("off_reads_hook", "is reading a file").isEmpty(), "a read was posted while reads are off");
            check(Fake.titled("off_reads_hook", "opened a sealed file").size() == 1, "only the sealed file opened before muting it should post");
            check(Fake.hits("off_reads_hook").size() == 1, "expected one post, got " + Fake.hits("off_reads_hook").size());
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_every_read")
    public static void everyReadPostsEachRead(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(filtered(Fake, "every_hook", DiscordCategory.Documents, DiscordFilters.standard().withReads(DiscordReads.EveryRead).withRereadMinutes(0)));
        MinecraftServer Server = Helper.getLevel().getServer();
        FakePlayer User = reader(Helper, "EveryReader");
        forgetReads(Server, List.of(User));
        ItemStack Copy = opened(Scp173);
        for (int Index = 0; Index < 3; Index++) {
            DiscordNotices.documentRead(User, Copy);
        }
        check(!DiscordReadLog.get(Server).has(User.getUUID(), Scp173), "EveryRead should not fill the first read log");
        settled(Helper, () -> {
            check(reads(Fake, "every_hook", "EveryReader", "SCP-173") == 3, "every read should post, got " + reads(Fake, "every_hook", "EveryReader", "SCP-173"));
            Fake.hits("every_hook").forEach(DiscordGameTests::checkPayload);
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_strangers_muted")
    public static void mutedStrangerDeliveriesAreNotPosted(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(filtered(Fake, "muted_strangers", DiscordCategory.Strangers, DiscordFilters.standard().withToggle(DiscordToggle.Deliveries, false)));
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Spot = Helper.absolutePos(new BlockPos(1, 1, 1));
        DiscordNotices.strangerAppeared(User, StrangerVariants.Foundation, Spot, Scp173);
        DiscordNotices.strangerDelivered(User, User, StrangerVariants.Foundation, List.of(new StrangerGift(ResourceLocation.withDefaultNamespace("apple"), 3)), Scp173);
        DiscordNotices.strangerLeft(World.getServer(), true, User.getUUID(), StrangerVariants.Foundation, Level.OVERWORLD.location().toString(), Spot, Scp173);
        settled(Helper, () -> {
            check(Fake.titled("muted_strangers", "handed").isEmpty(), "a muted delivery was posted");
            check(Fake.titled("muted_strangers", "appeared to").size() == 1 && Fake.titled("muted_strangers", "left").size() == 1, "appear or leave went missing");
            check(Fake.hits("muted_strangers").size() == 2, "expected two stranger posts, got " + Fake.hits("muted_strangers").size());
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_security_filtered")
    public static void securityFiltersDropTheQuieterEvents(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        DiscordFilters Rules = DiscordFilters.standard().withRaids(DiscordRaids.Milestones).withWarhead(DiscordWarhead.Countdown).withToggle(DiscordToggle.MtfSquads, false);
        Discord.useForTests(filtered(Fake, "filtered_security", DiscordCategory.Security, Rules));
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Panel = Helper.absolutePos(new BlockPos(2, 1, 2));
        long Now = World.getGameTime();
        Raid Current = new Raid(9002, ChaosFaction.Id, 0, World.dimension(), RaidBases.TeamPrefix + "Blue", Panel, 0.0, 8.0, 1.0, 0.0, true, Now, Panel);
        Current.setStage(RaidStage.Active, Now);
        RaidContext Context = RaidDirector.context(World.getServer(), Current, Now);
        check(Context != null, "no raid context for the Chaos Insurgency");
        DiscordNotices.raid(Context, "start", Component.literal("Raiders are here"));
        DiscordNotices.raid(Context, "stolen", Component.literal("Raiders took a crate"));
        DiscordNotices.mtfCalled(User, MtfTier.Volatile, Panel);
        DiscordNotices.warheadLever(World, Panel, User, true, 2);
        DiscordNotices.warheadEngaged(World, Panel, User, 2, Now + 1800);
        DiscordNotices.meltdown(World, Panel, false);
        settled(Helper, () -> {
            for (String Fragment : List.of("Raid started", "countdown started", "meltdown")) {
                check(Fake.titled("filtered_security", Fragment).size() == 1, "missing security embed: " + Fragment);
            }
            for (String Fragment : List.of("seized loot", "called in", "Warhead armed")) {
                check(Fake.titled("filtered_security", Fragment).isEmpty(), "a filtered security embed was posted: " + Fragment);
            }
            check(Fake.hits("filtered_security").size() == 3, "expected three security posts, got " + Fake.hits("filtered_security").size());
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_anomalies_filtered")
    public static void anomalyFiltersDropRecontainmentAndRifts(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        DiscordFilters Rules = DiscordFilters.standard().withContainment(DiscordContainment.Breaches).withToggle(DiscordToggle.Rifts, false);
        Discord.useForTests(filtered(Fake, "filtered_anomalies", DiscordCategory.Anomalies, Rules));
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Spot = Helper.absolutePos(new BlockPos(1, 1, 1));
        DiscordNotices.anomalyArrived(new AnomalySpawners.Discovery(World, Spot, User, Scp173, AnomalyDocuments.server().get(Scp173), true));
        DiscordNotices.containment(World, Spot, Scp106Registry.DocumentId, true);
        DiscordNotices.containment(World, Spot, Scp106Registry.DocumentId, false);
        DiscordNotices.rift(World, Spot, true);
        settled(Helper, () -> {
            check(Fake.titled("filtered_anomalies", "has manifested").size() == 1 && Fake.titled("filtered_anomalies", "breached containment").size() == 1,
                    "the arrival or the breach went missing");
            check(Fake.titled("filtered_anomalies", "been contained").isEmpty(), "a contained message was posted in Breaches mode");
            check(Fake.titled("filtered_anomalies", "Apocalyptic Rift").isEmpty(), "a muted rift was posted");
            check(Fake.hits("filtered_anomalies").size() == 2, "expected two anomaly posts, got " + Fake.hits("filtered_anomalies").size());
        });
    }

    @GameTest(template = Arena, timeoutTicks = Patience, batch = "discord_chat_filtered")
    public static void mutedScheduledMessagesAreNotPosted(GameTestHelper Helper) {
        FakeDiscord Fake = fake(Helper);
        Discord.useForTests(filtered(Fake, "filtered_chat", DiscordCategory.Computers, DiscordFilters.standard().withToggle(DiscordToggle.ScheduledMessages, false)));
        ServerLevel World = Helper.getLevel();
        FakePlayer User = tester(Helper);
        BlockPos Computer = Helper.absolutePos(new BlockPos(1, 1, 1));
        ComputerGrid.Reach Linked = ComputerGrid.reach(World, Computer);
        DiscordNotices.computerChat(User, World, Computer, ChatChannels.Network, new ChatMessage(0L, ChatMessage.Sender.Player, TesterName, "hello", "", Optional.empty(), 0L, false));
        DiscordNotices.computerPosted(World, Linked, ChatChannels.Network, new ChatMessage(0L, ChatMessage.Sender.System, "LUNCH", "Lunch is served", "", Optional.empty(), 0L, false));
        DiscordNotices.computerPosted(World, Linked, ChatChannels.Network, new ChatMessage(0L, ChatMessage.Sender.Anomaly, "VOICE", "Static", "", Optional.empty(), 0L, false));
        settled(Helper, () -> {
            List<FakeDiscord.Hit> Posts = Fake.hits("filtered_chat");
            check(Posts.size() == 2, "expected the player and anomalous messages only, got " + Posts.size());
            check(Posts.stream().noneMatch(Entry -> description(Entry).contains("Lunch is served")), "a muted scheduled message was posted");
            check(Posts.stream().anyMatch(Entry -> field(Entry, "Player").isPresent()), "the player message went missing");
            check(Posts.stream().anyMatch(Entry -> field(Entry, "Sender").orElse("").contains("anomalous")), "the anomalous message went missing");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "discord_unit")
    public static void eventSettingsKeepTheOldBehaviourByDefault(GameTestHelper Helper) {
        DiscordFilters Stock = DiscordConfig.snapshot().Filters();
        Helper.assertTrue(Stock.equals(DiscordFilters.standard()), "the shipped event settings are not the standard ones: " + Stock);
        Helper.assertTrue(Stock.Muted().isEmpty(), "an event kind is muted by default");
        Helper.assertTrue(Stock.Reads() == DiscordReads.EveryRead && Stock.RereadMinutes() == 10, "document reads no longer default to every read with a ten minute gap");
        Helper.assertTrue(Stock.Containment() == DiscordContainment.All && Stock.Raids() == DiscordRaids.All && Stock.Warhead() == DiscordWarhead.All, "a mode does not default to All");
        List<String> Synced = new ArrayList<>();
        collect(ScpConfigs.Server.getValues(), "", Synced);
        for (DiscordToggle Toggle : DiscordToggle.values()) {
            Helper.assertTrue(Synced.contains("discord." + Toggle.getCategory().getKey() + "." + Toggle.getKey()), "the server config misses " + Toggle.getKey());
        }
        for (String Path : List.of("discord.documents.reads", "discord.documents.rereadMinutes", "discord.anomalies.containment", "discord.security.raids", "discord.security.warhead")) {
            Helper.assertTrue(Synced.contains(Path), "the server config misses " + Path);
        }
        Helper.succeed();
    }
}
