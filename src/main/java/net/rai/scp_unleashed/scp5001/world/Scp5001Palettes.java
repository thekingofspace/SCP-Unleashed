package net.rai.scp_unleashed.scp5001.world;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scp5001Palettes {
    public static final String Folder = "scp5001_palette";
    private static final Logger Log = LogUtils.getLogger();
    private static volatile Map<String, Role> Roles = Map.of();

    public record Choice(ResourceLocation Block, int Weight) {
    }

    public record Role(String Name, String Kind, List<Choice> Entries, int Total) {
        public ResourceLocation pick(long Hash) {
            if (Entries.isEmpty() || Total <= 0) {
                return null;
            }
            long Roll = Math.floorMod(Hash, Total);
            for (Choice Each : Entries) {
                Roll -= Each.Weight();
                if (Roll < 0) {
                    return Each.Block();
                }
            }
            return Entries.get(Entries.size() - 1).Block();
        }
    }

    private Scp5001Palettes() {
    }

    @Nullable
    public static Role role(String Name) {
        return Roles.get(Name);
    }

    public static Map<String, Role> roles() {
        return Roles;
    }

    public static void replace(Map<String, Role> Fresh) {
        Roles = Map.copyOf(Fresh);
    }

    public static Map<String, Role> parse(Map<ResourceLocation, JsonElement> Files) {
        Map<String, Role> Made = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> File : Files.entrySet()) {
            try {
                JsonObject Root = File.getValue().getAsJsonObject().getAsJsonObject("roles");
                for (Map.Entry<String, JsonElement> Entry : Root.entrySet()) {
                    JsonObject Body = Entry.getValue().getAsJsonObject();
                    String Kind = Body.has("kind") ? Body.get("kind").getAsString() : "floor";
                    List<Choice> Choices = new ArrayList<>();
                    int Total = 0;
                    JsonArray Entries = Body.getAsJsonArray("entries");
                    for (JsonElement Each : Entries) {
                        JsonObject Item = Each.getAsJsonObject();
                        ResourceLocation Id = ResourceLocation.tryParse(Item.get("block").getAsString());
                        int Weight = Item.has("weight") ? Math.max(1, Item.get("weight").getAsInt()) : 1;
                        if (Id != null) {
                            Choices.add(new Choice(Id, Weight));
                            Total += Weight;
                        }
                    }
                    Role Existing = Made.get(Entry.getKey());
                    if (Existing != null) {
                        List<Choice> Merged = new ArrayList<>(Existing.Entries());
                        Merged.addAll(Choices);
                        Made.put(Entry.getKey(), new Role(Entry.getKey(), Existing.Kind(), List.copyOf(Merged), Existing.Total() + Total));
                    } else {
                        Made.put(Entry.getKey(), new Role(Entry.getKey(), Kind, List.copyOf(Choices), Total));
                    }
                }
            } catch (RuntimeException Problem) {
                Log.warn("SCP-5001 palette {} could not be read", File.getKey(), Problem);
            }
        }
        return Made;
    }

    public static class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(new Gson(), Folder);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> Files, ResourceManager Manager, ProfilerFiller Profiler) {
            replace(parse(Files));
            Log.info("SCP-5001 loaded {} prop roles", Roles.size());
        }
    }
}
