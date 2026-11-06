package net.rai.scp_unleashed.scp217.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp217Layout {
    public static final ResourceLocation Location = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217/clockwork.json");
    public static final List<String> ModelNames = List.of("gear_tiny", "gear_small", "gear_medium", "plate_brass", "plate_steel", "spring", "spike", "key");
    private static final Logger Log = LogUtils.getLogger();
    private static Map<String, Shape> Shapes = Map.of();
    private static List<Piece> Pieces = List.of();

    private Scp217Layout() {
    }

    public record Shape(ModelResourceLocation Model, ModelResourceLocation PerfectModel, String Axis, Vector3f Pivot) {
        public ModelResourceLocation pick(boolean Perfect) {
            return Perfect ? PerfectModel : Model;
        }
    }

    public record Piece(String Model, String Part, int Stage, Vector3f At, Vector3f Rotation, float Step, float Roll, boolean Armour, boolean FirstPerson, Vector3f Slim) {
    }

    public static ModelResourceLocation model(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp217/" + Name));
    }

    public static ModelResourceLocation perfectModel(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp217/perfect/" + Name));
    }

    public static List<ModelResourceLocation> models() {
        List<ModelResourceLocation> Found = new ArrayList<>();
        for (String Name : ModelNames) {
            Found.add(model(Name));
            Found.add(perfectModel(Name));
        }
        return Found;
    }

    public static List<Piece> pieces() {
        return Pieces;
    }

    @Nullable
    public static Shape shape(String Name) {
        return Shapes.get(Name);
    }

    public static void reload(ResourceManager Manager) {
        Optional<Resource> Found = Manager.getResource(Location);
        if (Found.isEmpty()) {
            Shapes = Map.of();
            Pieces = List.of();
            Log.warn("SCP-217 clockwork layout {} is missing", Location);
            return;
        }
        try (Reader Input = Found.get().openAsReader()) {
            JsonObject Root = JsonParser.parseReader(Input).getAsJsonObject();
            Map<String, Shape> NewShapes = new HashMap<>();
            JsonObject Models = GsonHelper.getAsJsonObject(Root, "models");
            for (Map.Entry<String, JsonElement> Entry : Models.entrySet()) {
                if (!ModelNames.contains(Entry.getKey())) {
                    continue;
                }
                JsonObject Data = Entry.getValue().getAsJsonObject();
                NewShapes.put(Entry.getKey(), new Shape(model(Entry.getKey()), perfectModel(Entry.getKey()), GsonHelper.getAsString(Data, "axis", "z"), vector(Data, "pivot")));
            }
            List<Piece> NewPieces = new ArrayList<>();
            for (JsonElement Element : GsonHelper.getAsJsonArray(Root, "pieces")) {
                JsonObject Data = Element.getAsJsonObject();
                NewPieces.add(new Piece(GsonHelper.getAsString(Data, "model"), GsonHelper.getAsString(Data, "part"), GsonHelper.getAsInt(Data, "stage", 4),
                        vector(Data, "at"), vector(Data, "rotation"), GsonHelper.getAsFloat(Data, "step", 0.0f), GsonHelper.getAsFloat(Data, "roll", 0.0f),
                        GsonHelper.getAsBoolean(Data, "armour", false), GsonHelper.getAsBoolean(Data, "first_person", false), vector(Data, "slim")));
            }
            Shapes = Map.copyOf(NewShapes);
            Pieces = List.copyOf(NewPieces);
        } catch (Exception Problem) {
            Shapes = Map.of();
            Pieces = List.of();
            Log.warn("SCP-217 clockwork layout {} could not be read", Location, Problem);
        }
    }

    private static Vector3f vector(JsonObject Data, String Key) {
        if (!Data.has(Key)) {
            return new Vector3f();
        }
        JsonArray Values = GsonHelper.getAsJsonArray(Data, Key);
        return new Vector3f(Values.get(0).getAsFloat(), Values.get(1).getAsFloat(), Values.get(2).getAsFloat());
    }
}
