package net.rai.scp_unleashed.scp001scarlet.court;

import java.util.List;
import java.util.Map;

public final class CourtWaves {
    public enum Source { Rim, PillarBase, PillarTop, Near }

    public record Entry(String What, int Count, Source From) {
    }

    public static final String Cultist = "cultist";
    public static final String Leader = "cultist_leader";
    public static final Map<String, List<Entry>> Mixes = Map.ofEntries(
            Map.entry("opening", List.of(new Entry("Thrall", 4, Source.Rim), new Entry("ChainHound", 2, Source.PillarBase), new Entry("AntlerStalker", 2, Source.PillarTop),
                    new Entry(Cultist, 2, Source.Rim))),
            Map.entry("horde", List.of(new Entry("Thrall", 5, Source.Rim), new Entry("Gnawer", 6, Source.Rim), new Entry(Cultist, 2, Source.PillarBase))),
            Map.entry("vanguard", List.of(new Entry("CrimsonKnight", 2, Source.PillarBase), new Entry("BellRinger", 1, Source.Rim), new Entry("FlayedHerald", 1, Source.Rim),
                    new Entry("Thrall", 3, Source.PillarBase))),
            Map.entry("swarm", List.of(new Entry("Eyeling", 6, Source.Near), new Entry("Gnawer", 8, Source.Rim))),
            Map.entry("flankers", List.of(new Entry("AntlerStalker", 3, Source.PillarTop), new Entry("ChainHound", 3, Source.Rim))),
            Map.entry("procession", List.of(new Entry("BrideHusk", 2, Source.PillarBase), new Entry("Thrall", 3, Source.PillarBase), new Entry(Leader, 1, Source.Rim),
                    new Entry(Cultist, 2, Source.Rim))),
            Map.entry("wraiths", List.of(new Entry("AshWraith", 3, Source.Near), new Entry("FlayedHerald", 1, Source.Rim), new Entry("ThornCrawler", 3, Source.PillarBase))),
            Map.entry("siege", List.of(new Entry("ThroneGolem", 1, Source.PillarBase), new Entry("BellRinger", 1, Source.Rim), new Entry("Thrall", 4, Source.Rim))),
            Map.entry("moths", List.of(new Entry("StarMoth", 6, Source.Near), new Entry("Eyeling", 2, Source.Near))),
            Map.entry("light", List.of(new Entry("Thrall", 3, Source.Rim), new Entry("Gnawer", 4, Source.Rim))),
            Map.entry("king_heralds", List.of(new Entry("FlayedHerald", 4, Source.PillarBase), new Entry("BellRinger", 1, Source.Rim))),
            Map.entry("king_thrones", List.of(new Entry("ThroneGolem", 2, Source.Near), new Entry("Thrall", 4, Source.Near))),
            Map.entry("king_procession", List.of(new Entry("BrideHusk", 2, Source.PillarBase), new Entry("CrimsonKnight", 2, Source.PillarBase),
                    new Entry("AntlerStalker", 3, Source.PillarTop), new Entry("Eyeling", 6, Source.Near))));
    public static final List<List<String>> DaughterRotations = List.of(
            List.of("horde", "flankers", "wraiths"),
            List.of("procession", "vanguard", "horde"),
            List.of("swarm", "wraiths", "flankers"),
            List.of("siege", "vanguard", "moths"));
    public static final List<String> KingRotation = List.of("horde", "vanguard", "flankers", "moths", "procession", "wraiths", "siege", "swarm");

    private CourtWaves() {
    }

    public static String forDaughter(int Daughter, int Turn) {
        List<String> Rotation = DaughterRotations.get(Math.max(0, Math.min(DaughterRotations.size() - 1, Daughter)));
        return Rotation.get(Math.floorMod(Turn, Rotation.size()));
    }

    public static String forKing(int Turn) {
        return KingRotation.get(Math.floorMod(Turn, KingRotation.size()));
    }

    public static String forKingPhase(int Phase) {
        return Phase <= 1 ? "king_heralds" : Phase == 2 ? "king_thrones" : "king_procession";
    }
}
