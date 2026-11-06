package net.rai.scp_unleashed.discord;

import java.util.Set;

public enum DiscordRaids {
    Off,
    Milestones,
    All;

    public static final Set<String> MilestoneEvents = Set.of("warning", "start", "defeated", "retreated", "timed_out", "stopped");

    public boolean allows(String Event) {
        return this == All || this == Milestones && MilestoneEvents.contains(Event);
    }
}
