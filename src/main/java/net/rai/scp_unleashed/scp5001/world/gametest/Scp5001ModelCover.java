package net.rai.scp_unleashed.scp5001.world.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001WallFaces;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Scp5001ModelCover {
    public static final int Grid = Scp5001WallFaces.Grid;
    private static final Set<String> FullParents = Set.of("minecraft:block/cube_all", "minecraft:block/cube", "minecraft:block/cube_bottom_top", "minecraft:block/cube_column",
            "block/cube_all", "block/cube", "block/cube_bottom_top", "block/cube_column");
    private static final String[] Names = {"down", "up", "north", "south", "west", "east"};

    private Scp5001ModelCover() {
    }

    public record Cover(boolean[][][] Sides, List<String> Problems) {
    }

    public static Cover cover(BlockState State) {
        ResourceLocation Id = BuiltInRegistries.BLOCK.getKey(State.getBlock());
        JsonObject Definition = json("/assets/" + Id.getNamespace() + "/blockstates/" + Id.getPath() + ".json");
        if (Definition == null) {
            return null;
        }
        boolean[][][] Sides = new boolean[6][Grid][Grid];
        List<String> Problems = new ArrayList<>();
        List<JsonObject> Picked = new ArrayList<>();
        if (Definition.has("variants")) {
            for (var Entry : Definition.getAsJsonObject("variants").entrySet()) {
                if (matches(State, Entry.getKey())) {
                    Picked.add(first(Entry.getValue()));
                    break;
                }
            }
        }
        if (Definition.has("multipart")) {
            for (JsonElement Part : Definition.getAsJsonArray("multipart")) {
                JsonObject Each = Part.getAsJsonObject();
                if (!Each.has("when") || when(State, Each.getAsJsonObject("when"))) {
                    Picked.add(first(Each.get("apply")));
                }
            }
        }
        for (JsonObject Use : Picked) {
            int Xr = Use.has("x") ? Use.get("x").getAsInt() : 0;
            int Yr = Use.has("y") ? Use.get("y").getAsInt() : 0;
            JsonArray Elements = elements(Use.get("model").getAsString(), 0);
            if (Elements == null) {
                return null;
            }
            paint(Sides, Elements, Xr, Yr, Problems, Use.get("model").getAsString());
        }
        return new Cover(Sides, Problems);
    }

    public static String throughGap(BlockState State) {
        ResourceLocation Id = BuiltInRegistries.BLOCK.getKey(State.getBlock());
        JsonObject Definition = json("/assets/" + Id.getNamespace() + "/blockstates/" + Id.getPath() + ".json");
        if (Definition == null || !Definition.has("variants")) {
            return State + " has no variant table";
        }
        for (var Entry : Definition.getAsJsonObject("variants").entrySet()) {
            if (!matches(State, Entry.getKey())) {
                continue;
            }
            JsonArray Elements = elements(first(Entry.getValue()).get("model").getAsString(), 0);
            if (Elements == null) {
                return State + " has no readable model";
            }
            int Open = 0;
            for (int A = 0; A < Grid; A++) {
                for (int B = 0; B < Grid; B++) {
                    double X = (A + 0.5) * 16.0 / Grid;
                    double Y = (B + 0.5) * 16.0 / Grid;
                    boolean Covered = false;
                    for (JsonElement Raw : Elements) {
                        double[] From = triple(Raw.getAsJsonObject().getAsJsonArray("from"));
                        double[] To = triple(Raw.getAsJsonObject().getAsJsonArray("to"));
                        if (From[0] <= X && X <= To[0] && From[1] <= Y && Y <= To[1] && To[2] > From[2]) {
                            Covered = true;
                            break;
                        }
                    }
                    Open += Covered ? 0 : 1;
                }
            }
            return Open == 0 ? null : State + " leaves " + Open + " of " + Grid * Grid + " cells see-through";
        }
        return State + " matches no variant";
    }

    public static List<String> leaks(BlockState State, Cover Found) {
        List<String> Out = new ArrayList<>();
        if (!State.canOcclude()) {
            return Out;
        }
        for (Direction Side : Direction.values()) {
            VoxelShape Face = State.getFaceOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Side);
            if (Face.isEmpty()) {
                continue;
            }
            List<AABB> Boxes = Face.toAabbs();
            int Holes = 0;
            boolean[][] Mask = Found.Sides()[index(Side)];
            for (int A = 0; A < Grid; A++) {
                for (int B = 0; B < Grid; B++) {
                    double[] Point = point(Side, (A + 0.5) / Grid, (B + 0.5) / Grid);
                    boolean Claimed = false;
                    for (AABB Box : Boxes) {
                        if (Box.contains(Point[0], Point[1], Point[2])) {
                            Claimed = true;
                            break;
                        }
                    }
                    if (Claimed && !Mask[A][B]) {
                        Holes++;
                    }
                }
            }
            if (Holes > 0) {
                Out.add(State + " hides its " + Side.getName() + " neighbour's face but its model leaves " + Holes + " of " + Grid * Grid + " cells open there");
            }
        }
        return Out;
    }

    static double[] point(Direction Side, double A, double B) {
        int Axis = Side.getAxis().ordinal();
        int[] Others = Axis == 0 ? new int[]{1, 2} : Axis == 1 ? new int[]{0, 2} : new int[]{0, 1};
        double[] Point = new double[3];
        Point[Axis] = 0.5;
        Point[Others[0]] = A;
        Point[Others[1]] = B;
        return Point;
    }

    static int index(Direction Side) {
        for (int Index = 0; Index < Names.length; Index++) {
            if (Names[Index].equals(Side.getName())) {
                return Index;
            }
        }
        return 0;
    }

    private static void paint(boolean[][][] Sides, JsonArray Elements, int Xr, int Yr, List<String> Problems, String Model) {
        boolean[] OpenSide = openSides(Elements);
        for (JsonElement Raw : Elements) {
            JsonObject Element = Raw.getAsJsonObject();
            if (Element.has("rotation") && Element.getAsJsonObject("rotation").get("angle").getAsDouble() != 0) {
                continue;
            }
            double[] From = triple(Element.getAsJsonArray("from"));
            double[] To = triple(Element.getAsJsonArray("to"));
            JsonObject Faces = Element.getAsJsonObject("faces");
            for (var Entry : Faces.entrySet()) {
                String Name = Entry.getKey();
                int Side = List.of(Names).indexOf(Name);
                int Axis = Side < 2 ? 1 : Side < 4 ? 2 : 0;
                boolean Positive = Side % 2 == 1;
                double Edge = Positive ? To[Axis] : From[Axis];
                boolean Boundary = Math.abs(Edge - (Positive ? 16 : 0)) < 1e-6;
                JsonObject Face = Entry.getValue().getAsJsonObject();
                if (Face.has("cullface")) {
                    String Cull = Face.get("cullface").getAsString();
                    boolean OnCull = Cull.equals(Name) && Boundary;
                    if (!OnCull && OpenSide[List.of(Names).indexOf(Cull)]) {
                        Problems.add(Model + " culls an inset " + Name + " face with " + Cull + " although that side's recess reaches the edge");
                    }
                }
                if (!Boundary) {
                    continue;
                }
                int[] Others = Axis == 0 ? new int[]{1, 2} : Axis == 1 ? new int[]{0, 2} : new int[]{0, 1};
                for (int A = 0; A < Grid; A++) {
                    for (int B = 0; B < Grid; B++) {
                        double Ca = (A + 0.5) * 16.0 / Grid;
                        double Cb = (B + 0.5) * 16.0 / Grid;
                        if (Ca < From[Others[0]] || Ca > To[Others[0]] || Cb < From[Others[1]] || Cb > To[Others[1]]) {
                            continue;
                        }
                        double[] Point = new double[3];
                        Point[Axis] = Edge;
                        Point[Others[0]] = Ca;
                        Point[Others[1]] = Cb;
                        double[] Normal = new double[3];
                        Normal[Axis] = Positive ? 1 : -1;
                        double[] Turned = turn(Point, Xr, Yr, 8);
                        double[] Facing = turn(Normal, Xr, Yr, 0);
                        int NewAxis = Math.abs(Facing[0]) > 0.5 ? 0 : Math.abs(Facing[1]) > 0.5 ? 1 : 2;
                        boolean NewPositive = Facing[NewAxis] > 0;
                        int NewSide = NewAxis == 1 ? (NewPositive ? 1 : 0) : NewAxis == 2 ? (NewPositive ? 3 : 2) : (NewPositive ? 5 : 4);
                        int[] NewOthers = NewAxis == 0 ? new int[]{1, 2} : NewAxis == 1 ? new int[]{0, 2} : new int[]{0, 1};
                        int Ia = (int) Math.floor(Turned[NewOthers[0]] / 16.0 * Grid);
                        int Ib = (int) Math.floor(Turned[NewOthers[1]] / 16.0 * Grid);
                        if (Ia >= 0 && Ia < Grid && Ib >= 0 && Ib < Grid) {
                            Sides[NewSide][Ia][Ib] = true;
                        }
                    }
                }
            }
        }
    }

    static boolean[] openSides(JsonArray Elements) {
        boolean[][][] Plain = new boolean[6][Grid][Grid];
        JsonArray Copy = new JsonArray();
        for (JsonElement Raw : Elements) {
            JsonObject Element = Raw.getAsJsonObject().deepCopy();
            JsonObject Faces = Element.getAsJsonObject("faces");
            for (var Entry : Faces.entrySet()) {
                Entry.getValue().getAsJsonObject().remove("cullface");
            }
            Copy.add(Element);
        }
        boolean[] Open = new boolean[6];
        paintPlain(Plain, Copy);
        for (int Side = 0; Side < 6; Side++) {
            for (int Index = 0; Index < Grid; Index++) {
                if (!Plain[Side][0][Index] || !Plain[Side][Grid - 1][Index] || !Plain[Side][Index][0] || !Plain[Side][Index][Grid - 1]) {
                    Open[Side] = true;
                }
            }
        }
        return Open;
    }

    private static void paintPlain(boolean[][][] Sides, JsonArray Elements) {
        for (JsonElement Raw : Elements) {
            JsonObject Element = Raw.getAsJsonObject();
            if (Element.has("rotation") && Element.getAsJsonObject("rotation").get("angle").getAsDouble() != 0) {
                continue;
            }
            double[] From = triple(Element.getAsJsonArray("from"));
            double[] To = triple(Element.getAsJsonArray("to"));
            for (String Name : Element.getAsJsonObject("faces").keySet()) {
                int Side = List.of(Names).indexOf(Name);
                int Axis = Side < 2 ? 1 : Side < 4 ? 2 : 0;
                boolean Positive = Side % 2 == 1;
                double Edge = Positive ? To[Axis] : From[Axis];
                if (Math.abs(Edge - (Positive ? 16 : 0)) > 1e-6) {
                    continue;
                }
                int[] Others = Axis == 0 ? new int[]{1, 2} : Axis == 1 ? new int[]{0, 2} : new int[]{0, 1};
                for (int A = 0; A < Grid; A++) {
                    for (int B = 0; B < Grid; B++) {
                        double Ca = (A + 0.5) * 16.0 / Grid;
                        double Cb = (B + 0.5) * 16.0 / Grid;
                        if (Ca >= From[Others[0]] && Ca <= To[Others[0]] && Cb >= From[Others[1]] && Cb <= To[Others[1]]) {
                            Sides[Side][A][B] = true;
                        }
                    }
                }
            }
        }
    }

    static double[] turn(double[] Point, int Xr, int Yr, double Pivot) {
        double X = Point[0] - Pivot;
        double Y = Point[1] - Pivot;
        double Z = Point[2] - Pivot;
        int Cx = cos(Xr);
        int Sx = sin(Xr);
        double Y1 = Cx * Y + Sx * Z;
        double Z1 = -Sx * Y + Cx * Z;
        int Cy = cos(Yr);
        int Sy = sin(Yr);
        double X2 = Cy * X - Sy * Z1;
        double Z2 = Sy * X + Cy * Z1;
        return new double[]{X2 + Pivot, Y1 + Pivot, Z2 + Pivot};
    }

    private static int cos(int Degrees) {
        return switch (Math.floorMod(Degrees, 360)) {
            case 90, 270 -> 0;
            case 180 -> -1;
            default -> 1;
        };
    }

    private static int sin(int Degrees) {
        return switch (Math.floorMod(Degrees, 360)) {
            case 90 -> 1;
            case 270 -> -1;
            default -> 0;
        };
    }

    private static JsonArray elements(String Model, int Depth) {
        if (Depth > 8) {
            return null;
        }
        if (FullParents.contains(Model)) {
            JsonArray Made = new JsonArray();
            JsonObject Cube = new JsonObject();
            Cube.add("from", JsonParser.parseString("[0,0,0]"));
            Cube.add("to", JsonParser.parseString("[16,16,16]"));
            JsonObject Faces = new JsonObject();
            for (String Name : Names) {
                Faces.add(Name, JsonParser.parseString("{\"cullface\":\"" + Name + "\"}"));
            }
            Cube.add("faces", Faces);
            Made.add(Cube);
            return Made;
        }
        ResourceLocation Where = ResourceLocation.parse(Model);
        JsonObject Body = json("/assets/" + Where.getNamespace() + "/models/" + Where.getPath() + ".json");
        if (Body == null) {
            return null;
        }
        if (Body.has("elements")) {
            return Body.getAsJsonArray("elements");
        }
        return Body.has("parent") ? elements(Body.get("parent").getAsString(), Depth + 1) : null;
    }

    private static double[] triple(JsonArray Values) {
        return new double[]{Values.get(0).getAsDouble(), Values.get(1).getAsDouble(), Values.get(2).getAsDouble()};
    }

    private static JsonObject first(JsonElement Value) {
        return Value.isJsonArray() ? Value.getAsJsonArray().get(0).getAsJsonObject() : Value.getAsJsonObject();
    }

    private static boolean matches(BlockState State, String Key) {
        if (Key.isEmpty()) {
            return true;
        }
        for (String Pair : Key.split(",")) {
            String[] Parts = Pair.split("=");
            if (!Parts[1].equals(value(State, Parts[0]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean when(BlockState State, JsonObject Condition) {
        for (var Entry : Condition.entrySet()) {
            if (Entry.getKey().equals("OR") || Entry.getKey().equals("AND")) {
                return true;
            }
            String Have = value(State, Entry.getKey());
            if (Have == null || !List.of(Entry.getValue().getAsString().split("\\|")).contains(Have)) {
                return false;
            }
        }
        return true;
    }

    private static String value(BlockState State, String Name) {
        for (Property<?> Each : State.getProperties()) {
            if (Each.getName().equals(Name)) {
                return text(State, Each);
            }
        }
        return null;
    }

    private static <T extends Comparable<T>> String text(BlockState State, Property<T> Each) {
        return Each.getName(State.getValue(Each));
    }

    private static JsonObject json(String Path) {
        try (InputStream Stream = Scp5001ModelCover.class.getResourceAsStream(Path)) {
            if (Stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception Failure) {
            return null;
        }
    }
}
