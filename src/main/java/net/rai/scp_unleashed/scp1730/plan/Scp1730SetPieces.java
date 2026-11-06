package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Scp1730SetPieces {
    public static final String Generic = "generic";
    public static final String PlagueDoctor = "plague_doctor";
    public static final String Hive = "hive";
    public static final String Incinerator = "incinerator";
    public static final String BeastPen = "beast_pen";
    public static final String StatueCell = "statue_cell";
    public static final String DarkCell = "dark_cell";
    public static final Set<String> Scenes = Set.of(Generic, PlagueDoctor, Hive, Incinerator, BeastPen, StatueCell, DarkCell);
    public static final Set<String> Biological = Set.of("scp008_zombie", "scp049", "scp049_2", "scp372", "scp548_ice_spider", "scp075_snail", "scp019_drone",
            "theodore_amalgamation", "scp093_torso", "scp354_crawler", "scp354_brute", "scp354_charger", "scp354_lion", "scp354_lizard", "scp354_leaper",
            "scp939", "scp427_1", "scp323", "scp058_heart", "scp031", "scp031_polyp", "scp439_brute", "scp439_worker", "scp337", "scp407_titan",
            "scp469_angel");

    public record Group(String Id, int Pack) {
    }

    public record Piece(String Scene, List<Group> Groups) {
        public boolean biological() {
            for (Group Each : Groups) {
                if (Scp1730SetPieces.biological(Each.Id())) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final List<List<Piece>> Pieces = List.of(
            List.of(generic("scp008_zombie", 3), generic("scp049_2", 2), generic("scp538_shadow_spider", 4), generic("scp066", 1)),
            List.of(generic("scp548_ice_spider", 3), generic("theodore_amalgamation", 1), new Piece(Generic, List.of(new Group("scp093_torso", 1), new Group("scp008_zombie", 2))),
                    new Piece(Hive, List.of(new Group("scp439_brute", 2), new Group("scp439_worker", 3))), new Piece(StatueCell, List.of(new Group("scp173", 1)))),
            List.of(generic("scp173", 1), generic("scp939", 3), generic("scp096_shy_guy", 1), generic("scp352", 1),
                    new Piece(PlagueDoctor, List.of(new Group("scp049", 1), new Group("scp049_2", 2))), new Piece(Incinerator, List.of(new Group("scp457", 1)))),
            List.of(generic("scp939", 2), generic("scp058_heart", 1), new Piece(BeastPen, List.of(new Group("scp354_leaper", 1), new Group("scp354_lizard", 1))),
                    new Piece(DarkCell, List.of(new Group("scp087_hunter", 1), new Group("scp538_shadow_spider", 3)))));
    public static final String[] Featured = {null, null, PlagueDoctor, null};
    public static final Piece ShyStandIn = generic("scp427_1", 1);

    private Scp1730SetPieces() {
    }

    private static Piece generic(String Id, int Pack) {
        return new Piece(Generic, List.of(new Group(Id, Pack)));
    }

    public static boolean biological(String Id) {
        return Biological.contains(Id);
    }

    public static int clampZone(int Zone) {
        return Math.max(0, Math.min(Pieces.size() - 1, Zone));
    }

    public static int count(int Zone) {
        return Pieces.get(clampZone(Zone)).size();
    }

    public static int featured(int Zone) {
        String Scene = Featured[clampZone(Zone)];
        if (Scene == null) {
            return -1;
        }
        List<Piece> Options = Pieces.get(clampZone(Zone));
        for (int Number = 0; Number < Options.size(); Number++) {
            if (Options.get(Number).Scene().equals(Scene)) {
                return Number;
            }
        }
        return -1;
    }

    public static Piece piece(int Zone, int Number, long Seed) {
        List<Piece> Options = Pieces.get(clampZone(Zone));
        Piece Picked = Options.get(Math.floorMod(Number, Options.size()));
        if (clampZone(Zone) == 2 && Picked.Groups().getFirst().Id().equals("scp096_shy_guy") && Math.floorMod(Seed >> 7, 4) != 0) {
            return ShyStandIn;
        }
        return Picked;
    }

    public static String scene(int Zone, int Number, long Seed) {
        return piece(Zone, Number, Seed).Scene();
    }

    public static boolean biological(int Zone, int Number, long Seed) {
        return piece(Zone, Number, Seed).biological();
    }

    public static boolean fits(int Zone, int Number, long Seed, Boolean Wanted) {
        return Wanted == null || biological(Zone, Number, Seed) == Wanted;
    }

    public static List<Integer> others(int Zone, long Seed, Boolean Wanted) {
        List<Integer> Found = new ArrayList<>();
        int Skip = featured(Zone);
        for (int Number = 0; Number < count(Zone); Number++) {
            if (Number != Skip && fits(Zone, Number, Seed, Wanted)) {
                Found.add(Number);
            }
        }
        return Found;
    }

    public static int choose(int Zone, int Index, long Seed) {
        return choose(Zone, Index, Seed, null);
    }

    public static int choose(int Zone, int Index, long Seed, Boolean Wanted) {
        int Star = featured(Zone);
        boolean Fits = Star >= 0 && fits(Zone, Star, Seed, Wanted);
        if (Fits && Index == 0) {
            return Star;
        }
        List<Integer> Options = others(Zone, Seed, Wanted);
        if (Options.isEmpty()) {
            return Fits ? Star : Math.floorMod(Index + Seed, count(Zone));
        }
        return Options.get(Math.floorMod((Fits ? Index - 1 : Index) + Seed, Options.size()));
    }

    public static final class Picker {
        private final long Seed;
        private final int[][] Used = new int[4][3];
        private final boolean[] StarPlaced = new boolean[4];

        public Picker(long Seed) {
            this.Seed = Seed;
        }

        public int next(int Zone) {
            return next(Zone, null);
        }

        public int next(int Zone, Boolean Wanted) {
            int Clamped = clampZone(Zone);
            int Star = featured(Clamped);
            if (Star >= 0 && !StarPlaced[Clamped] && fits(Clamped, Star, Seed, Wanted)) {
                StarPlaced[Clamped] = true;
                return Star;
            }
            List<Integer> Options = others(Clamped, Seed, Wanted);
            int Slot = Wanted == null ? 0 : Wanted ? 1 : 2;
            int Index = Used[Clamped][Slot]++;
            if (Options.isEmpty()) {
                return Star >= 0 && fits(Clamped, Star, Seed, Wanted) ? Star : Math.floorMod(Index + Seed, count(Clamped));
            }
            return Options.get(Math.floorMod(Index + Seed, Options.size()));
        }

        public boolean placed(int Zone) {
            return StarPlaced[clampZone(Zone)];
        }
    }
}
