package net.rai.scp_unleashed.scp1730.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.rai.scp_unleashed.Scp_unleashed;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp1730ModelTops {
    public static final double Rod = 1.0;
    public static final double Half = 2.0;
    public static final Set<String> FullParents = Set.of("minecraft:block/cube", "minecraft:block/cube_all", "minecraft:block/cube_column", "minecraft:block/cube_bottom_top",
            "minecraft:block/orientable", "minecraft:block/orientable_with_bottom", "minecraft:block/orientable_vertical", "minecraft:block/cube_directional",
            "minecraft:block/cube_mirrored_all", "minecraft:block/cube_mirrored", "minecraft:block/cube_column_horizontal", "minecraft:block/cube_column_mirrored",
            "minecraft:block/cube_top", "minecraft:block/leaves", "minecraft:block/cube_north_west_mirrored_all");
    private static final Map<String, List<Part>> Elements = new ConcurrentHashMap<>();
    private static final List<Part> Unknown = List.of();
    private static final List<Part> Full = List.of(new Part(new double[]{0, 0, 0}, new double[]{16, 16, 16}, identity(), new double[]{0, 0, 0}));

    public record Top(boolean Known, double Value, double Peak) {
        @Override
        public String toString() {
            return Known ? Scp1730ItemGameTests.fmt(Value) : "unknown";
        }
    }

    private record Part(double[] From, double[] To, double[][] Turn, double[] Origin) {
    }

    private record Variant(List<Part> Parts, double[][] Turn) {
    }

    private Scp1730ModelTops() {
    }

    public static Top under(BlockState State, double CentreX, double CentreZ) {
        List<Variant> Chosen = variants(State);
        if (Chosen == null) {
            return new Top(false, Double.NaN, Double.NaN);
        }
        double Best = Double.NaN;
        double Peak = Double.NaN;
        double Low = CentreX * 16.0 - Half;
        double LowZ = CentreZ * 16.0 - Half;
        for (int Px = 0; Px < 4; Px++) {
            for (int Pz = 0; Pz < 4; Pz++) {
                double Found = column(Chosen, Low + Px + 0.5, LowZ + Pz + 0.5);
                if (!Double.isNaN(Found)) {
                    Best = Double.isNaN(Best) ? Found : Math.max(Best, Found);
                }
            }
        }
        for (int X = 0; X < 16; X++) {
            for (int Z = 0; Z < 16; Z++) {
                double Found = column(Chosen, X + 0.5, Z + 0.5);
                if (!Double.isNaN(Found)) {
                    Peak = Double.isNaN(Peak) ? Found : Math.max(Peak, Found);
                }
            }
        }
        return new Top(true, Best, Peak);
    }

    private static List<Variant> variants(BlockState State) {
        ResourceLocation Key = BuiltInRegistries.BLOCK.getKey(State.getBlock());
        JsonObject Definition = read(Key.getNamespace(), "blockstates/" + Key.getPath() + ".json");
        if (Definition == null) {
            return null;
        }
        Map<String, String> Values = values(State);
        List<JsonObject> Uses = new ArrayList<>();
        if (Definition.has("variants")) {
            for (Map.Entry<String, JsonElement> Entry : Definition.getAsJsonObject("variants").entrySet()) {
                if (fits(Entry.getKey(), Values)) {
                    Uses.add(first(Entry.getValue()));
                    break;
                }
            }
        } else if (Definition.has("multipart")) {
            for (JsonElement Piece : Definition.getAsJsonArray("multipart")) {
                JsonObject Part0 = Piece.getAsJsonObject();
                if (!Part0.has("when") || when(Part0.getAsJsonObject("when"), Values)) {
                    Uses.add(first(Part0.get("apply")));
                }
            }
        }
        if (Uses.isEmpty()) {
            return null;
        }
        List<Variant> Made = new ArrayList<>();
        for (JsonObject Use : Uses) {
            List<Part> Parts = elements(Use.get("model").getAsString(), 0);
            if (Parts == Unknown) {
                return null;
            }
            double AngleX = Use.has("x") ? Use.get("x").getAsDouble() : 0.0;
            double AngleY = Use.has("y") ? Use.get("y").getAsDouble() : 0.0;
            Made.add(new Variant(Parts, times(rotation('x', AngleX), rotation('y', AngleY))));
        }
        return Made;
    }

    private static double column(List<Variant> Chosen, double X, double Z) {
        double Best = Double.NaN;
        for (Variant Each : Chosen) {
            double[] Direction = apply(Each.Turn(), new double[]{0.0, 1.0, 0.0});
            double[] Start = apply(Each.Turn(), new double[]{X - 8.0, -8.0, Z - 8.0});
            Start = new double[]{Start[0] + 8.0, Start[1] + 8.0, Start[2] + 8.0};
            for (Part Piece : Each.Parts()) {
                double Found = hit(Piece, Start, Direction);
                if (!Double.isNaN(Found)) {
                    Best = Double.isNaN(Best) ? Found : Math.max(Best, Found);
                }
            }
        }
        return Best;
    }

    private static double hit(Part Piece, double[] Start, double[] Direction) {
        double[] Shifted = {Start[0] - Piece.Origin()[0], Start[1] - Piece.Origin()[1], Start[2] - Piece.Origin()[2]};
        double[] Local = transposed(Piece.Turn(), Shifted);
        double[] Way = transposed(Piece.Turn(), Direction);
        double[] At = {Local[0] + Piece.Origin()[0], Local[1] + Piece.Origin()[1], Local[2] + Piece.Origin()[2]};
        double Low = -1.0e9;
        double High = 1.0e9;
        for (int Axis = 0; Axis < 3; Axis++) {
            if (Math.abs(Way[Axis]) < 1.0e-9) {
                if (At[Axis] < Piece.From()[Axis] - 1.0e-9 || At[Axis] > Piece.To()[Axis] + 1.0e-9) {
                    return Double.NaN;
                }
                continue;
            }
            double First = (Piece.From()[Axis] - At[Axis]) / Way[Axis];
            double Second = (Piece.To()[Axis] - At[Axis]) / Way[Axis];
            Low = Math.max(Low, Math.min(First, Second));
            High = Math.min(High, Math.max(First, Second));
        }
        return High < Low ? Double.NaN : High;
    }

    private static double[][] identity() {
        return new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    }

    private static double[][] rotation(char Axis, double Degrees) {
        double C = Math.cos(Math.toRadians(Degrees));
        double S = Math.sin(Math.toRadians(Degrees));
        return switch (Axis) {
            case 'x' -> new double[][]{{1, 0, 0}, {0, C, -S}, {0, S, C}};
            case 'y' -> new double[][]{{C, 0, S}, {0, 1, 0}, {-S, 0, C}};
            default -> new double[][]{{C, -S, 0}, {S, C, 0}, {0, 0, 1}};
        };
    }

    private static double[][] times(double[][] Left, double[][] Right) {
        double[][] Made = new double[3][3];
        for (int Row = 0; Row < 3; Row++) {
            for (int Col = 0; Col < 3; Col++) {
                for (int Inner = 0; Inner < 3; Inner++) {
                    Made[Row][Col] += Left[Row][Inner] * Right[Inner][Col];
                }
            }
        }
        return Made;
    }

    private static double[] apply(double[][] Matrix, double[] Vector) {
        double[] Made = new double[3];
        for (int Row = 0; Row < 3; Row++) {
            Made[Row] = Matrix[Row][0] * Vector[0] + Matrix[Row][1] * Vector[1] + Matrix[Row][2] * Vector[2];
        }
        return Made;
    }

    private static double[] transposed(double[][] Matrix, double[] Vector) {
        double[] Made = new double[3];
        for (int Col = 0; Col < 3; Col++) {
            Made[Col] = Matrix[0][Col] * Vector[0] + Matrix[1][Col] * Vector[1] + Matrix[2][Col] * Vector[2];
        }
        return Made;
    }

    private static JsonObject first(JsonElement Value) {
        return Value.isJsonArray() ? Value.getAsJsonArray().get(0).getAsJsonObject() : Value.getAsJsonObject();
    }

    private static <T extends Comparable<T>> String name(BlockState State, Property<T> Each) {
        return Each.getName(State.getValue(Each));
    }

    private static Map<String, String> values(BlockState State) {
        Map<String, String> Made = new HashMap<>();
        for (Property<?> Each : State.getProperties()) {
            Made.put(Each.getName(), name(State, Each));
        }
        return Made;
    }

    private static boolean fits(String Key, Map<String, String> Values) {
        if (Key.isEmpty() || Key.equals("normal")) {
            return true;
        }
        for (String Pair : Key.split(",")) {
            String[] Parts = Pair.split("=", 2);
            if (Parts.length == 2 && Values.containsKey(Parts[0]) && !Parts[1].equals(Values.get(Parts[0]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean when(JsonObject Condition, Map<String, String> Values) {
        if (Condition.has("OR")) {
            for (JsonElement Each : Condition.getAsJsonArray("OR")) {
                if (when(Each.getAsJsonObject(), Values)) {
                    return true;
                }
            }
            return false;
        }
        if (Condition.has("AND")) {
            for (JsonElement Each : Condition.getAsJsonArray("AND")) {
                if (!when(Each.getAsJsonObject(), Values)) {
                    return false;
                }
            }
            return true;
        }
        for (Map.Entry<String, JsonElement> Entry : Condition.entrySet()) {
            String Have = Values.get(Entry.getKey());
            if (Have == null) {
                continue;
            }
            boolean Any = false;
            for (String Option : Entry.getValue().getAsString().split("\\|")) {
                if (Option.equals(Have)) {
                    Any = true;
                }
            }
            if (!Any) {
                return false;
            }
        }
        return true;
    }

    private static boolean rod(double[] From, double[] To) {
        int Thin = 0;
        for (int Axis = 0; Axis < 3; Axis++) {
            if (To[Axis] - From[Axis] <= Rod + 1.0e-6) {
                Thin++;
            }
        }
        return Thin >= 2;
    }

    private static List<Part> elements(String Model, int Depth) {
        String Named = Model.contains(":") ? Model : "minecraft:" + Model;
        List<Part> Cached = Elements.get(Named);
        if (Cached != null) {
            return Cached;
        }
        List<Part> Found;
        if (FullParents.contains(Named)) {
            Found = Full;
        } else {
            String[] Parts = Named.split(":", 2);
            JsonObject Data = read(Parts[0], "models/" + Parts[1] + ".json");
            if (Data == null || Depth > 12) {
                Found = Unknown;
            } else if (Data.has("elements")) {
                Found = new ArrayList<>();
                for (JsonElement Each : Data.getAsJsonArray("elements")) {
                    JsonObject Element = Each.getAsJsonObject();
                    JsonArray FromArray = Element.getAsJsonArray("from");
                    JsonArray ToArray = Element.getAsJsonArray("to");
                    double[] From = new double[3];
                    double[] To = new double[3];
                    for (int Axis = 0; Axis < 3; Axis++) {
                        double A = FromArray.get(Axis).getAsDouble();
                        double B = ToArray.get(Axis).getAsDouble();
                        From[Axis] = Math.min(A, B);
                        To[Axis] = Math.max(A, B);
                    }
                    if (rod(From, To)) {
                        continue;
                    }
                    double[][] Turn = identity();
                    double[] Origin = {0, 0, 0};
                    if (Element.has("rotation")) {
                        JsonObject Spin = Element.getAsJsonObject("rotation");
                        double Angle = Spin.has("angle") ? Spin.get("angle").getAsDouble() : 0.0;
                        if (Angle != 0.0) {
                            Turn = rotation(Spin.get("axis").getAsString().charAt(0), Angle);
                            JsonArray Pivot = Spin.getAsJsonArray("origin");
                            Origin = new double[]{Pivot.get(0).getAsDouble(), Pivot.get(1).getAsDouble(), Pivot.get(2).getAsDouble()};
                        }
                    }
                    Found.add(new Part(From, To, Turn, Origin));
                }
            } else if (Data.has("parent")) {
                Found = elements(Data.get("parent").getAsString(), Depth + 1);
            } else {
                Found = Unknown;
            }
        }
        Elements.put(Named, Found);
        return Found;
    }

    private static JsonObject read(String Namespace, String Location) {
        String Resource = "assets/" + Namespace + "/" + Location;
        try {
            if (Scp_unleashed.ModId.equals(Namespace)) {
                IModFileInfo Info = ModList.get().getModFileById(Scp_unleashed.ModId);
                if (Info != null) {
                    Path Found = Info.getFile().findResource(Resource.split("/"));
                    if (Files.exists(Found)) {
                        return JsonParser.parseString(Files.readString(Found, StandardCharsets.UTF_8)).getAsJsonObject();
                    }
                }
                return null;
            }
            ClassLoader Loader = Thread.currentThread().getContextClassLoader();
            InputStream Stream = Loader == null ? null : Loader.getResourceAsStream(Resource);
            if (Stream == null) {
                return null;
            }
            try (InputStreamReader Reader = new InputStreamReader(Stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(Reader).getAsJsonObject();
            }
        } catch (Exception Problem) {
            return null;
        }
    }
}
