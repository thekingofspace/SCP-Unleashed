package net.rai.scp_unleashed.discord;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

import java.util.EnumMap;
import java.util.Map;

public final class DiscordWebhooks {
    public static final String FileName = ScpConfigs.CommonFile;
    public static final String Prefix = "config.scp_unleashed.discord.webhooks.";
    public static ModConfigSpec.ConfigValue<String> DefaultWebhook;
    public static Map<DiscordCategory, ModConfigSpec.ConfigValue<String>> Categories;

    public static void define(ModConfigSpec.Builder Builder) {
        Builder.comment(
                "Discord webhook URLs for SCP Unleashed. Every other Discord setting is in " + DiscordConfig.FileName + ".",
                "These URLs live in their own file on purpose: NeoForge sends every server config file to joining players, and this file is never sent.",
                "Treat each URL like a password: anyone who has it can post to your channel. The mod never prints them in the log.",
                "Setup: in Discord open Channel Settings > Integrations > Webhooks > New Webhook, copy its URL and paste it below.",
                "Only https://discord.com/api/webhooks/... URLs are accepted (discordapp.com, ptb and canary work too). Anything else is refused with a console warning.",
                "A category with an empty webhook uses defaultWebhook, so one URL is enough to start. Give a category its own URL to post it to another channel.")
                .translation(Prefix + "section").push("webhooks");
        DefaultWebhook = Builder.comment("Webhook URL used by every category that has no webhook of its own.")
                .translation(Prefix + "default").define("defaultWebhook", "", DiscordWebhooks::isText);
        Map<DiscordCategory, ModConfigSpec.ConfigValue<String>> Built = new EnumMap<>(DiscordCategory.class);
        for (DiscordCategory Category : DiscordCategory.values()) {
            Built.put(Category, Builder.comment(hint(Category))
                    .translation(Prefix + Category.getKey()).define(Category.getKey(), "", DiscordWebhooks::isText));
        }
        Builder.pop();
        Categories = Map.copyOf(Built);
    }

    private DiscordWebhooks() {
    }

    private static String hint(DiscordCategory Category) {
        return switch (Category) {
            case Computers -> "Webhook URL for computer chat only, usually a private moderator channel. Empty uses defaultWebhook.";
            default -> "Webhook URL for " + Category.getKey() + " events only. Empty uses defaultWebhook.";
        };
    }

    private static boolean isText(Object Value) {
        return Value instanceof String;
    }

    public static String defaultWebhook() {
        return ScpConfigs.Common.isLoaded() ? DefaultWebhook.get() : "";
    }

    public static String webhook(DiscordCategory Category) {
        return ScpConfigs.Common.isLoaded() ? Categories.get(Category).get() : "";
    }
}
