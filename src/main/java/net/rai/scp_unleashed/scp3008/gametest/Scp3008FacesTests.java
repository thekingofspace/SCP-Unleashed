package net.rai.scp_unleashed.scp3008.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008FacesTests {
    public static final String Arena = "gun_perf_arena";
    public static final List<String> VanillaCopies = List.of("block/scp3008/plywood/plywood_door_", "block/scp3008/plywood/plywood_trapdoor_");

    private Scp3008FacesTests() {
    }

    private static JsonObject read(String Location) {
        String[] Parts = Location.split("/");
        Path Found = ModList.get().getModFileById(Scp_unleashed.ModId).getFile().findResource(Parts);
        if (!Files.exists(Found)) {
            return null;
        }
        try (Reader Source = Files.newBufferedReader(Found, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(Source).getAsJsonObject();
        } catch (Exception Failure) {
            return null;
        }
    }

    private static Map<String, String> properties(String Key) {
        Map<String, String> Result = new HashMap<>();
        for (String Pair : Key.split(",")) {
            int Split = Pair.indexOf('=');
            if (Split > 0) {
                Result.put(Pair.substring(0, Split), Pair.substring(Split + 1));
            }
        }
        return Result;
    }

    private static List<String> models(JsonElement Entry) {
        List<String> Result = new ArrayList<>();
        if (Entry.isJsonArray()) {
            for (JsonElement Each : Entry.getAsJsonArray()) {
                Result.add(Each.getAsJsonObject().get("model").getAsString());
            }
        } else {
            Result.add(Entry.getAsJsonObject().get("model").getAsString());
        }
        return Result;
    }

    private static int checkModel(String Owner, String Model, String Half, List<String> Problems) {
        ResourceLocation Location = ResourceLocation.parse(Model);
        if (!Location.getNamespace().equals(Scp_unleashed.ModId)) {
            return 0;
        }
        JsonObject Data = read("assets/" + Scp_unleashed.ModId + "/models/" + Location.getPath() + ".json");
        if (Data == null) {
            Problems.add(Owner + " points at missing model " + Model);
            return 0;
        }
        if (!Data.has("elements")) {
            return 1;
        }
        boolean Copy = VanillaCopies.stream().anyMatch(Location.getPath()::startsWith);
        for (JsonElement Box : Data.getAsJsonArray("elements")) {
            JsonObject Faces = Box.getAsJsonObject().getAsJsonObject("faces");
            for (Map.Entry<String, JsonElement> Face : Faces.entrySet()) {
                JsonObject Value = Face.getValue().getAsJsonObject();
                if (!Value.has("cullface")) {
                    continue;
                }
                String Cull = Value.get("cullface").getAsString();
                boolean TowardPartner = ("lower".equals(Half) && Cull.equals("up")) || ("upper".equals(Half) && Cull.equals("down"));
                if (!Copy || TowardPartner) {
                    Problems.add(Model + " " + Face.getKey() + " culls " + Cull);
                }
            }
        }
        return 1;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3008_faces_models")
    public static void furnitureModelsNeverCullAgainstNeighbours(GameTestHelper Helper) {
        List<String> Problems = new ArrayList<>();
        int Checked = 0;
        for (ResourceLocation Id : BuiltInRegistries.BLOCK.keySet()) {
            if (!Id.getNamespace().equals(Scp_unleashed.ModId) || !Id.getPath().startsWith("scp3008_")) {
                continue;
            }
            JsonObject State = read("assets/" + Scp_unleashed.ModId + "/blockstates/" + Id.getPath() + ".json");
            if (State == null) {
                continue;
            }
            if (State.has("variants")) {
                for (Map.Entry<String, JsonElement> Variant : State.getAsJsonObject("variants").entrySet()) {
                    String Half = properties(Variant.getKey()).get("half");
                    for (String Model : models(Variant.getValue())) {
                        Checked += checkModel(Id.getPath(), Model, Half, Problems);
                    }
                }
            }
            if (State.has("multipart")) {
                for (JsonElement Part : State.getAsJsonArray("multipart")) {
                    for (String Model : models(Part.getAsJsonObject().get("apply"))) {
                        Checked += checkModel(Id.getPath(), Model, null, Problems);
                    }
                }
            }
        }
        Helper.assertTrue(Checked > 400, "only " + Checked + " SCP-3008 block model references were checked");
        Helper.assertTrue(Problems.isEmpty(), Problems.size() + " SCP-3008 model faces cull against a neighbour that is not guaranteed full: "
                + String.join("; ", Problems.subList(0, Math.min(6, Problems.size()))));
        Helper.succeed();
    }
}
