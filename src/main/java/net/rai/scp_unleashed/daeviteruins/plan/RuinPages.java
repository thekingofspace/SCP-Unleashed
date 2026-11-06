package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RuinPages {
    public static final int First = 1;
    public static final int Last = 31;

    public record Entry(int Page, int Tier, String Key, String Builder, Consumer<RuinCanvas> Build, RuinDecay.Wear Wear) {
    }

    private static final List<Entry> Entries = new ArrayList<>();

    static {
        add(1, "first_flame", "shrine", RuinOrigins::shrine, null);
        add(2, "oath_of_ash", "stone_ring", RuinOrigins::stoneRing, null);
        add(3, "red_banner_raised", "cairn", RuinOrigins::cairn, null);
        add(4, "taming_of_the_plains", "obelisk", RuinOrigins::obelisk, null);
        add(5, "first_overseer", "terrace", RuinOrigins::platform, null);
        add(6, "burning_of_the_river_towns", "tower", RuinConquests::watchtower, null);
        add(7, "spear_hosts_march", "camp", RuinConquests::warCamp, null);
        add(8, "breaking_of_the_hill_clans", "hall", RuinConquests::barracks, null);
        add(9, "bridge_of_nine_days", "bridge", RuinConquests::bridge, null);
        add(10, "armory_of_brass", "vault", RuinConquests::armory, null);
        add(11, "field_of_broken_spears", "battlefield", RuinConquests::battlefield, null);
        add(12, "ash_road", "road", RuinConquests::waystation, null);
        add(13, "gate_of_seven_kings", "gate", RuinEmpire::kingsGate, null);
        add(14, "vault_of_tribute", "vault", RuinEmpire::treasury, null);
        add(15, "tower_of_whispers", "round_tower", RuinEmpire::whisperTower, null);
        add(16, "colossus_of_victory", "colossus", RuinEmpire::colossus, null);
        add(17, "siege_of_the_white_city", "siege", RuinEmpire::siege, null);
        add(18, "hall_of_the_knights", "hall", RuinEmpire::knightsHall, null);
        add(19, "red_scriptorium", "hall", RuinEmpire::scriptorium, new RuinDecay.Wear(0.45, 0.55, 0.3, 0.25, 0.35, 0.45, 3));
        add(20, "eternal_braziers", "terrace", RuinEmpire::braziers, new RuinDecay.Wear(0.45, 0.3, 0.16, 0.2, 0.4, 0.4, 3));
        add(21, "stair_of_heaven", "ziggurat", RuinPower::ziggurat, new RuinDecay.Wear(0.4, 0.3, 0.16, 0.2, 0.35, 0.4, 3));
        add(22, "sunken_arena", "arena", RuinPower::arena, null);
        add(23, "observatory_of_omens", "round_tower", RuinEmpire::omenObservatory, null);
        add(24, "great_hall_of_conquest", "hall", RuinEmpire::greatHall, null);
        add(25, "palace_of_brass", "palace", RuinPower::palace, null);
        add(26, "six_armed_idol", "idol", RuinPower::idol, new RuinDecay.Wear(0.4, 0.3, 0.12, 0.15, 0.3, 0.4, 3));
        add(27, "throne_of_ash", "hall", RuinEmpire::throneRoom, new RuinDecay.Wear(0.4, 0.55, 0.28, 0.22, 0.25, 0.45, 3));
        add(28, "crown_forge", "forge", RuinGodKing::crownForge, null);
        add(29, "war_altar", "altar", RuinGodKing::warAltar, new RuinDecay.Wear(0.35, 0.42, 0.14, 0.15, 0.22, 0.4, 4));
        add(30, "gate_of_the_god", "gate", RuinGodKing::godGate, null);
        add(31, "last_procession", "avenue", RuinGodKing::procession, null);
    }

    private RuinPages() {
    }

    private static void add(int Page, String Key, String Builder, Consumer<RuinCanvas> Build, RuinDecay.Wear Wear) {
        Entries.add(new Entry(Page, tierOf(Page), Key, Builder, Build, Wear != null ? Wear : wearFor(tierOf(Page))));
    }

    public static int tierOf(int Page) {
        if (Page <= 5) {
            return 1;
        }
        if (Page <= 12) {
            return 2;
        }
        if (Page <= 20) {
            return 3;
        }
        if (Page <= 27) {
            return 4;
        }
        return 5;
    }

    public static RuinDecay.Wear wearFor(int Tier) {
        return switch (Tier) {
            case 1 -> new RuinDecay.Wear(0.35, 0.22, 0.15, 0.15, 0.55, 0.25, 2);
            case 2 -> new RuinDecay.Wear(0.45, 0.34, 0.3, 0.25, 0.45, 0.4, 2);
            case 3 -> new RuinDecay.Wear(0.45, 0.32, 0.3, 0.25, 0.4, 0.45, 3);
            case 4 -> new RuinDecay.Wear(0.4, 0.3, 0.28, 0.22, 0.35, 0.45, 3);
            default -> new RuinDecay.Wear(0.35, 0.4, 0.22, 0.18, 0.25, 0.4, 4);
        };
    }

    public static Entry entry(int Page) {
        for (Entry Found : Entries) {
            if (Found.Page() == Page) {
                return Found;
            }
        }
        return null;
    }

    public static List<Entry> entries() {
        return Entries;
    }

    public static boolean known(int Page) {
        return entry(Page) != null;
    }

    public static long seedFor(long WorldSeed, int Page) {
        return RuinRandom.mix(WorldSeed * 31L + Page * 0x5DEECE66DL + 0x140L);
    }

    public static int minDistance(int Page) {
        return 150 + (Math.max(1, Page) - 1) * 10;
    }

    public static int maxDistance(int Page) {
        return 300 + (int) Math.round((Math.max(1, Page) - 1) * 500.0 / 30.0);
    }

    public static RuinPlan build(long WorldSeed, int Page) {
        Entry Found = entry(Page);
        if (Found == null) {
            return null;
        }
        RuinPlan Plan = new RuinPlan(Found.Key(), Page, Found.Tier());
        RuinRandom Random = new RuinRandom(seedFor(WorldSeed, Page));
        RuinCanvas Canvas = new RuinCanvas(Plan, Random);
        Found.Build().accept(Canvas);
        RuinDecay.apply(Canvas, Found.Wear());
        if (Found.Tier() >= 5) {
            crown(Plan);
        }
        RuinDanger.prune(Plan);
        Plan.recomputeBounds();
        return Plan;
    }

    private static void crown(RuinPlan Plan) {
        List<int[]> Centre = Plan.marks(RuinPlan.Centre);
        int Cx = Centre.isEmpty() ? 0 : Centre.get(0)[0];
        int Cz = Centre.isEmpty() ? 0 : Centre.get(0)[2];
        int[] Best = null;
        for (int[] Spot : Plan.marks(RuinPlan.Guardian)) {
            if (Best == null || Math.abs(Spot[0] - Cx) + Math.abs(Spot[2] - Cz) < Math.abs(Best[0] - Cx) + Math.abs(Best[2] - Cz)) {
                Best = Spot;
            }
        }
        if (Best != null) {
            Best[4] = RuinDanger.Evoker;
        }
    }
}
