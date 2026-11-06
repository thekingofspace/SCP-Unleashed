package net.rai.scp_unleashed.archive.document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.investigation.DormantCondition;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AnomalyDocuments {
    public static final String Directory = "anomaly_document";
    public static final Comparator<Map.Entry<ResourceLocation, AnomalyDocument>> FileOrder = Comparator
            .<Map.Entry<ResourceLocation, AnomalyDocument>>comparingInt(Listed -> Listed.getValue().Classification().ordinal())
            .thenComparing(Listed -> Listed.getValue().Lang())
            .thenComparing(Map.Entry::getKey);
    private static final Logger Log = LogUtils.getLogger();
    private static final Gson Parser = new GsonBuilder().create();

    private static volatile Map<ResourceLocation, AnomalyDocument> ServerDocuments = Map.of();
    private static volatile Map<ResourceLocation, AnomalyDocument> ClientDocuments = Map.of();

    private AnomalyDocuments() {
    }

    public static Map<ResourceLocation, AnomalyDocument> server() {
        return ServerDocuments;
    }

    public static Map<ResourceLocation, AnomalyDocument> client() {
        return ClientDocuments;
    }

    public static Map<ResourceLocation, AnomalyDocument> forSide(boolean ClientSide) {
        return ClientSide ? ClientDocuments : ServerDocuments;
    }

    @Nullable
    public static AnomalyDocument get(Level World, ResourceLocation Id) {
        return forSide(World.isClientSide).get(Id);
    }

    @Nullable
    public static AnomalyDocument lookup(ResourceLocation Id) {
        AnomalyDocument Found = ClientDocuments.get(Id);
        return Found != null ? Found : ServerDocuments.get(Id);
    }

    public static void clearServer() {
        ServerDocuments = Map.of();
    }

    public static void acceptClient(Map<ResourceLocation, AnomalyDocument> Documents) {
        ClientDocuments = sorted(Documents);
    }

    public static void clearClient() {
        ClientDocuments = Map.of();
    }

    public static Map<ResourceLocation, AnomalyDocument> sorted(Map<ResourceLocation, AnomalyDocument> Documents) {
        Map<ResourceLocation, AnomalyDocument> Ordered = new LinkedHashMap<>();
        List<Map.Entry<ResourceLocation, AnomalyDocument>> Entries = Documents.entrySet().stream().sorted(FileOrder).toList();
        for (Map.Entry<ResourceLocation, AnomalyDocument> Listed : Entries) {
            Ordered.put(Listed.getKey(), Listed.getValue());
        }
        return Collections.unmodifiableMap(Ordered);
    }

    public static class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(Parser, Directory);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> Files, ResourceManager Resources, ProfilerFiller Profiler) {
            Map<ResourceLocation, AnomalyDocument> Loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> Source : Files.entrySet()) {
                AnomalyDocument.DocumentCodec.parse(JsonOps.INSTANCE, Source.getValue())
                        .resultOrPartial(Problem -> Log.error("Skipping anomaly document {}: {}", Source.getKey(), Problem))
                        .map(Parsed -> Parsed.Lang().isEmpty() ? Parsed.withLang(AnomalyDocument.defaultLang(Source.getKey())) : Parsed)
                        .ifPresent(Parsed -> {
                            if (Parsed.Condition() instanceof DormantCondition Dormant) {
                                Log.warn("Anomaly document {} uses unknown start condition type '{}' (known: {}); its investigations will stay open with no leads",
                                        Source.getKey(), Dormant.TypeName(), StartConditions.registeredTypes());
                            }
                            Loaded.put(Source.getKey(), Parsed);
                        });
            }
            ServerDocuments = sorted(Loaded);
            Log.info("Loaded {} anomaly documents", Loaded.size());
        }
    }
}
