package net.rai.scp_unleashed.decor.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class StairsBlendGameTests {
    public static final String Arena = "floor_corner_arena";
    private static final String Mod = Scp_unleashed.ModId;
    private static final String Straight = "facing=east,half=bottom,shape=straight";
    private static final List<String> Zones = List.of("containment_room", "ez", "lcz", "hcz", "office", "research", "medical", "power", "greenhouse");
    private static final List<String> Materials = List.of("brick", "carpet_blue", "carpet_maroon", "cinder_block", "concrete", "diamond_plate", "epoxy_floor", "lab_tile", "medical_tile", "painted_concrete", "shower_tile");
    private static final List<String> Sides = List.of("north", "south", "west", "east");
    private static final List<String> Directions = List.of("up", "down", "north", "south", "west", "east");
    private static final Map<String, Map<String, String>> VanillaFaces = vanillaFaces();

    private record Blend(String Stair, String Full, String Slab, Set<String> Treads, Set<String> Faces, boolean Strict, boolean SharedTop) {
    }

    private StairsBlendGameTests() {
    }

    private static Map<String, Map<String, String>> vanillaFaces() {
        Map<String, Map<String, String>> Table = new HashMap<>();
        Map<String, String> TopBottomSide = Map.of("up", "top", "down", "bottom", "north", "side", "south", "side", "west", "side", "east", "side");
        for (String Name : List.of("stairs", "inner_stairs", "outer_stairs", "slab", "slab_top", "cube_bottom_top")) {
            Table.put("minecraft:block/" + Name, TopBottomSide);
        }
        Map<String, String> Column = Map.of("up", "end", "down", "end", "north", "side", "south", "side", "west", "side", "east", "side");
        Table.put("minecraft:block/cube_column", Column);
        Table.put("minecraft:block/cube_column_horizontal", Column);
        Map<String, String> All = new HashMap<>();
        for (String Direction : Directions) {
            All.put(Direction, "all");
        }
        Table.put("minecraft:block/cube_all", All);
        Map<String, String> Each = new HashMap<>();
        for (String Direction : Directions) {
            Each.put(Direction, Direction);
        }
        Table.put("minecraft:block/cube", Each);
        Map<String, String> Wall = Map.of("up", "wall", "down", "wall", "north", "wall", "south", "wall", "west", "wall", "east", "wall");
        for (String Name : List.of("template_wall_post", "template_wall_side", "template_wall_side_tall")) {
            Table.put("minecraft:block/" + Name, Wall);
        }
        return Table;
    }

    private static String texture(String Path) {
        return Mod + ":block/" + Path;
    }

    private static List<Blend> blends() {
        List<Blend> Rows = new ArrayList<>();
        for (String Zone : Zones) {
            String Folder = "decor/" + Zone + "/";
            Rows.add(new Blend(Zone + "_stairs", Zone + "_floor", Zone + "_slab", Set.of(texture(Folder + "stairs_nosing")), Set.of(texture(Folder + "stairs_side"), texture(Folder + "stairs_riser")), false, false));
            Rows.add(new Blend(Zone + "_wall_stairs", Zone + "_wall_middle", Zone + "_wall_slab", Set.of(texture(Folder + "wall_step_top")), Set.of(texture(Folder + "wall_step_side")), true, true));
        }
        for (String Name : Materials) {
            Rows.add(new Blend("facility_" + Name + "_stairs", "facility_" + Name, "facility_" + Name + "_slab", Set.of(), Set.of(), false, false));
        }
        Rows.add(new Blend("lunar_stone_stairs", "lunar_stone", "lunar_stone_slab", Set.of(), Set.of(), false, false));
        Rows.add(new Blend("lunar_stone_brick_stairs", "lunar_stone_bricks", "lunar_stone_brick_slab", Set.of(), Set.of(), false, false));
        Rows.add(new Blend("scp143_stairs", "scp143_planks", "scp143_slab", Set.of(), Set.of(), false, false));
        for (String Finish : List.of("", "_white_laminate", "_dark_veneer")) {
            Rows.add(new Blend("scp3008_plywood" + Finish + "_stairs", "scp3008_plywood" + Finish, "scp3008_plywood" + Finish + "_slab", Set.of(), Set.of(), false, false));
        }
        Rows.add(new Blend("scp087_stairs", "scp087_concrete", null, Set.of(texture("scp087/tread")), Set.of(texture("scp087/riser")), true, false));
        Rows.add(new Blend("catwalk_stairs", "catwalk", null, Set.of(texture("utility/stair_plate"), texture("utility/stair_tread_straight")),
                Set.of(texture("utility/stair_plate"), texture("utility/stair_nosing"), texture("utility/stair_edge")), true, false));
        return Rows;
    }

    private static JsonObject read(String Path) {
        try (InputStream Stream = StairsBlendGameTests.class.getResourceAsStream(Path)) {
            if (Stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception Problem) {
            throw new IllegalStateException("Could not read " + Path, Problem);
        }
    }

    private static boolean exists(String Path) {
        try (InputStream Stream = StairsBlendGameTests.class.getResourceAsStream(Path)) {
            return Stream != null;
        } catch (Exception Problem) {
            return false;
        }
    }

    private static String full(String Location) {
        return Location.contains(":") ? Location : "minecraft:" + Location;
    }

    private static String assetPath(String Location, String Kind, String Suffix) {
        String Whole = full(Location);
        int Colon = Whole.indexOf(':');
        return "/assets/" + Whole.substring(0, Colon) + "/" + Kind + "/" + Whole.substring(Colon + 1) + Suffix;
    }

    private static JsonObject blockstate(String Id) {
        return read("/assets/" + Mod + "/blockstates/" + Id + ".json");
    }

    private static final class Resolved {
        final Map<String, Set<String>> Faces = new LinkedHashMap<>();
        final List<String> Problems = new ArrayList<>();
        boolean Empty = true;
    }

    private static String resolve(Map<String, String> Textures, String Reference) {
        String Value = Reference;
        for (int Guard = 0; Guard < 16 && Value != null && Value.startsWith("#"); Guard++) {
            Value = Textures.get(Value.substring(1));
        }
        return Value == null || Value.startsWith("#") ? null : full(Value);
    }

    private static Resolved model(String Location) {
        Resolved Result = new Resolved();
        List<JsonObject> Chain = new ArrayList<>();
        String Current = full(Location);
        String Vanilla = null;
        while (Current != null) {
            if (Current.startsWith("minecraft:")) {
                Vanilla = Current;
                break;
            }
            JsonObject Raw = read(assetPath(Current, "models", ".json"));
            if (Raw == null) {
                Result.Problems.add("missing model " + Current);
                return Result;
            }
            Chain.add(Raw);
            Current = Raw.has("parent") ? full(Raw.get("parent").getAsString()) : null;
        }
        Map<String, String> Textures = new HashMap<>();
        JsonArray Elements = null;
        for (int Index = Chain.size() - 1; Index >= 0; Index--) {
            JsonObject Raw = Chain.get(Index);
            if (Raw.has("textures")) {
                for (Map.Entry<String, JsonElement> Entry : Raw.getAsJsonObject("textures").entrySet()) {
                    Textures.put(Entry.getKey(), Entry.getValue().getAsString());
                }
            }
            if (Raw.has("elements")) {
                Elements = Raw.getAsJsonArray("elements");
            }
        }
        if (Elements != null && !Elements.isEmpty()) {
            Result.Empty = false;
            for (JsonElement Element : Elements) {
                JsonObject Faces = Element.getAsJsonObject().getAsJsonObject("faces");
                if (Faces == null) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> Entry : Faces.entrySet()) {
                    String Reference = Entry.getValue().getAsJsonObject().get("texture").getAsString();
                    addFace(Result, Entry.getKey(), resolve(Textures, Reference), Location + " " + Entry.getKey() + " " + Reference);
                }
            }
        } else if (Vanilla != null && VanillaFaces.containsKey(Vanilla)) {
            Result.Empty = false;
            for (Map.Entry<String, String> Entry : VanillaFaces.get(Vanilla).entrySet()) {
                addFace(Result, Entry.getKey(), resolve(Textures, "#" + Entry.getValue()), Location + " " + Entry.getKey() + " #" + Entry.getValue());
            }
        }
        if (Result.Empty) {
            Result.Problems.add("no elements in " + Location + (Vanilla == null ? "" : " (parent " + Vanilla + ")"));
        }
        String Particle = resolve(Textures, "#particle");
        if (Particle == null && Vanilla == null) {
            Result.Problems.add("no particle texture in " + Location);
        }
        return Result;
    }

    private static void addFace(Resolved Result, String Direction, String Texture, String Where) {
        if (Texture == null) {
            Result.Problems.add("unresolved texture at " + Where);
            return;
        }
        if (Texture.startsWith(Mod + ":") && !exists(assetPath(Texture, "textures", ".png"))) {
            Result.Problems.add("missing texture " + Texture + " at " + Where);
            return;
        }
        Result.Faces.computeIfAbsent(Direction, Key -> new TreeSet<>()).add(Texture);
    }

    private static <T extends Comparable<T>> String valueName(BlockState State, Property<T> Key) {
        return Key.getName(State.getValue(Key));
    }

    private static boolean matches(String Key, BlockState State) {
        if (Key.isEmpty()) {
            return true;
        }
        Map<String, String> Values = new HashMap<>();
        for (Property<?> Each : State.getProperties()) {
            Values.put(Each.getName(), valueName(State, Each));
        }
        for (String Part : Key.split(",")) {
            String[] Pair = Part.split("=");
            if (Pair.length != 2 || !Pair[1].equals(Values.get(Pair[0]))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> variantModels(JsonElement Value) {
        List<String> Found = new ArrayList<>();
        if (Value.isJsonArray()) {
            for (JsonElement Each : Value.getAsJsonArray()) {
                Found.add(Each.getAsJsonObject().get("model").getAsString());
            }
        } else {
            Found.add(Value.getAsJsonObject().get("model").getAsString());
        }
        return Found;
    }

    private static void checkBlock(String Id, Block Target, List<String> Problems) {
        JsonObject State = blockstate(Id);
        if (State == null) {
            Problems.add(Id + ": no blockstate file");
            return;
        }
        Set<String> Models = new TreeSet<>();
        if (State.has("variants")) {
            JsonObject Variants = State.getAsJsonObject("variants");
            for (BlockState Each : Target.getStateDefinition().getPossibleStates()) {
                boolean Covered = false;
                for (Map.Entry<String, JsonElement> Entry : Variants.entrySet()) {
                    if (matches(Entry.getKey(), Each)) {
                        Models.addAll(variantModels(Entry.getValue()));
                        Covered = true;
                        break;
                    }
                }
                if (!Covered) {
                    Problems.add(Id + ": no blockstate variant for " + Each);
                    return;
                }
            }
        } else if (State.has("multipart")) {
            for (JsonElement Part : State.getAsJsonArray("multipart")) {
                Models.addAll(variantModels(Part.getAsJsonObject().get("apply")));
            }
        }
        if (Models.isEmpty()) {
            Problems.add(Id + ": the blockstate names no models");
        }
        for (String Location : Models) {
            for (String Problem : model(Location).Problems) {
                Problems.add(Id + ": " + Problem);
            }
        }
    }

    private static Block block(String Id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(Mod, Id)).orElse(null);
    }

    private static String fullVariant(String Id) {
        JsonObject State = blockstate(Id);
        if (State == null || !State.has("variants")) {
            return null;
        }
        JsonObject Variants = State.getAsJsonObject("variants");
        for (String Preferred : List.of("", "axis=y")) {
            if (Variants.has(Preferred)) {
                return variantModels(Variants.get(Preferred)).get(0);
            }
        }
        Map.Entry<String, JsonElement> First = Variants.entrySet().iterator().next();
        return variantModels(First.getValue()).get(0);
    }

    private static String shapeModel(String Id, String Key) {
        JsonObject State = blockstate(Id);
        if (State == null || !State.has("variants") || !State.getAsJsonObject("variants").has(Key)) {
            return null;
        }
        return variantModels(State.getAsJsonObject("variants").get(Key)).get(0);
    }

    private static Set<String> faces(Resolved Result, List<String> Wanted) {
        Set<String> Found = new TreeSet<>();
        for (String Direction : Wanted) {
            Found.addAll(Result.Faces.getOrDefault(Direction, Set.of()));
        }
        return Found;
    }

    private static void checkShape(Blend Row, String Id, String Location, Resolved Base, List<String> Problems) {
        if (Location == null) {
            Problems.add(Id + ": no model for its plain shape");
            return;
        }
        Resolved Shape = model(Location);
        Problems.addAll(Shape.Problems.stream().map(Problem -> Id + ": " + Problem).toList());
        Set<String> Treads = new HashSet<>(Row.Treads());
        if (!Row.Strict()) {
            Treads.addAll(faces(Base, List.of("up")));
        }
        for (String Tread : faces(Shape, List.of("up"))) {
            if (!Treads.contains(Tread)) {
                Problems.add(Id + ": tread " + Tread + " is neither " + Row.Full() + "'s top nor a documented stair texture " + Row.Treads());
            }
        }
        Set<String> Walls = new HashSet<>(Row.Faces());
        Walls.addAll(faces(Base, Sides));
        for (String Side : faces(Shape, Sides)) {
            if (!Walls.contains(Side)) {
                Problems.add(Id + ": side " + Side + " is neither " + Row.Full() + "'s side nor a documented stair texture " + Row.Faces());
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "stairs_blend_models")
    public static void stairsBlendModels(GameTestHelper Helper) {
        List<String> Problems = new ArrayList<>();
        Set<String> Stairs = new TreeSet<>();
        for (Block Each : BuiltInRegistries.BLOCK) {
            ResourceLocation Key = BuiltInRegistries.BLOCK.getKey(Each);
            if (Key.getNamespace().equals(Mod) && Each instanceof StairBlock) {
                Stairs.add(Key.getPath());
                checkBlock(Key.getPath(), Each, Problems);
            }
        }
        Set<String> Extra = new TreeSet<>();
        for (Blend Row : blends()) {
            Helper.assertTrue(Stairs.contains(Row.Stair()), Row.Stair() + " is not a registered stair block");
            Extra.add(Row.Full());
            if (Row.Slab() != null) {
                Extra.add(Row.Slab());
            }
        }
        Extra.add("lunar_stone_wall");
        Extra.add("lunar_stone_brick_wall");
        for (String Id : Extra) {
            Block Target = block(Id);
            if (Target == null) {
                Problems.add(Id + ": not registered");
                continue;
            }
            checkBlock(Id, Target, Problems);
        }
        Helper.assertTrue(Stairs.size() >= 37, "Only " + Stairs.size() + " stair blocks were found");
        Helper.assertTrue(Problems.isEmpty(), Problems.size() + " stair model problems, first: " + String.join(" | ", Problems.subList(0, Math.min(6, Problems.size()))));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "stairs_blend_treads")
    public static void stairsBlendTreads(GameTestHelper Helper) {
        List<String> Problems = new ArrayList<>();
        for (Blend Row : blends()) {
            for (String Documented : Row.Treads()) {
                Helper.assertTrue(exists(assetPath(Documented, "textures", ".png")), Row.Stair() + ": documented stair texture " + Documented + " is missing");
            }
            for (String Documented : Row.Faces()) {
                Helper.assertTrue(exists(assetPath(Documented, "textures", ".png")), Row.Stair() + ": documented stair texture " + Documented + " is missing");
            }
            String FullModel = fullVariant(Row.Full());
            if (FullModel == null) {
                Problems.add(Row.Full() + ": no full block model");
                continue;
            }
            Resolved Base = model(FullModel);
            if (Row.SharedTop()) {
                Set<String> Ends = faces(Base, List.of("up", "down"));
                if (!Ends.equals(new TreeSet<>(Row.Treads()))) {
                    Problems.add(Row.Full() + ": top and bottom " + Ends + " do not match the stair tread " + Row.Treads());
                }
            }
            checkShape(Row, Row.Stair(), shapeModel(Row.Stair(), Straight), Base, Problems);
            if (Row.Slab() != null) {
                checkShape(Row, Row.Slab(), shapeModel(Row.Slab(), "type=bottom"), Base, Problems);
                checkShape(Row, Row.Slab(), shapeModel(Row.Slab(), "type=top"), Base, Problems);
            }
        }
        Helper.assertTrue(Problems.isEmpty(), Problems.size() + " stair blend problems, first: " + String.join(" | ", Problems.subList(0, Math.min(6, Problems.size()))));
        Helper.succeed();
    }
}
