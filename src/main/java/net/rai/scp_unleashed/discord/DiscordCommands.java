package net.rai.scp_unleashed.discord;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DiscordCommands {
    public static final String Prefix = "commands.scp_unleashed.discord.";
    public static final int Permission = 3;

    private DiscordCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> TestNode = Commands.literal("test").executes(Context -> test(Context.getSource(), null));
        for (DiscordCategory Category : DiscordCategory.values()) {
            TestNode.then(Commands.literal(Category.getKey()).executes(Context -> test(Context.getSource(), Category)));
        }
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("discord")
                        .requires(Source -> Source.hasPermission(Permission))
                        .then(TestNode)
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    private static String names(List<DiscordCategory> Categories) {
        List<String> Shown = new ArrayList<>();
        for (DiscordCategory Category : Categories) {
            Shown.add(Category.getKey());
        }
        return String.join(", ", Shown);
    }

    public static int test(CommandSourceStack Source, @Nullable DiscordCategory Only) {
        DiscordSettings Settings = Discord.settings();
        if (!Settings.Enabled()) {
            Source.sendFailure(Component.translatable(Prefix + "disabled", DiscordConfig.FileName));
            return 0;
        }
        List<DiscordCategory> Wanted = Only == null ? List.of(DiscordCategory.values()) : List.of(Only);
        Map<String, List<DiscordCategory>> Groups = new LinkedHashMap<>();
        Map<String, Discord.Route> Routes = new LinkedHashMap<>();
        for (DiscordCategory Category : Wanted) {
            Discord.Route Found = Discord.route(Settings, Category);
            switch (Found.State()) {
                case NoWebhook -> {
                    if (Only != null) {
                        Source.sendFailure(Component.translatable(Prefix + "no_webhook", Category.getKey(), DiscordWebhooks.FileName));
                    }
                }
                case BadWebhook -> Source.sendFailure(Component.translatable(Prefix + "invalid", Category.getKey(), Found.Label()));
                case Ready -> {
                    String Key = String.valueOf(Found.Target());
                    Groups.computeIfAbsent(Key, Ignored -> new ArrayList<>()).add(Category);
                    Routes.putIfAbsent(Key, Found);
                }
                case Disabled -> {
                }
            }
        }
        if (Groups.isEmpty()) {
            if (Only == null) {
                Source.sendFailure(Component.translatable(Prefix + "nothing", DiscordWebhooks.FileName));
            }
            return 0;
        }
        MinecraftServer Server = Source.getServer();
        int Count = 0;
        for (Map.Entry<String, List<DiscordCategory>> Group : Groups.entrySet()) {
            List<DiscordCategory> Members = Group.getValue();
            DiscordCategory Lead = Members.get(0);
            String Label = Routes.get(Group.getKey()).Label();
            String Listed = names(Members);
            DiscordEmbed Embed = DiscordNotices.sample(Lead, Source.getPlayer());
            if (Members.size() > 1) {
                Embed.field(DiscordText.translate("discord.scp_unleashed.field.categories"), Listed, false);
            }
            CompletableFuture<DiscordRelay.Result> Done = Discord.send(Settings, Lead, Embed);
            if (Done == null) {
                Source.sendFailure(Component.translatable(Prefix + "failed", Listed, Label, "not queued"));
                continue;
            }
            Source.sendSuccess(() -> Component.translatable(Prefix + "queued", Listed, Label).withStyle(ChatFormatting.GRAY), false);
            boolean Quiet = Members.stream().noneMatch(Member -> Settings.channel(Member).Enabled());
            Done.whenComplete((Reply, Thrown) -> Server.execute(() -> {
                if (Reply != null && Reply.Success()) {
                    Source.sendSuccess(() -> Component.translatable(Prefix + "success", Listed, Label, Reply.Detail()).withStyle(ChatFormatting.GREEN), false);
                    if (Quiet) {
                        Source.sendSuccess(() -> Component.translatable(Prefix + "category_off", Listed).withStyle(ChatFormatting.GRAY), false);
                    }
                } else {
                    String Why = Reply != null ? Reply.Detail() : Thrown == null ? "unknown" : Thrown.getClass().getSimpleName();
                    Source.sendFailure(Component.translatable(Prefix + "failed", Listed, Label, Why));
                }
            }));
            Count++;
        }
        return Count;
    }

    public static int status(CommandSourceStack Source) {
        DiscordSettings Settings = Discord.settings();
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.header", Component.translatable(Prefix + (Settings.Enabled() ? "status.on" : "status.off"))), false);
        DiscordRelay Relay = Discord.current();
        if (Relay == null) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.idle"), false);
        } else {
            DiscordRelay.Snapshot Now = Relay.status();
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.queue", Now.Queued(), Now.QueueLimit(), Now.Sent(), Now.Dropped(), Now.Failed(), Now.Limited()), false);
            if (Now.LastError().isEmpty()) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.no_error"), false);
            } else {
                long Seconds = Math.max(0L, Now.LastErrorAgoMillis() / 1000L);
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.error", Now.LastError(), Seconds).withStyle(ChatFormatting.RED), false);
            }
            if (Now.GlobalBlockedForMillis() > 0L) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.global", seconds(Now.GlobalBlockedForMillis())), false);
            }
            for (DiscordRelay.BucketView View : Now.Buckets()) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.bucket", View.Label(), View.UsedThisMinute(), Now.PerMinute(),
                        View.Remaining() < 0 ? "?" : String.valueOf(View.Remaining()), seconds(View.BlockedForMillis())), false);
            }
        }
        for (DiscordCategory Category : DiscordCategory.values()) {
            DiscordSettings.Channel Part = Settings.channel(Category);
            Discord.Route Found = Discord.route(Settings, Category);
            String State = !Part.Enabled() ? "status.category.off" : switch (Found.State()) {
                case Ready -> Part.Webhook().isEmpty() ? "status.category.default" : "status.category.own";
                case NoWebhook -> "status.category.none";
                case BadWebhook -> "status.category.invalid";
                case Disabled -> "status.category.master_off";
            };
            boolean Coordinates = Settings.coordinatesFor(Category);
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.category", Category.getKey(), Component.translatable(Prefix + State, Found.Label()),
                    Component.translatable(Prefix + (Coordinates ? "status.coordinates_on" : "status.coordinates_off"))), false);
        }
        return 1;
    }

    private static String seconds(long Millis) {
        return String.format(Locale.ROOT, "%.1f", Millis / 1000.0);
    }
}
