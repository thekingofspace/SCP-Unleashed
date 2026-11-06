package net.rai.scp_unleashed.discord;

import java.util.Locale;
import java.util.Optional;

public enum DiscordCategory {
    Players("players", 0x3BA55C, DiscordCoordinates.Default),
    Documents("documents", 0x5865F2, DiscordCoordinates.Default),
    Anomalies("anomalies", 0xC27C0E, DiscordCoordinates.Default),
    Security("security", 0xED4245, DiscordCoordinates.Default),
    Computers("computers", 0x1ABC9C, DiscordCoordinates.Off),
    Strangers("strangers", 0x9B59B6, DiscordCoordinates.Default),
    Apocalypse("apocalypse", 0x8B0A1A, DiscordCoordinates.Default);

    private final String Key;
    private final int DefaultColour;
    private final DiscordCoordinates DefaultCoordinates;

    DiscordCategory(String Key, int DefaultColour, DiscordCoordinates DefaultCoordinates) {
        this.Key = Key;
        this.DefaultColour = DefaultColour;
        this.DefaultCoordinates = DefaultCoordinates;
    }

    public String getKey() {
        return Key;
    }

    public int getDefaultColour() {
        return DefaultColour;
    }

    public DiscordCoordinates getDefaultCoordinates() {
        return DefaultCoordinates;
    }

    public String langKey() {
        return "discord.scp_unleashed.category." + Key;
    }

    public static Optional<DiscordCategory> byKey(String Name) {
        String Wanted = Name.toLowerCase(Locale.ROOT);
        for (DiscordCategory Category : values()) {
            if (Category.Key.equals(Wanted)) {
                return Optional.of(Category);
            }
        }
        return Optional.empty();
    }
}
