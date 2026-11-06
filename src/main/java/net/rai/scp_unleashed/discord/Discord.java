package net.rai.scp_unleashed.discord;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Discord {
    public static final Duration ShutdownFlush = Duration.ofSeconds(3);
    public static final Duration FinalFlush = Duration.ofSeconds(8);
    private static final Logger Log = LogUtils.getLogger();
    private static final Set<String> Warned = ConcurrentHashMap.newKeySet();
    @Nullable
    private static volatile DiscordSettings Cached;
    @Nullable
    private static volatile DiscordSettings Override;
    @Nullable
    private static DiscordRelay Relay;
    @Nullable
    private static volatile CompletableFuture<DiscordRelay.Result> Kept;

    public enum Readiness {
        Ready,
        Disabled,
        NoWebhook,
        BadWebhook
    }

    public record Route(Readiness State, @Nullable URI Target, String Label) {
    }

    private Discord() {
    }

    public static DiscordSettings settings() {
        DiscordSettings Testing = Override;
        if (Testing != null) {
            return Testing;
        }
        DiscordSettings Known = Cached;
        if (Known == null) {
            Known = DiscordConfig.snapshot();
            Cached = Known;
        }
        return Known;
    }

    public static void invalidate() {
        Cached = null;
        Warned.clear();
    }

    public static void useForTests(@Nullable DiscordSettings Settings) {
        Override = Settings;
        Warned.clear();
    }

    public static boolean wants(DiscordCategory Category) {
        return settings().wants(Category);
    }

    public static Route route(DiscordSettings Settings, DiscordCategory Category) {
        if (!Settings.Enabled()) {
            return new Route(Readiness.Disabled, null, "none");
        }
        String Raw = Settings.webhookFor(Category);
        if (Raw.isEmpty()) {
            return new Route(Readiness.NoWebhook, null, "none");
        }
        String Label = DiscordUrls.label(Raw);
        Optional<URI> Parsed = DiscordUrls.parse(Raw, Settings.AllowLoopback());
        if (Parsed.isEmpty()) {
            if (Warned.add(Category.getKey() + "|" + Label)) {
                Log.warn("Discord webhook {} for the {} category is not a discord.com webhook URL, nothing will be sent there",
                        Label, Category.getKey());
            }
            return new Route(Readiness.BadWebhook, null, Label);
        }
        return new Route(Readiness.Ready, Parsed.get(), Label);
    }

    public static Optional<CompletableFuture<DiscordRelay.Result>> post(DiscordCategory Category, DiscordEmbed Embed) {
        DiscordSettings Settings = settings();
        if (!Settings.wants(Category)) {
            return Optional.empty();
        }
        return Optional.ofNullable(send(Settings, Category, Embed));
    }

    @Nullable
    public static CompletableFuture<DiscordRelay.Result> send(DiscordSettings Settings, DiscordCategory Category, DiscordEmbed Embed) {
        Route Found = route(Settings, Category);
        if (Found.State() != Readiness.Ready || Found.Target() == null) {
            return null;
        }
        JsonObject Payload = DiscordEmbed.payload(Settings, Category, Embed, Instant.now());
        return relay(Settings).offer(Found.Target(), Payload.toString(), Category.getKey());
    }

    public static synchronized DiscordRelay relay(DiscordSettings Settings) {
        if (Relay == null) {
            Relay = new DiscordRelay(Settings.QueueLimit(), Settings.PerMinute());
        } else {
            Relay.limits(Settings.QueueLimit(), Settings.PerMinute());
        }
        return Relay;
    }

    @Nullable
    public static synchronized DiscordRelay current() {
        return Relay;
    }

    public static void keep(CompletableFuture<DiscordRelay.Result> Done) {
        Kept = Done;
    }

    public static boolean await(CompletableFuture<DiscordRelay.Result> Done, Duration Limit) {
        try {
            DiscordRelay.Result Reply = Done.get(Limit.toMillis(), TimeUnit.MILLISECONDS);
            return Reply != null && Reply.Success();
        } catch (InterruptedException Stop) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException Problem) {
            return false;
        }
    }

    public static void resetForTests() {
        Kept = null;
        DiscordRelay Stopping;
        synchronized (Discord.class) {
            Stopping = Relay;
            Relay = null;
        }
        if (Stopping != null) {
            Stopping.stop(Duration.ZERO);
        }
        Override = null;
        invalidate();
    }

    public static void shutdown() {
        DiscordRelay Stopping;
        synchronized (Discord.class) {
            Stopping = Relay;
            Relay = null;
        }
        CompletableFuture<DiscordRelay.Result> Pending = Kept;
        Kept = null;
        if (Stopping != null) {
            Stopping.stop(Pending != null && !Pending.isDone() ? FinalFlush : ShutdownFlush);
        }
        invalidate();
    }
}
