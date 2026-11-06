package net.rai.scp_unleashed.arrival.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.arrival.ArrivalCards;
import net.rai.scp_unleashed.arrival.ArrivalSeen;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.intro.IntroRegistry;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.scp024.Scp024Stage;
import net.rai.scp_unleashed.scp087.Scp087Stairwell;
import net.rai.scp_unleashed.scp093.Scp093Wastes;
import net.rai.scp_unleashed.scp106.Scp106Pocket;
import net.rai.scp_unleashed.scp2521.Scp2521Registry;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp860.Scp860Registry;
import net.rai.scp_unleashed.scp902.Scp902Registry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ArrivalCardGameTests {
    public static final String Arena = GatewayGameTests.Arena;
    public static final String IntroOverlayClass = "net/rai/scp_unleashed/intro/client/IntroOverlay.class";
    public static final String SharedCard = "net/rai/scp_unleashed/arrival/client/ArrivalCard";
    public static final List<ResourceKey<Level>> Expected = List.of(Scp4666Registry.Dimension, Scp024Stage.Key, Scp093Wastes.Key, Scp860Registry.Forest,
            Scp3008Registry.Dimension, Scp106Pocket.Key, Scp087Stairwell.Key, RocketryRegistry.Moon, net.rai.scp_unleashed.scp001day.Scp001DayWorld.Key, net.rai.scp_unleashed.scp001gate.Scp001GateGarden.Key, net.rai.scp_unleashed.scp1730.Scp1730World.Key, net.rai.scp_unleashed.scp2317.Scp2317World.Key);
    public static final List<ResourceKey<Level>> Skipped = List.of(Scp902Registry.Wastes, Scp2521Registry.Void, IntroRegistry.Dimension);

    private ArrivalCardGameTests() {
    }

    private static FakePlayer visitor(GameTestHelper Helper, String Name) {
        return new FakePlayer(Helper.getLevel(), new GameProfile(UUID.randomUUID(), Name));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "arrival_card_dimensions")
    public static void listedDimensionsTrigger(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Helper.assertTrue(Set.copyOf(ArrivalCards.Listed).equals(Set.copyOf(Expected)) && ArrivalCards.Listed.size() == Expected.size(),
                    "the listed dimensions drifted from the features' own keys: " + ArrivalCards.Listed);
            Helper.assertTrue(Set.copyOf(ArrivalCards.Cutscenes).equals(Set.copyOf(Skipped)), "the cutscene dimensions drifted: " + ArrivalCards.Cutscenes);
            for (ResourceKey<Level> Each : Expected) {
                Helper.assertTrue(ArrivalCards.triggers(Level.OVERWORLD, Each, false, false), Each.location() + " does not trigger a card from the Overworld");
                Helper.assertTrue(ArrivalCards.triggers(Level.NETHER, Each, false, false), Each.location() + " does not trigger a card from the Nether");
                Helper.assertTrue(!ArrivalCards.triggers(Each, Each, false, false), Each.location() + " triggers a card without changing dimension");
                for (ResourceKey<Level> Other : Expected) {
                    Helper.assertTrue(Other.equals(Each) || ArrivalCards.triggers(Other, Each, false, false), Each.location() + " does not trigger coming from " + Other.location());
                }
            }
            for (ResourceKey<Level> Each : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
                Helper.assertTrue(!ArrivalCards.triggers(ArrivalCards.Moon, Each, false, false), "vanilla dimension " + Each.location() + " triggers a card");
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "arrival_card_excluded")
    public static void cutsceneDimensionsNeverTrigger(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            for (ResourceKey<Level> Each : Skipped) {
                Helper.assertTrue(!ArrivalCards.listed(Each), Each.location() + " is listed");
                Helper.assertTrue(!ArrivalCards.triggers(Level.OVERWORLD, Each, false, false), "entering " + Each.location() + " triggers a card");
                for (ResourceKey<Level> Target : Expected) {
                    Helper.assertTrue(!ArrivalCards.triggers(Each, Target, false, false), "returning from " + Each.location() + " to " + Target.location() + " triggers a card");
                }
            }
            FakePlayer User = visitor(Helper, "arrival_back");
            Helper.assertTrue(!ArrivalCards.changed(User, IntroRegistry.Dimension, RocketryRegistry.Moon), "landing on the Moon from the intro sent a card");
            Helper.assertTrue(!ArrivalCards.hasSeen(User, RocketryRegistry.Moon), "a suppressed card was still marked seen");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "arrival_card_cutscene")
    public static void cutscenesSuppressTheCard(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            for (ResourceKey<Level> Each : Expected) {
                Helper.assertTrue(!ArrivalCards.triggers(Level.OVERWORLD, Each, true, false), Each.location() + " triggers a card during a cutscene");
            }
            FakePlayer User = visitor(Helper, "arrival_calm");
            Helper.assertTrue(!ArrivalCards.cutscene(User), "a plain visitor counts as being in a cutscene");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "arrival_card_once")
    public static void firstArrivalOnly(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            FakePlayer User = visitor(Helper, "arrival_once");
            Helper.assertTrue(ArrivalCards.seen(User).empty(), "a new visitor already has places seen");
            Helper.assertTrue(ArrivalCards.changed(User, Level.OVERWORLD, Scp3008Registry.Dimension), "the first arrival in the store sent no card");
            Helper.assertTrue(ArrivalCards.hasSeen(User, Scp3008Registry.Dimension), "the store was not marked seen when the card was sent");
            Helper.assertTrue(!ArrivalCards.changed(User, Level.OVERWORLD, Scp3008Registry.Dimension), "the second arrival in the store sent a card");
            Helper.assertTrue(!ArrivalCards.changed(User, Scp087Stairwell.Key, Scp3008Registry.Dimension), "a later arrival from another place sent a card");
            Helper.assertTrue(!ArrivalCards.hasSeen(User, Scp087Stairwell.Key), "the stairwell was marked seen without arriving there");
            Helper.assertTrue(ArrivalCards.changed(User, Scp3008Registry.Dimension, RocketryRegistry.Moon), "the first Moon landing sent no card");
            Helper.assertTrue(ArrivalCards.changed(User, RocketryRegistry.Moon, Scp087Stairwell.Key), "the first stairwell visit sent no card");
            Helper.assertTrue(!ArrivalCards.changed(User, Level.OVERWORLD, RocketryRegistry.Moon), "the second Moon landing sent a card");
            Helper.assertTrue(ArrivalCards.seen(User).Places().size() == 3, "the seen set holds " + ArrivalCards.seen(User).Places());
            FakePlayer Other = visitor(Helper, "arrival_other");
            Helper.assertTrue(ArrivalCards.changed(Other, Level.OVERWORLD, Scp3008Registry.Dimension), "another player's first arrival was blocked by the first player");
            ArrivalCards.reset(User);
            Helper.assertTrue(ArrivalCards.changed(User, Level.OVERWORLD, RocketryRegistry.Moon), "the reset did not let the Moon card show again");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "arrival_card_persist")
    public static void seenSurvivesDeathAndRelog(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            FakePlayer User = visitor(Helper, "arrival_keep");
            ArrivalCards.changed(User, Level.OVERWORLD, Scp4666Registry.Dimension);
            ArrivalCards.changed(User, Level.OVERWORLD, Scp106Pocket.Key);
            FakePlayer Reborn = new FakePlayer(Helper.getLevel(), User.getGameProfile());
            Reborn.copyAttachmentsFrom(User, true);
            Helper.assertTrue(ArrivalCards.hasSeen(Reborn, Scp4666Registry.Dimension) && ArrivalCards.hasSeen(Reborn, Scp106Pocket.Key),
                    "dying lost the seen places: " + ArrivalCards.seen(Reborn).Places());
            Helper.assertTrue(!ArrivalCards.changed(Reborn, Level.OVERWORLD, Scp106Pocket.Key), "respawning and going back sent the card again");
            CompoundTag Saved = User.saveWithoutId(new CompoundTag());
            FakePlayer Returning = new FakePlayer(Helper.getLevel(), User.getGameProfile());
            Returning.load(Saved);
            ArrivalSeen Loaded = ArrivalCards.seen(Returning);
            Helper.assertTrue(Loaded.has(Scp4666Registry.Dimension.location()) && Loaded.has(Scp106Pocket.Key.location()) && Loaded.Places().size() == 2,
                    "relogging lost the seen places: " + Loaded.Places());
            Helper.assertTrue(!ArrivalCards.changed(Returning, Level.OVERWORLD, Scp4666Registry.Dimension), "relogging and going back sent the card again");
            FakePlayer Blank = visitor(Helper, "arrival_blank");
            CompoundTag Empty = Blank.saveWithoutId(new CompoundTag());
            Helper.assertTrue(!Empty.getCompound("neoforge:attachments").contains("scp_unleashed:arrival_seen"), "an empty seen set was written to the save");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "arrival_card_lang")
    public static void everyCardHasText(GameTestHelper Helper) {
        Language Words = Language.getInstance();
        for (ResourceKey<Level> Each : ArrivalCards.Listed) {
            String Title = ArrivalCards.titleKey(Each);
            String Subtitle = ArrivalCards.subtitleKey(Each);
            Helper.assertTrue(Words.has(Title) && Words.has(Subtitle), "missing card text for " + Each.location());
            String Shown = Words.getOrDefault(Title);
            Helper.assertTrue(Shown.equals(Shown.toUpperCase()) && Shown.length() <= 24, "the title for " + Each.location() + " is not a short capitalised line: " + Shown);
            Helper.assertTrue(Words.getOrDefault(Subtitle).length() <= 48, "the subtitle for " + Each.location() + " is too long");
        }
        Helper.assertTrue(Words.has("config.scp_unleashed.location_titles") && Words.has("config.scp_unleashed.location_titles.tooltip"), "missing the config text");
        Helper.assertTrue(Words.has("commands.scp_unleashed.titles.reset") && Words.has("commands.scp_unleashed.titles.reset_many"), "missing the command text");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "arrival_card_intro")
    public static void introStillUsesTheSharedCard(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            String Bytes = classText(IntroOverlayClass);
            Helper.assertTrue(Bytes != null, "could not read the intro overlay class");
            Helper.assertTrue(Bytes.contains(SharedCard), "the intro overlay no longer draws through the shared arrival card");
            for (int Step = 0; Step <= 260; Step++) {
                float Ticks = Step * 0.5f;
                float In = Mth.clamp((Ticks - 10.0f) / 16.0f, 0.0f, 1.0f);
                float Out = Mth.clamp((120 - Ticks) / 24.0f, 0.0f, 1.0f);
                Helper.assertTrue(Math.abs(ArrivalCards.share(Ticks, 120) - Math.min(In, Out)) < 1.0E-6f, "the card fade changed at tick " + Ticks);
                Helper.assertTrue(Math.abs(ArrivalCards.ruleShare(Ticks) - Mth.clamp((Ticks - 16.0f) / 20.0f, 0.0f, 1.0f)) < 1.0E-6f, "the card rule changed at tick " + Ticks);
            }
            Helper.assertTrue(ArrivalCards.Length == 120 && ArrivalCards.TitleScale == 2.0f && ArrivalCards.TitleRise == -8 && ArrivalCards.RuleOffset == 12
                    && ArrivalCards.SubtitleOffset == 18 && ArrivalCards.RulePad == 16, "the card layout changed from the intro's");
            Helper.succeed();
        });
    }

    private static String classText(String Path) {
        try (InputStream Found = open(Path)) {
            return Found == null ? null : new String(Found.readAllBytes(), StandardCharsets.ISO_8859_1);
        } catch (IOException Problem) {
            return null;
        }
    }

    private static InputStream open(String Path) {
        InputStream Found = ArrivalCardGameTests.class.getClassLoader().getResourceAsStream(Path);
        return Found != null ? Found : ArrivalCardGameTests.class.getResourceAsStream("/" + Path);
    }
}
