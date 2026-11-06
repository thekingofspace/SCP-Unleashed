package net.rai.scp_unleashed.scp439.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.rai.scp_unleashed.scp439.Scp439Registry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp439GrowthShapes {
    public static final String Humanoid = "humanoid";
    public static final String Slim = "slim";
    public static final String Villager = "villager";
    public static final float MinimumSpan = 0.05f;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<String, Shape> Loaded = new HashMap<>();

    private Scp439GrowthShapes() {
    }

    public record Cube(int Tier, float Delay, float Span, float[] Origin, float[] Size, float[] Pivot, float[] Anchor, float[] Rotation, int U, int V, boolean Jaw) {
    }

    public record Part(float[] Pivot, List<Cube> Cubes) {
    }

    public record Shape(ResourceLocation Sprite, int Width, int Height, Map<String, Part> Parts, String JawPart, float[] JawPivot) {
    }

    public static void clear() {
        Loaded.clear();
    }

    @Nullable
    public static Shape get(String Name) {
        if (Loaded.containsKey(Name)) {
            return Loaded.get(Name);
        }
        Shape Made = load(Name);
        Loaded.put(Name, Made);
        return Made;
    }

    private static float[] floats(JsonElement Element) {
        JsonArray Values = Element.getAsJsonArray();
        float[] Result = new float[Values.size()];
        for (int Index = 0; Index < Values.size(); Index++) {
            Result[Index] = Values.get(Index).getAsFloat();
        }
        return Result;
    }

    public static Shape parse(JsonObject Root) {
        Map<String, Part> Parts = new HashMap<>();
        JsonObject PartList = Root.getAsJsonObject("parts");
        for (Map.Entry<String, JsonElement> Entry : PartList.entrySet()) {
            JsonObject Body = Entry.getValue().getAsJsonObject();
            List<Cube> Cubes = new ArrayList<>();
            for (JsonElement Element : Body.getAsJsonArray("cubes")) {
                JsonObject Spec = Element.getAsJsonObject();
                float[] Uv = floats(Spec.get("uv"));
                float Delay = Spec.get("delay").getAsFloat();
                float Span = Spec.has("span") ? Spec.get("span").getAsFloat() : 1.0f - Delay;
                float[] Pivot = floats(Spec.get("pivot"));
                float[] Anchor = Spec.has("anchor") ? floats(Spec.get("anchor")) : Pivot;
                boolean Jaw = Spec.has("group") && "jaw".equals(Spec.get("group").getAsString());
                Cubes.add(new Cube(Spec.get("tier").getAsInt(), Delay, Math.max(MinimumSpan, Span), floats(Spec.get("origin")), floats(Spec.get("size")), Pivot, Anchor,
                        floats(Spec.get("rotation")), (int) Uv[0], (int) Uv[1], Jaw));
            }
            Parts.put(Entry.getKey(), new Part(floats(Body.get("pivot")), List.copyOf(Cubes)));
        }
        ResourceLocation Sprite = ResourceLocation.parse(Root.get("texture").getAsString());
        String JawPart = "";
        float[] JawPivot = new float[3];
        if (Root.has("jaw")) {
            JsonObject Hinge = Root.getAsJsonObject("jaw");
            JawPart = Hinge.get("part").getAsString();
            JawPivot = floats(Hinge.get("pivot"));
        }
        return new Shape(Sprite, Root.get("width").getAsInt(), Root.get("height").getAsInt(), Map.copyOf(Parts), JawPart, JawPivot);
    }

    @Nullable
    private static Shape load(String Name) {
        ResourceLocation Location = Scp439Registry.location("scp439/growth/" + Name + ".json");
        Optional<Resource> Found = Minecraft.getInstance().getResourceManager().getResource(Location);
        if (Found.isEmpty()) {
            Log.warn("SCP-439 growth shape {} is missing", Location);
            return null;
        }
        try (Reader Text = Found.get().openAsReader()) {
            return parse(JsonParser.parseReader(Text).getAsJsonObject());
        } catch (Exception Problem) {
            Log.error("Could not read SCP-439 growth shape {}", Location, Problem);
            return null;
        }
    }
}
