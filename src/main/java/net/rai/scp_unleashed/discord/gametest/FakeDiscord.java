package net.rai.scp_unleashed.discord.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class FakeDiscord implements AutoCloseable {
    private static final AtomicInteger Threads = new AtomicInteger();
    private final HttpServer Server;
    private final ExecutorService Workers;
    private final List<Hit> Hits = new CopyOnWriteArrayList<>();
    private final Map<String, Deque<Scripted>> Scripts = new ConcurrentHashMap<>();

    public record Hit(String Path, String Body, String UserAgent, String ContentType, long AtMillis) {
        public JsonObject json() {
            return JsonParser.parseString(Body).getAsJsonObject();
        }

        public JsonObject embed() {
            return json().getAsJsonArray("embeds").get(0).getAsJsonObject();
        }
    }

    public record Scripted(int Status, String Body, Map<String, String> Headers) {
        public static Scripted ok() {
            return new Scripted(204, "", Map.of());
        }
    }

    private FakeDiscord(HttpServer Server, ExecutorService Workers) {
        this.Server = Server;
        this.Workers = Workers;
    }

    public static FakeDiscord start() {
        try {
            HttpServer Made = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 16);
            ExecutorService Pool = Executors.newFixedThreadPool(2, Task -> {
                Thread Spun = new Thread(Task, "Fake Discord " + Threads.incrementAndGet());
                Spun.setDaemon(true);
                return Spun;
            });
            FakeDiscord Fake = new FakeDiscord(Made, Pool);
            Made.createContext("/", Fake::handle);
            Made.setExecutor(Pool);
            Made.start();
            return Fake;
        } catch (IOException Problem) {
            throw new IllegalStateException("could not open the fake Discord server", Problem);
        }
    }

    public int port() {
        return Server.getAddress().getPort();
    }

    public String url(String Name) {
        return "http://127.0.0.1:" + port() + path(Name);
    }

    public static String path(String Name) {
        return "/api/webhooks/" + (100000 + Math.abs(Name.hashCode() % 899999)) + "/" + Name;
    }

    public void script(String Name, Scripted... Replies) {
        Deque<Scripted> Waiting = Scripts.computeIfAbsent(path(Name), Key -> new ArrayDeque<>());
        synchronized (Waiting) {
            Waiting.addAll(List.of(Replies));
        }
    }

    public List<Hit> hits() {
        return List.copyOf(Hits);
    }

    public List<Hit> hits(String Name) {
        String Wanted = path(Name);
        List<Hit> Found = new ArrayList<>();
        for (Hit Entry : Hits) {
            if (Entry.Path().equals(Wanted)) {
                Found.add(Entry);
            }
        }
        return Found;
    }

    public List<Hit> titled(String Name, String Fragment) {
        List<Hit> Found = new ArrayList<>();
        for (Hit Entry : hits(Name)) {
            JsonObject Embed = Entry.embed();
            if (Embed.has("title") && Embed.get("title").getAsString().contains(Fragment)) {
                Found.add(Entry);
            }
        }
        return Found;
    }

    private void handle(HttpExchange Exchange) throws IOException {
        String Body;
        try (InputStream In = Exchange.getRequestBody()) {
            Body = new String(In.readAllBytes(), StandardCharsets.UTF_8);
        }
        String RawPath = Exchange.getRequestURI().getRawPath();
        Hits.add(new Hit(RawPath, Body, String.valueOf(Exchange.getRequestHeaders().getFirst("User-Agent")),
                String.valueOf(Exchange.getRequestHeaders().getFirst("Content-Type")), System.nanoTime() / 1000000L));
        Scripted Answer = Scripted.ok();
        Deque<Scripted> Waiting = Scripts.get(RawPath);
        if (Waiting != null) {
            synchronized (Waiting) {
                if (!Waiting.isEmpty()) {
                    Answer = Waiting.pollFirst();
                }
            }
        }
        for (Map.Entry<String, String> Header : Answer.Headers().entrySet()) {
            Exchange.getResponseHeaders().add(Header.getKey(), Header.getValue());
        }
        byte[] Bytes = Answer.Body().getBytes(StandardCharsets.UTF_8);
        if (!Answer.Body().isEmpty()) {
            Exchange.getResponseHeaders().add("Content-Type", "application/json");
        }
        Exchange.sendResponseHeaders(Answer.Status(), Bytes.length == 0 ? -1 : Bytes.length);
        if (Bytes.length > 0) {
            try (OutputStream Out = Exchange.getResponseBody()) {
                Out.write(Bytes);
            }
        }
        Exchange.close();
    }

    @Override
    public void close() {
        Server.stop(0);
        Workers.shutdownNow();
    }
}
