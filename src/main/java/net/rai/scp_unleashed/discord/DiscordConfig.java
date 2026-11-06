package net.rai.scp_unleashed.discord;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class DiscordConfig {
    public static final String FileName = ScpConfigs.ServerFile;
    public static final String Prefix = "config.scp_unleashed.discord.";
    public static final Pattern ColourPattern = Pattern.compile("#?[0-9A-Fa-f]{6}");
    public static ModConfigSpec.BooleanValue Enabled;
    public static ModConfigSpec.ConfigValue<String> ServerName;
    public static ModConfigSpec.BooleanValue IncludeCoordinates;
    public static ModConfigSpec.IntValue RateLimitPerMinute;
    public static ModConfigSpec.IntValue QueueLimit;
    public static ModConfigSpec.ConfigValue<String> Username;
    public static ModConfigSpec.ConfigValue<String> AvatarUrl;
    public static ModConfigSpec.ConfigValue<String> PlayerAvatar;
    public static ModConfigSpec.BooleanValue Joins;
    public static ModConfigSpec.BooleanValue Leaves;
    public static ModConfigSpec.EnumValue<DiscordDeaths> Deaths;
    public static ModConfigSpec.EnumValue<DiscordReads> Reads;
    public static ModConfigSpec.IntValue RereadMinutes;
    public static ModConfigSpec.EnumValue<DiscordContainment> Containment;
    public static ModConfigSpec.EnumValue<DiscordRaids> Raids;
    public static ModConfigSpec.EnumValue<DiscordWarhead> Warhead;
    public static Map<DiscordCategory, Section> Sections;
    public static Map<DiscordToggle, ModConfigSpec.BooleanValue> Toggles;

    public record Section(ModConfigSpec.BooleanValue Enabled, ModConfigSpec.ConfigValue<String> Colour, ModConfigSpec.EnumValue<DiscordCoordinates> Coordinates) {
    }

    public static void define(ModConfigSpec.Builder Builder) {
        Builder.comment(
                "Discord webhook integration for server operators.",
                "Posts embeds about server events to Discord channels. Nothing is ever sent until you set enabled = true here and add a webhook.",
                "The webhook URLs themselves go in " + DiscordWebhooks.FileName + " next to this file. They are kept apart because NeoForge",
                "sends this server config file to every joining player, while the webhook file is never sent.",
                "Each category below can be turned off, coloured and given its own coordinate rule. Its channel is picked in the webhook file.",
                "Messages are sent in the background and never slow the server down. Mentions are always disabled, so nobody can ping @everyone or a role.",
                "Operators (permission level 3) can run /scp_unleashed discord test [category] and /scp_unleashed discord status in game.",
                "Both files live in the config folder and apply to every world. Copies placed in defaultconfigs are used when a file is missing.")
                .translation(Prefix + "general").push("general");
        Enabled = Builder.comment("Master switch. While false nothing is ever sent to Discord.")
                .translation(Prefix + "enabled").define("enabled", false);
        ServerName = Builder.comment("Optional name shown in the footer of every embed, such as your server's name.")
                .translation(Prefix + "server_name").define("serverName", "", DiscordConfig::isText);
        IncludeCoordinates = Builder.comment("Add block positions and the dimension to embeds. Each category can override this with its coordinates setting.")
                .translation(Prefix + "include_coordinates").define("includeCoordinates", true);
        RateLimitPerMinute = Builder.comment("Most messages sent to one webhook per minute. Discord allows about 30. Extra messages wait their turn in the queue.")
                .translation(Prefix + "rate_limit_per_minute").defineInRange("rateLimitPerMinute", DiscordSettings.DefaultPerMinute, 1, 120);
        QueueLimit = Builder.comment("Most messages waiting to be sent. New messages are dropped while the queue is full, for example when Discord is down.")
                .translation(Prefix + "queue_limit").defineInRange("queueLimit", DiscordSettings.DefaultQueueLimit, 10, 5000);
        Builder.pop();
        Builder.comment("How the webhook shows up in Discord.").translation(Prefix + "identity").push("identity");
        Username = Builder.comment("Optional name the webhook posts under. Empty keeps the name set in Discord.")
                .translation(Prefix + "username").define("username", "", DiscordConfig::isText);
        AvatarUrl = Builder.comment("Optional https image URL used as the webhook's picture. Empty keeps the picture set in Discord.")
                .translation(Prefix + "avatar_url").define("avatarUrl", "", DiscordConfig::isText);
        PlayerAvatar = Builder.comment(
                        "Image URL for player faces. {uuid} becomes the player's UUID and {name} their name. Leave empty to send no player images.",
                        "Privacy: with the default value, Discord loads faces from mc-heads.net, a third party service, so player UUIDs are shared with it.")
                .translation(Prefix + "player_avatar").define("playerAvatar", DiscordSettings.DefaultFace, DiscordConfig::isText);
        Builder.pop();
        Map<DiscordCategory, Section> Built = new EnumMap<>(DiscordCategory.class);
        Map<DiscordToggle, ModConfigSpec.BooleanValue> Switches = new EnumMap<>(DiscordToggle.class);
        DiscordFilters Stock = DiscordFilters.standard();
        ModConfigSpec.BooleanValue JoinValue = null;
        ModConfigSpec.BooleanValue LeaveValue = null;
        ModConfigSpec.EnumValue<DiscordDeaths> DeathValue = null;
        ModConfigSpec.EnumValue<DiscordReads> ReadValue = null;
        ModConfigSpec.IntValue RereadValue = null;
        ModConfigSpec.EnumValue<DiscordContainment> ContainmentValue = null;
        ModConfigSpec.EnumValue<DiscordRaids> RaidValue = null;
        ModConfigSpec.EnumValue<DiscordWarhead> WarheadValue = null;
        for (DiscordCategory Category : DiscordCategory.values()) {
            String Key = Category.getKey();
            Builder.comment(describe(Category)).translation(Prefix + Key).push(Key);
            ModConfigSpec.BooleanValue On = Builder.comment("Post " + Key + " events. The master switch must be on too.")
                    .translation(Prefix + Key + ".enabled").define("enabled", true);
            ModConfigSpec.ConfigValue<String> Colour = Builder.comment("Embed colour as a hex code such as #3BA55C.")
                    .translation(Prefix + Key + ".colour").define("colour", hex(Category.getDefaultColour()), DiscordConfig::isColour);
            ModConfigSpec.EnumValue<DiscordCoordinates> Coordinates = Builder.comment("Default follows includeCoordinates. On or Off overrides it for this category.")
                    .translation(Prefix + Key + ".coordinates").defineEnum("coordinates", Category.getDefaultCoordinates());
            if (Category == DiscordCategory.Players) {
                JoinValue = Builder.comment("Post when a player joins, with their face, a first join flag and the online count.")
                        .translation(Prefix + "players.joins").define("joins", true);
                LeaveValue = Builder.comment("Post when a player leaves.")
                        .translation(Prefix + "players.leaves").define("leaves", true);
                DeathValue = Builder.comment("Player deaths to post: Off, Anomalies for deaths caused by anomalies only, or All.")
                        .translation(Prefix + "players.deaths").defineEnum("deaths", DiscordDeaths.Anomalies);
            }
            for (DiscordToggle Toggle : DiscordToggle.of(Category)) {
                Switches.put(Toggle, Builder.comment(describe(Toggle)).translation(Prefix + Key + "." + Toggle.getLangKey()).define(Toggle.getKey(), Stock.allows(Toggle)));
            }
            if (Category == DiscordCategory.Documents) {
                ReadValue = Builder.comment("Reads of opened files to post: Off, FirstRead for only the first time each player opens each file, or EveryRead.",
                                "FirstRead remembers in the world save which files each player has opened. Breaking a seal counts as the first read when sealedFiles is on.")
                        .translation(Prefix + "documents.reads").defineEnum("reads", Stock.Reads());
                RereadValue = Builder.comment("With EveryRead, minutes before the same player reading the same file is posted again. 0 posts every read.")
                        .translation(Prefix + "documents.reread_minutes").defineInRange("rereadMinutes", Stock.RereadMinutes(), 0, DiscordFilters.MaxRereadMinutes);
            }
            if (Category == DiscordCategory.Anomalies) {
                ContainmentValue = Builder.comment("Containment changes to post: Off, Breaches for breaches only, or All to also post anomalies being contained again.")
                        .translation(Prefix + "anomalies.containment").defineEnum("containment", Stock.Containment());
            }
            if (Category == DiscordCategory.Security) {
                RaidValue = Builder.comment("Raid messages to post: Off, Milestones for warnings, starts and endings only, or All to also post what happens during a raid.")
                        .translation(Prefix + "security.raids").defineEnum("raids", Stock.Raids());
                WarheadValue = Builder.comment("Alpha Warhead messages to post: Off, Countdown for countdowns, cancels and detonations only, or All to also post arming and disarming.")
                        .translation(Prefix + "security.warhead").defineEnum("warhead", Stock.Warhead());
            }
            Builder.pop();
            Built.put(Category, new Section(On, Colour, Coordinates));
        }
        Joins = JoinValue;
        Leaves = LeaveValue;
        Deaths = DeathValue;
        Reads = ReadValue;
        RereadMinutes = RereadValue;
        Containment = ContainmentValue;
        Raids = RaidValue;
        Warhead = WarheadValue;
        Sections = Map.copyOf(Built);
        Toggles = Map.copyOf(Switches);
    }

    private DiscordConfig() {
    }

    private static String describe(DiscordCategory Category) {
        return switch (Category) {
            case Players -> "Players joining, leaving and dying.";
            case Documents -> "Players reading anomaly files and documents.";
            case Anomalies -> "Anomalies arriving in the world and changes to their containment.";
            case Security -> "Faction raids, MTF squads, the Alpha Warhead and other facility alarms.";
            case Computers -> "A copy of every message sent through computer chat, with the sender and the computer it came from. Coordinates are off by default.";
            case Strangers -> "Mysterious strangers appearing to players, what they hand over and when they leave.";
            case Apocalypse -> "XK-class end of the world scenarios such as SCP-5001 and SCP-001, from the first alarm to the end of the world, plus any other world ending.";
        };
    }

    private static String describe(DiscordToggle Toggle) {
        return switch (Toggle) {
            case SealedFiles -> "Post when a player breaks the seal on a sealed file.";
            case Arrivals -> "Post when an anomaly surfaces or manifests in the world.";
            case Rifts -> "Post when an Apocalyptic Rift forms or collapses.";
            case DaeviteGod -> "Post when the Daevite God is summoned or slain.";
            case MtfSquads -> "Post when a player calls in an MTF squad.";
            case Meltdowns -> "Post when a fission reactor or a reactor tower melts down.";
            case PlayerMessages -> "Post messages that players send through computer chat.";
            case ScheduledMessages -> "Post messages sent by scheduled computer actions.";
            case AnomalousMessages -> "Post anomalous messages that appear in computer chat.";
            case Appearances -> "Post when a stranger appears to a player.";
            case Deliveries -> "Post what a stranger hands over.";
            case Departures -> "Post when a stranger leaves.";
            case ScarletRituals -> "Post when cultists finish a ritual for the Scarlet King. Repeats within ten minutes are held back.";
        };
    }

    public static String hex(int Colour) {
        return String.format(Locale.ROOT, "#%06X", Colour & 0xFFFFFF);
    }

    private static boolean isText(Object Value) {
        return Value instanceof String;
    }

    private static boolean isColour(Object Value) {
        return Value instanceof String Text && ColourPattern.matcher(Text.strip()).matches();
    }

    public static int parseColour(String Text, int Fallback) {
        String Clean = Text == null ? "" : Text.strip();
        if (!ColourPattern.matcher(Clean).matches()) {
            return Fallback;
        }
        return Integer.parseInt(Clean.startsWith("#") ? Clean.substring(1) : Clean, 16);
    }

    public static DiscordSettings snapshot() {
        if (!ScpConfigs.Server.isLoaded()) {
            return DiscordSettings.disabled();
        }
        Map<DiscordCategory, DiscordSettings.Channel> Channels = new EnumMap<>(DiscordCategory.class);
        for (Map.Entry<DiscordCategory, Section> Entry : Sections.entrySet()) {
            Section Part = Entry.getValue();
            Channels.put(Entry.getKey(), new DiscordSettings.Channel(Part.Enabled().get(), DiscordWebhooks.webhook(Entry.getKey()),
                    parseColour(Part.Colour().get(), Entry.getKey().getDefaultColour()), Part.Coordinates().get()));
        }
        Set<DiscordToggle> Muted = EnumSet.noneOf(DiscordToggle.class);
        for (Map.Entry<DiscordToggle, ModConfigSpec.BooleanValue> Entry : Toggles.entrySet()) {
            if (!Entry.getValue().get()) {
                Muted.add(Entry.getKey());
            }
        }
        DiscordFilters Filters = new DiscordFilters(Muted, Reads.get(), RereadMinutes.get(), Containment.get(), Raids.get(), Warhead.get());
        return new DiscordSettings(Enabled.get(), DiscordWebhooks.defaultWebhook(), ServerName.get(), Username.get(), AvatarUrl.get(), PlayerAvatar.get(),
                IncludeCoordinates.get(), RateLimitPerMinute.get(), QueueLimit.get(), Channels, Joins.get(), Leaves.get(), Deaths.get(), Filters, false);
    }
}
