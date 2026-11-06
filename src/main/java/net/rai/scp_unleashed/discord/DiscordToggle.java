package net.rai.scp_unleashed.discord;

import java.util.ArrayList;
import java.util.List;

public enum DiscordToggle {
    SealedFiles(DiscordCategory.Documents, "sealedFiles", "sealed_files"),
    Arrivals(DiscordCategory.Anomalies, "arrivals", "arrivals"),
    Rifts(DiscordCategory.Anomalies, "rifts", "rifts"),
    DaeviteGod(DiscordCategory.Anomalies, "daeviteGod", "daevite_god"),
    MtfSquads(DiscordCategory.Security, "mtfSquads", "mtf_squads"),
    Meltdowns(DiscordCategory.Security, "meltdowns", "meltdowns"),
    PlayerMessages(DiscordCategory.Computers, "playerMessages", "player_messages"),
    ScheduledMessages(DiscordCategory.Computers, "scheduledMessages", "scheduled_messages"),
    AnomalousMessages(DiscordCategory.Computers, "anomalousMessages", "anomalous_messages"),
    Appearances(DiscordCategory.Strangers, "appearances", "appearances"),
    Deliveries(DiscordCategory.Strangers, "deliveries", "deliveries"),
    Departures(DiscordCategory.Strangers, "departures", "departures"),
    ScarletRituals(DiscordCategory.Apocalypse, "scarletRituals", "scarlet_rituals");

    private final DiscordCategory Category;
    private final String Key;
    private final String LangKey;

    DiscordToggle(DiscordCategory Category, String Key, String LangKey) {
        this.Category = Category;
        this.Key = Key;
        this.LangKey = LangKey;
    }

    public DiscordCategory getCategory() {
        return Category;
    }

    public String getKey() {
        return Key;
    }

    public String getLangKey() {
        return LangKey;
    }

    public static List<DiscordToggle> of(DiscordCategory Category) {
        List<DiscordToggle> Found = new ArrayList<>();
        for (DiscordToggle Toggle : values()) {
            if (Toggle.Category == Category) {
                Found.add(Toggle);
            }
        }
        return Found;
    }
}
