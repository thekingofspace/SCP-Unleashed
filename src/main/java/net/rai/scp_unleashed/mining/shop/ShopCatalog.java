package net.rai.scp_unleashed.mining.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopCatalog {
    public static final String Directory = "shop";
    private static final Logger Log = LogUtils.getLogger();
    private static final Gson Parser = new GsonBuilder().create();
    private static final Comparator<Map.Entry<ResourceLocation, ShopEntry>> Ordering = Comparator
            .<Map.Entry<ResourceLocation, ShopEntry>>comparingInt(Listed -> Listed.getValue().Order())
            .thenComparing(Map.Entry::getKey);

    private static volatile Map<ResourceLocation, ShopEntry> Entries = Map.of();

    private ShopCatalog() {
    }

    public static Map<ResourceLocation, ShopEntry> entries() {
        return Entries;
    }

    @Nullable
    public static ShopEntry get(ResourceLocation Id) {
        return Entries.get(Id);
    }

    public static void clear() {
        Entries = Map.of();
    }

    @Nullable
    static String problem(ShopEntry Entry) {
        if (Entry.Price() < 0L || Entry.Price() > ShopEntry.MaxPrice) {
            return "price must be between 0 and " + ShopEntry.MaxPrice;
        }
        if (Entry.Kind() == ShopKind.Item && (Entry.Goods().isEmpty() || Entry.Goods().get().isEmpty())) {
            return "an item entry needs an \"item\"";
        }
        if (Entry.Kind() == ShopKind.Entity) {
            if (Entry.Creature().isEmpty()) {
                return "an entity entry needs an \"entity\"";
            }
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(Entry.Creature().get())) {
                return "unknown entity type " + Entry.Creature().get();
            }
        }
        return null;
    }

    public static class Loader extends SimpleJsonResourceReloadListener {
        private final HolderLookup.Provider Registries;

        public Loader(HolderLookup.Provider Registries) {
            super(Parser, Directory);
            this.Registries = Registries;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> Files, ResourceManager Resources, ProfilerFiller Profiler) {
            RegistryOps<JsonElement> Ops = RegistryOps.create(JsonOps.INSTANCE, Registries);
            Map<ResourceLocation, ShopEntry> Loaded = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> Source : Files.entrySet()) {
                ShopEntry.EntryCodec.parse(Ops, Source.getValue())
                        .resultOrPartial(Problem -> Log.error("Skipping shop entry {}: {}", Source.getKey(), Problem))
                        .ifPresent(Parsed -> {
                            String Problem = problem(Parsed);
                            if (Problem != null) {
                                Log.error("Skipping shop entry {}: {}", Source.getKey(), Problem);
                                return;
                            }
                            Loaded.put(Source.getKey(), Parsed);
                        });
            }
            List<Map.Entry<ResourceLocation, ShopEntry>> Sorted = Loaded.entrySet().stream().sorted(Ordering).toList();
            Map<ResourceLocation, ShopEntry> Ordered = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, ShopEntry> Listed : Sorted) {
                Ordered.put(Listed.getKey(), Listed.getValue());
            }
            Entries = Collections.unmodifiableMap(Ordered);
            Log.info("Loaded {} shop entries", Ordered.size());
        }
    }
}
