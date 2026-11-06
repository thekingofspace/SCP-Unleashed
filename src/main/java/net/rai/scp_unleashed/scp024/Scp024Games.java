package net.rai.scp_unleashed.scp024;

import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.scp024.games.AnvilRainGame;
import net.rai.scp_unleashed.scp024.games.ArrowCorridorGame;
import net.rai.scp_unleashed.scp024.games.ChampionBoutGame;
import net.rai.scp_unleashed.scp024.games.CrumblingFloorGame;
import net.rai.scp_unleashed.scp024.games.DoorRouletteGame;
import net.rai.scp_unleashed.scp024.games.FightingPitGame;
import net.rai.scp_unleashed.scp024.games.HotFloorGame;
import net.rai.scp_unleashed.scp024.games.HotPotatoGame;
import net.rai.scp_unleashed.scp024.games.IceBridgeGame;
import net.rai.scp_unleashed.scp024.games.LightsOutGame;
import net.rai.scp_unleashed.scp024.games.MazeRunGame;
import net.rai.scp_unleashed.scp024.games.MemoryTilesGame;
import net.rai.scp_unleashed.scp024.games.MinefieldGame;
import net.rai.scp_unleashed.scp024.games.ParkourTrialGame;
import net.rai.scp_unleashed.scp024.games.PillarHopGame;
import net.rai.scp_unleashed.scp024.games.QuizShowGame;
import net.rai.scp_unleashed.scp024.games.RisingLavaGame;
import net.rai.scp_unleashed.scp024.games.ShootingGalleryGame;
import net.rai.scp_unleashed.scp024.games.ShrinkingArenaGame;
import net.rai.scp_unleashed.scp024.games.SilverfishEscapeGame;
import net.rai.scp_unleashed.scp024.games.SumoRingGame;
import net.rai.scp_unleashed.scp024.games.TargetPracticeGame;
import net.rai.scp_unleashed.scp024.games.TntDodgeGame;
import net.rai.scp_unleashed.scp024.games.WheelOfMisfortuneGame;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class Scp024Games {
    private static final Map<String, Supplier<Scp024Game>> Factories = new LinkedHashMap<>();
    private static final List<String> Order = new ArrayList<>();

    private Scp024Games() {
    }

    public static void register(String Id, Supplier<Scp024Game> Factory) {
        if (Factories.putIfAbsent(Id, Factory) != null) {
            throw new IllegalStateException("SCP-024 game '" + Id + "' is already registered");
        }
        Order.add(Id);
    }

    public static void bootstrap() {
        if (!Factories.isEmpty()) {
            return;
        }
        register("pillar_hop", PillarHopGame::new);
        register("shooting_gallery", ShootingGalleryGame::new);
        register("fighting_pit", FightingPitGame::new);
        register("rising_lava", RisingLavaGame::new);
        register("crumbling_floor", CrumblingFloorGame::new);
        register("anvil_rain", AnvilRainGame::new);
        register("maze_run", MazeRunGame::new);
        register("memory_tiles", MemoryTilesGame::new);
        register("quiz_show", QuizShowGame::new);
        register("door_roulette", DoorRouletteGame::new);
        register("tnt_dodge", TntDodgeGame::new);
        register("arrow_corridor", ArrowCorridorGame::new);
        register("minefield", MinefieldGame::new);
        register("ice_bridge", IceBridgeGame::new);
        register("sumo_ring", SumoRingGame::new);
        register("champion_bout", ChampionBoutGame::new);
        register("silverfish_escape", SilverfishEscapeGame::new);
        register("lights_out", LightsOutGame::new);
        register("hot_floor", HotFloorGame::new);
        register("shrinking_arena", ShrinkingArenaGame::new);
        register("hot_potato", HotPotatoGame::new);
        register("parkour_trial", ParkourTrialGame::new);
        register("target_practice", TargetPracticeGame::new);
        register("wheel_of_misfortune", WheelOfMisfortuneGame::new);
    }

    public static Collection<String> ids() {
        return List.copyOf(Order);
    }

    public static boolean exists(String Id) {
        return Factories.containsKey(Id);
    }

    public static int index(String Id) {
        return Math.max(0, Order.indexOf(Id));
    }

    @Nullable
    public static Scp024Game create(String Id) {
        Supplier<Scp024Game> Factory = Factories.get(Id);
        return Factory == null ? null : Factory.get();
    }

    public static String pick(Collection<String> Played, RandomSource Random) {
        List<String> Fresh = new ArrayList<>();
        for (String Id : Order) {
            if (!Played.contains(Id)) {
                Fresh.add(Id);
            }
        }
        List<String> Pool = Fresh.isEmpty() ? Order : Fresh;
        return Pool.get(Random.nextInt(Pool.size()));
    }
}
