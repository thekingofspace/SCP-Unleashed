package net.rai.scp_unleashed.scp001scarlet.court;

import java.util.Map;

public final class CourtGaits {
    public static final Map<String, Double> Strides = Map.ofEntries(
            Map.entry("scarlet_antler_stalker", 0.8333),
            Map.entry("scarlet_bell_ringer", 0.5040),
            Map.entry("scarlet_chain_hound", 0.7056),
            Map.entry("scarlet_crimson_knight", 0.8065),
            Map.entry("scarlet_gnawer", 0.3250),
            Map.entry("scarlet_thorn_crawler", 0.5208),
            Map.entry("scarlet_thrall", 0.7056),
            Map.entry("scarlet_throne_golem", 0.8654));

    private CourtGaits() {
    }

    public static double stride(String Name) {
        return Strides.getOrDefault(Name, 0.0);
    }
}
