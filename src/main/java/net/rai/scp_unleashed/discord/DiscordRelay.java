package net.rai.scp_unleashed.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class DiscordRelay {
    public static final Duration ConnectTimeout = Duration.ofSeconds(5);
    public static final Duration RequestTimeout = Duration.ofSeconds(10);
    public static final int MaxServerRetries = 3;
    public static final int MaxRateRetries = 5;
    public static final long BaseBackoffMillis = 1000L;
    public static final long MaxWaitMillis = 60000L;
    public static final long WindowMillis = 60000L;
    public static final long DropWarnMillis = 60000L;
    public static final String Agent = "SCP-Unleashed-Discord (NeoForge mod, webhook relay)";
    private static final Logger Log = LogUtils.getLogger();
    private static final AtomicInteger Threads = new AtomicInteger();

    private final LinkedBlockingDeque<Outgoing> Waiting = new LinkedBlockingDeque<>();
    private final Map<String, Bucket> Buckets = new ConcurrentHashMap<>();
    private final AtomicLong Sent = new AtomicLong();
    private final AtomicLong Dropped = new AtomicLong();
    private final AtomicLong Failed = new AtomicLong();
    private final AtomicLong Limited = new AtomicLong();
    private final AtomicInteger Busy = new AtomicInteger();
    private final Object Gate = new Object();
    private final long Backoff;
    private volatile int QueueLimit;
    private volatile int PerMinute;
    private volatile boolean Running;
    private volatile boolean Held;
    private volatile long GlobalUntil;
    private volatile long LastDropWarn;
    private volatile String LastError = "";
    private volatile long LastErrorAt;
    @Nullable
    private volatile Thread Worker;
    @Nullable
    private volatile HttpClient Client;

    public record Outgoing(URI Target, String Label, String Body, String Category, CompletableFuture<Result> Done, long Queued) {
    }

    public record Result(boolean Success, int Status, String Detail) {
        public static Result failure(String Detail) {
            return new Result(false, 0, Detail);
        }
    }

    public record BucketView(String Label, int UsedThisMinute, int Remaining, long BlockedForMillis) {
    }

    public record Snapshot(int Queued, int QueueLimit, int PerMinute, long Sent, long Dropped, long Failed, long Limited, String LastError, long LastErrorAgoMillis,
                         long GlobalBlockedForMillis, List<BucketView> Buckets, boolean Running) {
    }

    private static final class Bucket {
        private final String Label;
        private final Deque<Long> Recent = new ArrayDeque<>();
        private volatile long BlockedUntil;
        private volatile int Remaining = -1;

        private Bucket(String Label) {
            this.Label = Label;
        }
    }

    public DiscordRelay(int QueueLimit, int PerMinute) {
        this(QueueLimit, PerMinute, BaseBackoffMillis);
    }

    public DiscordRelay(int QueueLimit, int PerMinute, long Backoff) {
        this.QueueLimit = Math.max(1, QueueLimit);
        this.PerMinute = Math.max(1, PerMinute);
        this.Backoff = Math.max(1L, Backoff);
    }

    public void limits(int NewQueueLimit, int NewPerMinute) {
        QueueLimit = Math.max(1, NewQueueLimit);
        PerMinute = Math.max(1, NewPerMinute);
    }

    public void hold(boolean Value) {
        Held = Value;
        synchronized (Gate) {
            Gate.notifyAll();
        }
    }

    public int queued() {
        return Waiting.size();
    }

    public long sent() {
        return Sent.get();
    }

    public long dropped() {
        return Dropped.get();
    }

    public CompletableFuture<Result> offer(URI Target, String Body, String Category) {
        CompletableFuture<Result> Done = new CompletableFuture<>();
        String Label = DiscordUrls.label(Target.toString());
        if (Waiting.size() >= QueueLimit) {
            Dropped.incrementAndGet();
            long Now = millis();
            if (Now - LastDropWarn >= DropWarnMillis) {
                LastDropWarn = Now;
                Log.warn("Discord queue is full ({} waiting), dropping new {} messages for now", Waiting.size(), Category);
            }
            Done.complete(Result.failure("queue full"));
            return Done;
        }
        Waiting.offerLast(new Outgoing(Target, Label, Body, Category, Done, millis()));
        ensureWorker();
        return Done;
    }

    private synchronized void ensureWorker() {
        Thread Current = Worker;
        if (Running && Current != null && Current.isAlive()) {
            return;
        }
        Running = true;
        if (Client == null) {
            Client = HttpClient.newBuilder().connectTimeout(ConnectTimeout).followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
        }
        Thread Made = new Thread(this::work, "SCP Unleashed Discord relay " + Threads.incrementAndGet());
        Made.setDaemon(true);
        Worker = Made;
        Made.start();
    }

    private void work() {
        while (Running || !Waiting.isEmpty()) {
            Outgoing Next;
            try {
                waitWhileHeld();
                Next = Waiting.pollFirst(250L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException Stop) {
                break;
            }
            if (Next == null) {
                continue;
            }
            Busy.incrementAndGet();
            try {
                Next.Done().complete(deliver(Next));
            } catch (InterruptedException Stop) {
                Next.Done().complete(Result.failure("stopped"));
                Busy.decrementAndGet();
                break;
            } catch (RuntimeException Problem) {
                Failed.incrementAndGet();
                error(Next, "unexpected " + Problem.getClass().getSimpleName());
                Next.Done().complete(Result.failure("unexpected error"));
            }
            Busy.decrementAndGet();
        }
    }

    private void waitWhileHeld() throws InterruptedException {
        synchronized (Gate) {
            while (Held && Running) {
                Gate.wait(250L);
            }
        }
    }

    private Result deliver(Outgoing Parcel) throws InterruptedException {
        Bucket Slot = Buckets.computeIfAbsent(Parcel.Target().toString(), Key -> new Bucket(Parcel.Label()));
        int ServerTries = 0;
        int RateTries = 0;
        while (true) {
            pause(readyIn(Slot));
            HttpResponse<String> Reply;
            try {
                HttpClient Using = Client;
                if (Using == null) {
                    return Result.failure("stopped");
                }
                HttpRequest Request = HttpRequest.newBuilder(Parcel.Target()).timeout(RequestTimeout).header("Content-Type", "application/json")
                        .header("User-Agent", Agent).POST(HttpRequest.BodyPublishers.ofString(Parcel.Body(), StandardCharsets.UTF_8)).build();
                stamp(Slot);
                Reply = Using.send(Request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException Problem) {
                ServerTries++;
                String Reason = Problem.getClass().getSimpleName() + scrubbed(Problem.getMessage(), Parcel);
                if (ServerTries > MaxServerRetries || !Running) {
                    Failed.incrementAndGet();
                    error(Parcel, Reason);
                    return Result.failure(Reason);
                }
                pause(Backoff << (ServerTries - 1));
                continue;
            } catch (IllegalArgumentException Problem) {
                Failed.incrementAndGet();
                error(Parcel, "bad request");
                return Result.failure("bad request");
            }
            int Code = Reply.statusCode();
            readHeaders(Slot, Reply);
            if (Code >= 200 && Code < 300) {
                Sent.incrementAndGet();
                return new Result(true, Code, "HTTP " + Code);
            }
            if (Code == 429) {
                Limited.incrementAndGet();
                RateTries++;
                long Wait = retryAfter(Reply);
                if (isGlobal(Reply)) {
                    GlobalUntil = millis() + Wait;
                } else {
                    Slot.BlockedUntil = Math.max(Slot.BlockedUntil, millis() + Wait);
                }
                if (RateTries > MaxRateRetries) {
                    Failed.incrementAndGet();
                    error(Parcel, "rate limited too often");
                    return new Result(false, Code, "HTTP 429");
                }
                continue;
            }
            if (Code >= 500) {
                ServerTries++;
                if (ServerTries > MaxServerRetries || !Running) {
                    Failed.incrementAndGet();
                    error(Parcel, "HTTP " + Code);
                    return new Result(false, Code, "HTTP " + Code);
                }
                pause(Backoff << (ServerTries - 1));
                continue;
            }
            Failed.incrementAndGet();
            String Detail = "HTTP " + Code + (Code == 401 || Code == 404 ? " (webhook missing or deleted)" : Code == 400 ? " (Discord rejected the message)" : "");
            error(Parcel, Detail);
            return new Result(false, Code, "HTTP " + Code);
        }
    }

    private String scrubbed(@Nullable String Message, Outgoing Parcel) {
        if (Message == null || Message.isBlank()) {
            return "";
        }
        return ": " + DiscordUrls.scrub(Message, Parcel.Target().toString());
    }

    private void error(Outgoing Parcel, String Detail) {
        LastError = Parcel.Category() + " via " + Parcel.Label() + ": " + Detail;
        LastErrorAt = millis();
        Log.warn("Discord {} message via {} was not delivered: {}", Parcel.Category(), Parcel.Label(), Detail);
    }

    private long readyIn(Bucket Slot) {
        long Now = millis();
        long Ready = Math.max(GlobalUntil, Slot.BlockedUntil);
        synchronized (Slot.Recent) {
            while (!Slot.Recent.isEmpty() && Now - Slot.Recent.peekFirst() >= WindowMillis) {
                Slot.Recent.pollFirst();
            }
            if (Slot.Recent.size() >= PerMinute && !Slot.Recent.isEmpty()) {
                Ready = Math.max(Ready, Slot.Recent.peekFirst() + WindowMillis);
            }
        }
        return Math.max(0L, Math.min(MaxWaitMillis, Ready - Now));
    }

    private void stamp(Bucket Slot) {
        synchronized (Slot.Recent) {
            Slot.Recent.addLast(millis());
            while (Slot.Recent.size() > 512) {
                Slot.Recent.pollFirst();
            }
        }
    }

    private void readHeaders(Bucket Slot, HttpResponse<String> Reply) {
        Optional<String> Remaining = Reply.headers().firstValue("X-RateLimit-Remaining");
        Optional<String> ResetAfter = Reply.headers().firstValue("X-RateLimit-Reset-After");
        if (Remaining.isEmpty()) {
            return;
        }
        try {
            int Left = Integer.parseInt(Remaining.get().strip());
            Slot.Remaining = Left;
            if (Left <= 0 && ResetAfter.isPresent()) {
                long Wait = Math.min(MaxWaitMillis, Math.max(0L, Math.round(Double.parseDouble(ResetAfter.get().strip()) * 1000.0)));
                Slot.BlockedUntil = Math.max(Slot.BlockedUntil, millis() + Wait);
            }
        } catch (NumberFormatException Ignored) {
            Slot.Remaining = -1;
        }
    }

    private static long retryAfter(HttpResponse<String> Reply) {
        double Seconds = -1.0;
        try {
            JsonElement Parsed = JsonParser.parseString(Reply.body() == null ? "" : Reply.body());
            if (Parsed.isJsonObject()) {
                JsonObject Body = Parsed.getAsJsonObject();
                if (Body.has("retry_after") && Body.get("retry_after").isJsonPrimitive()) {
                    Seconds = Body.get("retry_after").getAsDouble();
                }
            }
        } catch (RuntimeException Ignored) {
            Seconds = -1.0;
        }
        if (Seconds < 0.0) {
            Optional<String> Header = Reply.headers().firstValue("Retry-After");
            try {
                Seconds = Header.map(Value -> Double.parseDouble(Value.strip())).orElse(1.0);
            } catch (NumberFormatException Ignored) {
                Seconds = 1.0;
            }
        }
        return Math.min(MaxWaitMillis, Math.max(50L, Math.round(Seconds * 1000.0)));
    }

    private static boolean isGlobal(HttpResponse<String> Reply) {
        if (Reply.headers().firstValue("X-RateLimit-Global").map("true"::equalsIgnoreCase).orElse(false)) {
            return true;
        }
        try {
            JsonElement Parsed = JsonParser.parseString(Reply.body() == null ? "" : Reply.body());
            return Parsed.isJsonObject() && Parsed.getAsJsonObject().has("global") && Parsed.getAsJsonObject().get("global").getAsBoolean();
        } catch (RuntimeException Ignored) {
            return false;
        }
    }

    private void pause(long Millis) throws InterruptedException {
        if (Millis > 0L) {
            Thread.sleep(Millis);
        }
    }

    public boolean flush(Duration Limit) {
        long Until = millis() + Limit.toMillis();
        while (millis() < Until) {
            if (Waiting.isEmpty() && Busy.get() == 0) {
                return true;
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException Stop) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return Waiting.isEmpty() && Busy.get() == 0;
    }

    public void stop(Duration Limit) {
        Held = false;
        synchronized (Gate) {
            Gate.notifyAll();
        }
        boolean Flushed = Worker == null || flush(Limit);
        Running = false;
        Thread Current = Worker;
        if (Current != null) {
            Current.interrupt();
            try {
                Current.join(1000L);
            } catch (InterruptedException Stop) {
                Thread.currentThread().interrupt();
            }
        }
        List<Outgoing> Left = new ArrayList<>();
        Waiting.drainTo(Left);
        for (Outgoing Parcel : Left) {
            Dropped.incrementAndGet();
            Parcel.Done().complete(Result.failure("server stopping"));
        }
        if (!Flushed || !Left.isEmpty()) {
            Log.warn("Discord relay stopped with {} messages unsent", Left.size());
        }
        HttpClient Using = Client;
        Client = null;
        Worker = null;
        if (Using != null) {
            Using.shutdownNow();
        }
    }

    public Snapshot status() {
        long Now = millis();
        List<BucketView> Views = new ArrayList<>();
        for (Bucket Slot : Buckets.values()) {
            int Used;
            synchronized (Slot.Recent) {
                Used = (int) Slot.Recent.stream().filter(Stamp -> Now - Stamp < WindowMillis).count();
            }
            Views.add(new BucketView(Slot.Label, Used, Slot.Remaining, Math.max(0L, Slot.BlockedUntil - Now)));
        }
        return new Snapshot(Waiting.size(), QueueLimit, PerMinute, Sent.get(), Dropped.get(), Failed.get(), Limited.get(), LastError,
                LastErrorAt == 0L ? -1L : Now - LastErrorAt, Math.max(0L, GlobalUntil - Now), List.copyOf(Views), Running && Worker != null);
    }

    private static long millis() {
        return System.nanoTime() / 1000000L;
    }
}
