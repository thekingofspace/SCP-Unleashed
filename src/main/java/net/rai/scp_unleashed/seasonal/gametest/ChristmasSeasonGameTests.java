package net.rai.scp_unleashed.seasonal.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.seasonal.Season;
import net.rai.scp_unleashed.seasonal.SeasonConfig;
import net.rai.scp_unleashed.seasonal.SeasonMode;
import net.rai.scp_unleashed.seasonal.Seasons;

import java.time.LocalDate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ChristmasSeasonGameTests {
    public static final String Arena = "scp420j_arena";

    private ChristmasSeasonGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "season_christmas_dates")
    public static void seasonChristmasDates(GameTestHelper Helper) {
        Season Which = Season.Christmas;
        for (int Year : new int[]{2024, 2025, 2026, 2027}) {
            Helper.assertFalse(Which.covers(LocalDate.of(Year, 12, 19)), "19 December " + Year + " counted as Christmas");
            Helper.assertTrue(Which.covers(LocalDate.of(Year, 12, 20)), "20 December " + Year + " is not Christmas");
            Helper.assertTrue(Which.covers(LocalDate.of(Year, 12, 25)), "25 December " + Year + " is not Christmas");
            Helper.assertTrue(Which.covers(LocalDate.of(Year, 12, 26)), "26 December " + Year + " is not Christmas");
            Helper.assertFalse(Which.covers(LocalDate.of(Year, 12, 27)), "27 December " + Year + " counted as Christmas");
            Helper.assertFalse(Which.covers(LocalDate.of(Year, 10, 31)), "Halloween " + Year + " counted as Christmas");
        }
        Helper.assertFalse(Season.Halloween.covers(LocalDate.of(2026, 12, 24)), "Christmas Eve counted as Halloween");
        Helper.assertTrue(Which.bit() != Season.Halloween.bit(), "Christmas shares Halloween's flag");
        Helper.assertTrue(Season.byKey("christmas").orElse(null) == Which, "the christmas key does not resolve");
        try {
            Seasons.useClock(() -> LocalDate.of(2026, 12, 19));
            Helper.assertFalse(Which.in(Seasons.compute()), "the injected clock on 19 December gave Christmas");
            Seasons.useClock(() -> LocalDate.of(2026, 12, 20));
            Helper.assertTrue(Which.in(Seasons.compute()), "the injected clock on 20 December missed Christmas");
            Helper.assertFalse(Season.Halloween.in(Seasons.compute()), "the injected clock on 20 December gave Halloween");
            Seasons.useClock(() -> LocalDate.of(2026, 12, 26));
            Helper.assertTrue(Which.in(Seasons.compute()), "the injected clock on 26 December missed Christmas");
            Seasons.useClock(() -> LocalDate.of(2026, 12, 27));
            Helper.assertFalse(Which.in(Seasons.compute()), "the injected clock on 27 December gave Christmas");
        } finally {
            Seasons.resetClock();
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "season_christmas_override")
    public static void seasonChristmasOverride(GameTestHelper Helper) {
        Season Which = Season.Christmas;
        Helper.assertTrue(SeasonConfig.mode(Which) == SeasonMode.Auto, "Christmas does not default to Auto");
        Helper.assertTrue(Seasons.decide(Which, LocalDate.of(2026, 7, 1), SeasonMode.On), "On did not force Christmas in July");
        Helper.assertFalse(Seasons.decide(Which, LocalDate.of(2026, 12, 25), SeasonMode.Off), "Off did not win over Christmas Day");
        try {
            Seasons.useClock(() -> LocalDate.of(2026, 7, 1));
            Seasons.force(Which, SeasonMode.On);
            Seasons.refresh();
            Helper.assertTrue(Seasons.christmas(), "forcing Christmas on did not reach the server");
            Helper.assertFalse(Seasons.halloween(), "forcing Christmas on also started Halloween");
            Seasons.force(Which, SeasonMode.Off);
            Seasons.useClock(() -> LocalDate.of(2026, 12, 24));
            Seasons.refresh();
            Helper.assertFalse(Seasons.christmas(), "forcing Christmas off did not hold on Christmas Eve");
        } finally {
            Seasons.force(Which, SeasonMode.Auto);
            Seasons.resetClock();
            Seasons.refresh();
        }
        Helper.succeed();
    }
}
