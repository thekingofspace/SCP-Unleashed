package net.rai.scp_unleashed.discord;

import java.util.EnumSet;
import java.util.Set;

public record DiscordFilters(Set<DiscordToggle> Muted, DiscordReads Reads, int RereadMinutes, DiscordContainment Containment, DiscordRaids Raids,
                             DiscordWarhead Warhead) {
    public static final int DefaultRereadMinutes = 10;
    public static final int MaxRereadMinutes = 1440;

    public DiscordFilters {
        Muted = Muted == null ? Set.of() : Set.copyOf(Muted);
        Reads = Reads == null ? DiscordReads.EveryRead : Reads;
        RereadMinutes = Math.max(0, Math.min(MaxRereadMinutes, RereadMinutes));
        Containment = Containment == null ? DiscordContainment.All : Containment;
        Raids = Raids == null ? DiscordRaids.All : Raids;
        Warhead = Warhead == null ? DiscordWarhead.All : Warhead;
    }

    public static DiscordFilters standard() {
        return new DiscordFilters(Set.of(), DiscordReads.EveryRead, DefaultRereadMinutes, DiscordContainment.All, DiscordRaids.All, DiscordWarhead.All);
    }

    public boolean allows(DiscordToggle Toggle) {
        return !Muted.contains(Toggle);
    }

    public long rereadGapMillis() {
        return RereadMinutes * 60000L;
    }

    public DiscordFilters withToggle(DiscordToggle Toggle, boolean On) {
        Set<DiscordToggle> Changed = EnumSet.noneOf(DiscordToggle.class);
        Changed.addAll(Muted);
        if (On) {
            Changed.remove(Toggle);
        } else {
            Changed.add(Toggle);
        }
        return new DiscordFilters(Changed, Reads, RereadMinutes, Containment, Raids, Warhead);
    }

    public DiscordFilters withReads(DiscordReads Value) {
        return new DiscordFilters(Muted, Value, RereadMinutes, Containment, Raids, Warhead);
    }

    public DiscordFilters withRereadMinutes(int Value) {
        return new DiscordFilters(Muted, Reads, Value, Containment, Raids, Warhead);
    }

    public DiscordFilters withContainment(DiscordContainment Value) {
        return new DiscordFilters(Muted, Reads, RereadMinutes, Value, Raids, Warhead);
    }

    public DiscordFilters withRaids(DiscordRaids Value) {
        return new DiscordFilters(Muted, Reads, RereadMinutes, Containment, Value, Warhead);
    }

    public DiscordFilters withWarhead(DiscordWarhead Value) {
        return new DiscordFilters(Muted, Reads, RereadMinutes, Containment, Raids, Value);
    }
}
