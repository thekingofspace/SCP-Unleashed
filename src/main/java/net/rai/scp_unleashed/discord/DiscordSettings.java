package net.rai.scp_unleashed.discord;

import java.util.EnumMap;
import java.util.Map;

public record DiscordSettings(boolean Enabled, String DefaultWebhook, String ServerName, String Username, String AvatarUrl, String FaceTemplate,
                              boolean Coordinates, int PerMinute, int QueueLimit, Map<DiscordCategory, Channel> Channels,
                              boolean Joins, boolean Leaves, DiscordDeaths Deaths, DiscordFilters Filters, boolean AllowLoopback) {
    public static final String DefaultFace = "https://mc-heads.net/avatar/{uuid}/64";
    public static final int DefaultPerMinute = 25;
    public static final int DefaultQueueLimit = 200;

    public DiscordSettings {
        DefaultWebhook = DefaultWebhook == null ? "" : DefaultWebhook.strip();
        ServerName = ServerName == null ? "" : ServerName.strip();
        Username = Username == null ? "" : Username.strip();
        AvatarUrl = AvatarUrl == null ? "" : AvatarUrl.strip();
        FaceTemplate = FaceTemplate == null ? "" : FaceTemplate.strip();
        PerMinute = Math.max(1, PerMinute);
        QueueLimit = Math.max(1, QueueLimit);
        EnumMap<DiscordCategory, Channel> Filled = new EnumMap<>(DiscordCategory.class);
        for (DiscordCategory Category : DiscordCategory.values()) {
            Channel Given = Channels == null ? null : Channels.get(Category);
            Filled.put(Category, Given == null ? Channel.standard(Category) : Given);
        }
        Channels = Map.copyOf(Filled);
        Deaths = Deaths == null ? DiscordDeaths.Anomalies : Deaths;
        Filters = Filters == null ? DiscordFilters.standard() : Filters;
    }

    public record Channel(boolean Enabled, String Webhook, int Colour, DiscordCoordinates Coordinates) {
        public Channel {
            Webhook = Webhook == null ? "" : Webhook.strip();
            Colour = Colour & 0xFFFFFF;
            Coordinates = Coordinates == null ? DiscordCoordinates.Default : Coordinates;
        }

        public static Channel standard(DiscordCategory Category) {
            return new Channel(true, "", Category.getDefaultColour(), Category.getDefaultCoordinates());
        }
    }

    public static DiscordSettings disabled() {
        return new DiscordSettings(false, "", "", "", "", DefaultFace, true, DefaultPerMinute, DefaultQueueLimit, Map.of(), true, true, DiscordDeaths.Anomalies, DiscordFilters.standard(), false);
    }

    public static DiscordSettings testing(String Webhook) {
        return new DiscordSettings(true, Webhook, "Test Site", "", "", DefaultFace, true, 600, DefaultQueueLimit, Map.of(), true, true, DiscordDeaths.All, DiscordFilters.standard(), true);
    }

    public Channel channel(DiscordCategory Category) {
        return Channels.get(Category);
    }

    public boolean wants(DiscordCategory Category) {
        return Enabled && channel(Category).Enabled();
    }

    public boolean allows(DiscordToggle Toggle) {
        return wants(Toggle.getCategory()) && Filters.allows(Toggle);
    }

    public String webhookFor(DiscordCategory Category) {
        String Own = channel(Category).Webhook();
        return Own.isEmpty() ? DefaultWebhook : Own;
    }

    public boolean coordinatesFor(DiscordCategory Category) {
        return channel(Category).Coordinates().resolve(Coordinates);
    }

    public DiscordSettings withEnabled(boolean Value) {
        return new DiscordSettings(Value, DefaultWebhook, ServerName, Username, AvatarUrl, FaceTemplate, Coordinates, PerMinute, QueueLimit, Channels, Joins, Leaves, Deaths, Filters, AllowLoopback);
    }

    public DiscordSettings withDefaultWebhook(String Value) {
        return new DiscordSettings(Enabled, Value, ServerName, Username, AvatarUrl, FaceTemplate, Coordinates, PerMinute, QueueLimit, Channels, Joins, Leaves, Deaths, Filters, AllowLoopback);
    }

    public DiscordSettings withFace(String Value) {
        return new DiscordSettings(Enabled, DefaultWebhook, ServerName, Username, AvatarUrl, Value, Coordinates, PerMinute, QueueLimit, Channels, Joins, Leaves, Deaths, Filters, AllowLoopback);
    }

    public DiscordSettings withCoordinates(boolean Value) {
        return new DiscordSettings(Enabled, DefaultWebhook, ServerName, Username, AvatarUrl, FaceTemplate, Value, PerMinute, QueueLimit, Channels, Joins, Leaves, Deaths, Filters, AllowLoopback);
    }

    public DiscordSettings withLimits(int NewPerMinute, int NewQueueLimit) {
        return new DiscordSettings(Enabled, DefaultWebhook, ServerName, Username, AvatarUrl, FaceTemplate, Coordinates, NewPerMinute, NewQueueLimit, Channels, Joins, Leaves, Deaths, Filters, AllowLoopback);
    }

    public DiscordSettings withDeaths(DiscordDeaths Value) {
        return new DiscordSettings(Enabled, DefaultWebhook, ServerName, Username, AvatarUrl, FaceTemplate, Coordinates, PerMinute, QueueLimit, Channels, Joins, Leaves, Value, Filters, AllowLoopback);
    }

    public DiscordSettings withFilters(DiscordFilters Value) {
        return new DiscordSettings(Enabled, DefaultWebhook, ServerName, Username, AvatarUrl, FaceTemplate, Coordinates, PerMinute, QueueLimit, Channels, Joins, Leaves, Deaths, Value, AllowLoopback);
    }

    public DiscordSettings withLoopback(boolean Value) {
        return new DiscordSettings(Enabled, DefaultWebhook, ServerName, Username, AvatarUrl, FaceTemplate, Coordinates, PerMinute, QueueLimit, Channels, Joins, Leaves, Deaths, Filters, Value);
    }

    public DiscordSettings withChannel(DiscordCategory Category, Channel Value) {
        EnumMap<DiscordCategory, Channel> Changed = new EnumMap<>(Channels);
        Changed.put(Category, Value);
        return new DiscordSettings(Enabled, DefaultWebhook, ServerName, Username, AvatarUrl, FaceTemplate, Coordinates, PerMinute, QueueLimit, Changed, Joins, Leaves, Deaths, Filters, AllowLoopback);
    }

    public DiscordSettings withWebhook(DiscordCategory Category, String Value) {
        Channel Old = channel(Category);
        return withChannel(Category, new Channel(Old.Enabled(), Value, Old.Colour(), Old.Coordinates()));
    }

    public DiscordSettings withCategoryCoordinates(DiscordCategory Category, DiscordCoordinates Value) {
        Channel Old = channel(Category);
        return withChannel(Category, new Channel(Old.Enabled(), Old.Webhook(), Old.Colour(), Value));
    }

    public DiscordSettings withCategoryEnabled(DiscordCategory Category, boolean Value) {
        Channel Old = channel(Category);
        return withChannel(Category, new Channel(Value, Old.Webhook(), Old.Colour(), Old.Coordinates()));
    }
}
