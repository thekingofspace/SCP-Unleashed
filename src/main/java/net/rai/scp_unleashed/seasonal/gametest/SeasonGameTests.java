package net.rai.scp_unleashed.seasonal.gametest;

import net.rai.scp_unleashed.config.ScpConfigs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.seasonal.Season;
import net.rai.scp_unleashed.seasonal.SeasonConfig;
import net.rai.scp_unleashed.seasonal.SeasonMode;
import net.rai.scp_unleashed.seasonal.SeasonSync;
import net.rai.scp_unleashed.seasonal.SeasonalLooks;
import net.rai.scp_unleashed.seasonal.Seasons;
import net.rai.scp_unleashed.seasonal.network.SeasonsPayload;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class SeasonGameTests {
    public static final String Arena = "scp420j_arena";
    public static final int Year = 2026;

    private SeasonGameTests() {
    }

    private static final class Recorder implements SeasonSync.Outbox {
        private final List<ServerPlayer> Receivers = new ArrayList<>();
        private final List<Integer> Direct = new ArrayList<>();
        private final List<Integer> Broadcast = new ArrayList<>();

        @Override
        public void toPlayer(ServerPlayer Receiver, SeasonsPayload Payload) {
            Receivers.add(Receiver);
            Direct.add(Payload.Mask());
        }

        @Override
        public void toAll(MinecraftServer Server, SeasonsPayload Payload) {
            Broadcast.add(Payload.Mask());
        }

        private void clear() {
            Receivers.clear();
            Direct.clear();
            Broadcast.clear();
        }
    }

    private static LocalDate day(int Year0, int Month0, int Day0) {
        return LocalDate.of(Year0, Month0, Day0);
    }

    private static void at(LocalDate Fixed) {
        Seasons.useClock(() -> Fixed);
    }

    private static Recorder record() {
        Recorder Made = new Recorder();
        SeasonSync.useOutbox(Made);
        return Made;
    }

    private static void restore(MinecraftServer Server) {
        for (Season Each : Season.values()) {
            Seasons.force(Each, SeasonMode.Auto);
        }
        Seasons.resetClock();
        SeasonSync.resetOutbox();
        SeasonSync.update(Server);
    }

    private static boolean configAuto() {
        for (Season Each : Season.values()) {
            if (SeasonConfig.mode(Each) != SeasonMode.Auto) {
                return false;
            }
        }
        return true;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "season_dates")
    public static void halloweenWindowByDate(GameTestHelper Helper) {
        Season Which = Season.Halloween;
        for (int Each : new int[]{2023, 2024, Year, 2028, 2100}) {
            Helper.assertFalse(Which.covers(day(Each, 10, 25)), "25 October " + Each + " counted as Halloween");
            Helper.assertTrue(Which.covers(day(Each, 10, 26)), "26 October " + Each + " is not Halloween");
            Helper.assertTrue(Which.covers(day(Each, 10, 31)), "31 October " + Each + " is not Halloween");
            Helper.assertFalse(Which.covers(day(Each, 11, 1)), "1 November " + Each + " counted as Halloween");
            Helper.assertFalse(Which.covers(day(Each, 1, 1)), "New Year " + Each + " counted as Halloween");
        }
        Helper.assertFalse(Which.covers(day(2024, 2, 29)), "the leap day counted as Halloween");
        Helper.assertTrue(Seasons.decide(day(Year, 10, 28), Any -> SeasonMode.Auto) == Which.bit(), "the calendar decision missed 28 October");
        Helper.assertTrue(Seasons.decide(day(Year, 7, 4), Any -> SeasonMode.Auto) == 0, "the calendar decision fired in July");
        Helper.assertTrue(Season.byKey("halloween").orElse(null) == Which, "the halloween key does not resolve");
        Helper.assertTrue(Seasons.of(Which.bit()).contains(Which) && Seasons.of(0).isEmpty(), "season masks do not round trip");
        MinecraftServer Server = Helper.getLevel().getServer();
        try {
            if (configAuto()) {
                at(day(Year, 10, 25));
                Helper.assertFalse(Which.in(Seasons.compute()), "the injected clock on 25 October gave Halloween");
                at(day(Year, 10, 26));
                Helper.assertTrue(Which.in(Seasons.compute()), "the injected clock on 26 October missed Halloween");
                at(day(2024, 10, 31));
                Helper.assertTrue(Which.in(Seasons.compute()), "the injected clock on 31 October 2024 missed Halloween");
                at(day(2024, 11, 1));
                Helper.assertFalse(Which.in(Seasons.compute()), "the injected clock on 1 November 2024 gave Halloween");
            }
        } finally {
            restore(Server);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "season_override")
    public static void overridesBeatTheDate(GameTestHelper Helper) {
        Season Which = Season.Halloween;
        Helper.assertTrue(Seasons.decide(Which, day(Year, 7, 1), SeasonMode.On), "On did not win over a July date");
        Helper.assertFalse(Seasons.decide(Which, day(Year, 10, 31), SeasonMode.Off), "Off did not win over Halloween itself");
        Helper.assertTrue(Seasons.decide(Which, day(Year, 10, 31), SeasonMode.Auto), "Auto ignored the date");
        int Every = 0;
        for (Season Each : Season.values()) {
            Every |= Each.bit();
        }
        Helper.assertTrue(Seasons.decide(day(Year, 3, 3), Any -> SeasonMode.On) == Every, "On for every season did not set every bit");
        Helper.assertTrue(Seasons.decide(day(Year, 10, 27), Any -> SeasonMode.Off) == 0, "Off for every season left a bit set");
        MinecraftServer Server = Helper.getLevel().getServer();
        Recorder Sent = record();
        try {
            at(day(Year, 10, 28));
            Seasons.force(Which, SeasonMode.Off);
            Helper.assertTrue(Seasons.mode(Which) == SeasonMode.Off, "the session override did not take");
            Helper.assertFalse(Which.in(Seasons.compute()), "a session Off still showed Halloween on 28 October");
            at(day(Year, 7, 1));
            Seasons.force(Which, SeasonMode.On);
            Helper.assertTrue(Which.in(Seasons.compute()), "a session On did not show Halloween in July");
            Seasons.force(Which, SeasonMode.Auto);
            Helper.assertTrue(Seasons.forced(Which) == SeasonMode.Auto, "auto did not clear the session override");
            if (configAuto()) {
                Helper.assertFalse(Which.in(Seasons.compute()), "clearing the override did not return to the calendar");
            }
            at(day(Year, 10, 29));
            SeasonSync.update(Server);
            Sent.clear();
            int Result = Server.getCommands().getDispatcher().execute("scp_unleashed season halloween off", Server.createCommandSourceStack().withSuppressedOutput());
            Helper.assertTrue(Result == 0 && Seasons.forced(Which) == SeasonMode.Off, "the season command did not force Halloween off");
            Helper.assertFalse(Which.in(Seasons.serverMask()), "the server still reports Halloween after the command");
            Helper.assertTrue(Sent.Broadcast.size() == 1 && Sent.Broadcast.get(0) == 0, "the command change was not broadcast once: " + Sent.Broadcast);
            Server.getCommands().getDispatcher().execute("scp_unleashed season halloween auto", Server.createCommandSourceStack().withSuppressedOutput());
            Helper.assertTrue(Seasons.forced(Which) == SeasonMode.Auto, "the season command did not clear the override");
            Server.getCommands().getDispatcher().execute("scp_unleashed season", Server.createCommandSourceStack().withSuppressedOutput());
        } catch (CommandSyntaxException Problem) {
            throw new IllegalStateException("the season command failed: " + Problem.getMessage(), Problem);
        } finally {
            restore(Server);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "season_sync")
    public static void loginAndDimensionChangeSendTheFlag(GameTestHelper Helper) {
        Season Which = Season.Halloween;
        MinecraftServer Server = Helper.getLevel().getServer();
        Recorder Sent = record();
        try {
            at(day(Year, 10, 30));
            Seasons.force(Which, SeasonMode.Auto);
            SeasonSync.update(Server);
            Sent.clear();
            ServerPlayer Joined = FakePlayerFactory.get(Helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes("season_test".getBytes(StandardCharsets.UTF_8)), "season_test"));
            int Expected = Seasons.serverMask();
            SeasonSync.loggedIn(new PlayerEvent.PlayerLoggedInEvent(Joined));
            Helper.assertTrue(Sent.Direct.size() == 1 && Sent.Receivers.get(0) == Joined && Sent.Direct.get(0) == Expected,
                    "a login did not send the season flag once: " + Sent.Direct);
            Helper.assertTrue(Sent.Broadcast.isEmpty(), "a login with nothing changed broadcast to everyone");
            if (configAuto()) {
                Helper.assertTrue(Which.in(Sent.Direct.get(0)), "the login flag missed Halloween on 30 October");
            }
            SeasonSync.changedDimension(new PlayerEvent.PlayerChangedDimensionEvent(Joined, Level.OVERWORLD, Level.NETHER));
            Helper.assertTrue(Sent.Direct.size() == 2 && Sent.Direct.get(1) == Expected, "a dimension change did not resend the flag: " + Sent.Direct);
            Sent.clear();
            at(day(Year, 11, 1));
            SeasonSync.loggedIn(new PlayerEvent.PlayerLoggedInEvent(Joined));
            if (configAuto()) {
                Helper.assertTrue(Sent.Broadcast.size() == 1 && !Which.in(Sent.Broadcast.get(0)) && Sent.Direct.isEmpty(),
                        "a login after an unnoticed rollover did not broadcast the new flag once: " + Sent.Broadcast + " " + Sent.Direct);
            }
        } finally {
            restore(Server);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "season_rollover")
    public static void rolloverResendsOnlyOnChange(GameTestHelper Helper) {
        Season Which = Season.Halloween;
        Helper.assertTrue(SeasonSync.due(0) && SeasonSync.due(SeasonSync.CheckInterval * 3), "the minute check does not fire on its interval");
        Helper.assertFalse(SeasonSync.due(SeasonSync.CheckInterval - 1) || SeasonSync.due(SeasonSync.CheckInterval / 2), "the minute check fires between intervals");
        ByteBuf Bytes = Unpooled.buffer();
        SeasonsPayload.PayloadCodec.encode(Bytes, new SeasonsPayload(Which.bit()));
        Helper.assertTrue(SeasonsPayload.PayloadCodec.decode(Bytes).Mask() == Which.bit() && Bytes.readableBytes() == 0, "the payload does not round trip");
        Bytes.release();
        MinecraftServer Server = Helper.getLevel().getServer();
        Recorder Sent = record();
        try {
            if (configAuto()) {
                at(day(Year, 10, 25));
                SeasonSync.update(Server);
                Sent.clear();
                Helper.assertFalse(SeasonSync.update(Server), "an unchanged day reported a change");
                Helper.assertTrue(Sent.Broadcast.isEmpty() && Sent.Direct.isEmpty(), "an unchanged day sent packets");
                at(day(Year, 10, 26));
                Helper.assertTrue(SeasonSync.update(Server), "the rollover into Halloween was missed");
                Helper.assertTrue(Sent.Broadcast.size() == 1 && Which.in(Sent.Broadcast.get(0)), "the rollover into Halloween was not broadcast once: " + Sent.Broadcast);
                Helper.assertTrue(Which.in(Seasons.serverMask()), "the server did not switch Halloween on");
                SeasonSync.update(Server);
                Helper.assertTrue(Sent.Broadcast.size() == 1, "the same day was broadcast twice");
                at(day(Year, 11, 1));
                SeasonSync.update(Server);
                Helper.assertTrue(Sent.Broadcast.size() == 2 && !Which.in(Sent.Broadcast.get(1)), "the rollover out of Halloween was not broadcast: " + Sent.Broadcast);
            }
        } finally {
            restore(Server);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "season_config")
    public static void configIsAServerFileDefaultingToAuto(GameTestHelper Helper) {
        ModConfig Found = ModConfigs.getFileMap().get(SeasonConfig.FileName);
        Helper.assertTrue(Found != null && Found.getType() == ModConfig.Type.SERVER, "the seasons config is not registered as a server config");
        Helper.assertTrue(Found.getSpec() == ScpConfigs.Server, "the seasons file holds the wrong spec");
        Helper.assertTrue(SeasonConfig.mode(Season.Halloween) == SeasonMode.Auto, "Halloween does not default to Auto");
        Helper.succeed();
    }

    private static JsonObject json(ResourceLocation Location) {
        String Path = "/assets/" + Location.getNamespace() + "/" + Location.getPath();
        try (InputStream Stream = SeasonGameTests.class.getResourceAsStream(Path)) {
            if (Stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception Problem) {
            return null;
        }
    }

    private static boolean hasBone(JsonObject Geo, String Name) {
        JsonArray Bones = Geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        for (JsonElement Bone : Bones) {
            if (Name.equals(Bone.getAsJsonObject().get("name").getAsString())) {
                return true;
            }
        }
        return false;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "season_scp173")
    public static void scp173PumpkinFollowsTheSyncedFlag(GameTestHelper Helper) {
        int Halloween = Season.Halloween.bit();
        Helper.assertTrue(SeasonalLooks.scp173Pumpkin(Halloween, false), "SCP-173 has no pumpkin while Halloween is synced");
        Helper.assertFalse(SeasonalLooks.scp173Pumpkin(0, false), "SCP-173 wears a pumpkin outside Halloween");
        Helper.assertFalse(SeasonalLooks.scp173Pumpkin(Halloween, true), "an invisible SCP-173 still shows its pumpkin");
        Helper.assertTrue(SeasonalLooks.emberGlow(0, 15, 0) == 0.0f, "the carved face glows in daylight");
        Helper.assertTrue(SeasonalLooks.emberGlow(14, 0, 0) == 0.0f, "the carved face glows under a torch");
        float Night = SeasonalLooks.emberGlow(0, 15, 11);
        Helper.assertTrue(Night > 0.5f && Night < 1.0f, "the night glow is not faint but visible: " + Night);
        Helper.assertTrue(SeasonalLooks.emberGlow(0, 0, 0) == 1.0f, "the glow is not full in the pitch dark");
        Helper.assertTrue(SeasonalLooks.emberShown(Night, false, false), "the night glow is hidden");
        Helper.assertFalse(SeasonalLooks.emberShown(Night, true, false), "the glow draws into the shader shadow pass");
        Helper.assertFalse(SeasonalLooks.emberShown(Night, false, true), "the glow draws on a petrified statue");
        Helper.assertFalse(SeasonalLooks.emberShown(0.0f, false, false), "a zero glow still draws");
        JsonObject Pumpkin = json(SeasonalLooks.PumpkinModel);
        Helper.assertTrue(Pumpkin != null && hasBone(Pumpkin, "pumpkin"), "the pumpkin model is missing");
        JsonObject Statue = json(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/scp173/scp173.geo.json"));
        Helper.assertTrue(Statue != null && hasBone(Statue, SeasonalLooks.HeadBone), "SCP-173 lost the head bone the pumpkin sits on");
        for (ResourceLocation Texture : new ResourceLocation[]{SeasonalLooks.PumpkinTexture, SeasonalLooks.PumpkinGlow}) {
            try (InputStream Stream = SeasonGameTests.class.getResourceAsStream("/assets/" + Texture.getNamespace() + "/" + Texture.getPath())) {
                Helper.assertTrue(Stream != null, "missing pumpkin texture " + Texture);
            } catch (Exception Problem) {
                Helper.fail("unreadable pumpkin texture " + Texture);
            }
        }
        Helper.succeed();
    }
}
